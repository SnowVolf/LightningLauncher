package net.pierrox.lightning_launcher.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.mintrocket.lib.mintpermissions.MintPermissions
import ru.mintrocket.lib.mintpermissions.flows.MintPermissionsFlow

object DataModule : FeatureModule {
    override val modules: List<Module>
        get() = listOf(
            permissionsModule,
        )
}

private val permissionsModule = module {
    single { MintPermissions.controller }
    factory { MintPermissions.createManager() }
    single { MintPermissionsFlow.dialogFlow }
    factory { MintPermissionsFlow.createManager() }
}