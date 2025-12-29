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
import android.widget.SeekBar
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Custom SeekBar that allows setting both a minimum and maximum value.
 * This also handles floating point values (to configurable decimal places)
 * through integer conversions.
 */
class IntervalSeekBar(context: Context, attrs: AttributeSet) : SeekBar(context, attrs) {

    var minimum: Float
        private set
    var maximum: Float
        private set
    val default: Float
    private val multiplier: Float

    init {
        val seekBarType = context.obtainStyledAttributes(attrs, R.styleable.IntervalSeekBar, 0, 0)

        maximum = seekBarType.getFloat(R.styleable.IntervalSeekBar_maxI, 3.0f)
        minimum = seekBarType.getFloat(R.styleable.IntervalSeekBar_minI, 0.0f)
        default = seekBarType.getFloat(R.styleable.IntervalSeekBar_defaultValuePure, 1.0f)

        val digits = seekBarType.getInt(R.styleable.IntervalSeekBar_digits, 1)
        multiplier = 10f.pow(digits)

        if (minimum > maximum) {
            val temp = maximum
            maximum = minimum
            minimum = temp
        }

        max = convertFloatToProgress(maximum)
        setProgressFloat(default)

        seekBarType.recycle()
    }

    /**
     * Converts from SeekBar units (which the SeekBar uses), to scale units
     * (which are saved). This operation is the inverse of setProgressFloat.
     */
    fun getProgressFloat(): Float = (progress / multiplier) + minimum

    /**
     * Converts from scale units (which are saved), to SeekBar units
     * (which the SeekBar uses). This also sets the SeekBar progress.
     * This operation is the inverse of getProgressFloat.
     */
    fun setProgressFloat(progress: Float) {
        setProgress(convertFloatToProgress(progress))
    }

    private fun convertFloatToProgress(value: Float): Int =
        ((value - minimum) * multiplier).roundToInt()

    fun setMaximum(max: Float) {
        maximum = max
        setMax(convertFloatToProgress(maximum))
    }

    fun setMinimum(min: Float) {
        minimum = min
    }
}
