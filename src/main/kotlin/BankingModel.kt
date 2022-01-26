import java.math.BigDecimal

enum class CountryCode {
    EE, US, GB
}

enum class CurrencyCode {
    EUR, RUB, USD
}

sealed interface PaymentMode
object Instant: PaymentMode
sealed interface NonInstantMode: PaymentMode
object Regular: NonInstantMode
object Urgent: NonInstantMode

data class PaymentAmount(val volume: BigDecimal, val currencyCode: CurrencyCode) {
    infix operator fun plus(amount: PaymentAmount): PaymentAmount? =
        if (currencyCode == amount.currencyCode)
            PaymentAmount(volume = volume + amount.volume, currencyCode = currencyCode)
        else
            null
    infix operator fun minus(amount: PaymentAmount): PaymentAmount? =
        if (currencyCode == amount.currencyCode)
            PaymentAmount(volume = volume - amount.volume, currencyCode = currencyCode)
        else
            null
}

fun BigDecimal.toEur() = PaymentAmount(volume = this, currencyCode = CurrencyCode.EUR)

data class BankInfo(
    val name: String,
    val address: String,
    val countryCode: CountryCode,
    val swiftCode: String
)

sealed interface PaymentParty {
    val accountNumber: String
}

data class InternationalPaymentParty(
    override val accountNumber: String,
    val fullName: String,
    val address: String,
    val bankInfo: BankInfo
) : PaymentParty

data class SepaPaymentParty(
    override val accountNumber: String,
    val fullName: String
) : PaymentParty

data class OwnAccountPaymentParty(
    override val accountNumber: String,
): PaymentParty

sealed interface Payment {
    val from: OwnAccountPaymentParty
    val to: PaymentParty
    val amount: PaymentAmount
}

data class AnyPayment<ReceiverParty: PaymentParty, Mode: PaymentMode, NoteType: String?>(
    override val from: OwnAccountPaymentParty,
    override val to: ReceiverParty,
    override val amount: PaymentAmount,
    val note: NoteType,
    val mode: Mode
): Payment

typealias InternationalPayment = AnyPayment<InternationalPaymentParty, Regular, String>
typealias SepaPayment = AnyPayment<SepaPaymentParty, NonInstantMode, String>
typealias LocalPayment = AnyPayment<SepaPaymentParty, Instant, String?>
typealias OwnAccountPayment = AnyPayment<OwnAccountPaymentParty, Instant, String?>

sealed interface PaymentError
object InsufficientMoneyError: PaymentError
data class CurrencyMismatchError(val requested: CurrencyCode, val actual: CurrencyCode): PaymentError
data class PaymentPartyNotFound(val paymentParty: PaymentParty): PaymentError
data class SameAccountError(val accountNumber: String): PaymentError
data class AccountLimitError(val requested: BigDecimal, val available: BigDecimal): PaymentError

sealed interface PaymentResult
data class PaymentSuccess(val payment: Payment, val remainingFrom: PaymentAmount, val remainingTo: PaymentAmount): PaymentResult {
    constructor(payment: Payment, remaining: Pair<PaymentAmount, PaymentAmount>): this(
        payment = payment,
        remainingFrom = remaining.first,
        remainingTo = remaining.second
    )
}
data class PaymentFailure(val error: PaymentError): PaymentResult

sealed interface AccountRecord {
    val accountNumber: String
    val amount: PaymentAmount

    infix operator fun plus(volume: BigDecimal): AccountRecord// = transfer(volume)
    infix operator fun minus(volume: BigDecimal): AccountRecord = plus(-volume)
}

data class OwnAccountRecord(
    override val accountNumber: String,
    override val amount: PaymentAmount
): AccountRecord {
    constructor(accountNumber: String, amount: String): this(
        accountNumber = accountNumber,
        amount = BigDecimal(amount).toEur()
    )

    override fun plus(volume: BigDecimal): AccountRecord = copy(amount = amount.copy(volume = amount.volume + volume))
}

data class SepaAccountRecord(
    override val accountNumber: String,
    override val amount: PaymentAmount,
    val fullName: String,
): AccountRecord {
    constructor(accountNumber: String, amount: String, fullName: String): this(
        accountNumber = accountNumber,
        amount = BigDecimal(amount).toEur(),
        fullName = fullName
    )

    override fun plus(volume: BigDecimal): AccountRecord = copy(amount = amount.copy(volume = amount.volume + volume))
}

data class InternationalAccountRecord(
    override val accountNumber: String,
    override val amount: PaymentAmount,
    val fullName: String,
    val address: String,
    val bankInfo: BankInfo
): AccountRecord {
    constructor(accountNumber: String, amount: String, fullName: String, address: String, bankInfo: BankInfo): this(
        accountNumber = accountNumber,
        amount = BigDecimal(amount).toEur(),
        fullName = fullName,
        address = address,
        bankInfo = bankInfo
    )

    override fun plus(volume: BigDecimal): AccountRecord = copy(amount = amount.copy(volume = amount.volume + volume))
}

fun Payment.matches(p: AccountRecord): Boolean = when(to) {
    is OwnAccountPaymentParty ->  p is OwnAccountRecord && to.accountNumber == p.accountNumber
    // TODO: WTF CASTS?
    is SepaPaymentParty ->
        p is SepaAccountRecord &&
            to.accountNumber == p.accountNumber &&
            (to as SepaPaymentParty).fullName == p.fullName
    is InternationalPaymentParty ->
        p is InternationalAccountRecord &&
            to.accountNumber == p.accountNumber &&
            (to as InternationalPaymentParty).fullName == p.fullName &&
            (to as InternationalPaymentParty).address == p.address &&
            (to as InternationalPaymentParty).bankInfo == p.bankInfo
}