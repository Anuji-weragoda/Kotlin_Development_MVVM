import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_module/features/bluetooth/presentation/cubit/bluetooth_cubit.dart';
import 'package:flutter_module/features/bluetooth/domain/entities/bluetooth_device.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:flutter/foundation.dart';

class BluetoothPage extends StatefulWidget {
  const BluetoothPage({Key? key}) : super(key: key);

  @override
  State<BluetoothPage> createState() => _BluetoothPageState();
}

class _BluetoothPageState extends State<BluetoothPage> {
  @override
  void initState() {
    super.initState();
    final cubit = context.read<BluetoothCubit>();
    cubit.checkBluetoothStatus();
    cubit.checkPermissions();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Bluetooth Devices'),
        actions: [
          BlocBuilder<BluetoothCubit, BluetoothState>(
            builder: (context, state) {
              if (state is BluetoothStatusChecked) {
                return Switch(
                  value: state.isEnabled,
                  onChanged: (value) {
                    final cubit = context.read<BluetoothCubit>();
                    if (value) {
                      cubit.enableBluetooth();
                    } else {
                      cubit.disableBluetooth();
                    }
                  },
                );
              }
              return const SizedBox.shrink();
            },
          ),
        ],
      ),
      body: BlocConsumer<BluetoothCubit, BluetoothState>(
        listener: (context, state) {
          if (state is BluetoothError) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(state.message), backgroundColor: Colors.red),
            );
          } else if (state is DeviceConnected) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Connected to ${state.device.name ?? "Unknown"}'),
                backgroundColor: Colors.green,
              ),
            );
          }
        },
        builder: (context, state) {
          if (state is BluetoothLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          final showPermissionWarning =
              state is PermissionsChecked && !state.hasPermissions;

          return Column(
            children: [
              // PERMISSION BANNER
              if (showPermissionWarning)
                MaterialBanner(
                  content: const Text(
                    'Bluetooth permissions are missing. Please grant them.',
                  ),
                  actions: [
                    TextButton(
                      onPressed: () async {
                        final statuses = await [
                          Permission.bluetooth,
                          Permission.bluetoothScan,
                          Permission.bluetoothConnect,
                          Permission.locationWhenInUse,
                        ].request();

                        final anyGranted =
                        statuses.values.any((s) => s.isGranted);

                        if (anyGranted) {
                          // Update permissions state
                          await context.read<BluetoothCubit>().checkPermissions();
                          // Try scanning again
                          context.read<BluetoothCubit>().startScan();
                        } else {
                          if (!mounted) return;
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('Permissions not granted'),
                            ),
                          );
                        }
                      },
                      child: const Text('Grant Permissions'),
                    ),
                  ],
                ),

              _buildConnectionStatus(),

              _buildScanButton(),
              Expanded(child: _buildDeviceList(state)),
            ],
          );
        },
      ),
    );
  }

  Widget _buildConnectionStatus() {
    return BlocBuilder<BluetoothCubit, BluetoothState>(
      builder: (context, state) {
        if (state is DeviceConnected) {
          return Container(
            padding: const EdgeInsets.all(16),
            color: Colors.green.shade100,
            child: Row(
              children: [
                const Icon(Icons.bluetooth_connected, color: Colors.green),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Connected to ${state.device.name ?? "Unknown"}',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: () =>
                      context.read<BluetoothCubit>().disconnect(),
                ),
              ],
            ),
          );
        }
        return const SizedBox.shrink();
      },
    );
  }

  Widget _buildScanButton() {
    return BlocBuilder<BluetoothCubit, BluetoothState>(
      builder: (context, state) {
        final isScanning = state is ScanningDevices;

        return Padding(
          padding: const EdgeInsets.all(16),
          child: ElevatedButton.icon(
            onPressed:
            isScanning ? null : () => context.read<BluetoothCubit>().startScan(),
            icon: Icon(isScanning ? Icons.hourglass_empty : Icons.search),
            label: Text(isScanning ? 'Scanning...' : 'Scan for Devices'),
            style: ElevatedButton.styleFrom(
              minimumSize: const Size(double.infinity, 48),
            ),
          ),
        );
      },
    );
  }

  Widget _buildDeviceList(BluetoothState state) {
    if (state is DevicesScanned) {
      if (state.devices.isEmpty) {
        return const Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.bluetooth_searching, size: 64, color: Colors.grey),
              SizedBox(height: 16),
              Text('No devices found'),
              SizedBox(height: 8),
              Text(
                'Make sure Bluetooth is enabled on nearby devices',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey),
              ),
            ],
          ),
        );
      }

      return ListView.builder(
        itemCount: state.devices.length,
        itemBuilder: (_, index) =>
            _buildDeviceItem(state.devices[index]),
      );
    }

    return const Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.bluetooth, size: 64, color: Colors.grey),
          SizedBox(height: 16),
          Text('Tap "Scan for Devices" to begin'),
        ],
      ),
    );
  }

  Widget _buildDeviceItem(BluetoothDevice device) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: ListTile(
        leading: Icon(
          device.isConnected ? Icons.bluetooth_connected : Icons.bluetooth,
          color: device.isConnected ? Colors.green : Colors.blue,
        ),
        title: Text(device.name ?? 'Unknown'),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(device.address),
            Row(
              children: [
                Icon(Icons.signal_cellular_alt,
                    size: 16, color: _getSignalColor(device.rssi)),
                const SizedBox(width: 4),
                Text('RSSI: ${device.rssi} dBm'),
              ],
            ),
          ],
        ),
        trailing: device.isConnected
            ? TextButton(
          onPressed: () =>
              context.read<BluetoothCubit>().disconnect(),
          child: const Text('Disconnect'),
        )
            : ElevatedButton(
          onPressed: () => context
              .read<BluetoothCubit>()
              .connectToDevice(device.address),
          child: const Text('Connect'),
        ),
      ),
    );
  }

  Color _getSignalColor(int rssi) {
    if (rssi >= -50) return Colors.green;
    if (rssi >= -70) return Colors.orange;
    return Colors.red;
  }
}
