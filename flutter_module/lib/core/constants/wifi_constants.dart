class WifiConstants {
  // Channel name
  static const String channelName = 'com.example.androidapp/wifi';

  // Method names
  static const String methodIsWifiEnabled = 'isWifiEnabled';
  static const String methodEnableWifi = 'enableWifi';
  static const String methodDisableWifi = 'disableWifi';
  static const String methodHasPermissions = 'hasWifiPermissions';
  static const String methodScanNetworks = 'scanNetworks';
  static const String methodConnectToNetwork = 'connectToNetwork';
  static const String methodGetCurrentNetwork = 'getCurrentNetwork';
  static const String methodSaveNetwork = 'saveNetwork';
  static const String methodGetSavedNetworks = 'getSavedNetworks';
  static const String methodRemoveNetwork = 'removeNetwork';
  static const String methodGetSignalStrength = 'getSignalStrength';

  // Callback method names
  static const String callbackOnNetworksFound = 'onNetworksFound';
  static const String callbackOnConnectionStatusChanged = 'onConnectionStatusChanged';

  // Defaults
  static const int defaultScanDuration = 15000; // ms
}

