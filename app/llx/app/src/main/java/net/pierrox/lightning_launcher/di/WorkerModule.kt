package net.pierrox.lightning_launcher.di

import androidx.work.WorkManager
import org.koin.core.module.Module
import org.koin.dsl.module

object WorkerModule : FeatureModule {
    override val modules: List<Module>
        get() = listOf(
            workerModule
        )

}

private val workerModule = module {
    factory {
        WorkManager.getInstance(get())
    }
}