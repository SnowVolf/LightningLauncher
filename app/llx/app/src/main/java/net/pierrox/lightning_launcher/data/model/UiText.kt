package net.pierrox.lightning_launcher.data.model

import androidx.annotation.StringRes

sealed class UiText {
    class DynamicString(
        val text: String,
    ) : UiText()

    class StringResource(
        @StringRes val resId: Int,
        vararg args: Any?,
    ) : UiText()

}