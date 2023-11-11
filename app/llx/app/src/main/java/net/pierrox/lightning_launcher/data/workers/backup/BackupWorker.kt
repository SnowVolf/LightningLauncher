package net.pierrox.lightning_launcher.data.workers.backup

import android.app.Notification
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import net.pierrox.lightning_launcher.data.BackupRestoreTool
import net.pierrox.lightning_launcher.data.BackupRestoreTool.BackupConfig
import net.pierrox.lightning_launcher.util.Constant
import net.pierrox.lightning_launcher.util.IssueCreator
import net.pierrox.lightning_launcher_extreme.R

class BackupWorker(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val BACKUP_FILE_PATH = "backup_file_path"
        fun create(
            backupFilePath: String
        ): OneTimeWorkRequest {
            val data = workDataOf(
                BACKUP_FILE_PATH to backupFilePath,
            )

            return OneTimeWorkRequestBuilder<BackupWorker>()
                .setInputData(data)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val config = BackupConfig()
        config.context = context

        val backupPath = inputData.getString(BACKUP_FILE_PATH)
        val pm: PackageManager = context.packageManager
        try {
            val pi: PackageInfo = pm.getPackageInfo(context.packageName, 0)
            val dataDir = pi.applicationInfo.dataDir + "/files"
            config.pathFrom = dataDir
        } catch (e: PackageManager.NameNotFoundException) {
            // pass
        }
        with(config) {
            pathTo = backupPath
            includeWidgetsData = true
            includeWallpaper = true
            includeFonts = true
        }

        val exception = BackupRestoreTool.backup(config)
        return if (exception != null) {
            Result.failure(
                workDataOf(
                    IssueCreator.ISSUE_TITLE to exception.message,
                    IssueCreator.ISSUE_BODY to exception.stackTrace
                )
            )
        } else {
            Result.success(
                workDataOf(
                    BACKUP_FILE_PATH to backupPath
                )
            )
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationId = this.hashCode()
        return ForegroundInfo(
            notificationId,
            createNotification()
        )
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannelCompat.Builder(
            Constant.BACKUP_NOTIFICATIONS,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName("Backup/Restore")
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)

        return NotificationCompat.Builder(context, channel.id)
            .setContentTitle(context.getString(R.string.tmpl_e_m))
            .setOngoing(true)
            .build()
    }

}