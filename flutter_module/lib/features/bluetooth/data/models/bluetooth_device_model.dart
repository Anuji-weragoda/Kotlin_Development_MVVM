import 'dart:typed_data';
import '../../domain/entities/bluetooth_device.dart';

class BluetoothDeviceModel extends BluetoothDevice {
  const BluetoothDeviceModel({
    super.name,
    required super.address,
    required super.rssi,
    super.isConnected,
    super.deviceType,
    super.services,
  });

  factory BluetoothDeviceModel.fromJson(Map<String, dynamic> json) {
    return BluetoothDeviceModel(
      name: json['name'] as String?,
      address: json['address'] as String,
      rssi: json['rssi'] as int,
      isConnected: json['isConnected'] as bool? ?? false,
      deviceType: _parseDeviceType(json['deviceType'] as String?),
      services: (json['services'] as List<dynamic>?)
          ?.map((e) => e.toString())
          .toList() ??
          [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'address': address,
      'rssi': rssi,
      'isConnected': isConnected,
      'deviceType': deviceType.name,
      'services': services,
    };
  }

  factory BluetoothDeviceModel.fromEntity(BluetoothDevice entity) {
    return BluetoothDeviceModel(
      name: entity.name,
      address: entity.address,
      rssi: entity.rssi,
      isConnected: entity.isConnected,
      deviceType: entity.deviceType,
      services: entity.services,
    );
  }

  BluetoothDevice toEntity() {
    return BluetoothDevice(
      name: name,
      address: address,
      rssi: rssi,
      isConnected: isConnected,
      deviceType: deviceType,
      services: services,
    );
  }

  static DeviceType _parseDeviceType(String? type) {
    switch (type?.toLowerCase()) {
      case 'classic':
        return DeviceType.classic;
      case 'ble':
        return DeviceType.ble;
      case 'dual':
        return DeviceType.dual;
      default:
        return DeviceType.unknown;
    }
  }
}

class ConnectionStatusModel extends ConnectionStatus {
  const ConnectionStatusModel({
    required super.state,
    super.device,
    super.message,
  });

  factory ConnectionStatusModel.fromJson(Map<String, dynamic> json) {
    return ConnectionStatusModel(
      state: _parseConnectionState(json['state'] as String),
      device: json['device'] != null
          ? BluetoothDeviceModel.fromJson(
          Map<String, dynamic>.from(json['device']))
          : null,
      message: json['message'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'state': state.name,
      'device': device != null
          ? (device is BluetoothDeviceModel
          ? (device as BluetoothDeviceModel).toJson()
          : BluetoothDeviceModel.fromEntity(device!).toJson())
          : null,
      'message': message,
    };
  }

  static ConnectionState _parseConnectionState(String state) {
    switch (state.toLowerCase()) {
      case 'connecting':
        return ConnectionState.connecting;
      case 'connected':
        return ConnectionState.connected;
      case 'disconnecting':
        return ConnectionState.disconnecting;
      case 'error':
        return ConnectionState.error;
      default:
        return ConnectionState.disconnected;
    }
  }
}
