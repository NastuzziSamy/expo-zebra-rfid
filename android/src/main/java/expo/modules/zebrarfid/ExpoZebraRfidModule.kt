package expo.modules.zebrarfid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zebra.scannercontrol.DCSSDKDefs
import com.zebra.scannercontrol.DCSScannerInfo
import com.zebra.scannercontrol.FirmwareUpdateEvent
import com.zebra.scannercontrol.IDcsSdkApiDelegate
import com.zebra.scannercontrol.SDKHandler
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoZebraRfidModule : Module(), IDcsSdkApiDelegate {
  private var sdkHandler: SDKHandler? = null
  private val scannerInfoList = ArrayList<DCSScannerInfo>()
  private val connectedScanners = mutableSetOf<Int>()

  override fun definition() = ModuleDefinition {
    Name("ExpoZebraRfid")

    OnCreate {
      try {
        appContext.reactContext?.let { context ->
          sdkHandler = SDKHandler(context)
          sdkHandler?.dcssdkSetDelegate(this@ExpoZebraRfidModule)
          println("✅ Zebra RFID SDK initialized successfully")
        }
      } catch (e: Exception) {
        println("❌ Failed to initialize Zebra RFID SDK: ${e.message}")
        e.printStackTrace()
      }
    }

    OnDestroy {
      try {
        sdkHandler?.dcssdkClose()
        sdkHandler = null
        println("🧹 Zebra RFID SDK cleaned up")
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    Events("onChange", "onRfidTagRead", "onScannerConnection", "onScannerDisconnection")

    Function("hello") { "Hello from Zebra RFID! 🦓📡" }
    Function("isSDKInitialized") { sdkHandler != null }
    Function("getSDKVersion") { sdkHandler?.dcssdkGetVersion() ?: "SDK not initialized" }

    Function("hasRequiredPermissions") { hasRequiredPermissions() }
    AsyncFunction("requestPermissionsAsync") { promise: Promise -> requestPermissions(promise) }

    AsyncFunction("getAvailableScannersAsync") { ->
      appContext.reactContext?.let { context -> getAvailableScanners(context) }
              ?: emptyList<Map<String, Any?>>()
    }

    AsyncFunction("connectToScannerAsync") { scannerId: Int, promise: Promise ->
      connectToScanner(scannerId, promise)
    }
    AsyncFunction("disconnectFromScannerAsync") { scannerId: Int, promise: Promise ->
      disconnectFromScanner(scannerId, promise)
    }
    Function("isConnectedToScanner") { scannerId: Int -> isConnectedToScanner(scannerId) }
    Function("getConnectedScanners") { getConnectedScanners() }

    AsyncFunction("startRfidInventory") { scannerId: Int, promise: Promise ->
      startRfidInventory(scannerId, promise)
    }

    AsyncFunction("stopRfidInventory") { scannerId: Int, promise: Promise ->
      stopRfidInventory(scannerId, promise)
    }
  }

  // IDcsSdkApiDelegate implementation - all methods are required
  override fun dcssdkEventScannerAppeared(dcsScannerInfo: DCSScannerInfo?) {
    dcsScannerInfo?.let { info ->
      println("🔍 Scanner appeared: ${info.scannerName} (ID: ${info.scannerID})")
      sendEvent(
              "onScannerConnection",
              mapOf(
                      "scannerId" to info.scannerID,
                      "scannerName" to info.scannerName,
                      "type" to "appeared"
              )
      )
    }
  }

  override fun dcssdkEventScannerDisappeared(scannerId: Int) {
    println("🔍 Scanner disappeared: ID $scannerId")
    connectedScanners.remove(scannerId)
    sendEvent("onScannerDisconnection", mapOf("scannerId" to scannerId, "type" to "disappeared"))
  }

  override fun dcssdkEventAuxScannerAppeared(
          dcsScannerInfo1: DCSScannerInfo,
          dcsScannerInfo2: DCSScannerInfo
  ) {
    dcsScannerInfo1.let { info ->
      println("🔍 Auxiliary scanner appeared: ${info.scannerName} (ID: ${info.scannerID})")
      sendEvent(
              "onScannerConnection",
              mapOf(
                      "scannerId" to info.scannerID,
                      "scannerName" to info.scannerName,
                      "type" to "aux-appeared"
              )
      )
    }

    dcsScannerInfo2.let { info ->
      println("🔍 Auxiliary scanner appeared: ${info.scannerName} (ID: ${info.scannerID})")
      sendEvent(
              "onScannerConnection",
              mapOf(
                      "scannerId" to info.scannerID,
                      "scannerName" to info.scannerName,
                      "type" to "aux-appeared"
              )
      )
    }
  }

  override fun dcssdkEventCommunicationSessionEstablished(dcsScannerInfo: DCSScannerInfo?) {
    dcsScannerInfo?.let { info ->
      println("🔗 Communication session established: ${info.scannerName}")
      connectedScanners.add(info.scannerID)
      sendEvent(
              "onScannerConnection",
              mapOf(
                      "scannerId" to info.scannerID,
                      "scannerName" to info.scannerName,
                      "type" to "connected"
              )
      )
    }
  }

  override fun dcssdkEventCommunicationSessionTerminated(scannerId: Int) {
    println("🔗 Communication session terminated: ID $scannerId")
    connectedScanners.remove(scannerId)
    sendEvent("onScannerDisconnection", mapOf("scannerId" to scannerId, "type" to "disconnected"))
  }

  override fun dcssdkEventBarcode(barcodeData: ByteArray?, barcodeType: Int, fromScannerID: Int) {
    // Not used - RFID only
  }

  override fun dcssdkEventImage(imageData: ByteArray?, fromScannerID: Int) {
    // Not used
  }

  override fun dcssdkEventVideo(videoFrame: ByteArray?, fromScannerID: Int) {
    // Not used
  }

  override fun dcssdkEventBinaryData(binaryData: ByteArray?, fromScannerID: Int) {
    binaryData?.let { data ->
      println("📡 RFID binary data received from scanner $fromScannerID: ${data.size} bytes")

      try {
        val rfidDataString = String(data, Charsets.UTF_8)
        println("📡 RFID tag data: $rfidDataString")

        sendEvent(
                "onRfidTagRead",
                mapOf(
                        "scannerId" to fromScannerID,
                        "tagData" to rfidDataString,
                        "timestamp" to System.currentTimeMillis()
                )
        )
      } catch (e: Exception) {
        println("⚠️ Error parsing RFID data as UTF-8: ${e.message}")

        // Fallback: send as hex string
        val hexString = data.joinToString("") { "%02x".format(it) }
        sendEvent(
                "onRfidTagRead",
                mapOf(
                        "scannerId" to fromScannerID,
                        "tagData" to hexString,
                        "timestamp" to System.currentTimeMillis(),
                        "format" to "hex"
                )
        )
      }
    }
  }

  override fun dcssdkEventFirmwareUpdate(firmwareUpdateEvent: FirmwareUpdateEvent?) {
    // Not used for basic RFID operations
  }

  // Private implementation methods
  private fun getAvailableScanners(context: Context): List<Map<String, Any?>> {
    if (sdkHandler == null) {
      sdkHandler = SDKHandler(context)
      sdkHandler?.dcssdkSetDelegate(this)
    }

    sdkHandler?.let { handler ->
      // Set operational modes
      handler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL)
      handler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC)

      // Set up notification mask for RFID events
      var notificationsMask = 0
      notificationsMask =
              notificationsMask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value
      notificationsMask =
              notificationsMask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value
      notificationsMask =
              notificationsMask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value
      notificationsMask =
              notificationsMask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value
      notificationsMask =
              notificationsMask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BINARY_DATA.value

      handler.dcssdkSubsribeForEvents(notificationsMask)
      handler.dcssdkEnableAvailableScannersDetection(true)

      scannerInfoList.clear()
      handler.dcssdkGetAvailableScannersList(scannerInfoList)

      println("🔍 Found ${scannerInfoList.size} available scanners")
    }

    return scannerInfoList.map { scannerInfo ->
      mapOf(
              "scannerId" to scannerInfo.scannerID,
              "scannerName" to scannerInfo.scannerName,
              "isActive" to scannerInfo.isActive,
              "isAvailable" to true,
              "connectionType" to scannerInfo.connectionType.toString()
      )
    }
  }

  private fun connectToScanner(scannerId: Int, promise: Promise) {
    sdkHandler?.let { handler ->
      println("🔗 Attempting to connect to scanner $scannerId")
      val result = handler.dcssdkEstablishCommunicationSession(scannerId)
      val success = result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS

      if (success) {
        connectedScanners.add(scannerId)
        println("✅ Connected to scanner $scannerId")
      } else {
        println("❌ Failed to connect to scanner $scannerId")
      }

      promise.resolve(success)
    }
            ?: promise.resolve(false)
  }

  private fun disconnectFromScanner(scannerId: Int, promise: Promise) {
    sdkHandler?.let { handler ->
      val result = handler.dcssdkTerminateCommunicationSession(scannerId)
      if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS) {
        connectedScanners.remove(scannerId)
        promise.resolve(true)
      } else {
        println("❌ Failed to disconnect from scanner $scannerId")
        promise.resolve(false)
      }
    }
            ?: promise.resolve(false)
  }

  private fun isConnectedToScanner(scannerId: Int): Boolean {
    return connectedScanners.contains(scannerId)
  }

  private fun getConnectedScanners(): List<Int> {
    return connectedScanners.toList()
  }

  private fun hasRequiredPermissions(): Boolean {
    val context = appContext.reactContext ?: return false
    val permissions =
            mutableListOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            )

    // Add Android 12+ permissions
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.addAll(
              listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
      )
    }

    return permissions.all { permission ->
      ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
  }

  private fun requestPermissions(promise: Promise) {
    val context =
            appContext.reactContext
                    ?: run {
                      promise.resolve(false)
                      return
                    }
    val permissions =
            mutableListOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            )

    // Add Android 12+ permissions
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.addAll(
              listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
      )
    }

    val permissionsToRequest =
            permissions.filter { permission ->
              ContextCompat.checkSelfPermission(context, permission) !=
                      PackageManager.PERMISSION_GRANTED
            }

    if (permissionsToRequest.isEmpty()) {
      promise.resolve(true)
      return
    }

    // Try to request permissions using current activity
    try {
      val currentActivity = appContext.currentActivity
      if (currentActivity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // Request permissions with a request code
        ActivityCompat.requestPermissions(
                currentActivity,
                permissionsToRequest.toTypedArray(),
                1001 // Request code
        )

        // For now, we return false since we can't easily listen for the result
        // The user will need to manually grant permissions in settings
        promise.resolve(false)
      } else {
        // Cannot request permissions automatically
        promise.resolve(false)
      }
    } catch (e: Exception) {
      // Fallback - cannot request permissions
      promise.resolve(false)
    }
  }

  private fun startRfidInventory(scannerId: Int, promise: Promise) {
    sdkHandler?.let { handler ->
      if (!connectedScanners.contains(scannerId)) {
        promise.resolve(false)
        return
      }

      println("🏷️ Starting RFID inventory for scanner $scannerId")

      // Set scanner to RFID mode and start inventory
      val setRfidModeXML =
              "<inArgs><scannerID>$scannerId</scannerID><cmdArgs><arg-xml><attrib_list>6000</attrib_list></arg-xml></cmdArgs></inArgs>"
      val rfidModeResult =
              handler.dcssdkExecuteCommandOpCodeInXMLForScanner(
                      DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_SET_ACTION,
                      setRfidModeXML,
                      null,
                      scannerId
              )

      if (rfidModeResult == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS) {
        val startInventoryXML =
                "<inArgs><scannerID>$scannerId</scannerID><cmdArgs><arg-int>1</arg-int></cmdArgs></inArgs>"
        val result =
                handler.dcssdkExecuteCommandOpCodeInXMLForScanner(
                        DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_SCAN_ENABLE,
                        startInventoryXML,
                        null,
                        scannerId
                )

        val success = result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS
        println(if (success) "✅ RFID inventory started" else "❌ Failed to start RFID inventory")
        promise.resolve(success)
      } else {
        println("❌ Failed to set RFID mode")
        promise.resolve(false)
      }
    }
            ?: promise.resolve(false)
  }

  private fun stopRfidInventory(scannerId: Int, promise: Promise) {
    sdkHandler?.let { handler ->
      if (!connectedScanners.contains(scannerId)) {
        promise.resolve(false)
        return
      }

      println("🏷️ Stopping RFID inventory for scanner $scannerId")

      val stopInventoryXML =
              "<inArgs><scannerID>$scannerId</scannerID><cmdArgs><arg-int>0</arg-int></cmdArgs></inArgs>"
      val result =
              handler.dcssdkExecuteCommandOpCodeInXMLForScanner(
                      DCSSDKDefs.DCSSDK_COMMAND_OPCODE.DCSSDK_DEVICE_SCAN_DISABLE,
                      stopInventoryXML,
                      null,
                      scannerId
              )

      val success = result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS
      println(if (success) "✅ RFID inventory stopped" else "❌ Failed to stop RFID inventory")
      promise.resolve(success)
    }
            ?: promise.resolve(false)
  }
}
