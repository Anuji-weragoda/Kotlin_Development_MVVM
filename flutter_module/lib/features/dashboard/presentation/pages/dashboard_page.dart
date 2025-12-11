import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../widgets/dashboard_header.dart';
import '../widgets/action_card.dart';
import '../widgets/stat_card.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../payment/presentation/pages/payment_page.dart';
import '../../../profile/presentation/pages/profile_page.dart';

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
  String? displayName;
  String? fullName;
  String? phoneNumber;
  String? preferredLanguage;
  bool? mfaEnabled;
  bool? emailVerified;
  String? role;
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
            displayName = args['displayName'] as String?;
            fullName = args['fullName'] as String?;
            phoneNumber = args['phoneNumber'] as String?;
            preferredLanguage = args['preferredLanguage'] as String?;
            mfaEnabled = args['mfaEnabled'] as bool?;
            emailVerified = args['emailVerified'] as bool?;
            role = args['role'] as String?;
            isLoading = false;
            errorMessage = null;
          });
          debugPrint('Received user session: $userEmail, displayName: $displayName');
        }
      }
      return null;
    });
  }

  String _getDisplayName() {
    // Use displayName from backend if available
    if (displayName != null && displayName!.isNotEmpty) {
      return displayName!;
    }

    // Fall back to fullName if available
    if (fullName != null && fullName!.isNotEmpty) {
      return fullName!;
    }

    // Fall back to email username
    if (userEmail == null) return 'Guest';
    final name = userEmail!.split('@')[0];
    return name
        .split('.')
        .map((s) => s.isNotEmpty ? s[0].toUpperCase() + s.substring(1) : s)
        .join(' ');
  }

  Future<void> _handleSignOut() async {
    setState(() => isSigningOut = true);

    try {
      // Call Android native logout method
      await platform.invokeMethod('logout');
      debugPrint('Logout successful');
    } catch (e) {
      debugPrint('Error during logout: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Logout failed: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => isSigningOut = false);
      }
    }
  }

  void _navigateToBluetooth() {
    Navigator.of(context).pushNamed('/bluetooth');

  }

  void _navigateToWifi() {
    Navigator.of(context).pushNamed('/wifi');

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
              // Header
              DashboardHeader(
                displayName: _getDisplayName(),
                isSigningOut: isSigningOut,
                onSignOut: _handleSignOut,
              ),

              const SizedBox(height: 24),

              // Connectivity Section
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 20),
                child: Text(
                  'Connectivity',
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
                child: Row(
                  children: [
                    Expanded(
                      child: _buildConnectivityCard(
                        icon: Icons.bluetooth_rounded,
                        title: 'Bluetooth',
                        description: 'Manage BLE devices',
                        gradient: const LinearGradient(
                          colors: [Color(0xFF3B82F6), Color(0xFF2563EB)],
                        ),
                        onTap: _navigateToBluetooth,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _buildConnectivityCard(
                        icon: Icons.wifi_rounded,
                        title: 'Wi-Fi',
                        description: 'Network settings',
                        gradient: const LinearGradient(
                          colors: [Color(0xFF10B981), Color(0xFF059669)],
                        ),
                        onTap: _navigateToWifi,
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
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => ProfilePage(
                              userEmail: userEmail,
                              userId: userId,
                              token: token,
                              displayName: displayName,
                              fullName: fullName,
                              phoneNumber: phoneNumber,
                              preferredLanguage: preferredLanguage,
                              mfaEnabled: mfaEnabled,
                              emailVerified: emailVerified,
                              role: role,
                            ),
                          ),
                        );
                      },
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

  Widget _buildConnectivityCard({
    required IconData icon,
    required String title,
    required String description,
    required Gradient gradient,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(20),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            gradient: gradient,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withAlpha(20),
                blurRadius: 12,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.white.withAlpha(51),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, color: Colors.white, size: 28),
              ),
              const SizedBox(height: 12),
              Text(
                title,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                description,
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.white.withAlpha(204),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    super.dispose();
  }
}