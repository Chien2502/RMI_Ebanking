package server;

import common.Account;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý lưu trữ và truy xuất thông tin tài khoản qua file CSV.
 * Áp dụng đồng bộ (synchronized) để đảm bảo thread-safety khi nhiều client RMI gọi đồng thời.
 */
public class AccountRepository {
    private final String filePath;
    private final Map<String, Account> accounts = new HashMap<>();

    public AccountRepository() {
        this("data/accounts.csv");
    }

    public AccountRepository(String filePath) {
        this.filePath = filePath;
        initDataFile();
        loadAccounts();
    }

    /**
     * Khởi tạo file CSV mẫu nếu file chưa tồn tại.
     */
    private synchronized void initDataFile() {
        File file = new File(filePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write("accountNumber,ownerName,balance\n");
                writer.write("0123,Nguyen Van A,5000\n");
                writer.write("0456,Tran Thi B,3000\n");
                writer.write("0789,Le Van C,1000\n");
                writer.write("1234,Pham Thi D,2000\n");
            } catch (IOException e) {
                System.err.println("[AccountRepository] Lỗi khởi tạo file CSV: " + e.getMessage());
            }
        }
    }

    /**
     * Đọc toàn bộ danh sách tài khoản từ file CSV vào bộ nhớ.
     */
    public synchronized void loadAccounts() {
        accounts.clear();
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length >= 3) {
                    String accNo = parts[0].trim();
                    String owner = parts[1].trim();
                    double balance = 0.0;
                    try {
                        balance = Double.parseDouble(parts[2].trim());
                    } catch (NumberFormatException ignored) {}
                    accounts.put(accNo, new Account(accNo, owner, balance));
                }
            }
        } catch (IOException e) {
            System.err.println("[AccountRepository] Lỗi đọc file CSV: " + e.getMessage());
        }
    }

    /**
     * Ghi toàn bộ dữ liệu tài khoản từ bộ nhớ ra file CSV.
     */
    public synchronized void saveAccounts() {
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write("accountNumber,ownerName,balance\n");
            for (Account acc : accounts.values()) {
                writer.write(String.format("%s,%s,%.2f\n",
                        acc.getAccountNumber(),
                        acc.getOwnerName(),
                        acc.getBalance()));
            }
        } catch (IOException e) {
            System.err.println("[AccountRepository] Lỗi ghi file CSV: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin tài khoản (trả về bản sao để tránh thay đổi trực tiếp ngoài repository).
     */
    public synchronized Account getAccount(String accountNumber) {
        Account acc = accounts.get(accountNumber);
        if (acc == null) return null;
        return new Account(acc.getAccountNumber(), acc.getOwnerName(), acc.getBalance());
    }

    /**
     * Cập nhật số dư cho tài khoản.
     */
    public synchronized boolean updateBalance(String accountNumber, double newBalance) {
        Account acc = accounts.get(accountNumber);
        if (acc != null) {
            acc.setBalance(newBalance);
            saveAccounts();
            return true;
        }
        return false;
    }

    /**
     * Nạp tiền vào tài khoản.
     */
    public synchronized boolean deposit(String accountNumber, double amount) {
        if (amount <= 0) return false;
        Account acc = accounts.get(accountNumber);
        if (acc == null) return false;
        acc.setBalance(acc.getBalance() + amount);
        saveAccounts();
        return true;
    }

    /**
     * Rút tiền từ tài khoản.
     */
    public synchronized boolean withdraw(String accountNumber, double amount) {
        if (amount <= 0) return false;
        Account acc = accounts.get(accountNumber);
        if (acc == null || acc.getBalance() < amount) return false;
        acc.setBalance(acc.getBalance() - amount);
        saveAccounts();
        return true;
    }

    /**
     * Thực hiện chuyển khoản nguyên tử (Atomic Transfer) giữa hai tài khoản.
     */
    public synchronized boolean transfer(String fromAccount, String toAccount, double amount) {
        if (amount <= 0) return false;
        Account from = accounts.get(fromAccount);
        Account to = accounts.get(toAccount);

        if (from == null || to == null) return false;
        if (from.getBalance() < amount) return false;

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);
        saveAccounts();
        return true;
    }

    public synchronized Map<String, Account> getAllAccounts() {
        Map<String, Account> copy = new HashMap<>();
        for (Map.Entry<String, Account> entry : accounts.entrySet()) {
            copy.put(entry.getKey(), new Account(entry.getValue().getAccountNumber(),
                    entry.getValue().getOwnerName(), entry.getValue().getBalance()));
        }
        return copy;
    }
}

