import java.math.BigDecimal

enum class CountryCode {
    EE
}

enum class CurrencyCode {
    EUR
}

interface BankInfo {
    val name: String
    val address: String
    val countryCode: CountryCode
}

data class InternationalBankInfo(
    override val name: String,
    override val address: String,
    override val countryCode: CountryCode
) : BankInfo

data class SepaBankInfo(
    override val name: String,
    override val address: String,
    override val countryCode: CountryCode
) : BankInfo

data class LocalBank(
    override val name: String,
    override val address: String
) : BankInfo {
    override val countryCode: CountryCode = CountryCode.EE
}

val myBank: LocalBank = LocalBank(
    name = "My Own Bank",
    address = "Somewhere in the middle of nowhere"
)

interface PaymentParty {
    val fullname: String
    val accountNumber: String
    val bankInfo: BankInfo
}

data class InternationalPaymentParty(
    override val fullname: String,
    override val accountNumber: String,
    override val bankInfo: BankInfo
) : PaymentParty

data class SepaPaymentParty(
    override val fullname: String,
    override val accountNumber: String,
    override val bankInfo: SepaBankInfo
) : PaymentParty

data class LocalPaymentParty(
    override val fullname: String,
    override val accountNumber: String,
    override val bankInfo: LocalBank
) : PaymentParty

data class InternalPaymentParty(
    override val fullname: String,
    override val accountNumber: String
) : PaymentParty {
    override val bankInfo: LocalBank = myBank
}

data class PaymentAmount(val volume: BigDecimal, val currencyCode: CurrencyCode)

private fun BigDecimal.toEur() = PaymentAmount(volume = this, currencyCode = CurrencyCode.EUR)

interface Payment {
    val sender: InternalPaymentParty
    val receiver: PaymentParty
    val amount: PaymentAmount
    val note: String
    val urgent: Boolean
}

data class AnyPayment<ReceiverParty: PaymentParty>(
    override val sender: InternalPaymentParty,
    override val receiver: ReceiverParty,
    override val amount: PaymentAmount,
    override val note: String,
    override val urgent: Boolean
): Payment

data class EuroPayment<ReceiverParty: PaymentParty>(
    override val sender: InternalPaymentParty,
    override val receiver: ReceiverParty,
    val amountInEuro: BigDecimal,
    override val note: String,
    override val urgent: Boolean
): Payment {
    override val amount: PaymentAmount = amountInEuro.toEur()
}

typealias InternationalPayment = AnyPayment<PaymentParty>
typealias SepaPayment = EuroPayment<SepaPaymentParty>
typealias LocalPayment = EuroPayment<LocalPaymentParty>
typealias InternalPayment = EuroPayment<InternalPaymentParty>

/*
data class InternationalPayment(
    override val sender: InternalPaymentParty,
    override val receiver: PaymentParty,
    override val amount: PaymentAmount,
    override val note: String,
    override val urgent: Boolean
) : Payment

data class SepaPayment(
    override val sender: InternalPaymentParty,
    override val receiver: SepaPaymentParty,
    val amountInEuro: BigDecimal,
    override val note: String,
    override val urgent: Boolean
) : Payment {
    override val amount: PaymentAmount = amountInEuro.toEur()
}

data class LocalPayment(
    override val sender: InternalPaymentParty,
    override val receiver: LocalPaymentParty,
    val amountInEuro: BigDecimal,
    override val note: String,
    override val urgent: Boolean
): Payment {
    override val amount: PaymentAmount = amountInEuro.toEur()
}

data class InternalPayment(
    override val sender: InternalPaymentParty,
    override val receiver: InternalPaymentParty,
    val amountInEuro: BigDecimal,
    override val note: String,
    override val urgent: Boolean
): Payment {
    override val amount: PaymentAmount = amountInEuro.toEur()
}
*/
data class PaymentResult<T: Payment>(
    val payment: T,
    val success: Boolean
)