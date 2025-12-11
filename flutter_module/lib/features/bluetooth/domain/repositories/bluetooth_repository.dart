import 'dart:typed_data';
import '../entities/bluetooth_device.dart';

abstract class BluetoothRepository {
  Future<bool> isBluetoothEnabled();
  Future<void> enableBluetooth();
  Future<void> disableBluetooth();
  Future<bool> hasPermissions();

  Stream<List<BluetoothDevice>> startScan({int duration = 5});
  Future<void> stopScan();

  Stream<ConnectionStatus> connectToDevice(String address);
  Future<void> disconnect();

  Future<bool> writeData({
    required String serviceUuid,
    required String characteristicUuid,
    required Uint8List data,
  });

  // Returns true if a read request was successfully initiated; actual read bytes
  // are delivered via platform callbacks/connectionStatus stream in this project.
  Future<bool> readData({
    required String serviceUuid,
    required String characteristicUuid,
  });

  Future<List<BluetoothDevice>> getPairedDevices();
  Stream<ConnectionStatus> get connectionStatusStream;

  // Diagnostic helper: query native handler for scanner availability and missing permissions
  Future<Map<String, dynamic>?> diagnosticGetState();
}
