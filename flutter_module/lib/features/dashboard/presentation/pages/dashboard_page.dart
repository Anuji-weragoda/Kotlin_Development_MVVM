import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../widgets/dashboard_header.dart';
import '../widgets/action_card.dart';
import '../widgets/stat_card.dart';
import '../../../../core/constants/app_colors.dart';

class DashboardPage extends StatefulWidget {
  const DashboardPage({super.key});

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  static const platform = MethodChannel('com.example.flutter/dashboard');

  String? userEmail;
  String? userId;
  String? token;
  bool isLoading = true;
  bool isSigningOut = false;
  String? errorMessage;

  @override
  void initState() {
    super.initState();
    platform.setMethodCallHandler((call) async {
      if (call.method == 'onUserSession') {
        final Map<dynamic, dynamic>? args = call.arguments as Map<dynamic, dynamic>?;
        if (args != null) {
          final email = args['email'] as String?;
          final userIdData = args['userId'] as String?;
          final tokenData = args['token'] as String?;

          debugPrint('Received onUserSession from Android: $email');

          setState(() {
            userEmail = email;
            userId = userIdData;
            token = tokenData;
            isLoading = false;
            errorMessage = null;
          });
          return null;
        }
      }
      return null;
    });

    _initializeDashboard();
  }

  Future<void> _initializeDashboard() async {
    await Future.delayed(const Duration(milliseconds: 1000));
    if (mounted) {
      _getUserSessionFromAndroid();
    }
  }

  Future<void> _getUserSessionFromAndroid() async {
    try {
      final result = await platform.invokeMethod<Map>('getUserSession');

      if (result != null) {
        final email = result['email'] as String?;
        final userIdData = result['userId'] as String?;
        final tokenData = result['token'] as String?;

        setState(() {
          userEmail = email;
          userId = userIdData;
          token = tokenData;
          isLoading = false;
        });

        debugPrint('✓ Dashboard initialized with real user: $email');
      } else {
        setState(() {
          errorMessage = "No user session found";
          isLoading = false;
        });
      }
    } on PlatformException catch (e) {
      debugPrint('PlatformException: ${e.code} - ${e.message}');

      if (e.code == "NO_SESSION" && !isLoading) {
        debugPrint('Session not ready, retrying in 500ms...');
        setState(() {
          errorMessage = "Session not ready, retrying...";
          isLoading = true;
        });

        await Future.delayed(const Duration(milliseconds: 500));
        if (mounted) {
          await _getUserSessionFromAndroid();
        }
      } else {
        setState(() {
          errorMessage = "Failed to get user session: ${e.message}";
          isLoading = false;
        });
      }
    } catch (e) {
      setState(() {
        errorMessage = "Error: $e";
        isLoading = false;
      });
      debugPrint('Exception: $e');
    }
  }

  String _getDisplayName() {
    if (userEmail == null) return 'Guest';
    final name = userEmail!.split('@')[0];
    return name.split('.').map((s) =>
      s.isNotEmpty ? s[0].toUpperCase() + s.substring(1) : s
    ).join(' ');
  }

  Future<void> _handleSignOut() async {
    setState(() => isSigningOut = true);

    // Simulate sign out process
    await Future.delayed(const Duration(seconds: 1));

    // Call your sign out method here
    // Navigator.of(context).pushReplacementNamed('/login');

    if (mounted) {
      setState(() => isSigningOut = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return Scaffold(
        backgroundColor: const Color(0xFFF8FAFC),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(
                color: AppColors.primaryDark,
              ),
              const SizedBox(height: 16),
              const Text(
                'Loading your dashboard...',
                style: TextStyle(
                  color: Color(0xFF64748B),
                  fontSize: 14,
                ),
              ),
            ],
          ),
        ),
      );
    }

    if (errorMessage != null) {
      return Scaffold(
        backgroundColor: const Color(0xFFF8FAFC),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(32),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Container(
                  padding: const EdgeInsets.all(24),
                  decoration: BoxDecoration(
                    color: Colors.red.withAlpha(26),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.error_outline_rounded,
                    size: 56,
                    color: Colors.red,
                  ),
                ),
                const SizedBox(height: 24),
                Text(
                  'Oops!',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: const Color(0xFF0F172A),
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  errorMessage!,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: Color(0xFF64748B),
                    fontSize: 15,
                  ),
                ),
                const SizedBox(height: 32),
                ElevatedButton(
                  onPressed: _getUserSessionFromAndroid,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primaryDark,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 32,
                      vertical: 16,
                    ),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                    elevation: 0,
                  ),
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.refresh_rounded, size: 20),
                      SizedBox(width: 8),
                      Text(
                        'Retry',
                        style: TextStyle(
                          fontWeight: FontWeight.w600,
                          fontSize: 15,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF8FAFC),
      body: SafeArea(
        child: SingleChildScrollView(
          physics: const BouncingScrollPhysics(),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header with greeting and logout
              DashboardHeader(
                displayName: _getDisplayName(),
                isSigningOut: isSigningOut,
                onSignOut: _handleSignOut,
              ),

              // Stats Cards
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Row(
                  children: [
                    Expanded(
                      child: StatCard(
                        icon: Icons.verified_user_rounded,
                        label: 'Account Status',
                        value: 'Active',
                        color: const Color(0xFF10B981),
                        gradient: const LinearGradient(
                          colors: [Color(0xFF10B981), Color(0xFF059669)],
                        ),
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: StatCard(
                        icon: Icons.access_time_rounded,
                        label: 'Member Since',
                        value: '2024',
                        color: const Color(0xFF6366F1),
                        gradient: AppColors.primaryGradient,
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 32),

              // Quick Actions Section
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 20),
                child: Text(
                  'Quick Actions',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF0F172A),
                    letterSpacing: -0.5,
                  ),
                ),
              ),

              const SizedBox(height: 16),

              // Action Cards
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Column(
                  children: [
                    ActionCard(
                      icon: Icons.account_circle_rounded,
                      title: 'View Profile',
                      description: 'Manage your account details',
                      gradient: const LinearGradient(
                        colors: [Color(0xFF6366F1), Color(0xFF4F46E5)],
                      ),
                      onTap: () {
                        debugPrint('View Profile tapped');
                      },
                    ),
                    const SizedBox(height: 12),
                    ActionCard(
                      icon: Icons.settings_rounded,
                      title: 'Settings',
                      description: 'Configure your preferences',
                      gradient: const LinearGradient(
                        colors: [Color(0xFF8B5CF6), Color(0xFF7C3AED)],
                      ),
                      onTap: () {
                        debugPrint('Settings tapped');
                      },
                    ),
                    const SizedBox(height: 12),
                    ActionCard(
                      icon: Icons.notification_important_rounded,
                      title: 'Notifications',
                      description: 'View your recent updates',
                      gradient: const LinearGradient(
                        colors: [Color(0xFFEC4899), Color(0xFFDB2777)],
                      ),
                      onTap: () {
                        debugPrint('Notifications tapped');
                      },
                    ),
                    const SizedBox(height: 12),
                    ActionCard(
                      icon: Icons.help_rounded,
                      title: 'Help & Support',
                      description: 'Get assistance anytime',
                      gradient: const LinearGradient(
                        colors: [Color(0xFF06B6D4), Color(0xFF0891B2)],
                      ),
                      onTap: () {
                        debugPrint('Help tapped');
                      },
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 32),

              // User Info Section (Debug info)
              if (userEmail != null) ...[
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(24),
                      border: Border.all(
                        color: const Color(0xFFF1F5F9),
                        width: 1,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withAlpha(10),
                          blurRadius: 12,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Row(
                          children: [
                            Icon(
                              Icons.info_outline_rounded,
                              color: Color(0xFF64748B),
                              size: 20,
                            ),
                            SizedBox(width: 8),
                            Text(
                              'Session Information',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF0F172A),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        _buildInfoRow('Email', userEmail!),
                        const Divider(height: 24),
                        _buildInfoRow(
                          'User ID',
                          userId ?? 'N/A',
                        ),
                        const Divider(height: 24),
                        _buildInfoRow(
                          'Token',
                          token != null && token!.length > 20
                              ? '${token!.substring(0, 20)}...'
                              : token ?? 'N/A',
                        ),
                      ],
                    ),
                  ),
                ),
              ],

              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 80,
          child: Text(
            label,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: Color(0xFF64748B),
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Text(
            value,
            style: const TextStyle(
              fontSize: 13,
              color: Color(0xFF0F172A),
            ),
          ),
        ),
      ],
    );
  }

  @override
  void dispose() {
    super.dispose();
  }
}