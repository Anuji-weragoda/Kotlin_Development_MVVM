import 'package:flutter/services.dart';

class UserSessionManager {
  static const platform = MethodChannel('com.example.flutter/dashboard');
  Map<String, dynamic>? userSession;

  void init() {
    // Listen for session updates from Android
    platform.setMethodCallHandler((call) async {
      if (call.method == 'updateUserSession') {
        userSession = Map<String, dynamic>.from(call.arguments);
        print('User session received from Android: $userSession');
      }
    });
  }
}
