import type { StyleProp, ViewStyle } from "react-native";

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
    deviceId: string;
    trigger: DeviceTrigger;
  }) => void;
};

export type ExpoZebraRfidViewProps = {
  url: string;
  onLoad: (event: { nativeEvent: OnLoadEventPayload }) => void;
  style?: StyleProp<ViewStyle>;
};
