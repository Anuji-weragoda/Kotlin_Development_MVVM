import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../widgets/dashboard_header.dart';
import '../widgets/action_card.dart';
import '../widgets/stat_card.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../payment/presentation/pages/payment_page.dart'; // <-- import payment page

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

    // Listen for session updates from Android
    platform.setMethodCallHandler((call) async {
      if (call.method == 'updateUserSession') {
        final Map<dynamic, dynamic>? args =
        call.arguments as Map<dynamic, dynamic>?;
        if (args != null) {
          setState(() {
            userEmail = args['email'] as String?;
            userId = args['userId'] as String?;
            token = args['accessToken'] as String?;
            isLoading = false;
            errorMessage = null;
          });
          debugPrint('Received user session: $userEmail');
        }
      }
      return null;
    });
  }

  String _getDisplayName() {
    if (userEmail == null) return 'Guest';
    final name = userEmail!.split('@')[0];
    return name
        .split('.')
        .map((s) => s.isNotEmpty ? s[0].toUpperCase() + s.substring(1) : s)
        .join(' ');
  }

  Future<void> _handleSignOut() async {
    setState(() => isSigningOut = true);
    await Future.delayed(const Duration(seconds: 1));
    if (mounted) setState(() => isSigningOut = false);
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
  Widget build(BuildContext context) {
    if (isLoading) {
      return Scaffold(
        backgroundColor: const Color(0xFFF8FAFC),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (errorMessage != null) {
      return Scaffold(
        backgroundColor: const Color(0xFFF8FAFC),
        body: Center(child: Text(errorMessage!)),
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
              DashboardHeader(
                displayName: _getDisplayName(),
                isSigningOut: isSigningOut,
                onSignOut: _handleSignOut,
              ),

              // Stats Cards (unchanged)
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
                        value: '',
                        color: const Color(0xFF6366F1),
                        gradient: AppColors.primaryGradient,
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 32),

              // Quick Actions
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 20),
                child: Text(
                  'Quick Actions',
                  style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF0F172A),
                      letterSpacing: -0.5),
                ),
              ),
              const SizedBox(height: 16),
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
                      onTap: () => debugPrint('View Profile tapped'),
                    ),
                    const SizedBox(height: 12),
                    // **New Payment Action**
                    ActionCard(
                      icon: Icons.payment_rounded,
                      title: 'Make a Payment',
                      description: 'Pay using Adyen',
                      gradient: const LinearGradient(
                        colors: [Color(0xFF10B981), Color(0xFF059669)],
                      ),
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => const PaymentPage(),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }
}
