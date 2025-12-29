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

package com.android.settings.spa.app.specialaccess

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.CloudMediaProviderContract
import com.android.settings.R
import com.android.settingslib.spaprivileged.model.app.AppOps
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionRecord
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object CloudMediaProvidersAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "CloudMediaProviders"
    override fun createModel(context: Context) = CloudMediaProvidersListModel(context)
}

class CloudMediaProvidersListModel(context: Context) : AppOpPermissionListModel(context) {
    override val pageTitleResId = R.string.cloud_media_providers_title
    override val switchTitleResId = R.string.permit_cloud_media_provider
    override val footerResId = R.string.allow_cloud_media_provider_description
    override val appOps = AppOps(
        op = AppOpsManager.OP_MANAGE_CLOUD_MEDIA_PROVIDERS,
        setModeByUid = true,
    )
    override val permission = CloudMediaProviderContract.MANAGE_CLOUD_MEDIA_PROVIDERS_PERMISSION

    /**
     * Returns the set of package names that declare a CloudMediaProvider.
     */
    private fun getCloudMediaProviderPackages(): Set<String> {
        val intent = Intent(CloudMediaProviderContract.PROVIDER_INTERFACE)
        return context.packageManager
            .queryIntentContentProviders(intent, 0)
            .filter { resolveInfo ->
                resolveInfo.providerInfo.authority != null
            }
            .map { it.providerInfo.packageName }
            .toSet()
    }

    override fun filter(
        userIdFlow: Flow<Int>,
        recordListFlow: Flow<List<AppOpPermissionRecord>>
    ): Flow<List<AppOpPermissionRecord>> {
        return recordListFlow.map { recordList ->
            val providerPackages = getCloudMediaProviderPackages()
            recordList.filter { record ->
                record.app.packageName in providerPackages
            }
        }
    }
}
