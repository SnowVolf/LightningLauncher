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
import net.pierrox.lightning_launcher_extreme.R

class ExportWorker(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val STATUS_BAR_HEIGHT = "status_bar_height"
        const val BACKUP_PATH = "backup_path"
        const val INCLUDED_PAGES = "included_pages"

        fun create(
            sbHeight: Int,
            backupPath: String,
            includedPages: Array<Int>
        ): OneTimeWorkRequest {
            val data = workDataOf(
                STATUS_BAR_HEIGHT to sbHeight,
                BACKUP_PATH to backupPath,
                INCLUDED_PAGES to includedPages
            )
            return OneTimeWorkRequestBuilder<ExportWorker>()
                .setInputData(data)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val config = BackupConfig()

        config.context = context
        val pm: PackageManager = context.packageManager
        try {
            val pi: PackageInfo = pm.getPackageInfo(context.packageName, 0)
            val data_dir = pi.applicationInfo.dataDir + "/files"
            config.pathFrom = data_dir
        } catch (e: PackageManager.NameNotFoundException) {
            // pass
        }
        with(config) {
            pathTo = inputData.getString(BACKUP_PATH)
            includeWidgetsData = true
            includeWallpaper = true
            includeFonts = true
            forTemplate = true
            statusBarHeight = inputData.getInt(STATUS_BAR_HEIGHT, 0)
            pagesToInclude = inputData.getIntArray(INCLUDED_PAGES)
        }
        return if (BackupRestoreTool.backup(config) == null) {
            Result.success()
        } else {
            Result.failure()
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
            .setContentTitle(context.getString(R.string.importing))
            .setOngoing(true)
            .build()
    }

}