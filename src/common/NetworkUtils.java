package common;

import java.net.*;
import java.util.*;

/**
 * Tiện ích dò tìm và trích xuất địa chỉ IPv4 mạng LAN chính xác của máy tính.
 * Xử lý thông minh:
 * - Ưu tiên các card mạng vật lý đang hoạt động (Wi-Fi, Ethernet).
 * - Loại bỏ triệt để các địa chỉ ảo, Loopback (127.0.0.1) và Link-Local (169.254.x.x APIPA).
 * - Sử dụng thuật toán định tuyến Routing Table của hệ điều hành để tìm IP hướng ra gateway chuẩn nhất.
 */
public class NetworkUtils {

    public static class InterfaceInfo {
        private final String name;
        private final String displayName;
        private final String ip;
        private final boolean isRecommended;

        public InterfaceInfo(String name, String displayName, String ip, boolean isRecommended) {
            this.name = name;
            this.displayName = displayName;
            this.ip = ip;
            this.isRecommended = isRecommended;
        }

        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getIp() { return ip; }
        public boolean isRecommended() { return isRecommended; }
    }

    /**
     * Lấy địa chỉ IPv4 LAN tối ưu nhất của máy hiện tại.
     * @return Chuỗi IP IPv4 (ví dụ: 192.168.1.15 hoặc 10.123.66.110)
     */
    public static String getLocalLanIp() {
        // Cách 1: Thử định tuyến qua UDP Socket (không gửi gói tin ra ngoài, chỉ mượn routing table của OS)
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            InetAddress addr = socket.getLocalAddress();
            if (isValidLanIp(addr)) {
                return addr.getHostAddress();
            }
        } catch (Exception ignored) {
            // Trường hợp mạng nội bộ không có kết nối ra ngoài Internet
        }

        // Cách 2: Quét tất cả các NetworkInterface và ưu tiên Wi-Fi / Ethernet
        List<InterfaceInfo> interfaces = getAllLanInterfaces();
        for (InterfaceInfo info : interfaces) {
            if (info.isRecommended()) {
                return info.getIp();
            }
        }

        if (!interfaces.isEmpty()) {
            return interfaces.get(0).getIp();
        }

        // Cách 3: Fallback về getLocalHost
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            if (isValidLanIp(localHost)) {
                return localHost.getHostAddress();
            }
        } catch (Exception ignored) {}

        // Cuối cùng: Trả về loopback nếu không có mạng nào
        return "127.0.0.1";
    }

    /**
     * Liệt kê tất cả các địa chỉ IPv4 hợp lệ đang hoạt động trên máy tính.
     */
    public static List<InterfaceInfo> getAllLanInterfaces() {
        List<InterfaceInfo> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                    continue;
                }

                String lowerName = (iface.getName() + " " + iface.getDisplayName()).toLowerCase();
                // Bỏ qua các adapter ảo thông dụng
                if (lowerName.contains("vmware") || lowerName.contains("virtual") ||
                    lowerName.contains("vbox") || lowerName.contains("docker") ||
                    lowerName.contains("hyper-v") || lowerName.contains("wsl") ||
                    lowerName.contains("bluetooth") || lowerName.contains("tap")) {
                    continue;
                }

                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (isValidLanIp(addr)) {
                        boolean isWifiOrEth = lowerName.contains("wi-fi") || lowerName.contains("wireless") ||
                                              lowerName.contains("wlan") || lowerName.contains("ethernet") ||
                                              lowerName.contains("eth");
                        result.add(new InterfaceInfo(
                                iface.getName(),
                                iface.getDisplayName(),
                                addr.getHostAddress(),
                                isWifiOrEth
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[NetworkUtils] Lỗi quét card mạng: " + e.getMessage());
        }

        // Sắp xếp đưa các card được khuyến nghị (Wi-Fi/Ethernet) lên đầu
        result.sort((a, b) -> Boolean.compare(b.isRecommended(), a.isRecommended()));
        return result;
    }

    /**
     * Kiểm tra xem một InetAddress có phải là IPv4 LAN hợp lệ (không phải Loopback, không phải Link-Local APIPA)
     */
    public static boolean isValidLanIp(InetAddress addr) {
        if (!(addr instanceof Inet4Address)) {
            return false;
        }
        if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()) {
            return false;
        }
        String ip = addr.getHostAddress();
        // Loại bỏ dải APIPA 169.254.x.x
        return !ip.startsWith("169.254.");
    }

    /**
     * In báo cáo danh sách card mạng IPv4 ra Console để người dùng dễ theo dõi
     */
    public static void printLanReport(String selectedIp) {
        System.out.println("==================================================================");
        System.out.println(" 📡 DÒ TÌM ĐỊA CHỈ IPV4 MẠNG LAN CỦA MÁY HIỆN TẠI                 ");
        System.out.println("==================================================================");
        List<InterfaceInfo> list = getAllLanInterfaces();
        if (list.isEmpty()) {
            System.out.println(" Không tìm thấy card mạng LAN vật lý nào (Chỉ có Localhost 127.0.0.1)");
        } else {
            System.out.printf(" %-28s | %-16s | %s\n", "Tên Card mạng", "Địa chỉ IPv4", "Trạng thái");
            System.out.println(" -----------------------------+------------------+----------------");
            for (InterfaceInfo info : list) {
                String status = info.getIp().equals(selectedIp)
                        ? "★ [ĐANG SỬ DỤNG CHO RMI]"
                        : (info.isRecommended() ? "√ [Khuyên dùng]" : "  [Phụ trợ]");
                String display = info.getDisplayName();
                if (display.length() > 28) display = display.substring(0, 25) + "...";
                System.out.printf(" %-28s | %-16s | %s\n", display, info.getIp(), status);
            }
        }
        System.out.println("==================================================================");
        System.out.println(" 👉 Địa chỉ IPv4 LAN được chọn: " + selectedIp);
        System.out.println("    (Dùng địa chỉ này để Client từ máy khác kết nối tới)");
        System.out.println("==================================================================\n");
    }
}

