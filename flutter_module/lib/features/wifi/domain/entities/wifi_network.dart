class WifiNetwork {
  final String ssid;
  final String bssid;
  final String capabilities;
  final int level; // RSSI-like
  final int frequency;
  final bool isSecured;
  final bool isSaved;
  final bool isConnected;

  const WifiNetwork({
    required this.ssid,
    required this.bssid,
    this.capabilities = '',
    required this.level,
    required this.frequency,
    this.isSecured = true,
    this.isSaved = false,
    this.isConnected = false,
  });
}

class SavedWifiNetwork {
  final String ssid;
  final String password;
  final int priority;
  final bool autoConnect;
  final DateTime savedTimestamp;

  const SavedWifiNetwork({
    required this.ssid,
    required this.password,
    this.priority = 0,
    this.autoConnect = true,
    required this.savedTimestamp,
  });
}

enum WifiState { disconnected, connecting, connected, disconnecting, suspended, error, disabled }

class WifiConnectionStatus {
  final WifiState state;
  final WifiNetwork? network;
  final String? ipAddress;
  final String? message;

  const WifiConnectionStatus({
    required this.state,
    this.network,
    this.ipAddress,
    this.message,
  });
}
