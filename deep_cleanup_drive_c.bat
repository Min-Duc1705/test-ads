@echo off
chcp 65001 >nul
title Deep Cleanup - Dọn Dẹp Sâu Ổ C
color 0A

echo ╔══════════════════════════════════════════════════════════════╗
echo ║          DEEP CLEANUP - DON DEP SAU O C                      ║
echo ║          Bat dau: %date% %time%                    ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

:: Kiểm tra quyền Admin
net session >nul 2>&1
if %errorLevel% neq 0 (
    color 0C
    echo [LOI] Script PHAI chay voi quyen Administrator!
    echo Hay click chuot phai va chon "Run as administrator"
    echo.
    pause
    exit /b 1
)

echo ══════════════════════════════════════════════════════════════
echo   PHAN 1: DON DEP FILES TAM HE THONG
echo ══════════════════════════════════════════════════════════════

echo [01/30] Xoa Windows Temp...
del /q/f/s "%WINDIR%\Temp\*" >nul 2>&1
rd /s /q "%WINDIR%\Temp" >nul 2>&1
md "%WINDIR%\Temp" >nul 2>&1
echo   [OK] Done

echo [02/30] Xoa User Temp...
del /q/f/s "%TEMP%\*" >nul 2>&1
del /q/f/s "%LOCALAPPDATA%\Temp\*" >nul 2>&1
echo   [OK] Done

echo [03/30] Xoa Prefetch...
del /q/f/s "%WINDIR%\Prefetch\*" >nul 2>&1
echo   [OK] Done

echo [04/30] Xoa Windows Logs...
del /q/f/s "%WINDIR%\Logs\*" >nul 2>&1
del /q/f/s "%WINDIR%\System32\LogFiles\*" >nul 2>&1
echo   [OK] Done

echo [05/30] Xoa CBS Logs...
del /q/f/s "%WINDIR%\Logs\CBS\*" >nul 2>&1
echo   [OK] Done

echo [06/30] Xoa DISM Logs...
del /q/f/s "%WINDIR%\Logs\DISM\*" >nul 2>&1
echo   [OK] Done

echo [07/30] Xoa Memory Dump files...
del /f /q "%WINDIR%\MEMORY.DMP" >nul 2>&1
del /f /q "%WINDIR%\Minidump\*" >nul 2>&1
del /f /q "%LOCALAPPDATA%\CrashDumps\*" >nul 2>&1
echo   [OK] Done

echo.
echo ══════════════════════════════════════════════════════════════
echo   PHAN 2: DON DEP WINDOWS UPDATE VA DRIVERS
echo ══════════════════════════════════════════════════════════════

echo [08/30] Dung Windows Update service...
net stop wuauserv >nul 2>&1
net stop bits >nul 2>&1

echo [09/30] Xoa Windows Update cache...
del /q/f/s "%WINDIR%\SoftwareDistribution\Download\*" >nul 2>&1
rd /s /q "%WINDIR%\SoftwareDistribution\Download" >nul 2>&1
md "%WINDIR%\SoftwareDistribution\Download" >nul 2>&1
echo   [OK] Done

echo [10/30] Xoa Windows Update DataStore...
del /q/f/s "%WINDIR%\SoftwareDistribution\DataStore\*" >nul 2>&1
echo   [OK] Done

echo [11/30] Xoa Driver cache...
del /q/f/s "%WINDIR%\System32\DriverStore\FileRepository\*.old" >nul 2>&1
echo   [OK] Done

echo [12/30] Khoi dong lai Windows Update service...
net start wuauserv >nul 2>&1
net start bits >nul 2>&1
echo   [OK] Done

echo.
echo ══════════════════════════════════════════════════════════════
echo   PHAN 3: DON DEP CACHE TRINH DUYET
echo ══════════════════════════════════════════════════════════════

echo [13/30] Xoa Internet Explorer/Edge cache...
del /q/f/s "%LOCALAPPDATA%\Microsoft\Windows\INetCache\*" >nul 2>&1
del /q/f/s "%LOCALAPPDATA%\Microsoft\Windows\INetCookies\*" >nul 2>&1
echo   [OK] Done

echo [14/30] Xoa Chrome cache...
del /q/f/s "%LOCALAPPDATA%\Google\Chrome\User Data\Default\Cache\*" >nul 2>&1
del /q/f/s "%LOCALAPPDATA%\Google\Chrome\User Data\Default\Code Cache\*" >nul 2>&1
del /q/f/s "%LOCALAPPDATA%\Google\Chrome\User Data\Default\GPUCache\*" >nul 2>&1
del /q/f/s "%LOCALAPPDATA%\Google\Chrome\User Data\ShaderCache\*" >nul 2>&1
echo   [OK] Done

echo [15/30] Xoa Firefox cache...
del /q/f/s "%LOCALAPPDATA%\Mozilla\Firefox\Profiles\*\cache2\*" >nul 2>&1
echo   [OK] Done

echo [16/30] Xoa Edge cache...
del /q/f/s "%LOCALAPPDATA%\Microsoft\Edge\User Data\Default\Cache\*" >nul 2>&1
del /q/f/s "%LOCALAPPDATA%\Microsoft\Edge\User Data\Default\Code Cache\*" >nul 2>&1
echo   [OK] Done

echo.
echo ══════════════════════════════════════════════════════════════
echo   PHAN 4: DON DEP CACHE DEV TOOLS
echo ══════════════════════════════════════════════════════════════

echo [17/30] Xoa npm cache...
where npm >nul 2>&1
if %errorLevel% equ 0 (
    call npm cache clean --force >nul 2>&1
    echo   [OK] Done
) else (
    echo   [-] npm khong tim thay
)

echo [18/30] Xoa Gradle cache...
if exist "%USERPROFILE%\.gradle\caches" (
    rd /s /q "%USERPROFILE%\.gradle\caches" >nul 2>&1
    echo   [OK] Done
) else (
    echo   [-] Gradle cache khong ton tai
)

echo [19/30] Xoa Maven cache...
if exist "%USERPROFILE%\.m2\repository" (
    rd /s /q "%USERPROFILE%\.m2\repository" >nul 2>&1
    echo   [OK] Done
) else (
    echo   [-] Maven cache khong ton tai
)

echo [20/30] Xoa Android SDK cache...
if exist "%LOCALAPPDATA%\Android\Sdk\.downloadIntermediates" (
    rd /s /q "%LOCALAPPDATA%\Android\Sdk\.downloadIntermediates" >nul 2>&1
)
if exist "%USERPROFILE%\.android\cache" (
    rd /s /q "%USERPROFILE%\.android\cache" >nul 2>&1
)
echo   [OK] Done

echo [21/30] Xoa Flutter/Dart cache...
if exist "%LOCALAPPDATA%\Pub\Cache" (
    rd /s /q "%LOCALAPPDATA%\Pub\Cache" >nul 2>&1
    echo   [OK] Done
) else (
    echo   [-] Dart cache khong ton tai
)

echo [22/30] Xoa VS Code cache...
del /q/f/s "%APPDATA%\Code\Cache\*" >nul 2>&1
del /q/f/s "%APPDATA%\Code\CachedData\*" >nul 2>&1
del /q/f/s "%APPDATA%\Code\CachedExtensions\*" >nul 2>&1
del /q/f/s "%APPDATA%\Code\CachedExtensionVSIXs\*" >nul 2>&1
echo   [OK] Done

echo [23/30] Xoa NuGet cache...
if exist "%LOCALAPPDATA%\NuGet\Cache" (
    rd /s /q "%LOCALAPPDATA%\NuGet\Cache" >nul 2>&1
    echo   [OK] Done
) else (
    echo   [-] NuGet cache khong ton tai
)

echo.
echo ══════════════════════════════════════════════════════════════
echo   PHAN 5: DON DEP UNG DUNG VA HE THONG
echo ══════════════════════════════════════════════════════════════

echo [24/30] Xoa Windows Error Reports...
del /q/f/s "%LOCALAPPDATA%\Microsoft\Windows\WER\*" >nul 2>&1
del /q/f/s "%PROGRAMDATA%\Microsoft\Windows\WER\*" >nul 2>&1
echo   [OK] Done

echo [25/30] Xoa Thumbnail cache...
del /q/f/s "%LOCALAPPDATA%\Microsoft\Windows\Explorer\thumbcache_*.db" >nul 2>&1
del /q/f/s "%LOCALAPPDATA%\Microsoft\Windows\Explorer\iconcache_*.db" >nul 2>&1
echo   [OK] Done

echo [26/30] Xoa Recent files...
del /q/f/s "%APPDATA%\Microsoft\Windows\Recent\*" >nul 2>&1
del /q/f/s "%APPDATA%\Microsoft\Windows\Recent\AutomaticDestinations\*" >nul 2>&1
del /q/f/s "%APPDATA%\Microsoft\Windows\Recent\CustomDestinations\*" >nul 2>&1
echo   [OK] Done

echo [27/30] Xoa Office cache...
del /q/f/s "%LOCALAPPDATA%\Microsoft\Office\16.0\OfficeFileCache\*" >nul 2>&1
echo   [OK] Done

echo [28/30] Xoa Teams cache...
del /q/f/s "%APPDATA%\Microsoft\Teams\Cache\*" >nul 2>&1
del /q/f/s "%APPDATA%\Microsoft\Teams\blob_storage\*" >nul 2>&1
del /q/f/s "%APPDATA%\Microsoft\Teams\databases\*" >nul 2>&1
del /q/f/s "%APPDATA%\Microsoft\Teams\GPUCache\*" >nul 2>&1
del /q/f/s "%APPDATA%\Microsoft\Teams\IndexedDB\*" >nul 2>&1
del /q/f/s "%APPDATA%\Microsoft\Teams\Local Storage\*" >nul 2>&1
del /q/f/s "%APPDATA%\Microsoft\Teams\tmp\*" >nul 2>&1
echo   [OK] Done

echo [29/30] Don Recycle Bin...
rd /s /q C:\$Recycle.Bin >nul 2>&1
echo   [OK] Done

echo.
echo ══════════════════════════════════════════════════════════════
echo   PHAN 6: TOI UU HOA HE THONG
echo ══════════════════════════════════════════════════════════════

echo [30/30] Chay Windows Disk Cleanup (Sageset)...
:: Cấu hình và chạy Disk Cleanup với tất cả các tùy chọn
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Active Setup Temp Folders" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Downloaded Program Files" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Internet Cache Files" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Old ChkDsk Files" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Recycle Bin" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Setup Log Files" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\System error memory dump files" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\System error minidump files" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Temporary Files" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Thumbnail Cache" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Update Cleanup" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Windows Defender" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Windows Error Reporting Archive Files" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Windows Error Reporting Queue Files" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1
reg add "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Explorer\VolumeCaches\Windows Upgrade Log Files" /v StateFlags0100 /t REG_DWORD /d 2 /f >nul 2>&1

cleanmgr /sagerun:100 >nul 2>&1
echo   [OK] Done

echo.
echo ╔══════════════════════════════════════════════════════════════╗
echo ║              HOAN THANH DON DEP SAU!                         ║
echo ║              Ket thuc: %date% %time%               ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.
echo [TIP] De giai phong them dung luong:
echo   1. Go lenh: Dism.exe /online /Cleanup-Image /StartComponentCleanup
echo   2. Go lenh: Dism.exe /online /Cleanup-Image /SPSuperseded
echo   3. Vao Settings ^> Apps de go cai dat ung dung khong can thiet
echo.
echo Nhan phim bat ky de dong...
pause >nul
