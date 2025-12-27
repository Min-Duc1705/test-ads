@echo off
echo ============================================
echo       DON DEP TOAN BO O C
echo ============================================
echo.
echo Script nay se don dep:
echo   - File tam thoi (Temp, Prefetch)
echo   - Cache Windows (Update, Thumbnail, Icon)
echo   - Cache trinh duyet (Chrome, Edge, Firefox)
echo   - Thung rac
echo.
echo LUU Y: 
echo   - Can chay voi quyen Administrator
echo   - Hay dong tat ca trinh duyet truoc khi chay
echo.
echo Nhan phim bat ky de bat dau...
pause >nul

echo.
echo ============================================
echo [BUOC 1/5] DON DEP FILE TAM THOI
echo ============================================
:: Xoa thu muc Temp cua User
del /q/f/s "%TEMP%\*" 2>nul
rd /s /q "%TEMP%" 2>nul
mkdir "%TEMP%" 2>nul

:: Xoa thu muc Temp cua Windows
del /q/f/s "C:\Windows\Temp\*" 2>nul

:: Xoa Prefetch
del /q/f/s "C:\Windows\Prefetch\*" 2>nul

:: Xoa Recent files
del /q/f/s "%APPDATA%\Microsoft\Windows\Recent\*" 2>nul

echo Hoan thanh!

echo.
echo ============================================
echo [BUOC 2/5] DON DEP CACHE WINDOWS
echo ============================================
:: Xoa Windows Update Cache
net stop wuauserv 2>nul
del /q/f/s "C:\Windows\SoftwareDistribution\Download\*" 2>nul
net start wuauserv 2>nul

:: Xoa Thumbnail Cache
del /q/f/s "%LOCALAPPDATA%\Microsoft\Windows\Explorer\thumbcache_*.db" 2>nul

:: Xoa Icon Cache
del /q/f "%LOCALAPPDATA%\IconCache.db" 2>nul

:: Xoa Windows Error Reports
del /q/f/s "C:\ProgramData\Microsoft\Windows\WER\*" 2>nul

echo Hoan thanh!

echo.
echo ============================================
echo [BUOC 3/5] DON DEP CACHE TRINH DUYET
echo ============================================
:: Chrome
rd /s /q "%LOCALAPPDATA%\Google\Chrome\User Data\Default\Cache" 2>nul
rd /s /q "%LOCALAPPDATA%\Google\Chrome\User Data\Default\Code Cache" 2>nul

:: Edge
rd /s /q "%LOCALAPPDATA%\Microsoft\Edge\User Data\Default\Cache" 2>nul
rd /s /q "%LOCALAPPDATA%\Microsoft\Edge\User Data\Default\Code Cache" 2>nul

:: Firefox
rd /s /q "%LOCALAPPDATA%\Mozilla\Firefox\Profiles\*.default*\cache2" 2>nul

echo Hoan thanh!

echo.
echo ============================================
echo [BUOC 4/5] LAM TRONG THUNG RAC
echo ============================================
rd /s /q C:\$Recycle.Bin 2>nul
echo Hoan thanh!

echo.
echo ============================================
echo [BUOC 5/5] CHAY DISK CLEANUP
echo ============================================
echo Dang khoi dong Disk Cleanup...
cleanmgr /sagerun:1

echo.
echo ============================================
echo       HOAN THANH DON DEP TOAN BO!
echo ============================================
echo.
echo Hay khoi dong lai may tinh de ap dung thay doi.
echo.
pause
