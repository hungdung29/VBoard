# VBoard — UI Redesign "Apple Vibe"

**Ngày:** 2026-05-06
**Trạng thái:** Design approved — chờ implementation plan
**Scope:** Toàn bộ app (8 screens + 1 dialog) — refactor UI layer, không touch data/domain
**Tech:** Native Android XML Views (giữ nguyên, không migrate sang Compose)
**Min SDK:** 26 (Android 8.0)
**Effort estimate:** ~13 ngày · 1 sprint · 4 PR

---

## 1. Mục tiêu & Bối cảnh

VBoard là app AAC (Augmentative and Alternative Communication) tiếng Việt cho người không nói được — chủ yếu trẻ em (autism, bại não, đột quỵ). Lưới thẻ từ vựng + sentence strip + Vietnamese TTS. Phụ huynh quản lý qua Admin Hub khoá bằng PIN + math challenge.

App đã hoàn thành Phase 1 chức năng (xem `Implementation.md`). Vấn đề: UI hiện tại theo design system **"The Gentle Path"** — vàng gold #ffd700 rực, bo góc 24-48dp + button pill 9999dp, gradient, elevation 0dp flat — cảm giác "Android bold + kid-cartoonish". Mục tiêu redesign: **chuyển sang Apple Vibe (iOS HIG)** — clean, premium, vẫn kid-friendly đủ cho Board.

### Quyết định nền

| # | Quyết định | Chốt |
|---|---|---|
| 1 | Scope | Toàn bộ app, giữ XML Views (không Compose) |
| 2 | Visual direction | iOS HIG / System (system blue accent, gray bg, hairline divider, subtle shadow) |
| 3 | Board tone | Tinted theo category (pastel 8-14% saturation) — chỉ áp Board. Admin/Settings dùng HIG neutral |
| 4 | Theme | Light + Dark song hành, design tokens đôi từ ngày 1 |
| 5 | Typography | Be Vietnam Pro (4 weights) — bundled vào assets |
| 6 | Device priority | Phone + Tablet equally — 2 layouts riêng `layout/` + `layout-sw600dp/` |
| 7 | Iconography | Material Symbols Rounded weight 300 fill 0 — qua font file |

---

## 2. Design tokens

### 2.1 Color tokens

42 token tổng cộng, cho cả light và dark mode.

#### Surface (3-tier hệ thống grouped)

| Token | Light | Dark | Dùng cho |
|---|---|---|---|
| `system_background` | `#F2F2F7` | `#000000` | Root background các screen list/grouped |
| `secondary_system_background` | `#FFFFFF` | `#1C1C1E` | Card surface, content trong list |
| `tertiary_system_background` | `#F2F2F7` | `#2C2C2E` | Grouped panel chứa list |

#### Label & Separator

| Token | Light | Dark | Dùng cho |
|---|---|---|---|
| `label` | `#1C1C1E` | `#FFFFFF` | Primary text |
| `secondary_label` | `#3C3C43` @ 60% | `#EBEBF5` @ 60% | Subtitle, supporting |
| `tertiary_label` | `#3C3C43` @ 30% | `#EBEBF5` @ 30% | Inactive, hint |
| `placeholder` | `#3C3C43` @ 22% | `#EBEBF5` @ 22% | Empty state |
| `separator` | `#E5E5EA` | `#38383A` | Hairline divider — XML dùng `android:layout_height="0.5dp"` (render 1px trên mdpi/hdpi, scaled trên xhdpi+, đạt look hairline iOS) |
| `opaque_separator` | `#C6C6C8` | `#545458` | Drag handle, opaque divider |

#### Accent & Semantic

| Token | Light | Dark | Dùng cho |
|---|---|---|---|
| `accent` | `#007AFF` | `#0A84FF` | Phát âm, link, primary CTA, active state |
| `success` | `#34C759` | `#30D158` | Toggle on, "đã sao lưu", delta tăng |
| `warning` | `#FF9500` | `#FF9F0A` | (reserve) |
| `error` | `#FF3B30` | `#FF453A` | Xoá, destructive action, sai PIN |
| `info` | `#5856D6` | `#5E5CE6` | AI Voice icon (reserve cho future feature) |
| `disabled` | `#8E8E93` | `#8E8E93` | Disabled state |

#### Category tints (chỉ áp Board)

Pastel saturation 8-14%. Mỗi category có background + label đảm bảo contrast WCAG AA.

| Category | Light bg | Light label | Dark bg | Dark label |
|---|---|---|---|---|
| 🍴 Ăn uống | `#FFF3E0` | `#9A4E00` | `#3A2A14` | `#FFD8A8` |
| 👨‍👩 Gia đình | `#E8F4FF` | `#0050C7` | `#102A45` | `#A6CFFF` |
| 💛 Cảm xúc | `#FFE8EE` | `#A0144F` | `#3A1A24` | `#FFB3C8` |
| 🎮 Hoạt động | `#E8F8E9` | `#1B5E20` | `#13301A` | `#A8E6B0` |
| 🎒 Đồ vật | `#F0E7FF` | `#4A148C` | `#251A40` | `#D0B8FF` |
| 🏠 Nơi chốn | `#FFF8DC` | `#8B6914` | `#322B14` | `#FFE08A` |

Token đặt tên: `category_food_bg`, `category_food_label`, ..., `category_place_bg`, `category_place_label`.

### 2.2 Typography (Be Vietnam Pro)

9 styles, dùng `letter-spacing` âm ở cỡ lớn (đặc trưng iOS), tracking 0 cho cỡ nhỏ (tránh đè dấu tiếng Việt).

| Style | Size | Weight | Letter-spacing | Line-height | Dùng |
|---|---|---|---|---|---|
| `display` | 36sp | 700 | -1.4 | 1.10 | Stat tile big number |
| `title_1` | 28sp | 700 | -0.6 | 1.15 | Screen title |
| `title_2` | 22sp | 600 | -0.4 | 1.20 | Section title |
| `title_3` | 18sp | 600 | -0.2 | 1.30 | Subsection, list group |
| `headline` | 17sp | 600 | 0 | 1.30 | Button, prominent inline |
| `body` | 15sp | 400 | 0 | 1.45 | Body text, list title |
| `callout` | 14sp | 500 | 0 | 1.40 | Chip, supporting bold |
| `caption_1` | 12sp | 500 | 0.1 | 1.35 | Subtitle, sub |
| `overline` | 10sp | 600 | 1.4 | 1.30 | Eyebrow uppercase |

XML: 9 `<style name="TextAppearance.VBoard.*">` trong `styles.xml`.

### 2.3 Spacing (8pt grid)

7 tokens. Bỏ token `40dp/48dp` hiện có.

| Token | Value | Note |
|---|---|---|
| `space_2xs` | 4dp | Inline gap |
| `space_xs` | 8dp | Tight padding, gap nhỏ |
| `space_sm` | 12dp | Inner padding chip, button compact |
| `space_md` | 16dp | **Default** — card padding, screen edge phone |
| `space_lg` | 20dp | Screen edge tablet (sw600dp), section spacing |
| `space_xl` | 24dp | Section gap lớn |
| `space_2xl` | 32dp | Hero spacing |

### 2.4 Corner radius

5 tokens. Thay đổi visual lớn nhất so với hiện tại.

| Token | Value | Dùng cho |
|---|---|---|
| `radius_xs` | 6dp | Tag, segment |
| `radius_sm` | 10dp | **Button** (giảm từ pill 9999dp) |
| `radius_md` | 14dp | **Card, list group, panel** (giảm từ 24dp) |
| `radius_lg` | 20dp | Bottom sheet, modal |
| `radius_full` | 9999dp | Avatar, FAB, pin dot, drag handle |

### 2.5 Elevation

4 tiers. Khác hiện tại (đang flat 0dp) — thêm subtle shadow vào mọi card.

| Tier | Light shadow | Dark fallback | Dùng |
|---|---|---|---|
| `e0` | none + 0.5px hairline | none + 0.5px hairline `#38383A` | Card flat, default |
| `e1` | `0 1px 2px rgba(0,0,0,0.04), 0 1px 3px rgba(0,0,0,0.06)` | 0.5px hairline (no shadow) | **Card default**, sentence strip |
| `e2` | `0 4px 12px rgba(0,0,0,0.08), 0 2px 4px rgba(0,0,0,0.04)` | `0 4px 12px rgba(0,0,0,0.4)` | Sheet, modal |
| `e3` | `0 12px 32px rgba(0,0,0,0.16), 0 6px 12px rgba(0,0,0,0.08)` | `0 12px 32px rgba(0,0,0,0.6)` | FAB, floating |

Implementation: dùng `MaterialCardView`'s `cardElevation` + custom shadow drawable hoặc `OutlineProvider` cho dark mode shadow.

### 2.6 Motion

4 durations + 4 easings.

**Durations:**
- `duration_tap` 100ms (haptic feedback, tap-press)
- `duration_fast` 200ms (chip add/remove, micro-interaction)
- `duration_medium` 350ms (sheet slide, screen transition)
- `duration_slow` 500ms (onboarding, special)

**Easings (interpolators):**
- `standard` cubic-bezier(0.4, 0, 0.2, 1)
- `emphasized` cubic-bezier(0.05, 0.7, 0.1, 1) — iOS spring
- `decelerate` cubic-bezier(0, 0, 0.2, 1)
- `accelerate` cubic-bezier(0.4, 0, 1, 1)

**Animation patterns:**
- Card tap → `scaleX/Y 0.96` 100ms emphasized → snap back trên touch up
- Chip add → translateX from `+30dp` + alpha `0→1` 200ms emphasized
- Chip remove → alpha `1→0` + collapse 200ms standard
- Bottom sheet → slideUp 350ms emphasized
- Sai PIN → shake (translateX ±8dp 4 lần) 200ms standard

XML: `res/anim/{tap_press,chip_slide_in,chip_fade_out,sheet_slide_up,shake_error}.xml` + `res/interpolator/{standard,emphasized,decelerate,accelerate}.xml`.

---

## 3. Component primitives

9 component reusable thông qua `<style>` XML + custom views khi cần.

### 3.1 Vocab Card (Board)

- Aspect 1:1, radius 14dp
- Background = `category_*_bg` theo category của card
- Label color = `category_*_label`
- Padding 14dp top/bottom, 10dp left/right
- Emoji 36sp, label callout 13sp/500
- Elevation e1
- Tap: scale 0.96 100ms emphasized, haptic 50ms
- Touch target ≥ 48dp via `minHeight`/`minWidth`

Custom view: `VBoardCardView extends MaterialCardView` — bake-in tap-scale animation.

### 3.2 Button (4 variants)

Height 48dp, radius 10dp, headline 17sp/600, letter-spacing -0.1.

| Variant | Background | Label | Border |
|---|---|---|---|
| `Primary` | `accent` solid | white | none |
| `Danger` | `secondary_system_background` | `error` | 0.5px `error` |
| `Tinted` | accent @ 12% | `accent` | none |
| `Plain` | transparent | `accent` | none, padding 10dp/6dp |

Pressed: `scale 0.97 + alpha 0.85` 100ms.

Style: `Widget.VBoard.Button.Primary` etc.

### 3.3 Chip — Word & Category

Radius 14dp, height 32dp, callout 14sp/500-600.

- **Word chip** (sentence strip): `accent` @ 12% bg, `accent` label, weight 600
- **Category chip / active**: `accent` solid bg, white label
- **Category chip / tinted**: `category_*_bg` bg, `category_*_label`, weight 500
- **Category chip / neutral**: `system_background` bg, `label` color, weight 500

Custom view: `VBoardChipView extends Chip` — auto-apply category tint qua attribute `app:vboardCategory="food"`.

### 3.4 Sentence Strip

- Container: e1 card, radius 14, padding 12dp/14dp
- Min height 56dp
- Horizontal scroll với chip word
- Empty state: italic placeholder "Chạm vào thẻ để ghép câu…" — `placeholder` color
- Backspace button trailing: 32×32 round, `system_background` bg, ⌫ icon

Animation: chip mới `chip_slide_in` 200ms; backspace → chip cuối `chip_fade_out` 200ms.

### 3.5 List Row (inset grouped)

Apple Settings.app pattern. Replace toàn bộ `item_admin_card` 64×64 hiện tại.

- Outer container card e1 radius 14, no padding
- Row: 14dp/16dp padding, 14dp gap
- Leading icon: 32×32 rounded square radius 8, tinted bg
- Title: body 15sp/500, label color
- Subtitle (optional): caption_1 12sp/60% opacity
- Trailing chevron `›` tertiary_label
- Hairline `separator` 0.5px giữa các row, không có ở row cuối

Layout include: `component/list_row.xml` với `<data>` binding cho icon/title/subtitle/trail.

### 3.6 Top App Bar

- Height 56dp
- Background `secondary_system_background`
- Hairline divider bottom 0.5px `separator`
- Leading: Plain button (back text "‹ Lùi" / "Huỷ") — `accent` color, body 14sp/500
- Title: headline 15sp/600 centered, letter-spacing -0.1
- Trailing: Plain button (action text "Xong" / "Sửa" — bold khi primary action)

### 3.7 Numpad (PIN)

- Grid 3 cột × 4 hàng
- Button 72×72 round (giảm từ 80×80)
- Background `secondary_system_background`, border 0.5px `separator`
- Number: title 26sp/400 (regular weight, không bold)
- Active state: scale 0.94 + bg `system_background`
- Special row: cancel/backspace là plain text buttons không có border

### 3.8 Section Header

- Eyebrow: overline 10sp/600 letter-spacing 1.4 uppercase, `tertiary_label`
- Title: title_3 18sp/600 letter-spacing -0.2
- Padding 16dp top, 8dp bottom

### 3.9 Stat Tile

- Card e1 radius 14 padding 16dp
- Number: display 36sp/700 letter-spacing -0.8
- Label: caption_1 12sp/60% opacity
- Delta indicator (optional): caption 11sp/500 weight 600, `success` color (tăng) hoặc `error` (giảm), prefix ↑/↓

---

## 4. Per-screen specifications

### 4.1 Board (`activity_main.xml`)

**Cấu trúc:**

```
┌─ Top App Bar ────────────────────────────────┐
│ ⚙ Settings    VBoard      ● Avatar           │
├──────────────────────────────────────────────┤
│ Sentence Strip card e1                        │
│   [chip Con] [chip muốn] [chip nước] ⌫      │
├──────────────────────────────────────────────┤
│ Category tabs (horizontal scroll)             │
│ [Tất cả·active] [🍴 Ăn] [👨 Gia] [😊 Cảm]   │
├──────────────────────────────────────────────┤
│ Vocab Grid                                    │
│  ┌────┐ ┌────┐ ┌────┐                        │
│  │🍚  │ │🥛  │ │🍎  │   (tinted theo cat)   │
│  │Cơm │ │Sữa │ │Táo │                        │
│  └────┘ └────┘ └────┘                        │
│  ...                                          │
├──────────────────────────────────────────────┤
│  [🔊 Phát âm  flex 2]  [Xoá]                 │
└──────────────────────────────────────────────┘
```

**Phone (412dp):** Grid 3 cột portrait.
**Tablet sw600dp portrait:** Grid 4 cột.
**Tablet sw600dp landscape:** Grid 6 cột, sentence strip cao 64dp.

**Actions:**
- Tap card → haptic 50ms + chip slide-in to sentence strip
- Tap ⌫ → chip cuối fade-out
- Tap "Xoá" → confirm dialog → clear sentence
- Tap "Phát âm" → TTS speak, button scale 0.97
- Tap ⚙ → mở PIN sheet (modal)

### 4.2 PIN Gate (`fragment_pin_sheet.xml` — convert từ Activity)

**Modal bottom sheet** — slide-up từ dưới với scrim 40% opacity. Drag handle 36×5 trên đầu.

**Cấu trúc:**

```
┌──────────────────────────────────┐
│        ▬▬▬▬ (drag handle)        │
│                                   │
│    CÂU HỎI (eyebrow)              │
│    4 + 5 = ?  (title_1 28sp)     │
│                                   │
│    ●  ●  ○  ○  (4 dots 14×14)   │
│                                   │
│    ┌──┐ ┌──┐ ┌──┐                │
│    │1 │ │2 │ │3 │   72×72 round  │
│    ├──┤ ├──┤ ├──┤                │
│    │4 │ │5 │ │6 │                │
│    ├──┤ ├──┤ ├──┤                │
│    │7 │ │8 │ │9 │                │
│    ├──┤ ├──┤ ├──┤                │
│    │Hủy│ │0 │ │⌫ │                │
│    └──┘ └──┘ └──┘                │
└──────────────────────────────────┘
```

**Logic:** giữ nguyên `PinViewModel`. Math challenge: `a = random(1..9)`, `b = random(1..9)`, op = `+` nếu `a≥b` else `-`. PIN sai: shake animation + dot màu `error` 200ms.

**File deleted:** `PinActivity.kt`, `activity_pin.xml`. **File added:** `PinSheetFragment.kt`, `fragment_pin_sheet.xml`. MainActivity show sheet qua `PinSheetFragment().show(supportFragmentManager, "pin")`.

### 4.3 Admin Hub (`activity_admin.xml`)

Inset grouped pattern (Settings.app).

**Cấu trúc:**

```
┌─ Top App Bar ─────────────────────┐
│ ‹ Lùi      Quản lý                │
├──────────────────────────────────┤
│ CHÍNH (eyebrow)                   │
│ ┌──────────────────────────────┐ │
│ │ 🔤  Quản lý từ vựng       › │ │
│ │     120 thẻ · 6 nhóm          │ │
│ ├──────────────────────────────┤ │
│ │ 🔊  Cài đặt giọng đọc     › │ │
│ │     Nữ miền Bắc                │ │
│ ├──────────────────────────────┤ │
│ │ 🎨  Giao diện              › │ │
│ │     3 cột · sáng              │ │
│ └──────────────────────────────┘ │
│                                   │
│ PHÂN TÍCH                         │
│ ┌──────────────────────────────┐ │
│ │ 📊  Thống kê sử dụng       › │ │
│ │     12 câu hôm nay            │ │
│ ├──────────────────────────────┤ │
│ │ 💾  Sao lưu & khôi phục    › │ │
│ │     Lần cuối: 3 ngày trước     │ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```

**Refactor:** GridLayout 2×2 → 2 RecyclerView grouped. Subtitle là live data từ ViewModel (counts). `AdminMenuItem(icon, title, subtitle, route)` data class.

**Tablet sw600dp landscape:** 2 grouped list side-by-side (CHÍNH | PHÂN TÍCH).

### 4.4 Edit (`activity_edit.xml`)

Pattern iOS Home Screen edit mode.

**Cấu trúc default mode:**

```
┌─ Top App Bar ─────────────────────┐
│ Huỷ          Quản lý từ    Sửa   │
├──────────────────────────────────┤
│ Category tabs                     │
├──────────────────────────────────┤
│ Vocab Grid (radius 14, neutral bg)│
│  ┌────┐ ┌────┐ ┌────┐            │
│  │🍚  │ │🥛  │ │🍎  │            │
│  │Cơm │ │Sữa │ │Táo │            │
│  └────┘ └────┘ └────┘            │
│  ┌────┐ ┌╶╶╶╶┐                   │
│  │🍎  │ │  +  │ ← inline add cell │
│  │Táo │ │     │                   │
│  └────┘ └╶╶╶╶┘                   │
│                                   │
│             ┌──────┐              │
│             │  +   │ FAB primary  │
│             └──────┘              │
└──────────────────────────────────┘
```

**Edit mode** (chạm "Sửa"):
- Mỗi card xuất hiện badge `−` (20dp circle red) ở góc trên phải
- Tap badge → confirm dialog → xoá card
- Top bar: "Sửa" → "Xong"

**Bỏ:** 2 FAB hiện tại (xanh + vàng). **Còn lại:** 1 FAB primary "+" (radius full, accent, e3 shadow).

**Folder management:** chuyển từ tab + FAB riêng sang Top Bar trailing menu (•••) → bottom sheet "Quản lý nhóm".

### 4.5 UI Settings (`activity_ui_settings.xml`)

Form layout inset grouped.

```
┌─ Top App Bar ─────────────────────┐
│ ‹ Quản lý       Giao diện        │
├──────────────────────────────────┤
│ HIỂN THỊ                          │
│ ┌──────────────────────────────┐ │
│ │ Số cột lưới       [2|3|4] segm│ │
│ ├──────────────────────────────┤ │
│ │ Chế độ tối             [○━━] │ │
│ ├──────────────────────────────┤ │
│ │ Cỡ chữ              ────●──   │ │
│ │                       1.0×    │ │
│ └──────────────────────────────┘ │
│                                   │
│ XEM TRƯỚC                         │
│ ┌──────────────────────────────┐ │
│ │  ┌──┐ ┌──┐                   │ │
│ │  │🍚│ │👨│  (live preview)   │ │
│ │  │Cơm│ │Bố│                   │ │
│ │  └──┘ └──┘                   │ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```

**Components:**
- Segmented control: `MaterialButtonToggleGroup` styled to iOS-style (background `system_background`, selected `secondary_system_background` + e1 shadow)
- Toggle: Material `SwitchCompat` với track `success` khi on, `system_background` khi off
- Slider: Material `Slider` với thumb 16dp white + e1 shadow, track 4dp `separator` / `accent`

### 4.6 Voice Settings (`activity_voice_settings.xml`)

```
┌─ Top App Bar ─────────────────────┐
│ ‹ Quản lý      Giọng đọc         │
├──────────────────────────────────┤
│ PHÁT ÂM                           │
│ ┌──────────────────────────────┐ │
│ │ 🔈 ──────●──── 🔊            │ │
│ └──────────────────────────────┘ │
│                                   │
│ LOẠI GIỌNG                        │
│ ┌──────────────────────────────┐ │
│ │ Nữ · miền Bắc           ✓   │ │
│ │ Nam · miền Bắc                │ │
│ │ Nữ · miền Nam                 │ │
│ │ Nam · miền Nam                │ │
│ └──────────────────────────────┘ │
│                                   │
│ ┌──────────────────────────────┐ │
│ │   ▶ Nghe thử "Con muốn..."   │ │
│ └──────────────────────────────┘ │
│                                   │
│ AI VOICE (sắp có)                 │
│ ┌──────────────────────────────┐ │
│ │ 🎤 Ghi giọng người thân     │ │
│ │    Ghi 30 giây để tạo giọng AI│ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```

Radio list dùng pattern grouped với checkmark ✓ thay vì RadioButton circle. Test button: Tinted button full width. AI Voice card: opacity 50% (disabled state).

### 4.7 Stats (`activity_stats.xml`)

Apple Health pattern.

```
┌─ Top App Bar ─────────────────────┐
│ ‹ Quản lý       Thống kê         │
├──────────────────────────────────┤
│ HÔM NAY                           │
│ ┌────────┐ ┌────────┐            │
│ │  12    │ │   8    │            │
│ │Câu ghép│ │Từ dùng │            │
│ │↑3 hqua │ │↑2 mới  │            │
│ └────────┘ └────────┘            │
│                                   │
│ 7 NGÀY QUA                        │
│ ┌──────────────────────────────┐ │
│ │  █                            │ │
│ │  █  █     █                   │ │
│ │  █  █  █  █  █  █  █          │ │
│ │  T2 T3 T4 T5 T6 T7 CN         │ │
│ └──────────────────────────────┘ │
│                                   │
│ TOP TỪ PHỔ BIẾN                   │
│ ┌──────────────────────────────┐ │
│ │ "Nước"              15× │ │
│ │ "Con"               12× │ │
│ │ "Muốn"              10× │ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```

Chart: custom `Canvas` view (không cần MPAndroidChart cho 7 bar đơn giản). Bar `accent` color, radius 4dp top, label `caption_1`.

### 4.8 Backup (`activity_backup.xml`)

```
┌─ Top App Bar ─────────────────────┐
│ ‹ Quản lý      Sao lưu           │
├──────────────────────────────────┤
│ TRẠNG THÁI                        │
│ ┌──────────────────────────────┐ │
│ │ ✓ Đã sao lưu                  │ │
│ │ Lần cuối: 3 ngày trước         │ │
│ │ 124 thẻ · 6 nhóm              │ │
│ └──────────────────────────────┘ │
│                                   │
│ HÀNH ĐỘNG                         │
│ ┌──────────────────────────────┐ │
│ │ 💾 Sao lưu ngay           › │ │
│ │    Tạo file .json             │ │
│ │ 📥 Khôi phục từ file      › │ │
│ │    Chọn file .json            │ │
│ └──────────────────────────────┘ │
│                                   │
│ CẢNH BÁO                          │
│ ┌──────────────────────────────┐ │
│ │ Đặt lại về mặc định       › │ │ ← title color = error
│ │ Xoá toàn bộ thẻ tuỳ chỉnh     │ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```

Status card status indicator: success check + label. Destructive action title `error` color.

### 4.9 Add Card Dialog (`fragment_add_card_sheet.xml` — convert từ Dialog)

Bottom sheet với drag handle.

```
┌──────────────────────────────────┐
│         ▬▬▬▬                     │
│ Huỷ      Thêm thẻ           Lưu  │
│                                   │
│ ┌──────────────────────────────┐ │
│ │      📷                       │ │
│ │   Chụp ảnh hoặc chọn ảnh      │ │
│ └──────────────────────────────┘ │
│                                   │
│ ┌──────────────────────────────┐ │
│ │ TỪ VỰNG                       │ │
│ │ Cốc                           │ │
│ └──────────────────────────────┘ │
│                                   │
│ ┌──────────────────────────────┐ │
│ │ NHÓM                       › │ │
│ │ Đồ vật                        │ │
│ └──────────────────────────────┘ │
└──────────────────────────────────┘
```

Field cards với eyebrow + value pattern (thay TextInputLayout). Group picker mở picker sheet thứ 2.

---

## 5. File-by-file change inventory

### 5.1 Add (mới hoàn toàn)

| File | Phase |
|---|---|
| `assets/fonts/BeVietnamPro-Regular.ttf` | 1 |
| `assets/fonts/BeVietnamPro-Medium.ttf` | 1 |
| `assets/fonts/BeVietnamPro-SemiBold.ttf` | 1 |
| `assets/fonts/BeVietnamPro-Bold.ttf` | 1 |
| `res/font/be_vietnam_pro.xml` | 1 |
| `res/font/material_symbols_rounded.ttf` | 1 |
| `res/values-night/colors.xml` | 1 |
| `res/anim/tap_press.xml` | 1 |
| `res/anim/chip_slide_in.xml` | 1 |
| `res/anim/chip_fade_out.xml` | 1 |
| `res/anim/sheet_slide_up.xml` | 1 |
| `res/anim/shake_error.xml` | 1 |
| `res/interpolator/standard.xml` | 1 |
| `res/interpolator/emphasized.xml` | 1 |
| `res/interpolator/decelerate.xml` | 1 |
| `res/interpolator/accelerate.xml` | 1 |
| `res/drawable/bg_card_tinted.xml` (single drawable + ColorStateList tint per category via `backgroundTint`) | 2 |
| `res/color/category_bg_tint.xml` (ColorStateList map 6 categories) | 2 |
| `res/color/category_label_tint.xml` (ColorStateList map 6 categories) | 2 |
| `res/drawable/bg_btn_primary.xml` | 2 |
| `res/drawable/bg_btn_danger.xml` | 2 |
| `res/drawable/bg_btn_tinted.xml` | 2 |
| `res/drawable/bg_chip_word.xml` | 2 |
| `res/drawable/bg_chip_category.xml` (state-list active/inactive + tint) | 2 |
| `res/drawable/bg_sentence_strip.xml` | 2 |
| `res/drawable/bg_numpad_button.xml` | 2 |
| `res/drawable/bg_segmented_control.xml` | 2 |
| `res/drawable/bg_card_neutral.xml` (cho List Row, Stat Tile, Settings card) | 2 |
| `res/layout/component/sentence_strip.xml` | 2 |
| `res/layout/component/list_row.xml` | 2 |
| `res/layout/component/section_header.xml` | 2 |
| `res/layout/component/stat_tile.xml` | 2 |
| `res/layout/component/numpad.xml` | 2 |
| `res/layout/component/top_app_bar.xml` | 2 |
| `res/layout/fragment_pin_sheet.xml` | 3 |
| `res/layout/fragment_add_card_sheet.xml` | 3 |
| `res/layout/fragment_folder_management_sheet.xml` (mở từ Edit top-bar trailing menu) | 3 |
| `res/layout-sw600dp/activity_main.xml` | 3 |
| `res/layout-sw600dp/activity_admin.xml` | 3 |
| `res/layout-sw600dp/activity_edit.xml` | 3 |
| `res/layout-sw600dp/activity_ui_settings.xml` | 3 |
| `res/layout-sw600dp/activity_voice_settings.xml` | 3 |
| `res/layout-sw600dp/activity_stats.xml` | 3 |
| `res/layout-sw600dp/activity_backup.xml` | 3 |
| `app/src/main/java/com/vboard/aac/ui/common/VBoardCardView.kt` | 2 |
| `app/src/main/java/com/vboard/aac/ui/common/VBoardChipView.kt` | 2 |
| `app/src/main/java/com/vboard/aac/ui/pin/PinSheetFragment.kt` | 3 |
| `app/src/main/java/com/vboard/aac/ui/edit/AddCardSheetFragment.kt` | 3 |
| `app/src/main/java/com/vboard/aac/ui/edit/FolderManagementSheetFragment.kt` | 3 |
| `app/src/debug/.../DebugStyleGalleryActivity.kt` (BuildConfig.DEBUG) | 2 |

### 5.2 Refactor (existing, đổi nội dung)

| File | Action | Phase |
|---|---|---|
| `res/values/colors.xml` | Replace 100% → 42 token mới | 1 |
| `res/values/dimens.xml` | Replace → 7 spacing + 5 radius + 4 elevation + 9 type sizes | 1 |
| `res/values/styles.xml` | Replace → 9 TextAppearance.VBoard.* + 9 Widget.VBoard.* | 1 |
| `res/values/themes.xml` | Update Material3 ColorScheme → token mới | 1 |
| `res/values/strings.xml` | Giữ nguyên content, audit thêm content_description | 4 |
| `res/layout/activity_main.xml` | Refactor Board layout (Section 4.1) | 3 |
| `res/layout/activity_admin.xml` | Refactor Admin Hub (Section 4.3) | 3 |
| `res/layout/activity_edit.xml` | Refactor Edit (Section 4.4) | 3 |
| `res/layout/activity_ui_settings.xml` | Refactor (Section 4.5) | 3 |
| `res/layout/activity_voice_settings.xml` | Refactor (Section 4.6) | 3 |
| `res/layout/activity_stats.xml` | Refactor (Section 4.7) | 3 |
| `res/layout/activity_backup.xml` | Refactor (Section 4.8) | 3 |
| `res/layout/activity_settings.xml` (pre-PIN) | Refactor đơn giản hoá thành sheet trigger | 3 |
| `res/layout/item_vocab_card.xml` | Refactor để dùng `VBoardCardView` | 3 |
| `res/layout/item_word_chip.xml` | Refactor radius 14 + accent-tinted | 3 |
| `res/layout/item_category_chip.xml` | Refactor 3 state variants | 3 |
| `res/layout/item_admin_card.xml` | DELETE — replace by `component/list_row.xml` | 3 |
| `res/layout/item_settings_card.xml` | DELETE — replace by `component/list_row.xml` | 3 |
| `res/layout/item_edit_card.xml` | Refactor để dùng `VBoardCardView` + edit-mode badge | 3 |
| `res/layout/item_folder_tab.xml` | Refactor radius + active state | 3 |
| `res/layout/dialog_add_card.xml` | DELETE — replace by `fragment_add_card_sheet.xml` | 3 |
| 8 ViewModel | Extend: thêm category-tint mapping cho `VocabCardUiState` | 3 |
| `MainActivity.kt` | Update để open `PinSheetFragment` thay startActivity | 3 |
| `EditActivity.kt` | Update để open `AddCardSheetFragment` + `FolderManagementSheetFragment` thay dialog/FAB | 3 |
| `app/src/main/java/com/vboard/aac/ui/common/extensions/ViewExtensions.kt` | Extend: animatePressIn/Out, fadeSlideInFromRight | 2 |
| `AndroidManifest.xml` | Remove `<activity>` block của `PinActivity`. Các activity khác giữ nguyên | 3 |
| `res/drawable/bg_pin_dot_empty.xml` | Refactor: 14×14 outline `tertiary_label` thay 20dp `secondary` | 3 |
| `res/drawable/bg_pin_dot_filled.xml` | Refactor: 14×14 solid `label` thay 20dp `primary_container` | 3 |

### 5.3 Delete

| File | Reason |
|---|---|
| `res/layout/activity_pin.xml` | Convert sang BottomSheet |
| `res/layout/dialog_add_card.xml` | Convert sang BottomSheet |
| `res/drawable/bg_button_gradient.xml` | Bỏ gradient (anti-Apple) |
| `res/drawable/bg_button_round.xml` | Replace bởi numpad button drawable mới |
| `res/drawable/bg_button_speak.xml` | Replace bởi `bg_btn_primary.xml` |
| `res/drawable/bg_settings_card.xml` | Replace bởi `bg_card.xml` (radius 14) |
| `res/drawable/bg_emoji_container.xml` | Bỏ — emoji render direct trong card |
| `res/drawable/bg_icon_circle.xml` | Replace bởi `bg_icon_rounded.xml` (radius 8 thay tròn) |
| `res/drawable/bg_icon_primary.xml` | Replace bởi tinted variants |
| `res/drawable/bg_icon_error.xml` | Replace bởi tinted variants |
| `res/drawable/bg_avatar.xml` | Replace, simplify |
| `res/drawable/bg_folder_tab.xml` | Replace bởi `bg_chip_category_*.xml` |
| `res/drawable/bg_vocab_card.xml` | Replace bởi `bg_card_tinted_*.xml` (6 variants) |
| `res/drawable/bg_word_chip.xml` | Replace bởi `bg_chip_word.xml` |
| `res/drawable/bg_category_chip.xml` | Replace bởi `bg_chip_category_*.xml` |
| `app/src/main/java/com/vboard/aac/ui/pin/PinActivity.kt` | Convert sang `PinSheetFragment` |

---

## 6. Migration plan

### Phase 1 — Foundations (~3 ngày)

Refactor toàn bộ design tokens. App chạy đúng visual hiện tại sau Phase 1 (vì không có view nào dùng token mới — token mới song song token cũ trong scope khác nhau, hoặc dùng feature flag).

**Cách an toàn nhất:** Phase 1 không xoá tên cũ trong `colors.xml`/`dimens.xml`. Thêm token mới song song. Theme cũ `Theme.VBoard` vẫn map sang token cũ; theme mới `Theme.VBoard.New` map sang token mới. Trong `themes.xml` toggle theme qua `BuildConfig.NEW_DESIGN_SYSTEM` (default `false` ở Phase 1, `true` từ Phase 3). Bỏ token cũ + flag ở cuối Phase 4.

**Deliverable:** all token files committed. Build green. UI cũ vẫn render đúng.

### Phase 2 — Components (~2 ngày)

Build 9 component primitive. Tạo `DebugStyleGalleryActivity` ẩn để QA xem side-by-side.

**Deliverable:** Gallery debug có đủ 9 component render đúng spec. UI sản phẩm chưa thay đổi.

### Phase 3 — Screens (~6 ngày)

Refactor lần lượt theo thứ tự ưu tiên:

1. Board (MainActivity) — 1.5 ngày
2. PIN (PinActivity → PinSheetFragment) — 1 ngày
3. Admin Hub — 0.75 ngày
4. Edit — 1 ngày
5. UI Settings + Voice + Stats + Backup — mỗi cái 0.5 ngày
6. Add Card Dialog → BottomSheet — 0.5 ngày

Mỗi screen 1 PR riêng. Existing 65 unit + 4 instrumented test phải xanh sau mỗi PR.

**Deliverable:** Full app rendered theo Apple Vibe.

### Phase 4 — Polish (~2 ngày)

- Motion timing audit
- Dark mode QA toàn bộ 8 screen (visual diff)
- TalkBack `contentDescription` audit (FR-13.1 đang đỏ)
- Touch target Lint check ≥ 48dp
- WCAG AA contrast audit (chỗ rủi ro nhất: category tints)
- Performance: layout inflation ≤ 16ms / screen

**Deliverable:** Ship-ready.

### Risk & rollback

- **Risk:** Material3 ColorScheme migration có thể vỡ FAB/Snackbar default → Phase 1 chỉ swap colors, giữ `parent="Theme.Material3.DayNight.NoActionBar"`. Smoke test trước merge.
- **Risk:** Be Vietnam Pro không bundle thì fallback Roboto → Phase 1 add font, test render Vietnamese diacritics trên API 26 (Android 8).
- **Risk:** Tablet sw600dp tăng workload → Board ưu tiên Phase 3, các màn khác kế thừa layout phone trên tablet OK (đa số là form, scale ổn).
- **Rollback:** Mỗi phase 1 PR riêng. Phase 1 dùng feature flag `BuildConfig.NEW_DESIGN_SYSTEM`. Phase 3 onward bỏ flag (token swap final).

---

## 7. Accessibility

| Tiêu chuẩn | Implementation |
|---|---|
| Touch target ≥ 48×48dp | Enforce qua `minWidth`/`minHeight` trên Vocab Card, Numpad, Button. Lint check Phase 4. |
| WCAG AA contrast (4.5:1) | Category tints đã chọn để đạt tiêu chuẩn (xem section 2.1). Audit Phase 4 với Material Color Tool. |
| Font scale 0.875× → 1.5× | Tokens dùng `sp` không `dp`. UISettings có slider chỉnh `Configuration.fontScale`. |
| TalkBack | `contentDescription` cho mọi ImageButton/icon. Section header dùng `accessibilityHeading="true"`. List row có `accessibilityCustomAction` để xoá. Audit Phase 4. |
| No color-only info | Mọi state có icon + text + color (active state có cả color + bold weight). |
| Focus order | Top bar → strip → tabs → grid → bottom bar. Test với keyboard tablet. |
| Haptic | 50ms tap (default), 100ms error (PIN sai). Đã có `HapticFeedbackManager`. |
| Dark mode | Auto qua `Configuration.UI_MODE_NIGHT_*`. Token đã pair light/dark. |

---

## 8. Out of scope (cho redesign này)

Để tránh scope creep, các items dưới đây không nằm trong redesign này:

- Voice Cloning (FR-13 — Phase 2 của Implementation.md)
- Drag & Drop sắp xếp thẻ (FR-14)
- Material You dynamic colors (Implementation Phase 3)
- Multi-language (Implementation Phase 3)
- Cloud backup (Implementation Phase 3)
- Onboarding flow lần đầu (Implementation Phase 2)
- Compose migration (đã quyết giữ XML)

Chỉ refactor UI layer cho 8 màn + 1 dialog đã có. Logic, data, domain, repository, use cases, ViewModel — không đổi (ngoại trừ extend mapping category-tint).

---

## 9. Token summary

- **Color:** 42 (3 surface × 2 + 6 label × 2 + 6 semantic × 2 + 6 category × 2)
- **Typography:** 9 styles
- **Spacing:** 7 tokens
- **Radius:** 5 tokens
- **Elevation:** 4 tiers
- **Motion:** 4 durations + 4 easings = 8

**Tổng: 75 tokens** thay thế design system "The Gentle Path" hiện tại.
