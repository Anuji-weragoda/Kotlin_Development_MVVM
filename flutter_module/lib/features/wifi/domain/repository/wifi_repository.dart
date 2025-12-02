import '../entities/wifi_network.dart';

abstract class WifiRepository {
  Future<bool> isWifiEnabled();
  Future<void> enableWifi();
  Future<void> disableWifi();
  Future<bool> hasPermissions();
  Stream<List<WifiNetwork>> scanNetworks();
  Future<WifiConnectionStatus> connectToNetwork({
    required String ssid,
    required String password,
  });
  Future<WifiNetwork?> getCurrentNetwork();
  Future<void> saveNetwork({
    required String ssid,
    required String password,
    bool autoConnect,
  });
  Future<List<SavedWifiNetwork>> getSavedNetworks();
  Future<void> removeNetwork(String ssid);
  Future<int> getSignalStrength(int level);
  Stream<WifiConnectionStatus> get connectionStatusStream;
}