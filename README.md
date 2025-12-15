<div align="center">

# 🌟 MAGIC ENGLISH - Ứng Dụng Học Tiếng Anh Thông Minh

<img src="https://img.shields.io/badge/Spring%20Boot-3.5.8-brightgreen?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot">
<img src="https://img.shields.io/badge/Flutter-3.0+-blue?style=for-the-badge&logo=flutter&logoColor=white" alt="Flutter">
<img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
<img src="https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
<img src="https://img.shields.io/badge/AI-Spring%20AI-purple?style=for-the-badge&logo=openai&logoColor=white" alt="AI">

### 🚀 Nền tảng học tiếng Anh hiện đại với công nghệ AI

[Tính năng](#-tính-năng-chính) •
[Cấu trúc](#-cấu-trúc-dự-án) •
[Cài đặt](#-hướng-dẫn-cài-đặt) •
[Đóng góp](#-quy-tắc-commit) •
[Thành viên](#-phân-công-nhiệm-vụ)

</div>

---

## 📖 Giới Thiệu Dự Án

**Magic English** là một ứng dụng học tiếng Anh thông minh, kết hợp công nghệ **AI** và **Machine Learning** để mang đến trải nghiệm học tập cá nhân hóa và hiệu quả.

### 🎯 Mục Tiêu

- ✨ Cung cấp trải nghiệm học tiếng Anh **tương tác** và **sinh động**
- 🤖 Ứng dụng **AI** để phân tích và đánh giá khả năng học viên
- 📱 Giao diện **thân thiện**, dễ sử dụng trên mobile
- 🎓 Cá nhân hóa lộ trình học tập theo **năng lực từng người**
- 🏆 Hệ thống **thành tích** và **động lực** học tập

### 💡 Công Nghệ Sử Dụng

<div align="center">

| Backend           | Frontend         | Database       | AI/ML            |
| ----------------- | ---------------- | -------------- | ---------------- |
| Spring Boot 3.5.8 | Flutter 3.0+     | MySQL 8.0      | Spring AI        |
| Spring Security   | Dart             | JPA/Hibernate  | OpenAI API       |
| OAuth2            | Provider Pattern | Caffeine Cache | Text Recognition |
| RESTful API       | Material Design  | -              | Google ML Kit    |

</div>

---

## 🏗️ Cấu Trúc Dự Án

```
BTL/
│
├── 📂 Backend/magic-english/              # 🔧 Backend - Spring Boot API
│   ├── 📂 src/main/java/vn/project/magic_english/
│   │   ├── 📁 config/                     # ⚙️ Cấu hình Spring (Security, CORS, Cache)
│   │   ├── 📁 controller/                 # 🎮 REST Controllers - Xử lý HTTP requests
│   │   ├── 📁 model/                      # 📊 Entities & DTOs - Models dữ liệu
│   │   ├── 📁 repository/                 # 💾 Data Access Layer - JPA Repositories
│   │   ├── 📁 service/                    # 💼 Business Logic - Services & AI Integration
│   │   ├── 📁 utils/                      # 🛠️ Utilities - Helper classes
│   │   └── 📄 MagicEnglishApplication.java # 🚀 Main Application Entry Point
│   │
│   ├── 📂 src/main/resources/
│   │   ├── 📄 application.yaml            # ⚙️ Cấu hình ứng dụng (DB, AI, Security)
│   │   └── 📁 templates/                  # 📧 Email templates (nếu có)
│   │
│   ├── 📂 public/                         # 🖼️ Static files (avatars, achievements)
│   │   ├── 📁 avatar/                     # 👤 User avatars
│   │   └── 📁 achievement/                # 🏆 Achievement icons
│   │
│   ├── 📄 build.gradle                    # 📦 Dependency management
│   ├── 📄 database_schema.sql             # 🗄️ Database schema
│   └── 📄 gradlew / gradlew.bat           # 🔨 Gradle wrapper scripts
│
├── 📂 Frontend-Moblie/magic_enlish/       # 📱 Mobile App - Flutter
│   ├── 📂 lib/
│   │   ├── 📄 main.dart                   # 🚀 App Entry Point
│   │   │
│   │   ├── 📁 core/                       # 🎯 Core functionality
│   │   │   ├── constants/                 # 📌 App constants (colors, strings, URLs)
│   │   │   ├── themes/                    # 🎨 App themes & styling
│   │   │   ├── utils/                     # 🛠️ Helper functions & utilities
│   │   │   └── routes/                    # 🗺️ Navigation & routing
│   │   │
│   │   ├── 📁 data/                       # 💾 Data Layer
│   │   │   ├── models/                    # 📊 Data models & entities
│   │   │   ├── repositories/              # 🔄 Data repositories
│   │   │   └── services/                  # 🌐 API services & HTTP clients
│   │   │
│   │   ├── 📁 features/                   # 🎯 Feature Modules (theo chức năng)
│   │   │   ├── auth/                      # 🔐 Authentication (Login, Register)
│   │   │   ├── home/                      # 🏠 Home screen
│   │   │   ├── lessons/                   # 📚 Lessons & learning content
│   │   │   ├── practice/                  # ✍️ Practice exercises
│   │   │   ├── vocabulary/                # 📖 Vocabulary management
│   │   │   ├── grammar/                   # 📝 Grammar lessons
│   │   │   ├── achievements/              # 🏆 Achievements & progress
│   │   │   └── profile/                   # 👤 User profile
│   │   │
│   │   └── 📁 providers/                  # 🔄 State Management (Provider pattern)
│   │       ├── auth_provider.dart         # 🔐 Authentication state
│   │       ├── lesson_provider.dart       # 📚 Lesson state
│   │       └── user_provider.dart         # 👤 User data state
│   │
│   ├── 📂 android/                        # 🤖 Android specific config
│   ├── 📂 ios/                            # 🍎 iOS specific config
│   ├── 📂 web/                            # 🌐 Web support (optional)
│   ├── 📄 pubspec.yaml                    # 📦 Flutter dependencies
│   └── 📄 analysis_options.yaml           # 🔍 Dart analyzer configuration
│
├── 📂 Frontend-Web/                       # 🌐 Web Admin Panel (tương lai)
│
└── 📄 README.md                           # 📖 Documentation (file này)
```

---

## 📋 Nhiệm Vụ Các Thư Mục

### 🔧 Backend (Spring Boot)

| Thư mục         | Mô tả             | Trách nhiệm                                          |
| --------------- | ----------------- | ---------------------------------------------------- |
| **config/**     | Cấu hình hệ thống | Security, CORS, JWT, Cache, OpenAI integration       |
| **controller/** | API Endpoints     | Nhận requests, validate, gọi services, trả responses |
| **model/**      | Data Models       | Entities (JPA), DTOs, Request/Response objects       |
| **repository/** | Database Access   | JPA Repositories, Custom queries                     |
| **service/**    | Business Logic    | Core logic, AI integration, data processing          |
| **utils/**      | Tiện ích          | Validators, formatters, helpers, constants           |

#### 🔑 Chức Năng Backend Chính:

- ✅ RESTful API cho mobile app
- 🔐 Authentication & Authorization (OAuth2 + JWT)
- 🤖 Tích hợp AI (Spring AI + OpenAI/Gemini)
- 💾 Quản lý dữ liệu người dùng, bài học, từ vựng
- 📊 Theo dõi tiến độ và thành tích
- ⚡ Caching với Caffeine

### 📱 Frontend Mobile (Flutter)

| Thư mục        | Mô tả            | Trách nhiệm                             |
| -------------- | ---------------- | --------------------------------------- |
| **core/**      | Core App         | Constants, themes, utilities, routing   |
| **data/**      | Data Layer       | Models, repositories, API services      |
| **features/**  | Features         | UI screens, widgets theo từng chức năng |
| **providers/** | State Management | Quản lý state với Provider pattern      |

#### 🎯 Chức Năng Mobile Chính:

- 📱 Giao diện người dùng (Material Design)
- 🔐 Đăng nhập / Đăng ký
- 📚 Hiển thị bài học và nội dung
- ✍️ Các bài tập thực hành
- 📖 Quản lý từ vựng cá nhân
- 🏆 Hệ thống thành tích và tiến độ
- 📸 OCR - Nhận diện chữ từ ảnh (ML Kit)
- 🔊 Phát âm và nghe hiểu

---

## 🚀 Hướng Dẫn Cài Đặt

### 📋 Yêu Cầu Hệ Thống

- ☕ **Java JDK 21+**
- 🐘 **MySQL 8.0+**
- 📱 **Flutter SDK 3.0+**
- 🔧 **Gradle 8.0+**
- 🎯 **Android Studio** hoặc **VS Code**

### 🔧 Cài Đặt Backend

```bash
# 1. Di chuyển vào thư mục backend
cd Backend/magic-english

# 2. Cấu hình database trong application.yaml
# Sửa username, password, database name

# 3. Chạy schema SQL
mysql -u root -p < database_schema.sql

# 4. Build project
gradlew clean build

# 5. Chạy ứng dụng
gradlew bootRun
```

Backend sẽ chạy tại: `http://localhost:8080`

### 📱 Cài Đặt Mobile App

```bash
# 1. Di chuyển vào thư mục mobile
cd Frontend-Moblie/magic_enlish

# 2. Cài đặt dependencies
flutter pub get

# 3. Chạy app (Android)
flutter run

# 4. Build APK
flutter build apk --release
```

---

## 🎨 Quy Tắc Commit

### 📝 Format Commit Message

```
[<prefix>] <type>(<scope>): <message>
```

### 🏷️ Prefixes

| Prefix  | Ý nghĩa                 | Ví dụ                                        |
| ------- | ----------------------- | -------------------------------------------- |
| `[BE]`  | Backend changes         | `[BE] feat(auth): add JWT authentication`    |
| `[FE]`  | Frontend/Mobile changes | `[FE] fix(login): fix validation error`      |
| `[DB]`  | Database changes        | `[DB] update: add user_progress table`       |
| `[DOC]` | Documentation           | `[DOC] update: improve README structure`     |
| `[ALL]` | Ảnh hưởng cả BE và FE   | `[ALL] refactor: update API response format` |

### 📦 Types

| Type       | Mô tả             | Khi nào dùng                   |
| ---------- | ----------------- | ------------------------------ |
| `feat`     | Tính năng mới     | Thêm feature, API mới          |
| `fix`      | Sửa bug           | Fix lỗi, bug                   |
| `refactor` | Tái cấu trúc code | Cải thiện code không đổi logic |
| `style`    | Style code        | Format, indent, whitespace     |
| `docs`     | Documentation     | Cập nhật README, comments      |
| `test`     | Testing           | Thêm/sửa tests                 |
| `perf`     | Performance       | Cải thiện hiệu suất            |
| `chore`    | Maintenance       | Update dependencies, configs   |
| `build`    | Build system      | Gradle, pubspec changes        |

### 🎯 Scopes (Phạm vi)

**Backend:**

- `auth`, `user`, `lesson`, `vocabulary`, `ai`, `cache`, `security`, `config`

**Frontend:**

- `login`, `home`, `lesson`, `practice`, `profile`, `achievement`, `ui`

### ✨ Ví Dụ Commits Chuẩn

```bash
# Backend
[BE] feat(auth): implement OAuth2 login with Google
[BE] fix(lesson): resolve null pointer in getLessonById
[BE] refactor(service): optimize AI prompt generation
[BE] perf(cache): add Caffeine cache for user data

# Frontend
[FE] feat(login): add biometric authentication
[FE] fix(ui): correct alignment on lesson cards
[FE] style(home): update color scheme to match design
[FE] test(auth): add unit tests for login validation

# Database
[DB] update: add indexes for performance optimization
[DB] migrate: create achievement_tracking table

# Documentation
[DOC] update: add API documentation
[DOC] fix: correct installation steps

# Multiple areas
[ALL] feat(api): update user response format
[ALL] refactor: standardize error handling
```

### ✅ Quy Tắc Viết Commit

1. ✅ **Dùng tiếng Anh** cho message
2. ✅ **Ngắn gọn, rõ ràng** (max 72 characters cho subject)
3. ✅ **Imperative mood** ("add" thay vì "added")
4. ✅ **Lowercase** cho type và scope
5. ✅ **Không dấu chấm** ở cuối subject
6. ✅ **Chi tiết hơn** trong body nếu cần (optional)

### ❌ Commits Nên Tránh

```bash
❌ update code
❌ fix bug
❌ wip
❌ asdasd
❌ Sửa lỗi đăng nhập (không dùng tiếng Việt)
❌ [BE] Added new feature (sai format - dùng "add" thay vì "added")
```

---

## 👥 Phân Công Nhiệm Vụ

<div align="center">

### 🎯 Team Members & Responsibilities

</div>

| 👤 Thành Viên          | 🎯 Vai Trò                    | 📋 Nhiệm Vụ Chính                                                                                          | 📧 Email | 🔗 GitHub   |
| ---------------------- | ----------------------------- | ---------------------------------------------------------------------------------------------------------- | -------- | ----------- |
| **[Tên thành viên 1]** | 👑 Team Leader / Backend Lead | • Quản lý dự án & phân công<br>• Backend architecture<br>• Spring Security & OAuth2<br>• Database design   | [email]  | [@username] |
| **[Tên thành viên 2]** | 🤖 AI/ML Engineer             | • Tích hợp Spring AI<br>• OpenAI/Gemini API<br>• ML Kit text recognition<br>• AI features development      | [email]  | [@username] |
| **[Tên thành viên 3]** | 📱 Mobile Lead                | • Flutter app architecture<br>• UI/UX implementation<br>• State management (Provider)<br>• Mobile features | [email]  | [@username] |
| **[Tên thành viên 4]** | 💾 Backend Developer          | • RESTful API development<br>• Service layer logic<br>• Database optimization<br>• Caching implementation  | [email]  | [@username] |
| **[Tên thành viên 5]** | 🎨 Frontend Developer         | • Mobile UI components<br>• Responsive design<br>• User experience<br>• Testing                            | [email]  | [@username] |
| **[Tên thành viên 6]** | 🧪 QA/Tester                  | • Testing strategy<br>• Unit & Integration tests<br>• Bug tracking<br>• Documentation                      | [email]  | [@username] |

### 📊 Sprint Planning

| Sprint       | Timeline | Focus Areas                                                               |
| ------------ | -------- | ------------------------------------------------------------------------- |
| **Sprint 1** | Week 1-2 | • Setup project<br>• Database schema<br>• Basic authentication            |
| **Sprint 2** | Week 3-4 | • Core API development<br>• Mobile UI foundation<br>• Basic features      |
| **Sprint 3** | Week 5-6 | • AI integration<br>• Advanced features<br>• Testing                      |
| **Sprint 4** | Week 7-8 | • Bug fixes<br>• Performance optimization<br>• Final testing & deployment |

---

## 📞 Liên Hệ & Đóng Góp

<div align="center">

### 💬 Kênh Giao Tiếp

[![Discord](https://img.shields.io/badge/Discord-Join%20us-7289DA?style=for-the-badge&logo=discord&logoColor=white)](your-discord-link)
[![Slack](https://img.shields.io/badge/Slack-Chat-4A154B?style=for-the-badge&logo=slack&logoColor=white)](your-slack-link)
[![Email](https://img.shields.io/badge/Email-Contact-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:your-email@example.com)

### 📚 Tài Liệu Tham Khảo

- 📖 [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- 📱 [Flutter Documentation](https://flutter.dev/docs)
- 🤖 [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- 🎨 [Material Design Guidelines](https://material.io/design)

---

### ⭐ Nếu thấy dự án hữu ích, hãy cho chúng tôi một Star nhé!

**Made with ❤️ by Magic English Team**

</div>
