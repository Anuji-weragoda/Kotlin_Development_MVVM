part of 'payment_cubit.dart';

abstract class PaymentState extends Equatable {
  const PaymentState();

  @override
  List<Object?> get props => [];
}

class PaymentInitial extends PaymentState {}

class PaymentLoading extends PaymentState {}

class PaymentSuccess extends PaymentState {
  final Map<String, dynamic> data;
  const PaymentSuccess(this.data);

  /// Backwards-compatible message getter for code that expects `state.message`.
  String get message =>
      (data['message'] ?? data['result'] ?? 'Payment successful')?.toString() ?? 'Payment successful';

  @override
  List<Object?> get props => [data];
}

class PaymentFailure extends PaymentState {
  final String error;
  const PaymentFailure(this.error);

  @override
  List<Object?> get props => [error];
}
