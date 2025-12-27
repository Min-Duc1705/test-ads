@echo off
echo ============================================
echo       DON DEP CACHE WINDOWS
echo ============================================
echo.
echo LUU Y: Can chay voi quyen Administrator!
echo.

:: Xoa Windows Update Cache
echo [1/5] Dang xoa Windows Update Cache...
net stop wuauserv 2>nul
del /q/f/s "C:\Windows\SoftwareDistribution\Download\*" 2>nul
net start wuauserv 2>nul

:: Xoa Thumbnail Cache
echo [2/5] Dang xoa Thumbnail Cache...
del /q/f/s "%LOCALAPPDATA%\Microsoft\Windows\Explorer\thumbcache_*.db" 2>nul

:: Xoa Icon Cache
echo [3/5] Dang xoa Icon Cache...
del /q/f "%LOCALAPPDATA%\IconCache.db" 2>nul
del /q/f/s "%LOCALAPPDATA%\Microsoft\Windows\Explorer\iconcache_*.db" 2>nul

:: Xoa Windows Error Reports
echo [4/5] Dang xoa Windows Error Reports...
del /q/f/s "C:\ProgramData\Microsoft\Windows\WER\*" 2>nul

:: Xoa Delivery Optimization Files
echo [5/5] Dang xoa Delivery Optimization Files...
del /q/f/s "C:\Windows\ServiceProfiles\NetworkService\AppData\Local\Microsoft\Windows\DeliveryOptimization\*" 2>nul

echo.
echo ============================================
echo       HOAN THANH DON DEP CACHE!
echo ============================================
pause
