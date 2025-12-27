# 🧹 Script Dọn Dẹp Ổ C - Windows

Tập hợp các script batch để dọn dẹp và giải phóng dung lượng ổ C trên Windows.

## 📁 Danh sách Script

| Script                      | Mô tả                                                             | Cần Admin |
| --------------------------- | ----------------------------------------------------------------- | --------- |
| `cleanup_temp.bat`          | Xóa file tạm thời (Temp, Prefetch, Recent)                        | ✅        |
| `cleanup_windows_cache.bat` | Xóa cache Windows (Update, Thumbnail, Icon, Error Reports)        | ✅        |
| `cleanup_browser_cache.bat` | Xóa cache trình duyệt (Chrome, Edge, Firefox, Opera)              | ❌        |
| `cleanup_recycle_bin.bat`   | Làm trống thùng rác                                               | ✅        |
| `cleanup_dev_cache.bat`     | Xóa cache dev tools (npm, Gradle, Maven, Android Studio, Flutter) | ❌        |
| `cleanup_all.bat`           | Chạy tất cả các bước dọn dẹp                                      | ✅        |

## 🚀 Hướng dẫn sử dụng

### Cách 1: Chạy từng script riêng lẻ

1. Click chuột phải vào file `.bat` muốn chạy
2. Chọn **Run as administrator** (nếu cần quyền Admin)
3. Làm theo hướng dẫn trên màn hình

### Cách 2: Chạy script tổng hợp

1. Click chuột phải vào `cleanup_all.bat`
2. Chọn **Run as administrator**
3. Đợi script hoàn thành tất cả các bước

## ⚠️ Lưu ý quan trọng

1. **Đóng tất cả trình duyệt** trước khi chạy `cleanup_browser_cache.bat`
2. **Chạy với quyền Administrator** để script có thể xóa các file hệ thống
3. **Backup dữ liệu quan trọng** trước khi chạy (đề phòng)
4. Với `cleanup_dev_cache.bat`:
   - Các dependency (npm, Gradle, Maven) sẽ cần tải lại khi build
   - Chỉ chạy khi cần giải phóng nhiều dung lượng

## 📊 Dung lượng có thể giải phóng

| Loại cache      | Dung lượng ước tính |
| --------------- | ------------------- |
| Temp files      | 500MB - 5GB         |
| Windows Update  | 1GB - 10GB          |
| Browser cache   | 500MB - 3GB         |
| Thumbnail cache | 100MB - 500MB       |
| Dev tools cache | 2GB - 20GB          |
| Recycle Bin     | Tùy thuộc           |

## 🔧 Mẹo thêm

Ngoài các script này, bạn có thể:

1. **Disk Cleanup nâng cao**:

   ```
   cleanmgr /sageset:1
   ```

   (Chọn các mục muốn xóa, sau đó chạy `cleanmgr /sagerun:1`)

2. **Xóa System Restore cũ**:

   - Vào System Properties > System Protection > Configure > Delete

3. **Tắt Hibernation** (tiết kiệm = RAM size):

   ```
   powercfg -h off
   ```

4. **Kiểm tra dung lượng ổ đĩa**:
   - Vào Settings > System > Storage
