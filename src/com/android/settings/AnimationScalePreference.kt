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

package com.android.settings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.android.settingslib.CustomDialogPreferenceCompat

class AnimationScalePreference(
    context: Context,
    attrs: AttributeSet
) : CustomDialogPreferenceCompat(context, attrs), SeekBar.OnSeekBarChangeListener {

    private var scaleText: TextView? = null
    private var seekBar: IntervalSeekBar? = null

    var scale: Float = 1.0f
        set(value) {
            field = value
            summary = formatScale(value)
        }

    init {
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
        setDialogLayoutResource(R.layout.preference_dialog_animation_scale)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        scaleText = view.findViewById<TextView>(R.id.scale)?.apply {
            text = formatScale(scale)
        }

        seekBar = view.findViewById<IntervalSeekBar>(R.id.scale_seekbar)?.apply {
            setProgressFloat(scale)
            setOnSeekBarChangeListener(this@AnimationScalePreference)
        }
    }

    private fun formatScale(scale: Float): String = String.format("%.1fx", scale)

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            seekBar?.let {
                scale = it.getProgressFloat()
                callChangeListener(scale)
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        this.seekBar?.let { scaleText?.text = formatScale(it.getProgressFloat()) }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}

    override fun onStopTrackingTouch(seekBar: SeekBar) {}
}
