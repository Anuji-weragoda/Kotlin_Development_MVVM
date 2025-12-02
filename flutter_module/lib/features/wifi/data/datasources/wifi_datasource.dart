import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter_module/core/constants/wifi_constants.dart';
import '../models/wifi_network_model.dart';

abstract class WifiDataSource {
  Future<bool> isWifiEnabled();
  Future<void> enableWifi();
  Future<void> disableWifi();
  Future<bool> hasPermissions();
  Stream<List<WifiNetworkModel>> scanNetworks();
  Future<WifiConnectionStatusModel> connectToNetwork({
    required String ssid,
    required String password,
  });
  Future<WifiNetworkModel?> getCurrentNetwork();
  Future<void> saveNetwork({
    required String ssid,
    required String password,
    bool autoConnect,
  });
  Future<List<SavedWifiNetworkModel>> getSavedNetworks();
  Future<void> removeNetwork(String ssid);
  Future<int> getSignalStrength(int level);
  Stream<WifiConnectionStatusModel> get connectionStatusStream;
}

class WifiDataSourceImpl implements WifiDataSource {
  final MethodChannel _channel;
  final StreamController<List<WifiNetworkModel>> _networksController =
  StreamController.broadcast();
  final StreamController<WifiConnectionStatusModel> _connectionController =
  StreamController.broadcast();

  WifiDataSourceImpl({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel(WifiConstants.channelName) {
    _setupCallbacks();
  }

  void _setupCallbacks() {
    _channel.setMethodCallHandler((call) async {
      switch (call.method) {
        case WifiConstants.callbackOnNetworksFound:
          final List<dynamic> networksList = call.arguments;
          final networks = networksList
              .map((e) =>
              WifiNetworkModel.fromJson(Map<String, dynamic>.from(e)))
              .toList();
          _networksController.add(networks);
          break;

        case WifiConstants.callbackOnConnectionStatusChanged:
          final Map<String, dynamic> statusMap =
          Map<String, dynamic>.from(call.arguments);
          final status = WifiConnectionStatusModel.fromJson(statusMap);
          _connectionController.add(status);
          break;
      }
    });
  }

  @override
  Future<bool> isWifiEnabled() async {
    try {
      final result = await _channel
          .invokeMethod<bool>(WifiConstants.methodIsWifiEnabled);
      return result ?? false;
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<void> enableWifi() async {
    try {
      await _channel.invokeMethod(WifiConstants.methodEnableWifi);
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<void> disableWifi() async {
    try {
      await _channel.invokeMethod(WifiConstants.methodDisableWifi);
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<bool> hasPermissions() async {
    try {
      final result = await _channel
          .invokeMethod<bool>(WifiConstants.methodHasPermissions);
      return result ?? false;
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Stream<List<WifiNetworkModel>> scanNetworks() {
    _channel.invokeMethod(WifiConstants.methodScanNetworks);
    return _networksController.stream;
  }

  @override
  Future<WifiConnectionStatusModel> connectToNetwork({
    required String ssid,
    required String password,
  }) async {
    try {
      final Map<dynamic, dynamic>? result =
      await _channel.invokeMethod<Map<dynamic, dynamic>>(
        WifiConstants.methodConnectToNetwork,
        {
          'ssid': ssid,
          'password': password,
        },
      );

      if (result != null) {
        return WifiConnectionStatusModel.fromJson(
            Map<String, dynamic>.from(result));
      }

      throw Exception('Connection failed: No response from platform');
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<WifiNetworkModel?> getCurrentNetwork() async {
    try {
      final Map<dynamic, dynamic>? result =
      await _channel.invokeMethod<Map<dynamic, dynamic>>(
        WifiConstants.methodGetCurrentNetwork,
      );

      if (result != null) {
        return WifiNetworkModel.fromJson(Map<String, dynamic>.from(result));
      }
      return null;
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<void> saveNetwork({
    required String ssid,
    required String password,
    bool autoConnect = true,
  }) async {
    try {
      await _channel.invokeMethod(
        WifiConstants.methodSaveNetwork,
        {
          'ssid': ssid,
          'password': password,
          'autoConnect': autoConnect,
        },
      );
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<List<SavedWifiNetworkModel>> getSavedNetworks() async {
    try {
      final List<dynamic>? result =
      await _channel.invokeMethod<List<dynamic>>(
        WifiConstants.methodGetSavedNetworks,
      );

      return result
          ?.map((e) => SavedWifiNetworkModel.fromJson(
          Map<String, dynamic>.from(e)))
          .toList() ??
          [];
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<void> removeNetwork(String ssid) async {
    try {
      await _channel.invokeMethod(
        WifiConstants.methodRemoveNetwork,
        {'ssid': ssid},
      );
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Future<int> getSignalStrength(int level) async {
    try {
      final result = await _channel.invokeMethod<int>(
        WifiConstants.methodGetSignalStrength,
        {'level': level},
      );
      return result ?? 0;
    } on PlatformException catch (e) {
      throw _handleException(e);
    }
  }

  @override
  Stream<WifiConnectionStatusModel> get connectionStatusStream =>
      _connectionController.stream;

  Exception _handleException(PlatformException e) {
    switch (e.code) {
      case 'INVALID_ARGUMENT':
        return Exception('Invalid argument: ${e.message}');
      case 'SCAN_ERROR':
        return Exception('WiFi scan failed: ${e.message}');
      case 'CONNECTION_ERROR':
        return Exception('Connection failed: ${e.message}');
      default:
        return Exception('WiFi error: ${e.message}');
    }
  }

  void dispose() {
    _networksController.close();
    _connectionController.close();
  }
}
