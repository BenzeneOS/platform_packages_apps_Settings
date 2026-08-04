package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.GosPackageState
import android.ext.settings.app.AppSwitch
import android.ext.settings.app.AswBenzenedRoot
import android.ext.settings.app.AswBenzenedRootUnrestricted
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.android.settings.R
import com.android.settings.spa.app.appinfo.AswPreference
import com.android.settingslib.widget.FooterPreference

object AswAdapterBenzenedRoot : AswAdapter<AswBenzenedRoot>() {

    override fun getAppSwitch() = AswBenzenedRoot.I

    override fun getCategory() = Category.PrivilegedAccess

    override fun getAswTitle(ctx: Context) = ctx.getText(R.string.benzened_root)

    override fun getDetailFragmentClass() = AppBenzenedRootFragment::class
}

@Composable
fun AppBenzenedRootPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    AswPreference(context, app, AswAdapterBenzenedRoot)
}

class AppBenzenedRootFragment : AswAppInfoFragment<AswBenzenedRoot>() {

    override fun getAswAdapter() = AswAdapterBenzenedRoot

    override fun getEntries(): Array<Entry> {
        val ctx: Context = requireContext()
        val info: ApplicationInfo = getAppInfo()
        val ps: GosPackageState? = GosPackageState.get(mPackageName, mUserId)

        val granted: Boolean =
            AswBenzenedRoot.I.get(ctx, mUserId, info, ps, AppSwitch.StateInfo())
        val unrestricted: Boolean = granted &&
            AswBenzenedRootUnrestricted.I.get(ctx, mUserId, info, ps, AppSwitch.StateInfo())

        val off: Entry = createEntry(ID_ROOT_OFF, ctx.getText(R.string.benzened_root_off))
        off.isChecked = !granted
        off.isEnabled = true

        val standard: Entry =
            createEntry(ID_ROOT_STANDARD, ctx.getText(R.string.benzened_root_standard))
        standard.isChecked = granted && !unrestricted
        standard.isEnabled = true

        val advanced: Entry =
            createEntry(ID_ROOT_UNRESTRICTED, ctx.getText(R.string.benzened_root_unrestricted))
        advanced.isChecked = unrestricted
        advanced.isEnabled = true

        return arrayOf(off, standard, advanced)
    }

    override fun onExtraEntrySelected(ed: GosPackageState.Editor, id: Int) {
        when (id) {
            ID_ROOT_OFF -> {
                AswBenzenedRoot.I.set(ed, false)
                AswBenzenedRootUnrestricted.I.set(ed, false)
            }
            ID_ROOT_STANDARD -> {
                AswBenzenedRoot.I.set(ed, true)
                AswBenzenedRootUnrestricted.I.set(ed, false)
            }
            ID_ROOT_UNRESTRICTED -> {
                AswBenzenedRoot.I.set(ed, true)
                AswBenzenedRootUnrestricted.I.set(ed, true)
            }
            else -> throw IllegalStateException()
        }
    }

    override fun updateFooter(fp: FooterPreference) {
        fp.setTitle(R.string.benzened_root_footer)
    }

    companion object {
        private const val ID_ROOT_OFF = 10
        private const val ID_ROOT_STANDARD = 11
        private const val ID_ROOT_UNRESTRICTED = 12
    }
}

class BenzenedRootAppListPrefController(context: Context, preferenceKey: String) :
    AswAppListPrefController(context, preferenceKey, AswAdapterBenzenedRoot) {

    override fun getAvailabilityStatus() = AVAILABLE
}
