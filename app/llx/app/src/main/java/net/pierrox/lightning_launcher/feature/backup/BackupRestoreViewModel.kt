package net.pierrox.lightning_launcher.feature.backup

import android.Manifest
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.pierrox.lightning_launcher.data.FileUtils
import net.pierrox.lightning_launcher.data.model.UiText
import net.pierrox.lightning_launcher.data.repostitories.backup.BackupRestoreRepository
import net.pierrox.lightning_launcher.data.workers.backup.ImportWorker
import net.pierrox.lightning_launcher.data.workers.backup.RestoreWorker
import net.pierrox.lightning_launcher_extreme.R
import ru.mintrocket.lib.mintpermissions.flows.MintPermissionsDialogFlow
import ru.mintrocket.lib.mintpermissions.flows.ext.isSuccess

class BackupRestoreViewModel(
    private val repository: BackupRestoreRepository,
    private val permissionsFlow: MintPermissionsDialogFlow,
) : ViewModel() {

    companion object {
        private val REQUEST_PERMISSIONS = listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val _messagesChannel = Channel<UiText>()
    val messagesFlow = _messagesChannel.receiveAsFlow()

    private val _archivesList = MutableSharedFlow<List<String>>()
    val archivesList = _archivesList.asSharedFlow()

    private var archiveName: String? = null
    private var archiveUri: Uri? = null


    init {
        viewModelScope.launch {
            fetchWithPermissions()
        }
    }

    private suspend fun fetchWithPermissions() {
        val result = permissionsFlow.request(REQUEST_PERMISSIONS)
        if (result.isSuccess()) {
            fetchArchives()
        }
    }

    private suspend fun sendErrorMsg() {
        _messagesChannel.send(UiText.StringResource(R.string.import_e))
    }

    private suspend fun fetchArchives() {
        val archives = FileUtils.LL_EXT_DIR
            .listFiles { file -> !file.isDirectory() }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.name }

        archives?.let {
            _archivesList.emit(archives)
        }
    }

    fun importFile(uri: Uri) {
        repository.observeImport(uri)
            .map {
                when (it.state) {
                    WorkInfo.State.ENQUEUED -> {}
                    WorkInfo.State.RUNNING -> {}
                    WorkInfo.State.SUCCEEDED -> {
                        val path = it.outputData.getString(ImportWorker.KEY_OUTPUT_FILE)
                        path?.let {
                            _messagesChannel.send(UiText.DynamicString(path))
                        } ?: run {
                            sendErrorMsg()
                        }
                        loadArchive(null, path?.substringAfterLast("/"))
                    }

                    else -> sendErrorMsg()
                }
            }.launchIn(viewModelScope)
    }

    fun loadArchive(archiveUri: Uri?, archiveName: String?) {
        this.archiveUri = archiveUri
        this.archiveName = archiveName
    }

    fun exportTemplate(
        sbHeight: Int,
        backupPath: String,
        includedPages: Array<Int>,
    ) {
        repository.observeExport(
            sbHeight,
            backupPath,
            includedPages
        ).map {
            when (it.state) {
                WorkInfo.State.ENQUEUED -> {}
                WorkInfo.State.RUNNING -> {}
                WorkInfo.State.SUCCEEDED -> {
                    _messagesChannel.send(UiText.StringResource(R.string.tmpl_e_d))
                }

                else -> {
                    _messagesChannel.send(UiText.StringResource(R.string.tmpl_e_e))
                }
            }
        }.launchIn(viewModelScope)
    }

    fun restoreBackup(
        fileUri: String,
        onSuccess: (RestoreWorker.RestoreResult) -> Unit,
    ) {
        repository.observeRestore(fileUri)
            .map {
                when (it.state) {
                    WorkInfo.State.ENQUEUED -> {}
                    WorkInfo.State.RUNNING -> {}
                    WorkInfo.State.SUCCEEDED -> {
                        val filePath =
                            it.outputData.getString(RestoreWorker.BACKUP_FILE_PATH).orEmpty()
                        val result = it.outputData.getInt(
                            RestoreWorker.KEY_RESULT,
                            RestoreWorker.RestoreResult.RESTORE_NONE.ordinal
                        )
                        _messagesChannel.send(UiText.DynamicString(filePath))
                        onSuccess(result.toResult())
                    }

                    else -> {
                        _messagesChannel.send(UiText.StringResource(R.string.restore_error))
                    }
                }
            }
    }

    private fun Int.toResult() = when (this) {
        1 -> RestoreWorker.RestoreResult.RESTORE_BACKUP
        2 -> RestoreWorker.RestoreResult.RESTORE_TEMPLATE
        else -> RestoreWorker.RestoreResult.RESTORE_NONE
    }

}