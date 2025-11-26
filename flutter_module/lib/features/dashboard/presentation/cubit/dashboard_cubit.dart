import 'package:flutter_bloc/flutter_bloc.dart';
import 'dashboard_state.dart';
import '../../domain/repositories/dashboard_repository.dart';
import '../../domain/entities/user_info.dart';

class DashboardCubit extends Cubit<DashboardState> {
  final DashboardRepository repository;

  DashboardCubit(this.repository) : super(DashboardInitial());

  /// Initialize dashboard with user data from Android login
  Future<void> initializeDashboard({
    required String email,
    required String userId,
    required String token,
  }) async {
    emit(DashboardLoading());
    try {
      // Store the token for API calls
      repository.setAuthToken(token);

      // Load dashboard data using the authenticated user
      final stats = await repository.getDashboardStats();

      // Create UserInfo from authenticated user data
      final userInfo = UserInfo(
        email: email,
        displayName: email.split('@')[0], // Extract name from email
      );

      emit(DashboardLoaded(userInfo: userInfo, stats: stats));
    } catch (e) {
      emit(DashboardError('Failed to initialize dashboard: $e'));
    }
  }

  Future<void> loadDashboard() async {
    emit(DashboardLoading());
    try {
      final userInfo = await repository.getUserInfo();
      final stats = await repository.getDashboardStats();
      emit(DashboardLoaded(userInfo: userInfo, stats: stats));
    } catch (e) {
      emit(DashboardError(e.toString()));
    }
  }

  Future<void> signOut() async {
    emit(DashboardSigningOut());
    try {
      await repository.signOut();
      emit(DashboardSignedOut());
    } catch (e) {
      emit(DashboardError(e.toString()));
    }
  }
}
