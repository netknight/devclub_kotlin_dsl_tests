import PaymentErrorEnum.Payment_Party_Not_Found
import PaymentErrorEnum.Same_Account_Error
import kotlin.test.Test

class BankApiTest {

    @Test
    fun testLocal() {
        val api = BankingApi()
        infix fun PaymentDefinition.remains(expected: Pair<String, String>) = api.transferOrder(toPayment()).remains(expected)
        infix fun PaymentDefinition.fails(expected: PaymentErrorEnum) = api.transferOrder(toPayment()).fails(expected)

        Transfer own "10.00" from "EE471000001020145685" to "EE471000001020145685" fails Same_Account_Error
        Transfer own "10.00" from "EE471000001020145685" to "EE471000001020145688" fails Payment_Party_Not_Found
        Transfer own "10.00" from "EE471000001020145685" to "EE471000001020145686" remains ("9990.00" and "10.00") // Was 10K & 0
        Transfer own "11.01" from "EE471000001020145685" to "EE471000001020145686" remains ("9978.99" and "21.01")
        Transfer own "0.01" from "EE471000001020145685" to "EE471000001020145686" remains ("9978.98" and "21.02")
    }

}