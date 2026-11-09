package common;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * Cơ chế Tự động Dò tìm BankServer trong mạng LAN (Auto-Discovery).
 * - Phía Server: Định kỳ mỗi 2 giây phát một gói tin UDP Broadcast thông báo IP và Port của mình.
 * - Phía Client: Lắng nghe gói tin Broadcast để tự động nhận diện IP của Server mà không cần người dùng nhập tay.
 */
public class ServerDiscovery {
    public static final int DISCOVERY_PORT = 8888;
    public static final int BEACON_INTERVAL_MS = 2000; // 2 giây theo yêu cầu
    public static final String MAGIC_HEADER = "EBANKING_SERVER_BEACON";

    public static class DiscoveredServer {
        private final String ip;
        private final int port;
        private final String serviceName;

        public DiscoveredServer(String ip, int port, String serviceName) {
            this.ip = ip;
            this.port = port;
            this.serviceName = serviceName;
        }

        public String getIp() { return ip; }
        public int getPort() { return port; }
        public String getServiceName() { return serviceName; }

        @Override
        public String toString() {
            return String.format("%s:%d/%s", ip, port, serviceName);
        }
    }

    /**
     * Khởi động luồng phát tín hiệu định kỳ (Beacon) từ phía Server.
     */
    public static Thread startBeacon(String serverIp, int rmiPort, String serviceName) {
        Thread beaconThread = new Thread(() -> {
            String payload = String.format("%s|%s|%d|%s", MAGIC_HEADER, serverIp, rmiPort, serviceName);
            byte[] buffer = payload.getBytes(StandardCharsets.UTF_8);

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);

                while (!Thread.currentThread().isInterrupted()) {
                    // 1. Gửi tới địa chỉ Broadcast toàn cục 255.255.255.255
                    try {
                        DatagramPacket packet = new DatagramPacket(
                                buffer, buffer.length,
                                InetAddress.getByName("255.255.255.255"),
                                DISCOVERY_PORT
                        );
                        socket.send(packet);
                    } catch (Exception ignored) {}

                    // 2. Gửi thêm tới từng địa chỉ Subnet Broadcast của từng card mạng để đảm bảo xuyên mạng
                    try {
                        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                        while (interfaces != null && interfaces.hasMoreElements()) {
                            NetworkInterface iface = interfaces.nextElement();
                            if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                            for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                                InetAddress broadcast = addr.getBroadcast();
                                if (broadcast != null) {
                                    DatagramPacket subPacket = new DatagramPacket(
                                            buffer, buffer.length, broadcast, DISCOVERY_PORT
                                    );
                                    socket.send(subPacket);
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    // Ngủ đúng 2 giây trước lần phát tiếp theo
                    try {
                        Thread.sleep(BEACON_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("[ServerDiscovery] Lỗi socket phát Beacon: " + e.getMessage());
            }
        }, "Server-Discovery-Beacon-Thread");

        beaconThread.setDaemon(true); // Luồng ngầm tự tắt khi Server dừng
        beaconThread.start();
        return beaconThread;
    }

    /**
     * Phía Client: Lắng nghe gói tin UDP Broadcast trong khoảng timeout (mili-giây).
     * @param timeoutMs Thời gian tối đa chờ đợi (ví dụ: 4500ms để bắt trọn nhịp 2s của Server)
     * @return Thông tin server tìm thấy, hoặc null nếu không tìm thấy sau thời gian chờ.
     */
    public static DiscoveredServer discoverServer(int timeoutMs) {
        DatagramSocket socket = null;
        try {
            // Cho phép tái sử dụng cổng (SO_REUSEADDR) để nhiều Client trên cùng 1 máy có thể cùng quét
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(DISCOVERY_PORT));
            socket.setSoTimeout(timeoutMs);

            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();

                if (message.startsWith(MAGIC_HEADER)) {
                    String[] parts = message.split("\\|");
                    if (parts.length >= 4) {
                        String ip = parts[1].trim();
                        int port = Integer.parseInt(parts[2].trim());
                        String serviceName = parts[3].trim();
                        return new DiscoveredServer(ip, port, serviceName);
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            // Hết thời gian chờ mà không nhận được gói tin
        } catch (Exception e) {
            System.err.println("[ClientDiscovery] Lỗi khi quét tìm Server: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
        return null;
    }
}

