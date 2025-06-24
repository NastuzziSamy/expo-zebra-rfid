import type { StyleProp, ViewStyle } from "react-native";
import type { ScannerInfo } from "./ExpoZebraRfidModule";

export type OnLoadEventPayload = {
  url: string;
};

export enum DeviceTrigger {
  PRESSED = "pressed",
  RELEASED = "released",
}

export type ExpoZebraRfidModuleEvents = {
  onRfidRead: (params: {
    tagId: string;
    deviceId: string;
    rssi: number | null;
    crc: number | null;
    antennaId: number | null;
    count: number;
  }) => void;
  onDeviceTriggered: (params: {
    scannerId: number;
    trigger: DeviceTrigger;
  }) => void;
};

export type ChangeEventPayload = {
  value?: string;
  event?:
    | "scannerAppeared"
    | "scannerDisappeared"
    | "sessionEstablished"
    | "sessionTerminated";
  scanner?: ScannerInfo;
  scannerId?: number;
  data?: string;
  type?: number;
};

export type ExpoZebraRfidViewProps = {
  url: string;
  onLoad: (event: { nativeEvent: OnLoadEventPayload }) => void;
  style?: StyleProp<ViewStyle>;
};
