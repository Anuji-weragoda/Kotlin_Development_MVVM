import 'package:equatable/equatable.dart';

class BluetoothDevice extends Equatable {
  final String? name;
  final String address;
  final int rssi;
  final bool isConnected;
  final DeviceType deviceType;
  final List<String> services;

  const BluetoothDevice({
    this.name,
    required this.address,
    required this.rssi,
    this.isConnected = false,
    this.deviceType = DeviceType.unknown,
    this.services = const [],
  });

  @override
  List<Object?> get props => [name, address, rssi, isConnected, deviceType, services];
}

enum DeviceType { classic, ble, dual, unknown }

enum ConnectionState { disconnected, connecting, connected, disconnecting, error }

class ConnectionStatus extends Equatable {
  final ConnectionState state;
  final BluetoothDevice? device;
  final String? message;

  const ConnectionStatus({
    required this.state,
    this.device,
    this.message,
  });

  @override
  List<Object?> get props => [state, device, message];
}
