package expo.modules.zebrarfid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zebra.scannercontrol.DCSSDKDefs
import com.zebra.scannercontrol.DCSScannerInfo
import com.zebra.scannercontrol.SDKHandler
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoZebraRfidModule : Module() {
  // SDKHandler instance for managing Zebra scanner connections
  private var sdkHandler: SDKHandler? = null

  // List to store available scanners
  private val scannerInfoList = ArrayList<DCSScannerInfo>()

  // Set to track connected scanner IDs
  private val connectedScanners = mutableSetOf<Int>()

  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  override fun definition() = ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a
    // string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for
    // clarity.
    // The module will be accessible from `requireNativeModule('ExpoZebraRfid')` in JavaScript.
    Name("ExpoZebraRfid")

    // Initialize SDK when module is created
    OnCreate {
      try {
        appContext.reactContext?.let { context -> sdkHandler = SDKHandler(context) }
        // Optional: Set up any initial configurations here
      } catch (e: Exception) {
        // Handle initialization errors
        e.printStackTrace()
      }
    }

    // Clean up resources when module is destroyed
    OnDestroy {
      try {
        sdkHandler?.dcssdkClose()
        sdkHandler = null
      } catch (e: Exception) {
        // Handle cleanup errors
        e.printStackTrace()
      }
    }

    // Sets constant properties on the module. Can take a dictionary or a closure that returns a
    // dictionary.
    Constants("PI" to Math.PI)

    // Defines event names that the module can send to JavaScript.
    Events("onChange")

    // Defines a JavaScript synchronous function that runs the native code on the JavaScript thread.
    Function("hello") { "Hello world! 👋" }

    // Check if SDK is initialized
    Function("isSDKInitialized") { sdkHandler != null }

    // Get SDK version
    Function("getSDKVersion") { sdkHandler?.dcssdkGetVersion() ?: "SDK not initialized" }

    // Check if required permissions are granted
    Function("hasRequiredPermissions") { hasRequiredPermissions() }

    // Request required permissions
    AsyncFunction("requestPermissionsAsync") { promise: Promise -> requestPermissions(promise) }

    // Get available scanners
    AsyncFunction("getAvailableScannersAsync") { ->
      appContext.reactContext?.let { context -> getAvailableScanners(context) }
              ?: emptyList<Map<String, Any?>>()
    }

    // Connect to a specific scanner
    AsyncFunction("connectToScannerAsync") { scannerId: Int, promise: Promise ->
      connectToScanner(scannerId, promise)
    }

    // Disconnect from a specific scanner
    AsyncFunction("disconnectFromScannerAsync") { scannerId: Int, promise: Promise ->
      disconnectFromScanner(scannerId, promise)
    }

    // Check if connected to a specific scanner
    Function("isConnectedToScanner") { scannerId: Int -> isConnectedToScanner(scannerId) }

    // Get list of connected scanners
    Function("getConnectedScanners") { getConnectedScanners() }

    // Defines a JavaScript function that always returns a Promise and whose native code
    // is by default dispatched on the different thread than the JavaScript runtime runs on.
    AsyncFunction("setValueAsync") { value: String ->
      // Send an event to JavaScript.
      sendEvent("onChange", mapOf("value" to value))
    }
  }

  // Get available scanners method
  private fun getAvailableScanners(context: Context): List<Map<String, Any?>> {
    if (sdkHandler == null) {
      sdkHandler = SDKHandler(context)
    }

    sdkHandler?.let { handler ->
      handler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL)
      handler.dcssdkSetOperationalMode(DCSSDKDefs.DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC)

      // For now, comment out delegate setup until we resolve interface issues
      // handler.dcssdkSetDelegate(this)
      var notificationsMask = 0
      notificationsMask =
              notificationsMask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value
      notificationsMask =
              notificationsMask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value
      notificationsMask =
              notificationsMask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value
      notificationsMask =
              notificationsMask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value
      notificationsMask = notificationsMask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value

      // Subscribe to events set in notification mask
      handler.dcssdkSubsribeForEvents(notificationsMask)
      handler.dcssdkEnableAvailableScannersDetection(true)

      scannerInfoList.clear()
      handler.dcssdkGetAvailableScannersList(scannerInfoList)
    }

    // Convert scanner info list to JavaScript-friendly format
    return scannerInfoList.map { scannerInfo ->
      mapOf(
              "scannerId" to scannerInfo.scannerID,
              "scannerName" to scannerInfo.scannerName,
              "isActive" to scannerInfo.isActive,
              "isAvailable" to true, // Default to true, actual availability logic can be added
              "connectionType" to scannerInfo.connectionType.toString()
      )
    }
  }

  // Connect to a specific scanner
  private fun connectToScanner(scannerId: Int, promise: Promise) {
    sdkHandler?.let { handler ->
      val result = handler.dcssdkEstablishCommunicationSession(scannerId)
      if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS) {
        connectedScanners.add(scannerId)
        promise.resolve(true)
      } else {
        promise.resolve(false)
      }
    }
            ?: promise.resolve(false)
  }

  // Disconnect from a specific scanner
  private fun disconnectFromScanner(scannerId: Int, promise: Promise) {
    sdkHandler?.let { handler ->
      val result = handler.dcssdkTerminateCommunicationSession(scannerId)
      if (result == DCSSDKDefs.DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS) {
        connectedScanners.remove(scannerId)
        promise.resolve(true)
      } else {
        promise.resolve(false)
      }
    }
            ?: promise.resolve(false)
  }

  // Check if connected to a specific scanner
  private fun isConnectedToScanner(scannerId: Int): Boolean {
    return connectedScanners.contains(scannerId)
  }

  // Get list of connected scanners
  private fun getConnectedScanners(): List<Int> {
    return connectedScanners.toList()
  }

  // Check if required permissions are granted
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

  // Request required permissions
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
}
