import '../../domain/entities/wifi_network.dart';
import 'package:flutter/foundation.dart';
import '../../domain/repository/wifi_repository.dart';
import '../datasources/wifi_datasource.dart';
import '../models/wifi_network_model.dart';

class WifiRepositoryImpl implements WifiRepository {
  final WifiDataSource dataSource;

  WifiRepositoryImpl({required this.dataSource});

  @override
  Future<bool> isWifiEnabled() async {
    try {
      return await dataSource.isWifiEnabled();
    } catch (e) {
      throw Exception('Failed to check WiFi status: $e');
    }
  }

  @override
  Future<void> enableWifi() async {
    try {
      await dataSource.enableWifi();
    } catch (e) {
      throw Exception('Failed to enable WiFi: $e');
    }
  }

  @override
  Future<void> disableWifi() async {
    try {
      await dataSource.disableWifi();
    } catch (e) {
      throw Exception('Failed to disable WiFi: $e');
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
  Stream<List<WifiNetwork>> scanNetworks() async* {
    // In debug mode (e.g. emulator) emit a short mock list so the UI can be tested
    if (kDebugMode) {
      await Future.delayed(const Duration(milliseconds: 300));
      yield [
        const WifiNetwork(
          ssid: 'MockWiFi-1',
          bssid: '00:11:22:33:44:55',
          capabilities: '[WPA2-PSK-CCMP][ESS]',
          level: -45,
          frequency: 2412,
          isSecured: true,
          isSaved: false,
          isConnected: false,
        ),
        const WifiNetwork(
          ssid: 'HomeNetwork',
          bssid: '66:77:88:99:AA:BB',
          capabilities: '[WPA2-PSK-CCMP][ESS]',
          level: -62,
          frequency: 2417,
          isSecured: true,
          isSaved: true,
          isConnected: false,
        ),
        const WifiNetwork(
          ssid: 'Guest',
          bssid: 'CC:DD:EE:FF:00:11',
          capabilities: '[OPEN][ESS]',
          level: -78,
          frequency: 2462,
          isSecured: false,
          isSaved: false,
          isConnected: false,
        ),
      ];
      // Continue streaming native results afterwards (if any)
    }

    await for (final models in dataSource.scanNetworks()) {
      yield models.map((m) => m.toEntity()).toList();
    }
  }

  @override
  Future<WifiConnectionStatus> connectToNetwork({required String ssid, required String password}) async {
    try {
      final model = await dataSource.connectToNetwork(ssid: ssid, password: password);
      return model.toEntity();
    } catch (e) {
      throw Exception('Failed to connect: $e');
    }
  }

  @override
  Future<WifiNetwork?> getCurrentNetwork() async {
    try {
      final model = await dataSource.getCurrentNetwork();
      return model?.toEntity();
    } catch (e) {
      throw Exception('Failed to get current network: $e');
    }
  }

  @override
  Future<void> saveNetwork({required String ssid, required String password, bool autoConnect = true}) async {
    try {
      await dataSource.saveNetwork(ssid: ssid, password: password, autoConnect: autoConnect);
    } catch (e) {
      throw Exception('Failed to save network: $e');
    }
  }

  @override
  Future<List<SavedWifiNetwork>> getSavedNetworks() async {
    try {
      final models = await dataSource.getSavedNetworks();
      return models.map((m) => m.toEntity()).toList();
    } catch (e) {
      throw Exception('Failed to get saved networks: $e');
    }
  }

  @override
  Future<void> removeNetwork(String ssid) async {
    try {
      await dataSource.removeNetwork(ssid);
    } catch (e) {
      throw Exception('Failed to remove network: $e');
    }
  }

  @override
  Future<int> getSignalStrength(int level) async {
    try {
      return await dataSource.getSignalStrength(level);
    } catch (e) {
      throw Exception('Failed to get signal strength: $e');
    }
  }

  @override
  Stream<WifiConnectionStatus> get connectionStatusStream => dataSource.connectionStatusStream.map((m) => m.toEntity());
}