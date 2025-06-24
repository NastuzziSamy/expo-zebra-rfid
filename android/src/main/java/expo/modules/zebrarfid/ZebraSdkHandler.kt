package expo.modules.zebrarfid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.ENUM_TRANSPORT
import com.zebra.rfid.api3.Readers
import com.zebra.rfid.api3.ReaderDevice
import com.zebra.rfid.api3.RfidEventsListener
import com.zebra.rfid.api3.RfidReadEvents
import com.zebra.rfid.api3.RfidStatusEvents
import com.zebra.rfid.api3.RFIDReader
import com.zebra.rfid.api3.TagData
import com.zebra.rfid.api3.Inventory
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ZebraSdkHandler(
  private val module: ExpoZebraRfidModule,
) {
  private val context: Context = module.getContext() ?: throw IllegalStateException("React context is not available")
  private lateinit var sdk: Readers
  private val connectedDevices: MutableMap<String, DeviceHandler> = mutableMapOf()

  companion object {
    const val SDK_VERSION = "2.0.4.192"
  }

  init {
    sdk = Readers(context, ENUM_TRANSPORT.BLUETOOTH)
  }

  public fun getSdkVersion(): String = SDK_VERSION

  public fun isReady(): Boolean = sdk != null

  public fun getAvailableDevices(): List<DeviceHandler> =
    sdk.GetAvailableRFIDReaderList().map { device ->
      DeviceHandler(this, device)
    }

  public fun getConnectedDevices(): List<DeviceHandler> =
    connectedDevices.values.toList()

  public fun connectToDevice(deviceId: String): Boolean {
    val availableDevices = getAvailableDevices()
    val device = availableDevices.find { it.getId() == deviceId }

    if (device == null) {
      println("Zebra: device with Address $deviceId not found in available Readers.")
      return false
    }

    device.connect()

    if (!device.isConnected()) {
      println("Zebra: Failed to connect to scanner: ${device.getName()}")
      return false
    }
    
    connectedDevices.set(deviceId, device)

    return true
  }

  public fun disconnectFromDevice(deviceAddress: String): Boolean {
    val device = connectedDevices[deviceAddress]

    if (device == null) {
      println("Zebra: No device connected with Address $deviceAddress.")
      return false
    }

    device.disconnect()
    connectedDevices.remove(deviceAddress)

    println("Zebra: Successfully disconnected from scanner: ${device.getName()}")
    return true
  }

  public fun onRfidRead(deviceHandler: DeviceHandler, tagData: TagData) {
    val tagId = tagData.getTagID()
    val rssi = tagData.getPeakRSSI()

    println("Zebra: RFID read from device: ${deviceHandler.getName()}, Tag ID: $tagId, RSSI: $rssi")

    module.onRfidRead(deviceHandler, tagData)
  }

  public fun onDeviceTriggered(deviceHandler: DeviceHandler, event: String) {
    when (event) {
      DeviceHandler.ON_TRIGGER_PRESSED -> {
        println("Zebra: Handheld trigger pressed event received.");
        module.onDeviceTriggered(deviceHandler)
      }
      DeviceHandler.ON_TRIGGER_RELEASED -> {
        println("Zebra: Handheld trigger released event received.");
        module.onDeviceReleased(deviceHandler)
      }
      else -> {
        println("Zebra: Unknown RFID read event: $event")
      }
    }
  }

  public fun forEachConnectedDevice(action: (DeviceHandler) -> Unit) {
    for (deviceHandler in connectedDevices.values) {
      try {
        val reader = deviceHandler.getReader()
        
        if (reader.isConnected()) {
          action(deviceHandler)
        } else {
          println("Zebra: Device ${deviceHandler.getName()} is not connected.")
        }
      } catch (e: InvalidUsageException) {
        println("Zebra: Invalid usage while starting action: ${e.message}")
      } catch (e: OperationFailureException) {
        println("Zebra: Operation failed while starting action: ${e.message}")
      }
    }
  }
}
