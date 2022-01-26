import PaymentErrorEnum.*
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import java.math.BigDecimal

sealed interface AmountDefinition {
    val amount: BigDecimal
}

data class OwnAmountDefinition(override val amount: BigDecimal): AmountDefinition {
    infix fun from(accountNumber: String): FromDefinition = FromDefinition(
        accountNumber = accountNumber,
        amount = this
    )
}

data class FromDefinition(val accountNumber: String, val amount: AmountDefinition) {
    infix fun to(accountNumber: String) = OwnPaymentDefinition(
        toAccountNumber = accountNumber,
        fromDefinition = this
    )
}

sealed interface PaymentDefinition

fun PaymentDefinition.toPayment(): Payment = when(this) {
    is OwnPaymentDefinition ->
        OwnAccountPayment(
            from = OwnAccountPaymentParty(accountNumber = fromDefinition.accountNumber),
            to = OwnAccountPaymentParty(accountNumber = toAccountNumber),
            amount = fromDefinition.amount.amount.toEur(),
            mode = Instant
        )
    else -> throw IllegalArgumentException(this.toString())
}

data class OwnPaymentDefinition(val toAccountNumber: String, val fromDefinition: FromDefinition): PaymentDefinition

data class ResultDefinition(val paymentResult: PaymentResult) {

}

object Transfer {
    infix fun own(amount: String): OwnAmountDefinition = OwnAmountDefinition(amount = BigDecimal(amount))
}

infix fun <A, B> A.and(that: B): Pair<A, B> = Pair(this, that)

fun PaymentResult.remains(expected: Pair<String, String>): Unit = when(this) {
    is PaymentSuccess -> {
        remainingFrom.volume shouldBe BigDecimal(expected.first)
        remainingTo.volume shouldBe BigDecimal(expected.second)
    }
    else -> fail("Payment success was expected, got: $this")
}

enum class PaymentErrorEnum {
    Same_Account_Error,
    Payment_Party_Not_Found
}

fun PaymentResult.fails(expected: PaymentErrorEnum): Unit = when(this) {
    is PaymentFailure -> when(expected) {
        Same_Account_Error -> error shouldBe beInstanceOf<SameAccountError>()
        Payment_Party_Not_Found -> error shouldBe beInstanceOf<PaymentPartyNotFound>()
    }
    else -> fail("Payment failure expected: $expected, got: $this")
}