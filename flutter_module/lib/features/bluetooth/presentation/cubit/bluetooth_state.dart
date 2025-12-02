part of 'bluetooth_cubit.dart';

abstract class BluetoothState extends Equatable {
  const BluetoothState();

  @override
  List<Object?> get props => [];
}

class BluetoothInitial extends BluetoothState {}

class BluetoothLoading extends BluetoothState {}

class BluetoothStatusChecked extends BluetoothState {
  final bool isEnabled;

  const BluetoothStatusChecked(this.isEnabled);

  @override
  List<Object?> get props => [isEnabled];
}

class PermissionsChecked extends BluetoothState {
  final bool hasPermissions;

  const PermissionsChecked(this.hasPermissions);

  @override
  List<Object?> get props => [hasPermissions];
}

class ScanningDevices extends BluetoothState {}

class DevicesScanned extends BluetoothState {
  final List<BluetoothDevice> devices;

  const DevicesScanned(this.devices);

  @override
  List<Object?> get props => [devices];
}

class ScanStopped extends BluetoothState {}

class ConnectingToDevice extends BluetoothState {}

class DeviceConnected extends BluetoothState {
  final BluetoothDevice device;

  const DeviceConnected(this.device);

  @override
  List<Object?> get props => [device];
}

class Disconnected extends BluetoothState {}

class DataWritten extends BluetoothState {
  final bool success;

  const DataWritten(this.success);

  @override
  List<Object?> get props => [success];
}

class DataRead extends BluetoothState {
  final bool success;

  const DataRead(this.success);

  @override
  List<Object?> get props => [success];
}

class PairedDevicesLoaded extends BluetoothState {
  final List<BluetoothDevice> devices;

  const PairedDevicesLoaded(this.devices);

  @override
  List<Object?> get props => [devices];
}

class ConnectionStatusChangedState extends BluetoothState {
  final ConnectionStatus status;

  const ConnectionStatusChangedState(this.status);

  @override
  List<Object?> get props => [status];
}

class BluetoothError extends BluetoothState {
  final String message;

  const BluetoothError(this.message);

  @override
  List<Object?> get props => [message];
}
