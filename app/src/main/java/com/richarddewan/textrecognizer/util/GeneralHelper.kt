package com.richarddewan.textrecognizer.util

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.regex.Pattern

object GeneralHelper {

    const val TAG = "GeneralHelper"
    private lateinit var pattern: Pattern

    /*
    try to match the regex for credit card
     */

    fun matchCreditCard(card: String): Boolean {
        var matcher: Boolean

        CreditCardType.values().forEach {

            pattern = Pattern.compile(it.cardTypeRegex)
            matcher = pattern.matcher(card).matches()
            if (matcher){
                return matcher
            }
        }

        return false

    }

    /*
    get the card vendor
     */
    fun getCreditCardVendor(card: String): String {
        var matcher: Boolean

        CreditCardType.values().forEach {

            pattern = Pattern.compile(it.cardTypeRegex)
            matcher = pattern.matcher(card).matches()
            if (matcher){
                return it.name
            }
        }

        return CreditCardType.UNKNOWN.name
    }

    /*
    There is an extra validation check that you can do on the credit card number before processing the order.
    The last digit in the credit card number is a checksum calculated according to the Luhn algorithm.
    Since this algorithm requires basic arithmetic, you cannot implement it with a regular expression.
    Below is the method which you can use to run checksum validation using Luhn Algorithm.
    This function takes a string with the credit card number as a parameter.
    The card number should consist only of digits.
    The actual algorithm runs on the array of digits, calculating a checksum.
    If the sum modulus 10 is zero, then the card number is valid. If not, the number is invalid.
    I have taken the reference implementation of Luhn Algo from Google Code.
    https://code.google.com/archive/p/gnuc-credit-card-checker/
     */
    fun checkValidCard(ccNumber: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in ccNumber.length - 1 downTo 0) {
            var n = ccNumber.substring(i, i + 1).toInt()
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = n % 10 + 1
                }
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }
}

