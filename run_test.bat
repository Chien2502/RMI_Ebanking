@echo off
chcp 65001 > nul
echo Dang chay kiem thu tich hop tu dong RMI eBanking...
java "-Dfile.encoding=UTF-8" -cp bin test.RmiEbankingIntegrationTest
pause
