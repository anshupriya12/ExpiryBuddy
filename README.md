# Expiry Buddy

Expiry Buddy helps you remember when things expire. Save an item with its expiry date (and a photo, if
you like), see what needs attention, and get a local reminder before it expires. Everything stays on
the device and works offline. There is no account, backend, or third-party service.

> Status: **Version 2** (UI redesign of the working V1 app). Same offline-first architecture, database
> and reminders; new design system, onboarding, photos, All Items, mark-as-used and Settings.

## Screens

| Screen | What it does |
|---|---|
| **Onboarding** | Three first-launch pages (track items, reminders, waste less) with Skip, Next and Get started. Shown once; completion is stored locally. |
| **Home** | Time-aware greeting, items that are **Expired** or **Expiring soon**, "See all", a search field and the round add button at the bottom for one-handed use. Searching shows every matching active item. Empty, "nothing expiring soon", no-results and error states. |
| **All items** | Search, category filter chips, sort (expiry date, name, category) and a **Used** filter. |
| **Item details** | Photo, name, category, a status panel in words and colour, reminder, notes, **Mark as used** (or **Restore to list**), Edit, and Delete behind a confirmation. |
| **Add / Edit item** | One form for both. Photo (camera or gallery, preview, replace, remove), name, category, Material date picker, reminder, notes, inline validation, Save pinned above the keyboard, and a neutral Delete in edit mode. |
| **Settings** | Notification status and switch (never shows "on" unless the system allows notifications), default reminder for new items, reminder time, version, and data and privacy. |

Every control does something: there are no decorative buttons, and every list is bound to the real Room
data. Confirmations (saved, deleted, marked as used, restored) appear as snackbars.

## Design system

Defined once in `ui/theme/` and used through Material 3 roles, never raw hex in screens.

- **Colour**: orange `#F56A2A` for primary actions, dark orange `#B94716` for orange text on light
  surfaces, peach `#FFF0E6` washes, canvas `#FFFCFA`, plus status colours for expired, soon, upcoming
  and success. Wallpaper-based dynamic colour is deliberately off so the brand looks the same on every
  phone. Dark mode follows the system setting.
- **Type**: Playfair Display for headings, Poppins for everything functional, Caveat once for a short
  handwritten line. Sizes are in `sp`, so they follow the system font scale.
- **Shape and spacing**: one corner radius per component role and a 4/8/12/16/24/32 dp spacing scale
  (`ui/theme/Shape.kt`). Touch targets are at least 48 dp.
- **Icons**: thin outlined Material icons, plus a few matching custom ones (`ui/components/AppIcons.kt`).
- **Logo**: the launcher icon is the approved egg-in-a-timer mark, drawn as a vector.
- **Fonts**: bundled in `app/src/main/res/font/` (Poppins, Playfair Display and Caveat, all SIL Open
  Font License; licences are in `third_party/fonts/`). Nothing is downloaded at run time.

Reusable components live in `ui/components/`: `ItemCard`, `ExpiryStatusLabel`, `ExpiryBuddySearchField`,
`CategoryFilterChip`, `PrimaryButton` and `SecondaryButton`, `AddItemFormField`, `CategorySelector`,
`ReminderSelector`, `ExpiryDateSelector`, `PhotoPicker`, `EmptyState`, `DeleteConfirmationDialog`,
`AppSnackbar` and the screen scaffold.

## Photos

- Camera and gallery both go through the system apps (the Android photo picker and the camera app), so
  the app asks for **no camera or storage permission**.
- A chosen picture is copied into the app's private storage, downsized to at most 1280 px and saved as a
  JPEG (camera rotation is applied). Photos are decoded at about the size they are shown, off the main
  thread, and cached.
- A photo is deleted when its item is deleted or the photo is replaced or removed. Pictures from an
  unsaved form are discarded, and any file no item refers to is cleaned up at app start.

## Reminders

- Each item can have a reminder on the same day, or 1, 3, 7 or 14 days before its expiry date, or none.
  New items start with the default chosen in Settings.
- A reminder is delivered at **9:00 local time** on the reminder day, as a notification that opens
  the item. It is scheduled with WorkManager (no exact alarms), so delivery can be a little late
  when the device is in Doze.
- A reminder is scheduled only if that 9:00 moment is still in the future. Editing an item replaces
  its reminder (or cancels it if there is no reminder, or the reminder moment has already passed);
  deleting an item cancels it. **Marking an item as used cancels its reminder too, and restoring it
  schedules the reminder again** if it still applies. Work is named `reminder-item-<id>`, so each item
  has at most one.
- Just before showing a notification the worker re-reads the item and shows nothing if it was
  deleted, was marked as used, has no reminder, the reminder day has not arrived, or the expiry date
  has passed.
- At app start, upcoming reminders are re-enqueued to correct for a time-zone change. This never
  cancels anything, because a cold start can be the worker itself firing.
- Android 13+: the first time you save an item with a reminder, the app explains why it wants to
  send notifications and then shows the system prompt. Declining is fine: the app works fully, and
  Settings shows the current status with a way to turn notifications on later.

## Expiry rules

| Status | Rule (dates are calendar days, compared to the device's local "today") |
|---|---|
| Expired | expiry date is before today |
| Expiring Soon | today through the next 7 days (inclusive) |
| Upcoming | more than 7 days away |

- The 7-day threshold is `ExpiryClassifier.EXPIRING_SOON_DAYS`, the only place it is defined.
- Expired items are sorted most recently expired first on Home; All Items sorts by the chosen order,
  earliest expiry first by default. Ties sort by name.
- Items marked as used are kept (never silently deleted) but leave Home and the active lists. Find them
  under **All items > Used**.
- Statuses refresh at local midnight and whenever the app returns to the foreground.

## Requirements

- JDK 17
- Android SDK with platform 35 and build-tools 35 (Android Studio Ladybug or newer installs these)
- Gradle 8.9 (provided through the wrapper)

## Setup and build

1. Open the folder in Android Studio, or point `ANDROID_HOME` at your SDK and create `local.properties`
   with `sdk.dir=<path to SDK>`.
2. If `gradlew` / `gradle/wrapper/gradle-wrapper.jar` are missing, run `gradle wrapper --gradle-version 8.9` once.
3. Build and test:

```bash
./gradlew assembleDebug              # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest          # JVM tests, no device needed
./gradlew connectedDebugAndroidTest  # Room, migration, photo, WorkManager and worker tests; needs an emulator or device
./gradlew lintDebug
```

On Windows use `gradlew.bat`. Gradle must run on JDK 17-21, not a newer one (set `JAVA_HOME`).

## Release build

```bash
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk (or -unsigned.apk)
```

- Release builds use R8 minification and resource shrinking. `app/proguard-rules.pro` keeps
  `ReminderWorker` (WorkManager instantiates it by name) and line numbers for stack traces.
- Without a keystore the APK is **unsigned** and cannot be installed on a phone. To sign it, create a
  keystore (`keytool -genkeypair -v -keystore expirybuddy-release.jks -alias expirybuddy -keyalg RSA
  -keysize 2048 -validity 10000`) and put a `keystore.properties` file in the project root with
  `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Both files are gitignored: never commit
  them, and back the keystore up (losing it means you cannot ship updates to installed copies).
  With that file present, `assembleRelease` produces a signed `app-release.apk`.
- Before publishing, bump `versionCode` / `versionName` in `app/build.gradle.kts`.

## Testing

| Suite | Command | Count |
|---|---|---|
| JVM unit tests (classification, sorting, reminder dates and DST, validation, filtering, all ViewModels, photo and used-state rules, repository decorator) | `./gradlew testDebugUnitTest` | 187 |
| Instrumented (Room DAO, **1 to 2 migration**, photo storage, real WorkManager scheduling, worker notifications) | `./gradlew connectedDebugAndroidTest` | 32 |

The UI was also checked by hand on an emulator, including dark mode, an 1.8x font scale, rotation, the
camera flow and the signed, minified release build. Not covered by automated tests: Compose UI, TalkBack
(screen reader) behaviour, the gallery picker, the worker's behaviour when notification permission has been
revoked, and an actual 9:00 delivery under Doze.

## Known limitations

- Reminders fire at a fixed 9:00 local time and can be late in Doze (WorkManager, not exact alarms).
- Text typed in the Add/Edit form survives rotation but not process death.
- Home search shows results inline instead of in a floating sheet.
- Category management and a separate Reminders screen from the design brief are not included (the
  categories are a fixed set, and the brief asks to add nothing beyond the listed screens).
- Primary buttons are white on the brand orange (about 3:1 contrast). That is fine for large bold labels
  but below the 4.5:1 guideline for small text; a darker orange would fix it if you want strict AA.
- English only; strings are in `res/values/strings.xml` and ready for translation.

## Data

- Items live in a local Room database (`expiry_buddy.db`); nothing leaves the device.
- `expiryDate` is a `LocalDate` stored as an epoch day, so it never shifts with time zones.
- `reminderDaysBefore` is nullable: `null` means no reminder, `0` means on the expiry day.
- Schema version 2 adds `photoUri`, `updatedAt` and `usedAt`. `MIGRATION_1_2` keeps every existing item
  (no photo, still active, `updatedAt` = creation time) and is covered by an instrumented migration test.
- Room schemas are exported to `app/schemas/` and should be committed. There is deliberately no
  destructive-migration fallback; schema changes need a real `Migration`.
- Small settings (onboarding done, default reminder) are stored in SharedPreferences.

## Project configuration

| Setting | Value |
|---|---|
| Package | `com.expirybuddy.app` |
| minSdk / targetSdk / compileSdk | 26 / 35 / 35 |
| Language | Kotlin 2.0.21, Java 17 |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose (string routes) |
| Storage | Room |
| Reminders | WorkManager |
| Architecture | MVVM + Repository, hand-written dependency container |

## Structure

```
app/src/main/java/com/expirybuddy/app/
  ui/theme/        Colours, type, shapes, spacing, Material 3 theme
  ui/navigation/   Routes and NavHost
  ui/components/   Reusable composables
  ui/screens/      onboarding, home, all, details, edit, settings
  domain/expiry/   ExpiryClassifier, ItemFilter, HomeSectionsBuilder, InventoryBuilder, todayUpdates
  domain/reminder/ ReminderCalculator (reminder date, delay, relevance)
  reminders/       ReminderScheduler (WorkManager), ReminderWorker, NotificationHelper
  domain/model/    ExpiryItem, ItemCategory, ReminderOptions
  domain/validation/ Form rules (pure Kotlin)
  data/local/      Room entity, DAO, database and migration, type converter
  data/repository/ ExpiryItemRepository and its Room implementation
  data/photo/      PhotoStorage (private-storage photos)
  data/prefs/      UserPreferences (onboarding, default reminder)
  AppContainer.kt  Manual dependency holder
```

## Roadmap

1. Foundation, theme, navigation (done)
2. Room database and repository (done)
3. Home and Add/Edit UI (done)
4. Expiry classification, sorting, search, filtering (done)
5. WorkManager reminders and notification permission (done)
6. Tests, polish, release readiness (done)
7. V2 redesign: design system, onboarding, photos, All Items, mark as used, Settings (done)
