import java.math.BigDecimal

class BankingApi {

    private val accounts: MutableList<AccountRecord> = mutableListOf(
        OwnAccountRecord(
            accountNumber = "EE471000001020145685",
            amount = "10000"
        ),
        OwnAccountRecord(
            accountNumber = "EE471000001020145686",
            amount = "0"
        ),
        SepaAccountRecord(
            accountNumber = "EE471000001020145687",
            amount = "1000",
            fullName = "Jaak Jola"
        ),
        SepaAccountRecord(
            accountNumber = "GB33BUKB20201555555555",
            amount = "2000",
            fullName = "John Lord"
        ),
        InternationalAccountRecord(
            accountNumber = "4003830171874018",
            amount = "3000",
            fullName = "Jimi Hendrix",
            address = "Seattle",
            bankInfo = BankInfo(
                name = "US Bank",
                address = "Chicago",
                countryCode = CountryCode.US,
                swiftCode = "ABBVUS44"
            )
        )
    )

    private fun AccountRecord.getMoneyError(requested: PaymentAmount): PaymentError? =
        (amount - requested).let {
            if (it == null)
                CurrencyMismatchError(requested = requested.currencyCode, actual = amount.currencyCode)
            else
                if (it.volume >= BigDecimal.ZERO) null else InsufficientMoneyError
        }

    private fun <T> MutableList<T>.replace(vararg values: Pair<T, T>): Unit = values.forEach { (from, to) ->
        remove(from).run { add(to) }
    }

    private fun transfer(from: AccountRecord, to: AccountRecord, vol: BigDecimal): Pair<PaymentAmount, PaymentAmount> =
        (from - vol).let { newFrom ->
            (to + vol).let { newTo ->
                accounts.replace(from to newFrom, to to newTo).run {
                    newFrom.amount to newTo.amount
                }
            }
        }

    fun transferOrder(payment: Payment): PaymentResult =
        if (payment.to.accountNumber == payment.from.accountNumber)
            PaymentFailure(SameAccountError(payment.to.accountNumber))
        else
            accounts.find { it.accountNumber == payment.from.accountNumber }?.let { account ->
                account.getMoneyError(payment.amount)
                    ?.let { PaymentFailure(it) }
                    ?: accounts.find { it.accountNumber == payment.to.accountNumber }?.let {
                        if (payment.matches(it))
                            PaymentSuccess(payment = payment, remaining = transfer(account, it, payment.amount.volume))
                        else
                            PaymentFailure(PaymentPartyNotFound(payment.to))
                    } ?: PaymentFailure(PaymentPartyNotFound(payment.to))
            } ?: PaymentFailure(PaymentPartyNotFound(payment.from))
}

