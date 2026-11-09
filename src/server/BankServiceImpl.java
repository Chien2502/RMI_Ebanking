package server;

import common.Account;
import common.BankService;
import common.ClientCallback;
import common.TransactionNotification;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cài đặt dịch vụ BankService phía Server.
 * Triển khai mô hình Observer Pattern (RMI Callback):
 * - BankServiceImpl là Subject/Publisher.
 * - ClientCallback là Observer.
 */
public class BankServiceImpl extends UnicastRemoteObject implements BankService {
    private static final long serialVersionUID = 1L;

    private final AccountRepository repository;
    private final Map<String, ClientCallback> callbacks = new ConcurrentHashMap<>();

    public BankServiceImpl() throws RemoteException {
        this(new AccountRepository());
    }

    public BankServiceImpl(AccountRepository repository) throws RemoteException {
        super();
        this.repository = repository;
    }

    @Override
    public Account getAccount(String accountNumber) throws RemoteException {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return null;
        }
        return repository.getAccount(accountNumber.trim());
    }

    @Override
    public synchronized String deposit(String accountNumber, double amount) throws RemoteException {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return "Lỗi: Số tài khoản không hợp lệ!";
        }
        if (amount <= 0) {
            return "Lỗi: Số tiền nạp phải lớn hơn 0!";
        }
        Account acc = repository.getAccount(accountNumber.trim());
        if (acc == null) {
            return "Lỗi: Tài khoản " + accountNumber + " không tồn tại!";
        }

        boolean success = repository.deposit(accountNumber.trim(), amount);
        if (success) {
            Account updated = repository.getAccount(accountNumber.trim());
            return String.format("Đã nạp: %.0f; Số dư hiện tại: %.0f", amount, updated.getBalance());
        } else {
            return "Lỗi: Giao dịch nạp tiền thất bại!";
        }
    }

    @Override
    public synchronized String withdraw(String accountNumber, double amount) throws RemoteException {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return "Lỗi: Số tài khoản không hợp lệ!";
        }
        if (amount <= 0) {
            return "Lỗi: Số tiền rút phải lớn hơn 0!";
        }
        Account acc = repository.getAccount(accountNumber.trim());
        if (acc == null) {
            return "Lỗi: Tài khoản " + accountNumber + " không tồn tại!";
        }
        if (acc.getBalance() < amount) {
            return "Lỗi: Số dư không đủ để thực hiện rút tiền! (Số dư: " + (long)acc.getBalance() + ")";
        }

        boolean success = repository.withdraw(accountNumber.trim(), amount);
        if (success) {
            Account updated = repository.getAccount(accountNumber.trim());
            return String.format("Đã rút: %.0f; Số dư hiện tại: %.0f", amount, updated.getBalance());
        } else {
            return "Lỗi: Giao dịch rút tiền thất bại!";
        }
    }

    @Override
    public synchronized String transfer(String fromAccount, String toAccount, double amount, String content) throws RemoteException {
        if (fromAccount == null || toAccount == null) {
            return "Lỗi: Tài khoản nguồn hoặc đích không hợp lệ!";
        }
        fromAccount = fromAccount.trim();
        toAccount = toAccount.trim();

        if (fromAccount.isEmpty() || toAccount.isEmpty()) {
            return "Lỗi: Vui lòng nhập đầy đủ tài khoản nguồn và tài khoản nhận!";
        }
        if (fromAccount.equals(toAccount)) {
            return "Lỗi: Tài khoản nguồn và nhận không được trùng nhau!";
        }
        if (amount <= 0) {
            return "Lỗi: Số tiền chuyển phải lớn hơn 0!";
        }

        Account fromAcc = repository.getAccount(fromAccount);
        if (fromAcc == null) {
            return "Lỗi: Tài khoản nguồn [" + fromAccount + "] không tồn tại!";
        }
        Account toAcc = repository.getAccount(toAccount);
        if (toAcc == null) {
            return "Lỗi: Tài khoản nhận [" + toAccount + "] không tồn tại!";
        }
        if (fromAcc.getBalance() < amount) {
            return "Lỗi: Số dư tài khoản nguồn không đủ! (Hiện có: " + (long)fromAcc.getBalance() + ")";
        }

        boolean success = repository.transfer(fromAccount, toAccount, amount);
        if (!success) {
            return "Lỗi: Giao dịch chuyển tiền thất bại khi ghi dữ liệu!";
        }

        Account updatedFrom = repository.getAccount(fromAccount);
        Account updatedTo = repository.getAccount(toAccount);

        // Gửi thông báo RMI Callback (Observer) cho tài khoản nhận nếu đang mở client
        ClientCallback recipientCallback = callbacks.get(toAccount);
        if (recipientCallback != null) {
            try {
                TransactionNotification notification = new TransactionNotification(
                        fromAccount,
                        toAccount,
                        amount,
                        (content == null || content.trim().isEmpty()) ? "Chuyen tien" : content.trim(),
                        updatedTo.getBalance(),
                        "RECEIVE_TRANSFER"
                );
                // Gọi callback không phong tỏa luồng bằng thread riêng hoặc gọi trực tiếp
                recipientCallback.onBalanceChanged(notification);
                System.out.println("[BankServer] Đã gửi Callback biến động số dư tới tài khoản " + toAccount);
            } catch (RemoteException e) {
                System.err.println("[BankServer] Gửi callback tới " + toAccount + " thất bại, tự động hủy đăng ký: " + e.getMessage());
                callbacks.remove(toAccount);
            }
        } else {
            System.out.println("[BankServer] Tài khoản nhận " + toAccount + " hiện không kết nối Client (không có callback)");
        }

        return String.format("Chuyển thành công: %.0f đến [%s]; Số dư hiện tại: %.0f",
                amount, toAccount, updatedFrom.getBalance());
    }

    @Override
    public void registerCallback(String accountNumber, ClientCallback callback) throws RemoteException {
        if (accountNumber != null && callback != null) {
            String acc = accountNumber.trim();
            callbacks.put(acc, callback);
            System.out.println("[BankServer] Đã đăng ký Observer Callback cho tài khoản: " + acc
                    + " (Tổng callback đang hoạt động: " + callbacks.size() + ")");
        }
    }

    @Override
    public void unregisterCallback(String accountNumber) throws RemoteException {
        if (accountNumber != null) {
            String acc = accountNumber.trim();
            callbacks.remove(acc);
            System.out.println("[BankServer] Đã hủy đăng ký Callback cho tài khoản: " + acc
                    + " (Còn lại: " + callbacks.size() + ")");
        }
    }

    public AccountRepository getRepository() {
        return repository;
    }
}

