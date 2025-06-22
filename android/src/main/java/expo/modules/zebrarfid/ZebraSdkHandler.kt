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
import com.zebra.rfid.api3.TagData
import com.zebra.rfid.api3.Inventory
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ZebraSdkHandler(
  private val context: Context,
) {
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

  public fun getAvailableDevices(): List<ReaderDevice> =
    sdk.GetAvailableRFIDReaderList()

  public fun getConnectedDevices(): List<ReaderDevice> =
    connectedDevices.values.map {
      it.getDevice()
    }.toList()

  public fun connectToDevice(deviceAddress: String): Boolean {
    val availableDevices = getAvailableDevices()
    val device = availableDevices.find { it.getAddress() == deviceAddress }

    if (device == null) {
      println("Zebra: device with Address $deviceAddress not found in available Readers.")
      return false
    }

    val reader = device.getRFIDReader()

    if (reader == null) {
      println("Zebra: Failed to get RFIDReader for scanner with Address $deviceAddress.")
      return false
    }

    reader.connect()

    if (!reader.isConnected()) {
      println("Zebra: Failed to connect to scanner: ${device.getName()}")
      return false
    }
    
    connectedDevices.set(deviceAddress, DeviceHandler(this, device))

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

    // Here you can handle the RFID read event, e.g., store it or process it further
  }

  public fun onDeviceTriggered(deviceHandler: DeviceHandler, event: String) {
    when (event) {
      DeviceHandler.ON_TRIGGER_PRESSED -> {
        startAction()
      }
      DeviceHandler.ON_TRIGGER_RELEASED -> {
        stopAction()
      }
      else -> {
        println("Zebra: Unknown RFID read event: $event")
      }
    }
  }

  public fun startAction() {
    for (deviceHandler in connectedDevices.values) {
      try {
        val reader = deviceHandler.getReader()
        
        if (reader.isConnected()) {
          reader.Actions.Inventory.perform()
          println("Zebra: Started inventory action on device: ${deviceHandler.getName()}")
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

  public fun stopAction() {
    for (deviceHandler in connectedDevices.values) {
      try {
        val reader = deviceHandler.getReader()
        
        if (reader.isConnected()) {
          reader.Actions.Inventory.stop()
          println("Zebra: Stopped inventory action on device: ${deviceHandler.getName()}")
        } else {
          println("Zebra: Device ${deviceHandler.getName()} is not connected.")
        }
      } catch (e: InvalidUsageException) {
        println("Zebra: Invalid usage while stopping action: ${e.message}")
      } catch (e: OperationFailureException) {
        println("Zebra: Operation failed while stopping action: ${e.message}")
      }
    }
  }
}
