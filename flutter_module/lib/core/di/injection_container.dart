// lib/core/di/injection_container.dart

import 'package:get_it/get_it.dart';
import '../../features/dashboard/data/datasources/dashboard_remote_datasource.dart';
import '../../features/dashboard/data/repositories/dashboard_repository_impl.dart';
import '../../features/dashboard/domain/repositories/dashboard_repository.dart';
import '../../features/dashboard/presentation/cubit/dashboard_cubit.dart';
import '../../features/bluetooth/data/datasources/bluetooth_datasource.dart';
import '../../features/bluetooth/data/repositories/bluetooth_repository_impl.dart';
import '../../features/bluetooth/domain/repositories/bluetooth_repository.dart';
import '../../features/bluetooth/presentation/cubit/bluetooth_cubit.dart';

final sl = GetIt.instance; // Service Locator

Future<void> init() async {
  //! Features - Dashboard
  // Cubit
  sl.registerFactory(
    () => DashboardCubit(sl()),
  );

  // Repository
  sl.registerLazySingleton<DashboardRepository>(
    () => DashboardRepositoryImpl(sl()),
  );

  // Data sources
  sl.registerLazySingleton<DashboardRemoteDataSource>(
    () => DashboardRemoteDataSourceImpl(),
  );

  sl.registerFactory(
    () => BluetoothCubit(repository: sl<BluetoothRepository>()),
  );

  // Repository
  sl.registerLazySingleton<BluetoothRepository>(
    () => BluetoothRepositoryImpl(
      dataSource: sl<BluetoothDataSource>(),
    ),
  );

  // Data Source
  sl.registerLazySingleton<BluetoothDataSource>(
    () => BluetoothDataSourceImpl(),
  );

  // Add more feature registrations here as you refactor them
  // Example:
  // //! Features - Auth
  // sl.registerFactory(() => AuthCubit(sl()));
  // sl.registerLazySingleton<AuthRepository>(() => AuthRepositoryImpl(sl()));
}