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

  companion object {
    const val ON_TRIGGER_PRESSED = "onTriggerPressed"
    const val ON_TRIGGER_RELEASED = "onTriggerReleased"
  }

  init {
    parametrizeReader()
  }

  fun disconnect() {
    reader.Events.removeEventsListener(this)
    reader.disconnect()
  }

  fun getName(): String = device.getName()

  fun getDevice(): ReaderDevice = device
  fun getReader(): RFIDReader = reader

  private fun parametrizeReader() {
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

  override fun eventReadNotify(rfidReadEvents: RfidReadEvents) {
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

        // try {
        //   reader.Actions.Inventory.perform();
        // } catch (e: Exception) {
        //   println("Zebra: Error starting reading: " + e.message);
        // }
      }
      HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED -> {
        sdkHandler.onDeviceTriggered(this, ON_TRIGGER_RELEASED)

        // try {
        //   reader.Actions.Inventory.stop();
        // } catch (e: Exception) {
        //   println("Zebra: Error stopping reading: " + e.message);
        // }
      }
      else -> {
        println("Zebra: Unhandled handheld trigger event type: " + eventData);
      }
    }
  }
}
