import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_module/core/constants/bluetooth_constants.dart';
import '../models/bluetooth_device_model.dart';
import '../../domain/entities/bluetooth_device.dart';

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

  // Diagnostic helper to query native state (scanner availability, missing permissions)
  Future<Map<String, dynamic>?> diagnosticGetState();
}

class BluetoothDataSourceImpl implements BluetoothDataSource {
  final MethodChannel _channel;
  // controllers are broadcast so multiple UI listeners can subscribe
  final StreamController<List<BluetoothDeviceModel>> _devicesController =
      StreamController<List<BluetoothDeviceModel>>.broadcast();
  final StreamController<ConnectionStatusModel> _connectionController =
      StreamController<ConnectionStatusModel>.broadcast();

  // Helper logging tag and timeout for platform calls
  final String _tag = 'BT_DS';
  final Duration _invokeTimeout = const Duration(seconds: 5);
  bool _isScanning = false;

  void _log(String msg) => debugPrint('$_tag ${DateTime.now().toIso8601String()} - $msg');

  Future<T?> _invokeWithTimeout<T>(String method, [dynamic arguments, Duration? timeout]) async {
    try {
      _log('invoke => $method, args=$arguments');
      final effectiveTimeout = timeout ?? _invokeTimeout;
      final res = await _channel.invokeMethod<T>(method, arguments).timeout(effectiveTimeout);
      _log('invoke <= $method, result=${res.runtimeType}');
      return res;
    } on TimeoutException catch (e) {
      _log('invoke TIMEOUT $method: $e');
      throw PlatformException(code: 'TIMEOUT', message: '$method timed out');
    } on PlatformException catch (e) {
      _log('invoke PlatformException $method: ${e.code} ${e.message}');
      throw e;
    } catch (e, st) {
      _log('invoke ERROR $method: $e\n$st');
      rethrow;
    }
  }

  BluetoothDataSourceImpl({
    MethodChannel? channel,
  }) : _channel = channel ?? const MethodChannel(BluetoothConstants.channelName) {
    _setupCallbacks();
  }

  void _setupCallbacks() {
    _channel.setMethodCallHandler((call) async {
      _log('method=${call.method}, arguments=${call.arguments}');
      try {
        switch (call.method) {
          case BluetoothConstants.callbackOnDevicesFound:
            final dynamic arg = call.arguments;
            if (arg is List) {
              final devices = (arg)
                  .map((e) => BluetoothDeviceModel.fromJson(Map<String, dynamic>.from(e)))
                  .toList();
              if (!_devicesController.isClosed) {
                _devicesController.add(devices);
              } else {
                _log('devices controller closed, skipping devices event');
              }
            } else {
              _log('unexpected devices payload: ${arg.runtimeType}');
            }
            break;

          case BluetoothConstants.callbackOnConnectionStatusChanged:
            final dynamic arg = call.arguments;
            if (arg is Map) {
              final status = ConnectionStatusModel.fromJson(Map<String, dynamic>.from(arg));
              if (!_connectionController.isClosed) {
                _connectionController.add(status);
              } else {
                _log('connection controller closed, skipping status event');
              }
            } else {
              _log('unexpected connection status payload: ${arg.runtimeType}');
            }
            break;
          default:
            _log('unhandled method from platform: ${call.method}');
        }
      } catch (e, st) {
        _log('exception handling platform call ${call.method}: $e\n$st');
        // Surface parsing errors to Dart side if appropriate
      }
    });
  }

  @override
  Future<bool> isBluetoothEnabled() async {
    try {
      final result = await _channel.invokeMethod<bool>(BluetoothConstants.methodIsBluetoothEnabled);
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
      final result = await _channel.invokeMethod<bool>(BluetoothConstants.methodHasPermissions);
      return result ?? false;
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Stream<List<BluetoothDeviceModel>> startScan({int duration = 10000}) {
    _isScanning = true;

    _channel
        .invokeMethod(BluetoothConstants.methodStartScan, {'duration': duration})
        .then((res) => _log('startScan invoke returned: ${res ?? 'null'}'))
        .catchError((e) {
      _log('startScan native error: $e');

      if (e is PlatformException && e.code == 'TIMEOUT') {
        _log('startScan timed out - suppressing stream error and continuing');
      } else {
        if (!_devicesController.isClosed) {
          _devicesController.addError(Exception('Native startScan failed: $e'));
        }
      }
    });

    try {
      Future.delayed(Duration(milliseconds: duration + 500), () async {
        if (_isScanning) {
          _log('auto-stop scan after duration');
          await stopScan().catchError((e) => _log('auto-stop error: $e'));
        }
      });
    } catch (e) {
      _log('scheduling auto-stop failed: $e');
    }

    return _devicesController.stream;
  }

  @override
  Future<void> stopScan() async {
    _isScanning = false;
    try {
      await _invokeWithTimeout(BluetoothConstants.methodStopScan);
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Stream<ConnectionStatusModel> connectToDevice(String address) {
    // fire-and-forget; native side will post connection updates via callback
    _invokeWithTimeout(BluetoothConstants.methodConnectToDevice, {'address': address}).catchError((e) {
      _log('connectToDevice native error: $e');
      if (!_connectionController.isClosed) {
        _connectionController.add(ConnectionStatusModel(
          state: ConnectionState.disconnected,
          message: 'Connection failed: $e',
        ));
      }
    });

    return _connectionController.stream;
  }

  @override
  Future<void> disconnect() async {
    try {
      await _invokeWithTimeout(BluetoothConstants.methodDisconnect);
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
      final List<dynamic>? result = await _invokeWithTimeout<List<dynamic>>(BluetoothConstants.methodGetPairedDevices);
      return result
              ?.map((e) => BluetoothDeviceModel.fromJson(Map<String, dynamic>.from(e)))
              .toList() ??
          [];
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Stream<ConnectionStatusModel> get connectionStatusStream =>
       _connectionController.stream;

  @override
  Future<Map<String, dynamic>?> diagnosticGetState() async {
    try {
      final res = await _invokeWithTimeout<Map<dynamic, dynamic>>('diagnosticGetState');
      if (res == null) return null;
      // Map<dynamic,dynamic> -> Map<String,dynamic>
      return Map<String, dynamic>.from(res);
    } catch (e) {
      _log('diagnosticGetState failed: $e');
      return null;
    }
  }

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
     _log('dispose called - stopping scan and closing controllers');
     _isScanning = false;

     _invokeWithTimeout(BluetoothConstants.methodStopScan).catchError((e) => _log('dispose stopScan error: $e'));
     if (!_devicesController.isClosed) {
       _devicesController.close();
     }
     if (!_connectionController.isClosed) {
       _connectionController.close();
     }
   }
 }
