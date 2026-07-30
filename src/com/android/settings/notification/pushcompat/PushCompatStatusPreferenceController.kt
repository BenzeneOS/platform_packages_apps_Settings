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

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.utils.ThreadUtils
import com.android.settingslib.widget.BannerMessagePreference

class PushCompatStatusPreferenceController(
    context: Context,
    key: String,
    private var manager: PushCompatManager,
) : BasePreferenceController(context, key) {
    constructor(context: Context, key: String) : this(
        context,
        key,
        PushCompatManager(context),
    )

    private var loadGeneration = 0
    private var shownState: BannerState? = null

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        val banner = screen.findPreference<BannerMessagePreference>(preferenceKey) ?: return
        shownState = null
        banner.setPositiveButtonText(R.string.pushcompat_retry)
        banner.setPositiveButtonOnClickListener { retry(banner) }
        banner.setPositiveButtonVisible(false)
        banner.isVisible = true
    }

    override fun updateState(preference: Preference) {
        val banner = preference as BannerMessagePreference
        val generation = ++loadGeneration
        ThreadUtils.postOnBackgroundThread {
            val status = manager.getStatus()
            ThreadUtils.postOnMainThread {
                if (generation == loadGeneration) {
                    applyStatus(banner, status)
                }
            }
        }
    }

    private fun applyStatus(
        banner: BannerMessagePreference,
        status: PushCompatStatus?,
    ) {
        val state = bannerState(status)
        if (state == shownState) {
            return
        }
        if (banner.title?.toString() != state.title.toString()) {
            banner.title = state.title
        }
        if (banner.summary?.toString() != state.summary.toString()) {
            banner.summary = state.summary
        }
        if (shownState?.attentionLevel != state.attentionLevel) {
            banner.setAttentionLevel(state.attentionLevel)
        }
        if (shownState?.icon != state.icon) {
            banner.setIcon(state.icon)
        }
        if (shownState?.retryAvailable != state.retryAvailable) {
            banner.setPositiveButtonVisible(state.retryAvailable)
        }
        shownState = state
    }

    private fun retry(banner: BannerMessagePreference) {
        banner.setPositiveButtonEnabled(false)
        ThreadUtils.postOnBackgroundThread {
            manager.reconcileNow()
            ThreadUtils.postOnMainThread {
                banner.setPositiveButtonEnabled(true)
                updateState(banner)
            }
        }
    }

    private fun bannerState(status: PushCompatStatus?): BannerState {
        if (status == null) {
            return BannerState(
                title = mContext.getText(R.string.pushcompat_banner_unavailable_title),
                summary = mContext.getText(R.string.pushcompat_unavailable),
                attentionLevel = BannerMessagePreference.AttentionLevel.HIGH,
                icon = R.drawable.ic_warning_24dp,
                retryAvailable = false,
            )
        }
        val relayRequired =
            status.unifiedPushCount > 0 ||
                (status.mode == PushCompatManager.MODE_PUSHCOMPAT && status.enabledCount > 0)
        val now = System.currentTimeMillis()
        val connectionFailed =
            status.connectFailureCount >= CONNECT_FAILURE_THRESHOLD ||
                (
                    status.socketState == PushCompatManager.SOCKET_CONNECTING &&
                        status.socketStateSinceMillis > 0L &&
                        now - status.socketStateSinceMillis >= CONNECT_FAILURE_TIMEOUT_MILLIS
                )
        if (relayRequired && connectionFailed) {
            return BannerState(
                title = mContext.getText(R.string.pushcompat_banner_connection_failed_title),
                summary = mContext.getText(R.string.pushcompat_banner_connection_failed_summary),
                attentionLevel = BannerMessagePreference.AttentionLevel.HIGH,
                icon = R.drawable.ic_sync_problem_24dp,
                retryAvailable = true,
            )
        }
        if (relayRequired && status.socketState == PushCompatManager.SOCKET_IDLE) {
            return BannerState(
                title = mContext.getText(R.string.pushcompat_banner_disconnected_title),
                summary = mContext.getText(R.string.pushcompat_banner_disconnected_summary),
                attentionLevel = BannerMessagePreference.AttentionLevel.HIGH,
                icon = R.drawable.ic_sync_problem_24dp,
                retryAvailable = true,
            )
        }
        if (relayRequired && status.socketState == PushCompatManager.SOCKET_CONNECTING) {
            return BannerState(
                title = mContext.getText(R.string.pushcompat_banner_connecting_title),
                summary = mContext.getText(R.string.pushcompat_banner_connecting_summary),
                attentionLevel = BannerMessagePreference.AttentionLevel.MEDIUM,
                icon = R.drawable.ic_sync,
                retryAvailable = true,
            )
        }
        if (status.unavailableCount > 0) {
            return BannerState(
                title =
                    mContext.resources.getQuantityString(
                        R.plurals.pushcompat_banner_unavailable_apps_title,
                        status.unavailableCount,
                        status.unavailableCount,
                    ),
                summary = mContext.getText(R.string.pushcompat_banner_unavailable_apps_summary),
                attentionLevel = BannerMessagePreference.AttentionLevel.HIGH,
                icon = R.drawable.ic_warning_24dp,
                retryAvailable = false,
            )
        }
        if (status.mode == PushCompatManager.MODE_PUSHCOMPAT &&
            status.enabledCount > status.activeCount
        ) {
            return BannerState(
                title = mContext.getText(R.string.pushcompat_banner_waiting_title),
                summary = mContext.getText(R.string.pushcompat_banner_waiting_summary),
                attentionLevel = BannerMessagePreference.AttentionLevel.MEDIUM,
                icon = R.drawable.ic_sync,
                retryAvailable = true,
            )
        }
        if (relayRequired &&
            status.socketState == PushCompatManager.SOCKET_CONNECTED &&
            status.connectedSinceMillis > 0L &&
            now - status.connectedSinceMillis < STABLE_CONNECTION_MILLIS
        ) {
            return BannerState(
                title = mContext.getText(R.string.pushcompat_banner_stabilizing_title),
                summary = mContext.getText(R.string.pushcompat_banner_stabilizing_summary),
                attentionLevel = BannerMessagePreference.AttentionLevel.NORMAL,
                icon = R.drawable.ic_sync,
                retryAvailable = false,
            )
        }
        if (status.mode == PushCompatManager.MODE_GMS) {
            return BannerState(
                title = mContext.getText(R.string.pushcompat_banner_play_services_title),
                summary = mContext.getText(R.string.pushcompat_status_play_services),
                attentionLevel = BannerMessagePreference.AttentionLevel.NORMAL,
                icon = R.drawable.ic_notifications,
                retryAvailable = false,
            )
        }
        return BannerState(
            title = mContext.getText(R.string.pushcompat_banner_connected_title),
            summary = mContext.getText(R.string.pushcompat_status_pushcompat),
            attentionLevel = BannerMessagePreference.AttentionLevel.NORMAL,
            icon = R.drawable.ic_check_circle_green_24dp,
            retryAvailable = false,
        )
    }

    fun setManager(manager: PushCompatManager) {
        this.manager = manager
    }
}

internal fun modeSummary(
    context: Context,
    status: PushCompatStatus,
): CharSequence =
    if (status.mode == PushCompatManager.MODE_GMS) {
        context.getText(R.string.pushcompat_status_play_services)
    } else {
        context.getText(R.string.pushcompat_status_pushcompat)
    }

private data class BannerState(
    val title: CharSequence,
    val summary: CharSequence,
    val attentionLevel: BannerMessagePreference.AttentionLevel,
    val icon: Int,
    val retryAvailable: Boolean,
)

private const val CONNECT_FAILURE_THRESHOLD = 3
private const val CONNECT_FAILURE_TIMEOUT_MILLIS = 2L * 60L * 1000L
private const val STABLE_CONNECTION_MILLIS = 60_000L
