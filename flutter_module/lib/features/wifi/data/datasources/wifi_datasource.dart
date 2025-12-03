import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
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
  // Stream to receive permission updates from native side
  final StreamController<bool> _permissionController = StreamController<bool>.broadcast();
  bool _callbacksRegistered = false;

  WifiDataSourceImpl({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel(WifiConstants.channelName) {
    _setupCallbacks();
    // Debug: confirm handler registration
    debugPrint('WifiDataSourceImpl: MethodChannel initialized for ${WifiConstants.channelName}');
    // Notify native side that Dart client is ready to receive callbacks. The Android
    // WifiHandler waits for a 'clientReady' handshake before invoking callback methods
    // (it buffers networks until this is received). Sending this ensures callbacks are delivered.
    _notifyClientReady();
  }

  void _setupCallbacks() {
    if (_callbacksRegistered) return;
     _channel.setMethodCallHandler((call) async {
       debugPrint('WifiDataSourceImpl: received platform call: ${call.method} with args=${call.arguments}');
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
    _callbacksRegistered = true;
  }


  Future<void> _notifyClientReady() async {
    try {
      await _channel.invokeMethod('clientReady');
      debugPrint('WifiDataSourceImpl: sent clientReady handshake to native');
    } on PlatformException catch (e) {
      debugPrint('WifiDataSourceImpl: clientReady handshake failed: ${e.message}');
    } catch (t) {
      debugPrint('WifiDataSourceImpl: unexpected error sending clientReady: $t');
    }
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
    // Ensure callbacks are registered before we request a native scan
    if (!_callbacksRegistered) _setupCallbacks();
    debugPrint('WifiDataSourceImpl: invoking native scanNetworks');

    // Start the native scan asynchronously and log the response. We await the
    // clientReady handshake before requesting a scan to reduce the chance that
    // native events are emitted before Dart is ready to receive them.
    () async {
      try {
        await _notifyClientReady();
        final dynamic result = await _channel
            .invokeMethod<dynamic>(WifiConstants.methodScanNetworks)
            .timeout(const Duration(seconds: 10));
        debugPrint('WifiDataSourceImpl: startScan invoke result: $result');
      } on PlatformException catch (e) {
        debugPrint('WifiDataSourceImpl: startScan PlatformException: ${e.code} ${e.message}');
        try {
          _networksController.addError(_handleException(e));
        } catch (_) {}
      } on TimeoutException {
        debugPrint('WifiDataSourceImpl: startScan timed out');
      } catch (e, st) {
        debugPrint('WifiDataSourceImpl: startScan unexpected error: $e\n$st');
      }
    }();

    return _networksController.stream;
  }

  /// Diagnostic helper: ask the native side for internal handler state (useful
  /// to verify whether native received `clientReady`, whether scanning is active,
  /// or why no devices were emitted). This method is optional on the native side.
  Future<Map<String, dynamic>?> diagnosticGetState() async {
    try {
      final Map<dynamic, dynamic>? result =
          await _channel.invokeMethod<Map<dynamic, dynamic>>('diagnosticGetState');
      return result == null ? null : Map<String, dynamic>.from(result);
    } on PlatformException catch (e) {
      debugPrint('WifiDataSourceImpl: diagnosticGetState failed: ${e.message}');
      return null;
    }
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
    if (!_permissionController.isClosed) _permissionController.close();
  }
}
