import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_module/core/constants/bluetooth_constants.dart';
import '../models/bluetooth_device_model.dart';

abstract class BluetoothDataSource {
  Future<bool> isBluetoothEnabled();
  Future<void> enableBluetooth();
  Future<void> disableBluetooth();
  Future<bool> hasPermissions();
  Stream<List<BluetoothDeviceModel>> startScan({int duration});
  Future<void> stopScan();
  Stream<ConnectionStatusModel> connectToDevice(String address);
  Future<void> disconnect();
  Future<bool> writeData({
    required String serviceUuid,
    required String characteristicUuid,
    required Uint8List data,
  });
  Future<bool> readData({
    required String serviceUuid,
    required String characteristicUuid,
  });
  Future<List<BluetoothDeviceModel>> getPairedDevices();
  Stream<ConnectionStatusModel> get connectionStatusStream;
}

class BluetoothDataSourceImpl implements BluetoothDataSource {
  final MethodChannel _channel;
  final StreamController<List<BluetoothDeviceModel>> _devicesController =
  StreamController.broadcast();
  final StreamController<ConnectionStatusModel> _connectionController =
  StreamController.broadcast();

  BluetoothDataSourceImpl({MethodChannel? channel})
      : _channel =
      channel ?? const MethodChannel(BluetoothConstants.channelName) {
    _setupCallbacks();
  }

  void _setupCallbacks() {
    _channel.setMethodCallHandler((call) async {
      // Debug: print incoming platform calls
      debugPrint('BluetoothDataSource: method=${call.method}, arguments=${call.arguments}');
      switch (call.method) {
        case BluetoothConstants.callbackOnDevicesFound:
          final List<dynamic> devicesList = call.arguments;
          final devices = devicesList
              .map((e) =>
              BluetoothDeviceModel.fromJson(Map<String, dynamic>.from(e)))
              .toList();
          _devicesController.add(devices);
          break;

        case BluetoothConstants.callbackOnConnectionStatusChanged:
          final Map<String, dynamic> statusMap =
          Map<String, dynamic>.from(call.arguments);
          final status = ConnectionStatusModel.fromJson(statusMap);
          _connectionController.add(status);
          break;
      }
    });
  }

  @override
  Future<bool> isBluetoothEnabled() async {
    try {
      final result = await _channel
          .invokeMethod<bool>(BluetoothConstants.methodIsBluetoothEnabled);
      return result ?? false;
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<void> enableBluetooth() async {
    try {
      await _channel.invokeMethod(BluetoothConstants.methodEnableBluetooth);
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<void> disableBluetooth() async {
    try {
      await _channel.invokeMethod(BluetoothConstants.methodDisableBluetooth);
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<bool> hasPermissions() async {
    try {
      final result =
      await _channel.invokeMethod<bool>(BluetoothConstants.methodHasPermissions);
      return result ?? false;
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Stream<List<BluetoothDeviceModel>> startScan({int duration = 10000}) {
    _channel.invokeMethod(
      BluetoothConstants.methodStartScan,
      {'duration': duration},
    );
    return _devicesController.stream;
  }

  @override
  Future<void> stopScan() async {
    try {
      await _channel.invokeMethod(BluetoothConstants.methodStopScan);
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Stream<ConnectionStatusModel> connectToDevice(String address) {
    _channel.invokeMethod(
      BluetoothConstants.methodConnectToDevice,
      {'address': address},
    );
    return _connectionController.stream;
  }

  @override
  Future<void> disconnect() async {
    try {
      await _channel.invokeMethod(BluetoothConstants.methodDisconnect);
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<bool> writeData({
    required String serviceUuid,
    required String characteristicUuid,
    required Uint8List data,
  }) async {
    try {
      final result = await _channel.invokeMethod<bool>(
        BluetoothConstants.methodWriteData,
        {
          'serviceUuid': serviceUuid,
          'characteristicUuid': characteristicUuid,
          'data': data,
        },
      );
      return result ?? false;
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<bool> readData({
    required String serviceUuid,
    required String characteristicUuid,
  }) async {
    try {
      final result = await _channel.invokeMethod<bool>(
        BluetoothConstants.methodReadData,
        {
          'serviceUuid': serviceUuid,
          'characteristicUuid': characteristicUuid,
        },
      );

      return result ?? false;
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<List<BluetoothDeviceModel>> getPairedDevices() async {
    try {
      final List<dynamic>? result = await _channel.invokeMethod<List<dynamic>>(
          BluetoothConstants.methodGetPairedDevices);
      return result
          ?.map((e) =>
          BluetoothDeviceModel.fromJson(Map<String, dynamic>.from(e)))
          .toList() ??
          [];
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Stream<ConnectionStatusModel> get connectionStatusStream =>
      _connectionController.stream;

  Exception _handleException(PlatformException e) {
    switch (e.code) {
      case 'INVALID_ARGUMENT':
        return Exception('Invalid argument: ${e.message}');
      case 'SCAN_ERROR':
        return Exception('Bluetooth scan failed: ${e.message}');
      case 'CONNECTION_ERROR':
        return Exception('Connection failed: ${e.message}');
      default:
        return Exception('Bluetooth error: ${e.message}');
    }
  }

  void dispose() {
    _devicesController.close();
    _connectionController.close();
  }
}
