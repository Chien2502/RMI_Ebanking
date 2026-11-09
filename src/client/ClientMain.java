package client;

import common.BankService;

import javax.swing.*;
import java.awt.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;

/**
 * Điểm khởi động Client ứng dụng eBanking.
 * Kết nối tới RMI Server, khởi tạo Callback và mở giao diện Swing.
 * Hỗ trợ chạy trên cùng máy (127.0.0.1) hoặc giữa 2 máy khác nhau qua mạng LAN.
 */
public class ClientMain {
    public static final int RMI_PORT = 1099;
    public static final String SERVICE_NAME = "BankService";

    public static void main(String[] args) {
        // Đặt Look and Feel chuẩn hệ thống
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        String accountNumber = null;
        String serverHost = "127.0.0.1";

        if (args.length >= 2) {
            accountNumber = args[0].trim();
            serverHost = args[1].trim();
        } else if (args.length == 1) {
            accountNumber = args[0].trim();
        } else {
            // Hiển thị hộp thoại nhập Số tài khoản và IP Server khi chạy trực tiếp không tham số
            JTextField txtAcc = new JTextField("0123", 12);
            JTextField txtHost = new JTextField("127.0.0.1", 12);

            JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
            panel.add(new JLabel("Số tài khoản (gợi ý: 0123, 0456, 1234):"));
            panel.add(txtAcc);
            panel.add(new JLabel("Địa chỉ IP BankServer:"));
            panel.add(txtHost);

            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "Đăng nhập eBanking Client",
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
        } else {
            String clientLanIp = getLocalLanIp();
            System.setProperty("java.rmi.server.hostname", clientLanIp);
            System.out.println("[ClientMain] IP mạng LAN của Client để nhận Callback: " + clientLanIp);
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
                    "1. Server đã được bật.\n" +
                    "2. Đã chạy file 'setup_firewall.bat' (Run as Administrator) trên cả 2 máy.\n" +
                    "3. Địa chỉ IP Server đã nhập chính xác.\n\nLỗi: " + e.getMessage(),
                    "Lỗi kết nối",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }
    }

    /**
     * Tự động dò tìm địa chỉ IPv4 LAN của máy Client
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
