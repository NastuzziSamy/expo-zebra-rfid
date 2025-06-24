package expo.modules.zebrarfid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zebra.rfid.api3.ENUM_TRANSPORT
import com.zebra.rfid.api3.Readers
import com.zebra.rfid.api3.ReaderDevice
import com.zebra.rfid.api3.RFIDReader
import com.zebra.rfid.api3.TagData
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoZebraRfidModule: Module() {
  private var currentContext: Context? = null
  private var sdkHandler: ZebraSdkHandler? = null

  companion object {
    const val ON_DEVICE_CONNECTED = "onDeviceConnected"
    const val ON_DEVICE_DISCONNECTED = "onDeviceDisconnected"
    const val ON_DEVICE_ERRORED = "onDeviceErrored"
    const val ON_DEVICE_TRIGGERED = "onDeviceTriggered"
    const val ON_RFID_READ = "onRfidRead"

    const val DEVICE_CONNECTED = "CONNECTED"
    const val DEVICE_DISCONNECTED = "DISCONNECTED"

    const val MODULE_NAME = "ExpoZebraRfid"

    const val MODULE_SDK_VERSION = "2.0.4.192"

    val MODULE_EVENTS = listOf<String>(
      ON_DEVICE_CONNECTED,
      ON_DEVICE_DISCONNECTED,
      ON_DEVICE_ERRORED,
      ON_DEVICE_TRIGGERED,
      ON_RFID_READ,
    )

    val MODULE_BASE_PERMISSIONS =
      listOf<String>(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
      )
    val MODULE_ANDROID_12_PERMISSIONS =
      listOf<String>(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
      )

    val MODULE_PERMISSIONS =
      mutableListOf<String>().apply {
        addAll(MODULE_BASE_PERMISSIONS)

        // Add Android 12+ permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          addAll(MODULE_ANDROID_12_PERMISSIONS)
        }
      }.toList()
  }

  public fun getContext(): Context? = currentContext

  override fun definition() = ModuleDefinition {
    Name(MODULE_NAME)

    Events(MODULE_EVENTS.toTypedArray())

    OnCreate {
      currentContext = appContext.reactContext

      sdkHandler = ZebraSdkHandler(this@ExpoZebraRfidModule)
    }

    OnDestroy {
      sdkHandler?.forEachConnectedDevice { device ->
        try {
          device.disconnect()
        } catch (e: Exception) {
          println("Zebra: Error disconnecting reader: ${e.message}")
        }
      }
      
      sdkHandler = null
      currentContext = null
    }

    Function("isSDKInitialized") { sdkHandler?.isReady() ?: false }

    Function("getSDKVersion") { sdkHandler?.getSdkVersion() }

    Function("hasRequiredPermissions") { checkHasPermissions() }

    // Request required permissions
    AsyncFunction("requestPermissionsAsync") { promise: Promise ->
      try {
        if (requestPermissions()) {
          if (checkHasPermissions()) {
            promise.resolve(true)
          } else {
            promise.reject("PERMISSIONS_ERROR", "Permissions were not granted", null)
          }
        } else {
          promise.reject("PERMISSIONS_ERROR", "Failed to request permissions", null)
        } 
      } catch (e: Exception) {
        promise.reject("PERMISSIONS_ERROR", "Failed to request permissions: ${e.message}", e)
      }
    }

    // Reader management functions using direct RFIDReader instantiation
    AsyncFunction("getAvailableDevicesAsync") { promise: Promise ->
      try {
        promise.resolve(getAvailableDevices())
      } catch (e: Exception) {
        promise.reject("GET_READERS_ERROR", "Failed to get available readers: ${e.message}", e)
      }
    }

    Function("getConnectedDevices") { getConnectedDevices() }

    AsyncFunction("connectToDeviceAsync") { deviceId: String, promise: Promise ->
      try {
        val result = connectToDevice(deviceId)
        promise.resolve(result)
      } catch (e: Exception) {
        promise.reject("CONNECT_SCANNER_ERROR", "Failed to connect to scanner: ${e.message}", e)
      }
    }

    AsyncFunction("disconnectFromDeviceAsync") { deviceId: String, promise: Promise ->
      try {
        val sdkHandler = sdkHandler ?: return@AsyncFunction promise.reject("SDK_NOT_INITIALIZED", "SDK is not initialized", null)

        sdkHandler.disconnectFromDevice(deviceId).also { isDisconnected ->
          if (isDisconnected) {
            promise.resolve(true)
          } else {
            promise.reject("DISCONNECT_SCANNER_ERROR", "Failed to disconnect from scanner with address $deviceId", null)
          }
        }
      } catch (e: Exception) {
        promise.reject("DISCONNECT_SCANNER_ERROR", "Failed to disconnect from scanner: ${e.message}", e)
      }
    }
  }

  private fun getMissingPermissions(): List<String> {
    val context = currentContext ?: return MODULE_PERMISSIONS

    return MODULE_PERMISSIONS.filter { permission ->
      ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }
  }

  private fun checkHasPermissions(): Boolean = getMissingPermissions().isEmpty()

  private fun requestPermissions(): Boolean {
    val permissionsToRequest = getMissingPermissions()
    if (permissionsToRequest.isEmpty()) {
      return true
    }

    val currentActivity = appContext.currentActivity

    if (currentActivity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ActivityCompat.requestPermissions(
        currentActivity,
        permissionsToRequest.toTypedArray(),
        1001 // Request code
      )

      return true
    } else {
      return false
    }
  }

  fun getAvailableDevices(): List<Map<String, String?>> =
    sdkHandler?.getAvailableDevices()?.map { 
      it.toReactObject() 
    } ?: emptyList()

  fun getConnectedDevices(): List<Map<String, String?>> =
    sdkHandler?.getConnectedDevices()?.map {
      it.toReactObject() 
    } ?: emptyList()

  fun connectToDevice(deviceAddress: String): Boolean {
    val sdkHandler = sdkHandler ?: return false

    return sdkHandler.connectToDevice(deviceAddress).also { isConnected ->
      if (isConnected) {
        val device = sdkHandler.getAvailableDevices().find { it.getAddress() == deviceAddress }
        if (device != null) {
          // appContext.emit(ON_DEVICE_CONNECTED, device.toReactObject())
        }
      } else {
        // appContext.emit(ON_DEVICE_ERRORED, "Failed to connect to device with address $deviceAddress")
      }
    }
  }

  public fun onDeviceTriggered(deviceHandler: DeviceHandler) {
    sendEvent(ON_DEVICE_TRIGGERED, mapOf(
      "deviceId" to deviceHandler.getId(),
      "trigger" to "pressed",
    ))

    startAction(deviceHandler);
  }

  public fun onDeviceReleased(deviceHandler: DeviceHandler) {
    sendEvent(ON_DEVICE_TRIGGERED, mapOf(
      "deviceId" to deviceHandler.getId(),
      "trigger" to "released",
    ))

    stopAction(deviceHandler);
  }

  public fun onRfidRead(deviceHandler: DeviceHandler, tagData: TagData) {
    sendEvent(ON_RFID_READ, mapOf(
      "tagId" to tagData.getTagID(),
      "deviceId" to deviceHandler.getDevice().getAddress(),
      "rssi" to tagData.getPeakRSSI(),
      "crc" to tagData.getCRC(),
      "antennaId" to tagData.getAntennaID(),
      "count" to tagData.getTagSeenCount(),
    ))
  }

  private fun startAction(device: DeviceHandler) {
    println("Zebra: Starting inventory action on device: ${device.getName()}")

    device.startInventory()
  }

  private fun stopAction(device: DeviceHandler) {
    println("Zebra: Stopping inventory action on device: ${device.getName()}")

    device.stopInventory()
  }
}
