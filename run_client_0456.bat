@echo off
chcp 65001 > nul
echo Dang khoi dong Client cho tai khoan 0456...
java "-Dfile.encoding=UTF-8" -cp bin client.ClientMain 0456
pause
