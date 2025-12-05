package com.example.androidapp.ui.payment

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.SessionDropInCallback
import com.adyen.checkout.dropin.SessionDropInResult
import com.example.androidapp.data.remote.RetrofitClient
import com.example.androidapp.data.repository.AdyenPaymentRepository
import kotlinx.coroutines.launch
import timber.log.Timber

class PaymentActivity : AppCompatActivity() {

    private lateinit var paymentRepository: AdyenPaymentRepository
    private lateinit var dropInLauncher: androidx.activity.result.ActivityResultLauncher<*>

    companion object {
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_CURRENCY = "extra_currency"
        const val RESULT_PAYMENT_SUCCESS = "payment_success"
        const val RESULT_PAYMENT_FAILURE = "payment_failure"
        const val RESULT_PAYMENT_CANCELLED = "payment_cancelled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repository
        val apiService = RetrofitClient.getApiService()
        paymentRepository = AdyenPaymentRepository(apiService, applicationContext)

        // Register the Drop-in launcher with explicit SessionDropInCallback
        val callback = SessionDropInCallback { result ->
            result?.let { onDropInResult(it) }
        }
        dropInLauncher = DropIn.registerForDropInResult(this, callback)

        val amount = intent.getStringExtra(EXTRA_AMOUNT)
        val currency = intent.getStringExtra(EXTRA_CURRENCY)

        if (amount.isNullOrEmpty() || currency.isNullOrEmpty()) {
            handleError("Invalid payment parameters")
            return
        }

        startPaymentFlow(amount, currency)
    }

    private fun startPaymentFlow(amount: String, currency: String) {
        lifecycleScope.launch {
            try {
                Timber.d("Starting payment flow: $amount $currency")

                // Step 1: Create payment session
                val sessionResult = paymentRepository.createPaymentSession(amount, currency)

                sessionResult.fold(
                    onSuccess = { sessionData ->
                        Timber.d("Session created: ${sessionData.id}")

                        // Step 2: Initialize checkout session
                        val checkoutResult = paymentRepository.initializeCheckoutSession(sessionData)

                        checkoutResult.fold(
                            onSuccess = { checkoutSession ->
                                Timber.d("Checkout session initialized, launching Drop-in")

                                try {

                                    val paramsClass = Class.forName("com.adyen.checkout.dropin.internal.ui.model.SessionDropInResultContractParams")


                                    val constructor = paramsClass.declaredConstructors.find { it.parameterTypes.size == 3 }
                                        ?: throw IllegalStateException("Constructor with 3 params not found")
                                    constructor.isAccessible = true


                                    val checkoutConfig = com.adyen.checkout.components.core.CheckoutConfiguration(
                                        environment = paymentRepository.environment,
                                        clientKey = paymentRepository.clientKey
                                    )

                                    val serviceClass = com.adyen.checkout.dropin.SessionDropInService::class.java


                                    val params = constructor.newInstance(checkoutConfig, checkoutSession, serviceClass)

                                    @Suppress("UNCHECKED_CAST")
                                    val typedLauncher = dropInLauncher as androidx.activity.result.ActivityResultLauncher<Any>
                                    typedLauncher.launch(params)
                                    Timber.d("Drop-in launched via reflection")
                                } catch (e: Exception) {
                                    Timber.e(e, "Reflection failed")
                                    handleError("Failed to launch Drop-in: ${e.message}")
                                }
                            },
                            onFailure = { error ->
                                Timber.e(error, "Failed to initialize checkout session")
                                handleError(error.message ?: "Failed to initialize checkout")
                            }
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to create payment session")
                        handleError(error.message ?: "Failed to create session")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Payment flow error")
                handleError(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun onDropInResult(dropInResult: SessionDropInResult) {
        Timber.d("Drop-in result received: $dropInResult")

        when (dropInResult) {
            is SessionDropInResult.Finished -> {
                val result = dropInResult.result
                Timber.d("Payment successful: ${result.resultCode}")
                val intent = Intent().apply {
                    putExtra("result", RESULT_PAYMENT_SUCCESS)
                    putExtra("message", "Payment completed")
                    putExtra("transactionId", result.sessionId ?: "")
                    putExtra("resultCode", result.resultCode)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
            is SessionDropInResult.CancelledByUser -> {
                Timber.d("Payment cancelled by user")
                val intent = Intent().apply {
                    putExtra("result", RESULT_PAYMENT_CANCELLED)
                    putExtra("message", "Payment cancelled")
                }
                setResult(RESULT_CANCELED, intent)
                finish()
            }
            is SessionDropInResult.Error -> {
                val errorMessage = dropInResult.reason?.let { "Payment error: $it" } ?: "Payment error"
                Timber.e("Payment error: $errorMessage")
                val intent = Intent().apply {
                    putExtra("result", RESULT_PAYMENT_FAILURE)
                    putExtra("message", errorMessage)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun handleError(message: String) {
        Timber.e("Payment error: $message")
        val intent = Intent().apply {
            putExtra("result", RESULT_PAYMENT_FAILURE)
            putExtra("message", message)
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}
