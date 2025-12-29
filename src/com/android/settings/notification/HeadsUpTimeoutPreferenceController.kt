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

package com.android.settings.notification

import android.content.Context
import android.provider.Settings
import android.widget.SeekBar
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.widget.SeekBarPreference

/**
 * Preference controller for heads-up notification timeout.
 */
class HeadsUpTimeoutPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key) {

    private val defaultTimeout = 5 // 5 seconds default

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)

        val seekBarPreference = screen.findPreference<SeekBarPreference>(preferenceKey)
        seekBarPreference?.apply {
            min = 1
            max = 60
            progress = Settings.System.getInt(
                mContext.contentResolver,
                Settings.System.HEADS_UP_TIMEOUT,
                defaultTimeout
            )
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        Settings.System.putInt(
                            mContext.contentResolver,
                            Settings.System.HEADS_UP_TIMEOUT,
                            progress
                        )
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            setSeekBarContentDescription(mContext.getString(R.string.heads_up_timeout_summary))
        }
    }
}
