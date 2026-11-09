package server;

import common.Account;
import common.BankService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;

/**
 * Điểm khởi động Server RMI Banking.
 * Khởi tạo RMI Registry tại cổng 1099 và đăng ký BankService.
 */
public class ServerMain {
    public static final int RMI_PORT = 1099;
    public static final String SERVICE_NAME = "BankService";

    public static void main(String[] args) {
        try {
            // Đảm bảo RMI liên lạc chính xác qua localhost
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");

            System.out.println("==================================================");
            System.out.println("      KHỞI ĐỘNG RMI E-BANKING SERVER              ");
            System.out.println("==================================================");

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
}

