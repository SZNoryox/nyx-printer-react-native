package com.nyxprinterreactnative

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.nyxprinterreactnative.PrinterResult.SDK_FEATURE_NOT_SUPPORT
import com.nyxprinterreactnative.PrinterResult.SDK_SERVICE_NOT_BIND
import com.nyxprinterreactnative.PrinterResult.msg
import net.nyx.printerservice.print.IPrinterService
import net.nyx.printerservice.print.PrintTextFormat


class NyxPrinterModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  companion object {
    private const val RC_SCAN: Int = 0x1000
  }

  private var printerService: IPrinterService? = null
  private val connService: ServiceConnection = object : ServiceConnection {
    override fun onServiceDisconnected(name: ComponentName) {
      Log.d("PrinterPlugin", "Printer service disconnected")
      printerService = null
      createAsyncHandler(Looper.myLooper())?.postDelayed({ bindService() }, 2000)
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      Log.d("PrinterPlugin", "Printer service connected")
      printerService = IPrinterService.Stub.asInterface(service)
    }
  }

  private fun bindService() {
    var prefix = "net.nyx"
    if (Build.VERSION.SDK_INT == 33 && "SC9863A" == getSystemProperty("ro.soc.model")) {
      prefix = "com.incar"
    }
    val intent = Intent()
    intent.setPackage("$prefix.printerservice")
    intent.setAction("$prefix.printerservice.IPrinterService")
    val bind = reactContext.bindService(intent, connService, Context.BIND_AUTO_CREATE)
    if (bind.not()) {
      Log.e("PrinterPlugin", "Bind printer service failed, please check the device")
    }
  }

  private fun unbindService(context: Context) {
    context.unbindService(connService)
  }

  private val qscReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if ("com.android.NYX_QSC_DATA" == intent.action) {
        val qsc = intent.getStringExtra("qsc")
        qsc?.let { sendScanResult("QSC_RESULT", qsc) }
      }
    }
  }

  private fun registerQscScanReceiver() {
    val filter = IntentFilter()
    filter.addAction("com.android.NYX_QSC_DATA")

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          reactContext.registerReceiver(
              qscReceiver,
              filter,
              Context.RECEIVER_NOT_EXPORTED
          )
      } else {
          reactContext.registerReceiver(qscReceiver, filter)
      }
  }

  private fun unregisterQscReceiver() {
    reactContext.unregisterReceiver(qscReceiver)
  }

  override fun initialize() {
    bindService()
    reactContext.addActivityEventListener(activityEventListener)
    registerQscScanReceiver()
  }

  override fun invalidate() {
    unbindService(reactContext)
    unregisterQscReceiver()
  }

  override fun getName(): String {
    return "NyxPrinter"
  }

  private val activityEventListener = object : BaseActivityEventListener() {
    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, intent: Intent?) {
      if (requestCode == RC_SCAN && resultCode == RESULT_OK && intent != null) {
        val result: String? = intent.getStringExtra("SCAN_RESULT")
        result?.let { sendScanResult("SCAN_RESULT", result) }
      }
    }
  }

  private fun sendScanResult(key: String, result: String) {
    val map = Arguments.createMap()
    map.putString(key, result)
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onScanResult", map)
  }

  @ReactMethod
  fun scan(map: ReadableMap = Arguments.createMap(), promise: Promise) {
    try {
      val intent = Intent()
      intent.setComponent(ComponentName("net.nyx.scanner", "net.nyx.scanner.ScannerActivity"))
      if (map.hasKey("title")) {
        // set the capture activity actionbar title
        intent.putExtra("TITLE", map.getString("title"));
      }
      if (map.hasKey("showAlbum")) {
        // show album icon, default true
        intent.putExtra("SHOW_ALBUM", map.getBoolean("showAlbum"));
      }
      if (map.hasKey("playSound")) {
        // play beep sound when get the scan result, default true
        intent.putExtra("PLAY_SOUND", map.getBoolean("playSound"));
      }
      if (map.hasKey("playVibrate")) {
        // play vibrate when get the scan result, default true
        intent.putExtra("PLAY_VIBRATE", map.getBoolean("playVibrate"));
      }
      currentActivity?.startActivityForResult(intent, RC_SCAN)
    } catch (e: Exception) {
      promise.reject(SDK_FEATURE_NOT_SUPPORT.toString(), msg(SDK_FEATURE_NOT_SUPPORT))
    }
  }

  @ReactMethod
  fun getServiceVersion(promise: Promise) {
    if (checkPrinterService(promise)) return
    promise.resolve(printerService?.serviceVersion)
  }

  @ReactMethod
  fun getPrinterVersion(promise: Promise) {
    if (checkPrinterService(promise)) return
    val res = arrayOfNulls<String>(1)
    val ret = printerService?.getPrinterVersion(res)
    if (ret == 0) {
      promise.resolve(res[0])
    } else {
      promise.reject(ret.toString(), msg(ret))
    }
  }

  @ReactMethod
  fun getPrinterStatus(promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.printerStatus
    if (ret != null && ret < -1200 && ret > -1300 || ret == 0) {
      promise.resolve(ret)
    } else {
      promise.reject(ret.toString(), msg(ret))
    }
  }

  @ReactMethod
  fun paperOut(px: Int, promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.paperOut(px)
    handleResult(ret, promise)
  }

  @ReactMethod
  fun paperBack(px: Int, promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.paperBack(px)
    handleResult(ret, promise)
  }

  @ReactMethod
  fun printText(text: String, style: ReadableMap?, promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.printText(text, convertTextStyle(style))
    handleResult(ret, promise)
  }

  @ReactMethod
  fun printText2(text: String, style: ReadableMap?, textWidth: Int, align: Int, promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.printText2(text, convertTextStyle(style), textWidth, align)
    handleResult(ret, promise)
  }

  @ReactMethod
  fun printBarcode(data: String, width: Int, height: Int, textPosition: Int, align: Int, promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.printBarcode(data, width, height, textPosition, align)
    handleResult(ret, promise)
  }

  @ReactMethod
  fun printQrCode(data: String, width: Int, height: Int, align: Int, promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.printQrCode(data, width, height, align)
    handleResult(ret, promise)
  }

  @ReactMethod
  fun printBitmap(data: String, type: Int, align: Int, promise: Promise) {
    if (checkPrinterService(promise)) return
    val bitmap = convertBase64Bitmap(data)
    val ret = printerService?.printBitmap(bitmap, type, align)
    handleResult(ret, promise)
  }

  @ReactMethod
  fun printRasterData(data: String, promise: Promise) {
    if (checkPrinterService(promise)) return
    val bytes: ByteArray = Base64.decode(data, Base64.DEFAULT)
    val ret = printerService?.printRasterData(bytes)
    handleResult(ret, promise)
  }

  @ReactMethod
  fun printEscposData(data: String, promise: Promise) {
    if (checkPrinterService(promise)) return
    val bytes: ByteArray = Base64.decode(data, Base64.DEFAULT)
    val ret = printerService?.printEscposData(bytes)
    handleResult(ret, promise)
  }

  @ReactMethod
  fun printTableText(texts: ReadableArray, weights: ReadableArray, styles: ReadableArray, promise: Promise) {
    if (checkPrinterService(promise)) return
    val textArr = arrayOfNulls<String>(texts.size())
    val weightArr = IntArray(weights.size())
    val styleArr = arrayOfNulls<PrintTextFormat>(styles.size())
    for (i in 0 until texts.size()) {
      textArr[i] = texts.getString(i)
    }
    for (i in 0 until weights.size()) {
      weightArr[i] = weights.getInt(i)
    }
    for (i in 0 until styles.size()) {
      styleArr[i] = convertTextStyle(styles.getMap(i))
    }
    val ret = printerService?.printTableText(textArr, weightArr, styleArr)
    handleResult(ret, promise)
  }

  @ReactMethod
  fun printEndAutoOut(promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.printEndAutoOut()
    handleResult(ret, promise)
  }

  @ReactMethod
  fun labelLocate(labelHeight: Int, labelGap: Int, promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.labelLocate(labelHeight, labelGap)
    handleResult(ret, promise)
  }

  @ReactMethod
  fun labelPrintEnd(promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.labelPrintEnd()
    handleResult(ret, promise)
  }

  @ReactMethod
  fun labelLocateAuto(promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.labelLocateAuto()
    handleResult(ret, promise)
  }

  @ReactMethod
  fun labelDetectAuto(promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.labelDetectAuto()
    handleResult(ret, promise)
  }

  @ReactMethod
  fun hasLabelLearning(promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.hasLabelLearning()
    promise.resolve(ret)
  }

  @ReactMethod
  fun clearLabelLearning(promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.clearLabelLearning()
    handleResult(ret, promise)
  }

  @ReactMethod
  fun configLcd(opt: Int, promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.configLcd(opt)
    handleResult(ret, promise)
  }

  @ReactMethod
  fun showLcdBitmap(data: String, promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.showLcdBitmap(convertBase64Bitmap(data))
    handleResult(ret, promise)
  }

  @ReactMethod
  fun openCashBox(promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.openCashBox()
    handleResult(ret, promise)
  }

  @ReactMethod
  fun qscScan(promise: Promise) {
    if (checkPrinterService(promise)) return
    val ret = printerService?.triggerQscScan()
    handleResult(ret, promise)
  }

  private fun checkPrinterService(promise: Promise): Boolean {
    if (printerService == null) {
      promise.reject(SDK_SERVICE_NOT_BIND.toString(), msg(SDK_SERVICE_NOT_BIND))
      return true
    }
    return false
  }

  private fun handleResult(ret: Int?, promise: Promise) {
    if (ret == 0) {
      promise.resolve(null)
    } else {
      promise.reject(ret.toString(), msg(ret))
    }
  }

  private fun convertTextStyle(map: ReadableMap?): PrintTextFormat {
    val format = PrintTextFormat()
    if (map == null) return format
    if (map.hasKey("textSize")) {
      format.textSize = map.getInt("textSize")
    }
    if (map.hasKey("underline")) {
      format.isUnderline = map.getBoolean("underline")
    }
    if (map.hasKey("textScaleX")) {
      format.textScaleX = map.getDouble("textScaleX").toFloat()
    }
    if (map.hasKey("textScaleY")) {
      format.textScaleY = map.getDouble("textScaleY").toFloat()
    }
    if (map.hasKey("letterSpacing")) {
      format.letterSpacing = map.getDouble("letterSpacing").toFloat()
    }
    if (map.hasKey("lineSpacing")) {
      format.lineSpacing = map.getDouble("lineSpacing").toFloat()
    }
    if (map.hasKey("topPadding")) {
      format.topPadding = map.getInt("topPadding")
    }
    if (map.hasKey("leftPadding")) {
      format.leftPadding = map.getInt("leftPadding")
    }
    if (map.hasKey("align")) {
      format.ali = map.getInt("align")
    }
    if (map.hasKey("font")) {
      val font = map.getString("font")
      format.font = 5
      format.path = font
    }
    return format
  }

  private fun convertBase64Bitmap(data: String): Bitmap? {
    var bitmap: Bitmap? = null
    try {
      var base64Str = data
      if (data.contains("base64")) {
        base64Str = data.substring(data.indexOf(",") + 1)
      }
      val base64: ByteArray = Base64.decode(base64Str, Base64.DEFAULT)
      bitmap = BitmapFactory.decodeByteArray(base64, 0, base64.size)
    } catch (_: Exception) {
    }
    return bitmap
  }

  private fun getSystemProperty(key: String?): String? {
    var result: String? = null
    try {
      val c = Class.forName("android.os.SystemProperties")
      val get = c.getMethod("get", String::class.java)
      result = get.invoke(c, key) as String
    } catch (e: java.lang.Exception) {
      e.printStackTrace()
    }
    return result
  }

  fun createAsyncHandler(looper: Looper?): Handler? {
    return looper?.let {
      if (Build.VERSION.SDK_INT >= 28) {
        Handler.createAsync(it)
      } else Handler(it)
    }
  }
}
