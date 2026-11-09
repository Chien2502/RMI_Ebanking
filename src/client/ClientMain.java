package client;

import common.BankService;
import common.NetworkUtils;
import common.ServerDiscovery;

import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Điểm khởi động Client ứng dụng eBanking.
 * Tự động dò tìm địa chỉ IP của BankServer qua UDP Broadcast (nhịp 2s của Server).
 * Tự động trích xuất IPv4 LAN của Client để Server có thể gọi RMI Callback ngược lại qua mạng LAN.
 */
public class ClientMain {
    public static final int RMI_PORT = 1099;
    public static final String SERVICE_NAME = "BankService";

    public static void main(String[] args) {
        // Đặt Look and Feel chuẩn hệ thống
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        String myLanIp = NetworkUtils.getLocalLanIp();
        String accountNumber = null;
        String serverHost = null;

        // Nếu người dùng đã truyền đủ tham số dòng lệnh: [accountNumber, serverHost]
        if (args.length >= 2) {
            accountNumber = args[0].trim();
            serverHost = args[1].trim();
        } else if (args.length == 1) {
            accountNumber = args[0].trim();
        }

        // Nếu chưa có IP server, tiến hành tự động quét tìm trong mạng LAN (tối đa ~4.2s để đón nhịp 2s)
        ServerDiscovery.DiscoveredServer discovered = null;
        if (serverHost == null) {
            System.out.println("==================================================================");
            System.out.println(" 📡 ĐANG TỰ ĐỘNG DÒ TÌM BANKSERVER TRONG MẠNG LAN...             ");
            System.out.println("    (Lắng nghe gói tin Broadcast nhịp 2s từ Server, tối đa ~4s)  ");
            System.out.println("==================================================================");

            discovered = ServerDiscovery.discoverServer(4200);

            if (discovered != null) {
                serverHost = discovered.getIp();
                System.out.println(" [✓ THÀNH CÔNG] Đã tự động bắt được Server tại IP: " + serverHost);
            } else {
                System.out.println(" [!] Không nhận được tín hiệu Broadcast sau 4s. Chuyển sang cấu hình mặc định/nhập tay.");
                serverHost = "127.0.0.1";
            }
        }

        // Nếu chạy không truyền tham số (args rỗng), mở hộp thoại cho người dùng xác nhận
        if (args.length == 0) {
            JTextField txtAcc = new JTextField("0123", 14);
            JTextField txtHost = new JTextField(serverHost, 14);

            JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
            panel.add(new JLabel("Số tài khoản (0123, 0456...):"));
            panel.add(txtAcc);
            panel.add(new JLabel("Địa chỉ IP BankServer:"));
            panel.add(txtHost);
            panel.add(new JLabel("Trạng thái tự động dò tìm:"));

            JLabel lblStatus = new JLabel(discovered != null ? "✓ Đã tìm thấy trên LAN" : "! Nhập tay (hoặc Localhost)");
            lblStatus.setForeground(discovered != null ? new Color(0, 153, 76) : new Color(204, 102, 0));
            lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
            panel.add(lblStatus);

            panel.add(new JLabel("IPv4 LAN máy bạn (tự dò):"));
            JLabel lblMyIp = new JLabel(myLanIp);
            lblMyIp.setForeground(new Color(0, 102, 204));
            lblMyIp.setFont(new Font("Segoe UI", Font.BOLD, 12));
            panel.add(lblMyIp);

            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "Đăng nhập eBanking Client (Auto-Discovery)",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                accountNumber = txtAcc.getText().trim();
                serverHost = txtHost.getText().trim();
            } else {
                System.exit(0);
            }
        }

        if (accountNumber == null || accountNumber.isEmpty()) {
            accountNumber = "0123";
        }
        if (serverHost == null || serverHost.isEmpty()) {
            serverHost = "127.0.0.1";
        }

        final String finalAccount = accountNumber;
        final String finalHost = serverHost;

        // Cấu hình IP mà Client công bố với Server để nhận RMI Callback
        if (finalHost.equals("127.0.0.1") || finalHost.equalsIgnoreCase("localhost")) {
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");
            System.out.println("[ClientMain] Đang chạy chế độ Localhost (127.0.0.1)");
        } else {
            System.setProperty("java.rmi.server.hostname", myLanIp);
            System.out.println("[ClientMain] Đang chạy chế độ mạng LAN!");
            System.out.println("[ClientMain] IP Server kết nối tới : " + finalHost);
            System.out.println("[ClientMain] IP máy Client nhận Callback: " + myLanIp);
        }

        // Kết nối tới Server RMI
        try {
            System.out.println("[ClientMain] Đang kết nối tới RMI Server tại " + finalHost + ":" + RMI_PORT + "...");

            Registry registry = LocateRegistry.getRegistry(finalHost, RMI_PORT);
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
                    "Không thể kết nối đến BankServer tại [" + finalHost + ":" + RMI_PORT + "]!\n" +
                    "Vui lòng đảm bảo:\n" +
                    "1. Server đã được bật trên máy chủ.\n" +
                    "2. Đã chạy file 'setup_firewall.bat' (Run as Administrator) trên cả 2 máy.\n" +
                    "3. Đã kết nối cùng mạng Wi-Fi/LAN.\n\nLỗi chi tiết: " + e.getMessage(),
                    "Lỗi kết nối",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }
    }
}
