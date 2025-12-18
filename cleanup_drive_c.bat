@echo off
chcp 65001 >nul
title Script Dọn Dẹp Ổ C - Windows Cleanup

echo ========================================
echo   SCRIPT DON DEP O C TU DONG
echo   Bat dau luc: %date% %time%
echo ========================================
echo.

:: Kiểm tra quyền Admin
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [CANH BAO] Script can quyen Administrator de hoat dong tot nhat!
    echo Hay click chuot phai va chon "Run as administrator"
    echo.
    pause
    exit /b 1
)

echo [1/12] Dang xoa Windows Temp files...
del /q/f/s "%WINDIR%\Temp\*" >nul 2>&1
rd /s /q "%WINDIR%\Temp" >nul 2>&1
md "%WINDIR%\Temp" >nul 2>&1
echo   [OK] Hoan thanh

echo [2/12] Dang xoa User Temp files...
del /q/f/s "%TEMP%\*" >nul 2>&1
rd /s /q "%TEMP%" >nul 2>&1
md "%TEMP%" >nul 2>&1
echo   [OK] Hoan thanh

echo [3/12] Dang xoa Prefetch files...
del /q/f/s "%WINDIR%\Prefetch\*" >nul 2>&1
echo   [OK] Hoan thanh

echo [4/12] Dang xoa Recent files...
del /q/f/s "%APPDATA%\Microsoft\Windows\Recent\*" >nul 2>&1
echo   [OK] Hoan thanh

echo [5/12] Dang xoa Windows Update cache...
net stop wuauserv >nul 2>&1
del /q/f/s "%WINDIR%\SoftwareDistribution\Download\*" >nul 2>&1
net start wuauserv >nul 2>&1
echo   [OK] Hoan thanh

echo [6/12] Dang xoa Windows Log files...
del /q/f/s "%WINDIR%\Logs\*" >nul 2>&1
echo   [OK] Hoan thanh

echo [7/12] Dang xoa Internet Explorer cache...
del /q/f/s "%LOCALAPPDATA%\Microsoft\Windows\INetCache\*" >nul 2>&1
echo   [OK] Hoan thanh

echo [8/12] Dang xoa Windows Error Reports...
del /q/f/s "%LOCALAPPDATA%\Microsoft\Windows\WER\*" >nul 2>&1
del /q/f/s "%PROGRAMDATA%\Microsoft\Windows\WER\*" >nul 2>&1
echo   [OK] Hoan thanh

echo [9/12] Dang xoa npm cache...
where npm >nul 2>&1
if %errorLevel% equ 0 (
    call npm cache clean --force >nul 2>&1
    echo   [OK] Hoan thanh
) else (
    echo   [-] npm khong duoc cai dat, bo qua
)

echo [10/12] Dang xoa Gradle cache...
if exist "%USERPROFILE%\.gradle\caches" (
    rd /s /q "%USERPROFILE%\.gradle\caches" >nul 2>&1
    echo   [OK] Hoan thanh
) else (
    echo   [-] Gradle cache khong ton tai, bo qua
)

echo [11/12] Dang don Recycle Bin...
rd /s /q C:\$Recycle.Bin >nul 2>&1
echo   [OK] Hoan thanh

echo [12/12] Dang chay Windows Disk Cleanup...
cleanmgr /sagerun:1 >nul 2>&1
echo   [OK] Hoan thanh

echo.
echo ========================================
echo   HOAN THANH DON DEP!
echo   Ket thuc luc: %date% %time%
echo ========================================
echo.
echo Nhan phim bat ky de dong...
pause >nul
