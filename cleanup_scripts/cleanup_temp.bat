@echo off
echo ============================================
echo       DON DEP FILE TAM THOI (TEMP)
echo ============================================
echo.

:: Xoa thu muc Temp cua User
echo [1/4] Dang xoa %TEMP%...
del /q/f/s "%TEMP%\*" 2>nul
rd /s /q "%TEMP%" 2>nul
mkdir "%TEMP%" 2>nul

:: Xoa thu muc Temp cua Windows
echo [2/4] Dang xoa C:\Windows\Temp...
del /q/f/s "C:\Windows\Temp\*" 2>nul
rd /s /q "C:\Windows\Temp" 2>nul
mkdir "C:\Windows\Temp" 2>nul

:: Xoa Prefetch (can quyen Admin)
echo [3/4] Dang xoa Prefetch...
del /q/f/s "C:\Windows\Prefetch\*" 2>nul

:: Xoa Recent files
echo [4/4] Dang xoa Recent files...
del /q/f/s "%APPDATA%\Microsoft\Windows\Recent\*" 2>nul

echo.
echo ============================================
echo       HOAN THANH DON DEP FILE TAM!
echo ============================================
pause
