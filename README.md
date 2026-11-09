# Dự án Mô phỏng eBanking qua Java RMI & RMI Callback (Observer Pattern)

Dự án bài tập lập trình mạng mô phỏng hệ thống Ngân hàng điện tử (eBanking) phân tán với **Java RMI (Remote Method Invocation)**, sử dụng mô hình **Observer Pattern (RMI Callback)** để cập nhật và hiển thị thông báo biến động số dư theo thời gian thực (real-time).

---

## 1. Kiến trúc hệ thống & Observer Pattern

```
  [ Client A (Swing GUI) ]
            │  (1) bankService.transfer("0123", "0456", 1000, "tra tien")
            ▼
┌──────────────────────────────────────────────┐
│            BankServer (Subject)              │
│  - Lưu trữ dữ liệu: data/accounts.csv        │
│  - Quản lý danh sách Observer Callbacks      │
└──────────────────────────────────────────────┘
            │
            │  (2) callbackB.onBalanceChanged(notification)
            ▼
  [ Client B (Swing GUI) ]  <--- (3) SwingUtilities.invokeLater() ---> Popup biến động số dư!
```

### Vai trò các thành phần:
- **`BankService` (Subject / Publisher)**: Interface từ xa của Server, cung cấp các chức năng vấn tin, nạp tiền, rút tiền, chuyển khoản và quản lý danh sách callback (`registerCallback`, `unregisterCallback`).
- **`ClientCallback` (Observer)**: Interface từ xa phía Client do Server gọi ngược về mỗi khi tài khoản có biến động nhận tiền.
- **`TransactionNotification`**: Dữ liệu thông báo (số tài khoản gửi/nhận, số tiền, nội dung, số dư mới, thời gian).
- **`AccountRepository`**: Đọc/ghi dữ liệu bền vững ra file CSV (`data/accounts.csv`), đảm bảo thread-safe với `synchronized`.

---

## 2. Cấu trúc thư mục

```
RMI_Ebanking/
├── data/
│   └── accounts.csv                    # Dữ liệu tài khoản
├── src/
│   ├── common/
│   │   ├── Account.java                # Lớp thực thể tài khoản (Serializable)
│   │   ├── TransactionNotification.java# Thông tin biến động số dư (Serializable)
│   │   ├── BankService.java            # RMI Interface Server (Subject)
│   │   └── ClientCallback.java         # RMI Interface Client Callback (Observer)
│   ├── server/
│   │   ├── AccountRepository.java      # Đọc/ghi CSV bền vững, thread-safe
│   │   ├── BankServiceImpl.java        # Triển khai RMI Service & quản lý callback
│   │   └── ServerMain.java             # Khởi động RMI Registry & bind BankService
│   ├── client/
│   │   ├── ClientCallbackImpl.java     # Triển khai Callback, chuyển tiếp về GUI Swing
│   │   ├── BankingClientGUI.java       # Giao diện Swing chuẩn theo đề bài VKU
│   │   └── ClientMain.java             # Khởi động client, nạp tài khoản & kết nối RMI
│   └── test/
│       └── RmiEbankingIntegrationTest.java # Bộ kiểm thử tích hợp tự động 9 ca test
├── compile.bat                         # Script biên dịch toàn bộ mã nguồn
├── run_server.bat                      # Script khởi động BankServer
├── run_client_0123.bat                 # Script chạy Client tài khoản 0123 (Nguyễn Văn A)
├── run_client_0456.bat                 # Script chạy Client tài khoản 0456 (Trần Thị B)
├── run_test.bat                        # Script chạy kiểm thử tự động
└── README.md
```

---

## 3. Dữ liệu mẫu ban đầu (`data/accounts.csv`)

| Số tài khoản | Chủ tài khoản | Số dư ban đầu |
|:---|:---|:---|
| `0123` | Nguyen Van A | 5,000 VND |
| `0456` | Tran Thi B | 3,000 VND |
| `0789` | Le Van C | 1,000 VND |
| `1234` | Pham Thi D | 2,000 VND |

---

## 4. Hướng dẫn biên dịch và chạy ứng dụng

### Bước 1: Biên dịch mã nguồn
Chạy file `compile.bat` hoặc mở terminal tại thư mục gốc và chạy:
```powershell
javac -encoding UTF-8 -d bin src/common/*.java src/server/*.java src/client/*.java src/test/*.java
```

### Bước 2: Khởi động Server
Chạy file `run_server.bat` hoặc lệnh:
```powershell
java -cp bin server.ServerMain
```
Server sẽ tự khởi tạo RMI Registry tại cổng `1099` và bind dịch vụ `BankService`.

### Bước 3: Mở Client B (nhận tiền)
Chạy file `run_client_0456.bat` hoặc lệnh:
```powershell
java -cp bin client.ClientMain 0456
```
Cửa sổ Client `0456` (Trần Thị B) sẽ mở lên và tự động đăng ký callback với Server.

### Bước 4: Mở Client A (chuyển tiền)
Chạy file `run_client_0123.bat` hoặc lệnh:
```powershell
java -cp bin client.ClientMain 0123
```
Trên Client `0123`:
1. Nhập **Số tiền**: `1000`
2. Nhập **Tài khoản nhận**: `0456`
3. Nhập **Nội dung**: `tra tien an`
4. Bấm nút **Chuyển khoản**.

### Kết quả quan sát:
- Client `0123` hiển thị log: `Chuyển thành công: 1000 đến [0456]; Số dư hiện tại: 4000`
- Client `0456` **ngay lập tức nảy popup thông báo**:
  - Tiêu đề: *Thông báo nhận tiền - 0456*
  - Nội dung: Bạn vừa nhận `+1,000 VND` từ `0123`, nội dung: `tra tien an`, số dư hiện tại `4,000 VND`.
  - Đồng thời log của Client `0456` tự ghi dòng: `Đã nhận: 1000; Tài khoản gửi: 0123;  Nội dung: tra tien an`.

---

## 5. Chạy kiểm thử tự động (Fresh Verification Evidence)
Chạy file `run_test.bat` hoặc:
```powershell
java -cp bin test.RmiEbankingIntegrationTest
```
Bộ kiểm thử sẽ kiểm tra tự động 9/9 ca:
1. Tạo Registry và bind RMI service
2. Vấn tin tài khoản
3. Nạp tiền
4. Rút tiền hợp lệ
5. Chặn rút tiền quá số dư
6. Chặn chuyển tiền tới tài khoản không tồn tại
7. Observer Callback nhận chuyển khoản thời gian thực
8. Hủy callback an toàn khi client ngắt kết nối
9. Lưu trữ bền vững (Persistence) qua đọc lại file CSV.

