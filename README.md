# Expiry Buddy

Expiry Buddy helps you remember when things expire. Save an item with its expiry date, see what is
expired, expiring soon, or upcoming, and get a local reminder before it expires. Everything stays on
the device and works offline. There is no account, backend, or third-party service.

> Status: **Phase 6 of 6 (MVP complete)**. You can add, view, edit and delete items, see them grouped
> as Expired, Expiring Soon and Upcoming with counts, search and filter by category, and get a local
> reminder before an item expires.

## Reminders

- Each item can have a reminder 0, 1, 3, 7 or 14 days before its expiry date, or none.
- A reminder is delivered at **9:00 local time** on the reminder day, as a notification that opens
  the item. It is scheduled with WorkManager (no exact alarms), so delivery can be a little late
  when the device is in Doze.
- A reminder is scheduled only if that 9:00 moment is still in the future. Editing an item replaces
  its reminder (or cancels it if there is no reminder, or the reminder moment has already passed);
  deleting an item cancels it. Work is named `reminder-item-<id>`, so each item has at most one.
- Just before showing a notification the worker re-reads the item and shows nothing if it was
  deleted, has no reminder, the reminder day has not arrived, or the expiry date has passed.
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
- Expired items are sorted most recently expired first; the other groups soonest first. Ties sort by name.
- The summary counts always cover every saved item. Search (name and notes) and the category filter
  only narrow the list below them.
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
./gradlew assembleDebug
./gradlew testDebugUnitTest          # JVM tests, no device needed
./gradlew connectedDebugAndroidTest  # Room DAO, WorkManager and worker tests; needs an emulator or device
```

On Windows use `gradlew.bat`. Gradle must run on JDK 17-21, not a newer one (set `JAVA_HOME`).

## Release build

```bash
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release-unsigned.apk
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
- The minified build was smoke-tested on an emulator: it launches, saves items (Room) and schedules
  reminders (WorkManager) without crashes.

## Testing

| Suite | Command | Count |
|---|---|---|
| JVM unit tests (classification, reminder dates, DST, validation, filtering, ViewModels, repository decorator) | `./gradlew testDebugUnitTest` | 139 |
| Instrumented (Room DAO, real WorkManager scheduling, worker notifications) | `./gradlew connectedDebugAndroidTest` | 20 |

Not covered by automated tests: Compose UI (checked manually on an emulator), the worker's behavior
when notification permission has been revoked, and an actual 9:00 delivery under Doze.

## Known limitations

- Reminders fire at a fixed 9:00 local time and can be late in Doze (WorkManager, not exact alarms).
- Text typed in the Add/Edit form survives rotation but not process death.
- English only; strings are in `res/values/strings.xml` and ready for translation.

## Data

- Items live in a local Room database (`expiry_buddy.db`); nothing leaves the device.
- `expiryDate` is a `LocalDate` stored as an epoch day, so it never shifts with time zones.
- `reminderDaysBefore` is nullable: `null` means no reminder, `0` means on the expiry day.
- Room schemas are exported to `app/schemas/` and should be committed. There is deliberately no
  destructive-migration fallback; schema changes need a real `Migration`.

## Project configuration

| Setting | Value |
|---|---|
| Package | `com.expirybuddy.app` |
| minSdk / targetSdk / compileSdk | 26 / 35 / 35 |
| Language | Kotlin 2.0.21, Java 17 |
| UI | Jetpack Compose, Material 3 (dynamic color on Android 12+) |
| Navigation | Navigation Compose (string routes) |
| Storage | Room |
| Reminders | WorkManager |

## Structure

```
app/src/main/java/com/expirybuddy/app/
  ui/theme/        Material 3 theme
  ui/navigation/   Routes and NavHost
  ui/components/   Reusable composables
  ui/screens/      home, edit, details, settings
  domain/expiry/   ExpiryClassifier, ItemFilter, HomeSectionsBuilder, todayUpdates
  domain/reminder/ ReminderCalculator (reminder date, delay, relevance)
  reminders/       ReminderScheduler (WorkManager), ReminderWorker, NotificationHelper
  domain/model/    ExpiryItem, ItemCategory, ReminderOptions
  domain/validation/ Form rules (pure Kotlin)
  data/local/      Room entity, DAO, database, type converter
  data/repository/ ExpiryItemRepository and its Room implementation
  AppContainer.kt  Manual dependency holder
```

## Roadmap

1. Foundation, theme, navigation (done)
2. Room database and repository (done)
3. Home and Add/Edit UI (done)
4. Expiry classification, sorting, search, filtering (done)
5. WorkManager reminders and notification permission (done)
6. Tests, polish, release readiness (done)
