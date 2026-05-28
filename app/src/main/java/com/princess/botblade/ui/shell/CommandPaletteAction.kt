package com.princess.botblade.ui.shell

import androidx.compose.ui.graphics.vector.ImageVector

data class CommandPaletteAction(
    val id: String,
    val title: String,
    val detail: String,
    val icon: ImageVector? = null,
    val run: () -> Unit,
)
