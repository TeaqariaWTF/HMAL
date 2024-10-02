package icu.nullptr.hidemyapplist.xposed.hook

import android.annotation.TargetApi
import android.content.pm.ApplicationInfo
import android.os.Build
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.common.CommonUtils
import icu.nullptr.hidemyapplist.xposed.HMAService

@TargetApi(Build.VERSION_CODES.S)
class PlatformCompatHook(private val service: HMAService) : IFrameworkHook {

    companion object {
        private const val TAG = "HMAL-ZAH"
        private val sAppDataIsolationEnabled = CommonUtils.isAppDataIsolationEnabled
    }

    private var hook: XC_MethodHook.Unhook? = null

    override fun load() {
        if (!service.config.forceMountData) return
        hook = findMethod("com.android.server.compat.PlatformCompat") {
            name == "isChangeEnabled"
        }.hookBefore { param ->
            runCatching {
                val changeId = param.args[0] as Long
                val appInfo = param.args[1] as ApplicationInfo
                if (changeId.toInt() != 143937733) return@hookBefore
                val apps = service.pms.getPackagesForUid(appInfo.uid) ?: return@hookBefore
                for (app in apps) {
                    if (service.isHookEnabled(app)) {
                        if (sAppDataIsolationEnabled) param.result = true
                        return@hookBefore
                    }
                }
            }.onFailure {
                unload()
            }
        }
    }

    override fun unload() {
        hook?.unhook()
        hook = null
    }

    override fun onConfigChanged() {
        if (service.config.forceMountData) {
            if (hook == null) load()
        } else {
            if (hook != null) unload()
        }
    }
}