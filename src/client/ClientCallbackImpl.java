package client;

import common.ClientCallback;
import common.TransactionNotification;

import javax.swing.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Cài đặt ClientCallback phía Client (Observer).
 * Nhận thông báo biến động số dư từ BankServer và chuyển giao cho giao diện Swing hiển thị.
 */
public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
    private static final long serialVersionUID = 1L;

    private BankingClientGUI gui;

    public ClientCallbackImpl() throws RemoteException {
        super();
    }

    public ClientCallbackImpl(BankingClientGUI gui) throws RemoteException {
        super();
        this.gui = gui;
    }

    public void setGui(BankingClientGUI gui) {
        this.gui = gui;
    }

    @Override
    public void onBalanceChanged(TransactionNotification notification) throws RemoteException {
        System.out.println("[ClientCallback] Nhận thông báo từ Server: " + notification);
        // Đảm bảo việc cập nhật UI Swing và hiện Popup được thực thi trên Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                gui.handleIncomingTransfer(notification);
            }
        });
    }

    /**
     * Hủy export đối tượng RMI khi đóng ứng dụng để giải phóng cổng mạng.
     */
    public void unexport() {
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (Exception ignored) {
        }
    }
}

