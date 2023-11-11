package net.pierrox.lightning_launcher.feature.backup

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import net.pierrox.lightning_launcher.API
import net.pierrox.lightning_launcher.LLApp
import net.pierrox.lightning_launcher.activities.ApplyTemplate
import net.pierrox.lightning_launcher.activities.Dashboard
import net.pierrox.lightning_launcher.activities.ScreenManager
import net.pierrox.lightning_launcher.data.Folder
import net.pierrox.lightning_launcher.data.Page
import net.pierrox.lightning_launcher.data.model.UiText
import net.pierrox.lightning_launcher.data.workers.backup.RestoreWorker
import net.pierrox.lightning_launcher_extreme.R
import net.pierrox.lightning_launcher_extreme.databinding.FragmentBackupRestoreBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.system.exitProcess

class BackupRestoreFragment : Fragment(R.layout.fragment_backup_restore),
    View.OnClickListener, OnItemClickListener, OnItemLongClickListener,
    OnLongClickListener {

    private val binding by viewBinding(FragmentBackupRestoreBinding::bind)
    private val viewModel: BackupRestoreViewModel by viewModel()

    private val loadLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        viewModel.loadArchive(it, null)
    }
    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.importFile(it) }
        }
    private val selectPagesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val selectedPages: IntArray? =
                    it.data?.getIntArrayExtra(API.SCREEN_PICKER_INTENT_EXTRA_SELECTED_SCREENS)
                val allPages = java.util.ArrayList<Int>()

                if (selectedPages != null) {
                    for (p in selectedPages) {
                        allPages.add(p)
                        addSubPages(allPages, p)
                    }
                    allPages.add(Page.APP_DRAWER_PAGE)
                    allPages.add(Page.USER_MENU_PAGE)

                    exportTemplate("", allPages.toTypedArray())
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        with(binding) {
            backup.setOnClickListener { }
            import.setOnClickListener { }
            export.setOnClickListener { }
        }
    }

    private fun setupObservers() {
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1)
        viewModel.archivesList
            .map {
                adapter.clear()
                adapter.addAll(it)
                binding.empty.isVisible = it.isEmpty()
                binding.archives.isVisible = it.isNotEmpty()
            }.launchIn(lifecycleScope)

        viewModel.messagesFlow.map {
            when (it) {
                is UiText.DynamicString -> {
                    Toast.makeText(context, it.text, Toast.LENGTH_SHORT).show()
                }

                is UiText.StringResource -> {
                    Toast.makeText(context, context?.getString(it.resId), Toast.LENGTH_SHORT).show()
                }
            }
        }.launchIn(lifecycleScope)
    }


    /**
     * Request the user to pick an archive.
     *
     * @param only_load true to directly load the archive without first importing it in the LL_EXT_DIR directory.
     */
    private fun selectFileToLoadOrImport(only_load: Boolean) {
        if (only_load) {
            loadLauncher.launch(arrayOf("*/*"))
        } else {
            importLauncher.launch(arrayOf("*/*"))
        }
    }

    override fun onClick(v: View?) {
        TODO("Not yet implemented")
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.loadArchive(null, parent?.adapter?.getItem(position).toString())
    }

    override fun onItemLongClick(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun onLongClick(v: View?): Boolean {
        TODO("Not yet implemented")
    }

    private fun exportTemplate(backupPath: String, includedPages: Array<Int>) {
        val sbHeight = Rect().apply {
            requireActivity().window.decorView.getWindowVisibleDisplayFrame(this)
        }.run {
            this.top
        }

        viewModel.exportTemplate(
            sbHeight = sbHeight,
            backupPath = backupPath,
            includedPages = includedPages
        )
    }

    private fun selectDesktopsToExport() {
        val intent = Intent(context, ScreenManager::class.java).apply {
            putExtra(
                API.SCREEN_PICKER_INTENT_EXTRA_SELECTED_SCREENS,
                LLApp.get().appEngine.globalConfig.screensOrder
            )
            putExtra(API.SCREEN_PICKER_INTENT_EXTRA_TITLE, getString(R.string.tmpl_s_p))
        }
        selectPagesLauncher.launch(intent)
    }

    private fun restoreBackup() {
        viewModel.restoreBackup("") { restoreResult ->
            when (restoreResult) {
                RestoreWorker.RestoreResult.RESTORE_TEMPLATE -> {
                    Intent(context, ApplyTemplate::class.java).apply {
                        putExtra(ApplyTemplate.INTENT_EXTRA_URI, "")
                    }.also {
                        startActivity(it)
                        requireActivity().finish()
                    }
                }

                RestoreWorker.RestoreResult.RESTORE_BACKUP -> {
                    startActivity(Intent(context, Dashboard::class.java))
                    exitProcess(0)
                }

                RestoreWorker.RestoreResult.RESTORE_NONE -> {}
            }
        }
    }

    private fun addSubPages(all_pages: ArrayList<Int>, p: Int) {
        val page = LLApp.get().appEngine.getOrLoadPage(p)
        for (i in page.items) {
            if (i is Folder) {
                val folder_page_id = i.folderPageId
                all_pages.add(folder_page_id)
                addSubPages(all_pages, folder_page_id)
            }
        }
    }

}