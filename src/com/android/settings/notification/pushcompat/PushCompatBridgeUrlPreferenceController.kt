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
import com.android.settings.widget.ValidatedEditTextPreference
import com.android.settingslib.utils.ThreadUtils

class PushCompatBridgeUrlPreferenceController(
    context: Context,
    key: String,
    private var manager: PushCompatManager,
) : BasePreferenceController(context, key),
    ValidatedEditTextPreference.Validator,
    Preference.OnPreferenceChangeListener {
    constructor(context: Context, key: String) : this(
        context,
        key,
        PushCompatManager(context),
    )

    private var preference: ValidatedEditTextPreference? = null

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference =
            screen.findPreference<ValidatedEditTextPreference>(preferenceKey)?.apply {
                setValidator(this@PushCompatBridgeUrlPreferenceController)
                onPreferenceChangeListener = this@PushCompatBridgeUrlPreferenceController
            }
    }

    private var loadGeneration = 0

    override fun updateState(preference: Preference) {
        val editPreference = preference as ValidatedEditTextPreference
        val generation = ++loadGeneration
        ThreadUtils.postOnBackgroundThread {
            val status = manager.getStatus()
            ThreadUtils.postOnMainThread {
                if (generation != loadGeneration) {
                    return@postOnMainThread
                }
                if (editPreference.isEnabled != (status != null)) {
                    editPreference.isEnabled = status != null
                }
                val summary =
                    status?.bridgeUrl ?: mContext.getText(R.string.pushcompat_unavailable)
                if (editPreference.summary?.toString() != summary.toString()) {
                    editPreference.summary = summary
                }
                if (status != null && editPreference.text != status.bridgeUrl) {
                    editPreference.text = status.bridgeUrl
                }
            }
        }
    }

    override fun isTextValid(value: String): Boolean = PushCompatManager.isValidBridgeUrl(value)

    override fun onPreferenceChange(
        preference: Preference,
        newValue: Any?,
    ): Boolean {
        val value = PushCompatManager.normalizeBridgeUrl(newValue?.toString().orEmpty())
        if (!isTextValid(value)) {
            return false
        }
        val editPreference = preference as ValidatedEditTextPreference
        editPreference.isEnabled = false
        ThreadUtils.postOnBackgroundThread {
            val applied = manager.setBridgeUrl(value)
            ThreadUtils.postOnMainThread {
                if (applied) {
                    editPreference.text = value
                    editPreference.summary = value
                }
                updateState(editPreference)
            }
        }
        return false
    }

    fun setManager(manager: PushCompatManager) {
        this.manager = manager
    }
}
