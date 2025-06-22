import { NativeModule, requireNativeModule } from "expo";

import { ExpoZebraRfidModuleEvents } from "./ExpoZebraRfid.types";

export interface ScannerInfo {
  id: string;
  name: string;
  serialNumber: string;
  transport: "Bluetooth" | "USB";
}

declare class ExpoZebraRfidModule extends NativeModule<ExpoZebraRfidModuleEvents> {
  isSDKInitialized(): boolean;
  getSDKVersion(): string;

  hasRequiredPermissions(): boolean;
  requestPermissions(): Promise<boolean>;

  getAvailableDevices(): Promise<ScannerInfo[]>;
  // connectToDevice(scannerId: number): Promise<boolean>;
  // disconnectFromScanner(scannerId: number): Promise<boolean>;
  // isConnectedToScanner(scannerId: number): boolean;
  // getConnectedDevices(): number[];
  // triggerScan(scannerId: number): Promise<boolean>;
  // startRfidInventory(scannerId: number): Promise<boolean>;
  // stopRfidInventory(scannerId: number): Promise<boolean>;
  // readRfidTag(scannerId: number, tagId: string): Promise<string | null>;
  // writeRfidTag(
  //   scannerId: number,
  //   tagId: string,
  //   data: string
  // ): Promise<boolean>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoZebraRfidModule>("ExpoZebraRfid");
