/*
 * Copyright (C) 2025 Amaan Qureshi <contact@amaanq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.privacy

import android.app.ActivityManager
import android.app.settings.SettingsEnums
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.settings.R
import com.android.settings.core.InstrumentedFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HideAppListSettings : InstrumentedFragment(), SearchView.OnQueryTextListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: AppListAdapter
    private var allApps: List<AppInfo> = emptyList()
    private var filteredApps: List<AppInfo> = emptyList()
    private var hiddenApps: MutableSet<String> = mutableSetOf()
    private var showSystemApps = false
    private var searchQuery = ""
    private var loadJob: Job? = null

    // Apps that should not appear in the hide list UI
    private val excludedApps = setOf(
        "android",
        "com.android.settings",
        "com.android.systemui",
        "com.android.shell"
    )

    override fun getMetricsCategory(): Int = SettingsEnums.PRIVACY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.hide_applist_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.apps_list)
        progressBar = view.findViewById(R.id.progress_bar)

        adapter = AppListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadApps()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadJob?.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.hide_applist_menu, menu)

        val searchItem = menu.findItem(R.id.menu_search)
        (searchItem?.actionView as? SearchView)?.setOnQueryTextListener(this)

        val showSystemItem = menu.findItem(R.id.menu_show_system)
        showSystemItem?.isChecked = showSystemApps
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_show_system -> {
                showSystemApps = !showSystemApps
                item.isChecked = showSystemApps
                filterApps()
                true
            }
            R.id.menu_manage_whitelist -> {
                parentFragmentManager.beginTransaction()
                    .replace(android.R.id.content, HideAppListWhitelistSettings())
                    .addToBackStack(null)
                    .commit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean = false

    override fun onQueryTextChange(newText: String?): Boolean {
        searchQuery = newText ?: ""
        filterApps()
        return true
    }

    private fun loadApps() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        loadJob = CoroutineScope(Dispatchers.IO).launch {
            // Load hidden apps from settings
            val hiddenAppsStr = Settings.Secure.getString(
                requireContext().contentResolver,
                Settings.Secure.HIDE_APPLIST
            ) ?: ""
            hiddenApps = hiddenAppsStr.split(",")
                .filter { it.isNotEmpty() }
                .toMutableSet()

            // Load all installed apps
            val pm = requireContext().packageManager
            val packages = pm.getInstalledPackages(PackageManager.MATCH_ANY_USER)

            allApps = packages
                .filter { !excludedApps.contains(it.packageName) }
                .map { pkg ->
                    AppInfo(
                        packageName = pkg.packageName,
                        label = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName,
                        icon = pkg.applicationInfo,
                        isSystemApp = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0,
                        isHidden = hiddenApps.contains(pkg.packageName)
                    )
                }
                .sortedBy { it.label.lowercase() }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                filterApps()
            }
        }
    }

    private fun filterApps() {
        filteredApps = allApps.filter { app ->
            val matchesSearch = searchQuery.isEmpty() ||
                app.label.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesSystemFilter = showSystemApps || !app.isSystemApp
            matchesSearch && matchesSystemFilter
        }
        adapter.notifyDataSetChanged()
    }

    private fun toggleApp(app: AppInfo) {
        if (app.isHidden) {
            hiddenApps.remove(app.packageName)
        } else {
            hiddenApps.add(app.packageName)
        }
        app.isHidden = !app.isHidden

        // Save to settings for all users
        val userManager = requireContext().getSystemService(UserManager::class.java)
        val users = userManager?.getUserHandles(true) ?: listOf(UserHandle.of(UserHandle.myUserId()))

        val newValue = hiddenApps.joinToString(",")
        for (user in users) {
            Settings.Secure.putStringForUser(
                requireContext().contentResolver,
                Settings.Secure.HIDE_APPLIST,
                newValue,
                user.identifier
            )
        }

        // Force stop the app to apply changes immediately
        try {
            val am = requireContext().getSystemService(ActivityManager::class.java)
            am?.forceStopPackage(app.packageName)
        } catch (e: Exception) {
            // Ignore - we may not have permission
        }
    }

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: ApplicationInfo?,
        val isSystemApp: Boolean,
        var isHidden: Boolean
    )

    inner class AppListAdapter : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.app_icon)
            val name: TextView = view.findViewById(R.id.app_name)
            val packageName: TextView = view.findViewById(R.id.app_package)
            val checkbox: CheckBox = view.findViewById(R.id.app_checkbox)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.hide_applist_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = filteredApps[position]
            val pm = holder.itemView.context.packageManager

            holder.name.text = app.label
            holder.packageName.text = app.packageName
            holder.checkbox.isChecked = app.isHidden

            app.icon?.let {
                holder.icon.setImageDrawable(it.loadIcon(pm))
            }

            holder.itemView.setOnClickListener {
                toggleApp(app)
                holder.checkbox.isChecked = app.isHidden
            }
        }

        override fun getItemCount(): Int = filteredApps.size
    }
}
