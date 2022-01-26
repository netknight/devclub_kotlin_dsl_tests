import CountryCode.US
import CurrencyCode.EUR
import PaymentErrorEnum.Payment_Party_Not_Found
import PaymentErrorEnum.Same_Account_Error
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class BankApiTest {

    @Test
    fun testToOwnAccounts() = withApi(BankingApi()) {
        Transfer own "10.00" from "EE471000001020145685" to "EE471000001020145685" fails Same_Account_Error
        Transfer own "10.00" from "EE471000001020145685" to "EE471000001020145688" fails Payment_Party_Not_Found // Account doesn't exist
        Transfer own "10.00" from "EE471000001020145685" to "EE471000001020145686" remains ("9990.00" and "10.00") // Was 10K & 0
        Transfer own "11.01" from "EE471000001020145685" to "EE471000001020145686" note ("return to credit card") remains ("9978.99" and "21.01")
        Transfer own "0.01" from "EE471000001020145685" to "EE471000001020145686" remains ("9978.98" and "21.02")
    }


    @Test
    fun testToLocalAccount() = withApi(BankingApi()) {
        // This will fail due to missing 'fullName' field
        //Transfer local "10.00" from "EE471000001020145685" to "EE471000001020145686" remains ("9990.00" and "10.00") // Was 10K & 0
        Transfer local "10.00" from "EE471000001020145685" to "EE471000001020145687" name "John Dow" fails Payment_Party_Not_Found // Name mismatch
        Transfer local "10.00" from "EE471000001020145685" to "EE471000001020145687" name "Jaak Jola" remains ("9990.00" and "1010.00")
        Transfer local "10.00" from "EE471000001020145685" to "EE471000001020145687" name "Jaak Jola" note "debt repay" remains ("9980.00" and "1020.00")
    }

    @Test
    fun testToSepaAccount() = withApi(BankingApi()) {
        // This will fail due to missing 'note' field
        // Transfer sepa "10.00" from "EE471000001020145685" to "EE471000001020145687" name "Jaak Jola" remains ("9990.00" and "1010.00")
        Transfer sepa "10.00" from "EE471000001020145685" to "EE471000001020145687" name "Jaak Jola" note "debt repay" remains ("9990.00" and "1010.00")
        Transfer urgent "10.00" from "EE471000001020145685" to "GB33BUKB20201555555555" name "John Lord" note "debt repay" remains ("9980.00" and "2010.00")
    }

    @Test
    fun testToInternational() = withApi(BankingApi()) { withBankInfo({
        Bank name "US Bank" address "Chicago" countryCode US swiftCode "ABBVUS44"
    }) {
        // Next 2 lines will fail due to missing 'address' & 'bankInfo' fields
        //Transfer international ("10.00" of EUR) from "EE471000001020145685" to "GB33BUKB20201555555555" name "John Lord" note "debt repay" remains ("9990.00" and "1010.00")
        //Transfer international ("10.00" of EUR) from "EE471000001020145685" to "4003830171874018" name "Jimi Hendrix" address "Seattle" note "debt repay" remains ("9990.00" and "3010.00")
        Transfer international ("10.00" of EUR) from "EE471000001020145685" to
            "4003830171874018" name "Jimi Hendrix" address "Seattle" bank
            Bank note "debt repay" remains ("9990.00" and "3010.00")
    }}

    @Test
    fun testToInternational_InvalidBank() = withApi(BankingApi()) { withBankInfo({
        Bank name "Fake Bank" address "Chicago" countryCode US swiftCode "ABBVUS44"
    }) {
        Transfer international ("10.00" of EUR) from "EE471000001020145685" to
                "4003830171874018" name "Jimi Hendrix" address "Seattle" bank
                Bank note "debt repay" fails Payment_Party_Not_Found
    }}

    @Test
    fun testBadInput() = withApi(BankingApi()) {
        shouldThrow<NumberFormatException> {
            Transfer own "DD.XX" from "EE471000001020145685" to "EE471000001020145685"
        }
        shouldThrow<NumberFormatException> {
            Transfer own "10.00" from "EE471000001020145685" to "EE471000001020145686" remains ("AAA.00" and "10.00")
        }
    }

}