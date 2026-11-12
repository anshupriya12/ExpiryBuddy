# Expiry Buddy

Expiry Buddy answers one question: **when should I use this item by?** Save an item with its use-by
date (and a photo, if you like), see what needs attention, and get a local reminder at the time you
choose. Everything stays on the device and works offline. There is no account, backend or
third-party service.

**Version 1.0.0** · Android 8.0+ (minSdk 26) · Kotlin, Jetpack Compose, Material 3

## Features

- **Home**: time-aware greeting, items that are *Past due* or need to be used *soon*, search, and a
  round add button at the bottom for one-handed use. Empty, "nothing to use soon", no-results and error
  states.
- **All items**: search, category filter chips, sort (use-by date, name, category) and a **Used** filter.
- **Item details**: photo, category, a status panel in words and colour, use-by date, opening details,
  storage, reminder, notes, **Mark as used** (or **Restore to list**), Edit, and Delete behind a
  confirmation.
- **Add / Edit item**: one form for both, with Save pinned above the keyboard and inline validation.
- **Settings**: notification status and switch, default advance notice, default notification time,
  version, and data and privacy.
- **Onboarding**: three first-launch pages, shown once.

### Categories

Food, Medication, Cosmetics and Other. (Earlier builds also had Document and Household; any saved
item in those became *Other* when the database was upgraded. Nothing was deleted.)

### One use-by deadline

Every item has a single **use-by date**; there are no separate package-expiry and after-opening
deadlines. The date can be:

- typed directly with the date picker, or
- **calculated** from *Opened on* + *Use within* (a number of days or months). The result appears
  immediately, is marked as calculated, and can still be edited by hand. Months follow the calendar
  (31 Jan + 1 month = 28 Feb, or 29 in a leap year).

Opening details are optional: a direct use-by date needs no opening information.

### Storage

Both fields are optional:

- **Storage type**: Not specified, Refrigerate, Frozen or Airtight (Material 3 dropdown).
- **Additional instructions**: free multiline text (for example "Keep tightly sealed after every use").

Both are saved, restored when editing and shown in Item details.

### Status rules

| Status | Rule (calendar days, compared with the device's local "today") |
|---|---|
| Past due | use-by date is before today |
| Use soon | today through the next 7 days (inclusive) |
| Upcoming | more than 7 days away |

The 7-day threshold is `ExpiryClassifier.EXPIRING_SOON_DAYS`, the only place it is defined. Items
marked as used are kept (never silently deleted) but leave Home and the active lists; find them under
**All items > Used**. Statuses refresh at local midnight and when the app returns to the foreground.

### Reminders

- **Advance notice**: on the use-by date, 1, 2 or 3 days before, a custom number of days (1 to 365), or
  no reminder.
- **Notification time**: any time of day, chosen with the Material time picker.
- **Defaults** (Settings): new items start at **1 day before**, at **9:00 AM**, until you choose
  otherwise. A default you saved earlier is never overwritten.
- **Per item**: each item can use its own advance notice and its own time, or follow the defaults.
  Example: use-by 14 Nov, 1 day before, 7:30 PM gives a notification on 13 Nov at 7:30 PM.
- Changing the default time re-plans every item that follows it; items with their own time keep it.
  Changing the default advance notice only affects items added afterwards.
- **One notification per item.** Reminders do not repeat.
- Reminders use WorkManager (no exact alarms), so delivery can be a little late, especially in Doze or
  battery saver. A reminder is only scheduled if its date and time are still in the future; the form says
  when the chosen time has already passed.
- Editing an item replaces its reminder, deleting cancels it, **marking it as used cancels it** and
  restoring schedules it again if it still applies. Work is named `reminder-item-<id>`, so each item has
  at most one.
- Just before showing a notification the worker re-reads the item and shows nothing if it was
  deleted, marked as used, has no reminder, the reminder day has not arrived, or the use-by date has
  passed.
- At app start upcoming reminders are re-enqueued to correct for a time-zone change (this never cancels).
- Android 13+: the first time you save an item with a reminder, the app explains why it wants to send
  notifications and then shows the system prompt. Declining is fine; Settings shows the current status
  and never shows notifications as on unless the system allows them.

### Photos

Camera and gallery both go through system apps (the camera app and the Android photo picker), so the app
asks for **no camera or storage permission**. A chosen picture is copied into the app's private storage,
downsized to at most 1280 px and saved as JPEG (camera rotation applied). Photos are deleted with their
item or when replaced or removed; pictures from an unsaved form are discarded, and files no item refers to
are cleaned up at app start.

## Design

Defined once in `ui/theme/` and used through Material 3 roles. Brand orange `#F56A2A` for actions, dark
orange for orange text on light surfaces, peach washes, status colours, light and dark themes
(wallpaper-based dynamic colour is deliberately off). Playfair Display for headings, Poppins for
functional text and Caveat for one handwritten line, all bundled in `res/font` (SIL Open Font License;
licences in `third_party/fonts/`; nothing is downloaded at run time). The launcher icon is a vector
egg-in-a-timer mark. No screenshots are included in the repository.

## Technical stack and architecture

| Area | Choice |
|---|---|
| Language / UI | Kotlin 2.0, Jetpack Compose, Material 3 |
| Architecture | MVVM + Repository, StateFlow, hand-written dependency container (`AppContainer`) |
| Navigation | Navigation Compose (string routes) |
| Storage | Room (schema exported to `app/schemas/`), SharedPreferences for small settings |
| Reminders | WorkManager one-time jobs |
| Build | Gradle 8.9, Android Gradle Plugin 8.7, JDK 17 |
| SDK | minSdk 26, targetSdk 35, compileSdk 35 |
| Package | `com.expirybuddy.app` |

```
app/src/main/java/com/expirybuddy/app/
  ui/theme/          Colours, type, shapes, spacing, Material 3 theme
  ui/navigation/     Routes and NavHost
  ui/components/     Reusable composables
  ui/screens/        onboarding, home, all, details, edit, settings
  domain/expiry/     ExpiryClassifier, UseByCalculator, ItemFilter, section and inventory builders
  domain/reminder/   ReminderCalculator (reminder date, time, delay, relevance)
  domain/model/      ExpiryItem, ItemCategory, StorageType, ReminderOptions
  domain/validation/ Form rules (pure Kotlin)
  reminders/         ReminderScheduler (WorkManager), ReminderWorker, NotificationHelper
  data/local/        Room entity, DAO, database and migrations
  data/repository/   Repository and its Room implementation, reminder-syncing decorator
  data/photo/        Private-storage photos
  data/prefs/        UserPreferences (onboarding, default notice and time)
  AppContainer.kt    Manual dependency holder
```

### Data and offline-first

- Items live in a local Room database (`expiry_buddy.db`); nothing leaves the device and nothing needs
  the network.
- The deadline is stored in the `expiryDate` column (an epoch day, so it never shifts with time
  zones); it is the use-by date.
- `reminderDaysBefore` is nullable (`null` = no reminder, `0` = on the use-by date) and
  `reminderMinuteOfDay` is nullable (`null` = follow the default notification time).
- **Schema version 4.** Migrations are real `Migration` objects with no destructive fallback:
  - 1 to 2: photo, `updatedAt` and `usedAt`.
  - 2 to 3: opening date, use-within amount and unit, storage instructions, per-item reminder time.
  - 3 to 4: storage type, and the retired `document` and `household` categories become `other`.

  Existing items, photos, notes and reminder choices come through every step; this is covered by
  instrumented migration tests.

## Install

Download `ExpiryBuddy_1.0.0.apk` from the project's GitHub Releases, copy it to an Android phone (8.0 or
newer), open it and allow installing from that source if asked. The APK is signed with the project's
release key. A copy installed from a debug build, or signed with a different key, must be uninstalled
first.

## Build and run locally

Requirements: JDK 17 (Gradle must run on JDK 17 to 21), the Android SDK with platform 35, and either
Android Studio or `ANDROID_HOME` plus a `local.properties` containing `sdk.dir=<path to SDK>`.

```bash
./gradlew assembleDebug              # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest          # JVM tests, no device needed
./gradlew connectedDebugAndroidTest  # needs an emulator or device
./gradlew lintDebug
```

On Windows use `gradlew.bat`.

### Signed release build

```bash
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk (or -unsigned.apk)
```

Release builds use R8 minification and resource shrinking (`app/proguard-rules.pro` keeps
`ReminderWorker`). Without a keystore the APK is unsigned and cannot be installed. To sign it, create a
keystore with `keytool` and put a `keystore.properties` file in the project root with `storeFile`,
`storePassword`, `keyAlias` and `keyPassword`. `keystore.properties` and keystores are gitignored:
never commit them, and back the keystore up, because losing it means you cannot ship updates that
install over existing copies.

## Testing

Results of the final 1.0.0 verification run on an Android emulator (API 37):

| Check | Command | Result |
|---|---|---|
| JVM unit tests | `./gradlew testDebugUnitTest` | 254 run, 0 failed |
| Instrumented tests | `./gradlew connectedDebugAndroidTest` | 44 run, 0 failed |
| Lint | `./gradlew lintDebug` | 0 errors, 55 warnings |
| Debug build | `./gradlew assembleDebug` | succeeded |
| Release build | `./gradlew assembleRelease` | succeeded (signed) |

The JVM tests cover status classification, use-by calculation, reminder dates, times and daylight-saving
handling, validation, filtering, sorting, all ViewModels, preferences and the reminder-syncing repository.
The instrumented tests cover the Room DAO, the 1 to 2, 2 to 3 and 3 to 4 migrations, photo storage, real
settings storage, real WorkManager scheduling and the worker's notifications.

The app was also exercised by hand on the emulator (onboarding, adding and editing items, the
calculated use-by date, the time picker, Settings, and an in-place upgrade from the earlier release
with saved data). There are no automated Compose UI tests, and TalkBack, the gallery picker and an
actual notification arriving at its set time under Doze have not been tested.

## Known limitations

- Reminders are a single notification and can be late under Doze or battery saver; they are skipped if
  their time has already passed when the item is saved.
- Text typed in the Add/Edit form survives rotation but not process death.
- Home search shows results inline rather than in a floating sheet.
- Categories are a fixed set, and there is no separate Reminders screen or category management.
- Primary buttons are white on the brand orange (about 3:1 contrast), which is fine for large bold labels
  but below the 4.5:1 guideline for small text.
- English only; strings are in `res/values/strings.xml` and ready for translation.
- Installing this build over a copy with a higher `versionCode` is rejected by Android; uninstall that
  copy first.
