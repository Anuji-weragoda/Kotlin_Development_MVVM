import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'core/di/injection_container.dart' as di;
import 'features/dashboard/presentation/cubit/dashboard_cubit.dart';
import 'features/dashboard/presentation/pages/dashboard_page.dart';
import 'features/bluetooth/presentation/cubit/bluetooth_cubit.dart';
import 'features/bluetooth/presentation/pages/bluetooth_page.dart';
import 'features/wifi/presentation/cubit/wifi_cubit.dart';
import 'features/wifi/presentation/pages/wifi_page.dart';
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
        BlocProvider<BluetoothCubit>(
          create: (context) => di.sl<BluetoothCubit>(),
        ),
        BlocProvider<WifiCubit>(
          create: (context) => di.sl<WifiCubit>(),
        ),
        // Add more BLoC providers as you refactor other features
        // BlocProvider<AuthCubit>(
        //   create: (context) => di.sl<AuthCubit>(),
        // ),
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
        // Define named routes
        initialRoute: '/',
        routes: {
          '/': (context) => const DashboardPage(),
          '/bluetooth': (context) => const BluetoothPage(),
          '/wifi': (context) => const WifiPage(),
        },
        // Alternative: Use onGenerateRoute for more control
        onGenerateRoute: _generateRoute,
      ),
    );
  }

  // Optional: Custom route generator with transitions
  static Route<dynamic>? _generateRoute(RouteSettings settings) {
    switch (settings.name) {
      case '/':
        return MaterialPageRoute(builder: (_) => const DashboardPage());

      case '/bluetooth':
        return _createRoute(const BluetoothPage());

      case '/wifi':
        return _createRoute(const WifiPage());

      default:
        return MaterialPageRoute(
          builder: (_) => Scaffold(
            body: Center(
              child: Text('No route defined for ${settings.name}'),
            ),
          ),
        );
    }
  }

  // Custom page transition
  static Route _createRoute(Widget page) {
    return PageRouteBuilder(
      pageBuilder: (context, animation, secondaryAnimation) => page,
      transitionsBuilder: (context, animation, secondaryAnimation, child) {
        const begin = Offset(1.0, 0.0);
        const end = Offset.zero;
        const curve = Curves.easeInOutCubic;

        var tween = Tween(begin: begin, end: end).chain(
          CurveTween(curve: curve),
        );

        return SlideTransition(
          position: animation.drive(tween),
          child: child,
        );
      },
      transitionDuration: const Duration(milliseconds: 300),
    );
  }
}