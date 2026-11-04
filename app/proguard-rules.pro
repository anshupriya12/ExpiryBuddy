# Release shrinking rules for Expiry Buddy.
# Room, WorkManager, Compose and Navigation ship their own consumer rules.

# WorkManager instantiates ReminderWorker by class name (stored in its database), so it must
# survive minification with its (Context, WorkerParameters) constructor.
-keep class com.expirybuddy.app.reminders.ReminderWorker { <init>(...); }

# Keep readable stack traces from release crash reports.
-keepattributes SourceFile,LineNumberTable
