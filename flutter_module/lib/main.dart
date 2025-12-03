// lib/main.dart

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'core/di/injection_container.dart' as di;
import 'features/dashboard/presentation/cubit/dashboard_cubit.dart';
import 'features/dashboard/presentation/pages/dashboard_page.dart';
import 'features/payment/presentation/cubit/payment_cubit.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize dependency injection
  await di.init();

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider<DashboardCubit>(
          create: (context) => di.sl<DashboardCubit>(),
        ),
        BlocProvider<PaymentCubit>(
          create: (context) => di.sl<PaymentCubit>(),
        ),
        // Add more BLoC providers here as you refactor other features
      ],
      child: MaterialApp(
        title: 'Your App Name',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          primarySwatch: Colors.blue,
          useMaterial3: true,
        ),
        home: const DashboardPage(),
      ),
    );
  }
}