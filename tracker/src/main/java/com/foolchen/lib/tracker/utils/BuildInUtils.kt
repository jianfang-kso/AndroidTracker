package com.foolchen.lib.tracker.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.foolchen.lib.tracker.BuildConfig
import com.foolchen.lib.tracker.data.MNC
import com.foolchen.lib.tracker.data.NetworkType


internal val buildInObject: HashMap<String, Any> = HashMap()
internal val buildInLib: HashMap<String, Any> = HashMap()
internal val buildInProperties: HashMap<String, Any> = HashMap()

internal var buildInUUID = ""

/**
 * 获取内置属性
 */
internal fun initBuildInProperties(context: Context) {

  buildInObject.put("\$time", System.currentTimeMillis())

  buildInLib.put("\$lib", "Android")
  buildInLib.put("\$lib_version", BuildConfig.VERSION_NAME)
  buildInLib.put("\$app_version", context.getVersionName())

  buildInProperties.put("\$lib", "Android")
  buildInProperties.put("\$lib_version", BuildConfig.VERSION_NAME)
  buildInProperties.put("\$app_version", context.getVersionName())
  buildInProperties.put("\$manufacturer", Build.BRAND)
  buildInProperties.put("\$model", Build.MODEL)
  buildInProperties.put("\$os", "Android")
  buildInProperties.put("\$os_version", Build.VERSION.RELEASE)
  buildInProperties.put("\$os_version", Build.VERSION.RELEASE)
  buildInProperties.put("\$screen_height", context.getScreenWidth())
  buildInProperties.put("\$screen_width", context.getScreenHeight())
  buildInProperties.put("\$wifi", context.isWiFi())
  buildInProperties.put("\$carrier", context.getMNC().desc())
  buildInProperties.put("\$network_type", context.getNetworkType().desc())
  buildInProperties.put("\$imeicode", context.getIMEI())
  buildInProperties.put("\$device_id", context.getAndroidId())

  buildInUUID = context.getUUID()
}

internal fun login(userId: String) {
  buildInProperties.put("\$distinct_id", userId)
}

internal fun logout(context: Context) {
  buildInProperties.put("\$distinct_id", context.getUUID())
}

/**
 * 获取当前app的版本名称
 */
private fun Context.getVersionName(): String {
  var versionName = ""
  try {
    val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS)
    versionName = packageInfo.versionName
  } catch (e: PackageManager.NameNotFoundException) {
  }
  return versionName
}


/**
 * 使用上下文对象获取当前手机屏幕宽度
 */
private fun Context.getScreenWidth(): Int {
  val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
  val display = wm.defaultDisplay
  val metrics = DisplayMetrics()
  display.getMetrics(metrics)
  return metrics.widthPixels
}

/**
 * 使用上下文对象获取当前手机屏幕高度
 */
private fun Context.getScreenHeight(): Int {
  val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
  val display = wm.defaultDisplay
  val metrics = DisplayMetrics()
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
    display.getRealMetrics(metrics)
  } else {
    display.getMetrics(metrics)
  }
  return metrics.heightPixels
}

@SuppressLint("MissingPermission")
/**
 * 判断当前网络状态是否为WiFi
 */
private fun Context.isWiFi(): Boolean {
  return if (ContextCompat.checkSelfPermission(this,
      Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
    val connectivityManager = this.getSystemService(
        Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    // 执行到此处时，已有权限，忽略该警告
    val activeNetInfo = connectivityManager.activeNetworkInfo
    activeNetInfo != null && activeNetInfo.type == ConnectivityManager.TYPE_WIFI
  } else {
    false
  }
}

private fun Context.getMNC(): MNC {
  if (ContextCompat.checkSelfPermission(this,
      Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
    val telManager = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    val operator = telManager.networkOperator
    //        int mcc = Integer.parseInt(operator.substring(0, 3));//移动国家代码（中国的为460）；
    try {//防止启动就崩溃，这里加上异常处理
      val mnc = Integer.parseInt(operator.substring(3))//，移动网络号码（中国移动为0,2，中国联通为1，中国电信为3）；
      when (mnc) {
        0 -> return MNC.CMCC
        1 -> return MNC.CUCC
        2 //因为移动网络编号46000下的IMSI已经用完，所以虚拟了一个46002编号，134/159号段使用了此编号 //中国移动
        -> return MNC.CMCC
        3 -> return MNC.CTCC
        11//在电信4g的情况下返回46011
        -> return MNC.CTCC
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

  }
  return MNC.OTHER
}

/**
 * 根据[Context]来获取网络类型
 *
 * 需要 android.permission.ACCESS_NETWORK_STATE 权限
 *
 * @return [NetType]
 */
private fun Context.getNetworkType(): NetworkType {
  if (ContextCompat.checkSelfPermission(this,
      Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
    if (isWiFi()) {
      return NetworkType.WIFI
    }

    if (!isNetworkAvailable()) {
      return NetworkType.NO_NET
    }

    //下面类型的判断不包含WIFI，如果是wifi类型会返回 UNKNOWN
    val telManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    val networkType = telManager.networkType
    return when (networkType) {
      TelephonyManager.NETWORK_TYPE_LTE  // 4G
        , TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_EHRPD -> NetworkType.G4
      TelephonyManager.NETWORK_TYPE_UMTS // 3G
        , TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_EVDO_B, 17//隐藏API
      -> NetworkType.G3
      TelephonyManager.NETWORK_TYPE_GPRS // 2G
        , TelephonyManager.NETWORK_TYPE_EDGE, 16 -> NetworkType.G2
      TelephonyManager.NETWORK_TYPE_UNKNOWN -> NetworkType.UNKNOWN
      else -> NetworkType.NO_DEAL
    }
  } else {
    return NetworkType.UNKNOWN
  }
}

@SuppressLint("MissingPermission")
/**
 * 当前是否连接网络
 *
 * 需要 android.permission.ACCESS_NETWORK_STATE 权限
 *
 * @return true联网，false没有联网
 */
private fun Context.isNetworkAvailable(): Boolean {
  if (ContextCompat.checkSelfPermission(this,
      Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
    val connectivityManager = this.getSystemService(
        Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    // 执行到此处时，已有权限，忽略该警告
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
  }
  // 默认网络可用
  return true
}

@SuppressLint("HardwareIds")
private fun Context.getAndroidId(): String {
  return android.provider.Settings.Secure.getString(contentResolver,
      android.provider.Settings.Secure.ANDROID_ID) ?: ""
}

private fun Context.getUUID(): String = (Build.BRAND + Build.MODEL).hashCode().toString() + getAndroidId()

@SuppressLint("MissingPermission")
/**
 * 获取设备Id(IMEI)
 *
 * @param context 上下文对象
 * @return 设备Id
 */
private fun Context.getIMEI(): String {
  var deviceId: String? = null
  if (ContextCompat.checkSelfPermission(this,
      Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
    // 如果有权限才获取设备id,防止崩溃
    val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    deviceId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      tm.imei
    } else {
      // 执行到此处时，已有权限，忽略该警告
      @Suppress("DEPRECATION")
      tm.deviceId
    }
  }
  return deviceId ?: ""
}