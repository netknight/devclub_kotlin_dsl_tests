import PaymentErrorEnum.*
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import java.math.BigDecimal

data class MoneyToOwnDefinition(val money: PaymentAmount) {
    infix fun from(accountNumber: String): MoneyToOwnFromDefinition = MoneyToOwnFromDefinition(
        accountNumber = accountNumber,
        money = money
    )
}

data class MoneyToLocalDefinition(val money: PaymentAmount) {
    infix fun from(accountNumber: String): MoneyToLocalFromDefinition = MoneyToLocalFromDefinition(
        accountNumber = accountNumber,
        money = money
    )
}

data class MoneyToSepaDefinition(val money: PaymentAmount, val mode: NonInstantMode) {
    infix fun from(accountNumber: String): MoneyToSepaFromDefinition = MoneyToSepaFromDefinition(
        accountNumber = accountNumber,
        money = money,
        mode = mode
    )
}

data class MoneyToInternational(val money: PaymentAmount) {
    infix fun from(accountNumber: String): MoneyToInternationalFromDefinition = MoneyToInternationalFromDefinition(
        accountNumber = accountNumber,
        money = money
    )
}

data class MoneyToOwnFromDefinition(val accountNumber: String, val money: PaymentAmount) {
    infix fun to(accountNumber: String) = OwnPaymentDefinition(
        toAccountNumber = accountNumber,
        fromDefinition = this
    )
}

data class MoneyToLocalFromDefinition(
    val accountNumber: String,
    val money: PaymentAmount
) {
    infix fun to(accountNumber: String): LocalPaymentDefinition = LocalPaymentDefinition(
        toAccountNumber = accountNumber,
        fromDefinition = this,
    )
}

data class MoneyToSepaFromDefinition(
    val accountNumber: String,
    val money: PaymentAmount,
    val mode: NonInstantMode
) {
    infix fun to(accountNumber: String): SepaPaymentDefinition = SepaPaymentDefinition(
        toAccountNumber = accountNumber,
        fromDefinition = this
    )
}

data class MoneyToInternationalFromDefinition(
    val accountNumber: String,
    val money: PaymentAmount,
) {
    infix fun to(accountNumber: String): InternationalPaymentDefinition = InternationalPaymentDefinition(
        toAccountNumber = accountNumber,
        fromDefinition = this
    )
}

sealed interface PaymentDefinition

data class OwnPaymentDefinition(
    val toAccountNumber: String,
    val fromDefinition: MoneyToOwnFromDefinition,
    val note: String? = null
): PaymentDefinition {
    infix fun note(note: String) = copy(note = note)
}

data class LocalPaymentDefinition(
    val toAccountNumber: String,
    val fromDefinition: MoneyToLocalFromDefinition,
    val fullName: String? = null,
    val note: String? = null
): PaymentDefinition {
    infix fun name(fullName: String) = copy(fullName = fullName)
    infix fun note(note: String) = copy(note = note)
}

data class SepaPaymentDefinition(
    val toAccountNumber: String,
    val fromDefinition: MoneyToSepaFromDefinition,
    val fullName: String? = null,
    val note: String? = null
): PaymentDefinition {
    infix fun name(fullName: String) = copy(fullName = fullName)
    infix fun note(note: String) = copy(note = note)
}

data class InternationalPaymentDefinition(
    val toAccountNumber: String,
    val fromDefinition: MoneyToInternationalFromDefinition,
    val fullName: String? = null,
    val note: String? = null,
    val address: String? = null,
    val bankInfo: BankInfo? = null
): PaymentDefinition {
    infix fun name(fullName: String) = copy(fullName = fullName)
    infix fun note(note: String) = copy(note = note)
    infix fun address(address: String) = copy(address = address)
    infix fun bank(bank: BankInfo) = copy(bankInfo = bank)
}


fun PaymentDefinition.toPayment(): Payment = when(this) {
    is OwnPaymentDefinition ->
        OwnAccountPayment(
            from = OwnAccountPaymentParty(accountNumber = fromDefinition.accountNumber),
            to = OwnAccountPaymentParty(accountNumber = toAccountNumber),
            amount = fromDefinition.money,
            mode = Instant,
            note = note
        )
    is LocalPaymentDefinition ->
        LocalPayment(
            from = OwnAccountPaymentParty(accountNumber = fromDefinition.accountNumber),
            to = SepaPaymentParty(accountNumber = toAccountNumber, fullName = fullName ?: throw IllegalArgumentException("'Full name' field is mandatory for Local Payment!")),
            amount = fromDefinition.money,
            mode = Instant,
            note = note
        )
    is SepaPaymentDefinition ->
        SepaPayment(
            from = OwnAccountPaymentParty(accountNumber = fromDefinition.accountNumber),
            to = SepaPaymentParty(accountNumber = toAccountNumber, fullName = fullName ?: throw IllegalArgumentException("'Full name' field is mandatory for SEPA Payment!")),
            amount = fromDefinition.money,
            mode = fromDefinition.mode,
            note = note ?: throw IllegalArgumentException("'Note' field is mandatory for SEPA Payment!")
        )
    is InternationalPaymentDefinition ->
        InternationalPayment(
            from = OwnAccountPaymentParty(accountNumber = fromDefinition.accountNumber),
            to = InternationalPaymentParty(
                accountNumber = toAccountNumber,
                fullName = fullName ?: throw IllegalArgumentException("'Full name' field is mandatory for International Payment!"),
                address = address ?: throw IllegalArgumentException("'Address' field is mandatory for International Payment!"),
                bankInfo = bankInfo ?: throw IllegalArgumentException("'Bank info' field is mandatory for International Payment!")
            ),
            amount = fromDefinition.money,
            mode = Regular,
            note = note ?: throw IllegalArgumentException("'Note' field is mandatory for International Payment!")
        )
}

object Transfer {
    infix fun own(amount: String): MoneyToOwnDefinition =
        MoneyToOwnDefinition(money = BigDecimal(amount).toEur())

    infix fun local(amount: String): MoneyToLocalDefinition =
        MoneyToLocalDefinition(money = BigDecimal(amount).toEur())

    infix fun sepa(amount: String): MoneyToSepaDefinition =
        MoneyToSepaDefinition(money = BigDecimal(amount).toEur(), mode = Regular)

    infix fun urgent(amount: String): MoneyToSepaDefinition =
        MoneyToSepaDefinition(money = BigDecimal(amount).toEur(), mode = Urgent)

    infix fun international(amount: PaymentAmount): MoneyToInternational =
        MoneyToInternational(money = amount)
}

infix fun String.of(currencyCode: CurrencyCode): PaymentAmount = PaymentAmount(
    volume = BigDecimal(this),
    currencyCode = currencyCode
)

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

data class ApiContext(private val api: BankingApi) {
    infix fun PaymentDefinition.remains(expected: Pair<String, String>) =
        api.transferOrder(toPayment()).remains(expected)

    infix fun PaymentDefinition.fails(expected: PaymentErrorEnum) =
        api.transferOrder(toPayment()).fails(expected)
}

fun withApi(api: BankingApi, block: ApiContext.() -> Unit): Unit =
    with(ApiContext(api), block)

data class BankInfoContext(val Bank: BankInfo)

fun withBankInfo(bankInfo: () -> BankInfoDefinition, block: BankInfoContext.() -> Unit): Unit =
    with(BankInfoContext(bankInfo().toBankInfo()), block)

object Bank {
    infix fun name(name: String): BankInfoDefinition = BankInfoDefinition(name = name)
}

data class BankInfoDefinition(
    val name: String,
    val address: String? = null,
    val countryCode: CountryCode? = null,
    val swiftCode: String? = null
) {
    infix fun address(address: String) = copy(address = address)
    infix fun countryCode(countryCode: CountryCode) = copy(countryCode = countryCode)
    infix fun swiftCode(swiftCode: String) = copy(swiftCode = swiftCode)
}

private fun BankInfoDefinition.toBankInfo() = BankInfo(
    name = name,
    address = address ?: throw IllegalArgumentException("'Address' field is mandatory for Bank Info!"),
    countryCode = countryCode ?: throw IllegalArgumentException("'Country Code' field is mandatory for Bank Info!"),
    swiftCode = swiftCode  ?: throw IllegalArgumentException("'Swift Code' field is mandatory for Bank Info!")
)