package net.pierrox.lightning_launcher.di

import net.pierrox.lightning_launcher.data.repostitories.backup.BackupRestoreRepository
import net.pierrox.lightning_launcher.data.repostitories.backup.BackupRestoreRepositoryImpl
import net.pierrox.lightning_launcher.feature.backup.BackupRestoreViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

object BackupRestoreModule : FeatureModule {

    override val modules: List<Module>
        get() = listOf(
            repositoriesModule,
            viewModelsModule,
        )

}

private val repositoriesModule = module {
    factory<BackupRestoreRepository> {
        BackupRestoreRepositoryImpl(
            workManager = get()
        )
    }
}

private val viewModelsModule = module {
    viewModel {
        BackupRestoreViewModel(
            repository = get(),
            permissionsFlow = get(),
        )
    }
}