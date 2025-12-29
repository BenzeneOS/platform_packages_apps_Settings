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

package com.android.settings.sound

import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import com.android.settings.notification.AudioHelper

/**
 * Preference controller for linking notification volume to ringer mode.
 *
 * When enabled, notification sounds are muted when ringer is set to silent or vibrate.
 * When disabled (default), notification volume is independent of ringer mode.
 */
class NotificationVolumeFollowsRingerController(
    context: Context,
    key: String
) : TogglePreferenceController(context, key) {

    private val helper = AudioHelper(context)

    override fun getAvailabilityStatus(): Int =
        if (mContext.resources.getBoolean(R.bool.config_show_notification_volume) && !helper.isSingleVolume)
            AVAILABLE
        else
            UNSUPPORTED_ON_DEVICE

    override fun isChecked(): Boolean =
        Settings.Global.getInt(mContext.contentResolver, Settings.Global.NOTIFICATION_VOLUME_FOLLOWS_RINGER, 0) == 1

    override fun setChecked(isChecked: Boolean): Boolean =
        Settings.Global.putInt(mContext.contentResolver, Settings.Global.NOTIFICATION_VOLUME_FOLLOWS_RINGER, if (isChecked) 1 else 0)

    override fun getSliceHighlightMenuRes(): Int = R.string.menu_key_sound
}
