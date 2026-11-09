package common;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Thông tin biến động số dư gửi từ Server về Client qua RMI Callback (Observer pattern).
 */
public class TransactionNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fromAccount;
    private String toAccount;
    private double amount;
    private String content;
    private double newBalance;
    private String transactionType; // Ví dụ: "RECEIVE_TRANSFER"
    private long timestamp;

    public TransactionNotification() {
        this.timestamp = System.currentTimeMillis();
    }

    public TransactionNotification(String fromAccount, String toAccount, double amount,
                                   String content, double newBalance, String transactionType) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.content = content;
        this.newBalance = newBalance;
        this.transactionType = transactionType;
        this.timestamp = System.currentTimeMillis();
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public void setToAccount(String toAccount) {
        this.toAccount = toAccount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public double getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(double newBalance) {
        this.newBalance = newBalance;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        return sdf.format(new Date(timestamp));
    }

    @Override
    public String toString() {
        return "TransactionNotification{" +
                "fromAccount='" + fromAccount + '\'' +
                ", toAccount='" + toAccount + '\'' +
                ", amount=" + amount +
                ", content='" + content + '\'' +
                ", newBalance=" + newBalance +
                ", transactionType='" + transactionType + '\'' +
                ", timestamp=" + getFormattedTime() +
                '}';
    }
}

