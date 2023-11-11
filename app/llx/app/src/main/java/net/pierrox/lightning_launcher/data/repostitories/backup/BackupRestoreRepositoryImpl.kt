package net.pierrox.lightning_launcher.data.repostitories.backup

import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.pierrox.lightning_launcher.data.workers.backup.BackupWorker
import net.pierrox.lightning_launcher.data.workers.backup.ExportWorker
import net.pierrox.lightning_launcher.data.workers.backup.ImportWorker
import net.pierrox.lightning_launcher.data.workers.backup.RestoreWorker

class BackupRestoreRepositoryImpl(
    private val workManager: WorkManager,
) : BackupRestoreRepository {
    override fun observeBackup(filePath: String): Flow<WorkInfo> = flow {
        val request = BackupWorker.create(filePath)
        workManager.enqueueUniqueWork(
            /* uniqueWorkName = */ "backup_${System.currentTimeMillis()}",
            /* existingWorkPolicy = */ ExistingWorkPolicy.KEEP,
            /* work = */ request
        )
        workManager.getWorkInfoByIdFlow(request.id)
    }

    override fun observeRestore(uriString: String): Flow<WorkInfo> = flow {
        val request = RestoreWorker.create(uriString)
        workManager.enqueueUniqueWork(
            /* uniqueWorkName = */ "restore_${System.currentTimeMillis()}",
            /* existingWorkPolicy = */ ExistingWorkPolicy.KEEP,
            /* work = */ request
        )
        workManager.getWorkInfoByIdFlow(request.id)
    }

    override fun observeImport(uri: Uri): Flow<WorkInfo> = flow {
        val request = ImportWorker.create(uri.toString())
        workManager.enqueueUniqueWork(
            /* uniqueWorkName = */ "import_${System.currentTimeMillis()}",
            /* existingWorkPolicy = */ ExistingWorkPolicy.KEEP,
            /* work = */ request
        )
        workManager.getWorkInfoByIdFlow(request.id)
    }

    override fun observeExport(
        sbHeight: Int,
        backupPath: String,
        includedPages: Array<Int>,
    ): Flow<WorkInfo> = flow {
        val request = ExportWorker.create(sbHeight, backupPath, includedPages)
        workManager.enqueueUniqueWork(
            /* uniqueWorkName = */ "export_${System.currentTimeMillis()}",
            /* existingWorkPolicy = */ ExistingWorkPolicy.KEEP,
            /* work = */ request
        )
        workManager.getWorkInfoByIdFlow(request.id)
    }
}