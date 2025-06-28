import { NativeModule, requireNativeModule } from "expo";

import { ExpoZebraRfidModuleEvents } from "./ExpoZebraRfid.types";

export const DEVICE_CONNECTED = "CONNECTED";
export const DEVICE_DISCONNECTED = "DISCONNECTED";

export type DeviceTransport = "BLUETOOTH" | "USB";
export interface Device {
  id: string;
  name: string;
  address: string;
  serialNumber: string;
  transport: DeviceTransport;
  version: string | null;
  isConnected: boolean;
}

export interface NativeDevice extends Omit<Device, "isConnected"> {
  status: string;
}

declare class ExpoZebraRfidModule extends NativeModule<ExpoZebraRfidModuleEvents> {
  isSDKInitialized(): boolean;
  getSDKVersion(): string;

  hasRequiredPermissions(): boolean;
  requestPermissions(): Promise<boolean>;

  getAvailableDevices(): Promise<NativeDevice[]>;
  connectToDevice(deviceId: string): Promise<boolean>;
  disconnectFromDevice(deviceId: string): Promise<boolean>;
  isConnectedToDevice(deviceId: string): boolean;
  getConnectedDevices(): NativeDevice[];
  // triggerScan(deviceId: string): Promise<boolean>;
  // startRfidInventory(deviceId: string): Promise<boolean>;
  // stopRfidInventory(deviceId: string): Promise<boolean>;
  // readRfidTag(deviceId: string, tagId: string): Promise<string | null>;
  // writeRfidTag(
  //   deviceId: string,
  //   tagId: string,
  //   data: string
  // ): Promise<boolean>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoZebraRfidModule>("ExpoZebraRfid");
