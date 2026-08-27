package com.android.settings.sound

import android.content.Context
import android.content.pm.PackageManager
import android.os.UserHandle
import android.provider.Settings
import com.android.settings.R
import com.android.settings.core.BasePreferenceController

class NowPlayingEntryPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key) {

  override fun getAvailabilityStatus(): Int {
    if (UserHandle.myUserId() != UserHandle.USER_SYSTEM) {
      return CONDITIONALLY_UNAVAILABLE
    }
    val flags = PackageManager.MATCH_DIRECT_BOOT_AWARE or PackageManager.MATCH_DIRECT_BOOT_UNAWARE
    return if (mContext.packageManager.resolveContentProvider(AUTHORITY, flags) != null) {
      AVAILABLE
    } else {
      UNSUPPORTED_ON_DEVICE
    }
  }

  override fun getSummary(): CharSequence =
      mContext.getString(
          if (Settings.Global.getInt(mContext.contentResolver, ENABLED, 0) != 0) {
            R.string.switch_on_text
          } else {
            R.string.switch_off_text
          },
      )

  private companion object {
    const val AUTHORITY = "com.benzeneos.nowplaying.settings"
    const val ENABLED = "now_playing_enabled"
  }
}
