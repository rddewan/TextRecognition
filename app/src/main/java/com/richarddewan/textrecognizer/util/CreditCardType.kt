package com.richarddewan.textrecognizer.util



enum class CreditCardType(val cardTypeRegex: String) {
    UNKNOWN(""),
    VISA("^4[0-9]{12}(?:[0-9]{3}){0,2}$"),
    MASTERCARD("^(?:5[1-5][0-9]{2}|222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[01][0-9]|2720)[0-9]{12}$"),
    AMERICAN_EXPRESS("^3[47][0-9]{13}$"),
    DINERS_CLUB("^3(?:0[0-5]\\d|095|6\\d{0,2}|[89]\\d{2})\\d{12,15}$"),
    DISCOVER("^6(?:011|[45][0-9]{2})[0-9]{12}$"),
    JCB("^(?:2131|1800|35\\d{3})\\d{11}$"),
    CHINA_UNION_PAY("^62[0-9]{14,17}$"),
    UNION_PAY_CARD("^(62[0-9]{14,17})$")
}