import 'package:flutter_module/features/wifi/domain/entities/wifi_network.dart';


abstract class WifiViewState {}

class WifiInitial extends WifiViewState {}
class WifiLoading extends WifiViewState {}
class WifiLoaded extends WifiViewState {
  final List<WifiNetwork> networks;
  WifiLoaded(this.networks);
}
class WifiError extends WifiViewState {
  final String message;
  WifiError(this.message);
}
class WifiConnectionStatusState extends WifiViewState {
  final WifiConnectionStatus status;
  WifiConnectionStatusState(this.status);
}
