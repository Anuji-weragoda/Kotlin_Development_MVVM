import 'dart:convert';
import 'dart:async';
import 'package:http/http.dart' as http;
import '../config/api_gateway.dart';

class ApiService {
  static String? _authToken;

  static const String baseUrl = String.fromEnvironment(
    'AUTH_BASE_URL',
    defaultValue: 'http://10.0.2.2:8081',
  );

  static const String leaveBaseUrl = String.fromEnvironment(
    'LEAVE_BASE_URL',
    defaultValue: '',
  );

  static String effectiveLeaveBase() {
    if (generatedApiGatewayUrl.isNotEmpty) return generatedApiGatewayUrl;
    if (leaveBaseUrl.isNotEmpty) return leaveBaseUrl;
    final msg = 'No leave service URL configured. Ensure lib/config/api_gateway.dart contains the deployed ApiUrl or pass --dart-define=LEAVE_BASE_URL=<url> when running.';
    // best-effort log
    try {
      print(msg);
    } catch (_) {}
    throw Exception(msg);
  }

  /// Set authentication token from host (Android/iOS)
  static void setAuthToken(String token) {
    _authToken = token;
  }

  /// Get stored authentication token
  static String? getAuthToken() => _authToken;

  /// Helper to get current user's ID token (host-provided)
  static Future<String> _getIdToken() async {
    if (_authToken != null && _authToken!.isNotEmpty) {
      return _authToken!;
    }
    throw Exception('No authentication token available. Call ApiService.setAuthToken(token) from the host app.');
  }

  /// Build authorization headers using host-provided token
  static Future<Map<String, String>> _authHeaders() async {
    final token = await _getIdToken();
    return {
      'Authorization': 'Bearer $token',
      'Content-Type': 'application/json',
    };
  }

  /// Generic GET request returning a Map
  static Future<Map<String, dynamic>> _get(String url) async {
    final headers = await _authHeaders();
    final response = await http.get(Uri.parse(url), headers: headers).timeout(const Duration(seconds: 10));

    if (response.statusCode == 200) {
      return json.decode(response.body) as Map<String, dynamic>;
    } else {
      throw Exception('GET $url failed: ${response.statusCode} - ${response.body}');
    }
  }

  /// Generic GET request returning a List
  static Future<List<dynamic>> _getList(String url) async {
    final headers = await _authHeaders();
    final response = await http.get(Uri.parse(url), headers: headers).timeout(const Duration(seconds: 10));

    if (response.statusCode == 200) {
      return json.decode(response.body) as List<dynamic>;
    } else {
      throw Exception('GET (list) $url failed: ${response.statusCode} - ${response.body}');
    }
  }

  /// Generic POST request helper
  static Future<Map<String, dynamic>> _post(String url, Map<String, dynamic> body, {int timeoutSec = 10}) async {
    final headers = await _authHeaders();
    final response = await http.post(
      Uri.parse(url),
      headers: headers,
      body: json.encode(body),
    ).timeout(Duration(seconds: timeoutSec));

    if (response.statusCode == 200 || response.statusCode == 201) {
      return json.decode(response.body) as Map<String, dynamic>;
    } else {
      throw Exception('POST $url failed: ${response.statusCode} - ${response.body}');
    }
  }

  /// Generic PATCH request helper
  static Future<Map<String, dynamic>> _patch(String url, Map<String, dynamic> body, {int timeoutSec = 10}) async {
    final headers = await _authHeaders();
    final response = await http.patch(
      Uri.parse(url),
      headers: headers,
      body: json.encode(body),
    ).timeout(Duration(seconds: timeoutSec));

    if (response.statusCode == 200 || response.statusCode == 204) {
      return response.body.isNotEmpty ? json.decode(response.body) as Map<String, dynamic> : {};
    } else {
      throw Exception('PATCH $url failed: ${response.statusCode} - ${response.body}');
    }
  }

  /// Get user profile
  static Future<Map<String, dynamic>> getUserProfile() async {
    return _get('$baseUrl/api/v1/me');
  }

  /// Get the current user's attendance records (defensive)
  static Future<List<dynamic>> getMyAttendance({int limit = 100}) async {
    try {
      final url = '${effectiveLeaveBase()}/api/attendance/my?limit=$limit';
      // best-effort log
      try {
        print('API Request: getMyAttendance -> $url');
      } catch (_) {}
      return await _getList(url);
    } catch (e) {
      // Defensive: return empty list on any failure and log
      try {
        print('getMyAttendance error: $e');
      } catch (_) {}
      return <dynamic>[];
    }
  }

  /// Update user profile
  static Future<Map<String, dynamic>> updateUserProfile(Map<String, dynamic> updates) async {
    return _patch('$baseUrl/api/v1/me', updates);
  }

  /// Logout
  static Future<void> logoutUser() async {
    try {
      await _post('$baseUrl/api/v1/auth/logout', {});
    } catch (e) {
      try { print('Error logging logout: $e'); } catch (_) {}
      rethrow;
    }
  }

  /// Clock in
  static Future<Map<String, dynamic>> clockIn({Map<String, dynamic>? body}) async {
    final idToken = await _getIdToken();
    String? userSub;
    try {
      final parts = idToken.split('.');
      if (parts.length >= 2) {
        var payload = parts[1];
        final mod = payload.length % 4;
        if (mod != 0) payload += '=' * (4 - mod);
        final decoded = utf8.decode(base64Url.decode(payload));
        final map = json.decode(decoded) as Map<String, dynamic>;
        userSub = map['sub']?.toString();
      }
    } catch (_) {}

    final sendBody = Map<String, dynamic>.from(body ?? {});
    if (userSub != null && (sendBody['user_id'] == null || sendBody['user_id'].toString().isEmpty)) {
      sendBody['user_id'] = userSub;
    }
    return _post('${effectiveLeaveBase()}/api/attendance/clock-in', sendBody);
  }

  /// Clock out
  static Future<Map<String, dynamic>> clockOut({Map<String, dynamic>? body}) async {
    final idToken = await _getIdToken();
    String? userSub;
    try {
      final parts = idToken.split('.');
      if (parts.length >= 2) {
        var payload = parts[1];
        final mod = payload.length % 4;
        if (mod != 0) payload += '=' * (4 - mod);
        final decoded = utf8.decode(base64Url.decode(payload));
        final map = json.decode(decoded) as Map<String, dynamic>;
        userSub = map['sub']?.toString();
      }
    } catch (_) {}

    final sendBody = Map<String, dynamic>.from(body ?? {});
    if (userSub != null && (sendBody['user_id'] == null || sendBody['user_id'].toString().isEmpty)) {
      sendBody['user_id'] = userSub;
    }
    return _post('${effectiveLeaveBase()}/api/attendance/clock-out', sendBody);
  }

  /// Password reset (host must handle auth flows)
  static Future<void> requestPasswordReset(String email) async {
    throw Exception('Password reset not implemented in hostless module.');
  }
}
