package client;

import common.BankService;

import javax.swing.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Điểm khởi động Client ứng dụng eBanking.
 * Kết nối tới RMI Server, khởi tạo Callback và mở giao diện Swing.
 */
public class ClientMain {
    public static final String SERVER_HOST = "127.0.0.1";
    public static final int RMI_PORT = 1099;
    public static final String SERVICE_NAME = "BankService";

    public static void main(String[] args) {
        // Đặt Look and Feel chuẩn hệ thống
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Xác định số tài khoản từ tham số dòng lệnh hoặc hộp thoại nhập
        String accountNumber = (args.length > 0 && !args[0].trim().isEmpty()) ? args[0].trim() : null;

        if (accountNumber == null) {
            accountNumber = JOptionPane.showInputDialog(
                    null,
                    "Nhập số tài khoản của Client (gợi ý: 0123, 0456, 0789, 1234):",
                    "Đăng nhập eBanking",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (accountNumber == null || accountNumber.trim().isEmpty()) {
                accountNumber = "0123"; // Giá trị mặc định
            } else {
                accountNumber = accountNumber.trim();
            }
        }

        final String finalAccount = accountNumber;

        // Kết nối tới Server RMI
        try {
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");
            System.out.println("[ClientMain] Đang kết nối tới RMI Server tại " + SERVER_HOST + ":" + RMI_PORT + "...");

            Registry registry = LocateRegistry.getRegistry(SERVER_HOST, RMI_PORT);
            BankService bankService = (BankService) registry.lookup(SERVICE_NAME);
            System.out.println("[ClientMain] Đã kết nối thành công tới dịch vụ '" + SERVICE_NAME + "'");

            // Khởi tạo RMI Callback đối tượng (Observer)
            ClientCallbackImpl callback = new ClientCallbackImpl();

            // Khởi tạo và hiển thị GUI trên luồng Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                BankingClientGUI gui = new BankingClientGUI(bankService, callback, finalAccount);
                gui.setVisible(true);
                System.out.println("[ClientMain] GUI Client đã hiển thị cho tài khoản [" + finalAccount + "]");
            });

        } catch (Exception e) {
            System.err.println("[ClientMain] Lỗi kết nối RMI Server: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Không thể kết nối đến BankServer!\nVui lòng đảm bảo Server đã được khởi chạy.\nLỗi: " + e.getMessage(),
                    "Lỗi kết nối",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }
    }
}

