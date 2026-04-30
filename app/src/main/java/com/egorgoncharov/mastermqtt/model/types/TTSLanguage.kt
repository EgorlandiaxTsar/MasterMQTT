package com.egorgoncharov.mastermqtt.model.types

import java.util.Locale

enum class TTSLanguage(val label: String, val locale: Locale) {
    EN("English (US)", Locale("en", "US")),
    RU("Russian", Locale("ru", "RU")),
    IT("Italian", Locale("it", "IT")),
    FR("French", Locale("fr", "FR")),
    DE("German", Locale("de", "DE")),
    ES("Spanish", Locale("es", "ES"))
}