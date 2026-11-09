package client;

import common.Account;
import common.BankService;
import common.TransactionNotification;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Giao diện người dùng Swing mô phỏng eBanking Client.
 * Thiết kế bám sát nguyên mẫu bài tập VKU:
 * - 4 hàng: Tài khoản gốc, Số tiền, Tài khoản nhận, Nội dung.
 * - 4 nút bên phải: Vấn tin, Nạp tiền, Rút tiền, Chuyển khoản.
 * - Khu vực log bên dưới hiển thị lịch sử biến động số dư.
 * - Popup thông báo tự động nảy lên khi nhận tiền qua RMI Callback.
 */
public class BankingClientGUI extends JFrame {
    private final BankService bankService;
    private final ClientCallbackImpl callback;
    private String currentAccount;

    private JTextField txtSourceAccount;
    private JTextField txtAmount;
    private JTextField txtTargetAccount;
    private JTextField txtContent;
    private JTextArea txtLogs;

    private final NumberFormat currencyFormatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

    public BankingClientGUI(BankService bankService, ClientCallbackImpl callback, String initialAccount) {
        this.bankService = bankService;
        this.callback = callback;
        this.currentAccount = initialAccount != null ? initialAccount.trim() : "0123";

        // Gắn GUI vào callback
        if (this.callback != null) {
            this.callback.setGui(this);
        }

        initUI();
        registerCallbackForCurrentAccount();
        doInquiry(false); // Vấn tin lần đầu khi mở app
    }

    private void initUI() {
        setTitle(currentAccount);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(520, 420);
        setMinimumSize(new Dimension(480, 380));
        setLocationRelativeTo(null);

        // Thiết lập sự kiện đóng cửa sổ để hủy callback sạch sẽ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupAndExit();
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Form nhập liệu phía trên (4 hàng, 3 cột: Label, TextField, Button)
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Font chữ chuẩn
        Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 13);
        Font buttonFont = new Font("Segoe UI", Font.BOLD, 12);

        // Hàng 1: Tài khoản gốc - [ TextField ] - [ Vấn tin ]
        JLabel lblSource = new JLabel("Tài khoản gốc:");
        lblSource.setFont(labelFont);
        txtSourceAccount = new JTextField(currentAccount, 14);
        txtSourceAccount.setFont(fieldFont);
        JButton btnInquiry = new JButton("Vấn tin");
        btnInquiry.setFont(buttonFont);
        btnInquiry.setPreferredSize(new Dimension(115, 26));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(lblSource, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        formPanel.add(txtSourceAccount, gbc);
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(btnInquiry, gbc);

        // Hàng 2: Số tiền - [ TextField ] - [ Nạp tiền ]
        JLabel lblAmount = new JLabel("Số tiền:");
        lblAmount.setFont(labelFont);
        txtAmount = new JTextField(14);
        txtAmount.setFont(fieldFont);
        JButton btnDeposit = new JButton("Nạp tiền");
        btnDeposit.setFont(buttonFont);
        btnDeposit.setPreferredSize(new Dimension(115, 26));

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(lblAmount, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        formPanel.add(txtAmount, gbc);
        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(btnDeposit, gbc);

        // Hàng 3: Tài khoản nhận - [ TextField ] - [ Rút tiền ]
        JLabel lblTarget = new JLabel("Tài khoản nhận:");
        lblTarget.setFont(labelFont);
        txtTargetAccount = new JTextField(14);
        txtTargetAccount.setFont(fieldFont);
        JButton btnWithdraw = new JButton("Rút tiền");
        btnWithdraw.setFont(buttonFont);
        btnWithdraw.setPreferredSize(new Dimension(115, 26));

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(lblTarget, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        formPanel.add(txtTargetAccount, gbc);
        gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(btnWithdraw, gbc);

        // Hàng 4: Nội dung - [ TextField ] - [ Chuyển khoản ]
        JLabel lblContent = new JLabel("Nội dung:");
        lblContent.setFont(labelFont);
        txtContent = new JTextField(14);
        txtContent.setFont(fieldFont);
        JButton btnTransfer = new JButton("Chuyển khoản");
        btnTransfer.setFont(buttonFont);
        btnTransfer.setPreferredSize(new Dimension(115, 26));

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(lblContent, gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        formPanel.add(txtContent, gbc);
        gbc.gridx = 2; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(btnTransfer, gbc);

        mainPanel.add(formPanel, BorderLayout.NORTH);

        // Khu vực Log hiển thị kết quả và biến động phía dưới (JTextArea trong JScrollPane)
        txtLogs = new JTextArea();
        txtLogs.setEditable(false);
        txtLogs.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLogs.setMargin(new Insets(6, 8, 6, 8));
        txtLogs.setBackground(new Color(248, 249, 250));
        txtLogs.setForeground(new Color(33, 37, 41));

        JScrollPane scrollPane = new JScrollPane(txtLogs);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Lịch sử giao dịch & Biến động"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(mainPanel);

        // Gán sự kiện cho các nút
        btnInquiry.addActionListener(e -> doInquiry(true));
        btnDeposit.addActionListener(e -> doDeposit());
        btnWithdraw.addActionListener(e -> doWithdraw());
        btnTransfer.addActionListener(e -> doTransfer());
    }

    /**
     * Đăng ký callback của client hiện tại lên server
     */
    private void registerCallbackForCurrentAccount() {
        if (bankService != null && callback != null && currentAccount != null && !currentAccount.isEmpty()) {
            try {
                bankService.registerCallback(currentAccount, callback);
                System.out.println("[ClientGUI] Đã đăng ký callback cho tài khoản " + currentAccount);
            } catch (RemoteException e) {
                appendLog("[Hệ thống] Không thể đăng ký callback: " + e.getMessage());
            }
        }
    }

    /**
     * Vấn tin tài khoản
     */
    private void doInquiry(boolean explicitClick) {
        String inputAcc = txtSourceAccount.getText().trim();
        if (inputAcc.isEmpty()) {
            if (explicitClick) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số tài khoản gốc!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        // Nếu người dùng đổi số tài khoản trong ô nhập, đổi callback đăng ký theo
        if (!inputAcc.equals(currentAccount)) {
            try {
                if (bankService != null && currentAccount != null) {
                    bankService.unregisterCallback(currentAccount);
                }
            } catch (Exception ignored) {}
            currentAccount = inputAcc;
            setTitle(currentAccount);
            registerCallbackForCurrentAccount();
        }

        try {
            Account acc = bankService.getAccount(currentAccount);
            if (acc != null) {
                appendLog(String.format("Số dư: %.0f", acc.getBalance()));
            } else {
                appendLog("Lỗi: Tài khoản [" + currentAccount + "] không tồn tại trong hệ thống!");
            }
        } catch (RemoteException e) {
            appendLog("Lỗi kết nối Server: " + e.getMessage());
        }
    }

    /**
     * Nạp tiền
     */
    private void doDeposit() {
        String acc = txtSourceAccount.getText().trim();
        String amtStr = txtAmount.getText().trim();

        if (acc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số tài khoản gốc!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (amtStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số tiền cần nạp!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ (phải là số > 0)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String result = bankService.deposit(acc, amount);
            appendLog(result);
            txtAmount.setText("");
        } catch (RemoteException e) {
            appendLog("Lỗi RMI nạp tiền: " + e.getMessage());
        }
    }

    /**
     * Rút tiền
     */
    private void doWithdraw() {
        String acc = txtSourceAccount.getText().trim();
        String amtStr = txtAmount.getText().trim();

        if (acc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số tài khoản gốc!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (amtStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số tiền cần rút!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ (phải là số > 0)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String result = bankService.withdraw(acc, amount);
            appendLog(result);
            txtAmount.setText("");
        } catch (RemoteException e) {
            appendLog("Lỗi RMI rút tiền: " + e.getMessage());
        }
    }

    /**
     * Chuyển khoản
     */
    private void doTransfer() {
        String fromAcc = txtSourceAccount.getText().trim();
        String toAcc = txtTargetAccount.getText().trim();
        String amtStr = txtAmount.getText().trim();
        String content = txtContent.getText().trim();

        if (fromAcc.isEmpty() || toAcc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Tài khoản gốc và Tài khoản nhận!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (amtStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số tiền cần chuyển!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ (phải là số > 0)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String result = bankService.transfer(fromAcc, toAcc, amount, content);
            appendLog(result);
            txtAmount.setText("");
            txtTargetAccount.setText("");
            txtContent.setText("");
        } catch (RemoteException e) {
            appendLog("Lỗi RMI chuyển tiền: " + e.getMessage());
        }
    }

    /**
     * Xử lý khi nhận được thông báo RMI Callback từ Server (Observer Pattern).
     * Được gọi trên luồng Swing Event Dispatch Thread (EDT).
     */
    public void handleIncomingTransfer(TransactionNotification notif) {
        // Ghi log định dạng giống mẫu đề bài: "Đã nhận: 1000; Tài khoản gửi: 1234;  Nội dung: trả tiền"
        String logLine = String.format("Đã nhận: %.0f; Tài khoản gửi: %s;  Nội dung: %s",
                notif.getAmount(), notif.getFromAccount(), notif.getContent());
        appendLog(logLine);

        // Bật Popup Dialog thông báo biến động số dư
        String popupMessage = String.format(
                "🔔 BIẾN ĐỘNG SỐ DƯ (RMI Callback)\n" +
                "-----------------------------------------------\n" +
                "Tài khoản thụ hưởng: %s\n" +
                "Số tiền nhận: +%s VND\n" +
                "Người gửi: %s\n" +
                "Nội dung: %s\n" +
                "Số dư hiện tại: %s VND\n" +
                "Thời gian: %s",
                notif.getToAccount(),
                currencyFormatter.format(notif.getAmount()),
                notif.getFromAccount(),
                notif.getContent(),
                currencyFormatter.format(notif.getNewBalance()),
                notif.getFormattedTime()
        );

        JOptionPane.showMessageDialog(this, popupMessage,
                "Thông báo nhận tiền - " + notif.getToAccount(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void appendLog(String text) {
        txtLogs.append(text + "\n");
        txtLogs.setCaretPosition(txtLogs.getDocument().getLength());
    }

    /**
     * Dọn dẹp RMI Callback và thoát ứng dụng
     */
    private void cleanupAndExit() {
        try {
            if (bankService != null && currentAccount != null) {
                bankService.unregisterCallback(currentAccount);
                System.out.println("[ClientGUI] Đã hủy đăng ký callback cho " + currentAccount);
            }
        } catch (Exception ignored) {}

        if (callback != null) {
            callback.unexport();
        }

        dispose();
        System.exit(0);
    }
}
