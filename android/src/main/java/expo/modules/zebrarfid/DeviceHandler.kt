package expo.modules.zebrarfid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zebra.rfid.api3.ENUM_TRANSPORT
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.IEvents.HandheldTriggerEventData
import com.zebra.rfid.api3.TriggerInfo;
import com.zebra.rfid.api3.Readers
import com.zebra.rfid.api3.ReaderDevice
import com.zebra.rfid.api3.RFIDReader
import com.zebra.rfid.api3.RfidEventsListener
import com.zebra.rfid.api3.RfidReadEvents
import com.zebra.rfid.api3.RfidStatusEvents
import com.zebra.rfid.api3.START_TRIGGER_TYPE
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE
import com.zebra.rfid.api3.STATUS_EVENT_TYPE
import com.zebra.rfid.api3.TagData
import com.zebra.rfid.api3.Inventory
import com.zebra.rfid.api3.TagDataArray
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class DeviceHandler(
  private val sdkHandler: ZebraSdkHandler,
  private val device: ReaderDevice,
): RfidEventsListener {
  private val reader = device.getRFIDReader()
  private val triggerInfo: TriggerInfo = TriggerInfo().apply {
    StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE)
    StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE)
  }
  private var currentAction: DeviceAction = DeviceAction.NONE

  companion object {
    const val ON_TRIGGER_PRESSED = "onTriggerPressed"
    const val ON_TRIGGER_RELEASED = "onTriggerReleased"

    enum class DeviceAction {
      NONE,
      INVENTORY,
    }
  }

  fun connect() {
    reader.connect()

    loadReader()
  }

  fun disconnect() {
    unloadReader()

    reader.disconnect()
  }

  fun getDevice(): ReaderDevice = device
  fun getReader(): RFIDReader = reader

  fun getId(): String = getAddress()
  fun getName(): String = device.getName()
  fun getAddress(): String = device.getAddress()
  fun getSerialNumber(): String = device.getSerialNumber()
  fun isConnected(): Boolean = reader.isConnected()
  fun getTransport(): String = device.getTransport().toString()
  fun getVersion(): String? = try {
    reader.versionInfo()?.getVersion() ?: null
  } catch (e: Exception) {
    null
  }
  fun getAction(): DeviceAction = currentAction
  fun isInventoryRunning(): Boolean = currentAction == DeviceAction.INVENTORY

  fun toReactObject(): Map<String, String?> =
    mapOf(
      "id" to getId(),
      "name" to getName(),
      "address" to getAddress(),
      "serialNumber" to getSerialNumber(),
      "transport" to getTransport(),
      "version" to getVersion(),
      "connected" to isConnected().toString(),
    )

  private fun loadReader() {
    try {
      // receive events from reader
      reader.Events.addEventsListener(this);
      // HH event
      reader.Events.setHandheldEvent(true);
      // tag event with tag data
      reader.Events.setTagReadEvent(true);
      // application will collect tag using getReadTags API
      reader.Events.setAttachTagDataWithReadEvent(true);
      // TODO: Need to handle others.
      // reader.Events.setReaderDisconnectEvent(true);
      // set start and stop triggers
      reader.Config.setStartTrigger(triggerInfo.StartTrigger);
      reader.Config.setStopTrigger(triggerInfo.StopTrigger);
    } catch (e: OperationFailureException) {
      println("Zebra: Failed to parametrize reader: " + e.message);
      e.printStackTrace()
    } catch (e: InvalidUsageException ) {
      println("Zebra: Invalid usage while parametrizing reader: " + e.message);
      e.printStackTrace()
    }
  }

  private fun unloadReader() {
    reader.Events.removeEventsListener(this)
  }

  override fun eventReadNotify(rfidReadEvents: RfidReadEvents) {
    println("Zebra: Read Notification: " + rfidReadEvents.getReadEventData().tagData.getTagID());

    sdkHandler.onRfidRead(this, rfidReadEvents.getReadEventData().tagData)
  }

  override fun eventStatusNotify(rfidStatusEvents: RfidStatusEvents) {
    println("Zebra: Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());

    when (rfidStatusEvents.StatusEventData.getStatusEventType()) {
      STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT -> {
        handleHandheldTriggerEvent(rfidStatusEvents.StatusEventData.HandheldTriggerEventData)
      }
      else -> {
        println("Zebra: Unhandled status event type: " + rfidStatusEvents.StatusEventData.getStatusEventType());
      }
    }
  }

  private fun handleHandheldTriggerEvent(eventData: HandheldTriggerEventData) {
    when (eventData.getHandheldEvent()) {
      HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED -> {
        sdkHandler.onDeviceTriggered(this, ON_TRIGGER_PRESSED)
      }
      HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED -> {
        sdkHandler.onDeviceTriggered(this, ON_TRIGGER_RELEASED)
      }
      else -> {
        println("Zebra: Unhandled handheld trigger event type: " + eventData);
      }
    }
  }

  public fun startInventory(): Boolean {
    if (currentAction != DeviceAction.NONE) {
      println("Zebra: Cannot start inventory, another action is already in progress.")

      if (currentAction == DeviceAction.INVENTORY) {
        println("Zebra: Inventory is already running on device: ${getName()}")
        return true
      }

      return false
    }

    try {
      reader.Actions.Inventory.perform()
      currentAction = DeviceAction.INVENTORY

      println("Zebra: Inventory started on device: ${getName()}")
      return true
    } catch (e: InvalidUsageException) {
      println("Zebra: Invalid usage while starting inventory: ${e.message}")
    } catch (e: OperationFailureException) {
      println("Zebra: Operation failure while starting inventory: ${e.message}")
    }

    return false
  }

  public fun stopInventory(): Boolean {
    if (currentAction != DeviceAction.INVENTORY) {
      println("Zebra: Cannot stop inventory, no inventory action is in progress.")

      return false
    }

    try {
      reader.Actions.Inventory.stop()
      currentAction = DeviceAction.NONE

      println("Zebra: Inventory stopped on device: ${getName()}")
      return true
    } catch (e: InvalidUsageException) {
      println("Zebra: Invalid usage while stopping inventory: ${e.message}")
    } catch (e: OperationFailureException) {
      println("Zebra: Operation failure while stopping inventory: ${e.message}")
    }

    return false
  }
}
