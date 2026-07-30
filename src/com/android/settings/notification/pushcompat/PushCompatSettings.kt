/*
 * Copyright (C) 2026 Amaan Qureshi <contact@amaanq.com>
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

package com.android.settings.notification.pushcompat

import android.app.settings.SettingsEnums
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.utils.ThreadUtils
import java.text.DateFormat
import java.util.Date

@SearchIndexable
class PushCompatSettings : DashboardFragment() {
    private val handler = Handler(Looper.getMainLooper())

    // Only refresh the section that changed.
    private inner class SectionObserver(
        private val refresh: () -> Unit,
    ) : ContentObserver(handler) {
        private val runnable = Runnable { refresh() }

        override fun onChange(selfChange: Boolean) {
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, OBSERVER_DEBOUNCE_MILLIS)
        }

        fun cancel() {
            handler.removeCallbacks(runnable)
        }
    }

    private val statusObserver = SectionObserver { refreshStatus() }
    private val appsObserver = SectionObserver { refreshApps() }
    private val deliveriesObserver = SectionObserver { renderDeliveries() }

    private lateinit var manager: PushCompatManager
    private lateinit var statusController: PushCompatStatusPreferenceController
    private lateinit var bridgeUrlController: PushCompatBridgeUrlPreferenceController
    private lateinit var onDeviceController: PushCompatOnDevicePreferenceController
    private lateinit var appsController: PushCompatAppsPreferenceController
    private var observersRegistered = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        manager = PushCompatManager(context)
        statusController =
            use(PushCompatStatusPreferenceController::class.java).also { it.setManager(manager) }
        bridgeUrlController =
            use(PushCompatBridgeUrlPreferenceController::class.java).also {
                it.setManager(manager)
            }
        onDeviceController =
            use(PushCompatOnDevicePreferenceController::class.java).also {
                it.setManager(manager)
            }
        appsController =
            use(PushCompatAppsPreferenceController::class.java).also { it.setManager(manager) }
    }

    override fun onStart() {
        super.onStart()
        val resolver = requireContext().contentResolver
        resolver.registerContentObserver(PushCompatManager.STATUS_URI, false, statusObserver)
        resolver.registerContentObserver(PushCompatManager.APPS_URI, false, appsObserver)
        resolver.registerContentObserver(PushCompatManager.UNIFIED_PUSH_URI, false, appsObserver)
        resolver.registerContentObserver(
            PushCompatManager.DELIVERIES_URI,
            false,
            deliveriesObserver,
        )
        observersRegistered = true
        refreshPage()
    }

    override fun onStop() {
        statusObserver.cancel()
        appsObserver.cancel()
        deliveriesObserver.cancel()
        if (observersRegistered) {
            val resolver = requireContext().contentResolver
            resolver.unregisterContentObserver(statusObserver)
            resolver.unregisterContentObserver(appsObserver)
            resolver.unregisterContentObserver(deliveriesObserver)
            observersRegistered = false
        }
        super.onStop()
    }

    private fun refreshStatus() {
        findPreference<Preference>(KEY_STATUS)?.let(statusController::updateState)
        findPreference<Preference>(KEY_BRIDGE_URL)?.let(bridgeUrlController::updateState)
    }

    private fun refreshApps() {
        findPreference<Preference>(KEY_APPS)?.let(appsController::updateState)
        findPreference<Preference>(KEY_ON_DEVICE)?.let(onDeviceController::updateState)
    }

    override fun getPreferenceScreenResId(): Int = R.xml.pushcompat_settings

    override fun getLogTag(): String = TAG

    override fun getMetricsCategory(): Int = SettingsEnums.CONFIGURE_NOTIFICATION

    private fun refreshPage() {
        refreshStatus()
        refreshApps()
        renderDeliveries()
    }

    private var renderedDeliveries: List<PushCompatDelivery>? = null
    private var deliveriesGeneration = 0

    private fun renderDeliveries() {
        val generation = ++deliveriesGeneration
        ThreadUtils.postOnBackgroundThread {
            val deliveries = manager.getDeliveryLog(MAX_VISIBLE_DELIVERIES)
            ThreadUtils.postOnMainThread {
                if (generation == deliveriesGeneration && isAdded) {
                    applyDeliveries(deliveries)
                }
            }
        }
    }

    private fun applyDeliveries(deliveries: List<PushCompatDelivery>?) {
        val category =
            findPreference<PreferenceCategory>(KEY_DELIVERIES) ?: return
        if (deliveries != null && deliveries == renderedDeliveries) {
            return
        }
        renderedDeliveries = deliveries
        category.isEnabled = deliveries != null
        if (deliveries == null) {
            category.removeAll()
            category.addPreference(messagePreference(R.string.pushcompat_unavailable))
            return
        }
        if (deliveries.isEmpty()) {
            category.removeAll()
            category.addPreference(messagePreference(R.string.pushcompat_deliveries_empty))
            return
        }
        val wanted = deliveries.indices.mapTo(mutableSetOf()) { "pushcompat_delivery:$it" }
        val stale = mutableListOf<Preference>()
        for (index in 0 until category.preferenceCount) {
            val child = category.getPreference(index)
            if (child.key !in wanted) {
                stale += child
            }
        }
        stale.forEach { category.removePreference(it) }
        val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)
        deliveries.forEachIndexed { index, delivery ->
            val rowKey = "pushcompat_delivery:$index"
            val title = manager.badgedLabel(delivery.appId, delivery.userId)
            val summary =
                getString(
                    R.string.pushcompat_delivery_summary,
                    formatter.format(Date(delivery.timestamp)),
                    deliveryKind(delivery.kind),
                    deliveryDetail(delivery),
                )
            val existing = category.findPreference<Preference>(rowKey)
            if (existing == null) {
                category.addPreference(
                    Preference(requireContext()).apply {
                        key = rowKey
                        this.title = title
                        this.summary = summary
                        isSelectable = false
                        order = index
                    },
                )
            } else {
                if (existing.title?.toString() != title.toString()) {
                    existing.title = title
                }
                if (existing.summary?.toString() != summary) {
                    existing.summary = summary
                }
            }
        }
    }

    private fun deliveryKind(kind: String): String =
        when (kind) {
            "fcm" -> "FCM"
            "unified_push" -> "UnifiedPush"
            else -> kind
        }

    private fun deliveryDetail(delivery: PushCompatDelivery): CharSequence =
        if (delivery.delivered) {
            delivery.detail
        } else {
            getText(R.string.pushcompat_delivery_not_delivered)
        }

    private fun messagePreference(message: Int): Preference =
        Preference(requireContext()).apply {
            summary = getText(message)
            isSelectable = false
        }

    companion object {
        private const val TAG = "PushCompatSettings"
        private const val KEY_STATUS = "pushcompat_status"
        private const val KEY_BRIDGE_URL = "pushcompat_bridge_url"
        private const val KEY_ON_DEVICE = "pushcompat_on_device"
        private const val KEY_APPS = "pushcompat_apps"
        private const val KEY_DELIVERIES = "pushcompat_deliveries"
        private const val MAX_VISIBLE_DELIVERIES = 12
        private const val OBSERVER_DEBOUNCE_MILLIS = 500L

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER =
            object : BaseSearchIndexProvider(R.xml.pushcompat_settings) {
                override fun isPageSearchEnabled(context: Context): Boolean = PushCompatManager(context).isBackendAvailable()
            }
    }
}
