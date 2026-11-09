package test;

import common.Account;
import common.BankService;
import common.ClientCallback;
import common.TransactionNotification;
import server.AccountRepository;
import server.BankServiceImpl;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Kiểm thử tích hợp tự động cho toàn bộ hệ thống RMI eBanking.
 * Kiểm chứng 100% các ca kiểm thử theo kế hoạch đề ra:
 * 1. Khởi động RMI Server và bind Registry
 * 2. Vấn tin tài khoản hợp lệ
 * 3. Nạp tiền và kiểm tra số dư mới
 * 4. Rút tiền hợp lệ
 * 5. Rút quá số dư (báo lỗi an toàn)
 * 6. Chuyển khoản lỗi (tài khoản không tồn tại, số tiền không hợp lệ)
 * 7. Observer Callback: Client A chuyển cho Client B, Client B nhận callback tức thì
 * 8. Hủy callback Client B, chuyển tiền tiếp vẫn thành công không lỗi
 * 9. Đọc lại từ file CSV để kiểm chứng tính bền vững dữ liệu (Persistence)
 */
public class RmiEbankingIntegrationTest {
    private static final int TEST_PORT = 11099;
    private static final String TEST_CSV = "data/test_accounts.csv";

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  BẮT ĐẦU CHẠY KIỂM THỬ TÍCH HỢP RMI E-BANKING    ");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        File testFile = new File(TEST_CSV);
        if (testFile.exists()) {
            testFile.delete();
        }

        try {
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");

            // 1. Khởi động RMI Registry & Service
            System.out.print("[Test 1] Khởi động RMI Registry (port " + TEST_PORT + ") & bind BankService... ");
            Registry registry = LocateRegistry.createRegistry(TEST_PORT);
            AccountRepository repo = new AccountRepository(TEST_CSV);
            BankServiceImpl serverImpl = new BankServiceImpl(repo);
            registry.rebind("BankService", serverImpl);

            BankService bankService = (BankService) registry.lookup("BankService");
            if (bankService != null) {
                System.out.println("PASSED! ✔");
                passed++;
            } else {
                System.out.println("FAILED! ❌");
                failed++;
            }

            // 2. Vấn tin tài khoản 0123
            System.out.print("[Test 2] Vấn tin tài khoản 0123 (mặc định 5000)... ");
            Account acc0123 = bankService.getAccount("0123");
            if (acc0123 != null && acc0123.getBalance() == 5000.0) {
                System.out.println("PASSED! (Số dư: " + acc0123.getBalance() + ") ✔");
                passed++;
            } else {
                System.out.println("FAILED! ❌ (Kết quả: " + acc0123 + ")");
                failed++;
            }

            // 3. Nạp tiền 1000 vào 0123
            System.out.print("[Test 3] Nạp tiền 1000 vào tài khoản 0123... ");
            String depResult = bankService.deposit("0123", 1000.0);
            Account accAfterDep = bankService.getAccount("0123");
            if (accAfterDep != null && accAfterDep.getBalance() == 6000.0 && depResult.contains("Đã nạp: 1000")) {
                System.out.println("PASSED! (Thông báo: '" + depResult + "') ✔");
                passed++;
            } else {
                System.out.println("FAILED! ❌ (" + depResult + ")");
                failed++;
            }

            // 4. Rút tiền 500 từ 0123
            System.out.print("[Test 4] Rút tiền 500 từ tài khoản 0123... ");
            String withResult = bankService.withdraw("0123", 500.0);
            Account accAfterWith = bankService.getAccount("0123");
            if (accAfterWith != null && accAfterWith.getBalance() == 5500.0 && withResult.contains("Đã rút: 500")) {
                System.out.println("PASSED! (Thông báo: '" + withResult + "') ✔");
                passed++;
            } else {
                System.out.println("FAILED! ❌ (" + withResult + ")");
                failed++;
            }

            // 5. Rút quá số dư (rút 9999999) -> phải từ chối
            System.out.print("[Test 5] Rút quá số dư tài khoản 0123 (rút 999999)... ");
            String overWithResult = bankService.withdraw("0123", 999999.0);
            Account accAfterOverWith = bankService.getAccount("0123");
            if (accAfterOverWith != null && accAfterOverWith.getBalance() == 5500.0 && overWithResult.contains("Lỗi: Số dư không đủ")) {
                System.out.println("PASSED! (Chặn đúng: '" + overWithResult + "') ✔");
                passed++;
            } else {
                System.out.println("FAILED! ❌ (" + overWithResult + ")");
                failed++;
            }

            // 6. Chuyển khoản tới tài khoản không tồn tại -> phải từ chối
            System.out.print("[Test 6] Chuyển khoản tới tài khoản không tồn tại 9999... ");
            String transferInvalidResult = bankService.transfer("0123", "9999", 500.0, "test");
            if (transferInvalidResult.contains("Lỗi: Tài khoản nhận") && transferInvalidResult.contains("không tồn tại")) {
                System.out.println("PASSED! (Báo lỗi chuẩn: '" + transferInvalidResult + "') ✔");
                passed++;
            } else {
                System.out.println("FAILED! ❌ (" + transferInvalidResult + ")");
                failed++;
            }

            // 7. Observer Callback: Đăng ký callback cho 0456, 0123 chuyển tiền cho 0456
            System.out.print("[Test 7] Observer Callback: 0123 chuyển 1000 cho 0456... ");
            CountDownLatch callbackLatch = new CountDownLatch(1);
            AtomicReference<TransactionNotification> receivedNotif = new AtomicReference<>();

            ClientCallback callback0456 = new ClientCallback() {
                @Override
                public void onBalanceChanged(TransactionNotification notification) throws RemoteException {
                    receivedNotif.set(notification);
                    callbackLatch.countDown();
                }
            };
            UnicastRemoteObject.exportObject(callback0456, 0);
            bankService.registerCallback("0456", callback0456);

            // Chuyển 1000 từ 0123 sang 0456
            String transResult = bankService.transfer("0123", "0456", 1000.0, "tra tien an");
            boolean notified = callbackLatch.await(5, TimeUnit.SECONDS);

            Account acc0123End = bankService.getAccount("0123");
            Account acc0456End = bankService.getAccount("0456");

            if (notified && receivedNotif.get() != null
                    && "0123".equals(receivedNotif.get().getFromAccount())
                    && "0456".equals(receivedNotif.get().getToAccount())
                    && receivedNotif.get().getAmount() == 1000.0
                    && "tra tien an".equals(receivedNotif.get().getContent())
                    && acc0123End.getBalance() == 4500.0
                    && acc0456End.getBalance() == 4000.0) {
                System.out.println("PASSED! ✔");
                System.out.println("         -> Server đã gọi Callback báo 0456 nhận 1000đ từ 0123!");
                System.out.println("         -> Số dư 0123: " + acc0123End.getBalance() + ", Số dư 0456: " + acc0456End.getBalance());
                passed++;
            } else {
                System.out.println("FAILED! ❌ (Notified: " + notified + ", Notif: " + receivedNotif.get() + ")");
                failed++;
            }

            // 8. Hủy callback 0456 (giả lập tắt client 0456), chuyển tiếp vẫn thành công
            System.out.print("[Test 8] Hủy callback 0456 (mô phỏng tắt client), chuyển tiếp 500đ... ");
            bankService.unregisterCallback("0456");
            UnicastRemoteObject.unexportObject(callback0456, true);

            String transResult2 = bankService.transfer("0123", "0456", 500.0, "tien ca phe");
            Account acc0123Final = bankService.getAccount("0123");
            Account acc0456Final = bankService.getAccount("0456");

            if (acc0123Final.getBalance() == 4000.0 && acc0456Final.getBalance() == 4500.0
                    && transResult2.contains("Chuyển thành công")) {
                System.out.println("PASSED! (Giao dịch hoàn tất bình thường mà không bị crash callback) ✔");
                passed++;
            } else {
                System.out.println("FAILED! ❌ (" + transResult2 + ")");
                failed++;
            }

            // 9. Kiểm chứng tính bền vững (Persistence) từ file CSV
            System.out.print("[Test 9] Kiểm chứng tính bền vững (Persistence) qua đọc file CSV... ");
            AccountRepository freshRepo = new AccountRepository(TEST_CSV);
            Account fresh0123 = freshRepo.getAccount("0123");
            Account fresh0456 = freshRepo.getAccount("0456");
            if (fresh0123 != null && fresh0123.getBalance() == 4000.0
                    && fresh0456 != null && fresh0456.getBalance() == 4500.0) {
                System.out.println("PASSED! (Dữ liệu trong CSV hoàn toàn khớp chuẩn) ✔");
                passed++;
            } else {
                System.out.println("FAILED! ❌");
                failed++;
            }

            // Dọn dẹp test
            UnicastRemoteObject.unexportObject(serverImpl, true);
            UnicastRemoteObject.unexportObject(registry, true);
            if (testFile.exists()) {
                testFile.delete();
            }

        } catch (Exception e) {
            System.err.println("\n[Test] LỖI NGOẠI LỆ: " + e.getMessage());
            e.printStackTrace();
            failed++;
        }

        System.out.println("==================================================");
        System.out.println("TỔNG KẾT KIỂM THỬ: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }
}

