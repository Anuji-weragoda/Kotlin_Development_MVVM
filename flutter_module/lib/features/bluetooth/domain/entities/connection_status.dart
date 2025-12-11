import 'bluetooth_device.dart';

enum ConnectionState {
  connecting,
  connected,
  disconnecting,
  disconnected,
  error,
}

class ConnectionStatus {
  final ConnectionState state;
  final BluetoothDevice? device;
  final String? message;

  const ConnectionStatus({
    required this.state,
    this.device,
    this.message,
  });
}
