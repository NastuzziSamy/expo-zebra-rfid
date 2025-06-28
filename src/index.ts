// Reexport the native module. On web, it will be resolved to ExpoZebraRfidModule.web.ts
// and on native platforms to ExpoZebraRfidModule.ts
export { default } from "./ExpoZebraRfidModule";
export * from "./ExpoZebraRfid.types";
export type { Device } from "./ExpoZebraRfidModule";

import ExpoZebraRfidModule, {
  type Device,
  DEVICE_CONNECTED,
  NativeDevice,
} from "./ExpoZebraRfidModule";

export const fromNativeDevice = ({
  status,
  ...device
}: NativeDevice): Device => ({
  ...device,
  isConnected: status === DEVICE_CONNECTED,
});

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

export async function getAvailableDevices(): Promise<Device[]> {
  return (await ExpoZebraRfidModule.getAvailableDevicesAsync()).map(
    fromNativeDevice
  );
}

export async function connectToDevice(deviceId: string): Promise<boolean> {
  return await ExpoZebraRfidModule.connectToDeviceAsync(deviceId);
}

export async function disconnectFromDevice(deviceId: string): Promise<boolean> {
  return await ExpoZebraRfidModule.disconnectFromDeviceAsync(deviceId);
}

export function isConnectedToScanner(deviceId: string): boolean {
  return ExpoZebraRfidModule.isConnectedToScanner(deviceId);
}

export function getConnectedDevices(): Device[] {
  return ExpoZebraRfidModule.getConnectedDevices().map(fromNativeDevice);
}

// export async function triggerScan(deviceId: string): Promise<boolean> {
//   return await ExpoZebraRfidModule.triggerScan(deviceId);
// }

// export async function startRfidInventory(deviceId: string): Promise<boolean> {
//   return await ExpoZebraRfidModule.startRfidInventory(deviceId);
// }

// export async function stopRfidInventory(deviceId: string): Promise<boolean> {
//   return await ExpoZebraRfidModule.stopRfidInventory(deviceId);
// }

// export async function readRfidTag(
//   deviceId: string,
//   tagId: string
// ): Promise<string | null> {
//   return await ExpoZebraRfidModule.readRfidTag(deviceId, tagId);
// }

// export async function writeRfidTag(
//   deviceId: string,
//   tagId: string,
//   data: string
// ): Promise<boolean> {
//   return await ExpoZebraRfidModule.writeRfidTag(deviceId, tagId, data);
// }
