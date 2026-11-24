package com.recovery.companion.data

/** Fixed trigger list. Users pick a code — never type free text. */
data class Trigger(val id: String, val labelAr: String)

object Triggers {
    val ALL = listOf(
        Trigger("loneliness", "وحدة"),
        Trigger("stress", "ضغط / توتر"),
        Trigger("boredom", "ملل / فراغ"),
        Trigger("night", "سهر لوحدي بالليل"),
        Trigger("phone", "تصفّح من غير هدف"),
        Trigger("sadness", "حزن / إحباط"),
        Trigger("anger", "غضب"),
        Trigger("celebration", "فرحة / مكافأة لنفسي"),
        Trigger("sleep", "مش عارف أنام"),
        Trigger("other", "سبب تاني")
    )
    fun label(id: String): String = ALL.firstOrNull { it.id == id }?.labelAr ?: id
}
