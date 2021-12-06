class BankingApi {

    fun <T: Payment> transferOrder(payment: T): PaymentResult<T> {
        println("Processing payment: $payment")
        return PaymentResult(payment = payment, success = true)
    }

}

