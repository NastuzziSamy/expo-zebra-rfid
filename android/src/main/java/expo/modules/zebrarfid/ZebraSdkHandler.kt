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
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ZebraSdkHandler(
  private val context: Context,
) {
  private lateinit var sdk: Readers
  private val connectedDevices: MutableMap<String, ReaderDevice> = mutableMapOf()

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
    connectedDevices.values.toList()

  public fun connectToDevice(deviceAddress: String): Boolean {
    val availableDevices = getAvailableDevices()
    val device = availableDevices.find { it.getAddress() == deviceAddress }

    if (device == null) {
      println("device with Address $deviceAddress not found in available Readers.")
      return false
    }

    val reader = device.getRFIDReader()

    if (reader == null) {
      println("Failed to get RFIDReader for scanner with Address $deviceAddress.")
      return false
    }

    reader.connect()

    return reader.isConnected().also { isConnected ->
      if (isConnected) { 
        connectedDevices.set(deviceAddress, device)
        println("Successfully connected to scanner: ${device.getName()}")
      } else {
        println("Failed to connect to scanner: ${device.getName()}")
      }
    }
  }
}
