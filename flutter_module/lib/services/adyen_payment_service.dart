import 'package:flutter/services.dart';

class AdyenPaymentService {
  final MethodChannel _channel = const MethodChannel('com.example.app/adyen');

  AdyenPaymentService();

  Future<String?> startPayment({
    required String amount,
    required String currency,
  }) async {
    try {
      final result = await _channel.invokeMethod('startPayment', {
        'amount': amount,
        'currency': currency,
      });
      return result as String?;
    } on PlatformException catch (e) {
      return "Error: ${e.message}";
    }
  }
}
