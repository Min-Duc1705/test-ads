# ============================================
# Script Dọn Dẹp Ổ C - Windows Cleanup Script
# Chạy với quyền Administrator để có hiệu quả tốt nhất
# ============================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SCRIPT DỌN DẸP Ổ C TỰ ĐỘNG" -ForegroundColor Cyan
Write-Host "  Bắt đầu lúc: $(Get-Date)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Lưu dung lượng trước khi dọn
$beforeCleanup = (Get-PSDrive C).Free
Write-Host "Dung lượng trống trước khi dọn: $([math]::Round($beforeCleanup/1GB, 2)) GB" -ForegroundColor Yellow
Write-Host ""

# 1. Xóa thư mục Temp của Windows
Write-Host "[1/10] Đang xóa Windows Temp files..." -ForegroundColor Green
Remove-Item -Path "$env:windir\Temp\*" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "  ✓ Hoàn thành" -ForegroundColor DarkGreen

# 2. Xóa thư mục Temp của User
Write-Host "[2/10] Đang xóa User Temp files..." -ForegroundColor Green
Remove-Item -Path "$env:TEMP\*" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "  ✓ Hoàn thành" -ForegroundColor DarkGreen

# 3. Xóa Prefetch files
Write-Host "[3/10] Đang xóa Prefetch files..." -ForegroundColor Green
Remove-Item -Path "$env:windir\Prefetch\*" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "  ✓ Hoàn thành" -ForegroundColor DarkGreen

# 4. Xóa cache của npm
Write-Host "[4/10] Đang xóa npm cache..." -ForegroundColor Green
if (Get-Command npm -ErrorAction SilentlyContinue) {
    npm cache clean --force 2>$null
    Write-Host "  ✓ Hoàn thành" -ForegroundColor DarkGreen
} else {
    Write-Host "  - npm không được cài đặt, bỏ qua" -ForegroundColor DarkYellow
}

# 5. Xóa cache của Gradle
Write-Host "[5/10] Đang xóa Gradle cache..." -ForegroundColor Green
$gradleCachePath = "$env:USERPROFILE\.gradle\caches"
if (Test-Path $gradleCachePath) {
    Remove-Item -Path "$gradleCachePath\*" -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "  ✓ Hoàn thành" -ForegroundColor DarkGreen
} else {
    Write-Host "  - Gradle cache không tồn tại, bỏ qua" -ForegroundColor DarkYellow
}

# 6. Xóa Windows Update cache
Write-Host "[6/10] Đang xóa Windows Update cache..." -ForegroundColor Green
Stop-Service -Name wuauserv -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$env:windir\SoftwareDistribution\Download\*" -Recurse -Force -ErrorAction SilentlyContinue
Start-Service -Name wuauserv -ErrorAction SilentlyContinue
Write-Host "  ✓ Hoàn thành" -ForegroundColor DarkGreen

# 7. Xóa Thumbnail cache
Write-Host "[7/10] Đang xóa Thumbnail cache..." -ForegroundColor Green
Remove-Item -Path "$env:LOCALAPPDATA\Microsoft\Windows\Explorer\thumbcache_*.db" -Force -ErrorAction SilentlyContinue
Write-Host "  ✓ Hoàn thành" -ForegroundColor DarkGreen

# 8. Xóa Recycle Bin
Write-Host "[8/10] Đang dọn Recycle Bin..." -ForegroundColor Green
Clear-RecycleBin -Force -ErrorAction SilentlyContinue
Write-Host "  ✓ Hoàn thành" -ForegroundColor DarkGreen

# 9. Xóa Recent files
Write-Host "[9/10] Đang xóa Recent files..." -ForegroundColor Green
Remove-Item -Path "$env:APPDATA\Microsoft\Windows\Recent\*" -Force -ErrorAction SilentlyContinue
Write-Host "  ✓ Hoàn thành" -ForegroundColor DarkGreen

# 10. Xóa log files cũ
Write-Host "[10/10] Đang xóa log files cũ..." -ForegroundColor Green
Remove-Item -Path "$env:windir\Logs\*" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$env:LOCALAPPDATA\Temp\*" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "  ✓ Hoàn thành" -ForegroundColor DarkGreen

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

# Tính toán dung lượng đã giải phóng
$afterCleanup = (Get-PSDrive C).Free
$freedSpace = $afterCleanup - $beforeCleanup
Write-Host "Dung lượng trống sau khi dọn: $([math]::Round($afterCleanup/1GB, 2)) GB" -ForegroundColor Yellow
Write-Host "Đã giải phóng: $([math]::Round($freedSpace/1MB, 2)) MB" -ForegroundColor Magenta
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  HOÀN THÀNH DỌN DẸP!" -ForegroundColor Green
Write-Host "  Kết thúc lúc: $(Get-Date)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Nhấn Enter để đóng..." -ForegroundColor Gray
Read-Host
