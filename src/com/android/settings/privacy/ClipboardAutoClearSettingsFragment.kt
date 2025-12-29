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

import android.app.settings.SettingsEnums
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

/**
 * Fragment for clipboard auto-clear settings.
 * Contains the timeout picker preference.
 */
@SearchIndexable
class ClipboardAutoClearSettingsFragment : DashboardFragment() {

    override fun getPreferenceScreenResId(): Int = R.xml.clipboard_auto_clear_settings

    override fun getLogTag(): String = TAG

    override fun getMetricsCategory(): Int = SettingsEnums.PRIVACY_CONTROLS

    companion object {
        private const val TAG = "ClipboardAutoClearSettings"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.clipboard_auto_clear_settings)
    }
}
