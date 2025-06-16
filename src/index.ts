// Reexport the native module. On web, it will be resolved to ExpoZebraRfidModule.web.ts
// and on native platforms to ExpoZebraRfidModule.ts
export { default } from "./ExpoZebraRfidModule";
export * from "./ExpoZebraRfid.types";
export type { ScannerInfo } from "./ExpoZebraRfidModule";

import ExpoZebraRfidModule, { ScannerInfo } from "./ExpoZebraRfidModule";

// Convenience functions for easier usage
export function hello(): string {
  return ExpoZebraRfidModule.hello();
}

export function isSDKInitialized(): boolean {
  return ExpoZebraRfidModule.isSDKInitialized();
}

export function getSDKVersion(): string {
  return ExpoZebraRfidModule.getSDKVersion();
}

export function hasRequiredPermissions(): boolean {
  return ExpoZebraRfidModule.hasRequiredPermissions();
}

export async function requestPermissionsAsync(): Promise<boolean> {
  return await ExpoZebraRfidModule.requestPermissionsAsync();
}

export async function getAvailableScannersAsync(): Promise<ScannerInfo[]> {
  return await ExpoZebraRfidModule.getAvailableScannersAsync();
}

export async function connectToScannerAsync(
  scannerId: number
): Promise<boolean> {
  return await ExpoZebraRfidModule.connectToScannerAsync(scannerId);
}

export async function disconnectFromScannerAsync(
  scannerId: number
): Promise<boolean> {
  return await ExpoZebraRfidModule.disconnectFromScannerAsync(scannerId);
}

export function isConnectedToScanner(scannerId: number): boolean {
  return ExpoZebraRfidModule.isConnectedToScanner(scannerId);
}

export function getConnectedScanners(): number[] {
  return ExpoZebraRfidModule.getConnectedScanners();
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

export async function setValueAsync(value: string): Promise<void> {
  return await ExpoZebraRfidModule.setValueAsync(value);
}
