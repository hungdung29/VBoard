# VBoard AAC — Kế hoạch Triển khai Chi tiết
**Phiên bản:** 1.1 | **Ngày:** 2026-05-03
**Trạng thái:** ✅ Phase 1 Sprint 1–2 đã triển khai — chạy `gradlew assembleDebug`
**Công nghệ:** Native Android (Kotlin 1.9 + Java 17, Gradle 8.7, AGP 8.2.2, Hilt 2.50, Room 2.6.1, DataStore 1.0.0)
**Min SDK:** 26 (Android 8.0 Oreo) | **Target SDK:** 34
**Design System:** "The Gentle Path" (Zen-Editorial, Lumina v5)

---

## Mục lục

1. [Tổng quan Đề xuất Kiến trúc](#1-tổng-quan-đề-xuất-kiến-trúc)
2. [Yêu cầu Hệ thống](#2-yêu-cầu-hệ-thống)
3. [Phân tích Chức năng & Use Cases Chi tiết](#3-phân-tích-chức-năng--use-cases-chi-tiết)
4. [Thiết kế Kiến trúc Ứng dụng](#4-thiết-kế-kiến-trúc-ứng-dụng)
5. [Thiết kế Cơ sở Dữ liệu](#5-thiết-kế-cơ-sở-dữ-liệu)
6. [Luồng UI/UX — Thứ tự Hoạt động](#6-luồng-uiux--thứ-tự-hoạt-động)
7. [Mô tả Từng Màn hình (Screens)](#7-mô-tả-từng-màn-hình-screens)
8. [Kiến trúc Module & Package Layout](#8-kiến-trúc-module--package-layout)
9. [Danh sách Tính năng & Mức Ưu tiên Triển khai](#9-danh-sách-tính-năng--mức-ưu-tiên-triển-khai)
10. [Lộ trình Triển khai (Sprint Roadmap)](#10-lộ-trình-triển-khai-sprint-roadmap)
11. [Thiết kế API & Data Layer](#11-thiết-kế-api--data-layer)
12. [Text-to-Speech & Voice Cloning](#12-text-to-speech--voice-cloning)
13. [Accessibility & Quy chuẩn Thiết kế](#13-accessibility--quy-chuẩn-thiết-kế)
14. [Kiểm thử & Đảm bảo Chất lượng](#14-kiểm-thử--đảm-bảo-chất-lượng)
15. [Phụ lục](#15-phụ-lục)

---

## 1. Tổng quan Đề xuất Kiến trúc

### 1.1 Mô hình Kiến trúc Chọn: MVVM + Clean Architecture

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│  (Activities / Fragments / ViewModels / Adapters)│
├─────────────────────────────────────────────────┤
│                 Domain Layer                      │
│     (Use Cases / Entities / Repository Interfaces)│
├─────────────────────────────────────────────────┤
│                  Data Layer                       │
│  (Repository Implementations / Room / SharedPrefs)│
├─────────────────────────────────────────────────┤
│                Platform Layer                     │
│    (Android APIs / TTS / Camera / File I/O)      │
└─────────────────────────────────────────────────┘
```

**Lý do chọn MVVM + Clean Architecture:**
- **Tách biệt rõ ràng** giữa UI, logic nghiệp vụ, và truy xuất dữ liệu — dễ bảo trì, dễ kiểm thử.
- **ViewModel** sống sót qua configuration change (xoay màn hình), không reset trạng thái người dùng.
- **Repository Pattern** ẩn đi nguồn dữ liệu (Room DB / SharedPreferences / Remote API), dễ thay đổi sau này.
- **Use Cases** độc lập, có thể kiểm thử đơn vị mà không cần Activity.

### 1.2 Stack Công nghệ Chi tiết

| Tầng | Thành phần | Phiên bản | Ghi chú |
|---|---|---|---|
| Ngôn ngữ | Kotlin | 1.9.24 | Ưu tiên Kotlin, Java cho JNI/callback |
| Ngôn ngữ | Java | 17 | CameraX callbacks, legacy integration |
| Build | Gradle | 8.7 | Wrapper, Kotlin DSL |
| Build | AGP | 8.5.2 | Android Gradle Plugin |
| UI | Android Views + XML | SDK 35 | Data Binding bật để layout type-safe |
| UI | Material Design 3 | 1.12.0 | Material Components for Android |
| Navigation | Jetpack Navigation | 2.7.7 | Single Activity, Fragment-based |
| DI | Hilt | 2.51.1 | Dependency Injection |
| Async | Kotlin Coroutines | 1.8.1 | ViewModelScope, IO dispatcher |
| Database | Room | 2.6.1 | SQLite abstraction |
| Preferences | DataStore Preferences | 1.1.1 | Thay SharedPreferences |
| TTS | Android TTS + Voice Compatibility | Native API | Vietnamese (vi-VN) |
| Voice Cloning | Coqui XTTS v2 | REST API / Local | Mô-đun tương lai |
| Image Loading | Coil | 2.6.0 | Kotlin-first, coroutine-native |
| Camera | CameraX | 1.3.4 | Chụp ảnh thẻ từ vựng |
| Animation | Lottie | 6.4.0 | Animations phản hồi chạm thẻ |
| Testing | JUnit 5 + Espresso | 4.13.2 / 3.5.1 | Unit + UI tests |

---

## 2. Yêu cầu Hệ thống

### 2.1 Yêu cầu Chức năng (Functional Requirements)

| Mã | Mô tả | Ưu tiên | Loại |
|---|---|---|---|
| FR-01 | Hiển thị lưới thẻ từ vựng (Big Grid) trên màn hình chính | Must | Core |
| FR-02 | Chọn danh mục để lọc thẻ hiển thị | Must | Core |
| FR-03 | Ghép câu: chạm thẻ → thêm vào Sentence Strip | Must | Core |
| FR-04 | Phát âm toàn bộ câu qua TTS khi bấm nút PHÁT ÂM | Must | Core |
| FR-05 | Xóa từ cuối (Backspace) hoặc xóa toàn bộ câu | Must | Core |
| FR-06 | Rào cản PIN (math challenge) chặn truy cập Admin Hub | Must | Core |
| FR-07 | Quản lý thẻ: Thêm, Sửa, Xóa thẻ từ vựng | Must | Admin |
| FR-08 | Quản lý danh mục/thư mục: Tạo, Sửa, Xóa, Đổi màu | Must | Admin |
| FR-09 | Cài đặt giao diện: chế độ Sáng/Tối, số cột lưới (2/3/4), cỡ chữ | Must | Admin |
| FR-10 | Cài đặt giọng đọc: âm lượng, loại giọng, Voice Cloning | Should | Admin |
| FR-11 | Thống kê sử dụng: số câu/ngày, từ phổ biến, biểu đồ | Should | Admin |
| FR-12 | Chụp ảnh thực tế để gán vào thẻ | Should | Admin |
| FR-13 | Voice Cloning: ghi âm 30s → tạo Voice Profile | Won't (MVP) | Future |
| FR-14 | Tái cấu trúc thẻ bằng Drag & Drop | Won't (MVP) | Future |

### 2.2 Yêu cầu Phi chức năng (Non-Functional Requirements)

| Mã | Mô tả | Ngưỡng |
|---|---|---|
| NFR-01 | Thời gian khởi tạo ứng dụng (Cold start) | < 2 giây |
| NFR-02 | Thời gian phản hồi khi chạm thẻ | < 100ms |
| NFR-03 | Thời gian phát TTS sau khi bấm PHÁT ÂM | < 300ms |
| NFR-04 | Touch target tối thiểu cho thẻ từ vựng | 48×48dp |
| NFR-05 | Hỗ trợ Android 8.0 (API 26) trở lên | API 26+ |
| NFR-06 | Hỗ trợ thiết bị có RAM 2GB trở lên | RAM ≥ 2GB |
| NFR-07 | Offline-first: tất cả chức năng hoạt động không cần Internet | 100% offline |
| NFR-08 | Kích thước APK sau khi build (debug) | < 30MB |
| NFR-09 | Tỷ lệ crash trên main thread | 0% |
| NFR-10 | Hỗ trợ đa ngôn ngữ (tiếng Việt mặc định, dễ mở rộng) | i18n-ready |

### 2.3 Device Profile Mục tiêu

- **Chính:** Tablet Android 7"–12" (đối tượng AAC chính — tư thế cố định, màn hình lớn)
- **Phụ:** Smartphone Android 5"–6.7"
- **Orientation:** Portrait mặc định; Landscape hỗ trợ tốt

---

## 3. Phân tích Chức năng & Use Cases Chi tiết

### 3.1 Sơ đồ Use Cases Toàn hệ thống

```
                        ┌─────────────────────────────────────────┐
                        │          Hệ thống VBoard AAC           │
                        └─────────────────────────────────────────┘
                                          │
            ┌──────────────────────────────┼──────────────────────────────┐
            │                              │                              │
            ▼                              ▼                              ▼
    ┌───────────────┐             ┌───────────────┐             ┌───────────────┐
    │  Người dùng   │             │   Phụ huynh  │             │  Người hỗ trợ │
    │   (Trẻ em)    │             │              │             │               │
    └───────────────┘             └───────────────┘             └───────────────┘
            │                              │                              │
            │                              │                              │
     ┌──────┴──────┐                ┌──────┴──────┐                ┌──────┴──────┐
     │ UC-01 Ghép  │                │ UC-03 PIN    │                │ UC-03 PIN    │
     │     câu     │                │   Anti-escape│                │   Anti-escape│
     ├─────────────┤                ├─────────────┤                ├─────────────┤
     │ UC-02 Xóa/ │                │ UC-04 Quản lý│                │ UC-04 Quản lý│
     │  Sửa câu   │                │   Thẻ từ vựng│                │   Thẻ từ vựng│
     └─────────────┘                ├─────────────┤                ├─────────────┤
                                    │ UC-05 Voice │                │ UC-06 UI    │
                                    │  Cloning    │                │  Scaling    │
                                    ├─────────────┤                ├─────────────┤
                                    │ UC-06 UI    │                │ UC-07 Thống │
                                    │  Scaling    │                │   kê        │
                                    ├─────────────┤                └─────────────┘
                                    │ UC-07 Thống │
                                    │   kê        │
                                    └─────────────┘
```

### 3.2 UC-01: Ghép câu giao tiếp cơ bản

**Actor:** Trẻ em / Người dùng AAC
**Mô tả:** Người dùng chạm vào các thẻ từ vựng trên lưới để xây dựng câu giao tiếp và phát âm.

**Pre-condition:** Ứng dụng đang mở ở Board Screen.

**Luồng chính (Main Flow):**
1. Người dùng nhìn thấy lưới thẻ 2D tĩnh (Big Grid, không viền cứng).
2. Hệ thống hiển thị 8 danh mục (tab ngang) ở dưới thanh câu.
3. Người dùng chạm vào danh mục (ví dụ: "Ăn uống") để lọc thẻ.
4. Hệ thống lọc và hiển thị chỉ thẻ thuộc danh mục đó.
5. Người dùng chạm vào thẻ "Con" → phát hiệu ứng ripple + haptic feedback (50ms, amplitude default).
6. Hệ thống tạo `SentenceItem` mới, thêm vào `sentenceItems` list.
7. Hệ thống cập nhật Sentence Strip: hiển thị chip từ với animation slide-in từ phải qua.
8. Người dùng chạm "Con" → chip nhảy lên → chạm "Muốn" → chip nhảy lên → chạm "Nước" → chip nhảy lên.
9. Người dùng chạm nút **PHÁT ÂM** (loa khổng lồ ở bottom bar).
10. Hệ thống nối các từ: "Con muốn nước" → gọi TTS speak().
11. TTS phát âm câu tiếng Việt với speech rate 0.4.

**Luồng phụ (Alternative Flow A — sai thẻ):**
5a. Người dùng chạm nhầm thẻ "Bánh" thay vì "Nước".
5b. Người dùng chạm nút ⌫ (Backspace) trên Sentence Strip.
5c. Hệ thống xóa chip cuối cùng khỏi `sentenceItems`, cập nhật strip.
5d. Quay lại bước 9.

**Luồng phụ (Alternative Flow B — xóa toàn bộ):**
5c'. Người dùng chạm nút **XÓA** màu đỏ ở bottom bar.
5d'. Hệ thống clear toàn bộ `sentenceItems`, hiển thị placeholder.

**Post-condition:**
- `sentenceItems` chứa danh sách từ đã chọn.
- TTS đã phát xong câu (hoặc đang phát).
- Thống kê `recordWordUsage()` đã được ghi.

**Data thao tác:**
- Entity: `SentenceItem`, `VocabCard`
- Repository: `VocabRepository.recordWordUsage(word)`, `VocabRepository.recordSentence()`

---

### 3.3 UC-02: Xóa / Sửa câu đang ghép

**Actor:** Trẻ em / Người dùng AAC
**Mô tả:** Sửa lỗi ghép sai bằng backspace hoặc xóa toàn bộ.

**Luồng chính:**
1. Người dùng nhấn nút ⌫ (Backspace) trên Sentence Strip.
2. Hệ thống kiểm tra `sentenceItems` không rỗng.
3. Hệ thống xóa phần tử cuối cùng, cập nhật UI.
4. Chip từ biến mất với animation fade-out.

**Luồng phụ:**
1'. Người dùng nhấn nút XÓA (toàn bộ).
2'. Hệ thống clear list, hiển thị placeholder.

---

### 3.4 UC-03: Rào cản Anti-escape (PIN + Math Challenge)

**Actor:** Phụ huynh / Người hỗ trợ
**Mô tả:** Bảo vệ khu vực Admin Hub bằng hộp thoại PIN có bàn phím số to và câu hỏi toán cộng/trừ.

**Pre-condition:** Người dùng đang ở Board Screen, chạm vào biểu tượng ⚙️.

**Luồng chính:**
1. Hệ thống hiển thị `PinActivity` dưới dạng dialog modal (nền mờ overlay).
2. Hệ thống hiển thị câu hỏi toán (ví dụ: "4 + 5 = ?") và bàn phím số Numpad 3×4 (các nút 80×80dp).
3. Người dùng nhập 4 số (PIN thực).
4. Hệ thống kiểm tra PIN qua `VocabRepository.verifyPin(pin)`.
5. ĐÚNG: Chuyển sang `AdminActivity`. PIN modal đóng lại.
6. SAI: Rung nhẹ (VibrationEffect 100ms), hiển thị text "Sai mã PIN, thử lại", xóa input.

**Biến thể (Math Challenge):**
- Câu hỏi được sinh ngẫu nhiên: phép cộng 2 số (1–9) hoặc trừ 2 số (kết quả dương).
- Mục đích: trẻ em không thể tính nhẩm được → không thể truy cập Admin.

**Post-condition:**
- Đúng: `PinActivity` đóng, `AdminActivity` mở.
- Sai: PIN input reset, user có thể thử lại không giới hạn.

---

### 3.5 UC-04: Quản lý Thẻ & Từ vựng (CRUD)

**Actor:** Phụ huynh / Người hỗ trợ
**Mô tả:** Thêm thẻ mới với ảnh thực tế, sửa từ/nhóm, xóa thẻ.

**Pre-condition:** Đã vượt qua PIN (UC-03).

**Luồng chính (Thêm thẻ):**
1. Phụ huynh ở `EditActivity`, bấm FAB (+) màu xanh lá.
2. Hệ thống hiển thị dialog "Thêm thẻ mới": gồm khung hình vuông (camera icon), text field "Nhập từ", dropdown "Chọn nhóm".
3. Phụ huynh bấm nút Camera → hệ thống mở `CameraX` để chụp ảnh.
4. Hệ thống crop ảnh tỷ lệ 1:1, lưu vào `app_internal_storage/vocab_images/`.
5. Phụ huynh nhập "Cốc mới", chọn nhóm "Đồ vật", bấm **Lưu**.
6. Hệ thống tạo `VocabCard` mới với `id = UUID`, `localImagePath = saved_path`, `isCustom = true`.
7. Hệ thống ghi vào Room DB → thẻ xuất hiện ngay trên Board Screen.

**Luồng Sửa thẻ:**
1. Phụ huynh chạm icon "Bút chì" trên thẻ trong `EditActivity`.
2. Hệ thống hiển thị dialog Edit với thông tin hiện tại.
3. Phụ huynh sửa từ → **Lưu** → cập nhật Room DB.

**Luồng Xóa thẻ:**
1. Phụ huynh chạm icon "Thùng rác" trên thẻ.
2. Hệ thống hiển thị dialog xác nhận: "Bạn có chắc muốn xóa thẻ này?".
3. Phụ huynh bấm **Đồng ý** → xóa khỏi Room DB, xóa file ảnh.

**Quản lý Danh mục (Folders):**
1. Phụ huynh ở tab "Thư mục" trong `EditActivity`.
2. Bấm FAB (+) → nhập tên, chọn màu từ bảng màu.
3. Thư mục mới tạo → có thể kéo thẻ vào bằng long-press + drag.

---

### 3.6 UC-05: Voice Cloning (AI)

**Actor:** Phụ huynh
**Mô tả:** Ghi âm giọng của ba/mẹ một lần, hệ thống tạo Voice Profile và áp dụng cho TTS.

**Trạng thái:** **GIAI ĐOẠN 2** — MVP không bao gồm. Thiết kế sẵn interface.

**Interface thiết kế (Design for Future):**
1. `VoiceSettingsActivity` có nút "Tạo giọng người thân (AI Voice)".
2. Hệ thống hiển thị đoạn văn mẫu tiếng Việt (~30 giây đọc).
3. Phụ huynh bấm ghi âm → dùng `MediaRecorder` thu âm.
4. File âm thanh được upload hoặc xử lý local qua Coqui XTTS v2.
5. Voice Profile lưu vào `internal_storage/voice_profiles/`.
6. TTS sử dụng voice profile làm pitch/rate override.

---

### 3.7 UC-06: Điều chỉnh Giao diện (UI Scaling)

**Actor:** Phụ huynh
**Mô tả:** Thay đổi cỡ lưới, chế độ Sáng/Tối, cỡ chữ.

**Luồng chính:**
1. Phụ huynh vào `UISettingsActivity` từ Admin Hub.
2. **Cỡ lưới:** Slider với 3 mức: Nhỏ (4 cột), Vừa (3 cột — mặc định), Lớn (2 cột).
3. **Chế độ:** Toggle Sáng / Tối (Material You dark theme).
4. **Cỡ chữ:** Slider từ 14sp → 24sp.
5. Mỗi thay đổi preview ngay trên màn hình (reactive UI).
6. **Lưu:** `DataStore` ghi lại preference.

**Tác động:**
- `gridColumns` → `GridLayoutManager.spanCount` thay đổi, `RecyclerView` re-render.
- Dark mode → `AppCompatDelegate.setDefaultNightMode()`.
- Font size → `resources.configuration.fontScale` hoặc custom `TextView` với `setTextSize()`.

---

### 3.8 UC-07: Thống kê Sử dụng

**Actor:** Phụ huynh
**Mô tả:** Xem số câu đã tạo hôm nay, từ phổ biến, biểu đồ.

**Data hiển thị:**
- Số câu đã phát âm hôm nay (`getSentencesToday()`).
- Số từ vựng đã sử dụng hôm nay (`getUniqueWordsCount()`).
- Top 4 từ phổ biến nhất (`getTopWords(4)`).
- Biểu đồ cột đơn giản (thủ công Canvas hoặc MPAndroidChart).

---

## 4. Thiết kế Kiến trúc Ứng dụng

### 4.1 Sơ đồ Kiến trúc Chi tiết (Layered Architecture)

```
  ┌─────────────────────────────────────────────────────────┐
  │                    PRESENTATION LAYER                     │
  │                                                          │
  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐│
  │  │MainActivity│  │PinActivity│  │AdminActivity│ │EditActivity││
  │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘│
  │       │             │             │             │       │
  │  ┌────▼─────────────▼─────────────▼─────────────▼─────┐│
  │  │              ViewModels (Hilt-injected)               ││
  │  │ BoardVM │ PinVM │ AdminVM │ EditVM │ SettingsVM │   ││
  │  │ StatsVM │ VoiceVM │                                ││
  │  └──────────────────────┬───────────────────────────────┘│
  │                         │ Coroutines + Flow              │
  ├─────────────────────────┼─────────────────────────────────┤
  │                  DOMAIN LAYER                             │
  │                                                          │
  │  ┌─────────────────┐  ┌─────────────────────────────────┐│
  │  │  Use Cases /    │  │         Entities / Models       ││
  │  │  Interactors    │  │  VocabCard │ Category │ Sentence ││
  │  │                 │  │  VoiceProfile │ AppSettings     ││
  │  │ AddCardUseCase  │  │  DailyStats │ WordUsage         ││
  │  │ DeleteCardUseCase│  │                                 ││
  │  │ SpeakSentenceUseCase│                                  ││
  │  │ VerifyPinUseCase│  │  Repository Interfaces:          ││
  │  │ UpdateUISettings│  │  IVocabRepository                ││
  │  │ RecordStatsUseCase│ │  ISettingsRepository            ││
  │  │                 │  │  IStatsRepository                ││
  │  └─────────────────┘  └─────────────────────────────────┘│
  │                                                          │
  ├──────────────────────────────────────────────────────────┤
  │                    DATA LAYER                            │
  │                                                          │
  │  ┌─────────────────┐  ┌─────────────────┐  ┌────────────┐│
  │  │ Room Database   │  │ DataStore Prefs │  │ File Storage││
  │  │ VBoardDatabase  │  │  AppDataStore   │  │ImageStorage ││
  │  │ VocabCardDao    │  │  dark_mode      │  │VocabularyImages││
  │  │ CategoryDao     │  │  grid_columns   │  │VoiceProfiles  ││
  │  │                 │  │  pin_code       │  │               ││
  │  │                 │  │  voice_settings │  │               ││
  │  └────────┬────────┘  └────────┬────────┘  └──────┬─────┘│
  │           │                      │                   │      │
  │           └──────────────┬───────┘                   │      │
  │                          │                           │      │
  │              ┌───────────▼───────────┐               │      │
  │              │ VocabRepositoryImpl   │◄──────────────┘      │
  │              │ SettingsRepositoryImpl│                     │
  │              │ StatsRepositoryImpl   │                     │
  │              └───────────────────────┘                     │
  │                                                          │
  ├──────────────────────────────────────────────────────────┤
  │                  PLATFORM LAYER                           │
  │  AndroidTTS │ CameraX │ MediaPlayer │ HapticManager       │
  │  Vibrator │ ActivityResultContracts │ FileProvider        │
  └──────────────────────────────────────────────────────────┘
```

### 4.2 Navigation Architecture (Single Activity)

```
MainActivity (Host)
 │
 ├── BoardFragment (startDestination)
 │    └── sentenceStrip + vocabGrid + categoryTabs
 │
 ├── PinDialogFragment (modal dialog)
 │    └── math challenge + numpad
 │
 └── AdminNavGraph
      ├── AdminHubFragment
      ├── EditFragment (vocab management)
      │    └── AddCardDialog / EditCardDialog / FolderDialog
      ├── UISettingsFragment
      ├── VoiceSettingsFragment
      └── StatsFragment
```

**Navigation Graph (XML):**
- `nav_graph.xml` chứa toàn bộ destinations.
- Safe Args plugin tạo `BoardFragmentArgs`, `EditFragmentArgs` type-safe.
- Bottom navigation KHÔNG có — vì anti-escape: chỉ có một con đường ra Admin là qua PIN.

### 4.3 Dependency Injection (Hilt)

```kotlin
// Application-scoped
@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides fun provideDatabase(@ApplicationContext ctx: Context) =
        Room.databaseBuilder(ctx, VBoardDatabase::class.java, "vboard.db").build()

    @Provides fun provideVocabDao(db: VBoardDatabase) = db.vocabCardDao()
    @Provides fun provideCategoryDao(db: VBoardDatabase) = db.categoryDao()
}

// Activity-scoped
@Module @InstallIn(ActivityComponent::class)
object ActivityModule {
    @Provides fun provideTTS(@ApplicationContext ctx: Context) =
        TextToSpeech(ctx) { /* init callback */ }
}

// ViewModel injection via @HiltViewModel
@HiltViewModel
class BoardViewModel @Inject constructor(
    private val vocabRepo: IVocabRepository,
    private val settingsRepo: ISettingsRepository,
    private val statsRepo: IStatsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() { ... }
```

---

## 5. Thiết kế Cơ sở Dữ liệu

### 5.1 Room Database Schema

```sql
-- Entity: vocab_cards
CREATE TABLE vocab_cards (
    id TEXT PRIMARY KEY,          -- UUID, e.g. "uuid-xxx"
    word TEXT NOT NULL,           -- Tiếng Việt, e.g. "Con"
    category_id TEXT NOT NULL,     -- FK → categories.id
    image_url TEXT,               -- URL từ xa (nullable)
    local_image_path TEXT,        -- Đường dẫn file ảnh cục bộ
    is_custom INTEGER NOT NULL DEFAULT 0,  -- 0=default, 1=custom
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL, -- Unix timestamp (ms)
    updated_at INTEGER NOT NULL   -- Unix timestamp (ms)
);

-- Entity: categories
CREATE TABLE categories (
    id TEXT PRIMARY KEY,          -- "cat-1", "cat-2", ...
    name TEXT NOT NULL,           -- "Gia đình", "Ăn uống"
    icon TEXT NOT NULL,           -- Emoji: "👨‍👩‍👧"
    color TEXT NOT NULL,          -- Hex: "#FF6B6B"
    display_order INTEGER NOT NULL DEFAULT 0
);

-- Entity: word_usage (daily aggregation)
CREATE TABLE word_usage (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word TEXT NOT NULL,
    usage_date TEXT NOT NULL,     -- yyyy-MM-dd
    count INTEGER NOT NULL DEFAULT 0,
    UNIQUE(word, usage_date)
);

-- Entity: daily_stats
CREATE TABLE daily_stats (
    date TEXT PRIMARY KEY,        -- yyyy-MM-dd
    sentences_count INTEGER NOT NULL DEFAULT 0,
    unique_words INTEGER NOT NULL DEFAULT 0
);

-- Entity: voice_profiles (future)
CREATE TABLE voice_profiles (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,           -- "Giọng mẹ", "Giọng ba"
    file_path TEXT NOT NULL,      -- Path đến audio file
    is_default INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
);

-- Indexes
CREATE INDEX idx_cards_category ON vocab_cards(category_id);
CREATE INDEX idx_usage_date ON word_usage(usage_date);
```

### 5.2 Room DAO Interfaces

```kotlin
@Dao
interface VocabCardDao {
    @Query("SELECT * FROM vocab_cards ORDER BY display_order ASC")
    fun getAllCards(): Flow<List<VocabCardEntity>>

    @Query("SELECT * FROM vocab_cards WHERE category_id = :categoryId ORDER BY display_order ASC")
    fun getCardsByCategory(categoryId: String): Flow<List<VocabCardEntity>>

    @Query("SELECT * FROM vocab_cards WHERE id = :id")
    suspend fun getCardById(id: String): VocabCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: VocabCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<VocabCardEntity>)

    @Update
    suspend fun updateCard(card: VocabCardEntity)

    @Delete
    suspend fun deleteCard(card: VocabCardEntity)

    @Query("DELETE FROM vocab_cards WHERE id = :id")
    suspend fun deleteCardById(id: String)

    @Query("SELECT COUNT(*) FROM vocab_cards")
    suspend fun getCardCount(): Int
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY display_order ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsByDate(date: String): DailyStatsEntity?

    @Query("SELECT * FROM word_usage WHERE usage_date = :date ORDER BY count DESC LIMIT :limit")
    fun getTopWords(date: String, limit: Int): Flow<List<WordUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: DailyStatsEntity)

    @Query("UPDATE daily_stats SET sentences_count = sentences_count + 1 WHERE date = :date")
    suspend fun incrementSentenceCount(date: String)

    @Transaction
    suspend fun recordWordUsage(word: String, date: String) {
        // Upsert word_usage, update daily_stats unique_words
    }
}
```

### 5.3 DataStore Preferences Schema

```kotlin
// File: preferences_data_store.kt
object PreferencesKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val GRID_COLUMNS = intPreferencesKey("grid_columns")
    val SHOW_LABELS = booleanPreferencesKey("show_labels")
    val PIN_CODE = stringPreferencesKey("pin_code")
    val VOICE_VOLUME = floatPreferencesKey("voice_volume")
    val VOICE_TYPE = stringPreferencesKey("voice_type")
    val FONT_SCALE = floatPreferencesKey("font_scale")
    val ACTIVE_VOICE_PROFILE_ID = stringPreferencesKey("active_voice_profile_id")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val LAST_STATS_DATE = stringPreferencesKey("last_stats_date")
}

// Default values:
// dark_mode = false
// grid_columns = 3
// show_labels = true
// pin_code = "1234"
// voice_volume = 1.0f
// voice_type = "nam-bac"
// font_scale = 1.0f
```

---

## 6. Luồng UI/UX — Thứ tự Hoạt động

Các màn hình được thiết kế trong `stitch_vboard_aac_mobile/` và thứ tự điều hướng như sau:

### 6.1 Thứ tự Màn hình (Navigation Order)

```
  [1] Board Screen (Mặc định, Entry Point)
         │
         │ (Tap Settings ⚙️)
         ▼
  [2] PIN Verification Screen (Modal Dialog)
         │
         │ (PIN Correct)
         ▼
  [3] Admin Hub Screen (Central Dashboard)
         │
         ├──► [4] Vocabulary Management / Edit Screen
         │         │
         │         └──► [4a] Add Card Dialog
         │         └──► [4b] Edit Card Dialog
         │         └──► [4c] Folder Management Dialog
         │
         ├──► [5] Voice Settings Screen
         │         │
         │         └──► [5a] Voice Cloning Flow (Future)
         │
         ├──► [6] UI Settings Screen
         │         │
         │         └──► Grid / Font / Theme controls
         │
         └──► [7] Usage Statistics Screen
                   └──► [7a] Daily Stats + Top Words
```

### 6.2 Screen-by-Screen Navigation Description

| Bước | Màn hình | File thiết kế Stitch | Mô tả luồng vào/ra |
|---|---|---|---|
| 1 | **Board Screen** | `Giao tiếp chính/` | Entry point. Người dùng tương tác chính. Ra: tap ⚙️ → PIN |
| 2 | **PIN Verification** | `Xác minh mã pin/` | Modal overlay từ Board. Đúng → Admin Hub. Sai → rung + reset |
| 3 | **Admin Hub** | *(menu trong settings)* | Dashboard 4 card. Ra: tap card → màn hình tương ứng. Back → Board |
| 4 | **Vocabulary Management** | `Chế độ chỉnh sửa và quản lý thư mục/` | Danh sách thẻ + FAB. Ra: tap + → Add Dialog, tap edit → Edit Dialog |
| 4a | **Add Card Dialog** | `Hộp thoại thêm thẻ từ vụng/` | Camera + text field + category dropdown. Save → quay lại list |
| 5 | **Voice Settings** | `Cài đặt giọng nói/` | Volume slider + voice type dropdown + preview. Save → quay Admin Hub |
| 6 | **UI Settings** | `Cài đặt giao diện/` | Grid columns (2/3/4) + Dark mode toggle + font size. Live preview |
| 7 | **Usage Statistics** | `Thống kê sử dụng từ vựng/` | Bar chart + top words + sentences/day. Read-only |
| — | **System Settings** | `Cài đặt hệ thống/` | App version, reset data, export/import (mở rộng) |

### 6.3 Gesture & Interaction Map

| Hành động | Vị trí | Phản hồi |
|---|---|---|
| Tap thẻ từ | Board Grid | Ripple + haptic 50ms + chip nhảy lên Sentence Strip |
| Long-press thẻ | Edit Mode | Hiện edit/delete overlay |
| Swipe ngang | Category Tabs | Cuộn tab danh mục |
| Tap nút Loa | Sentence Strip / Bottom Bar | TTS phát âm câu |
| Tap nút ⌫ | Sentence Strip | Xóa chip cuối (fade out) |
| Tap nút Xóa | Bottom Bar | Clear toàn bộ câu + placeholder |
| Tap ⚙️ | Board Header | Hiện PIN modal |
| Tap FAB (+) | Edit Screen | Hiện Add Card dialog |
| Drag & Drop | Edit Screen (Folder) | Di chuyển thẻ vào thư mục |

---

## 7. Mô tả Từng Màn hình (Screens)

### 7.1 Board Screen (`MainActivity`)

**Layout (ConstraintLayout):**
```
┌──────────────────────────────────────────┐
│  [⚙️]     VBoard AAC       [👤 Avatar]  │  ← Header (56dp)
├──────────────────────────────────────────┤
│  ┌────────────────────────────────────┐  │
│  │ [Con] [Muốn] [Nước]          [⌫]  │  │  ← Sentence Strip (CardView, 120dp min)
│  │ "Nhấn thẻ để xây dựng câu..."     │  │
│  └────────────────────────────────────┘  │
├──────────────────────────────────────────┤
│  [📋Tất cả] [👨‍👩‍👧Gia đình] [🍎Ăn uống] ... │  ← Category Tabs (56dp)
├──────────────────────────────────────────┤
│  ┌─────┐ ┌─────┐ ┌─────┐               │
│  │ 💧  │ │ 🍚  │ │ 🍰  │               │
│  │Nước │ │Cơm  │ │Bánh │               │  ← Vocab Grid (RecyclerView, GridLayout)
│  ├─────┤ ├─────┤ ├─────┤               │     3 cột × N hàng (2/3/4 configurable)
│  │ 🥛  │ │ 🍎  │ │ 🥩  │               │
│  │Sữa  │ │Tr. cây│ │Thịt │               │
│  └─────┘ └─────┘ └─────┘               │
│                                          │
│  ┌─────┐ ┌─────┐ ┌─────┐               │
│  │...  │ │...  │ │...  │               │
│                                          │
├──────────────────────────────────────────┤
│  [ 🔊 PHÁT ÂM  ]  [ 🗑 XÓA  ]          │  ← Bottom Bar (80dp, pill buttons)
└──────────────────────────────────────────┘
```

**Thành phần chi tiết:**
- **Sentence Strip:** CardView bo góc 16dp (radius_xl), nền warm paper `#faf9f7`, có HorizontalScrollView bên trong. Placeholder italic hiển thị khi trống.
- **Category Tabs:** HorizontalScrollView chứa chip dạng LinearLayout với icon (emoji) + text. Active chip có nền primary color, inactive có nền surface variant.
- **Vocab Grid:** RecyclerView với `GridLayoutManager`, mỗi item là CardView bo góc 16dp. Chứa emoji/icon + text label. Item touch target ≥ 48×48dp.
- **Bottom Bar:** LinearLayout horizontal, 2 pill-shaped MaterialButton. PHÁT ÂM: secondary container color. XÓA: error color.
- **Haptic Feedback:** `VibrationEffect.createOneShot(50, DEFAULT_AMPLITUDE)` sau mỗi tap.

### 7.2 PIN Verification Screen (`PinActivity`)

**Layout:**
```
┌──────────────────────────────────────────┐
│           🔒 Xác minh mã PIN             │
│                                          │
│         Câu hỏi: 4 + 5 = ?              │  ← TextView 32sp bold
│                                          │
│           ● ● ● ○  (4 dots)             │  ← 4 indicator dots
│                                          │
│     ┌───────┬───────┬───────┐            │
│     │   1   │   2   │   3   │            │
│     ├───────┼───────┼───────┤            │  ← Numpad 3×4
│     │   4   │   5   │   6   │            │     Button size: 80×80dp
│     ├───────┼───────┼───────┤            │     Font: 32sp bold
│     │   7   │   8   │   9   │            │
│     ├───────┼───────┼───────┤            │
│     │   ✕   │   0   │   ⌫   │            │
│     └───────┴───────┴───────┘            │
│                                          │
│     [Hủy]              [Vào cài đặt]    │  ← Cancel / Enter
└──────────────────────────────────────────┘
```

**Logic:**
- Math challenge sinh ngẫu nhiên: `a = random(1..9)`, `b = random(1..9)`, `op = if (a>=b) "+" else "-"` để kết quả ≥ 0.
- PIN nhập vào = 4 chữ số. Đúng → `startActivity(AdminActivity)`, finish PinActivity.
- Sai → `Vibrator.vibrate(100ms)`, đổi dot màu đỏ, hiện text "Sai mã PIN".

### 7.3 Admin Hub Screen (`AdminActivity`)

**Layout:** GridLayout 2 cột × 2 hàng, mỗi card là `CardView` với icon + title + subtitle.

```
┌─────────────────┬─────────────────┐
│  📝 Quản lý    │  🔊 Cài đặt     │
│  Từ vựng       │  Giọng đọc      │
│  Thêm/Sửa/Xóa  │  Giọng + Volume │


│  Thẻ từ vựng   │                  │
├─────────────────┼─────────────────┤
│  🎨 Cài đặt   │  📊 Thống kê   │
│  Giao diện     │  Sử dụng từ    │
│  Lưới + Sáng/  │  Số câu hôm nay│
│  Tối + Font    │  Top từ phổ biến│
└─────────────────┴─────────────────┘
```

### 7.4 Edit / Vocabulary Management (`EditActivity`)

```
┌──────────────────────────────────────────┐
│  [←]  Quản lý Từ vựng        [+ Thêm]  │
├──────────────────────────────────────────┤
│  [Tất cả] [Gia đình] [Ăn uống] ...     │  ← Filter tabs
├──────────────────────────────────────────┤
│  ┌─────┐ ┌─────┐ ┌─────┐               │
│  │ 🖊️ │ │ 🗑️ │               │  ← Edit / Delete overlay on long-press
│  │Cốc  │ │Bánh │ │ ... │               │
│  │mới  │ │     │ │     │               │
│  └─────┘ └─────┘ └─────┘               │
│                                          │
│  [+ Thư mục] (FAB màu xanh, góc phải)  │
├──────────────────────────────────────────┤
│  [+ Tạo thẻ mới] (FAB xanh dương)      │  ← Floating Action Button
└──────────────────────────────────────────┘
```

### 7.5 Add Card Dialog

```
┌──────────────────────────────────────────┐
│          Thêm thẻ từ vựng mới           │
│                                          │
│     ┌──────────────────────────┐         │
│     │      📷 Chụp ảnh         │         │  ← Camera / Gallery button
│     │      hoặc tải lên        │         │
│     └──────────────────────────┘         │
│                                          │
│  Nhập từ: [________________]            │
│  Chọn nhóm: [Dropdown ▾]               │
│                                          │
│     [Hủy]              [Lưu thẻ]        │
└──────────────────────────────────────────┘
```

### 7.6 UI Settings (`UISettingsActivity`)

```
┌──────────────────────────────────────────┐
│  [←]  Cài đặt Giao diện                  │
├──────────────────────────────────────────┤
│                                          │
│  Cỡ lưới hiển thị                       │
│  ○ Nhỏ (4 cột)                          │
│  ● Vừa (3 cột)      ◄─── Slider ───►  │
│  ○ Lớn (2 cột)                           │
│                                          │
│  Chế độ màu                              │
│  [ 🌙 Sáng  ]  [ ☀️ Tối ]              │  ← Toggle buttons
│                                          │
│  Cỡ chữ                                   │
│  A ────────●────────────── A            │
│  14sp              1.0x            24sp │
│                                          │
│  Hiển thị nhãn chữ                       │
│  [✓] Hiển thị từ bên dưới thẻ          │
│                                          │
│  ──────────── Live Preview ───────────   │
│  ┌─────┐ ┌─────┐ ┌─────┐               │  ← Mini preview của Board
│  │Con  │ │Muốn │ │Nước │               │
│  └─────┘ └─────┘ └─────┘               │
└──────────────────────────────────────────┘
```

### 7.7 Voice Settings (`VoiceSettingsActivity`)

```
┌──────────────────────────────────────────┐
│  [←]  Cài đặt Giọng đọc                  │
├──────────────────────────────────────────┤
│                                          │
│  Âm lượng                                 │
│  🔈 ────────────●──────── 🔊             │
│                                          │
│  Loại giọng                              │
│  [Nam miền Bắc] [Nam miền Nam]          │  ← Radio group
│  [Nữ miền Bắc]  [Nữ miền Nam]           │
│                                          │
│  [ ▶ Nghe thử "Con muốn uống nước"]     │  ← Preview button
│                                          │
│  ───── Giọng nói người thân (AI) ───── │
│  [🎤 Ghi âm giọng của bạn]              │  ← Future: Voice Cloning
│   Ghi 30 giây → tạo Voice Profile        │
└──────────────────────────────────────────┘
```

### 7.8 Stats Screen (`StatsActivity`)

```
┌──────────────────────────────────────────┐
│  [←]  Thống kê sử dụng                  │
├──────────────────────────────────────────┤
│  Hôm nay                                  │
│                                          │
│   Số câu đã tạo     Từ đã dùng         │
│      📝 12             🔤 8             │  ← Two stat cards
│                                          │
│  ─────── Biểu đồ 7 ngày ────────────    │
│  │    █                                     │
│  │ █ █ █ █ █ █ █                         │  ← Bar chart (7 days)
│  │ █ █ █ █ █ █ █                         │
│  └─────────────────────────────           │
│   T2  T3  T4  T5  T6  T7  CN            │
│                                          │
│  Top từ phổ biến                         │
│  1. "Nước" (15 lần) ████████████        │
│  2. "Con"  (12 lần) ██████████          │  ← Horizontal bars
│  3. "Muốn"(10 lần) ████████             │
│  4. "Cơm"  (8 lần)  ██████              │
└──────────────────────────────────────────┘
```

---

## 8. Kiến trúc Module & Package Layout

### 8.1 Package Structure

```
com.vboard.aac/
│
├── VBoardApplication.kt              # @HiltAndroidApp
│
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── VBoardDatabase.kt     # Room database (singleton)
│   │   │   ├── dao/
│   │   │   │   ├── VocabCardDao.kt
│   │   │   │   ├── CategoryDao.kt
│   │   │   │   └── StatsDao.kt
│   │   │   └── entity/
│   │   │       ├── VocabCardEntity.kt
│   │   │       ├── CategoryEntity.kt
│   │   │       ├── WordUsageEntity.kt
│   │   │       ├── DailyStatsEntity.kt
│   │   │       └── VoiceProfileEntity.kt
│   │   ├── datastore/
│   │   │   └── AppPreferencesDataStore.kt
│   │   └── storage/
│   │       ├── ImageStorageManager.kt
│   │       └── VoiceProfileStorage.kt
│   │
│   ├── repository/
│   │   ├── VocabRepositoryImpl.kt    # Implements IVocabRepository
│   │   ├── SettingsRepositoryImpl.kt  # Implements ISettingsRepository
│   │   └── StatsRepositoryImpl.kt    # Implements IStatsRepository
│   │
│   └── mapper/
│       ├── VocabCardMapper.kt        # Entity ↔ Domain model
│       └── CategoryMapper.kt
│
├── domain/
│   ├── model/
│   │   ├── VocabCard.kt              # Domain model (pure Kotlin data class)
│   │   ├── Category.kt
│   │   ├── SentenceItem.kt
│   │   ├── WordUsage.kt
│   │   ├── DailyStats.kt
│   │   ├── VoiceProfile.kt
│   │   └── AppSettings.kt
│   │
│   ├── repository/                    # Interfaces
│   │   ├── IVocabRepository.kt
│   │   ├── ISettingsRepository.kt
│   │   └── IStatsRepository.kt
│   │
│   └── usecase/
│       ├── board/
│       │   ├── AddWordToSentenceUseCase.kt
│       │   ├── RemoveWordFromSentenceUseCase.kt
│       │   ├── SpeakSentenceUseCase.kt
│       │   └── GetFilteredCardsUseCase.kt
│       ├── admin/
│       │   ├── AddCardUseCase.kt
│       │   ├── UpdateCardUseCase.kt
│       │   ├── DeleteCardUseCase.kt
│       │   ├── AddCategoryUseCase.kt
│       │   └── VerifyPinUseCase.kt
│       └── settings/
│           ├── UpdateGridColumnsUseCase.kt
│           ├── ToggleDarkModeUseCase.kt
│           └── GetAppSettingsUseCase.kt
│
├── platform/
│   ├── tts/
│   │   ├── TextToSpeechManager.kt    # Wraps Android TTS
│   │   └── VoiceCloningManager.kt    # Future: Coqui XTTS wrapper
│   ├── camera/
│   │   └── CameraManager.kt          # CameraX integration
│   ├── audio/
│   │   └── AudioRecorderManager.kt    # MediaRecorder for voice cloning
│   └── feedback/
│       └── HapticFeedbackManager.kt  # VibrationEffect wrapper
│
├── di/
│   ├── AppModule.kt                  # @Module @InstallIn(ApplicationComponent)
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   └── UseCaseModule.kt
│
└── ui/
    ├── main/
    │   ├── MainActivity.kt            # Board Screen (Single Activity host)
    │   ├── MainFragment.kt
    │   ├── BoardViewModel.kt
    │   └── adapter/
    │       ├── VocabGridAdapter.kt    # RecyclerView adapter cho grid
    │       ├── CategoryChipAdapter.kt
    │       └── SentenceStripAdapter.kt
    │
    ├── pin/
    │   ├── PinActivity.kt            # Hoặc PinDialogFragment
    │   └── PinViewModel.kt
    │
    ├── admin/
    │   ├── AdminActivity.kt
    │   └── AdminViewModel.kt
    │
    ├── edit/
    │   ├── EditActivity.kt
    │   ├── EditViewModel.kt
    │   └── adapter/
    │       └── EditCardAdapter.kt
    │
    │   ├── dialog/
    │   │   ├── AddCardDialogFragment.kt
    │   │   ├── EditCardDialogFragment.kt
    │   │   └── ConfirmDeleteDialogFragment.kt
    │
    ├── settings/
    │   ├── ui/
    │   │   ├── UISettingsActivity.kt
    │   │   └── UISettingsViewModel.kt
    │   └── voice/
    │       ├── VoiceSettingsActivity.kt
    │       └── VoiceSettingsViewModel.kt
    │
    ├── stats/
    │   ├── StatsActivity.kt
    │   └── StatsViewModel.kt
    │
    └── common/
        ├── BaseActivity.kt
        ├── BaseViewModel.kt
        ├── ViewModelFactory.kt        # AssistedInject fallback
        └── extensions/
            ├── ContextExtensions.kt
            ├── ViewExtensions.kt
            └── LifecycleExtensions.kt
```

### 8.2 Activity vs Fragment Decision

| Screen | Type | Lý do |
|---|---|---|
| Board (Main) | Activity + Fragments | Host, cần giữ state khi PIN dialog hiện |
| PIN Verification | DialogFragment | Modal overlay, không exit nếu cancel |
| Admin Hub | Fragment | Phụ thuộc navigation graph |
| Edit/Vocab | Fragment | Có thể dùng shared ViewModel với Admin |
| UISettings | Fragment | Nhẹ, không cần lifecycle phức tạp |
| VoiceSettings | Fragment | Nhẹ |
| Stats | Fragment | Nhẹ |
| Add/Edit Dialog | DialogFragment | Modal, task-specific |

---

## 9. Danh sách Tính năng & Mức Ưu tiên Triển khai

### Phase 1 — MVP (Sprint 1–3)

| # | Tính năng | Module | Trạng thái hiện tại |
|---|---|---|---|
| 1 | Board Screen: hiển thị lưới thẻ + Sentence Strip | `ui.main` | ✅ Hoàn thành — MVVM + ViewBinding |
| 2 | Category filtering | `ui.main` | ✅ Hoàn thành — CategoryChipAdapter |
| 3 | TTS Vietnamese speak | `platform.tts` | ✅ Hoàn thành — TextToSpeechManager |
| 4 | PIN Anti-escape (math challenge) | `ui.pin` | ✅ Hoàn thành — PinViewModel |
| 5 | Admin Hub dashboard | `ui.admin` | ✅ Hoàn thành — AdminActivity |
| 6 | CRUD thẻ từ vựng | `ui.edit` | ✅ Hoàn thành — EditViewModel + EditCardAdapter |
| 7 | Room Database setup | `data.local.db` | ✅ Hoàn thành — VBoardDatabase + 3 DAO |
| 8 | DataStore Preferences | `data.local.datastore` | ✅ Hoàn thành — AppPreferencesDataStore |
| 9 | Haptic feedback chuẩn | `platform.feedback` | ✅ Hoàn thành — HapticFeedbackManager |
| 10 | UI Settings (grid/dark/font) | `ui.settings.ui` | ✅ Hoàn thành — UISettingsViewModel |
| 11 | Stats Screen | `ui.stats` | ✅ Hoàn thành — StatsViewModel |
| 12 | Voice Settings | `ui.settings.voice` | ✅ Hoàn thành — VoiceSettingsViewModel |
| 13 | Camera integration (chụp ảnh thẻ) | `platform.camera` | 🔴 Chưa có |
| 14 | ImageStorage (lưu ảnh cục bộ) | `data.local.storage` | 🔴 Chưa có |
| 15 | **Backup/Restore JSON** | `data.backup` | ✅ Hoàn thành — BackupManager + BackupActivity |

### Phase 2 — Enhanced (Sprint 4–6)

| # | Tính năng | Module |
|---|---|---|
| 15 | Folder/Category CRUD với đổi màu | `ui.edit`, `domain.usecase` |
| 16 | TTS rate/pitch control per profile | `platform.tts` |
| 17 | Voice Cloning (Coqui XTTS v2) | `platform.tts`, `platform.audio` |
| 18 | Lottie animations on card tap | `ui.main` |
| 19 | Biểu đồ thống kê (MPAndroidChart) | `ui.stats` |
| 20 | Onboarding flow lần đầu mở app | `ui.onboarding` |
| 21 | Export/Import data (JSON backup) | `data.export` |
| 22 | Keeptooltip hướng dẫn cho người dùng mới | `ui.common` |

### Phase 3 — Polish (Sprint 7–8)

| # | Tính năng |
|---|---|
| 23 | Drag & Drop sắp xếp thẻ |
| 24 | Material You dynamic colors (Android 12+) |
| 25 | Accessibility TalkBack full support |
| 26 | Multi-language (Tiếng Anh, tiếng dân tộc) |
| 27 | Cloud backup (Firebase) |

---

## 10. Lộ trình Triển khai (Sprint Roadmap)

```
Sprint 1 (2 tuần): Nền tảng + Board Screen Core
├── Setup project: Gradle, Hilt, Room, DataStore, Navigation
├── Refactor MainActivity → MVVM (BoardViewModel)
├── Implement BoardFragment + SentenceStrip
├── Implement VocabGridAdapter + CategoryChipAdapter
├── TTS Manager wrapper
├── Haptic Feedback Manager
└── Unit tests: BoardViewModel

Sprint 2 (2 tuần): PIN + Admin + Data Layer
├── Room Database + Entities + DAOs
├── Repository implementations
├── Migrate from SharedPreferences → DataStore
├── PIN Activity refactor → MVVM + math challenge
├── Admin Hub Fragment
├── Navigation graph setup
└── Unit tests: Repositories, Use Cases

Sprint 3 (2 tuần): Edit & Settings
├── EditActivity → Fragments (EditVocabularyFragment)
├── Add/Edit/Delete Card dialogs
├── CameraX integration
├── ImageStorage manager
├── UISettings Fragment + live preview
├── VoiceSettings Fragment + TTS preview
└── Instrumented tests: EditCardFlow

Sprint 4 (2 tuần): Statistics + Polish
├── StatsFragment + bar chart
├── StatsRepository → daily aggregation
├── Lottie animations on card tap
├── Dark mode full support
├── Font scaling system
├── Onboarding Fragment (first launch)
└── Espresso tests: BoardFlow, AdminFlow

Sprint 5 (2 tuần): Voice Cloning (Phase 1)
├── MediaRecorder manager
├── Voice recording UI
├── Coqui XTTS v2 integration (REST)
├── Voice profile storage
├── Apply custom voice to TTS
└── Unit tests: VoiceCloningManager

Sprint 6 (2 tuần): Polish + i18n + Accessibility
├── TalkBack labels on all interactive elements
├── i18n strings (EN, VI)
├── Export/Import JSON
├── Material You dynamic colors (Android 12+)
├── Performance: Lazy loading images
└── Full test suite (coverage > 80%)
```

---

## 11. Thiết kế API & Data Layer

### 11.1 Repository Interfaces (Domain Layer)

```kotlin
// IVocabRepository.kt
interface IVocabRepository {
    fun getAllCards(): Flow<List<VocabCard>>
    fun getCardsByCategory(categoryId: String): Flow<List<VocabCard>>
    suspend fun getCardById(id: String): VocabCard?
    suspend fun addCard(card: VocabCard)
    suspend fun updateCard(card: VocabCard)
    suspend fun deleteCard(id: String)
    fun getAllCategories(): Flow<List<Category>>
    suspend fun addCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: String)
}

// ISettingsRepository.kt
interface ISettingsRepository {
    val darkMode: Flow<Boolean>
    val gridColumns: Flow<Int>
    val showLabels: Flow<Boolean>
    val fontScale: Flow<Float>
    val pinCode: Flow<String>
    val voiceVolume: Flow<Float>
    val voiceType: Flow<String>
    val activeVoiceProfileId: Flow<String?>

    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setGridColumns(columns: Int)
    suspend fun setShowLabels(show: Boolean)
    suspend fun setFontScale(scale: Float)
    suspend fun setPinCode(code: String)
    suspend fun setVoiceVolume(volume: Float)
    suspend fun setVoiceType(type: String)
    suspend fun setActiveVoiceProfile(profileId: String?)
    suspend fun verifyPin(pin: String): Boolean
}

// IStatsRepository.kt
interface IStatsRepository {
    fun getTodayStats(): Flow<DailyStats>
    fun getTopWords(limit: Int): Flow<List<WordUsage>>
    fun getWeeklyStats(): Flow<List<DailyStats>>
    suspend fun recordSentence()
    suspend fun recordWordUsage(word: String)
}
```

### 11.2 Use Cases

```kotlin
// SpeakSentenceUseCase.kt
class SpeakSentenceUseCase @Inject constructor(
    private val ttsManager: TextToSpeechManager,
    private val statsRepo: IStatsRepository
) {
    operator fun invoke(words: List<String>) {
        val sentence = words.joinToString(" ")
        ttsManager.speak(sentence)
        words.forEach { statsRepo.recordWordUsage(it) }
        statsRepo.recordSentence()
    }
}

// VerifyPinUseCase.kt
class VerifyPinUseCase @Inject constructor(
    private val settingsRepo: ISettingsRepository
) {
    suspend operator fun invoke(pin: String): Boolean =
        settingsRepo.verifyPin(pin)
}

// AddCardUseCase.kt
class AddCardUseCase @Inject constructor(
    private val vocabRepo: IVocabRepository,
    private val imageStorage: ImageStorageManager
) {
    suspend operator fun invoke(
        word: String,
        categoryId: String,
        imagePath: String?
    ): VocabCard {
        val localPath = imagePath?.let { imageStorage.saveImage(it) }
        val card = VocabCard(
            id = UUID.randomUUID().toString(),
            word = word,
            categoryId = categoryId,
            localImagePath = localPath,
            isCustom = true,
            order = 0
        )
        vocabRepo.addCard(card)
        return card
    }
}
```

---

## 12. Text-to-Speech & Voice Cloning

### 12.1 TextToSpeechManager

```kotlin
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentLocale = Locale("vi", "VN")

    val isReady: Boolean get() = isInitialized

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        isInitialized = status == TextToSpeech.SUCCESS
        if (isInitialized) {
            tts?.language = currentLocale
            tts?.setSpeechRate(0.4f)   // Chậm rãi, rõ ràng cho AAC
            tts?.setPitch(1.0f)
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isInitialized) return
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone() { onComplete?.invoke() }
            override fun onError() { }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vboard_${System.currentTimeMillis()}")
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.25f, 2.0f))
    }

    fun setVolume(volume: Float) {
        // Android TTS không có setVolume trực tiếp
        // Giải pháp: dùng AudioManager.setStreamVolume
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
```

### 12.2 Vietnamese TTS Compatibility Matrix

| Thiết bị | Android 8.0 | Android 11 | Android 13+ | Android 15 |
|---|---|---|---|---|
| Samsung Galaxy Tab A | ✅ vi-VN | ✅ vi-VN | ✅ vi-VN | ✅ vi-VN |
| Lenovo Tab M10 | ⚠️ Fallback en | ✅ vi-VN | ✅ vi-VN | ✅ vi-VN |
| Huawei MediaPad | ❌ Không | ❌ Không | ⚠️ Cài thủ công | ⚠️ Cài thủ công |
| Xiaomi Pad 5 | ✅ vi-VN | ✅ vi-VN | ✅ vi-VN | ✅ vi-VN |

**Fallback Strategy:**
- Khi `TextToSpeech.getAvailableLanguages()` không chứa `vi-VN`:
  1. Thử `Locale("vi")` (ngôn ngữ, không vùng).
  2. Fallback sang Google TTS (nếu có) — `Intent(ACTION_TTS_SETTINGS)`.
  3. Nếu vẫn không có → hiện toast "Vui lòng cài Google Tiếng Việt TTS".

### 12.3 Voice Cloning Architecture (Design — Phase 2)

```
[Thu âm 30s]
    │
    ▼
MediaRecorderManager
    │
    ▼
Audio file (.wav, 16kHz, mono)
    │
    ├──► [Local] Coqui XTTS v2 (ONNX runtime on-device)
    │         │
    │         ▼
    │    Voice Profile JSON (speaker embedding)
    │         │
    │         ▼
    │    CustomTTSEngine (thay pitch/rate theo profile)
    │
    └──► [Cloud] REST API → XTTS v2 server → Voice Profile JSON
              │
              ▼
         VoiceProfileEntity stored in Room
              │
              ▼
         Active voice profile applied to TTS
```

---

## 13. Accessibility & Quy chuẩn Thiết kế

### 13.1 Accessibility Checklist

| Tiêu chuẩn | Chi tiết | Trạng thái |
|---|---|---|
| Touch Target | Thẻ từ vựng ≥ 48×48dp | ✅ Enforce trong grid item layout |
| Color Contrast | Tỷ lệ ≥ 4.5:1 (WCAG AA) | ✅ Kiểm tra với Material Theme token |
| Font Size | Tối thiểu 14sp, mặc định 16sp, scale được | ✅ Font scale từ 0.875x → 1.5x |
| TalkBack | `contentDescription` trên mọi ImageButton | 🔴 Cần implement |
| Directionality | Hỗ trợ RTL (tiếng Ả Rập) nếu mở rộng | 🔴 i18n infrastructure only |
| Focus Order | Duyệt thứ tự hợp lý trên tablet keyboard | 🔴 Cần kiểm thử |
| No color-only info | Icon + text label, không chỉ màu | ✅ Emoji + text label |
| Haptic feedback | Phản hồi xúc giác cho mọi tap | ✅ HapticFeedbackManager |

### 13.2 Design System: "The Gentle Path" Tokens

```xml
<!-- colors.xml -->
<resources>
    <!-- Primary -->
    <color name="gold_700">#705d00</color>
    <color name="gold_500">#c9a400</color>
    <color name="gold_200">#f0de6e</color>
    <color name="gold_50">#fffce8</color>

    <!-- Secondary (Green) -->
    <color name="green_700">#006e1c</color>
    <color name="green_500">#00a032</color>
    <color name="green_200">#91f78e</color>

    <!-- Tertiary (Warm) -->
    <color name="warm_700">#904d00</color>
    <color name="warm_200">#ffd1af</color>

    <!-- Surface (warm paper) -->
    <color name="surface_light">#faf9f7</color>
    <color name="surface_dark">#1a1a1a</color>

    <!-- Semantic -->
    <color name="on_surface_light">#1a1c1b</color>  <!-- Never pure black -->
    <color name="on_surface_dark">#e2e3e1</color>

    <!-- No-line rule: use tonal shift, NOT borders -->
    <color name="ghost_border">#1a000000</color>  <!-- 10% opacity -->
</resources>
```

```xml
<!-- dimens.xml -->
<resources>
    <!-- Touch targets (WCAG AA) -->
    <dimen name="touch_target_min">48dp</dimen>

    <!-- Card -->
    <dimen name="card_radius_xl">48dp</dimen>
    <dimen name="card_padding">16dp</dimen>
    <dimen name="card_elevation">0dp</dimen>  <!-- No shadows -->

    <!-- Button -->
    <dimen name="button_height_large">64dp</dimen>
    <dimen name="button_radius_full">999dp</dimen>  <!-- Pill shape -->

    <!-- Numpad -->
    <dimen name="numpad_button_size">80dp</dimen>
    <dimen name="numpad_text_size">32sp</dimen>

    <!-- Font sizes -->
    <dimen name="text_body">16sp</dimen>
    <dimen name="text_label">14sp</dimen>
    <dimen name="text_heading">24sp</dimen>

    <!-- Spacing (8pt grid) -->
    <dimen name="spacing_xs">4dp</dimen>
    <dimen name="spacing_sm">8dp</dimen>
    <dimen name="spacing_md">16dp</dimen>
    <dimen name="spacing_lg">24dp</dimen>
    <dimen name="spacing_xl">32dp</dimen>
</resources>
```

---

## 14. Kiểm thử & Đảm bảo Chất lượng

### 14.1 Test Pyramid

```
        ┌──────────────┐
        │  Espresso   │  ← 20% — E2E UI flows (10 tests)
        │  UI Tests   │
        ├──────────────┤
        │  Unit Tests │  ← 60% — ViewModels, Use Cases, Repositories (50 tests)
        │  (JUnit 5)   │
        ├──────────────┤
        │ Integration  │  ← 20% — Room + Repository (15 tests)
        │    Tests     │
        └──────────────┘
```

### 14.2 Test Cases Chính

**Unit Tests:**
- `BoardViewModel_testAddWordToSentence`: verify `sentenceItems` updated
- `BoardViewModel_testClearSentence`: verify clear all
- `VerifyPinUseCase_testCorrectPin`: returns true
- `VerifyPinUseCase_testWrongPin`: returns false
- `AddCardUseCase_testSaveCardWithImage`: image saved, card created
- `VocabCardMapper_testEntityToDomain`: correct mapping
- `MathChallengeGenerator_testRange`: results between 0-18

**Instrumented Tests (Android Test Orchestrator):**
- `BoardFlowTest`: tap 3 cards → speak → verify TTS triggered
- `PinFlowTest`: enter correct PIN → navigate to Admin
- `EditCardFlowTest`: add card → verify in list → delete → verify gone
- `SettingsFlowTest`: change grid 3→2 → verify re-render

### 14.3 CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/android.yml
name: Android CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - name: Cache Gradle
        uses: actions/cache@v4
        with: { path: ~/.gradle/caches, key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }} }
      - name: Run unit tests
        run: ./gradlew test
      - name: Run instrumented tests
        run: ./gradlew connectedAndroidTest
      - name: Build debug APK
        run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v4
        with: { name: apk, path: app/build/outputs/apk/debug/*.apk }
```

---

## 15. Phụ lục

### 15.1 Stitch Design Screens Reference

| Màn hình | Thư mục | File thiết kế |
|---|---|---|
| Giao tiếp chính | `Giao tiếp chính/` | `code.html`, `screen.png` |
| Xác minh mã PIN | `Xác minh mã pin/` | `code.html`, `screen.png` |
| Cài đặt hệ thống | `Cài đặt hệ thống/` | `code.html`, `screen.png` |
| Cài đặt giao diện | `Cài đặt giao diện/` | `code.html`, `screen.png` |
| Cài đặt giọng nói | `Cài đặt giọng nói/` | `code.html`, `screen.png` |
| Chế độ chỉnh sửa | `Chế độ chỉnh sửa và quản lý thư mục/` | `code.html`, `screen.png` |
| Hộp thoại thêm thẻ | `Hộp thoại thêm thẻ từ vụng/` | `code.html`, `screen.png` |
| Thống kê sử dụng | `Thống kê sử dụng từ vựng/` | `code.html`, `screen.png` |

### 15.2 Font & Typography

- **Primary Font:** Be Vietnam Pro (Regular 400, Medium 500, SemiBold 600, Bold 700, ExtraBold 800)
- **Font Loading:** `assets/fonts/BeVietnamPro-*.ttf`, use via `ResourcesCompat.getFont()`
- **No pure black text:** `#1a1c1b` (on-surface-light) thay vì `#000000`

### 15.3 Asset Requirements

| Asset | Định dạng | Kích thước | Số lượng |
|---|---|---|---|
| App Icon | PNG + Adaptive Icon | 48×48, 72×72, 96×96, 108×108, 144×144, 192×192 | 6 |
| Splash Screen | XML (layer-list) | Toàn màn hình | 1 |
| Vocab card placeholder | PNG | 512×512 @1x | 1 |
| Empty state illustration | SVG / PNG | 200×200 @1x | 2 |
| Category icons | Emoji (Unicode) | — | 8+ |
| Lottie: card tap | JSON | < 50KB | 1 |
| Lottie: success | JSON | < 20KB | 1 |

### 15.4 String Resources (i18n Base)

```xml
<!-- res/values-vi/strings.xml -->
<resources>
    <string name="app_name">VBoard AAC</string>
    <string name="placeholder_sentence">Nhấn thẻ để xây dựng câu…</string>
    <string name="btn_speak">PHÁT ÂM</string>
    <string name="btn_clear">XÓA</string>
    <string name="pin_title">Xác minh mã PIN</string>
    <string name="pin_hint">Nhập mã PIN</string>
    <string name="pin_wrong">Sai mã PIN, thử lại</string>
    <string name="admin_vocab_title">Quản lý Từ vựng</string>
    <string name="admin_voice_title">Cài đặt Giọng đọc</string>
    <string name="admin_ui_title">Cài đặt Giao diện</string>
    <string name="admin_stats_title">Thống kê Sử dụng</string>
    <string name="add_card_title">Thêm thẻ từ vựng mới</string>
    <string name="add_card_word_hint">Nhập từ</string>
    <string name="add_card_category_hint">Chọn nhóm</string>
    <string name="btn_save">Lưu</string>
    <string name="btn_cancel">Hủy</string>
    <string name="confirm_delete_title">Xác nhận xóa</string>
    <string name="confirm_delete_message">Bạn có chắc muốn xóa thẻ này?</string>
    <string name="confirm_delete_yes">Đồng ý</string>
    <string name="grid_small">Nhỏ (4 cột)</string>
    <string name="grid_medium">Vừa (3 cột)</string>
    <string name="grid_large">Lớn (2 cột)</string>
    <string name="stats_sentences_today">Số câu hôm nay</string>
    <string name="stats_words_today">Từ đã dùng</string>
    <string name="stats_top_words">Top từ phổ biến</string>
    <string name="voice_preview">Nghe thử</string>
    <string name="voice_test_sentence">Con muốn uống nước</string>
    <string name="tts_not_available">Không tìm thấy giọng Tiếng Việt. Vui lòng cài Google Tiếng Việt TTS.</string>
</resources>

<!-- res/values/strings.xml (English fallback) -->
<resources>
    <string name="app_name">VBoard AAC</string>
    <!-- ... translate all strings -->
</resources>
```

### 15.5 Migration Plan: Gốc → Kiến trúc Mới

| Bước | File hiện tại | Hành động | Thứ tự |
|---|---|---|---|
| 1 | `MainActivity.kt` | Extract business logic → `BoardViewModel` | Trước |
| 2 | `VocabRepository.kt` (gốc) | Thay thế bằng Room + Repository pattern | Trước |
| 3 | `activity_main.xml` | Thêm Data Binding tags | Sau 1 |
| 4 | `PinActivity.kt` | Refactor → `PinViewModel` | Sau 1 |
| 5 | `EditActivity.kt` | Fragment + ViewModel | Sau 2 |
| 6 | Settings Activities | Hợp nhất vào Navigation graph | Sau 3 |
| 7 | SharedPreferences | Migrate → DataStore | Sau 4 |
| 8 | Legacy item layouts | Thay bằng RecyclerView.ViewHolder | Sau 5 |

---

*Tài liệu này được tạo dựa trên: BTL_P3_PhacThaoThietKe.md, vboard_use_cases.md, BTL_P5_KiemThuBanMauGiay.md, vboard_test_script.md, stitch_vboard_aac_mobile/ (thiết kế Stitch), VBoardAAC_Android/ (code gốc Kotlin/Java), và CLAUDE.md.*
