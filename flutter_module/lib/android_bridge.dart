import 'package:flutter/services.dart';

class UserSessionManager {
  static const platform = MethodChannel('com.example.flutter/dashboard');
  Map<String, dynamic>? userSession;

  Future<void> init() async {
    try {
      final session = await platform.invokeMethod<Map>('getUserSession');
      if (session != null) {
        userSession = Map<String, dynamic>.from(session);
        print('User session: $userSession');
      }
    } catch (e) {
      print('Failed to get user session: $e');
    }

    // Listen for session updates from Android
    platform.setMethodCallHandler((call) async {
      if (call.method == 'updateUserSession') {
        userSession = Map<String, dynamic>.from(call.arguments);
        print('User session updated: $userSession');
      }
    });
  }
}
