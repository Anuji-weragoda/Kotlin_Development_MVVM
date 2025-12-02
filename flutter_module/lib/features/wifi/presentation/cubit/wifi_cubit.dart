import 'dart:async';
import 'package:bloc/bloc.dart';
import 'package:flutter_module/features/wifi/domain/entities/wifi_network.dart';
import 'package:flutter_module/features/wifi/domain/repository/wifi_repository.dart';
import 'wifi_state.dart';

class WifiCubit extends Cubit<WifiViewState> {
  final WifiRepository repository;

  StreamSubscription<List<WifiNetwork>>? _scanSub;
  StreamSubscription<WifiConnectionStatus>? _statusSub;

  WifiCubit({required this.repository}) : super(WifiInitial()) {
    _listenStatus();
  }

  void _listenStatus() {
    _statusSub = repository.connectionStatusStream.listen((status) {
      emit(WifiConnectionStatusState(status));
    });
  }

  Future<void> scan() async {
    emit(WifiLoading());
    await _scanSub?.cancel();
    _scanSub = repository.scanNetworks().listen((networks) {
      emit(WifiLoaded(networks));
    }, onError: (e) {
      emit(WifiError(e.toString()));
    });
  }

  Future<void> stopScan() async {
    await _scanSub?.cancel();
    emit(WifiInitial());
  }

  Future<void> connect(String ssid, String password) async {
    emit(WifiLoading());
    try {
      final status = await repository.connectToNetwork(ssid: ssid, password: password);
      emit(WifiConnectionStatusState(status));
    } catch (e) {
      emit(WifiError(e.toString()));
    }
  }

  @override
  Future<void> close() {
    _scanSub?.cancel();
    _statusSub?.cancel();
    return super.close();
  }
}
