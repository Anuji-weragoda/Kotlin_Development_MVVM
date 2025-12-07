import 'package:get_it/get_it.dart';
import '../../features/dashboard/data/datasources/dashboard_remote_datasource.dart';
import '../../features/dashboard/data/repositories/dashboard_repository_impl.dart';
import '../../features/dashboard/domain/repositories/dashboard_repository.dart';
import '../../features/dashboard/presentation/cubit/dashboard_cubit.dart';


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


  sl.registerFactory(() => PaymentCubit(sl()));
}
