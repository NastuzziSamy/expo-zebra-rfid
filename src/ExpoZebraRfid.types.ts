import type { StyleProp, ViewStyle } from "react-native";
import type { ScannerInfo } from "./ExpoZebraRfidModule";

export type OnLoadEventPayload = {
  url: string;
};

export type ExpoZebraRfidModuleEvents = {
  onChange: (params: ChangeEventPayload) => void;
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
