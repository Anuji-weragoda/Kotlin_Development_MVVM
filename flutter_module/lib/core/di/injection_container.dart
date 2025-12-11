import 'package:get_it/get_it.dart';
import '../../features/dashboard/data/datasources/dashboard_remote_datasource.dart';
import '../../features/dashboard/data/repositories/dashboard_repository_impl.dart';
import '../../features/dashboard/domain/repositories/dashboard_repository.dart';
import '../../features/dashboard/presentation/cubit/dashboard_cubit.dart';
import '../../features/bluetooth/data/datasources/bluetooth_datasource.dart';
import '../../features/bluetooth/data/repositories/bluetooth_repository_impl.dart';
import '../../features/bluetooth/domain/repositories/bluetooth_repository.dart';
import '../../features/bluetooth/presentation/cubit/bluetooth_cubit.dart';
import '../../features/wifi/data/datasources/wifi_datasource.dart';
import '../../features/wifi/data/repository/wifi_repository_impl.dart';
import '../../features/wifi/domain/repository/wifi_repository.dart';
import '../../features/wifi/presentation/cubit/wifi_cubit.dart';
import 'package:flutter/foundation.dart';


import '../../features/payment/presentation/cubit/payment_cubit.dart';
import '../../../services/adyen_payment_service.dart';

final sl = GetIt.instance;

Future<void> init() async {

  sl.registerLazySingleton(() => AdyenPaymentService());

  sl.registerFactory(() => DashboardCubit(sl()));


  sl.registerLazySingleton<DashboardRepository>(
        () => DashboardRepositoryImpl(sl()),
  );


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

  sl.registerLazySingleton<BluetoothDataSource>(
    () => BluetoothDataSourceImpl(),
  );

  // WiFi feature registrations
  sl.registerFactory(() => WifiCubit(repository: sl<WifiRepository>()));

  sl.registerLazySingleton<WifiRepository>(() => WifiRepositoryImpl(dataSource: sl<WifiDataSource>()));

  sl.registerLazySingleton<WifiDataSource>(() => WifiDataSourceImpl());



  sl.registerFactory(() => PaymentCubit(sl()));
}
