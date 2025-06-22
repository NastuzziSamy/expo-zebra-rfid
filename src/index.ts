// Reexport the native module. On web, it will be resolved to ExpoZebraRfidModule.web.ts
// and on native platforms to ExpoZebraRfidModule.ts
export { default } from "./ExpoZebraRfidModule";
export * from "./ExpoZebraRfid.types";
export type { ScannerInfo } from "./ExpoZebraRfidModule";

import ExpoZebraRfidModule, { ScannerInfo } from "./ExpoZebraRfidModule";

export function isSDKInitialized(): boolean {
  return ExpoZebraRfidModule.isSDKInitialized();
}

export function getSDKVersion(): string {
  return ExpoZebraRfidModule.getSDKVersion();
}

export function hasRequiredPermissions(): boolean {
  return ExpoZebraRfidModule.hasRequiredPermissions();
}

export async function requestPermissions(): Promise<boolean> {
  return await ExpoZebraRfidModule.requestPermissionsAsync();
}

export async function getAvailableDevices(): Promise<ScannerInfo[]> {
  return await ExpoZebraRfidModule.getAvailableDevicesAsync();
}

export async function connectToDevice(scannerId: number): Promise<boolean> {
  return await ExpoZebraRfidModule.connectToDeviceAsync(scannerId);
}

export async function disconnectFromScanner(
  scannerId: number
): Promise<boolean> {
  return await ExpoZebraRfidModule.disconnectFromScanner(scannerId);
}

export function isConnectedToScanner(scannerId: number): boolean {
  return ExpoZebraRfidModule.isConnectedToScanner(scannerId);
}

export function getConnectedDevices(): number[] {
  return ExpoZebraRfidModule.getConnectedDevices();
}

export async function triggerScan(scannerId: number): Promise<boolean> {
  return await ExpoZebraRfidModule.triggerScan(scannerId);
}

export async function startRfidInventory(scannerId: number): Promise<boolean> {
  return await ExpoZebraRfidModule.startRfidInventory(scannerId);
}

export async function stopRfidInventory(scannerId: number): Promise<boolean> {
  return await ExpoZebraRfidModule.stopRfidInventory(scannerId);
}

export async function readRfidTag(
  scannerId: number,
  tagId: string
): Promise<string | null> {
  return await ExpoZebraRfidModule.readRfidTag(scannerId, tagId);
}

export async function writeRfidTag(
  scannerId: number,
  tagId: string,
  data: string
): Promise<boolean> {
  return await ExpoZebraRfidModule.writeRfidTag(scannerId, tagId, data);
}
