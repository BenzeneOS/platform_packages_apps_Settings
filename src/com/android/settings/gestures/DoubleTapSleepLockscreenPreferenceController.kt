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

package com.android.settings.gestures

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.core.TogglePreferenceController
import com.android.settingslib.core.lifecycle.LifecycleObserver
import com.android.settingslib.core.lifecycle.events.OnStart
import com.android.settingslib.core.lifecycle.events.OnStop

/** Preference controller for double tap to sleep on the lockscreen. */
class DoubleTapSleepLockscreenPreferenceController(context: Context, key: String) :
    TogglePreferenceController(context, key),
    LifecycleObserver,
    OnStart,
    OnStop {

    private var preference: Preference? = null

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            preference?.let { updateState(it) }
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }

    override fun onStart() {
        mContext.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.DOUBLE_TAP_SLEEP_LOCKSCREEN),
            false,
            settingsObserver
        )
    }

    override fun onStop() {
        mContext.contentResolver.unregisterContentObserver(settingsObserver)
    }

    override fun isChecked(): Boolean =
        Settings.System.getInt(
            mContext.contentResolver,
            Settings.System.DOUBLE_TAP_SLEEP_LOCKSCREEN,
            0
        ) == 1

    override fun setChecked(isChecked: Boolean): Boolean =
        Settings.System.putInt(
            mContext.contentResolver,
            Settings.System.DOUBLE_TAP_SLEEP_LOCKSCREEN,
            if (isChecked) 1 else 0
        )

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun getSliceHighlightMenuRes(): Int = 0
}
