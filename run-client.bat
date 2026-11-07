@echo off
chcp 65001 > nul 2>&1
title MiniMart - Khoi chay Socket Client
echo ========================================================
echo   CHUONG TRINH CLIENT SOCKET MINIMART (CHUONG 5)
echo ========================================================
echo.
echo   [1] Khoi chay Client Giao Dien Do Hoa (Java Swing GUI)
echo   [2] Khoi chay Client Console (Terminal)
echo   [3] Khoi chay HTTP Client (HttpURLConnection)
echo.
set /p opt="Vui long chon (1, 2 hoac 3): "

if "%opt%"=="1" (
    echo Dang mo Client Do hoa...
    java -Dfile.encoding=UTF-8 socket-client/MiniMartGuiClient.java
) else if "%opt%"=="2" (
    echo Dang mo Client Console...
    java -Dfile.encoding=UTF-8 socket-client/MiniMartClient.java
) else if "%opt%"=="3" (
    echo Dang gui yeu cau HttpURLConnection...
    java -Dfile.encoding=UTF-8 socket-client/MiniMartHttpClient.java
) else (
    echo Lua chon khong hop le.
)
pause

