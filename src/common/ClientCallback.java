package common;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface RMI Callback cho phía Client.
 * Đóng vai trò là Observer trong mô hình Observer Pattern.
 * Server (Subject) sẽ gọi phương thức này khi có biến động số dư tới tài khoản của client.
 */
public interface ClientCallback extends Remote {
    /**
     * Được Server gọi ngược về khi tài khoản có biến động số dư (nhận tiền chuyển khoản...).
     * @param notification Thông tin chi tiết giao dịch
     * @throws RemoteException nếu xảy ra lỗi truyền thông RMI
     */
    void onBalanceChanged(TransactionNotification notification) throws RemoteException;
}

