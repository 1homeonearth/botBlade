package com.princess.botblade.ui.shell

import androidx.fragment.app.Fragment
import com.princess.botblade.ui.dashboard.DashboardFragment
import com.princess.botblade.ui.deployments.DeploymentsFragment
import com.princess.botblade.ui.editor.CodeEditorComposeFragment
import com.princess.botblade.ui.importforge.ImportForgeFragment
import com.princess.botblade.ui.settings.SettingsComposeFragment

enum class BotBladeDestination(val label: String, val commandLabel: String) {
    Dashboard("Home", "Open Dashboard"),
    Projects("Projects", "Open Import Forge"),
    Editor("Editor", "Open Code Editor"),
    Deployments("Deploy", "Open Deployment Pipeline"),
    Settings("Settings", "Open Settings");

    fun createFragment(): Fragment = when (this) {
        Dashboard -> DashboardFragment()
        Projects -> ImportForgeFragment()
        Editor -> CodeEditorComposeFragment()
        Deployments -> DeploymentsFragment.newInstance()
        Settings -> SettingsComposeFragment()
    }
}
