@echo off
echo ============================================
echo       DON DEP CACHE DEV TOOLS
echo ============================================
echo.

:: Xoa npm cache
echo [1/6] Dang xoa npm cache...
npm cache clean --force 2>nul

:: Xoa Gradle cache
echo [2/6] Dang xoa Gradle cache...
rd /s /q "%USERPROFILE%\.gradle\caches" 2>nul

:: Xoa Maven cache
echo [3/6] Dang xoa Maven cache...
rd /s /q "%USERPROFILE%\.m2\repository" 2>nul

:: Xoa Android Studio cache
echo [4/6] Dang xoa Android Studio cache...
rd /s /q "%USERPROFILE%\.android\cache" 2>nul
rd /s /q "%LOCALAPPDATA%\Google\AndroidStudio*\caches" 2>nul

:: Xoa Flutter/Dart pub cache
echo [5/6] Dang xoa Flutter pub cache...
rd /s /q "%LOCALAPPDATA%\Pub\Cache" 2>nul

:: Xoa VS Code cache
echo [6/6] Dang xoa VS Code cache...
rd /s /q "%APPDATA%\Code\Cache" 2>nul
rd /s /q "%APPDATA%\Code\CachedData" 2>nul
rd /s /q "%APPDATA%\Code\CachedExtensions" 2>nul

echo.
echo ============================================
echo       HOAN THANH DON DEP DEV CACHE!
echo ============================================
echo.
echo LUU Y: Cac dependency can tai lai khi build!
pause
