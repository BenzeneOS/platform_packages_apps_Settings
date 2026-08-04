package com.android.settings.applications

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.widget.ui.Category

@Composable
fun BenzenedRootCategory(app: ApplicationInfo) {
    Category(title = stringResource(R.string.benzened_root_category_title)) {
        AppBenzenedRootPreference(app)
    }
}
