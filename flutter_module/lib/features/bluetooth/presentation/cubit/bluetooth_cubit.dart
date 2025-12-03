import 'dart:async';
import 'dart:typed_data';
import 'package:bloc/bloc.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter_module/features/bluetooth/domain/entities/bluetooth_device.dart';
import 'package:flutter_module/features/bluetooth/domain/repositories/bluetooth_repository.dart';

part 'bluetooth_state.dart';

class BluetoothCubit extends Cubit<BluetoothState> {
  final BluetoothRepository repository;

  StreamSubscription<List<BluetoothDevice>>? _scanSubscription;
  StreamSubscription? _connectionSubscription;
  StreamSubscription? _statusSubscription;

  BluetoothCubit({required this.repository}) : super(BluetoothInitial()) {
    _listenToConnectionStatus();
  }

  void _listenToConnectionStatus() {
    _statusSubscription =
        repository.connectionStatusStream.listen((status) {
          emit(ConnectionStatusChangedState(status));
        });
  }

  Future<void> checkBluetoothStatus() async {
    try {
      final enabled = await repository.isBluetoothEnabled();
      emit(BluetoothStatusChecked(enabled));
    } catch (e) {
      emit(BluetoothError(e.toString()));
    }
  }

  Future<void> enableBluetooth() async {
    emit(BluetoothLoading());
    try {
      await repository.enableBluetooth();
      emit(const BluetoothStatusChecked(true));
    } catch (e) {
      emit(BluetoothError(e.toString()));
    }
  }

  Future<void> disableBluetooth() async {
    emit(BluetoothLoading());
    try {
      await repository.disableBluetooth();
      emit(const BluetoothStatusChecked(false));
    } catch (e) {
      emit(BluetoothError(e.toString()));
    }
  }

  Future<void> checkPermissions() async {
    try {
      final hasPermissions = await repository.hasPermissions();
      emit(PermissionsChecked(hasPermissions));
    } catch (e) {
      emit(BluetoothError(e.toString()));
    }
  }

  Future<void> startScan({int duration = 10000}) async {
    emit(ScanningDevices());

    // Cancel any existing subscription first
    await _scanSubscription?.cancel();

    // Listen to repository scan stream and handle data/errors/done properly
    _scanSubscription = repository.startScan(duration: duration).listen(
      (devices) {
        // dataSource streams concrete device lists; emit directly
        emit(DevicesScanned(devices));
      },
      onError: (error) {
        // Surface errors to UI and stop scanning state
        emit(BluetoothError(error?.toString() ?? 'Unknown scan error'));
        emit(ScanStopped());
      },
      onDone: () {
        // Scanning finished (native timed out or completed)
        emit(ScanStopped());
      },
    );
  }

  Future<void> stopScan() async {
    await _scanSubscription?.cancel();
    _scanSubscription = null;
    await repository.stopScan();
    emit(ScanStopped());
  }

  Future<void> connectToDevice(String address) async {
    emit(ConnectingToDevice());
    await _connectionSubscription?.cancel();

    _connectionSubscription =
        repository.connectToDevice(address).listen((status) {
          emit(ConnectionStatusChangedState(status));
        });
  }

  Future<void> disconnect() async {
    await repository.disconnect();
    emit(Disconnected());
  }

  Future<void> writeData({
    required String serviceUuid,
    required String characteristicUuid,
    required Uint8List data,
  }) async {
    try {
      final success = await repository.writeData(
        serviceUuid: serviceUuid,
        characteristicUuid: characteristicUuid,
        data: data,
      );
      emit(DataWritten(success));
    } catch (e) {
      emit(BluetoothError(e.toString()));
    }
  }

  Future<void> readData({
    required String serviceUuid,
    required String characteristicUuid,
  }) async {
    try {
      final success = await repository.readData(serviceUuid: serviceUuid, characteristicUuid: characteristicUuid);
      emit(DataRead(success));
    } catch (e) {
      emit(BluetoothError(e.toString()));
    }
  }

  Future<void> getPairedDevices() async {
    try {
      final devices = await repository.getPairedDevices();
      emit(PairedDevicesLoaded(devices));
    } catch (e) {
      emit(BluetoothError(e.toString()));
    }
  }

  /// Run quick native diagnostics and return the map (or null on failure).
  Future<Map<String, dynamic>?> runDiagnostics() async {
    try {
      return await repository.diagnosticGetState();
    } catch (e) {
      return null;
    }
  }

  @override
  Future<void> close() {
    _scanSubscription?.cancel();
    _connectionSubscription?.cancel();
    _statusSubscription?.cancel();
    return super.close();
  }
}
