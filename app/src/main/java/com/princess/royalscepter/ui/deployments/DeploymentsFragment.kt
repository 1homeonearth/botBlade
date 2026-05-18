package com.princess.royalscepter.ui.deployments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.princess.royalscepter.R
import com.princess.royalscepter.data.store.ActiveProjectStore

class DeploymentsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_deployments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hasActiveProject = ActiveProjectStore(requireContext()).getActiveProjectId() != null
        view.findViewById<Button>(R.id.create_deployment_button).isEnabled = hasActiveProject
    }
}
