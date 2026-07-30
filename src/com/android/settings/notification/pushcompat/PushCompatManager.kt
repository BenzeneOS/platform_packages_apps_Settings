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
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.util.IconDrawableFactory
import java.net.URI

data class PushCompatStatus(
    val mode: Int,
    val gmsPresent: Boolean,
    val socketState: Int,
    val socketStateSinceMillis: Long,
    val connectedSinceMillis: Long,
    val connectFailureCount: Int,
    val lastSocketError: String?,
    val bridgeUrl: String,
    val enabledCount: Int,
    val activeCount: Int,
    val unavailableCount: Int,
    val unifiedPushCount: Int,
    val deliveryMode: Int,
)

data class PushCompatApp(
    val userId: Int,
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val active: Boolean,
    val available: Boolean,
)

data class PushCompatUnifiedPushApp(
    val userId: Int,
    val packageName: String,
    val label: String,
)

data class PushCompatDelivery(
    val userId: Int,
    val timestamp: Long,
    val appId: String,
    val kind: String,
    val delivered: Boolean,
    val detail: String,
)

open class PushCompatManager(
    protected val context: Context,
) {
    private val iconDrawableFactory by lazy { IconDrawableFactory.newInstance(context) }

    open fun isBackendAvailable(): Boolean =
        runCatching {
            val provider =
                context.packageManager.resolveContentProvider(
                    AUTHORITY,
                    PackageManager.MATCH_DISABLED_COMPONENTS,
                )
            provider != null && provider.enabled && provider.applicationInfo?.enabled == true
        }.getOrDefault(false)

    open fun getStatus(): PushCompatStatus? {
        val result = call(METHOD_GET_STATUS) ?: return null
        val gmsPresent = result.getBoolean(KEY_GMS_PRESENT, false)
        return PushCompatStatus(
            mode = result.getInt(KEY_MODE, if (gmsPresent) MODE_GMS else MODE_PUSHCOMPAT),
            gmsPresent = gmsPresent,
            socketState = result.getInt(KEY_SOCKET_STATE, SOCKET_IDLE),
            socketStateSinceMillis = result.getLong(KEY_SOCKET_STATE_SINCE, 0L),
            connectedSinceMillis = result.getLong(KEY_CONNECTED_SINCE, 0L),
            connectFailureCount = result.getInt(KEY_CONNECT_FAILURE_COUNT, 0),
            lastSocketError = result.getString(KEY_LAST_SOCKET_ERROR),
            bridgeUrl = result.getString(KEY_BRIDGE_URL).orEmpty(),
            enabledCount = result.getInt(KEY_ENABLED_COUNT, 0),
            activeCount = result.getInt(KEY_ACTIVE_COUNT, 0),
            unavailableCount = result.getInt(KEY_UNAVAILABLE_COUNT, 0),
            unifiedPushCount = result.getInt(KEY_UNIFIED_PUSH_COUNT, 0),
            deliveryMode =
                result
                    .getInt(KEY_DELIVERY_MODE, DELIVERY_MODE_BRIDGE)
                    .takeIf { it == DELIVERY_MODE_ON_DEVICE } ?: DELIVERY_MODE_BRIDGE,
        )
    }

    open fun listApps(): List<PushCompatApp>? =
        query(APPS_URI) { cursor ->
            PushCompatApp(
                userId = cursor.int(COLUMN_USER_ID),
                packageName = cursor.string(COLUMN_PACKAGE_NAME),
                label = cursor.string(COLUMN_LABEL),
                enabled = cursor.boolean(COLUMN_ENABLED),
                active = cursor.boolean(COLUMN_ACTIVE),
                available = cursor.boolean(COLUMN_AVAILABLE),
            )
        }

    open fun listUnifiedPushApps(): List<PushCompatUnifiedPushApp>? =
        query(UNIFIED_PUSH_URI) { cursor ->
            val userId = cursor.int(COLUMN_USER_ID)
            val packageName = cursor.string(COLUMN_PACKAGE_NAME)
            PushCompatUnifiedPushApp(
                userId = userId,
                packageName = packageName,
                label = loadAppLabel(packageName, userId),
            )
        }?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

    open fun loadAppIcon(
        packageName: String,
        userId: Int,
    ): Drawable? =
        runCatching {
            val appInfo =
                context.packageManager.getApplicationInfoAsUser(packageName, 0, userId)
            iconDrawableFactory.getBadgedIcon(appInfo, userId)
        }.getOrNull()

    private fun loadAppLabel(
        packageName: String,
        userId: Int,
    ): String =
        runCatching {
            val appInfo = context.packageManager.getApplicationInfoAsUser(packageName, 0, userId)
            context.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)

    open fun badgedLabel(
        label: CharSequence,
        userId: Int,
    ): CharSequence =
        if (userId == context.userId) {
            label
        } else {
            context.packageManager.getUserBadgedLabel(label, UserHandle.of(userId))
        }

    open fun getDeliveryLog(limit: Int): List<PushCompatDelivery>? {
        val uri =
            DELIVERIES_URI
                .buildUpon()
                .appendQueryParameter("limit", limit.toString())
                .build()
        return query(uri) { cursor ->
            PushCompatDelivery(
                userId = cursor.int(COLUMN_USER_ID),
                timestamp = cursor.long(COLUMN_TIMESTAMP),
                appId = cursor.string(COLUMN_APP_ID),
                kind = cursor.string(COLUMN_KIND),
                delivered = cursor.boolean(COLUMN_DELIVERED),
                detail = cursor.string(COLUMN_DETAIL),
            )
        }
    }

    open fun setAppEnabled(
        packageName: String,
        userId: Int,
        enabled: Boolean,
    ): Boolean =
        call(
            METHOD_SET_APP_ENABLED,
            packageName,
            Bundle().apply {
                putBoolean(KEY_ENABLED, enabled)
                putInt(KEY_USER_ID, userId)
            },
        ) != null

    open fun setDeliveryMode(mode: Int): Boolean =
        call(
            METHOD_SET_DELIVERY_MODE,
            null,
            Bundle().apply { putInt(KEY_DELIVERY_MODE, mode) },
        ) != null

    open fun setBridgeUrl(url: String): Boolean = call(METHOD_SET_BRIDGE_URL, normalizeBridgeUrl(url)) != null

    open fun reconcileNow(): Boolean = call(METHOD_RECONCILE_NOW) != null

    private fun call(
        method: String,
        arg: String? = null,
        extras: Bundle? = null,
    ): Bundle? {
        if (!isBackendAvailable()) {
            return null
        }
        return try {
            val client =
                context.contentResolver.acquireUnstableContentProviderClient(AUTHORITY)
                    ?: return null
            val result =
                try {
                    client.call(method, arg, extras)
                } finally {
                    client.close()
                }
            result?.takeIf { it.getInt(KEY_ERROR, ERROR_NONE) == ERROR_NONE }
        } catch (_: Exception) {
            null
        }
    }

    private fun <T> query(
        uri: Uri,
        parse: (Cursor) -> T,
    ): List<T>? {
        if (!isBackendAvailable()) {
            return null
        }
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
            cursor.use {
                buildList {
                    while (it.moveToNext()) {
                        add(parse(it))
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun Cursor.columnIndex(name: String): Int = getColumnIndex(name)

    private fun Cursor.string(name: String): String {
        val index = columnIndex(name)
        return if (index < 0 || isNull(index)) "" else getString(index).orEmpty()
    }

    private fun Cursor.boolean(name: String): Boolean {
        val index = columnIndex(name)
        return index >= 0 && !isNull(index) && getInt(index) != 0
    }

    private fun Cursor.int(name: String): Int {
        val index = columnIndex(name)
        return if (index < 0 || isNull(index)) 0 else getInt(index)
    }

    private fun Cursor.long(name: String): Long {
        val index = columnIndex(name)
        return if (index < 0 || isNull(index)) 0L else getLong(index)
    }

    companion object {
        const val AUTHORITY = "com.benzeneos.pushcompat.management"

        @JvmField
        val STATUS_URI: Uri = Uri.parse("content://$AUTHORITY/status")

        @JvmField
        val APPS_URI: Uri = Uri.parse("content://$AUTHORITY/apps")

        @JvmField
        val UNIFIED_PUSH_URI: Uri = Uri.parse("content://$AUTHORITY/unified_push")

        @JvmField
        val DELIVERIES_URI: Uri = Uri.parse("content://$AUTHORITY/deliveries")

        const val MODE_PUSHCOMPAT = 1
        const val MODE_GMS = 3
        const val SOCKET_IDLE = 0
        const val SOCKET_CONNECTING = 1
        const val SOCKET_CONNECTED = 2
        const val DELIVERY_MODE_BRIDGE = 0
        const val DELIVERY_MODE_ON_DEVICE = 1

        private const val METHOD_GET_STATUS = "getStatus"
        private const val METHOD_SET_APP_ENABLED = "setAppEnabled"
        private const val METHOD_SET_DELIVERY_MODE = "setDeliveryMode"
        private const val METHOD_SET_BRIDGE_URL = "setBridgeUrl"
        private const val METHOD_RECONCILE_NOW = "reconcileNow"

        private const val KEY_ERROR = "error"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_USER_ID = "userId"
        private const val KEY_DELIVERY_MODE = "deliveryMode"
        private const val KEY_MODE = "mode"
        private const val KEY_GMS_PRESENT = "gmsPresent"
        private const val KEY_SOCKET_STATE = "socketState"
        private const val KEY_SOCKET_STATE_SINCE = "socketStateSince"
        private const val KEY_CONNECTED_SINCE = "connectedSince"
        private const val KEY_CONNECT_FAILURE_COUNT = "connectFailureCount"
        private const val KEY_LAST_SOCKET_ERROR = "lastSocketError"
        private const val KEY_BRIDGE_URL = "bridgeUrl"
        private const val KEY_ENABLED_COUNT = "enabledCount"
        private const val KEY_ACTIVE_COUNT = "activeCount"
        private const val KEY_UNAVAILABLE_COUNT = "unavailableCount"
        private const val KEY_UNIFIED_PUSH_COUNT = "unifiedPushCount"

        private const val COLUMN_USER_ID = "userId"
        private const val COLUMN_PACKAGE_NAME = "packageName"
        private const val COLUMN_LABEL = "label"
        private const val COLUMN_ENABLED = "enabled"
        private const val COLUMN_ACTIVE = "active"
        private const val COLUMN_AVAILABLE = "available"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_APP_ID = "appId"
        private const val COLUMN_KIND = "kind"
        private const val COLUMN_DELIVERED = "delivered"
        private const val COLUMN_DETAIL = "detail"

        private const val ERROR_NONE = 0

        fun normalizeBridgeUrl(value: String): String = value.trim().trimEnd('/')

        @JvmStatic
        fun isValidBridgeUrl(value: String): Boolean {
            val uri = runCatching { URI(normalizeBridgeUrl(value)) }.getOrNull()
            return uri?.scheme == "https" &&
                !uri.host.isNullOrEmpty() &&
                uri.userInfo == null &&
                uri.query == null &&
                uri.fragment == null &&
                (uri.port == -1 || uri.port in 1..65535)
        }
    }
}
