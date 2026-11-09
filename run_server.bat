@echo off
chcp 65001 > nul
echo Dang khoi dong RMI BankServer...
java "-Dfile.encoding=UTF-8" -cp bin server.ServerMain
pause
