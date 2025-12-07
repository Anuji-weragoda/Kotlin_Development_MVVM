import 'package:flutter/services.dart';

class AdyenPaymentService {
  final MethodChannel _channel = const MethodChannel('com.example.app/adyen');

  AdyenPaymentService();


  Future<Map<String, dynamic>?> startPayment({
    required String amount,
    required String currency,
  }) async {
    try {
      final result = await _channel.invokeMethod('startPayment', {
        'amount': amount,
        'currency': currency,
      });

      if (result == null) return null;


      if (result is Map) {
        return Map<String, dynamic>.from(result as Map);
      }


      return {'result': result.toString()};
    } on PlatformException catch (e) {
      return {'error': e.message ?? 'Platform exception'};
    } catch (e) {
      return {'error': e.toString()};
    }
  }
}
