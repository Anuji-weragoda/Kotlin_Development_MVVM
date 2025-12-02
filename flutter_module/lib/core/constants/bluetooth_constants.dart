class BluetoothConstants {
  // Channel name
  static const String channelName = 'com.example.androidapp/bluetooth';

  // Method names
  static const String methodIsBluetoothEnabled = 'isBluetoothEnabled';
  static const String methodEnableBluetooth = 'enableBluetooth';
  static const String methodDisableBluetooth = 'disableBluetooth';
  static const String methodHasPermissions = 'hasPermissions';
  static const String methodStartScan = 'startScan';
  static const String methodStopScan = 'stopScan';
  static const String methodConnectToDevice = 'connectToDevice';
  static const String methodDisconnect = 'disconnect';
  static const String methodWriteData = 'writeData';
  static const String methodReadData = 'readData';
  static const String methodGetPairedDevices = 'getPairedDevices';

  // Callback method names
  static const String callbackOnDevicesFound = 'onDevicesFound';
  static const String callbackOnConnectionStatusChanged = 'onConnectionStatusChanged';

  // Scan settings
  static const int defaultScanDuration = 10000; // 10 seconds
  static const int maxScanDuration = 30000; // 30 seconds

  // Common BLE Service UUIDs
  static const String heartRateServiceUuid = '0000180d-0000-1000-8000-00805f9b34fb';
  static const String batteryServiceUuid = '0000180f-0000-1000-8000-00805f9b34fb';
  static const String deviceInfoServiceUuid = '0000180a-0000-1000-8000-00805f9b34fb';

  // Timeouts
  static const Duration connectionTimeout = Duration(seconds: 30);
  static const Duration dataTransferTimeout = Duration(seconds: 10);
}