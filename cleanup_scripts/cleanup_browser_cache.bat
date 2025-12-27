@echo off
echo ============================================
echo       DON DEP CACHE TRINH DUYET
echo ============================================
echo.
echo LUU Y: Hay dong tat ca trinh duyet truoc khi chay!
echo.
pause

:: Xoa Chrome Cache
echo [1/4] Dang xoa Google Chrome Cache...
rd /s /q "%LOCALAPPDATA%\Google\Chrome\User Data\Default\Cache" 2>nul
rd /s /q "%LOCALAPPDATA%\Google\Chrome\User Data\Default\Code Cache" 2>nul
rd /s /q "%LOCALAPPDATA%\Google\Chrome\User Data\Default\GPUCache" 2>nul

:: Xoa Edge Cache
echo [2/4] Dang xoa Microsoft Edge Cache...
rd /s /q "%LOCALAPPDATA%\Microsoft\Edge\User Data\Default\Cache" 2>nul
rd /s /q "%LOCALAPPDATA%\Microsoft\Edge\User Data\Default\Code Cache" 2>nul
rd /s /q "%LOCALAPPDATA%\Microsoft\Edge\User Data\Default\GPUCache" 2>nul

:: Xoa Firefox Cache
echo [3/4] Dang xoa Mozilla Firefox Cache...
rd /s /q "%LOCALAPPDATA%\Mozilla\Firefox\Profiles\*.default*\cache2" 2>nul

:: Xoa Opera Cache
echo [4/4] Dang xoa Opera Cache...
rd /s /q "%APPDATA%\Opera Software\Opera Stable\Cache" 2>nul

echo.
echo ============================================
echo       HOAN THANH DON DEP BROWSER!
echo ============================================
pause
