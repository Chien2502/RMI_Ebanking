@echo off
chcp 65001 > nul
echo Dang khoi dong Client cho tai khoan 0123...
java "-Dfile.encoding=UTF-8" -cp bin client.ClientMain 0123
pause
