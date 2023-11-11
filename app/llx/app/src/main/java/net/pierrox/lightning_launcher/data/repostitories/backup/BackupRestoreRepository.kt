package net.pierrox.lightning_launcher.data.repostitories.backup

import android.net.Uri
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow

interface BackupRestoreRepository {

    fun observeBackup(filePath: String): Flow<WorkInfo>

    fun observeRestore(uriString: String): Flow<WorkInfo>

    fun observeImport(uri: Uri): Flow<WorkInfo>

    fun observeExport(
        sbHeight: Int,
        backupPath: String,
        includedPages: Array<Int>,
    ): Flow<WorkInfo>

}