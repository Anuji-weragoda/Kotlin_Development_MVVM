import 'dart:typed_data';
import '../../domain/entities/bluetooth_device.dart';
import '../../domain/repositories/bluetooth_repository.dart';
import '../datasources/bluetooth_datasource.dart';
import '../models/bluetooth_device_model.dart';

class BluetoothRepositoryImpl implements BluetoothRepository {
  final BluetoothDataSource dataSource;

  BluetoothRepositoryImpl({required this.dataSource});

  @override
  Future<bool> isBluetoothEnabled() async {
    try {
      return await dataSource.isBluetoothEnabled();
    } catch (e) {
      throw Exception('Failed to check Bluetooth status: $e');
    }
  }

  @override
  Future<void> enableBluetooth() async {
    try {
      await dataSource.enableBluetooth();
    } catch (e) {
      throw Exception('Failed to enable Bluetooth: $e');
    }
  }

  @override
  Future<void> disableBluetooth() async {
    try {
      await dataSource.disableBluetooth();
    } catch (e) {
      throw Exception('Failed to disable Bluetooth: $e');
    }
  }

  @override
  Future<bool> hasPermissions() async {
    try {
      return await dataSource.hasPermissions();
    } catch (e) {
      throw Exception('Failed to check permissions: $e');
    }
  }

  @override
  Stream<List<BluetoothDevice>> startScan({int duration = 10000}) async* {
    try {
      await for (final devices in dataSource.startScan(duration: duration)) {
        // If dataSource returns models, convert to entity
        yield devices.map((e) => e is BluetoothDeviceModel ? e.toEntity() : e).toList();
      }
    } catch (e) {
      throw Exception('Failed to start scan: $e');
    }
  }

  @override
  Future<void> stopScan() async {
    try {
      await dataSource.stopScan();
    } catch (e) {
      throw Exception('Failed to stop scan: $e');
    }
  }

  @override
  Stream<ConnectionStatus> connectToDevice(String address) async* {
    try {
      await for (final status in dataSource.connectToDevice(address)) {
        yield ConnectionStatus(
          state: status.state,
          device: status.device, // <-- remove .toEntity()
          message: status.message,
        );
      }
    } catch (e) {
      throw Exception('Failed to connect to device: $e');
    }
  }

  @override
  Future<void> disconnect() async {
    try {
      await dataSource.disconnect();
    } catch (e) {
      throw Exception('Failed to disconnect: $e');
    }
  }

  @override
  Future<bool> writeData({
    required String serviceUuid,
    required String characteristicUuid,
    required Uint8List data,
  }) async {
    try {
      return await dataSource.writeData(
        serviceUuid: serviceUuid,
        characteristicUuid: characteristicUuid,
        data: data,
      );
    } catch (e) {
      throw Exception('Failed to write data: $e');
    }
  }

  @override
  Future<bool> readData({
    required String serviceUuid,
    required String characteristicUuid,
  }) async {
    try {
      return await dataSource.readData(
        serviceUuid: serviceUuid,
        characteristicUuid: characteristicUuid,
      );
    } catch (e) {
      throw Exception('Failed to read data: $e');
    }
  }

  @override
  Future<List<BluetoothDevice>> getPairedDevices() async {
    try {
      final devices = await dataSource.getPairedDevices();
      return devices.map((e) => e is BluetoothDeviceModel ? e.toEntity() : e).toList();
    } catch (e) {
      throw Exception('Failed to get paired devices: $e');
    }
  }

  @override
  Stream<ConnectionStatus> get connectionStatusStream =>
      dataSource.connectionStatusStream.map(
            (status) => ConnectionStatus(
          state: status.state,
          device: status.device, // <-- remove .toEntity()
          message: status.message,
        ),
      );
}
