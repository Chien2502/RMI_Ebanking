@echo off
chcp 65001 > nul
echo [1/2] Dang tao thu muc bin...
if not exist bin mkdir bin

echo [2/2] Dang bien dich ma nguon Java...
javac -encoding UTF-8 -d bin src/common/*.java src/server/*.java src/client/*.java src/test/*.java

if %ERRORLEVEL% EQU 0 (
    echo [THANH CONG] Bien dich hoan tat vao thu muc bin/!
) else (
    echo [THAT BAI] Co loi xay ra trong qua trinh bien dich!
)
pause

