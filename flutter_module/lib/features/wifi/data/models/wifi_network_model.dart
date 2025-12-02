import 'package:flutter_module/features/wifi/domain/entities/wifi_network.dart';

class WifiNetworkModel extends WifiNetwork {
  const WifiNetworkModel({
    required super.ssid,
    required super.bssid,
    required super.capabilities,
    required super.level,
    required super.frequency,
    super.isSecured,
    super.isSaved,
    super.isConnected,
  });

  factory WifiNetworkModel.fromJson(Map<String, dynamic> json) {
    return WifiNetworkModel(
      ssid: json['ssid'] as String,
      bssid: json['bssid'] as String,
      capabilities: json['capabilities'] as String,
      level: json['level'] as int,
      frequency: json['frequency'] as int,
      isSecured: json['isSecured'] as bool? ?? true,
      isSaved: json['isSaved'] as bool? ?? false,
      isConnected: json['isConnected'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'ssid': ssid,
      'bssid': bssid,
      'capabilities': capabilities,
      'level': level,
      'frequency': frequency,
      'isSecured': isSecured,
      'isSaved': isSaved,
      'isConnected': isConnected,
    };
  }

  factory WifiNetworkModel.fromEntity(WifiNetwork entity) {
    return WifiNetworkModel(
      ssid: entity.ssid,
      bssid: entity.bssid,
      capabilities: entity.capabilities,
      level: entity.level,
      frequency: entity.frequency,
      isSecured: entity.isSecured,
      isSaved: entity.isSaved,
      isConnected: entity.isConnected,
    );
  }

  WifiNetwork toEntity() {
    return WifiNetwork(
      ssid: ssid,
      bssid: bssid,
      capabilities: capabilities,
      level: level,
      frequency: frequency,
      isSecured: isSecured,
      isSaved: isSaved,
      isConnected: isConnected,
    );
  }
}

class SavedWifiNetworkModel extends SavedWifiNetwork {
  const SavedWifiNetworkModel({
    required super.ssid,
    required super.password,
    super.priority,
    super.autoConnect,
    required super.savedTimestamp,
  });

  factory SavedWifiNetworkModel.fromJson(Map<String, dynamic> json) {
    return SavedWifiNetworkModel(
      ssid: json['ssid'] as String,
      password: json['password'] as String? ?? '',
      priority: json['priority'] as int? ?? 0,
      autoConnect: json['autoConnect'] as bool? ?? true,
      savedTimestamp: DateTime.fromMillisecondsSinceEpoch(
        json['savedTimestamp'] as int,
      ),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'ssid': ssid,
      'password': password,
      'priority': priority,
      'autoConnect': autoConnect,
      'savedTimestamp': savedTimestamp.millisecondsSinceEpoch,
    };
  }

  factory SavedWifiNetworkModel.fromEntity(SavedWifiNetwork entity) {
    return SavedWifiNetworkModel(
      ssid: entity.ssid,
      password: entity.password,
      priority: entity.priority,
      autoConnect: entity.autoConnect,
      savedTimestamp: entity.savedTimestamp,
    );
  }

  SavedWifiNetwork toEntity() {
    return SavedWifiNetwork(
      ssid: ssid,
      password: password,
      priority: priority,
      autoConnect: autoConnect,
      savedTimestamp: savedTimestamp,
    );
  }
}

class WifiConnectionStatusModel extends WifiConnectionStatus {
  const WifiConnectionStatusModel({
    required super.state,
    super.network,
    super.ipAddress,
    super.message,
  });

  factory WifiConnectionStatusModel.fromJson(Map<String, dynamic> json) {
    return WifiConnectionStatusModel(
      state: _parseWifiState(json['state'] as String),
      network: json['network'] != null
          ? WifiNetworkModel.fromJson(json['network'] as Map<String, dynamic>)
          : null,
      ipAddress: json['ipAddress'] as String?,
      message: json['message'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'state': state.name,
      'network': network != null
          ? WifiNetworkModel.fromEntity(network!).toJson()
          : null,
      'ipAddress': ipAddress,
      'message': message,
    };
  }

  WifiConnectionStatus toEntity() {
    return WifiConnectionStatus(
      state: state,
      network: network != null ? WifiNetworkModel.fromEntity(network!).toEntity() : null,
      ipAddress: ipAddress,
      message: message,
    );
  }

  static WifiState _parseWifiState(String state) {
    switch (state.toLowerCase()) {
      case 'connecting':
        return WifiState.connecting;
      case 'connected':
        return WifiState.connected;
      case 'disconnecting':
        return WifiState.disconnecting;
      case 'suspended':
        return WifiState.suspended;
      case 'error':
        return WifiState.error;
      case 'disabled':
        return WifiState.disabled;
      default:
        return WifiState.disconnected;
    }
  }
}