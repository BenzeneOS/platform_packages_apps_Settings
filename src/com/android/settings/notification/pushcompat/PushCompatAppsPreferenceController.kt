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
import android.content.pm.UserProperties
import android.graphics.drawable.Drawable
import android.os.UserManager
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.utils.ThreadUtils

class PushCompatAppsPreferenceController(
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
    private var renderedSnapshot: AppsSnapshot? = null
    private var appsCategory: PreferenceCategory? = null

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        appsCategory = screen.findPreference(preferenceKey)
    }

    override fun updateState(preference: Preference) {
        // A profile toggle still needs to refresh the main apps category.
        val category = appsCategory ?: preference as PreferenceCategory
        val generation = ++loadGeneration
        ThreadUtils.postOnBackgroundThread {
            val listed = manager.listApps()
            val listedUnifiedPush = manager.listUnifiedPushApps()
            val profiles = profileSections()
            val snapshot = AppsSnapshot(listed, listedUnifiedPush, profiles)
            // Rebuilding the sections tears the whole list down and pops icons back
            // in asynchronously, so a no-op refresh reads as visible flicker.
            if (snapshot == renderedSnapshot) {
                return@postOnBackgroundThread
            }
            val unifiedPushKeys =
                listedUnifiedPush.orEmpty().mapTo(mutableSetOf()) {
                    appKey(it.userId, it.packageName)
                }
            val apps =
                listed
                    ?.filter { appKey(it.userId, it.packageName) !in unifiedPushKeys }
                    ?.map { app ->
                        AppRow(
                            app = app,
                            icon = manager.loadAppIcon(app.packageName, app.userId),
                        )
                    }
            val unifiedPushApps =
                listedUnifiedPush
                    .orEmpty()
                    .map { app ->
                        UnifiedPushRow(
                            app = app,
                            icon = manager.loadAppIcon(app.packageName, app.userId),
                        )
                    }
            ThreadUtils.postOnMainThread {
                if (generation == loadGeneration) {
                    renderedSnapshot = snapshot
                    renderApps(category, apps, unifiedPushApps, profiles)
                }
            }
        }
    }

    // Rows and sections are synced in place — tearing the hierarchy down on every
    // data tick reads as flicker, and a toggle produces several ticks in a row.
    private fun renderApps(
        category: PreferenceCategory,
        apps: List<AppRow>?,
        unifiedPushApps: List<UnifiedPushRow>,
        profiles: List<ProfileSection>,
    ) {
        val screen = category.parent ?: return
        category.isEnabled = apps != null
        if (apps == null) {
            category.isVisible = true
            syncMessage(category, R.string.pushcompat_unavailable)
            removeStaleProfileCategories(screen, emptySet())
            return
        }
        val ownerApps = apps.filter { it.app.userId == mContext.userId }
        val ownerUnifiedPushApps =
            unifiedPushApps.filter { it.app.userId == mContext.userId }
        category.isVisible = true
        if (ownerApps.isEmpty() && ownerUnifiedPushApps.isEmpty()) {
            syncMessage(category, R.string.pushcompat_apps_empty)
        } else {
            syncRows(category, ownerApps, ownerUnifiedPushApps)
        }
        val wantedCategories = mutableSetOf<String>()
        profiles.forEachIndexed { index, profile ->
            val appRows = apps.filter { it.app.userId == profile.userId }
            val unifiedPushRows =
                unifiedPushApps.filter { it.app.userId == profile.userId }
            if (appRows.isEmpty() && unifiedPushRows.isEmpty() && !profile.paused) {
                return@forEachIndexed
            }
            val categoryKey = "$PROFILE_CATEGORY_PREFIX${profile.userId}"
            wantedCategories += categoryKey
            var profileCategory = screen.findPreference<PreferenceCategory>(categoryKey)
            if (profileCategory == null) {
                profileCategory = PreferenceCategory(mContext).apply { key = categoryKey }
                screen.addPreference(profileCategory)
            }
            profileCategory.order = category.order + 1 + index
            if (profileCategory.title != profile.name) {
                profileCategory.title = profile.name
            }
            if (profile.paused) {
                syncMessage(profileCategory, R.string.pushcompat_profile_paused)
            } else {
                syncRows(profileCategory, appRows, unifiedPushRows)
            }
        }
        removeStaleProfileCategories(screen, wantedCategories)
    }

    private fun syncRows(
        category: PreferenceCategory,
        appRows: List<AppRow>,
        unifiedPushRows: List<UnifiedPushRow>,
    ) {
        val wanted =
            appRows.mapTo(mutableSetOf()) {
                "$APP_KEY_PREFIX${it.app.userId}:${it.app.packageName}"
            }
        unifiedPushRows.mapTo(wanted) {
            "$UNIFIED_PUSH_KEY_PREFIX${it.app.userId}:${it.app.packageName}"
        }
        val stale = mutableListOf<Preference>()
        for (index in 0 until category.preferenceCount) {
            val child = category.getPreference(index)
            if (child.key !in wanted) {
                stale += child
            }
        }
        stale.forEach { category.removePreference(it) }
        appRows.forEachIndexed { index, row ->
            val key = "$APP_KEY_PREFIX${row.app.userId}:${row.app.packageName}"
            val existing = category.findPreference<SwitchPreferenceCompat>(key)
            if (existing == null) {
                category.addPreference(appPreference(category, row, index))
            } else {
                updateAppRow(existing, row, index)
            }
        }
        unifiedPushRows.forEachIndexed { index, row ->
            val key =
                "$UNIFIED_PUSH_KEY_PREFIX${row.app.userId}:${row.app.packageName}"
            val order = UNIFIED_PUSH_ORDER_OFFSET + index
            val existing = category.findPreference<Preference>(key)
            if (existing == null) {
                category.addPreference(unifiedPushPreference(row, order))
            } else {
                updateUnifiedPushRow(existing, row, order)
            }
        }
    }

    private fun updateAppRow(
        preference: SwitchPreferenceCompat,
        row: AppRow,
        order: Int,
    ) {
        val app = row.app
        preference.order = order
        val summary = appSummary(app)
        if (preference.summary != summary) {
            preference.summary = summary
        }
        if (preference.isChecked != app.enabled) {
            preference.isChecked = app.enabled
        }
        if (!preference.isEnabled) {
            preference.isEnabled = true
        }
        if (preference.icon == null) {
            preference.icon = row.icon
        }
    }

    private fun updateUnifiedPushRow(
        preference: Preference,
        row: UnifiedPushRow,
        order: Int,
    ) {
        preference.order = order
        if (preference.title?.toString() != row.app.label) {
            preference.title = row.app.label
        }
        val summary = unifiedPushSummary(row.app.packageName)
        if (preference.summary?.toString() != summary) {
            preference.summary = summary
        }
        if (!preference.isEnabled) {
            preference.isEnabled = true
        }
        if (preference.icon == null) {
            preference.icon = row.icon
        }
    }

    private fun syncMessage(
        category: PreferenceCategory,
        message: Int,
    ) {
        val text = mContext.getText(message)
        val existing =
            (category.findPreference<Preference>(MESSAGE_KEY))
                ?.takeIf { category.preferenceCount == 1 }
        if (existing != null) {
            if (existing.summary != text) {
                existing.summary = text
            }
            return
        }
        category.removeAll()
        category.addPreference(messagePreference(message))
    }

    private fun removeStaleProfileCategories(
        screen: PreferenceGroup,
        wanted: Set<String>,
    ) {
        val stale = mutableListOf<Preference>()
        for (index in 0 until screen.preferenceCount) {
            val child = screen.getPreference(index)
            if (child.key?.startsWith(PROFILE_CATEGORY_PREFIX) == true && child.key !in wanted) {
                stale += child
            }
        }
        stale.forEach { screen.removePreference(it) }
    }

    private fun appPreference(
        category: PreferenceCategory,
        row: AppRow,
        order: Int,
    ): Preference {
        val app = row.app
        return SwitchPreferenceCompat(mContext).apply {
            key = "$APP_KEY_PREFIX${app.userId}:${app.packageName}"
            icon = row.icon
            title = app.label
            summary = appSummary(app)
            isPersistent = false
            isChecked = app.enabled
            this.order = order
            onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, value ->
                    preference.isEnabled = false
                    ThreadUtils.postOnBackgroundThread {
                        manager.setAppEnabled(app.packageName, app.userId, value == true)
                        ThreadUtils.postOnMainThread {
                            renderedSnapshot = null
                            (appsCategory ?: category).let(::updateState)
                        }
                    }
                    false
                }
        }
    }

    private fun unifiedPushPreference(
        row: UnifiedPushRow,
        order: Int,
    ): Preference =
        Preference(mContext).apply {
            key = "$UNIFIED_PUSH_KEY_PREFIX${row.app.userId}:${row.app.packageName}"
            icon = row.icon
            title = row.app.label
            summary = unifiedPushSummary(row.app.packageName)
            isSelectable = false
            this.order = order
        }

    // A hidden-in-quiet-mode profile (private space) must be omitted entirely, not
    // listed as paused — a paused row would reveal that the private space exists.
    private fun profileSections(): List<ProfileSection> {
        val userManager = mContext.getSystemService(UserManager::class.java) ?: return emptyList()
        return userManager.userProfiles
            .filter { it.identifier != mContext.userId }
            .mapNotNull { user ->
                val paused = userManager.isQuietModeEnabled(user)
                if (paused &&
                    userManager.getUserProperties(user).showInQuietMode ==
                    UserProperties.SHOW_IN_QUIET_MODE_HIDDEN
                ) {
                    return@mapNotNull null
                }
                val name = userManager.getUserInfo(user.identifier)?.name ?: return@mapNotNull null
                ProfileSection(userId = user.identifier, name = name, paused = paused)
            }
    }

    private fun appSummary(app: PushCompatApp): String =
        if (app.enabled && !app.available) {
            mContext.getString(R.string.pushcompat_app_unavailable, app.packageName)
        } else if (app.enabled && !app.active) {
            mContext.getString(R.string.pushcompat_app_waiting, app.packageName)
        } else {
            app.packageName
        }

    private fun unifiedPushSummary(base: String): String =
        mContext.getString(R.string.pushcompat_app_unifiedpush, base)

    private fun appKey(
        userId: Int,
        packageName: String,
    ): String = "$userId:$packageName"

    private fun messagePreference(message: Int): Preference =
        Preference(mContext).apply {
            key = MESSAGE_KEY
            summary = mContext.getText(message)
            isSelectable = false
        }

    fun setManager(manager: PushCompatManager) {
        this.manager = manager
    }

    companion object {
        private const val APP_KEY_PREFIX = "pushcompat_app:"
        private const val UNIFIED_PUSH_KEY_PREFIX = "pushcompat_unified_push:"
        private const val PROFILE_CATEGORY_PREFIX = "pushcompat_apps_user:"
        private const val MESSAGE_KEY = "pushcompat_apps_message"
        private const val UNIFIED_PUSH_ORDER_OFFSET = 1000
    }

    private data class AppRow(
        val app: PushCompatApp,
        val icon: Drawable?,
    )

    private data class UnifiedPushRow(
        val app: PushCompatUnifiedPushApp,
        val icon: Drawable?,
    )

    private data class ProfileSection(
        val userId: Int,
        val name: String,
        val paused: Boolean,
    )

    private data class AppsSnapshot(
        val apps: List<PushCompatApp>?,
        val unifiedPushApps: List<PushCompatUnifiedPushApp>?,
        val profiles: List<ProfileSection>,
    )
}
