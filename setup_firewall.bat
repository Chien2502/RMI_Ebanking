@echo off
chcp 65001 > nul
echo ======================================================================
echo    THIẾT LẬP WINDOWS DEFENDER FIREWALL CHO JAVA RMI & UDP DISCOVERY
echo ======================================================================

:: 1. Kiểm tra quyền Administrator
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [CẢNH BÁO] Cần quyền Administrator để thêm quy tắc Tường lửa!
    echo Đang tự động yêu cầu quyền Administrator (UAC)...
    powershell -Command "Start-Process cmd -ArgumentList '/c \"\"%~f0\"\"' -Verb RunAs"
    exit /b
)

:: 2. Tìm đường dẫn java.exe và javaw.exe
set "JAVA_BIN="
set "JAVAW_BIN="

for /f "tokens=*" %%i in ('where java.exe 2^>nul') do (
    if not defined JAVA_BIN set "JAVA_BIN=%%i"
)
for /f "tokens=*" %%i in ('where javaw.exe 2^>nul') do (
    if not defined JAVAW_BIN set "JAVAW_BIN=%%i"
)

if "%JAVA_BIN%"=="" (
    echo [LỖI] Không tìm thấy java.exe trong biến môi trường PATH!
    pause
    exit /b 1
)

echo Tìm thấy Java  : "%JAVA_BIN%"
echo Tìm thấy Javaw : "%JAVAW_BIN%"
echo.

:: 3. Thêm Inbound Rule cho java.exe (Cho phép cả TCP và UDP)
echo [1/2] Đang tạo Inbound Rule cho java.exe (TCP + UDP Broadcast)...
netsh advfirewall firewall delete rule name="Java Platform SE Binary (RMI Inbound)" >nul 2>&1
netsh advfirewall firewall add rule name="Java Platform SE Binary (RMI Inbound)" dir=in action=allow program="%JAVA_BIN%" enable=yes profile=any protocol=any >nul
if %errorLevel% equ 0 (
    echo       -> Thành công! Cho phép mọi kết nối Inbound (TCP/UDP) đối với java.exe.
) else (
    echo       -> Thất bại khi tạo rule cho java.exe.
)

:: 4. Thêm Inbound Rule cho javaw.exe (Cho phép cả TCP và UDP)
if not "%JAVAW_BIN%"=="" (
    echo [2/2] Đang tạo Inbound Rule cho javaw.exe (TCP + UDP Broadcast)...
    netsh advfirewall firewall delete rule name="Java Platform SE Binary - Javaw (RMI Inbound)" >nul 2>&1
    netsh advfirewall firewall add rule name="Java Platform SE Binary - Javaw (RMI Inbound)" dir=in action=allow program="%JAVAW_BIN%" enable=yes profile=any protocol=any >nul
    if %errorLevel% equ 0 (
        echo       -> Thành công! Cho phép mọi kết nối Inbound (TCP/UDP) đối với javaw.exe.
    ) else (
        echo       -> Thất bại khi tạo rule cho javaw.exe.
    )
)

echo.
echo ======================================================================
echo [HOÀN TẤT] Tường lửa đã mở toàn diện:
echo 1. RMI TCP Socket (Port 1099 + Cổng ngẫu nhiên Callback)
echo 2. UDP Broadcast Auto-Discovery (Port 8888, nhịp 2 giây)
echo ======================================================================
pause
