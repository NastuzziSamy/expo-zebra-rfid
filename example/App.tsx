import { useEvent } from "expo";
import ExpoZebraRfid, {
  isSDKInitialized,
  getSDKVersion,
  hasRequiredPermissions,
  requestPermissions,
  getAvailableDevices,
  connectToDevice,
  disconnectFromDevice,
  isConnectedToScanner,
  getConnectedDevices,
  triggerScan,
  startRfidInventory,
  stopRfidInventory,
  readRfidTag,
  writeRfidTag,
  ScannerInfo,
} from "expo-zebra-rfid";
import {
  Button,
  SafeAreaView,
  ScrollView,
  Text,
  View,
  TextInput,
  Alert,
} from "react-native";
import { useState, useEffect } from "react";

export default function App() {
  const onChangePayload = useEvent(ExpoZebraRfid, "onChange");
  const [sdkInitialized, setSdkInitialized] = useState<boolean>(false);
  const [sdkVersion, setSdkVersion] = useState<string>("");
  const [Readers, setReaders] = useState<ScannerInfo[]>([]);
  const [isScanning, setIsScanning] = useState<boolean>(false);
  const [events, setEvents] = useState<string[]>([]);
  const [hasPermissions, setHasPermissions] = useState<boolean>(false);
  const [isRequestingPermissions, setIsRequestingPermissions] =
    useState<boolean>(false);
  const [connectedReaders, setConnectedReaders] = useState<number[]>([]);
  const [connectingToScanner, setConnectingToScanner] = useState<number | null>(
    null
  );
  const [scanningStates, setScanningStates] = useState<{
    [scannerId: number]: { barcode: boolean; rfid: boolean };
  }>({});
  const [tagIdInput, setTagIdInput] = useState<string>("");
  const [tagDataInput, setTagDataInput] = useState<string>("");
  useEffect(() => {
    setSdkInitialized(isSDKInitialized());
    setSdkVersion(getSDKVersion());
    setHasPermissions(hasRequiredPermissions());
    getAvailableDevices().then((availableReaders) => {
      setReaders(availableReaders);
      console.log("Available Readers:", availableReaders);
    });
    setConnectedReaders(getConnectedDevices().map((scanner) => scanner.id));
  }, []);

  useEffect(() => {
    // Handle scanner events
    if (onChangePayload) {
      const timestamp = new Date().toLocaleTimeString();
      let eventMessage = `[${timestamp}] `;

      if (onChangePayload.event === "scannerAppeared") {
        eventMessage += `Scanner appeared: ${onChangePayload.scanner?.scannerName}`;
      } else if (onChangePayload.event === "scannerDisappeared") {
        eventMessage += `Scanner disappeared: ID ${onChangePayload.scannerId}`;
      } else if (onChangePayload.event === "sessionEstablished") {
        eventMessage += `Session established: ${onChangePayload.scanner?.scannerName}`;
      } else if (onChangePayload.event === "sessionTerminated") {
        eventMessage += `Session terminated: ID ${onChangePayload.scannerId}`;
      } else if (onChangePayload.event === "barcode") {
        eventMessage += `Barcode: ${onChangePayload.data} (Type: ${onChangePayload.type})`;
      } else if (onChangePayload.value) {
        eventMessage += `Value: ${onChangePayload.value}`;
      }

      setEvents((prev) => [eventMessage, ...prev.slice(0, 9)]); // Keep last 10 events
    }
  }, [onChangePayload]);

  const scanForDevices = async () => {
    try {
      setIsScanning(true);
      const availableReaders = await getAvailableDevices();
      setReaders(availableReaders);
      console.log("Available Readers:", availableReaders);
    } catch (error) {
      console.error("Error scanning for devices:", error);
    } finally {
      setIsScanning(false);
    }
  };

  const askForPermissions = async () => {
    try {
      setIsRequestingPermissions(true);
      const granted = await requestPermissions();
      setHasPermissions(granted);
      if (granted) {
        const timestamp = new Date().toLocaleTimeString();
        setEvents((prev) => [
          `[${timestamp}] Permissions granted successfully`,
          ...prev.slice(0, 9),
        ]);
      } else {
        const timestamp = new Date().toLocaleTimeString();
        setEvents((prev) => [
          `[${timestamp}] Permissions denied`,
          ...prev.slice(0, 9),
        ]);
      }
    } catch (error) {
      console.error("Error requesting permissions:", error);
    } finally {
      setIsRequestingPermissions(false);
    }
  };

  const tryToConnect = async (scannerId: String) => {
    try {
      setConnectingToScanner(scannerId);
      const connected = await connectToDevice(scannerId);
      const timestamp = new Date().toLocaleTimeString();

      if (connected) {
        const devices = getConnectedDevices();
        console.log("Connected Devices:", devices);
        setConnectedReaders(devices.map((scanner) => scanner.id));
        const scanner = Readers.find((s) => s.id === scannerId);
        setEvents((prev) => [
          `[${timestamp}] Successfully connected to ${
            scanner?.name || `Scanner ${scannerId}`
          }`,
          ...prev.slice(0, 9),
        ]);
      } else {
        setEvents((prev) => [
          `[${timestamp}] Failed to connect to Scanner ${scannerId}`,
          ...prev.slice(0, 9),
        ]);
      }
    } catch (error) {
      console.error("Error connecting to scanner:", error);
      const timestamp = new Date().toLocaleTimeString();
      setEvents((prev) => [
        `[${timestamp}] Error connecting to Scanner ${scannerId}`,
        ...prev.slice(0, 9),
      ]);
    } finally {
      setConnectingToScanner(null);
    }
  };

  const tryToDisconnect = async (scannerId: number) => {
    try {
      const disconnected = await disconnectFromDevice(scannerId);
      const timestamp = new Date().toLocaleTimeString();

      if (disconnected) {
        setConnectedReaders(getConnectedDevices().map((scanner) => scanner.id));
        const scanner = Readers.find((s) => s.id === scannerId);
        setEvents((prev) => [
          `[${timestamp}] Successfully disconnected from ${
            scanner?.name || `Scanner ${scannerId}`
          }`,
          ...prev.slice(0, 9),
        ]);
      } else {
        setEvents((prev) => [
          `[${timestamp}] Failed to disconnect from Scanner ${scannerId}`,
          ...prev.slice(0, 9),
        ]);
      }
    } catch (error) {
      console.error("Error disconnecting from scanner:", error);
      const timestamp = new Date().toLocaleTimeString();
      setEvents((prev) => [
        `[${timestamp}] Error disconnecting from Scanner ${scannerId}`,
        ...prev.slice(0, 9),
      ]);
    }
  };

  const handleRfidInventory = async (scannerId: number, start: boolean) => {
    const timestamp = new Date().toLocaleTimeString();
    const scanner = Readers.find((s) => s.id === scannerId);

    try {
      const result = start
        ? await startRfidInventory(scannerId)
        : await stopRfidInventory(scannerId);

      if (result) {
        setScanningStates((prev) => ({
          ...prev,
          [scannerId]: { ...prev[scannerId], rfid: start },
        }));
        setEvents((prev) => [
          `[${timestamp}] ${start ? "Started" : "Stopped"} RFID inventory on ${
            scanner?.name || `Scanner ${scannerId}`
          }`,
          ...prev.slice(0, 9),
        ]);
      } else {
        setEvents((prev) => [
          `[${timestamp}] Failed to ${
            start ? "start" : "stop"
          } RFID inventory on Scanner ${scannerId}`,
          ...prev.slice(0, 9),
        ]);
      }
    } catch (error) {
      console.error("Error with RFID inventory:", error);
      setEvents((prev) => [
        `[${timestamp}] Error ${
          start ? "starting" : "stopping"
        } RFID inventory on Scanner ${scannerId}`,
        ...prev.slice(0, 9),
      ]);
    }
  };

  const handleTriggerScan = async (scannerId: number) => {
    const timestamp = new Date().toLocaleTimeString();
    const scanner = Readers.find((s) => s.id === scannerId);

    try {
      const result = await triggerScan(scannerId);

      if (result) {
        setEvents((prev) => [
          `[${timestamp}] Triggered scan on ${
            scanner?.name || `Scanner ${scannerId}`
          }`,
          ...prev.slice(0, 9),
        ]);
      } else {
        setEvents((prev) => [
          `[${timestamp}] Failed to trigger scan on Scanner ${scannerId}`,
          ...prev.slice(0, 9),
        ]);
      }
    } catch (error) {
      console.error("Error triggering scan:", error);
      setEvents((prev) => [
        `[${timestamp}] Error triggering scan on Scanner ${scannerId}`,
        ...prev.slice(0, 9),
      ]);
    }
  };

  const handleReadRfidTag = async (scannerId: number) => {
    if (!tagIdInput.trim()) {
      Alert.alert("Error", "Please enter a Tag ID");
      return;
    }

    const timestamp = new Date().toLocaleTimeString();
    const scanner = Readers.find((s) => s.id === scannerId);

    try {
      const result = await readRfidTag(scannerId, tagIdInput);

      if (result) {
        setEvents((prev) => [
          `[${timestamp}] Read RFID tag ${tagIdInput} from ${
            scanner?.name || `Scanner ${scannerId}`
          }: ${result}`,
          ...prev.slice(0, 9),
        ]);
      } else {
        setEvents((prev) => [
          `[${timestamp}] Failed to read RFID tag ${tagIdInput} from Scanner ${scannerId}`,
          ...prev.slice(0, 9),
        ]);
      }
    } catch (error) {
      console.error("Error reading RFID tag:", error);
      setEvents((prev) => [
        `[${timestamp}] Error reading RFID tag ${tagIdInput} from Scanner ${scannerId}`,
        ...prev.slice(0, 9),
      ]);
    }
  };

  const handleWriteRfidTag = async (scannerId: number) => {
    if (!tagIdInput.trim() || !tagDataInput.trim()) {
      Alert.alert("Error", "Please enter both Tag ID and Data");
      return;
    }

    const timestamp = new Date().toLocaleTimeString();
    const scanner = Readers.find((s) => s.id === scannerId);

    try {
      const result = await writeRfidTag(scannerId, tagIdInput, tagDataInput);

      if (result) {
        setEvents((prev) => [
          `[${timestamp}] Wrote RFID tag ${tagIdInput} to ${
            scanner?.name || `Scanner ${scannerId}`
          }: ${tagDataInput}`,
          ...prev.slice(0, 9),
        ]);
      } else {
        setEvents((prev) => [
          `[${timestamp}] Failed to write RFID tag ${tagIdInput} to Scanner ${scannerId}`,
          ...prev.slice(0, 9),
        ]);
      }
    } catch (error) {
      console.error("Error writing RFID tag:", error);
      setEvents((prev) => [
        `[${timestamp}] Error writing RFID tag ${tagIdInput} to Scanner ${scannerId}`,
        ...prev.slice(0, 9),
      ]);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.container}>
        <Text style={styles.header}>Module API Example</Text>
        <Group name="Zebra SDK Status">
          <Text>SDK Initialized: {sdkInitialized ? "✅ Yes" : "❌ No"}</Text>
          <Text>SDK Version: {sdkVersion}</Text>
          <Text>
            Permissions: {hasPermissions ? "✅ Granted" : "❌ Missing"}
          </Text>
          <Text>
            Connected Readers:{" "}
            {connectedReaders.length > 0
              ? `🔗 ${connectedReaders.length}`
              : "❌ None"}
          </Text>
          {connectedReaders.length > 0 && (
            <Text style={styles.permissionText}>
              • Scanner IDs:{" "}
              {connectedReaders.map(({ name }) => name).join(", ")}
            </Text>
          )}
          <Button
            title="Refresh SDK Status"
            onPress={() => {
              setSdkInitialized(isSDKInitialized());
              setSdkVersion(getSDKVersion());
              setHasPermissions(hasRequiredPermissions());
              setConnectedReaders(
                getConnectedDevices().map((scanner) => scanner.id)
              );
            }}
          />
        </Group>
        <Group name="Permissions">
          <Text>Required permissions for Bluetooth scanning:</Text>
          <Text style={styles.permissionText}>• Bluetooth Connect</Text>
          <Text style={styles.permissionText}>• Bluetooth Scan</Text>
          <Text style={styles.permissionText}>• Location Access</Text>
          <Button
            title={
              isRequestingPermissions ? "Requesting..." : "Request Permissions"
            }
            onPress={askForPermissions}
            disabled={isRequestingPermissions || hasPermissions}
          />
        </Group>
        <Group name="Scanner Detection">
          <Button
            title={isScanning ? "Scanning..." : "Scan for Readers"}
            onPress={scanForDevices}
            disabled={isScanning || !sdkInitialized || !hasPermissions}
          />
          {!hasPermissions && (
            <Text style={styles.warningText}>
              ⚠️ Permissions required for scanner detection
            </Text>
          )}
          <Text>Found {Readers.length} scanner(s):</Text>
          {Readers.map((scanner, index) => {
            const isConnected = connectedReaders.includes(scanner.id);
            const isConnecting = connectingToScanner === scanner.id;
            const scanningState = scanningStates[scanner.id] || {
              barcode: false,
              rfid: false,
            };

            return (
              <View key={scanner.id} style={styles.scannerItem}>
                <Text style={styles.scannerName}>
                  {scanner.name} (ID: {scanner.id})
                </Text>
                <Text>Type: {scanner.transport}</Text>
                {/* <Text>Active: {scanner.isActive ? "✅" : "❌"}</Text>
                <Text>Available: {scanner.isAvailable ? "✅" : "❌"}</Text>
                <Text>Connected: {isConnected ? "🔗 Yes" : "❌ No"}</Text> */}

                {/* Connection Controls */}
                <View style={styles.buttonContainer}>
                  {!isConnected ? (
                    <Button
                      title={isConnecting ? "Connecting..." : "Connect"}
                      onPress={() => tryToConnect(scanner.id)}
                      disabled={
                        isConnecting || !sdkInitialized || !hasPermissions
                      }
                    />
                  ) : (
                    <Button
                      title="Disconnect"
                      onPress={() => tryToDisconnect(scanner.id)}
                      disabled={!sdkInitialized}
                    />
                  )}
                </View>

                {/* Scanning Controls - Only show if connected */}
                {isConnected && (
                  <View style={styles.scanningSection}>
                    <Text style={styles.sectionHeader}>🏷️ RFID Operations</Text>
                    <View style={styles.buttonRow}>
                      <Button
                        title={scanningState.rfid ? "Stop RFID" : "Start RFID"}
                        onPress={() =>
                          handleRfidInventory(scanner.id, !scanningState.rfid)
                        }
                        disabled={!sdkInitialized}
                      />
                    </View>
                    <Text style={styles.statusText}>
                      Status:{" "}
                      {scanningState.rfid ? "🟢 Scanning" : "🔴 Stopped"}
                    </Text>
                  </View>
                )}
              </View>
            );
          })}
        </Group>
        <Group name="RFID Tag Operations">
          <Text>Operations for connected RFID Readers:</Text>
          <View style={styles.inputContainer}>
            <Text style={styles.inputLabel}>Tag ID:</Text>
            <TextInput
              style={styles.textInput}
              value={tagIdInput}
              onChangeText={setTagIdInput}
              placeholder="Enter tag ID (e.g., 1234ABCD)"
              autoCapitalize="none"
            />
          </View>
          <View style={styles.inputContainer}>
            <Text style={styles.inputLabel}>Tag Data (for writing):</Text>
            <TextInput
              style={styles.textInput}
              value={tagDataInput}
              onChangeText={setTagDataInput}
              placeholder="Enter data to write"
              autoCapitalize="none"
            />
          </View>
          {connectedReaders.length > 0 ? (
            connectedReaders.map((scannerId) => {
              const scanner = Readers.find((s) => s.id === scannerId);
              return (
                <View key={scannerId} style={styles.rfidOperationItem}>
                  <Text style={styles.scannerName}>
                    {scanner.name || `Scanner ${scanner.id}`}
                  </Text>
                  <View style={styles.buttonRow}>
                    <Button
                      title="Read Tag"
                      onPress={() => handleReadRfidTag(scanner.id)}
                      disabled={!tagIdInput.trim()}
                    />
                    <Button
                      title="Write Tag"
                      onPress={() => handleWriteRfidTag(scanner.id)}
                      disabled={!tagIdInput.trim() || !tagDataInput.trim()}
                    />
                  </View>
                </View>
              );
            })
          ) : (
            <Text style={styles.noConnectedReaders}>
              No connected Readers for RFID operations
            </Text>
          )}
        </Group>
        <Group name="Events">
          <Button title="Clear Events" onPress={() => setEvents([])} />
          <ScrollView style={styles.eventsList} nestedScrollEnabled>
            {events.length === 0 ? (
              <Text style={styles.noEvents}>No events yet...</Text>
            ) : (
              events.map((event, index) => (
                <Text key={index} style={styles.eventItem}>
                  {event}
                </Text>
              ))
            )}
          </ScrollView>
        </Group>
      </ScrollView>
    </SafeAreaView>
  );
}

function Group(props: { name: string; children: React.ReactNode }) {
  return (
    <View style={styles.group}>
      <Text style={styles.groupHeader}>{props.name}</Text>
      {props.children}
    </View>
  );
}

const styles = {
  header: {
    fontSize: 30,
    margin: 20,
  },
  groupHeader: {
    fontSize: 20,
    marginBottom: 20,
  },
  group: {
    margin: 20,
    backgroundColor: "#fff",
    borderRadius: 10,
    padding: 20,
  },
  container: {
    flex: 1,
    backgroundColor: "#eee",
  },
  view: {
    flex: 1,
    height: 200,
  },
  scannerItem: {
    backgroundColor: "#f5f5f5",
    padding: 10,
    marginVertical: 5,
    borderRadius: 5,
    borderLeftWidth: 3,
    borderLeftColor: "#007AFF",
  },
  scannerName: {
    fontSize: 16,
    fontWeight: "bold" as const,
    marginBottom: 5,
  },
  eventsList: {
    maxHeight: 200,
    backgroundColor: "#f9f9f9",
    padding: 10,
    marginTop: 10,
    borderRadius: 5,
  },
  eventItem: {
    fontSize: 12,
    marginBottom: 5,
    color: "#333",
  },
  noEvents: {
    fontSize: 14,
    color: "#888",
    fontStyle: "italic" as const,
    textAlign: "center" as const,
  },
  permissionText: {
    fontSize: 14,
    color: "#666",
    marginLeft: 10,
    marginVertical: 2,
  },
  warningText: {
    fontSize: 14,
    color: "#ff6b6b",
    marginTop: 5,
    fontStyle: "italic" as const,
  },
  buttonContainer: {
    marginTop: 10,
    marginHorizontal: 5,
  },
  scanningSection: {
    backgroundColor: "#f0f8ff",
    padding: 10,
    marginTop: 10,
    borderRadius: 5,
    borderLeftWidth: 3,
    borderLeftColor: "#4CAF50",
  },
  sectionHeader: {
    fontSize: 16,
    fontWeight: "bold" as const,
    marginBottom: 8,
    color: "#333",
  },
  buttonRow: {
    flexDirection: "row" as const,
    justifyContent: "space-around" as const,
    marginBottom: 5,
  },
  statusText: {
    fontSize: 14,
    color: "#666",
    marginTop: 5,
    textAlign: "center" as const,
  },
  inputContainer: {
    marginVertical: 10,
  },
  inputLabel: {
    fontSize: 16,
    fontWeight: "bold" as const,
    marginBottom: 5,
    color: "#333",
  },
  textInput: {
    borderWidth: 1,
    borderColor: "#ddd",
    borderRadius: 5,
    padding: 10,
    fontSize: 16,
    backgroundColor: "#fff",
  },
  rfidOperationItem: {
    backgroundColor: "#f9f9f9",
    padding: 10,
    marginVertical: 5,
    borderRadius: 5,
    borderLeftWidth: 3,
    borderLeftColor: "#FF9800",
  },
  noConnectedReaders: {
    fontSize: 14,
    color: "#999",
    fontStyle: "italic" as const,
    textAlign: "center" as const,
    marginTop: 10,
  },
};
