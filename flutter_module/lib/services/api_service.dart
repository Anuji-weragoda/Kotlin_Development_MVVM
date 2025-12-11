import 'dart:convert';
import 'dart:async';
import 'package:http/http.dart' as http;
import '../config/api_gateway.dart';

class ApiService {
  // Static token from Android login
  static String? _authToken;

  static const String baseUrl = String.fromEnvironment('AUTH_BASE_URL', defaultValue: 'http://10.0.2.2:8081');
  static const String leaveBaseUrl = String.fromEnvironment('LEAVE_BASE_URL', defaultValue: '');

  static String effectiveLeaveBase() {
    if (generatedApiGatewayUrl.isNotEmpty) return generatedApiGatewayUrl;
    if (leaveBaseUrl.isNotEmpty) return leaveBaseUrl;
    final msg = 'No leave service URL configured. Ensure lib/config/api_gateway.dart contains the deployed ApiUrl or pass --dart-define=LEAVE_BASE_URL=<url> when running.';
    print(msg);
    throw Exception(msg);
  }

  /// Set authentication token from Android login
  static void setAuthToken(String token) {
    _authToken = token;
    print('✓ Auth token set from Android: {token.substring(0, 20)}...');
  }

  /// Get stored authentication token
  static String? getAuthToken() {
    return _authToken;
  }

  // Helper to get auth header
  static Map<String, String> _authHeaders() {
    if (_authToken == null) throw Exception('Auth token not set');
    return {
      'Authorization': 'Bearer $_authToken',
      'Content-Type': 'application/json',
    };
  }

  static Future<Map<String, dynamic>> getUserProfile() async {
    final response = await http.get(
      Uri.parse('$baseUrl/api/v1/me'),
      headers: _authHeaders(),
    ).timeout(const Duration(seconds: 10));
    if (response.statusCode == 200) {
      return json.decode(response.body);
    } else {
      throw Exception('Failed to load profile: ${response.statusCode} - ${response.body}');
    }
  }

  static Future<Map<String, dynamic>> updateUserProfile(Map<String, dynamic> updates) async {
    final response = await http.patch(
      Uri.parse('$baseUrl/api/v1/me'),
      headers: _authHeaders(),
      body: json.encode(updates),
    );
    if (response.statusCode == 200) {
      return json.decode(response.body);
    } else {
      throw Exception('Failed to update profile: ${response.statusCode}');
    }
  }

  static Future<bool> testConnection() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/actuator/health'),
      ).timeout(const Duration(seconds: 5));
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  static Future<List<dynamic>> getMyLeaves({int limit = 50}) async {
    final uri = Uri.parse('${effectiveLeaveBase()}/api/v1/leave/requests?limit=$limit&mine=true');
    final response = await http.get(uri, headers: _authHeaders()).timeout(const Duration(seconds: 10));
    if (response.statusCode == 200) {
      final body = json.decode(response.body);
      if (body is List) return body;
      return (body['items'] ?? body['data'] ?? body['requests'] ?? []) as List<dynamic>;
    } else {
      throw Exception('Failed to load leaves: ${response.statusCode} - ${response.body}');
    }
  }

  static Future<Map<String, dynamic>> createLeaveRequest(Map<String, dynamic> payload) async {
    final uri = Uri.parse('${effectiveLeaveBase()}/api/v1/leave/requests');
    final response = await http.post(uri, headers: _authHeaders(), body: json.encode(payload)).timeout(const Duration(seconds: 10));
    if (response.statusCode == 201 || response.statusCode == 200) {
      return json.decode(response.body) as Map<String, dynamic>;
    } else {
      throw Exception('Failed to create leave: ${response.statusCode} - ${response.body}');
    }
  }

  static Future<void> cancelLeaveRequest(dynamic id) async {
    final uri = Uri.parse('${effectiveLeaveBase()}/api/v1/leave/requests/$id');
    final response = await http.patch(uri,
        headers: _authHeaders(),
        body: json.encode({'action': 'cancel'})).timeout(const Duration(seconds: 10));
    if (response.statusCode == 200 || response.statusCode == 204) {
      return;
    } else {
      throw Exception('Failed to cancel leave: ${response.statusCode} - ${response.body}');
    }
  }

  static Future<List<dynamic>> getMyAttendance({int limit = 100}) async {
    final uri = Uri.parse('${effectiveLeaveBase()}/api/attendance?limit=$limit&mine=true');
    final response = await http.get(uri, headers: _authHeaders()).timeout(const Duration(seconds: 10));
    if (response.statusCode == 200) {
      final body = json.decode(response.body);
      if (body is List) return body;
      return (body['items'] ?? body['data'] ?? body['rows'] ?? []) as List<dynamic>;
    } else {
      throw Exception('Failed to load attendance: ${response.statusCode} - ${response.body}');
    }
  }

  static Future<double> getMyLeaveBalance() async {
    final uri = Uri.parse('${effectiveLeaveBase()}/api/v1/leave/balance');
    final response = await http.get(uri, headers: _authHeaders()).timeout(const Duration(seconds: 10));
    if (response.statusCode == 200) {
      final body = json.decode(response.body);
      if (body is num) return body.toDouble();
      if (body is Map) {
        final val = body['balance'] ?? body['available'] ?? body['remaining'] ?? body['leave_balance'];
        if (val is num) return val.toDouble();
        final parsed = double.tryParse(val?.toString() ?? '');
        if (parsed != null) return parsed;
      }
      throw Exception('Unexpected leave balance payload');
    } else {
      throw Exception('Failed to load leave balance: ${response.statusCode} - ${response.body}');
    }
  }

  static Future<Map<String, dynamic>> clockIn({Map<String, dynamic>? body}) async {
    final sendBody = Map<String, dynamic>.from(body ?? {});
    final uri = Uri.parse('${effectiveLeaveBase()}/api/attendance/clock-in');
    final response = await http.post(uri, headers: _authHeaders(), body: json.encode(sendBody)).timeout(const Duration(seconds: 10));
    if (response.statusCode == 201 || response.statusCode == 200) {
      return json.decode(response.body) as Map<String, dynamic>;
    } else {
      throw Exception('Failed to clock in: ${response.statusCode} - ${response.body}');
    }
  }

  static Future<Map<String, dynamic>> clockOut({Map<String, dynamic>? body}) async {
    final sendBody = Map<String, dynamic>.from(body ?? {});
    final uri = Uri.parse('${effectiveLeaveBase()}/api/attendance/clock-out');
    final response = await http.post(uri, headers: _authHeaders(), body: json.encode(sendBody)).timeout(const Duration(seconds: 10));
    if (response.statusCode == 201 || response.statusCode == 200) {
      return json.decode(response.body) as Map<String, dynamic>;
    } else {
      throw Exception('Failed to clock out: ${response.statusCode} - ${response.body}');
    }
  }

  static Future<void> logoutUser() async {
    try {
      // Call backend logout endpoint
      if (_authToken != null) {
        final response = await http.post(
          Uri.parse('$baseUrl/api/v1/sessions/logout'),
          headers: _authHeaders(),
        ).timeout(const Duration(seconds: 10));

        if (response.statusCode == 200 || response.statusCode == 302) {
          print('Logout successful on backend');
        } else {
          print('Backend logout returned: ${response.statusCode}');
        }
      }
    } catch (e) {
      print('Error calling backend logout: $e');
      // Continue with local cleanup even if backend call fails
    } finally {
      // Clear the stored auth token regardless of backend response
      _authToken = null;
      print('User logged out (token cleared)');
    }
  }
}
