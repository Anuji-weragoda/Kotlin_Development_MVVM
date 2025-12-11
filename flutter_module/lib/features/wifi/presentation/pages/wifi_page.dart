import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_module/features/wifi/presentation/cubit/wifi_cubit.dart';
import 'package:flutter_module/features/wifi/presentation/cubit/wifi_state.dart';

class WifiPage extends StatelessWidget {
  const WifiPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('WiFi')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: ElevatedButton(
              onPressed: () => context.read<WifiCubit>().scan(),
              child: const Text('Scan'),
            ),
          ),
          Expanded(
            child: BlocBuilder<WifiCubit, WifiViewState>(
              builder: (context, state) {
                if (state is WifiLoading) return const Center(child: CircularProgressIndicator());
                if (state is WifiLoaded) {
                  return ListView.builder(
                    itemCount: state.networks.length,
                    itemBuilder: (context, index) {
                      final n = state.networks[index];
                      return ListTile(
                        title: Text(n.ssid),
                        subtitle: Text('${n.bssid} • RSSI: ${n.level}'),
                        trailing: n.isSecured ? const Icon(Icons.lock) : null,
                      );
                    },
                  );
                }
                if (state is WifiError) return Center(child: Text('Error: ${state.message}'));
                return const Center(child: Text('Press Scan to discover WiFi networks'));
              },
            ),
          )
        ],
      ),
    );
  }
}
