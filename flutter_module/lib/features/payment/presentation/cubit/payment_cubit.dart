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

      if (result != null && !result.startsWith("Error")) {
        emit(PaymentSuccess(result));
      } else {
        emit(PaymentFailure(result ?? "Unknown error"));
      }
    } catch (e) {
      emit(PaymentFailure(e.toString()));
    }
  }
}
