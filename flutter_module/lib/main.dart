// lib/main.dart

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'core/di/injection_container.dart' as di;
import 'features/dashboard/presentation/cubit/dashboard_cubit.dart';
import 'features/dashboard/presentation/pages/dashboard_page.dart';

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
    return MaterialApp(
      title: 'Your App Name',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primarySwatch: Colors.blue,
        useMaterial3: true,
      ),
      // Wrap your app with BlocProvider at the top level
      // This makes the DashboardCubit available throughout the app
      home: BlocProvider(
        create: (context) => di.sl<DashboardCubit>(),
        child: const DashboardPage(),
      ),
    );
  }
}

// Alternative: If you have multiple screens that need the cubit
class MyAppWithMultiProviders extends StatelessWidget {
  const MyAppWithMultiProviders({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider<DashboardCubit>(
          create: (context) => di.sl<DashboardCubit>(),
        ),
        // Add more BLoC providers as you refactor other features
        // BlocProvider<AuthCubit>(
        //   create: (context) => di.sl<AuthCubit>(),
        // ),
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