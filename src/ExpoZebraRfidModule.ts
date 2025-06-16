import { NativeModule, requireNativeModule } from "expo";

import { ExpoZebraRfidModuleEvents } from "./ExpoZebraRfid.types";

export interface ScannerInfo {
  scannerId: number;
  scannerName: string;
  isActive: boolean;
  isAvailable: boolean;
  connectionType: string;
}

declare class ExpoZebraRfidModule extends NativeModule<ExpoZebraRfidModuleEvents> {
  PI: number;
  hello(): string;
  isSDKInitialized(): boolean;
  getSDKVersion(): string;
  hasRequiredPermissions(): boolean;
  requestPermissionsAsync(): Promise<boolean>;
  getAvailableScannersAsync(): Promise<ScannerInfo[]>;
  connectToScannerAsync(scannerId: number): Promise<boolean>;
  disconnectFromScannerAsync(scannerId: number): Promise<boolean>;
  isConnectedToScanner(scannerId: number): boolean;
  getConnectedScanners(): number[];
  triggerScan(scannerId: number): Promise<boolean>;
  startRfidInventory(scannerId: number): Promise<boolean>;
  stopRfidInventory(scannerId: number): Promise<boolean>;
  readRfidTag(scannerId: number, tagId: string): Promise<string | null>;
  writeRfidTag(
    scannerId: number,
    tagId: string,
    data: string
  ): Promise<boolean>;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoZebraRfidModule>("ExpoZebraRfid");
