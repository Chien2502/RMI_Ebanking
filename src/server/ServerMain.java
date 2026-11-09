package server;

import common.Account;
import common.BankService;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;
import java.util.Map;

/**
 * Điểm khởi động Server RMI Banking.
 * Khởi tạo RMI Registry tại cổng 1099 và đăng ký BankService.
 * Tự động phát hiện IP LAN thật để hỗ trợ chạy giữa 2 máy khác nhau qua mạng.
 */
public class ServerMain {
    public static final int RMI_PORT = 1099;
    public static final String SERVICE_NAME = "BankService";

    public static void main(String[] args) {
        try {
            // Xác định IP máy chủ: ưu tiên tham số dòng lệnh -> tự phát hiện IP LAN -> 127.0.0.1
            String serverIp = (args.length > 0 && !args[0].trim().isEmpty())
                    ? args[0].trim()
                    : getLocalLanIp();

            System.setProperty("java.rmi.server.hostname", serverIp);

            System.out.println("==================================================");
            System.out.println("      KHỞI ĐỘNG RMI E-BANKING SERVER              ");
            System.out.println("==================================================");
            System.out.println("[ServerMain] IP máy chủ được công bố: " + serverIp);
            System.out.println("[ServerMain] Cổng RMI Registry: " + RMI_PORT);
            System.out.println("[ServerMain] (Nếu chạy Client từ máy khác, hãy nhập IP: " + serverIp + ")");
            System.out.println("--------------------------------------------------");

            // Khởi tạo RMI Registry nếu chưa có
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(RMI_PORT);
                System.out.println("[ServerMain] RMI Registry đã được tạo tại cổng " + RMI_PORT);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(RMI_PORT);
                System.out.println("[ServerMain] Sử dụng RMI Registry sẵn có tại cổng " + RMI_PORT);
            }

            // Tạo và đăng ký service
            BankServiceImpl bankService = new BankServiceImpl();
            registry.rebind(SERVICE_NAME, bankService);
            System.out.println("[ServerMain] Đã bind dịch vụ '" + SERVICE_NAME + "' thành công!");

            // In danh sách tài khoản hiện có trong CSDL CSV
            Map<String, Account> accounts = bankService.getRepository().getAllAccounts();
            System.out.println("\n[ServerMain] Danh sách tài khoản nạp từ data/accounts.csv:");
            System.out.println("--------------------------------------------------");
            System.out.printf("%-12s | %-20s | %-12s\n", "Số TK", "Chủ tài khoản", "Số dư");
            System.out.println("--------------------------------------------------");
            for (Account acc : accounts.values()) {
                System.out.printf("%-12s | %-20s | %,12.0f VND\n",
                        acc.getAccountNumber(), acc.getOwnerName(), acc.getBalance());
            }
            System.out.println("--------------------------------------------------");
            System.out.println("\n[ServerMain] Server đang lắng nghe kết nối từ Client...");
            System.out.println("Nhấn Ctrl+C để dừng server.\n");

        } catch (Exception e) {
            System.err.println("[ServerMain] Lỗi khởi động RMI Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tự động dò tìm địa chỉ IPv4 LAN thật của máy tính (bỏ qua loopback và virtual NIC).
     */
    public static String getLocalLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
