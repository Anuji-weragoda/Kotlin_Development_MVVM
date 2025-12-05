import 'package:bloc/bloc.dart';
import 'package:equatable/equatable.dart';
import '../../../../services/adyen_payment_service.dart';

part 'payment_state.dart';

class PaymentCubit extends Cubit<PaymentState> {
  final AdyenPaymentService _adyenPaymentService;

  PaymentCubit(this._adyenPaymentService) : super(PaymentInitial());

  Future<void> pay({required String amount, required String currency}) async {
    try {
      emit(PaymentLoading());
      final result = await _adyenPaymentService.startPayment(
        amount: amount,
        currency: currency,
      );

      if (result == null) {
        emit(const PaymentFailure('No response from platform'));
        return;
      }

      // If the platform returned an error map
      if (result.containsKey('error')) {
        emit(PaymentFailure(result['error']?.toString() ?? 'Unknown error'));
        return;
      }

      // If the platform returned a simple result map
      final bool success = result['success'] == true;
      if (success) {
        emit(PaymentSuccess(result));
      } else {
        final String message = result['message']?.toString() ?? result['result']?.toString() ?? 'Payment failed';
        emit(PaymentFailure(message));
      }
    } catch (e) {
      emit(PaymentFailure(e.toString()));
    }
  }
}
