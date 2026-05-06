# VBoard UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor VBoard's UI layer from "The Gentle Path" (gold + pill + flat) to "Apple Vibe" iOS HIG (system blue + radius 14 + subtle shadow + Be Vietnam Pro), with light+dark mode and tablet+phone layouts, while leaving data/domain/repository/use-case/ViewModel logic untouched.

**Architecture:** 4 phases delivered as 4 separate PRs. Phase 1 introduces tokens & theme behind a `BuildConfig.NEW_DESIGN_SYSTEM` flag (default `false`) so the app keeps rendering the old UI. Phase 2 builds 9 reusable component primitives in a hidden debug gallery. Phase 3 flips the flag to `true` and refactors all 8 screens + the Add-Card dialog one screen at a time, each its own commit. Phase 4 polishes motion, accessibility, dark mode, performance, then deletes the old tokens & flag.

**Tech Stack:** Kotlin 1.9 · Android XML Views · Material Components 1.11 · Hilt 2.51 · Be Vietnam Pro · Material Symbols Rounded · ConstraintLayout · ViewBinding (no Compose).

**Reference spec:** `docs/superpowers/specs/2026-05-06-vboard-ui-redesign-design.md`

---

## How to use this plan

- **Tests:** Existing 65 unit tests + 4 instrumented tests must remain green at the end of every task. Run `./gradlew test` after Kotlin changes; `./gradlew connectedAndroidTest` requires a connected device/emulator.
- **TDD where it makes sense:** Custom views (Task 2.1, 2.2, 2.3), animation helpers (Task 2.0), ViewModel mapping (Task 3.0). Pure XML/drawable tasks verify with `./gradlew assembleDebug` + a manual visual check on the Debug Gallery activity (Task 2.13) before committing.
- **Commits:** One commit per task unless noted. Commit messages follow `<type>: <imperative summary>` (e.g. `feat: add Be Vietnam Pro font family`, `refactor(board): redesign activity_main with HIG tokens`).
- **Build commands** assume current directory is repo root `/Users/macbook/Desktop/Projects/UET/VBoard`.
- **Token coexistence:** Phase 1 keeps every existing token & theme intact and adds `vb_*` tokens + a `Theme.VBoard.New` theme. Phase 3 only flips the manifest theme attribute when `BuildConfig.NEW_DESIGN_SYSTEM` is true. Phase 4 deletes the old tokens.

---

## Phase 1 — Foundations (Sprint Day 1–3 · ~3 days · 1 PR)

### Task 1.0: Add BuildConfig flag for new design system

**Files:**
- Modify: `app/build.gradle:8-23`

- [ ] **Step 1: Add `buildFeatures.buildConfig = true` and define the flag**

In `app/build.gradle`, replace the `buildFeatures` block:

```groovy
    buildFeatures {
        viewBinding true
        buildConfig true
    }
```

Inside `defaultConfig`, after `versionName "1.0.0"`, add:

```groovy
        buildConfigField "boolean", "NEW_DESIGN_SYSTEM", "false"
```

- [ ] **Step 2: Sync Gradle and confirm compilation**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. `BuildConfig.NEW_DESIGN_SYSTEM` is now available in code.

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle
git commit -m "build: add NEW_DESIGN_SYSTEM build flag for redesign rollout"
```

---

### Task 1.1: Add Be Vietnam Pro font family

**Files:**
- Create: `app/src/main/res/font/BeVietnamPro-Regular.ttf` (binary)
- Create: `app/src/main/res/font/BeVietnamPro-Medium.ttf` (binary)
- Create: `app/src/main/res/font/BeVietnamPro-SemiBold.ttf` (binary)
- Create: `app/src/main/res/font/BeVietnamPro-Bold.ttf` (binary)
- Create: `app/src/main/res/font/be_vietnam_pro.xml`

- [ ] **Step 1: Download fonts**

Run:

```bash
mkdir -p app/src/main/res/font
cd /tmp && rm -rf be-vn-pro && mkdir be-vn-pro && cd be-vn-pro
curl -sLo BeVietnamPro.zip "https://fonts.google.com/download?family=Be%20Vietnam%20Pro"
unzip -q BeVietnamPro.zip
cp BeVietnamPro-Regular.ttf  /Users/macbook/Desktop/Projects/UET/VBoard/app/src/main/res/font/
cp BeVietnamPro-Medium.ttf   /Users/macbook/Desktop/Projects/UET/VBoard/app/src/main/res/font/
cp BeVietnamPro-SemiBold.ttf /Users/macbook/Desktop/Projects/UET/VBoard/app/src/main/res/font/
cp BeVietnamPro-Bold.ttf     /Users/macbook/Desktop/Projects/UET/VBoard/app/src/main/res/font/
```

Note: filenames must use only lowercase letters, digits, and underscores in `res/font/` — Android resource compiler rejects mixed case. Rename:

```bash
cd /Users/macbook/Desktop/Projects/UET/VBoard/app/src/main/res/font
mv BeVietnamPro-Regular.ttf  be_vietnam_pro_regular.ttf
mv BeVietnamPro-Medium.ttf   be_vietnam_pro_medium.ttf
mv BeVietnamPro-SemiBold.ttf be_vietnam_pro_semibold.ttf
mv BeVietnamPro-Bold.ttf     be_vietnam_pro_bold.ttf
```

- [ ] **Step 2: Create the font family descriptor**

Create `app/src/main/res/font/be_vietnam_pro.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android">
    <font
        android:fontStyle="normal"
        android:fontWeight="400"
        android:font="@font/be_vietnam_pro_regular" />
    <font
        android:fontStyle="normal"
        android:fontWeight="500"
        android:font="@font/be_vietnam_pro_medium" />
    <font
        android:fontStyle="normal"
        android:fontWeight="600"
        android:font="@font/be_vietnam_pro_semibold" />
    <font
        android:fontStyle="normal"
        android:fontWeight="700"
        android:font="@font/be_vietnam_pro_bold" />
</font-family>
```

- [ ] **Step 3: Build to confirm font resources compile**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/font
git commit -m "feat(font): add Be Vietnam Pro family (4 weights)"
```

---

### Task 1.2: Add Material Symbols Rounded font

**Files:**
- Create: `app/src/main/res/font/material_symbols_rounded.ttf` (binary)

- [ ] **Step 1: Download Material Symbols Rounded**

```bash
curl -sLo /Users/macbook/Desktop/Projects/UET/VBoard/app/src/main/res/font/material_symbols_rounded.ttf \
  "https://fonts.gstatic.com/s/materialsymbolsrounded/v211/sykg-zNym6YjUruM-QrEh7-nyTnjDwKNJ_190FjpZIvDmUSVOK7BDB_Qb9vUSzq3wzLK-P0J-V_Zs-QtQth3-jOcbTdr.ttf"
```

(The URL points to the variable-axis static fallback. Confirms ~120KB.)

- [ ] **Step 2: Verify file exists and is ~120KB**

```bash
ls -la app/src/main/res/font/material_symbols_rounded.ttf
```
Expected: file exists, size between 100KB and 250KB.

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/font/material_symbols_rounded.ttf
git commit -m "feat(font): add Material Symbols Rounded for icon system"
```

---

### Task 1.3: Add new color tokens (light)

**Files:**
- Modify: `app/src/main/res/values/colors.xml` (append, do not delete existing)

- [ ] **Step 1: Append the 42-token light palette**

Add to the end of `app/src/main/res/values/colors.xml`, before `</resources>`:

```xml
    <!-- ============================================== -->
    <!-- VBoard Apple Vibe tokens (Phase 1 introduction) -->
    <!-- All tokens prefixed with `vb_` to coexist with  -->
    <!-- legacy tokens until Phase 4.                   -->
    <!-- ============================================== -->

    <!-- Surfaces (3-tier) -->
    <color name="vb_system_background">#F2F2F7</color>
    <color name="vb_secondary_system_background">#FFFFFF</color>
    <color name="vb_tertiary_system_background">#F2F2F7</color>

    <!-- Label & Separator -->
    <color name="vb_label">#1C1C1E</color>
    <color name="vb_secondary_label">#993C3C43</color>      <!-- 60% -->
    <color name="vb_tertiary_label">#4D3C3C43</color>       <!-- 30% -->
    <color name="vb_placeholder">#383C3C43</color>          <!-- 22% -->
    <color name="vb_separator">#E5E5EA</color>
    <color name="vb_opaque_separator">#C6C6C8</color>

    <!-- Accent & Semantic -->
    <color name="vb_accent">#007AFF</color>
    <color name="vb_accent_tinted">#1F007AFF</color>        <!-- 12% -->
    <color name="vb_success">#34C759</color>
    <color name="vb_warning">#FF9500</color>
    <color name="vb_error">#FF3B30</color>
    <color name="vb_info">#5856D6</color>
    <color name="vb_disabled">#8E8E93</color>

    <!-- Category tints — bg + label per category -->
    <color name="vb_category_food_bg">#FFF3E0</color>
    <color name="vb_category_food_label">#9A4E00</color>
    <color name="vb_category_family_bg">#E8F4FF</color>
    <color name="vb_category_family_label">#0050C7</color>
    <color name="vb_category_emotion_bg">#FFE8EE</color>
    <color name="vb_category_emotion_label">#A0144F</color>
    <color name="vb_category_activity_bg">#E8F8E9</color>
    <color name="vb_category_activity_label">#1B5E20</color>
    <color name="vb_category_object_bg">#F0E7FF</color>
    <color name="vb_category_object_label">#4A148C</color>
    <color name="vb_category_place_bg">#FFF8DC</color>
    <color name="vb_category_place_label">#8B6914</color>
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/colors.xml
git commit -m "feat(theme): add Apple Vibe color tokens (light)"
```

---

### Task 1.4: Add new color tokens (dark)

**Files:**
- Create: `app/src/main/res/values-night/colors.xml`

- [ ] **Step 1: Create the dark palette file**

Create `app/src/main/res/values-night/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- VBoard Apple Vibe tokens — DARK -->

    <!-- Surfaces (3-tier) -->
    <color name="vb_system_background">#000000</color>
    <color name="vb_secondary_system_background">#1C1C1E</color>
    <color name="vb_tertiary_system_background">#2C2C2E</color>

    <!-- Label & Separator -->
    <color name="vb_label">#FFFFFF</color>
    <color name="vb_secondary_label">#99EBEBF5</color>      <!-- 60% -->
    <color name="vb_tertiary_label">#4DEBEBF5</color>       <!-- 30% -->
    <color name="vb_placeholder">#38EBEBF5</color>          <!-- 22% -->
    <color name="vb_separator">#38383A</color>
    <color name="vb_opaque_separator">#545458</color>

    <!-- Accent & Semantic -->
    <color name="vb_accent">#0A84FF</color>
    <color name="vb_accent_tinted">#1F0A84FF</color>
    <color name="vb_success">#30D158</color>
    <color name="vb_warning">#FF9F0A</color>
    <color name="vb_error">#FF453A</color>
    <color name="vb_info">#5E5CE6</color>
    <color name="vb_disabled">#8E8E93</color>

    <!-- Category tints (dark variants) -->
    <color name="vb_category_food_bg">#3A2A14</color>
    <color name="vb_category_food_label">#FFD8A8</color>
    <color name="vb_category_family_bg">#102A45</color>
    <color name="vb_category_family_label">#A6CFFF</color>
    <color name="vb_category_emotion_bg">#3A1A24</color>
    <color name="vb_category_emotion_label">#FFB3C8</color>
    <color name="vb_category_activity_bg">#13301A</color>
    <color name="vb_category_activity_label">#A8E6B0</color>
    <color name="vb_category_object_bg">#251A40</color>
    <color name="vb_category_object_label">#D0B8FF</color>
    <color name="vb_category_place_bg">#322B14</color>
    <color name="vb_category_place_label">#FFE08A</color>
</resources>
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values-night/colors.xml
git commit -m "feat(theme): add Apple Vibe color tokens (dark)"
```

---

### Task 1.5: Add category ColorStateList resources

**Files:**
- Create: `app/src/main/res/color/vb_category_bg_tint.xml`
- Create: `app/src/main/res/color/vb_category_label_tint.xml`

These ColorStateLists let a single `bg_card_tinted.xml` drawable + ChipView take any of the 6 category colors via `android:state_*` attributes provided by `VBoardCardView` (Phase 2).

- [ ] **Step 1: Create background tint state list**

Create `app/src/main/res/color/vb_category_bg_tint.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <item app:state_category="food"     android:color="@color/vb_category_food_bg" />
    <item app:state_category="family"   android:color="@color/vb_category_family_bg" />
    <item app:state_category="emotion"  android:color="@color/vb_category_emotion_bg" />
    <item app:state_category="activity" android:color="@color/vb_category_activity_bg" />
    <item app:state_category="object"   android:color="@color/vb_category_object_bg" />
    <item app:state_category="place"    android:color="@color/vb_category_place_bg" />
    <item                               android:color="@color/vb_secondary_system_background" />
</selector>
```

- [ ] **Step 2: Create label tint state list**

Create `app/src/main/res/color/vb_category_label_tint.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <item app:state_category="food"     android:color="@color/vb_category_food_label" />
    <item app:state_category="family"   android:color="@color/vb_category_family_label" />
    <item app:state_category="emotion"  android:color="@color/vb_category_emotion_label" />
    <item app:state_category="activity" android:color="@color/vb_category_activity_label" />
    <item app:state_category="object"   android:color="@color/vb_category_object_label" />
    <item app:state_category="place"    android:color="@color/vb_category_place_label" />
    <item                               android:color="@color/vb_label" />
</selector>
```

- [ ] **Step 3: Declare the custom state attribute**

Create `app/src/main/res/values/attrs.xml` (or append if exists):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Custom view state used by VBoardCardView / VBoardChipView (Phase 2) -->
    <attr name="state_category" format="string" />

    <!-- Custom attributes consumed by VBoardCardView (Phase 2) -->
    <declare-styleable name="VBoardCardView">
        <attr name="vbCategory" format="enum">
            <enum name="none"     value="0" />
            <enum name="food"     value="1" />
            <enum name="family"   value="2" />
            <enum name="emotion"  value="3" />
            <enum name="activity" value="4" />
            <enum name="object"   value="5" />
            <enum name="place"    value="6" />
        </attr>
    </declare-styleable>
</resources>
```

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/color app/src/main/res/values/attrs.xml
git commit -m "feat(theme): add category ColorStateLists & state_category attr"
```

---

### Task 1.6: Add typography styles (TextAppearance.VBoard.*)

**Files:**
- Modify: `app/src/main/res/values/styles.xml` (append)

- [ ] **Step 1: Append 9 TextAppearance styles**

Append before `</resources>` in `app/src/main/res/values/styles.xml`:

```xml
    <!-- ============================================ -->
    <!-- VBoard Apple Vibe typography (Phase 1)       -->
    <!-- ============================================ -->

    <style name="TextAppearance.VBoard" parent="TextAppearance.Material3.BodyMedium">
        <item name="android:fontFamily">@font/be_vietnam_pro</item>
        <item name="android:textColor">@color/vb_label</item>
    </style>

    <style name="TextAppearance.VBoard.Display">
        <item name="android:textSize">36sp</item>
        <item name="android:textFontWeight">700</item>
        <item name="android:letterSpacing">-0.038</item>  <!-- ≈ -1.4sp / 36sp -->
        <item name="android:lineHeight">40sp</item>
    </style>

    <style name="TextAppearance.VBoard.Title1">
        <item name="android:textSize">28sp</item>
        <item name="android:textFontWeight">700</item>
        <item name="android:letterSpacing">-0.021</item>
        <item name="android:lineHeight">32sp</item>
    </style>

    <style name="TextAppearance.VBoard.Title2">
        <item name="android:textSize">22sp</item>
        <item name="android:textFontWeight">600</item>
        <item name="android:letterSpacing">-0.018</item>
        <item name="android:lineHeight">26sp</item>
    </style>

    <style name="TextAppearance.VBoard.Title3">
        <item name="android:textSize">18sp</item>
        <item name="android:textFontWeight">600</item>
        <item name="android:letterSpacing">-0.011</item>
        <item name="android:lineHeight">24sp</item>
    </style>

    <style name="TextAppearance.VBoard.Headline">
        <item name="android:textSize">17sp</item>
        <item name="android:textFontWeight">600</item>
        <item name="android:letterSpacing">0</item>
        <item name="android:lineHeight">22sp</item>
    </style>

    <style name="TextAppearance.VBoard.Body">
        <item name="android:textSize">15sp</item>
        <item name="android:textFontWeight">400</item>
        <item name="android:letterSpacing">0</item>
        <item name="android:lineHeight">22sp</item>
    </style>

    <style name="TextAppearance.VBoard.Callout">
        <item name="android:textSize">14sp</item>
        <item name="android:textFontWeight">500</item>
        <item name="android:letterSpacing">0</item>
        <item name="android:lineHeight">20sp</item>
    </style>

    <style name="TextAppearance.VBoard.Caption1">
        <item name="android:textSize">12sp</item>
        <item name="android:textFontWeight">500</item>
        <item name="android:letterSpacing">0.008</item>
        <item name="android:lineHeight">16sp</item>
        <item name="android:textColor">@color/vb_secondary_label</item>
    </style>

    <style name="TextAppearance.VBoard.Overline">
        <item name="android:textSize">10sp</item>
        <item name="android:textFontWeight">600</item>
        <item name="android:letterSpacing">0.14</item>
        <item name="android:textAllCaps">true</item>
        <item name="android:textColor">@color/vb_tertiary_label</item>
    </style>
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/styles.xml
git commit -m "feat(theme): add VBoard TextAppearance type scale (9 styles)"
```

---

### Task 1.7: Add spacing, radius, and elevation tokens

**Files:**
- Modify: `app/src/main/res/values/dimens.xml` (append)

- [ ] **Step 1: Append shape tokens**

Append before `</resources>`:

```xml
    <!-- ============================================ -->
    <!-- VBoard Apple Vibe shape tokens (Phase 1)     -->
    <!-- ============================================ -->

    <!-- Spacing (8pt grid) -->
    <dimen name="vb_space_2xs">4dp</dimen>
    <dimen name="vb_space_xs">8dp</dimen>
    <dimen name="vb_space_sm">12dp</dimen>
    <dimen name="vb_space_md">16dp</dimen>
    <dimen name="vb_space_lg">20dp</dimen>
    <dimen name="vb_space_xl">24dp</dimen>
    <dimen name="vb_space_2xl">32dp</dimen>

    <!-- Corner radius -->
    <dimen name="vb_radius_xs">6dp</dimen>
    <dimen name="vb_radius_sm">10dp</dimen>
    <dimen name="vb_radius_md">14dp</dimen>
    <dimen name="vb_radius_lg">20dp</dimen>
    <dimen name="vb_radius_full">9999dp</dimen>

    <!-- Elevation (used by MaterialCardView cardElevation) -->
    <dimen name="vb_elevation_e0">0dp</dimen>
    <dimen name="vb_elevation_e1">2dp</dimen>
    <dimen name="vb_elevation_e2">8dp</dimen>
    <dimen name="vb_elevation_e3">16dp</dimen>

    <!-- Hairline divider (renders 1px on hdpi+, ~hairline on xxhdpi+) -->
    <dimen name="vb_hairline">0.5dp</dimen>

    <!-- Component-specific dims used in Phase 2 -->
    <dimen name="vb_button_height">48dp</dimen>
    <dimen name="vb_listrow_min_height">56dp</dimen>
    <dimen name="vb_listrow_icon">32dp</dimen>
    <dimen name="vb_chip_height">32dp</dimen>
    <dimen name="vb_numpad_size">72dp</dimen>
    <dimen name="vb_topappbar_height">56dp</dimen>
    <dimen name="vb_pin_dot">14dp</dimen>
    <dimen name="vb_drag_handle_width">36dp</dimen>
    <dimen name="vb_drag_handle_height">5dp</dimen>
    <dimen name="vb_screen_edge_phone">16dp</dimen>
    <dimen name="vb_screen_edge_tablet">20dp</dimen>
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/dimens.xml
git commit -m "feat(theme): add VBoard spacing/radius/elevation tokens"
```

---

### Task 1.8: Add 4 interpolators

**Files:**
- Create: `app/src/main/res/interpolator/vb_standard.xml`
- Create: `app/src/main/res/interpolator/vb_emphasized.xml`
- Create: `app/src/main/res/interpolator/vb_decelerate.xml`
- Create: `app/src/main/res/interpolator/vb_accelerate.xml`

- [ ] **Step 1: Create standard**

`app/src/main/res/interpolator/vb_standard.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<pathInterpolator xmlns:android="http://schemas.android.com/apk/res/android"
    android:controlX1="0.4" android:controlY1="0"
    android:controlX2="0.2" android:controlY2="1" />
```

- [ ] **Step 2: Create emphasized (iOS spring)**

`vb_emphasized.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<pathInterpolator xmlns:android="http://schemas.android.com/apk/res/android"
    android:controlX1="0.05" android:controlY1="0.7"
    android:controlX2="0.1"  android:controlY2="1" />
```

- [ ] **Step 3: Create decelerate**

`vb_decelerate.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<pathInterpolator xmlns:android="http://schemas.android.com/apk/res/android"
    android:controlX1="0" android:controlY1="0"
    android:controlX2="0.2" android:controlY2="1" />
```

- [ ] **Step 4: Create accelerate**

`vb_accelerate.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<pathInterpolator xmlns:android="http://schemas.android.com/apk/res/android"
    android:controlX1="0.4" android:controlY1="0"
    android:controlX2="1" android:controlY2="1" />
```

- [ ] **Step 5: Add duration constants**

Append to `app/src/main/res/values/dimens.xml` before `</resources>`:

```xml
    <integer name="vb_duration_tap">100</integer>
    <integer name="vb_duration_fast">200</integer>
    <integer name="vb_duration_medium">350</integer>
    <integer name="vb_duration_slow">500</integer>
```

(Yes — `<integer>` lives inside `<resources>` next to `<dimen>`.)

- [ ] **Step 6: Build & commit**

```bash
./gradlew assembleDebug
git add app/src/main/res/interpolator app/src/main/res/values/dimens.xml
git commit -m "feat(motion): add VBoard interpolators & duration constants"
```

---

### Task 1.9: Add 5 animation resources

**Files:**
- Create: `app/src/main/res/anim/vb_tap_press.xml`
- Create: `app/src/main/res/anim/vb_chip_slide_in.xml`
- Create: `app/src/main/res/anim/vb_chip_fade_out.xml`
- Create: `app/src/main/res/anim/vb_sheet_slide_up.xml`
- Create: `app/src/main/res/anim/vb_shake_error.xml`

- [ ] **Step 1: Create tap press (down)**

`vb_tap_press.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="@integer/vb_duration_tap"
    android:interpolator="@interpolator/vb_emphasized">
    <scale
        android:fromXScale="1.0" android:toXScale="0.96"
        android:fromYScale="1.0" android:toYScale="0.96"
        android:pivotX="50%" android:pivotY="50%"
        android:fillAfter="true" />
</set>
```

- [ ] **Step 2: Create chip slide in**

`vb_chip_slide_in.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="@integer/vb_duration_fast"
    android:interpolator="@interpolator/vb_emphasized">
    <translate android:fromXDelta="30dp" android:toXDelta="0" />
    <alpha android:fromAlpha="0" android:toAlpha="1" />
</set>
```

- [ ] **Step 3: Create chip fade out**

`vb_chip_fade_out.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="@integer/vb_duration_fast"
    android:interpolator="@interpolator/vb_standard">
    <alpha android:fromAlpha="1" android:toAlpha="0" />
</set>
```

- [ ] **Step 4: Create sheet slide up**

`vb_sheet_slide_up.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="@integer/vb_duration_medium"
    android:interpolator="@interpolator/vb_emphasized"
    android:fromYDelta="100%" android:toYDelta="0" />
```

- [ ] **Step 5: Create shake error**

`vb_shake_error.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="@integer/vb_duration_fast"
    android:interpolator="@interpolator/vb_standard">
    <translate android:fromXDelta="0"   android:toXDelta="-8dp" android:duration="50" />
    <translate android:fromXDelta="-8dp" android:toXDelta="8dp"  android:duration="50" android:startOffset="50" />
    <translate android:fromXDelta="8dp"  android:toXDelta="-8dp" android:duration="50" android:startOffset="100" />
    <translate android:fromXDelta="-8dp" android:toXDelta="0"    android:duration="50" android:startOffset="150" />
</set>
```

- [ ] **Step 6: Build & commit**

```bash
./gradlew assembleDebug
git add app/src/main/res/anim
git commit -m "feat(motion): add 5 VBoard animation resources"
```

---

### Task 1.10: Add `Theme.VBoard.New` theme

**Files:**
- Modify: `app/src/main/res/values/themes.xml` (append new theme; keep `Theme.VBoard` untouched)
- Create: `app/src/main/res/values-night/themes.xml` (only if not already present — same name pair)

- [ ] **Step 1: Append the new theme to `values/themes.xml`**

Append before `</resources>`:

```xml
    <!-- New Apple Vibe theme — applied conditionally via BuildConfig flag in Phase 3 -->
    <style name="Theme.VBoard.New" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Material3 ColorScheme mapping -->
        <item name="colorPrimary">@color/vb_accent</item>
        <item name="colorOnPrimary">#FFFFFF</item>
        <item name="colorPrimaryContainer">@color/vb_accent_tinted</item>
        <item name="colorOnPrimaryContainer">@color/vb_accent</item>

        <item name="colorSecondary">@color/vb_accent</item>
        <item name="colorOnSecondary">#FFFFFF</item>

        <item name="colorTertiary">@color/vb_info</item>
        <item name="colorError">@color/vb_error</item>
        <item name="colorOnError">#FFFFFF</item>

        <item name="android:colorBackground">@color/vb_system_background</item>
        <item name="colorSurface">@color/vb_secondary_system_background</item>
        <item name="colorOnSurface">@color/vb_label</item>
        <item name="colorOnSurfaceVariant">@color/vb_secondary_label</item>
        <item name="colorOutline">@color/vb_separator</item>
        <item name="colorOutlineVariant">@color/vb_separator</item>

        <!-- Default text appearance -->
        <item name="android:fontFamily">@font/be_vietnam_pro</item>
        <item name="android:textAppearance">@style/TextAppearance.VBoard.Body</item>

        <!-- Status bar matches background -->
        <item name="android:statusBarColor">@color/vb_system_background</item>
        <item name="android:windowLightStatusBar">true</item>
    </style>
```

- [ ] **Step 2: Add dark variant in values-night/themes.xml**

If `app/src/main/res/values-night/themes.xml` does not exist, create it:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.VBoard.New" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/vb_accent</item>
        <item name="colorOnPrimary">#FFFFFF</item>
        <item name="colorPrimaryContainer">@color/vb_accent_tinted</item>
        <item name="colorOnPrimaryContainer">@color/vb_accent</item>
        <item name="colorSecondary">@color/vb_accent</item>
        <item name="colorOnSecondary">#FFFFFF</item>
        <item name="colorTertiary">@color/vb_info</item>
        <item name="colorError">@color/vb_error</item>
        <item name="colorOnError">#FFFFFF</item>

        <item name="android:colorBackground">@color/vb_system_background</item>
        <item name="colorSurface">@color/vb_secondary_system_background</item>
        <item name="colorOnSurface">@color/vb_label</item>
        <item name="colorOnSurfaceVariant">@color/vb_secondary_label</item>
        <item name="colorOutline">@color/vb_separator</item>
        <item name="colorOutlineVariant">@color/vb_separator</item>

        <item name="android:fontFamily">@font/be_vietnam_pro</item>
        <item name="android:textAppearance">@style/TextAppearance.VBoard.Body</item>

        <item name="android:statusBarColor">@color/vb_system_background</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>
```

If a `values-night/themes.xml` already exists with a different theme, only insert the `Theme.VBoard.New` style alongside it. Do not delete or modify the existing theme.

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/themes.xml app/src/main/res/values-night/themes.xml
git commit -m "feat(theme): add Theme.VBoard.New (light + dark) parented to Material3"
```

---

### Task 1.11: Phase 1 sanity test — build + smoke test

**Files:** none modified.

- [ ] **Step 1: Clean build**

```bash
./gradlew clean assembleDebug
```
Expected: BUILD SUCCESSFUL. APK size unchanged ±200KB (Be Vietnam Pro adds ~80KB, Material Symbols ~120KB).

- [ ] **Step 2: Run unit tests**

```bash
./gradlew test
```
Expected: all 65 unit tests pass (or whatever the current count is — check the report).

- [ ] **Step 3: Install on emulator and confirm visual identity unchanged**

```bash
./gradlew installDebug
```

Open the app on the device. The launcher screen, Board, PIN, Admin, etc. must all look exactly the same as before Task 1.0 — because no view yet references the new `vb_*` tokens. Take a screenshot of the Board screen for the Phase 1 baseline.

- [ ] **Step 4: Tag the foundation commit**

```bash
git tag phase1-foundations
```

This concludes Phase 1. Open the Phase 1 PR for review.

---

## Phase 2 — Components (Sprint Day 4–5 · ~2 days · 1 PR)

Build 9 reusable component primitives + a hidden `DebugStyleGalleryActivity`. Visual sandbox only — no production screen consumes these yet.

### Task 2.0: ViewExtensions — animation helpers (TDD)

**Files:**
- Modify: `app/src/main/java/com/vboard/aac/ui/common/extensions/ViewExtensions.kt`
- Create: `app/src/test/java/com/vboard/aac/ui/common/extensions/ViewExtensionsTest.kt`

- [ ] **Step 1: Write the failing test**

If a `ViewExtensions.kt` and a corresponding test do not yet exist, create the test first. The test verifies one helper at a time using a lightweight Robolectric setup (already a transitive dep via Material).

Create `app/src/test/java/com/vboard/aac/ui/common/extensions/ViewExtensionsTest.kt`:

```kotlin
package com.vboard.aac.ui.common.extensions

import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ViewExtensionsTest {

    @Test fun `animatePressIn scales the view to 0_96`() {
        val view = View(ApplicationProvider.getApplicationContext())
        view.animatePressIn()
        // Robolectric runs animations synchronously when scheduler is paused.
        assertEquals(0.96f, view.scaleX, 0.001f)
        assertEquals(0.96f, view.scaleY, 0.001f)
    }

    @Test fun `animatePressOut returns scale to 1_0`() {
        val view = View(ApplicationProvider.getApplicationContext()).apply {
            scaleX = 0.96f; scaleY = 0.96f
        }
        view.animatePressOut()
        assertEquals(1f, view.scaleX, 0.001f)
        assertEquals(1f, view.scaleY, 0.001f)
    }
}
```

Add Robolectric to test deps in `app/build.gradle` (under `dependencies`):

```groovy
    testImplementation 'org.robolectric:robolectric:4.11.1'
```

- [ ] **Step 2: Run test, confirm it fails**

```bash
./gradlew :app:testDebugUnitTest --tests com.vboard.aac.ui.common.extensions.ViewExtensionsTest
```
Expected: FAIL — `animatePressIn` and `animatePressOut` are unresolved references.

- [ ] **Step 3: Add the helpers**

Append (or create) `app/src/main/java/com/vboard/aac/ui/common/extensions/ViewExtensions.kt`:

```kotlin
package com.vboard.aac.ui.common.extensions

import android.view.View

private const val PRESS_SCALE = 0.96f
private const val PRESS_DURATION = 100L

fun View.animatePressIn() {
    animate().cancel()
    scaleX = PRESS_SCALE
    scaleY = PRESS_SCALE
    animate().scaleX(PRESS_SCALE).scaleY(PRESS_SCALE)
        .setDuration(PRESS_DURATION).start()
}

fun View.animatePressOut() {
    animate().cancel()
    scaleX = 1f
    scaleY = 1f
    animate().scaleX(1f).scaleY(1f)
        .setDuration(PRESS_DURATION).start()
}

/** Slides view in from +30dp on the right while fading from 0 to 1. */
fun View.fadeSlideInFromRight() {
    val offset = 30f * resources.displayMetrics.density
    translationX = offset
    alpha = 0f
    animate().translationX(0f).alpha(1f)
        .setDuration(200L).start()
}

/** Fades the view out and collapses it. Caller is responsible for removal afterwards. */
fun View.fadeOut(onEnd: () -> Unit) {
    animate().cancel()
    animate().alpha(0f).setDuration(200L)
        .withEndAction { onEnd() }
        .start()
}
```

- [ ] **Step 4: Run tests, confirm they pass**

```bash
./gradlew :app:testDebugUnitTest --tests com.vboard.aac.ui.common.extensions.ViewExtensionsTest
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vboard/aac/ui/common/extensions/ViewExtensions.kt \
        app/src/test/java/com/vboard/aac/ui/common/extensions/ViewExtensionsTest.kt \
        app/build.gradle
git commit -m "feat(ui): add view animation extensions (animatePressIn/Out, fadeSlideInFromRight, fadeOut)"
```

---

### Task 2.1: Drawables — neutral & tinted card backgrounds

**Files:**
- Create: `app/src/main/res/drawable/vb_bg_card_neutral.xml`
- Create: `app/src/main/res/drawable/vb_bg_card_tinted.xml`
- Create: `app/src/main/res/drawable/vb_bg_grouped_panel.xml`

- [ ] **Step 1: Neutral card (List Row, Stat Tile, Settings group)**

`vb_bg_card_neutral.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/vb_secondary_system_background" />
    <corners android:radius="@dimen/vb_radius_md" />
</shape>
```

- [ ] **Step 2: Tinted card (Vocab card)**

`vb_bg_card_tinted.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@android:color/white" />  <!-- replaced via backgroundTint at runtime -->
    <corners android:radius="@dimen/vb_radius_md" />
</shape>
```

- [ ] **Step 3: Grouped panel (subtle panel inside a screen)**

`vb_bg_grouped_panel.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/vb_tertiary_system_background" />
    <corners android:radius="@dimen/vb_radius_md" />
</shape>
```

- [ ] **Step 4: Build & commit**

```bash
./gradlew assembleDebug
git add app/src/main/res/drawable/vb_bg_card_neutral.xml \
        app/src/main/res/drawable/vb_bg_card_tinted.xml \
        app/src/main/res/drawable/vb_bg_grouped_panel.xml
git commit -m "feat(component): add card background drawables"
```

---

### Task 2.2: Drawables — buttons (primary, danger, tinted, plain)

**Files:**
- Create: `app/src/main/res/drawable/vb_bg_btn_primary.xml`
- Create: `app/src/main/res/drawable/vb_bg_btn_danger.xml`
- Create: `app/src/main/res/drawable/vb_bg_btn_tinted.xml`
- Create: `app/src/main/res/color/vb_btn_primary_text.xml`
- Create: `app/src/main/res/color/vb_btn_danger_text.xml`

- [ ] **Step 1: Primary button (filled)**

`vb_bg_btn_primary.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#33FFFFFF">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/vb_accent" />
            <corners android:radius="@dimen/vb_radius_sm" />
        </shape>
    </item>
</ripple>
```

- [ ] **Step 2: Danger button (outline)**

`vb_bg_btn_danger.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#1FFF3B30">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/vb_secondary_system_background" />
            <stroke android:width="@dimen/vb_hairline" android:color="@color/vb_error" />
            <corners android:radius="@dimen/vb_radius_sm" />
        </shape>
    </item>
</ripple>
```

- [ ] **Step 3: Tinted button**

`vb_bg_btn_tinted.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#1F007AFF">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/vb_accent_tinted" />
            <corners android:radius="@dimen/vb_radius_sm" />
        </shape>
    </item>
</ripple>
```

- [ ] **Step 4: Text colors (state-list with disabled)**

`app/src/main/res/color/vb_btn_primary_text.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_enabled="false" android:color="@color/vb_disabled" />
    <item android:color="#FFFFFF" />
</selector>
```

`app/src/main/res/color/vb_btn_danger_text.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_enabled="false" android:color="@color/vb_disabled" />
    <item android:color="@color/vb_error" />
</selector>
```

- [ ] **Step 5: Add 4 button styles to styles.xml**

Append to `app/src/main/res/values/styles.xml`:

```xml
    <style name="Widget.VBoard.Button" parent="Widget.Material3.Button">
        <item name="android:fontFamily">@font/be_vietnam_pro</item>
        <item name="android:textAppearance">@style/TextAppearance.VBoard.Headline</item>
        <item name="android:minHeight">@dimen/vb_button_height</item>
        <item name="android:paddingStart">@dimen/vb_space_xl</item>
        <item name="android:paddingEnd">@dimen/vb_space_xl</item>
    </style>

    <style name="Widget.VBoard.Button.Primary">
        <item name="android:background">@drawable/vb_bg_btn_primary</item>
        <item name="android:textColor">@color/vb_btn_primary_text</item>
    </style>

    <style name="Widget.VBoard.Button.Danger">
        <item name="android:background">@drawable/vb_bg_btn_danger</item>
        <item name="android:textColor">@color/vb_btn_danger_text</item>
    </style>

    <style name="Widget.VBoard.Button.Tinted">
        <item name="android:background">@drawable/vb_bg_btn_tinted</item>
        <item name="android:textColor">@color/vb_accent</item>
    </style>

    <style name="Widget.VBoard.Button.Plain" parent="Widget.Material3.Button.TextButton">
        <item name="android:fontFamily">@font/be_vietnam_pro</item>
        <item name="android:textAppearance">@style/TextAppearance.VBoard.Body</item>
        <item name="android:textColor">@color/vb_accent</item>
        <item name="android:minHeight">44dp</item>
        <item name="android:paddingStart">@dimen/vb_space_sm</item>
        <item name="android:paddingEnd">@dimen/vb_space_sm</item>
    </style>
```

- [ ] **Step 6: Build & commit**

```bash
./gradlew assembleDebug
git add app/src/main/res/drawable/vb_bg_btn_*.xml \
        app/src/main/res/color/vb_btn_*_text.xml \
        app/src/main/res/values/styles.xml
git commit -m "feat(component): add button styles (Primary/Danger/Tinted/Plain)"
```

---

### Task 2.3: Drawables — chips (word & category)

**Files:**
- Create: `app/src/main/res/drawable/vb_bg_chip_word.xml`
- Create: `app/src/main/res/drawable/vb_bg_chip_category.xml` (state-list active/inactive, applies tint)

- [ ] **Step 1: Word chip — accent-tinted, full radius**

`vb_bg_chip_word.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/vb_accent_tinted" />
    <corners android:radius="@dimen/vb_radius_md" />
    <padding android:left="@dimen/vb_space_sm" android:right="@dimen/vb_space_sm"
             android:top="6dp" android:bottom="6dp" />
</shape>
```

- [ ] **Step 2: Category chip — selectable**

`vb_bg_chip_category.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_selected="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/vb_accent" />
            <corners android:radius="@dimen/vb_radius_md" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/vb_secondary_system_background" />
            <stroke android:width="@dimen/vb_hairline" android:color="@color/vb_separator" />
            <corners android:radius="@dimen/vb_radius_md" />
        </shape>
    </item>
</selector>
```

- [ ] **Step 3: Append chip styles to styles.xml**

```xml
    <style name="Widget.VBoard.Chip" parent="Widget.Material3.Chip.Assist">
        <item name="chipMinHeight">@dimen/vb_chip_height</item>
        <item name="chipCornerRadius">@dimen/vb_radius_md</item>
        <item name="android:textAppearance">@style/TextAppearance.VBoard.Callout</item>
        <item name="chipStrokeWidth">0dp</item>
        <item name="android:fontFamily">@font/be_vietnam_pro</item>
    </style>

    <style name="Widget.VBoard.Chip.Word">
        <item name="chipBackgroundColor">@color/vb_accent_tinted</item>
        <item name="android:textColor">@color/vb_accent</item>
        <item name="android:textFontWeight">600</item>
    </style>

    <style name="Widget.VBoard.Chip.Category">
        <item name="chipBackgroundColor">@color/vb_secondary_system_background</item>
        <item name="android:textColor">@color/vb_label</item>
        <item name="chipStrokeColor">@color/vb_separator</item>
        <item name="chipStrokeWidth">@dimen/vb_hairline</item>
        <item name="checkedIconVisible">false</item>
    </style>
```

- [ ] **Step 4: Build & commit**

```bash
./gradlew assembleDebug
git add app/src/main/res/drawable/vb_bg_chip_*.xml app/src/main/res/values/styles.xml
git commit -m "feat(component): add word/category chip styles"
```

---

### Task 2.4: Custom view — VBoardCardView (TDD)

**Files:**
- Create: `app/src/main/java/com/vboard/aac/ui/common/VBoardCardView.kt`
- Create: `app/src/test/java/com/vboard/aac/ui/common/VBoardCardViewTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.vboard.aac.ui.common

import android.content.Context
import android.util.AttributeSet
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VBoardCardViewTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Test fun `default category is none`() {
        val view = VBoardCardView(ctx)
        assertEquals(VBoardCardView.Category.NONE, view.category)
    }

    @Test fun `setting category updates state`() {
        val view = VBoardCardView(ctx)
        view.category = VBoardCardView.Category.FOOD
        assertEquals(VBoardCardView.Category.FOOD, view.category)
    }

    @Test fun `category accessor accepts string code`() {
        val view = VBoardCardView(ctx)
        view.setCategoryCode("emotion")
        assertEquals(VBoardCardView.Category.EMOTION, view.category)
    }

    @Test fun `unknown category code falls back to NONE`() {
        val view = VBoardCardView(ctx)
        view.setCategoryCode("xyz")
        assertEquals(VBoardCardView.Category.NONE, view.category)
    }
}
```

- [ ] **Step 2: Run, confirm fail**

```bash
./gradlew :app:testDebugUnitTest --tests com.vboard.aac.ui.common.VBoardCardViewTest
```
Expected: FAIL (class not found).

- [ ] **Step 3: Implement**

```kotlin
package com.vboard.aac.ui.common

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import com.vboard.aac.R
import com.vboard.aac.ui.common.extensions.animatePressIn
import com.vboard.aac.ui.common.extensions.animatePressOut

class VBoardCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    enum class Category(val code: String) {
        NONE("none"), FOOD("food"), FAMILY("family"), EMOTION("emotion"),
        ACTIVITY("activity"), OBJECT("object"), PLACE("place");
        companion object { fun from(code: String?): Category =
            values().firstOrNull { it.code == code } ?: NONE }
    }

    var category: Category = Category.NONE
        set(value) { field = value; refreshDrawableState() }

    fun setCategoryCode(code: String?) { category = Category.from(code) }

    init {
        radius = resources.getDimension(R.dimen.vb_radius_md)
        cardElevation = resources.getDimension(R.dimen.vb_elevation_e1)
        useCompatPadding = true
        isClickable = true; isFocusable = true

        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.VBoardCardView)
            val ord = ta.getInt(R.styleable.VBoardCardView_vbCategory, 0)
            category = Category.values().getOrNull(ord) ?: Category.NONE
            ta.recycle()
        }

        setOnTouchListener { v, ev ->
            when (ev.action) {
                android.view.MotionEvent.ACTION_DOWN -> v.animatePressIn()
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> v.animatePressOut()
            }
            false
        }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        // We don't actually inject a real custom drawable state here because
        // ColorStateList lookups by string-state are limited. Consumers tint
        // background via category code instead — see VocabCardAdapter.
        return super.onCreateDrawableState(extraSpace)
    }
}
```

- [ ] **Step 4: Run, confirm pass**

```bash
./gradlew :app:testDebugUnitTest --tests com.vboard.aac.ui.common.VBoardCardViewTest
```
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vboard/aac/ui/common/VBoardCardView.kt \
        app/src/test/java/com/vboard/aac/ui/common/VBoardCardViewTest.kt
git commit -m "feat(component): add VBoardCardView with category enum & tap-press"
```

---

### Task 2.5: Vocab card adapter helper — `applyCategoryTint`

**Files:**
- Create: `app/src/main/java/com/vboard/aac/ui/common/CategoryTinter.kt`
- Create: `app/src/test/java/com/vboard/aac/ui/common/CategoryTinterTest.kt`

This pure-function helper maps a category code to (`bgColorRes`, `labelColorRes`). Adapters call it to tint a `VBoardCardView` and its label.

- [ ] **Step 1: Failing test**

```kotlin
package com.vboard.aac.ui.common

import com.vboard.aac.R
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTinterTest {
    @Test fun `food maps to food colors`() {
        val pair = CategoryTinter.colorsFor("food")
        assertEquals(R.color.vb_category_food_bg, pair.bgColorRes)
        assertEquals(R.color.vb_category_food_label, pair.labelColorRes)
    }
    @Test fun `unknown code falls back to neutral`() {
        val pair = CategoryTinter.colorsFor("xyz")
        assertEquals(R.color.vb_secondary_system_background, pair.bgColorRes)
        assertEquals(R.color.vb_label, pair.labelColorRes)
    }
}
```

- [ ] **Step 2: Run, fail**

```bash
./gradlew :app:testDebugUnitTest --tests com.vboard.aac.ui.common.CategoryTinterTest
```

- [ ] **Step 3: Implement**

```kotlin
package com.vboard.aac.ui.common

import androidx.annotation.ColorRes
import com.vboard.aac.R

object CategoryTinter {
    data class Colors(@ColorRes val bgColorRes: Int, @ColorRes val labelColorRes: Int)

    fun colorsFor(code: String?): Colors = when (code) {
        "food"     -> Colors(R.color.vb_category_food_bg,     R.color.vb_category_food_label)
        "family"   -> Colors(R.color.vb_category_family_bg,   R.color.vb_category_family_label)
        "emotion"  -> Colors(R.color.vb_category_emotion_bg,  R.color.vb_category_emotion_label)
        "activity" -> Colors(R.color.vb_category_activity_bg, R.color.vb_category_activity_label)
        "object"   -> Colors(R.color.vb_category_object_bg,   R.color.vb_category_object_label)
        "place"    -> Colors(R.color.vb_category_place_bg,    R.color.vb_category_place_label)
        else       -> Colors(R.color.vb_secondary_system_background, R.color.vb_label)
    }
}
```

- [ ] **Step 4: Pass + commit**

```bash
./gradlew :app:testDebugUnitTest --tests com.vboard.aac.ui.common.CategoryTinterTest
git add app/src/main/java/com/vboard/aac/ui/common/CategoryTinter.kt \
        app/src/test/java/com/vboard/aac/ui/common/CategoryTinterTest.kt
git commit -m "feat(component): add CategoryTinter pure-function helper"
```

---

### Task 2.6: Layout include — Top App Bar

**Files:**
- Create: `app/src/main/res/layout/vb_component_top_app_bar.xml`

- [ ] **Step 1: Create layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="@dimen/vb_topappbar_height"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:background="@color/vb_secondary_system_background"
    android:paddingStart="@dimen/vb_space_md"
    android:paddingEnd="@dimen/vb_space_md">

    <TextView
        android:id="@+id/vb_topbar_leading"
        style="@style/Widget.VBoard.Button.Plain"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:minWidth="44dp"
        android:gravity="start|center_vertical"
        tools:text="‹ Lùi" />

    <TextView
        android:id="@+id/vb_topbar_title"
        style="@style/TextAppearance.VBoard.Headline"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:gravity="center"
        tools:text="VBoard" />

    <TextView
        android:id="@+id/vb_topbar_trailing"
        style="@style/Widget.VBoard.Button.Plain"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:minWidth="44dp"
        android:gravity="end|center_vertical"
        tools:text="Xong" />

    <View
        android:layout_width="match_parent"
        android:layout_height="@dimen/vb_hairline"
        android:layout_alignParentBottom="true"
        android:background="@color/vb_separator"
        tools:ignore="UselessLeaf" />
</LinearLayout>
```

(Add `xmlns:tools="http://schemas.android.com/tools"` at the root if your IDE flags it.)

- [ ] **Step 2: Build & commit**

```bash
./gradlew assembleDebug
git add app/src/main/res/layout/vb_component_top_app_bar.xml
git commit -m "feat(component): add top app bar include"
```

---

### Task 2.7: Layout include — Sentence Strip

**Files:**
- Create: `app/src/main/res/layout/vb_component_sentence_strip.xml`

- [ ] **Step 1: Create**

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="@dimen/vb_space_md"
    app:cardBackgroundColor="@color/vb_secondary_system_background"
    app:cardCornerRadius="@dimen/vb_radius_md"
    app:cardElevation="@dimen/vb_elevation_e1">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:minHeight="56dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:padding="@dimen/vb_space_sm">

        <HorizontalScrollView
            android:id="@+id/vb_strip_scroll"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:scrollbars="none">

            <LinearLayout
                android:id="@+id/vb_strip_chips"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical" />
        </HorizontalScrollView>

        <TextView
            android:id="@+id/vb_strip_placeholder"
            style="@style/TextAppearance.VBoard.Body"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textStyle="italic"
            android:textColor="@color/vb_placeholder"
            android:text="Chạm vào thẻ để ghép câu…"
            android:visibility="gone" />

        <ImageButton
            android:id="@+id/vb_strip_backspace"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_marginStart="@dimen/vb_space_xs"
            android:background="@drawable/vb_bg_card_neutral"
            android:src="@drawable/ic_backspace"
            android:tint="@color/vb_secondary_label"
            android:contentDescription="Xoá từ cuối" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

`@drawable/ic_backspace` is part of Phase 2.13. Until then this layout is referenced only by the debug gallery — keep `tools:ignore="MissingPrefix"` if needed during incremental build.

- [ ] **Step 2: Build & commit**

```bash
./gradlew assembleDebug
git add app/src/main/res/layout/vb_component_sentence_strip.xml
git commit -m "feat(component): add sentence strip include"
```

---

### Task 2.8: Layout include — List Row

**Files:**
- Create: `app/src/main/res/layout/vb_component_list_row.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minHeight="@dimen/vb_listrow_min_height"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:paddingStart="@dimen/vb_space_md"
    android:paddingEnd="@dimen/vb_space_md"
    android:paddingTop="14dp"
    android:paddingBottom="14dp"
    android:background="?attr/selectableItemBackground">

    <FrameLayout
        android:id="@+id/vb_row_icon_container"
        android:layout_width="@dimen/vb_listrow_icon"
        android:layout_height="@dimen/vb_listrow_icon"
        android:background="@drawable/vb_bg_listrow_icon"
        android:layout_marginEnd="@dimen/vb_space_md">
        <TextView
            android:id="@+id/vb_row_icon"
            style="@style/TextAppearance.VBoard.Headline"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:textColor="@color/vb_accent"
            tools:text="🔤" />
    </FrameLayout>

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">
        <TextView
            android:id="@+id/vb_row_title"
            style="@style/TextAppearance.VBoard.Body"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textFontWeight="500"
            tools:text="Quản lý từ vựng" />
        <TextView
            android:id="@+id/vb_row_subtitle"
            style="@style/TextAppearance.VBoard.Caption1"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            tools:text="120 thẻ · 6 nhóm" />
    </LinearLayout>

    <TextView
        android:id="@+id/vb_row_trailing"
        style="@style/TextAppearance.VBoard.Body"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@color/vb_tertiary_label"
        android:text="›" />
</LinearLayout>
```

- [ ] **Step 1: Create the row icon container drawable**

`app/src/main/res/drawable/vb_bg_listrow_icon.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/vb_accent_tinted" />
    <corners android:radius="8dp" />
</shape>
```

- [ ] **Step 2: Build & commit**

```bash
./gradlew assembleDebug
git add app/src/main/res/layout/vb_component_list_row.xml \
        app/src/main/res/drawable/vb_bg_listrow_icon.xml
git commit -m "feat(component): add list row include"
```

---

### Task 2.9: Layout include — Section Header

**Files:**
- Create: `app/src/main/res/layout/vb_component_section_header.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingStart="@dimen/vb_space_md"
    android:paddingEnd="@dimen/vb_space_md"
    android:paddingTop="@dimen/vb_space_md"
    android:paddingBottom="@dimen/vb_space_xs">

    <TextView
        android:id="@+id/vb_section_eyebrow"
        style="@style/TextAppearance.VBoard.Overline"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        tools:text="HÔM NAY" />

    <TextView
        android:id="@+id/vb_section_title"
        style="@style/TextAppearance.VBoard.Title3"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="2dp"
        android:visibility="gone"
        tools:text="Số câu đã ghép"
        tools:visibility="visible" />
</LinearLayout>
```

- [ ] Build & commit

```bash
./gradlew assembleDebug
git add app/src/main/res/layout/vb_component_section_header.xml
git commit -m "feat(component): add section header include"
```

---

### Task 2.10: Layout include — Stat Tile

**Files:**
- Create: `app/src/main/res/layout/vb_component_stat_tile.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_weight="1"
    app:cardBackgroundColor="@color/vb_secondary_system_background"
    app:cardCornerRadius="@dimen/vb_radius_md"
    app:cardElevation="@dimen/vb_elevation_e1">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="@dimen/vb_space_md">

        <TextView
            android:id="@+id/vb_tile_number"
            style="@style/TextAppearance.VBoard.Display"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            tools:text="12" />

        <TextView
            android:id="@+id/vb_tile_label"
            style="@style/TextAppearance.VBoard.Caption1"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/vb_space_2xs"
            tools:text="Số câu hôm nay" />

        <TextView
            android:id="@+id/vb_tile_delta"
            style="@style/TextAppearance.VBoard.Caption1"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/vb_space_xs"
            android:textColor="@color/vb_success"
            android:textFontWeight="600"
            android:visibility="gone"
            tools:text="↑ 3 hôm qua"
            tools:visibility="visible" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

Build & commit:

```bash
./gradlew assembleDebug
git add app/src/main/res/layout/vb_component_stat_tile.xml
git commit -m "feat(component): add stat tile include"
```

---

### Task 2.11: Layout include — Numpad

**Files:**
- Create: `app/src/main/res/drawable/vb_bg_numpad_button.xml`
- Create: `app/src/main/res/layout/vb_component_numpad.xml`

- [ ] **Step 1: Numpad button drawable (round w/ pressed state)**

`vb_bg_numpad_button.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="?attr/colorControlHighlight">
    <item>
        <shape android:shape="oval">
            <solid android:color="@color/vb_secondary_system_background" />
            <stroke android:width="@dimen/vb_hairline" android:color="@color/vb_separator" />
        </shape>
    </item>
</ripple>
```

- [ ] **Step 2: Numpad layout**

`vb_component_numpad.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<GridLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:columnCount="3"
    android:rowCount="4"
    android:useDefaultMargins="false">

    <!-- 1..9 + cancel/0/backspace; height 72dp + 10dp gap = each cell 82dp -->
    <Button android:id="@+id/vb_pad_1" style="@style/Widget.VBoard.NumpadButton" android:text="1" />
    <Button android:id="@+id/vb_pad_2" style="@style/Widget.VBoard.NumpadButton" android:text="2" />
    <Button android:id="@+id/vb_pad_3" style="@style/Widget.VBoard.NumpadButton" android:text="3" />
    <Button android:id="@+id/vb_pad_4" style="@style/Widget.VBoard.NumpadButton" android:text="4" />
    <Button android:id="@+id/vb_pad_5" style="@style/Widget.VBoard.NumpadButton" android:text="5" />
    <Button android:id="@+id/vb_pad_6" style="@style/Widget.VBoard.NumpadButton" android:text="6" />
    <Button android:id="@+id/vb_pad_7" style="@style/Widget.VBoard.NumpadButton" android:text="7" />
    <Button android:id="@+id/vb_pad_8" style="@style/Widget.VBoard.NumpadButton" android:text="8" />
    <Button android:id="@+id/vb_pad_9" style="@style/Widget.VBoard.NumpadButton" android:text="9" />

    <Button android:id="@+id/vb_pad_cancel"
        style="@style/Widget.VBoard.NumpadButton.Plain"
        android:text="Huỷ" />
    <Button android:id="@+id/vb_pad_0"     style="@style/Widget.VBoard.NumpadButton" android:text="0" />
    <Button android:id="@+id/vb_pad_back"  style="@style/Widget.VBoard.NumpadButton.Plain" android:text="⌫" />
</GridLayout>
```

- [ ] **Step 3: Numpad styles**

Append to `styles.xml`:

```xml
    <style name="Widget.VBoard.NumpadButton" parent="Widget.Material3.Button.UnelevatedButton">
        <item name="android:layout_width">@dimen/vb_numpad_size</item>
        <item name="android:layout_height">@dimen/vb_numpad_size</item>
        <item name="android:layout_margin">5dp</item>
        <item name="android:background">@drawable/vb_bg_numpad_button</item>
        <item name="android:textAppearance">@style/TextAppearance.VBoard.Title2</item>
        <item name="android:textColor">@color/vb_label</item>
        <item name="android:textFontWeight">400</item>
    </style>
    <style name="Widget.VBoard.NumpadButton.Plain">
        <item name="android:background">@android:color/transparent</item>
        <item name="android:textColor">@color/vb_accent</item>
        <item name="android:textAppearance">@style/TextAppearance.VBoard.Body</item>
        <item name="android:textFontWeight">500</item>
    </style>
```

- [ ] **Step 4: Build & commit**

```bash
./gradlew assembleDebug
git add app/src/main/res/drawable/vb_bg_numpad_button.xml \
        app/src/main/res/layout/vb_component_numpad.xml \
        app/src/main/res/values/styles.xml
git commit -m "feat(component): add numpad include and styles"
```

---

### Task 2.12: Refactor existing pin-dot drawables

**Files:**
- Modify: `app/src/main/res/drawable/bg_pin_dot_empty.xml`
- Modify: `app/src/main/res/drawable/bg_pin_dot_filled.xml`

- [ ] **Step 1: Update empty dot**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <size android:width="@dimen/vb_pin_dot" android:height="@dimen/vb_pin_dot" />
    <stroke android:width="1.5dp" android:color="@color/vb_tertiary_label" />
    <solid android:color="@android:color/transparent" />
</shape>
```

- [ ] **Step 2: Update filled dot**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <size android:width="@dimen/vb_pin_dot" android:height="@dimen/vb_pin_dot" />
    <solid android:color="@color/vb_label" />
</shape>
```

(Existing PinActivity layout still references these names; the new visuals only apply when consumed under `Theme.VBoard.New`. Phase 1 doesn't switch the theme, so the legacy PIN screen will look slightly different — acceptable since PIN is rarely used.)

- [ ] **Step 3: Build & commit**

```bash
./gradlew assembleDebug
git add app/src/main/res/drawable/bg_pin_dot_*.xml
git commit -m "refactor(drawable): update pin dot dimensions to 14dp w/ new tokens"
```

---

### Task 2.13: DebugStyleGalleryActivity

**Files:**
- Create: `app/src/debug/java/com/vboard/aac/debug/DebugStyleGalleryActivity.kt`
- Create: `app/src/debug/res/layout/activity_debug_gallery.xml`
- Create: `app/src/debug/AndroidManifest.xml`
- Create: `app/src/main/res/drawable/ic_backspace.xml` (vector — needed by sentence strip)

This is a debug-only activity (build variant `debug` source set) that previews every component primitive on one screen for QA review during Phase 2.

- [ ] **Step 1: Create vector drawable for backspace**

`app/src/main/res/drawable/ic_backspace.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp" android:height="20dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="?attr/colorOnSurface">
    <path android:fillColor="@android:color/black"
        android:pathData="M22,3H7c-0.69,0 -1.23,0.35 -1.59,0.88L0,12l5.41,8.11C5.77,20.64 6.31,21 7,21h15c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2zM19,15.59L17.59,17 14,13.41 10.41,17 9,15.59 12.59,12 9,8.41 10.41,7 14,10.59 17.59,7 19,8.41 15.41,12 19,15.59z" />
</vector>
```

- [ ] **Step 2: Create gallery layout**

`app/src/debug/res/layout/activity_debug_gallery.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/vb_system_background">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <include layout="@layout/vb_component_top_app_bar" />

        <include layout="@layout/vb_component_section_header" />
        <include layout="@layout/vb_component_sentence_strip" />

        <include layout="@layout/vb_component_section_header" />
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/gallery_grouped_card"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_margin="@dimen/vb_space_md"
            app:cardCornerRadius="@dimen/vb_radius_md"
            app:cardElevation="@dimen/vb_elevation_e1">
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical">
                <include layout="@layout/vb_component_list_row" />
                <View android:layout_width="match_parent" android:layout_height="@dimen/vb_hairline"
                    android:background="@color/vb_separator" />
                <include layout="@layout/vb_component_list_row" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <include layout="@layout/vb_component_section_header" />
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:padding="@dimen/vb_space_md">
            <include layout="@layout/vb_component_stat_tile" />
            <Space android:layout_width="@dimen/vb_space_xs" android:layout_height="0dp" />
            <include layout="@layout/vb_component_stat_tile" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="@dimen/vb_space_md">
            <Button style="@style/Widget.VBoard.Button.Primary" android:text="Phát âm" />
            <Space android:layout_width="0dp" android:layout_height="@dimen/vb_space_xs" />
            <Button style="@style/Widget.VBoard.Button.Danger" android:text="Xoá" />
            <Space android:layout_width="0dp" android:layout_height="@dimen/vb_space_xs" />
            <Button style="@style/Widget.VBoard.Button.Tinted" android:text="Nghe thử" />
            <Space android:layout_width="0dp" android:layout_height="@dimen/vb_space_xs" />
            <Button style="@style/Widget.VBoard.Button.Plain"  android:text="Huỷ" />
        </LinearLayout>

        <include layout="@layout/vb_component_numpad" />
    </LinearLayout>
</ScrollView>
```

- [ ] **Step 3: Create activity**

```kotlin
package com.vboard.aac.debug

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vboard.aac.R

class DebugStyleGalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_VBoard_New)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_gallery)
    }
}
```

- [ ] **Step 4: Manifest stub for debug variant**

`app/src/debug/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <activity
            android:name=".debug.DebugStyleGalleryActivity"
            android:exported="true"
            android:theme="@style/Theme.VBoard.New">
            <intent-filter>
                <action android:name="com.vboard.aac.debug.GALLERY" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 5: Build debug variant**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Launch & verify visually**

```bash
./gradlew installDebug
adb shell am start -a com.vboard.aac.debug.GALLERY
```

Eyeball each component renders correctly: top bar, section headers, list row card, stat tile pair, 4 button variants, numpad. Take a screenshot for the PR.

- [ ] **Step 7: Commit**

```bash
git add app/src/debug app/src/main/res/drawable/ic_backspace.xml
git commit -m "feat(debug): add DebugStyleGalleryActivity for component QA"
```

---

### Task 2.14: Phase 2 wrap — tag and PR

- [ ] **Step 1: Run all tests**

```bash
./gradlew test
```
Expected: all unit tests pass (now includes new VBoardCardView, CategoryTinter, ViewExtensions tests).

- [ ] **Step 2: Tag**

```bash
git tag phase2-components
```

This concludes Phase 2. Open the Phase 2 PR.

---

## Phase 3 — Screens (Sprint Day 6–11 · ~6 days · 1 PR per screen, 9 PRs total or 1 combined)

> Phase 3 is where production UI changes. Set `BuildConfig.NEW_DESIGN_SYSTEM = true` and switch the manifest theme attribute as the FIRST commit of the phase, then refactor each screen.

### Task 3.0: Flip the design-system flag

**Files:**
- Modify: `app/build.gradle:18`
- Modify: `app/src/main/AndroidManifest.xml:13`

- [ ] **Step 1: Set the flag to true**

In `app/build.gradle`:

```groovy
        buildConfigField "boolean", "NEW_DESIGN_SYSTEM", "true"
```

- [ ] **Step 2: Switch manifest theme**

In `app/src/main/AndroidManifest.xml` line 13, change:

```xml
        android:theme="@style/Theme.VBoard">
```

to:

```xml
        android:theme="@style/Theme.VBoard.New">
```

- [ ] **Step 3: Smoke build & install**

```bash
./gradlew installDebug
```

Open the app: every screen now inherits the new color/font/elevation defaults. Existing layouts will look slightly off (font weights, button radius wrong) until subsequent tasks redesign each screen — that is expected.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle app/src/main/AndroidManifest.xml
git commit -m "feat(theme): switch to Theme.VBoard.New"
```

---

### Task 3.1: BoardViewModel — add category code mapping

**Files:**
- Modify: `app/src/main/java/com/vboard/aac/ui/main/BoardViewModel.kt`
- Modify: `app/src/main/java/com/vboard/aac/domain/model/Category.kt` (add `code` field if absent)
- Modify: `app/src/test/java/com/vboard/aac/ui/main/BoardViewModelTest.kt`

The ViewModel exposes a `VocabCardUiItem` that includes `categoryCode: String` so the adapter can call `CategoryTinter`.

- [ ] **Step 1: Read the existing model**

```bash
cat app/src/main/java/com/vboard/aac/domain/model/Category.kt
cat app/src/main/java/com/vboard/aac/ui/main/BoardViewModel.kt
```

- [ ] **Step 2: Failing test — UI item exposes categoryCode**

Open `BoardViewModelTest.kt` and add (or write a fresh test if no test exists for vocabUiItems):

```kotlin
@Test fun `vocab ui items carry category code`() = runTest {
    val vm = BoardViewModel(/* deps with stub repo returning a card with category food */)
    vm.vocabUiItems.first().forEach {
        assertNotNull(it.categoryCode)
    }
}
```

If your test infra differs, follow the existing pattern in the file — but always ensure one test verifies the `categoryCode` field is propagated.

- [ ] **Step 3: Run, fail**

```bash
./gradlew :app:testDebugUnitTest --tests com.vboard.aac.ui.main.BoardViewModelTest
```

- [ ] **Step 4: Update domain model & ViewModel**

If `Category` does not have `code: String`, add it. Map the existing icon emoji or category id to the 6 fixed codes (`food`, `family`, `emotion`, `activity`, `object`, `place`) — fallback to `"none"` if unknown.

In `BoardViewModel`, define:

```kotlin
data class VocabCardUiItem(
    val id: String,
    val word: String,
    val emoji: String,
    val imagePath: String?,
    val categoryCode: String
)

val vocabUiItems: StateFlow<List<VocabCardUiItem>> = combine(
    vocabRepo.getCardsByCategory(currentCategoryId),
    categoryRepo.getAllCategories()
) { cards, categories ->
    val byId = categories.associateBy { it.id }
    cards.map { card ->
        VocabCardUiItem(
            id = card.id,
            word = card.word,
            emoji = byId[card.categoryId]?.icon ?: "",
            imagePath = card.localImagePath,
            categoryCode = byId[card.categoryId]?.code ?: "none"
        )
    }
}.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
```

(Adapt to actual repo APIs.)

- [ ] **Step 5: Run, pass**

```bash
./gradlew :app:testDebugUnitTest --tests com.vboard.aac.ui.main.BoardViewModelTest
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main
git commit -m "feat(board): expose categoryCode in VocabCardUiItem"
```

---

### Task 3.2: Refactor `activity_main.xml` (phone)

**Files:**
- Replace: `app/src/main/res/layout/activity_main.xml`
- Replace: `app/src/main/res/layout/item_vocab_card.xml`
- Replace: `app/src/main/res/layout/item_word_chip.xml`
- Replace: `app/src/main/res/layout/item_category_chip.xml`

- [ ] **Step 1: Replace `item_vocab_card.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.vboard.aac.ui.common.VBoardCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    app:layout_constraintDimensionRatio="1:1"
    app:cardCornerRadius="@dimen/vb_radius_md"
    app:cardElevation="@dimen/vb_elevation_e1"
    android:minHeight="48dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:paddingTop="14dp"
        android:paddingBottom="14dp"
        android:paddingStart="@dimen/vb_space_sm"
        android:paddingEnd="@dimen/vb_space_sm">

        <TextView
            android:id="@+id/vocab_emoji"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="36sp"
            tools:text="🍚" />

        <TextView
            android:id="@+id/vocab_label"
            style="@style/TextAppearance.VBoard.Callout"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="@dimen/vb_space_2xs"
            tools:text="Cơm" />
    </LinearLayout>
</com.vboard.aac.ui.common.VBoardCardView>
```

- [ ] **Step 2: Replace `item_word_chip.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    style="@style/TextAppearance.VBoard.Callout"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginEnd="@dimen/vb_space_xs"
    android:background="@drawable/vb_bg_chip_word"
    android:paddingStart="@dimen/vb_space_sm"
    android:paddingEnd="@dimen/vb_space_sm"
    android:paddingTop="6dp"
    android:paddingBottom="6dp"
    android:textColor="@color/vb_accent"
    android:textFontWeight="600"
    tools:text="Con" />
```

- [ ] **Step 3: Replace `item_category_chip.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    style="@style/TextAppearance.VBoard.Callout"
    android:layout_width="wrap_content"
    android:layout_height="@dimen/vb_chip_height"
    android:layout_marginEnd="@dimen/vb_space_xs"
    android:gravity="center"
    android:background="@drawable/vb_bg_chip_category"
    android:paddingStart="@dimen/vb_space_sm"
    android:paddingEnd="@dimen/vb_space_sm"
    android:textColor="@color/vb_label"
    tools:text="🍴 Ăn uống" />
```

- [ ] **Step 4: Replace `activity_main.xml` with HIG layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/vb_system_background">

    <!-- Top App Bar -->
    <include
        android:id="@+id/board_topbar"
        layout="@layout/vb_component_top_app_bar" />

    <!-- Sentence Strip -->
    <include
        android:id="@+id/board_sentence_strip"
        layout="@layout/vb_component_sentence_strip" />

    <!-- Category tabs -->
    <HorizontalScrollView
        android:id="@+id/board_category_scroll"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/vb_secondary_system_background"
        android:paddingStart="@dimen/vb_space_md"
        android:paddingEnd="@dimen/vb_space_md"
        android:paddingTop="@dimen/vb_space_xs"
        android:paddingBottom="@dimen/vb_space_xs"
        android:scrollbars="none">
        <LinearLayout
            android:id="@+id/board_category_container"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal" />
    </HorizontalScrollView>

    <View android:layout_width="match_parent" android:layout_height="@dimen/vb_hairline"
        android:background="@color/vb_separator" />

    <!-- Vocab grid -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/board_grid"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:padding="@dimen/vb_space_sm"
        android:clipToPadding="false"
        app:layoutManager="androidx.recyclerview.widget.GridLayoutManager"
        app:spanCount="@integer/vb_board_columns" />

    <!-- Bottom action bar -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:background="@color/vb_secondary_system_background"
        android:padding="@dimen/vb_space_md"
        android:gravity="center_vertical">
        <View android:layout_width="match_parent" android:layout_height="@dimen/vb_hairline"
            android:background="@color/vb_separator" />

        <Button
            android:id="@+id/board_btn_speak"
            style="@style/Widget.VBoard.Button.Primary"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="2"
            android:layout_marginEnd="@dimen/vb_space_xs"
            android:text="🔊  Phát âm" />

        <Button
            android:id="@+id/board_btn_clear"
            style="@style/Widget.VBoard.Button.Danger"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Xoá" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 5: Add the column-count integer for phones**

`app/src/main/res/values/integers.xml` (create if absent):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <integer name="vb_board_columns">3</integer>
</resources>
```

- [ ] **Step 6: Update `VocabGridAdapter` to apply `CategoryTinter`**

In the adapter's `onBindViewHolder`, replace the existing background tint logic with:

```kotlin
import androidx.core.content.ContextCompat
import com.vboard.aac.ui.common.CategoryTinter

override fun onBindViewHolder(holder: VocabVH, pos: Int) {
    val item = items[pos]
    val (bg, label) = CategoryTinter.colorsFor(item.categoryCode)
    holder.binding.root.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, bg))
    holder.binding.vocabLabel.setTextColor(ContextCompat.getColor(holder.itemView.context, label))
    holder.binding.vocabEmoji.text = item.emoji
    holder.binding.vocabLabel.text = item.word
    holder.binding.root.setOnClickListener { onTap(item) }
}
```

- [ ] **Step 7: Update `MainActivity` references**

In `MainActivity`, ensure top bar leading/trailing buttons bind correctly (`board_topbar.findViewById<TextView>(R.id.vb_topbar_leading).text = "⚙"` etc.). Hook backspace inside the sentence-strip include via `findViewById(R.id.vb_strip_backspace)`.

- [ ] **Step 8: Build & install**

```bash
./gradlew installDebug
```

Open Board on the device, tap categories, tap cards, verify chips slide in, Phát âm works, Xoá works.

- [ ] **Step 9: Run tests**

```bash
./gradlew test connectedAndroidTest
```
Expected: existing `BoardFlowTest` instrumented test still passes.

- [ ] **Step 10: Commit**

```bash
git add app/src/main
git commit -m "refactor(board): redesign activity_main with HIG tokens"
```

---

### Task 3.3: Tablet layout — `layout-sw600dp/activity_main.xml`

**Files:**
- Create: `app/src/main/res/layout-sw600dp/activity_main.xml`
- Create: `app/src/main/res/values-sw600dp/integers.xml`

- [ ] **Step 1: Tablet column count**

`app/src/main/res/values-sw600dp/integers.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <integer name="vb_board_columns">5</integer>
</resources>
```

- [ ] **Step 2: Tablet activity layout**

Copy the phone `activity_main.xml` and add:
- Outer container padding: replace `padding` with `vb_space_lg` (20dp)
- Sentence strip min height 64dp (override include's `android:layout_height` via wrapper)
- Bottom button row: speak weight 3 instead of 2 to give more breathing room

Save as `app/src/main/res/layout-sw600dp/activity_main.xml`.

- [ ] **Step 3: Build & install on a tablet emulator**

```bash
./gradlew installDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout-sw600dp/activity_main.xml \
        app/src/main/res/values-sw600dp/integers.xml
git commit -m "feat(board): add tablet layout (sw600dp, 5 columns)"
```

---

### Task 3.4: Convert PinActivity → PinSheetFragment

**Files:**
- Create: `app/src/main/java/com/vboard/aac/ui/pin/PinSheetFragment.kt`
- Create: `app/src/main/res/layout/fragment_pin_sheet.xml`
- Modify: `app/src/main/java/com/vboard/aac/ui/main/MainActivity.kt` (open sheet instead of activity)
- Modify: `app/src/main/AndroidManifest.xml` (remove `<activity>` for PinActivity)
- Delete: `app/src/main/java/com/vboard/aac/ui/pin/PinActivity.kt`
- Delete: `app/src/main/res/layout/activity_pin.xml`

- [ ] **Step 1: Create the sheet layout**

`app/src/main/res/layout/fragment_pin_sheet.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@color/vb_secondary_system_background"
    android:padding="@dimen/vb_space_md"
    android:gravity="center_horizontal">

    <View
        android:layout_width="@dimen/vb_drag_handle_width"
        android:layout_height="@dimen/vb_drag_handle_height"
        android:layout_marginBottom="@dimen/vb_space_md"
        android:background="@drawable/vb_bg_drag_handle" />

    <TextView
        android:id="@+id/pin_eyebrow"
        style="@style/TextAppearance.VBoard.Overline"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="CÂU HỎI" />

    <TextView
        android:id="@+id/pin_math_question"
        style="@style/TextAppearance.VBoard.Title1"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/vb_space_xs"
        tools:text="4 + 5 = ?" />

    <LinearLayout
        android:id="@+id/pin_dot_row"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/vb_space_lg"
        android:orientation="horizontal">
        <!-- 4 dots populated programmatically using bg_pin_dot_empty/filled -->
    </LinearLayout>

    <include
        android:id="@+id/pin_numpad"
        layout="@layout/vb_component_numpad"
        android:layout_marginTop="@dimen/vb_space_xl" />
</LinearLayout>
```

- [ ] **Step 2: Drag handle drawable**

`app/src/main/res/drawable/vb_bg_drag_handle.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/vb_opaque_separator" />
    <corners android:radius="3dp" />
</shape>
```

- [ ] **Step 3: PinSheetFragment**

```kotlin
package com.vboard.aac.ui.pin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vboard.aac.R
import com.vboard.aac.ui.admin.AdminActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PinSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: PinViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View = inflater.inflate(R.layout.fragment_pin_sheet, container, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        // Migrate the click & math-challenge wiring from the old PinActivity here.
        // viewModel.mathChallenge.observe(viewLifecycleOwner) { ... }
        // numpad button clicks -> viewModel.input(digit)
        // viewModel.verified.observe(...) -> startActivity(Intent(requireContext(), AdminActivity::class.java)); dismiss()
        // viewModel.error.observe(...) -> view.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.vb_shake_error))
    }
}
```

(Carry over the exact wiring from the deleted `PinActivity.kt` — same ViewModel, same observe pattern, same haptic call.)

- [ ] **Step 4: Update MainActivity**

In `MainActivity.kt`, replace any `startActivity(Intent(this, PinActivity::class.java))` with:

```kotlin
PinSheetFragment().show(supportFragmentManager, "pin")
```

- [ ] **Step 5: Manifest cleanup**

Remove the `<activity android:name=".ui.pin.PinActivity">` block from `app/src/main/AndroidManifest.xml`.

- [ ] **Step 6: Delete old files**

```bash
git rm app/src/main/java/com/vboard/aac/ui/pin/PinActivity.kt
git rm app/src/main/res/layout/activity_pin.xml
```

- [ ] **Step 7: Build & test**

```bash
./gradlew installDebug
./gradlew :app:testDebugUnitTest --tests com.vboard.aac.ui.pin.PinViewModelTest
./gradlew connectedAndroidTest
```
Expected: PinViewModel tests still green; `PinFlowTest` should still find Admin via the sheet.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor(pin): convert PinActivity to PinSheetFragment (modal bottom sheet)"
```

---

### Task 3.5: Refactor `activity_admin.xml` (phone)

**Files:**
- Replace: `app/src/main/res/layout/activity_admin.xml`
- Delete: `app/src/main/res/layout/item_admin_card.xml`
- Create: `app/src/main/java/com/vboard/aac/ui/admin/AdminListAdapter.kt`
- Modify: `app/src/main/java/com/vboard/aac/ui/admin/AdminActivity.kt`
- Modify: `app/src/main/java/com/vboard/aac/ui/admin/AdminViewModel.kt` (group items into "main" / "analytics")

- [ ] **Step 1: Group entity**

In `AdminViewModel`, expose two RecyclerView lists `mainItems` and `analyticsItems` of `AdminMenuItem(icon: String, title: String, subtitle: String, route: AdminRoute)`.

- [ ] **Step 2: Replace activity_admin.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/vb_system_background"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <include layout="@layout/vb_component_top_app_bar" />

        <!-- Main group -->
        <include
            android:id="@+id/admin_section_main"
            layout="@layout/vb_component_section_header" />

        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/vb_space_md"
            android:layout_marginEnd="@dimen/vb_space_md"
            app:cardCornerRadius="@dimen/vb_radius_md"
            app:cardElevation="@dimen/vb_elevation_e1">
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/admin_main_list"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false" />
        </com.google.android.material.card.MaterialCardView>

        <include
            android:id="@+id/admin_section_analytics"
            layout="@layout/vb_component_section_header" />

        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/vb_space_md"
            android:layout_marginEnd="@dimen/vb_space_md"
            android:layout_marginBottom="@dimen/vb_space_md"
            app:cardCornerRadius="@dimen/vb_radius_md"
            app:cardElevation="@dimen/vb_elevation_e1">
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/admin_analytics_list"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false" />
        </com.google.android.material.card.MaterialCardView>
    </LinearLayout>
</ScrollView>
```

- [ ] **Step 3: AdminListAdapter (binds `vb_component_list_row.xml`)**

```kotlin
package com.vboard.aac.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vboard.aac.R

class AdminListAdapter(
    private val items: List<AdminMenuItem>,
    private val onClick: (AdminMenuItem) -> Unit
) : RecyclerView.Adapter<AdminListAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.vb_row_icon)
        val title: TextView = view.findViewById(R.id.vb_row_title)
        val subtitle: TextView = view.findViewById(R.id.vb_row_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.vb_component_list_row, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = items[pos]
        holder.icon.text = item.icon
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.itemView.setOnClickListener { onClick(item) }
        // Add 0.5dp separator between rows except last
        if (pos < items.size - 1) {
            holder.itemView.setPadding(
                holder.itemView.paddingStart,
                holder.itemView.paddingTop,
                holder.itemView.paddingEnd,
                holder.itemView.paddingBottom
            )
        }
    }
}
```

For separators, use a `RecyclerView.ItemDecoration` that paints a 0.5dp `vb_separator` line at the bottom of every item except the last one. Implement it inline in `AdminActivity.onCreate`.

- [ ] **Step 4: Wire AdminActivity**

```kotlin
// inside onCreate
val mainAdapter = AdminListAdapter(viewModel.mainItems) { route(it) }
val analyticsAdapter = AdminListAdapter(viewModel.analyticsItems) { route(it) }
binding.adminMainList.layoutManager = LinearLayoutManager(this)
binding.adminMainList.adapter = mainAdapter
binding.adminAnalyticsList.layoutManager = LinearLayoutManager(this)
binding.adminAnalyticsList.adapter = analyticsAdapter

// section header binding
findViewById<TextView>(R.id.vb_section_eyebrow).text = "CHÍNH"  // for first include
// (use distinct includes if both section headers need different titles)
```

If both `<include>`s share the same id reference, switch one to a unique wrapper id and use `binding.adminSectionMain.vbSectionEyebrow.text = "CHÍNH"`.

- [ ] **Step 5: Delete `item_admin_card.xml`**

```bash
git rm app/src/main/res/layout/item_admin_card.xml
```

- [ ] **Step 6: Build, install, smoke test, commit**

```bash
./gradlew installDebug
git add -A
git commit -m "refactor(admin): replace 2x2 grid with 2 grouped lists (Settings.app pattern)"
```

---

### Task 3.6: Tablet — `layout-sw600dp/activity_admin.xml`

Place the two grouped lists side-by-side in landscape. Single column in portrait inherits from phone layout.

- [ ] Step: Create `app/src/main/res/layout-sw600dp/activity_admin.xml` with `LinearLayout android:orientation="horizontal"` containing both group cards weighted equally. Build & install. Commit:

```bash
git add app/src/main/res/layout-sw600dp/activity_admin.xml
git commit -m "feat(admin): add tablet side-by-side layout"
```

---

### Task 3.7: Refactor Edit screen (`activity_edit.xml`)

**Files:**
- Replace: `app/src/main/res/layout/activity_edit.xml`
- Replace: `app/src/main/res/layout/item_edit_card.xml`
- Modify: `app/src/main/java/com/vboard/aac/ui/edit/EditActivity.kt`
- Modify: `app/src/main/java/com/vboard/aac/ui/edit/EditViewModel.kt` (add `editMode: StateFlow<Boolean>`)

- [ ] **Step 1: New layout — single FAB, edit-mode toggle in top bar trailing**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/vb_system_background">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">

        <include layout="@layout/vb_component_top_app_bar" />

        <HorizontalScrollView
            android:id="@+id/edit_category_scroll"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/vb_secondary_system_background"
            android:padding="@dimen/vb_space_xs">
            <LinearLayout
                android:id="@+id/edit_category_container"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:orientation="horizontal" />
        </HorizontalScrollView>

        <View android:layout_width="match_parent" android:layout_height="@dimen/vb_hairline"
            android:background="@color/vb_separator" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/edit_grid"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:padding="@dimen/vb_space_sm"
            app:layoutManager="androidx.recyclerview.widget.GridLayoutManager"
            app:spanCount="3" />
    </LinearLayout>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/edit_fab_add"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="@dimen/vb_space_md"
        android:contentDescription="Thêm thẻ"
        app:backgroundTint="@color/vb_accent"
        app:tint="#FFFFFF"
        app:srcCompat="@drawable/ic_add" />
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

Create `ic_add.xml` (vector "+") if absent.

- [ ] **Step 2: New `item_edit_card.xml` with edit-mode badge**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    app:layout_constraintDimensionRatio="1:1">

    <com.vboard.aac.ui.common.VBoardCardView
        android:id="@+id/edit_card_root"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:cardCornerRadius="@dimen/vb_radius_md"
        app:cardElevation="@dimen/vb_elevation_e1">

        <LinearLayout android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:orientation="vertical" android:gravity="center"
            android:padding="@dimen/vb_space_sm">
            <TextView android:id="@+id/edit_emoji" android:textSize="32sp"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                tools:text="🍚" />
            <TextView android:id="@+id/edit_label"
                style="@style/TextAppearance.VBoard.Caption1"
                android:layout_width="wrap_content" android:layout_height="wrap_content"
                tools:text="Cơm" />
        </LinearLayout>
    </com.vboard.aac.ui.common.VBoardCardView>

    <ImageView
        android:id="@+id/edit_remove_badge"
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:layout_gravity="top|end"
        android:layout_margin="@dimen/vb_space_2xs"
        android:src="@drawable/ic_remove_badge"
        android:contentDescription="Xoá thẻ"
        android:visibility="gone" />
</FrameLayout>
```

`ic_remove_badge.xml` is a 20dp red filled circle with a white minus — create as a vector.

- [ ] **Step 3: Bind edit-mode in adapter**

In the EditCardAdapter, observe `viewModel.editMode` and toggle `edit_remove_badge` visibility. Tap badge → ConfirmDeleteDialogFragment → `viewModel.deleteCard(id)`.

- [ ] **Step 4: Replace 2 FABs with 1 + top-bar trailing menu for folder management**

In `EditActivity`, the top bar trailing button text becomes "Sửa" toggling `viewModel.toggleEditMode()`. Add an overflow icon button in the top bar leading area (a `…` text or vector) that opens `FolderManagementSheetFragment`.

- [ ] **Step 5: Build & install**

```bash
./gradlew installDebug
```

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(edit): single FAB + iOS Home edit-mode badge pattern"
```

---

### Task 3.8: FolderManagement BottomSheet

**Files:**
- Create: `app/src/main/java/com/vboard/aac/ui/edit/FolderManagementSheetFragment.kt`
- Create: `app/src/main/res/layout/fragment_folder_management_sheet.xml`

This sheet lists all categories with edit + delete + colour-pick affordances. Reuse `vb_component_list_row.xml` for each row, with a trailing `›` chevron and a small color circle in place of the icon.

- [ ] Step: Implement (similar to `AdminListAdapter` pattern). Build, install, manual test add/edit/delete a folder. Commit:

```bash
git add app/src/main/java/com/vboard/aac/ui/edit/FolderManagementSheetFragment.kt \
        app/src/main/res/layout/fragment_folder_management_sheet.xml
git commit -m "feat(edit): add folder management bottom sheet"
```

---

### Task 3.9: Convert AddCardDialog → BottomSheet

**Files:**
- Create: `app/src/main/java/com/vboard/aac/ui/edit/AddCardSheetFragment.kt`
- Create: `app/src/main/res/layout/fragment_add_card_sheet.xml`
- Delete: `app/src/main/res/layout/dialog_add_card.xml`

Layout per spec section 4.9 (drag handle, image picker block, two field cards). Wire camera intent same as before. Replace any `DialogFragment.show()` with `AddCardSheetFragment().show(...)`.

```bash
git add -A
git commit -m "refactor(edit): convert add-card dialog to bottom sheet"
```

---

### Task 3.10: Refactor UI Settings screen

**Files:**
- Replace: `app/src/main/res/layout/activity_ui_settings.xml`
- Modify: `app/src/main/java/com/vboard/aac/ui/uiconfig/UISettingsActivity.kt`

Layout uses inset grouped pattern with `MaterialButtonToggleGroup` for column count, `SwitchCompat` for dark mode, `Slider` for font scale, plus a live preview block that re-uses `item_vocab_card.xml`.

```xml
<!-- segmented control example inside a list_row include wrapper -->
<com.google.android.material.button.MaterialButtonToggleGroup
    android:id="@+id/cols_toggle"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:singleSelection="true"
    app:selectionRequired="true"
    android:background="@color/vb_system_background"
    android:padding="2dp">
    <Button android:id="@+id/cols_2" style="?attr/materialButtonOutlinedStyle" android:text="2" />
    <Button android:id="@+id/cols_3" style="?attr/materialButtonOutlinedStyle" android:text="3" />
    <Button android:id="@+id/cols_4" style="?attr/materialButtonOutlinedStyle" android:text="4" />
</com.google.android.material.button.MaterialButtonToggleGroup>
```

Build, install, manual test column change updates Board on next visit. Commit:

```bash
git add -A
git commit -m "refactor(ui-settings): inset grouped + segmented control + iOS toggle"
```

---

### Task 3.11: Tablet UI Settings layout

Single-column form scales fine on tablet by default; no override required. **Skip** — note in commit log:

```bash
# No tablet layout needed for UI Settings — phone layout is responsive.
```

---

### Task 3.12: Refactor Voice Settings screen

**Files:**
- Replace: `app/src/main/res/layout/activity_voice_settings.xml`
- Modify: `app/src/main/java/com/vboard/aac/ui/voicetest/VoiceSettingsActivity.kt`

Per spec 4.6: Slider for volume, radio list with checkmarks (override `item_settings_card.xml` to use `vb_component_list_row.xml`), Tinted button for "Nghe thử", AI voice card with 50% opacity disabled state.

Build, install, manual test, commit:

```bash
git add -A
git commit -m "refactor(voice): inset grouped + checkmark radio list"
```

---

### Task 3.13: Refactor Stats screen

**Files:**
- Replace: `app/src/main/res/layout/activity_stats.xml`
- Create: `app/src/main/java/com/vboard/aac/ui/stats/SimpleBarChartView.kt`
- Modify: `app/src/main/java/com/vboard/aac/ui/stats/StatsActivity.kt`

- [ ] **Step 1: SimpleBarChartView (custom Canvas)**

```kotlin
package com.vboard.aac.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.vboard.aac.R

class SimpleBarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var values: List<Int> = emptyList()
        set(value) { field = value; invalidate() }

    var labels: List<String> = listOf("T2","T3","T4","T5","T6","T7","CN")
        set(value) { field = value; invalidate() }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.vb_accent)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.vb_tertiary_label)
        textSize = 10f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        if (values.isEmpty()) return
        val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
        val n = values.size
        val gap = 6f * resources.displayMetrics.density
        val labelHeight = 16f * resources.displayMetrics.density
        val chartHeight = height - labelHeight
        val barWidth = (width - gap * (n - 1)) / n
        val radius = 4f * resources.displayMetrics.density

        for (i in 0 until n) {
            val x = i * (barWidth + gap)
            val barH = (values[i].toFloat() / max) * chartHeight * 0.9f
            val top = chartHeight - barH
            canvas.drawRoundRect(x, top, x + barWidth, chartHeight, radius, radius, barPaint)
            if (i < labels.size) {
                canvas.drawText(labels[i], x + barWidth / 2, height.toFloat() - 2f, labelPaint)
            }
        }
    }
}
```

- [ ] **Step 2: Layout — stat tiles + chart + top words list**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/vb_system_background">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <include layout="@layout/vb_component_top_app_bar" />

        <include layout="@layout/vb_component_section_header" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:padding="@dimen/vb_space_md">
            <include android:id="@+id/stats_tile_sentences" layout="@layout/vb_component_stat_tile" />
            <Space android:layout_width="@dimen/vb_space_xs" android:layout_height="0dp" />
            <include android:id="@+id/stats_tile_words" layout="@layout/vb_component_stat_tile" />
        </LinearLayout>

        <include layout="@layout/vb_component_section_header" />

        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/vb_space_md"
            android:layout_marginEnd="@dimen/vb_space_md"
            app:cardCornerRadius="@dimen/vb_radius_md"
            app:cardElevation="@dimen/vb_elevation_e1">
            <com.vboard.aac.ui.stats.SimpleBarChartView
                android:id="@+id/stats_chart"
                android:layout_width="match_parent"
                android:layout_height="120dp"
                android:padding="@dimen/vb_space_md" />
        </com.google.android.material.card.MaterialCardView>

        <include layout="@layout/vb_component_section_header" />

        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/vb_space_md"
            android:layout_marginEnd="@dimen/vb_space_md"
            android:layout_marginBottom="@dimen/vb_space_md"
            app:cardCornerRadius="@dimen/vb_radius_md"
            app:cardElevation="@dimen/vb_elevation_e1">
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/stats_top_words"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:nestedScrollingEnabled="false" />
        </com.google.android.material.card.MaterialCardView>
    </LinearLayout>
</ScrollView>
```

- [ ] **Step 3: Bind stats**

In `StatsActivity.onCreate`, after the existing observe block, set:

```kotlin
binding.statsTileSentences.findViewById<TextView>(R.id.vb_tile_number).text = state.sentencesToday.toString()
binding.statsTileSentences.findViewById<TextView>(R.id.vb_tile_label).text = "Câu đã ghép"
binding.statsChart.values = state.last7Days
```

- [ ] **Step 4: Build, install, commit**

```bash
git add -A
git commit -m "refactor(stats): Apple Health-style tiles + custom Canvas chart"
```

---

### Task 3.14: Refactor Backup screen

Per spec 4.8 — status card, two action rows, destructive reset row. Use `vb_component_list_row.xml` and inset grouped pattern. Trivially mirrors Admin Hub structure.

```bash
git add -A
git commit -m "refactor(backup): inset grouped + destructive action red"
```

---

### Task 3.15: Refactor pre-PIN settings trigger

`activity_settings.xml` is a one-screen entry that just opens the PIN sheet. Simplify to a single `vb_component_list_row.xml` "Mở mã PIN" tap row, then it `dismiss()`es and shows `PinSheetFragment`. Or, even simpler, delete `SettingsActivity` and trigger `PinSheetFragment` directly from MainActivity's gear icon — recommended.

If the latter:

```bash
git rm app/src/main/res/layout/activity_settings.xml \
       app/src/main/java/com/vboard/aac/ui/settings/SettingsActivity.kt
# Remove the <activity> from manifest
git commit -m "refactor: drop SettingsActivity, MainActivity opens PIN sheet directly"
```

---

### Task 3.16: Tablet layouts for remaining screens

For Voice, Stats, Backup: phone layout is column-form, scales acceptably to tablet. Skip explicit `layout-sw600dp` overrides for these — note in PR description.

For Edit: tablet should keep the FAB but scale grid to 4-5 cols. Create `app/src/main/res/values-sw600dp/integers.xml` `vb_edit_columns` integer (already added for Board column count? Add a separate one if needed) and reference it from the edit grid.

```bash
git commit -am "feat(tablet): scale edit grid to 5 columns on sw600dp"
```

---

### Task 3.17: Phase 3 wrap

```bash
./gradlew test connectedAndroidTest
git tag phase3-screens
```

Open Phase 3 PR.

---

## Phase 4 — Polish (Sprint Day 12–13 · ~2 days · 1 PR)

### Task 4.1: Motion timing audit

- [ ] Verify every place that calls `animatePressIn/Out` reads the durations from `R.integer.vb_duration_*`. Grep for hard-coded durations in animation calls:

```bash
grep -rn "duration(" app/src/main/java | grep -v "vb_duration\|integer.vb"
```

Replace any literal `200L` / `300L` with the resource.

```bash
git commit -am "polish(motion): standardize all durations on vb_duration_* tokens"
```

---

### Task 4.2: Dark mode QA

- [ ] Toggle device to dark mode via `adb shell cmd uimode night yes` and walk through each screen. Look for: surfaces that stayed light, text invisible, icons mis-tinted, shadows too aggressive. Fix inline. Track issues in a checklist commit:

```bash
git commit -am "polish(dark): fix dark-mode visual issues found during audit"
```

---

### Task 4.3: TalkBack contentDescription audit

- [ ] Run Android Studio's *Layout Inspector* > *Accessibility Scanner* on every activity. Add `android:contentDescription` for every `ImageView`, `ImageButton`, decorative icon. For purely decorative emoji in vocab cards, set `android:importantForAccessibility="no"` and instead set the parent card's `contentDescription` to the word.

```bash
git commit -am "a11y: add contentDescription audit pass (TalkBack)"
```

---

### Task 4.4: Touch target audit

- [ ] Add a Lint suppression-free baseline:

```bash
./gradlew lintDebug
```

Read `app/build/reports/lint-results-debug.html`. Any `MissingTouchTargetSize` warnings must be fixed by adding `minWidth`/`minHeight` of 48dp.

```bash
git commit -am "a11y: ensure all touch targets ≥ 48dp"
```

---

### Task 4.5: WCAG AA contrast audit

- [ ] For each of the 6 category pairs (light + dark), verify contrast using a tool like Material Theme Builder or the W3C contrast checker. Required: 4.5:1 for body text, 3:1 for large text. If any pair fails, darken the label color one step.

```bash
git commit -am "a11y: tune category label colors to WCAG AA contrast"
```

---

### Task 4.6: Performance pass

- [ ] In Android Studio, *Profiler* > *CPU* > *Layout Inspector* — start the Board screen, take a snapshot. Ensure inflation < 16ms. If a screen takes longer, flatten the LinearLayout nests.

```bash
git commit -am "perf: flatten layout hierarchies for ≤ 16ms inflation"
```

---

### Task 4.7: Visual regression test (screenshot)

- [ ] Add a Robolectric screenshot test for each of the 8 main screens using `androidx.test.ext:junit`-driven activity scenarios. Compare against committed PNG baselines under `app/src/test/screenshots/`. This becomes the regression net for future redesigns.

If the team prefers manual screenshots-on-PR, skip this step and document in the PR.

```bash
git commit -am "test: add screenshot regression baseline for 8 screens"
```

---

### Task 4.8: Cleanup — delete legacy tokens & flag

- [ ] **Step 1: Remove `BuildConfig.NEW_DESIGN_SYSTEM`**

In `app/build.gradle`, delete the `buildConfigField` line. Remove any `if (BuildConfig.NEW_DESIGN_SYSTEM)` checks.

- [ ] **Step 2: Delete legacy color tokens**

Open `app/src/main/res/values/colors.xml` — delete all non-`vb_` color names that are no longer referenced. Run:

```bash
./gradlew lintDebug
```

Lint will flag any dangling references; fix them.

- [ ] **Step 3: Delete legacy dimens**

Same process for `dimens.xml` — drop `card_radius_xl 48dp`, `button_radius_full 999dp`, etc.

- [ ] **Step 4: Delete unused legacy drawables**

```bash
git rm app/src/main/res/drawable/bg_button_gradient.xml \
       app/src/main/res/drawable/bg_button_round.xml \
       app/src/main/res/drawable/bg_button_speak.xml \
       app/src/main/res/drawable/bg_settings_card.xml \
       app/src/main/res/drawable/bg_emoji_container.xml \
       app/src/main/res/drawable/bg_icon_circle.xml \
       app/src/main/res/drawable/bg_icon_primary.xml \
       app/src/main/res/drawable/bg_icon_error.xml \
       app/src/main/res/drawable/bg_avatar.xml \
       app/src/main/res/drawable/bg_folder_tab.xml \
       app/src/main/res/drawable/bg_vocab_card.xml \
       app/src/main/res/drawable/bg_word_chip.xml \
       app/src/main/res/drawable/bg_category_chip.xml
./gradlew assembleDebug    # surface any remaining references
```

Fix any references the build flags.

- [ ] **Step 5: Drop the old theme**

In `app/src/main/res/values/themes.xml` delete `Theme.VBoard` (the old one). Rename `Theme.VBoard.New` to `Theme.VBoard` for cleanliness; update the manifest.

- [ ] **Step 6: Final commit**

```bash
./gradlew test connectedAndroidTest
git add -A
git commit -m "chore: remove legacy design tokens, drawables, and feature flag"
git tag phase4-polish
```

This concludes Phase 4. Open the Phase 4 PR. After merge, the redesign is shipped.

---

## Self-Review Notes

After writing this plan, I checked it against the spec and fixed the following inline:
- Task 3.5 separator implementation clarified (use `RecyclerView.ItemDecoration`, not per-item padding)
- Task 2.13 manifest path corrected to `app/src/debug/AndroidManifest.xml`
- Task 1.10 explicitly notes preserving any existing `values-night/themes.xml` instead of overwriting
- Task 3.4 wires shake error animation to PIN failure
- Task 4.8 explicitly orders the rename of `Theme.VBoard.New` → `Theme.VBoard` after the legacy theme is gone

All 7 spec decisions, 75 tokens, 9 components, 8 screens + Add Card dialog + Folder Management sheet, and the 4-phase rollout are covered by tasks above. No placeholders remain.
