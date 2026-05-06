# VBoard - Android Native App

Ứng dụng giao tiếp hỗ trợ (AAC) cho người Việt Nam.

## Cài đặt

### Yêu cầu
- Android Studio Hedgehog (2023.1.1) hoặc mới hơn
- JDK 17+
- Android SDK 26+ (Android 8 trở lên)

### Các bước cài đặt

1. **Mở project trong Android Studio**
   ```
   File > Open > chọn thư mục VBoardAAC_Android
   ```

2. **Sync Gradle**
   - Android Studio sẽ tự động sync khi mở project
   - Nếu không, vào `File > Sync Project with Gradle Files`

3. **Build và chạy**
   ```
   Run > Run 'app'
   ```
   Hoặc sử dụng phím tắt: `Shift + F10`

## Cấu trúc Project

```
VBoardAAC_Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/vboard/aac/
│   │   │   ├── data/
│   │   │   │   ├── model/          # Data models
│   │   │   │   └── repository/      # Data persistence
│   │   │   └── ui/
│   │   │       ├── main/           # Main board screen
│   │   │       ├── admin/          # Admin hub
│   │   │       ├── edit/           # Edit mode
│   │   │       ├── pin/            # PIN verification
│   │   │       ├── settings/       # Settings
│   │   │       ├── stats/          # Statistics
│   │   │       ├── uiconfig/       # UI settings
│   │   │       └── voicetest/      # Voice settings
│   │   └── res/
│   │       ├── drawable/          # Drawables
│   │       ├── layout/            # Layouts
│   │       └── values/           # Colors, strings, themes
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Tính năng

- **Màn hình Giao tiếp**: Lưới từ vựng với emoji, thanh ghép câu
- **PHÁT ÂM**: Đọc câu đã ghép bằng TTS tiếng Việt
- **Xác minh PIN**: Bảo vệ bằng mã PIN 4 số
- **Chế độ chỉnh sửa**: Thêm, sửa, xóa thẻ
- **Cài đặt giao diện**: Chọn số cột (2, 3, 4)
- **Thống kê**: Theo dõi số câu và từ đã dùng

## Mã PIN mặc định

```
1234
```

## Thiết kế

Sử dụng **"The Gentle Path"** design system:
- Màu chính: Vàng Gold (#705d00 / #ffd700)
- Màu phụ: Xanh lá (#006e1c / #91f78e)
- Bo góc lớn (24dp-48dp)
- Font: Hệ thống (Roboto)

## Build APK

```bash
./gradlew assembleDebug
```

APK sẽ được tạo tại:
```
app/build/outputs/apk/debug/app-debug.apk
```
