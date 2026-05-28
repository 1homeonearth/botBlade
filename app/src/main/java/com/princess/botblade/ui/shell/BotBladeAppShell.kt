package com.princess.botblade.ui.shell

import android.view.View
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import com.princess.botblade.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotBladeAppShell(
    selectedDestination: BotBladeDestination,
    runtimeOnline: Boolean?,
    @IdRes fragmentContainerId: Int,
    onDestinationSelected: (BotBladeDestination) -> Unit,
    contentReady: () -> Unit,
) {
    var commandPaletteOpen by remember { mutableStateOf(false) }
    val statusLabel = when (runtimeOnline) {
        true -> "Backend online"
        false -> "Backend stopped"
        null -> "Backend checking"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Column {
                        Text("BotBlade", fontWeight = FontWeight.Bold)
                        Text(
                            selectedDestination.commandLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    AssistChip(
                        onClick = {},
                        label = { Text(statusLabel) },
                        leadingIcon = { Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    )
                    IconButton(onClick = { commandPaletteOpen = true }) {
                        Icon(Icons.Filled.KeyboardCommandKey, contentDescription = "Command palette")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                listOf(
                    BotBladeDestination.Dashboard,
                    BotBladeDestination.Projects,
                    BotBladeDestination.Editor,
                    BotBladeDestination.Deployments,
                ).forEach { destination ->
                    NavigationBarItem(
                        selected = selectedDestination == destination,
                        onClick = { onDestinationSelected(destination) },
                        icon = { Icon(destination.icon(), contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        FragmentHost(
            fragmentContainerId = fragmentContainerId,
            padding = padding,
            contentReady = contentReady,
        )
    }

    if (commandPaletteOpen) {
        CommandPaletteDialog(
            selectedDestination = selectedDestination,
            onDismiss = { commandPaletteOpen = false },
            onDestinationSelected = { destination ->
                commandPaletteOpen = false
                onDestinationSelected(destination)
            },
        )
    }
}

@Composable
private fun FragmentHost(
    @IdRes fragmentContainerId: Int,
    padding: PaddingValues,
    contentReady: () -> Unit,
) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        factory = {
            FragmentContainerView(context).apply {
                id = fragmentContainerId
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                post { contentReady() }
            }
        },
        update = { view ->
            if (view.id == View.NO_ID) view.id = fragmentContainerId
        },
    )
}

@Composable
private fun CommandPaletteDialog(
    selectedDestination: BotBladeDestination,
    onDismiss: () -> Unit,
    onDestinationSelected: (BotBladeDestination) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Command palette") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Jump between BotBlade workbench surfaces while Phase 1 migrates the chrome to Compose.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BotBladeDestination.entries.forEach { destination ->
                        FilterChip(
                            selected = selectedDestination == destination,
                            onClick = { onDestinationSelected(destination) },
                            label = { Text(destination.label) },
                            leadingIcon = { Icon(destination.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Phase 1 target", fontWeight = FontWeight.Bold)
                        Text(
                            "Compose shell, four-tab navigation, command overlay, and fragment-safe migration path.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(onClick = { onDestinationSelected(BotBladeDestination.Settings) }) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Text("Open Settings", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
    )
}

private fun BotBladeDestination.icon(): ImageVector = when (this) {
    BotBladeDestination.Dashboard -> Icons.Filled.DeveloperBoard
    BotBladeDestination.Projects -> Icons.Filled.Folder
    BotBladeDestination.Editor -> Icons.Filled.Code
    BotBladeDestination.Deployments -> Icons.Filled.RocketLaunch
    BotBladeDestination.Settings -> Icons.Filled.Settings
}
