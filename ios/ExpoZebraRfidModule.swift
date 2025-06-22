import ExpoModulesCore
import ZebraRfidSdkFramework

public class ExpoZebraRfidModule: Module {
  // RFID SDK API instance
  private var rfidSdkApi: srfidISdkApi?
  
  // Array to store available readers
  private var availableReadersList = NSMutableArray()
  
  // Set to track connected reader IDs
  private var connectedReaders = Set<Int>()
  
  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  public func definition() -> ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('ExpoZebraRfid')` in JavaScript.
    Name("ExpoZebraRfid")
    
    // Initialize SDK when module is created
    OnCreate {
      print("🚀 Initializing RFID SDK...")
      self.rfidSdkApi = srfidSdkFactory.createRfidSdkApiInstance()
      if let api = self.rfidSdkApi {
        print("✅ RFID SDK API created successfully")
        
        // Set up operational modes for MFi and BTLE
        let mfiResult = api.srfidSetOperationalMode(Int32(SRFID_OPMODE_MFI))
        print("📱 MFi mode setup result: \(mfiResult)")
        
        let btleResult = api.srfidSetOperationalMode(Int32(SRFID_OPMODE_BTLE))
        print("📶 BTLE mode setup result: \(btleResult)")
        
        print("🎯 SDK Version: \(api.srfidGetSdkVersion())")
      } else {
        print("❌ Failed to create RFID SDK API instance")
      }
    }
    
    // Clean up resources when module is destroyed
    OnDestroy {
      // Terminate all communication sessions
      for readerId in self.connectedReaders {
        _ = self.rfidSdkApi?.srfidTerminateCommunicationSession(Int32(readerId))
      }
      self.connectedReaders.removeAll()
      self.rfidSdkApi = nil
    }

    // Defines event names that the module can send to JavaScript.
    Events("onChange")
    
    // Check if SDK is initialized
    Function("isSDKInitialized") {
      return self.rfidSdkApi != nil
    }
    
    // Get SDK version
    Function("getSDKVersion") {
      return self.rfidSdkApi?.srfidGetSdkVersion() ?? "SDK not initialized"
    }
    
    // Check if required permissions are granted (iOS handles Bluetooth permissions automatically)
    Function("hasRequiredPermissions") {
      return true // iOS handles Bluetooth permissions through Info.plist
    }
    
    // Request required permissions (iOS doesn't need explicit permission requests for RFID)
    AsyncFunction("requestPermissions") { (promise: Promise) in
      promise.resolve(true) // iOS automatically requests permissions when needed
    }
    
    // Get available Readers/readers
    AsyncFunction("getAvailableDevices") { (promise: Promise) in
      self.getAvailableDevices(promise: promise)
    }
    
    // Connect to a specific scanner/reader
    AsyncFunction("connectToDevice") { (scannerId: Int, promise: Promise) in
      self.connectToReader(readerId: scannerId, promise: promise)
    }
    
    // Disconnect from a specific scanner/reader
    AsyncFunction("disconnectFromDevice") { (scannerId: Int, promise: Promise) in
      self.disconnectFromReader(readerId: scannerId, promise: promise)
    }
    
    // Check if connected to a specific scanner/reader
    Function("isConnectedToScanner") { (scannerId: Int) in
      return self.connectedReaders.contains(scannerId)
    }
    
    // Get list of connected Readers/readers
    Function("getConnectedDevices") {
      return Array(self.connectedReaders)
    }
    
    // Trigger a scan on a connected reader (single scan)
    AsyncFunction("triggerScan") { (scannerId: Int, promise: Promise) in
      self.triggerScan(readerId: scannerId, promise: promise)
    }
    
    // Start RFID inventory operation
    AsyncFunction("startRfidInventory") { (scannerId: Int, promise: Promise) in
      self.startRfidInventory(readerId: scannerId, promise: promise)
    }
    
    // Stop RFID inventory operation
    AsyncFunction("stopRfidInventory") { (scannerId: Int, promise: Promise) in
      self.stopRfidInventory(readerId: scannerId, promise: promise)
    }
    
    // Read RFID tag data
    AsyncFunction("readRfidTag") { (scannerId: Int, tagId: String, promise: Promise) in
      self.readRfidTag(readerId: scannerId, tagId: tagId, promise: promise)
    }
    
    // Write RFID tag data
    AsyncFunction("writeRfidTag") { (scannerId: Int, tagId: String, data: String, promise: Promise) in
      self.writeRfidTag(readerId: scannerId, tagId: tagId, data: data, promise: promise)
    }

    // Defines a JavaScript function that always returns a Promise and whose native code
    // is by default dispatched on the different thread than the JavaScript runtime runs on.
    AsyncFunction("setValueAsync") { (value: String) in
      // Send an event to JavaScript.
      self.sendEvent("onChange", [
        "value": value
      ])
    }

    // Enables the module to be used as a native view. Definition components that are accepted as part of the
    // view definition: Prop, Events.
    View(ExpoZebraRfidView.self) {
      // Defines a setter for the `url` prop.
      Prop("url") { (view: ExpoZebraRfidView, url: URL) in
        if view.webView.url != url {
          view.webView.load(URLRequest(url: url))
        }
      }

      Events("onLoad")
    }
  }
  
  // MARK: - Private Methods
  
  private func getAvailableDevices(promise: Promise) {
    guard let rfidSdkApi = self.rfidSdkApi else {
      print("❌ RFID SDK API is nil")
      promise.resolve([])
      return
    }
    
    print("🔍 Starting reader detection...")
    
    // Enable available readers detection
    let enableResult = rfidSdkApi.srfidEnableAvailableReadersDetection(true)
    print("📡 Enable readers detection result: \(enableResult)")
    
    // Get available readers list
    var readersList: NSMutableArray?
    let result = rfidSdkApi.srfidgetAvailableDevicesList(&readersList)
    print("📋 Get readers list result: \(result)")
    print("📋 Readers list: \(readersList?.description ?? "nil")")
    
    if result == SRFID_RESULT_SUCCESS && readersList != nil && readersList!.count > 0 {
      self.availableReadersList = readersList!
      print("✅ Found \(readersList!.count) readers")
      
      // Convert to JavaScript-friendly format
      var readersArray: [[String: Any]] = []
      
      for case let readerInfo as srfidReaderInfo in readersList! {
        let readerDict: [String: Any] = [
          "scannerId": Int(readerInfo.getReaderID()),
          "scannerName": readerInfo.getReaderName() ?? "Unknown Reader",
          "isActive": readerInfo.isActive(),
          "isAvailable": true,
          "connectionType": self.getConnectionTypeString(Int(readerInfo.getConnectionType()))
        ]
        print("📱 Found reader: \(readerDict)")
        readersArray.append(readerDict)
      }
      
      promise.resolve(readersArray)
    } else {
      print("❌ No readers found or error occurred. Result: \(result)")
      
      // Check if we're running in simulator and provide mock data
      #if targetEnvironment(simulator)
      print("🤖 Running in simulator - providing mock scanner data")
      let mockReaders: [[String: Any]] = [
        [
          "scannerId": 1001,
          "scannerName": "Mock RFID Scanner (Simulator)",
          "isActive": false,
          "isAvailable": true,
          "connectionType": "BLUETOOTH_LE"
        ],
        [
          "scannerId": 1002, 
          "scannerName": "Mock Barcode Scanner (Simulator)",
          "isActive": false,
          "isAvailable": true,
          "connectionType": "MFI"
        ]
      ]
      promise.resolve(mockReaders)
      #else
      promise.resolve([])
      #endif
    }
  }
  
  private func connectToReader(readerId: Int, promise: Promise) {
    guard let rfidSdkApi = self.rfidSdkApi else {
      promise.resolve(false)
      return
    }
    
    // Check if this is a mock scanner in simulator
    #if targetEnvironment(simulator)
    if readerId >= 1000 {
      print("🤖 Simulator: Mock connecting to scanner \(readerId)")
      self.connectedReaders.insert(readerId)
      promise.resolve(true)
      return
    }
    #endif
    
    let result = rfidSdkApi.srfidEstablishCommunicationSession(Int32(readerId))
    
    if result == SRFID_RESULT_SUCCESS {
      self.connectedReaders.insert(readerId)
      promise.resolve(true)
    } else {
      promise.resolve(false)
    }
  }
  
  private func disconnectFromReader(readerId: Int, promise: Promise) {
    guard let rfidSdkApi = self.rfidSdkApi else {
      promise.resolve(false)
      return
    }
    
    // Check if this is a mock scanner in simulator
    #if targetEnvironment(simulator)
    if readerId >= 1000 {
      print("🤖 Simulator: Mock disconnecting from scanner \(readerId)")
      self.connectedReaders.remove(readerId)
      promise.resolve(true)
      return
    }
    #endif
    
    let result = rfidSdkApi.srfidTerminateCommunicationSession(Int32(readerId))
    
    if result == SRFID_RESULT_SUCCESS {
      self.connectedReaders.remove(readerId)
      promise.resolve(true)
    } else {
      promise.resolve(false)
    }
  }
  
  private func getConnectionTypeString(_ connectionType: Int) -> String {
    switch connectionType {
    case SRFID_CONNTYPE_MFI:
      return "MFI"
    case SRFID_CONNTYPE_BTLE:
      return "BLUETOOTH_LE"
    default:
      return "UNKNOWN"
    }
  }
  
  private func triggerScan(readerId: Int, promise: Promise) {
    guard self.rfidSdkApi != nil else {
      promise.resolve(false)
      return
    }
    
    guard connectedReaders.contains(readerId) else {
      promise.resolve(false)
      return
    }
    
    // Check if this is a mock scanner in simulator
    #if targetEnvironment(simulator)
    if readerId >= 1000 {
      print("🤖 Simulator: Mock triggering scan on \(readerId)")
      promise.resolve(true)
      return
    }
    #endif
    
    // Placeholder implementation
    promise.resolve(true)
  }
  
  private func startRfidInventory(readerId: Int, promise: Promise) {
    guard self.rfidSdkApi != nil else {
      promise.resolve(false)
      return
    }
    
    guard connectedReaders.contains(readerId) else {
      promise.resolve(false)
      return
    }
    
    // Check if this is a mock scanner in simulator
    #if targetEnvironment(simulator)
    if readerId >= 1000 {
      print("🤖 Simulator: Mock starting RFID inventory on \(readerId)")
      promise.resolve(true)
      return
    }
    #endif
    
    // Placeholder implementation - in a real implementation, you would use the appropriate RFID SDK methods
    promise.resolve(true)
  }
  
  private func stopRfidInventory(readerId: Int, promise: Promise) {
    guard self.rfidSdkApi != nil else {
      promise.resolve(false)
      return
    }
    
    guard connectedReaders.contains(readerId) else {
      promise.resolve(false)
      return
    }
    
    // Check if this is a mock scanner in simulator
    #if targetEnvironment(simulator)
    if readerId >= 1000 {
      print("🤖 Simulator: Mock stopping RFID inventory on \(readerId)")
      promise.resolve(true)
      return
    }
    #endif
    
    // Placeholder implementation
    promise.resolve(true)
  }
  
  private func readRfidTag(readerId: Int, tagId: String, promise: Promise) {
    guard self.rfidSdkApi != nil else {
      promise.resolve(nil)
      return
    }
    
    guard connectedReaders.contains(readerId) else {
      promise.resolve(nil)
      return
    }
    
    // Check if this is a mock scanner in simulator
    #if targetEnvironment(simulator)
    if readerId >= 1000 {
      print("🤖 Simulator: Mock reading RFID tag \(tagId) on \(readerId)")
      promise.resolve("Mock tag data for \(tagId) from scanner \(readerId)")
      return
    }
    #endif
    
    // Read RFID tag data - placeholder implementation
    // In a real implementation, this would use the actual RFID SDK methods
    promise.resolve("Simulated tag data for \(tagId)")
  }
  
  private func writeRfidTag(readerId: Int, tagId: String, data: String, promise: Promise) {
    guard self.rfidSdkApi != nil else {
      promise.resolve(false)
      return
    }
    
    guard connectedReaders.contains(readerId) else {
      promise.resolve(false)
      return
    }
    
    // Check if this is a mock scanner in simulator
    #if targetEnvironment(simulator)
    if readerId >= 1000 {
      print("🤖 Simulator: Mock writing RFID tag \(tagId) with data '\(data)' on \(readerId)")
      promise.resolve(true)
      return
    }
    #endif
    
    // Write RFID tag data - placeholder implementation
    // In a real implementation, this would use the actual RFID SDK methods
    promise.resolve(true)
  }
}
