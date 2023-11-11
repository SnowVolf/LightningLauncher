package net.pierrox.lightning_launcher.data.workers.backup

import android.app.Notification
import android.content.Context
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
import net.pierrox.lightning_launcher.data.FileUtils
import net.pierrox.lightning_launcher.util.Constant
import net.pierrox.lightning_launcher.util.getNameForUri
import net.pierrox.lightning_launcher_extreme.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class ImportWorker(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_IMPORT_URI = "import_uri"
        const val KEY_OUTPUT_FILE = "output_file"

        fun create(uriString: String): OneTimeWorkRequest {
            val data = workDataOf(
                KEY_IMPORT_URI to uriString,
            )

            return OneTimeWorkRequestBuilder<ImportWorker>()
                .setInputData(data)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val uri = params.inputData.getString(KEY_IMPORT_URI)?.toUri()

        var inputStream: InputStream? = null
        var os: FileOutputStream? = null
        var outFile: File? = null
        uri?.let { uriString ->
            return try {
                val name: String? = context.getNameForUri(uriString)
                outFile = File(FileUtils.LL_EXT_DIR, name)
                inputStream = context.contentResolver.openInputStream(uriString)
                os = FileOutputStream(outFile)
                FileUtils.copyStream(inputStream, os)
                Result.success(
                    workDataOf(
                        KEY_OUTPUT_FILE to outFile?.path
                    )
                )
            } catch (e: IOException) {
                outFile?.delete()
                Result.failure()
            } finally {
                inputStream?.close()
                os?.close()
            }
        }
        return Result.failure()
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