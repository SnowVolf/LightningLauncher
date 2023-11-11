package net.pierrox.lightning_launcher.data.workers.backup

import android.app.Notification
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import net.pierrox.lightning_launcher.data.BackupRestoreTool
import net.pierrox.lightning_launcher.data.BackupRestoreTool.RestoreConfig
import net.pierrox.lightning_launcher.util.Constant
import net.pierrox.lightning_launcher_extreme.R
import java.io.IOException
import java.io.InputStream

class RestoreWorker(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params) {

    enum class RestoreResult {
        RESTORE_NONE,
        RESTORE_BACKUP,
        RESTORE_TEMPLATE,
    }

    companion object {
        const val BACKUP_FILE_PATH = "backup_file_path"
        const val KEY_RESULT = "restore_result"
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
        val uri = inputData.getString(BACKUP_FILE_PATH)?.toUri()
        val config = RestoreConfig()

        var stream: InputStream? = null
        try {
            val cr: ContentResolver = context.contentResolver
            stream = uri?.let { cr.openInputStream(it) }
            val manifest = BackupRestoreTool.readManifest(stream)
            if (manifest != null) {
                // it looks like a template
                return Result.success(
                    workDataOf(
                        KEY_RESULT to RestoreResult.RESTORE_TEMPLATE,
                    )
                )
            }
        } catch (e: Exception) {
            // not a template, continue with normal restore
        } finally {
            if (stream != null) try {
                stream.close()
            } catch (e: IOException) {
            }
        }

        config.context = context
        val pm: PackageManager = context.packageManager
        try {
            val pi: PackageInfo = pm.getPackageInfo(context.packageName, 0)
            val data_dir = pi.applicationInfo.dataDir + "/files"
            config.pathTo = data_dir
        } catch (e: PackageManager.NameNotFoundException) {
            // pass
        }
        config.uriFrom = uri
        config.restoreWidgetsData = true
        config.restoreWallpaper = true
        config.restoreFonts = true

        // ensure this directory at least is created with right permissions
        try {
            config.context.createPackageContext(context.packageName, 0)
                .getDir("files", Context.MODE_PRIVATE)
        } catch (e1: PackageManager.NameNotFoundException) {
            return Result.failure()
        }

        return if (BackupRestoreTool.restore(config))
            Result.success(
                workDataOf(
                    KEY_RESULT to RestoreResult.RESTORE_BACKUP
                )
            )
        else
            Result.failure()
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

