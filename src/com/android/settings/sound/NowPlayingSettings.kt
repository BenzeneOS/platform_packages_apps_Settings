package com.android.settings.sound

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class NowPlayingSettings : DashboardFragment() {
  private lateinit var enabledPreference: SwitchPreferenceCompat
  private lateinit var storageCapPreference: ListPreference
  private lateinit var catalogPreference: Preference
  private lateinit var deletePreference: Preference

  override fun getPreferenceScreenResId(): Int = R.xml.now_playing_settings

  override fun getLogTag(): String = TAG

  override fun getMetricsCategory(): Int = SettingsEnums.SOUND

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enabledPreference = requireNotNull(findPreference(KEY_ENABLED))
    storageCapPreference = requireNotNull(findPreference(KEY_STORAGE_CAP))
    catalogPreference = requireNotNull(findPreference(KEY_CATALOG))
    deletePreference = requireNotNull(findPreference(KEY_DELETE))

    enabledPreference.setOnPreferenceChangeListener { _, newValue ->
      setEnabled(newValue as Boolean)
    }
    storageCapPreference.setOnPreferenceChangeListener { _, newValue ->
      val value = (newValue as String).toLongOrNull() ?: return@setOnPreferenceChangeListener false
      if (!Settings.Global.putLong(contentResolver, STORAGE_CAP, value)) {
        return@setOnPreferenceChangeListener false
      }
      storageCapPreference.value = newValue
      updateStorageCap()
      true
    }
    requireNotNull<Preference>(findPreference(KEY_REFRESH)).setOnPreferenceClickListener {
      val requested = callProvider(METHOD_REFRESH)?.getBoolean(KEY_SUCCESS) == true
      Toast.makeText(
              requireContext(),
              if (requested) R.string.now_playing_refresh_requested
              else R.string.now_playing_action_failed,
              Toast.LENGTH_SHORT,
          )
          .show()
      true
    }
    deletePreference.setOnPreferenceClickListener {
      showDeleteDialog()
      true
    }
    requireNotNull<Preference>(findPreference(KEY_HISTORY)).setOnPreferenceClickListener {
      startActivity(
          Intent().setComponent(ComponentName(NOW_PLAYING_PACKAGE, HISTORY_ACTIVITY)),
      )
      true
    }
  }

  override fun onResume() {
    super.onResume()
    enabledPreference.isChecked = isEnabled()
    updateStorageCap()
    updateCatalogState()
  }

  private fun setEnabled(enabled: Boolean): Boolean {
    val previous = isEnabled()
    if (!Settings.Global.putInt(contentResolver, ENABLED, if (enabled) 1 else 0)) {
      return false
    }
    if (callProvider(METHOD_APPLY_ENABLED)?.getBoolean(KEY_SUCCESS) == true) {
      return true
    }
    Settings.Global.putInt(contentResolver, ENABLED, if (previous) 1 else 0)
    callProvider(METHOD_APPLY_ENABLED)
    Toast.makeText(requireContext(), R.string.now_playing_action_failed, Toast.LENGTH_SHORT).show()
    return false
  }

  private fun updateStorageCap() {
    val value = Settings.Global.getLong(contentResolver, STORAGE_CAP, 0L).toString()
    storageCapPreference.value = value
    val index = storageCapPreference.findIndexOfValue(value).takeIf { it >= 0 } ?: 0
    val label = storageCapPreference.entries[index]
    storageCapPreference.summary = getString(R.string.now_playing_storage_cap_summary, label)
  }

  private fun updateCatalogState() {
    val state = callProvider(METHOD_CATALOG_STATE)
    if (state == null) {
      catalogPreference.summary = getString(R.string.now_playing_catalog_unavailable)
      deletePreference.isEnabled = false
      return
    }
    val count = state.getInt(KEY_SHARD_COUNT)
    val bytes = state.getLong(KEY_CATALOG_BYTES)
    val build = state.getString(KEY_BUILD)
    catalogPreference.summary =
        if (count == 0 || build == null) {
          getString(R.string.now_playing_catalog_empty)
        } else {
          getString(
              R.string.now_playing_catalog_summary,
              build,
              count,
              Formatter.formatFileSize(requireContext(), bytes),
          )
        }
    deletePreference.isEnabled = count > 0
  }

  private fun showDeleteDialog() {
    AlertDialog.Builder(requireContext())
        .setTitle(R.string.now_playing_delete_catalog_title)
        .setMessage(R.string.now_playing_delete_catalog_message)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(R.string.now_playing_delete) { _, _ ->
          val deleted = callProvider(METHOD_DELETE)?.getBoolean(KEY_SUCCESS) == true
          Toast.makeText(
                  requireContext(),
                  if (deleted) R.string.now_playing_catalog_deleted
                  else R.string.now_playing_action_failed,
                  Toast.LENGTH_SHORT,
              )
              .show()
          updateCatalogState()
        }
        .show()
  }

  private fun callProvider(method: String): Bundle? =
      try {
        contentResolver.call(PROVIDER_URI, method, null, null)
      } catch (_: RuntimeException) {
        null
      }

  private fun isEnabled(): Boolean = Settings.Global.getInt(contentResolver, ENABLED, 0) != 0

  companion object {
    private const val TAG = "NowPlayingSettings"
    private const val NOW_PLAYING_PACKAGE = "com.benzeneos.nowplaying"
    private const val HISTORY_ACTIVITY = "$NOW_PLAYING_PACKAGE.HistoryActivity"
    private const val ENABLED = "now_playing_enabled"
    private const val STORAGE_CAP = "now_playing_storage_cap_mib"
    private const val KEY_ENABLED = "now_playing_enabled"
    private const val KEY_STORAGE_CAP = "now_playing_storage_cap"
    private const val KEY_CATALOG = "now_playing_catalog"
    private const val KEY_REFRESH = "now_playing_refresh"
    private const val KEY_DELETE = "now_playing_delete"
    private const val KEY_HISTORY = "now_playing_history"
    private const val METHOD_APPLY_ENABLED = "apply_enabled"
    private const val METHOD_CATALOG_STATE = "catalog_state"
    private const val METHOD_REFRESH = "refresh_catalog"
    private const val METHOD_DELETE = "delete_catalog"
    private const val KEY_SUCCESS = "success"
    private const val KEY_BUILD = "build"
    private const val KEY_SHARD_COUNT = "shard_count"
    private const val KEY_CATALOG_BYTES = "catalog_bytes"
    private val PROVIDER_URI = Uri.parse("content://com.benzeneos.nowplaying.settings")

    @JvmField val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.now_playing_settings)
  }
}
