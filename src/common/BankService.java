package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface RMI chính phía Server cung cấp các dịch vụ ngân hàng.
 * Đóng vai trò Subject/Publisher trong mô hình Observer Pattern.
 */
public interface BankService extends Remote {

    /**
     * Vấn tin thông tin tài khoản.
     * @param accountNumber Số tài khoản cần tra cứu
     * @return Thông tin tài khoản hoặc null nếu không tìm thấy
     * @throws RemoteException
     */
    Account getAccount(String accountNumber) throws RemoteException;

    /**
     * Nạp tiền vào tài khoản.
     * @param accountNumber Số tài khoản nhận
     * @param amount Số tiền nạp (> 0)
     * @return Thông báo kết quả giao dịch
     * @throws RemoteException
     */
    String deposit(String accountNumber, double amount) throws RemoteException;

    /**
     * Rút tiền từ tài khoản.
     * @param accountNumber Số tài khoản rút
     * @param amount Số tiền rút (> 0, <= số dư)
     * @return Thông báo kết quả giao dịch
     * @throws RemoteException
     */
    String withdraw(String accountNumber, double amount) throws RemoteException;

    /**
     * Chuyển tiền từ tài khoản nguồn sang tài khoản nhận.
     * Nếu tài khoản nhận đang mở client có đăng ký callback, Server sẽ gọi thông báo biến động số dư.
     * @param fromAccount Số tài khoản chuyển
     * @param toAccount Số tài khoản nhận
     * @param amount Số tiền chuyển
     * @param content Nội dung chuyển khoản
     * @return Thông báo kết quả giao dịch
     * @throws RemoteException
     */
    String transfer(String fromAccount, String toAccount, double amount, String content) throws RemoteException;

    /**
     * Đăng ký Observer Callback để nhận thông báo biến động số dư theo số tài khoản.
     * @param accountNumber Số tài khoản đăng ký
     * @param callback Đối tượng callback phía client
     * @throws RemoteException
     */
    void registerCallback(String accountNumber, ClientCallback callback) throws RemoteException;

    /**
     * Hủy đăng ký Observer Callback khi client đóng ứng dụng.
     * @param accountNumber Số tài khoản hủy
     * @throws RemoteException
     */
    void unregisterCallback(String accountNumber) throws RemoteException;
}

