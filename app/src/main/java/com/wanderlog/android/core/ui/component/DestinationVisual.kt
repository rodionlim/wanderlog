package com.wanderlog.android.core.ui.component

import androidx.compose.ui.graphics.Color

/** Visual treatment for a trip based on its destination string. */
data class DestinationVisual(
    val emoji: String,
    val gradientStart: Color,
    val gradientEnd: Color
)

private val GRADIENTS = listOf(
    Color(0xFF0093E9) to Color(0xFF80D0C7), // ocean
    Color(0xFFFF6B6B) to Color(0xFFFFE66D), // sunset
    Color(0xFFA8EDEA) to Color(0xFFFED6E3), // pastel
    Color(0xFFFF9966) to Color(0xFFFF5E62), // coral
    Color(0xFF667EEA) to Color(0xFF764BA2), // dusk
    Color(0xFF11998E) to Color(0xFF38EF7D), // tropical
    Color(0xFFFC466B) to Color(0xFF3F5EFB), // neon
    Color(0xFFEE9CA7) to Color(0xFFFFDDE1), // rose
    Color(0xFFF7971E) to Color(0xFFFFD200), // gold
    Color(0xFF5B86E5) to Color(0xFF36D1DC), // sky
)

private data class Rule(val keywords: List<String>, val emoji: String)

private val RULES = listOf(
    // country flags take precedence over city emoji
    Rule(listOf("japan", "tokyo", "kyoto", "osaka", "okinawa"), "🇯🇵"),
    Rule(listOf("korea", "seoul", "busan"), "🇰🇷"),
    Rule(listOf("taiwan", "taipei", "kaohsiung"), "🇹🇼"),
    Rule(listOf("china", "beijing", "shanghai"), "🇨🇳"),
    Rule(listOf("hong kong"), "🇭🇰"),
    Rule(listOf("singapore"), "🇸🇬"),
    Rule(listOf("thailand", "bangkok", "phuket", "chiang mai"), "🇹🇭"),
    Rule(listOf("vietnam", "hanoi", "ho chi minh"), "🇻🇳"),
    Rule(listOf("malaysia", "kuala lumpur"), "🇲🇾"),
    Rule(listOf("indonesia", "bali", "jakarta"), "🇮🇩"),
    Rule(listOf("philippines", "manila"), "🇵🇭"),
    Rule(listOf("india", "mumbai", "delhi", "goa"), "🇮🇳"),
    Rule(listOf("maldives"), "🇲🇻"),
    Rule(listOf("sri lanka", "colombo"), "🇱🇰"),
    Rule(listOf("nepal", "kathmandu"), "🇳🇵"),
    Rule(listOf("cambodia", "siem reap"), "🇰🇭"),
    Rule(listOf("australia", "sydney", "melbourne", "perth", "brisbane", "gold coast", "cairns", "hobart"), "🇦🇺"),
    Rule(listOf("new zealand", "auckland", "queenstown"), "🇳🇿"),
    Rule(listOf("uk", "united kingdom", "britain", "london", "edinburgh", "scotland", "wales"), "🇬🇧"),
    Rule(listOf("france", "paris", "nice", "lyon"), "🇫🇷"),
    Rule(listOf("italy", "rome", "florence", "venice", "milan"), "🇮🇹"),
    Rule(listOf("spain", "barcelona", "madrid", "seville"), "🇪🇸"),
    Rule(listOf("portugal", "lisbon", "porto"), "🇵🇹"),
    Rule(listOf("netherlands", "amsterdam"), "🇳🇱"),
    Rule(listOf("germany", "berlin", "munich"), "🇩🇪"),
    Rule(listOf("czech", "prague"), "🇨🇿"),
    Rule(listOf("austria", "vienna"), "🇦🇹"),
    Rule(listOf("switzerland", "zurich", "interlaken", "geneva"), "🇨🇭"),
    Rule(listOf("greece", "athens", "santorini", "mykonos"), "🇬🇷"),
    Rule(listOf("turkey", "istanbul"), "🇹🇷"),
    Rule(listOf("iceland", "reykjavik"), "🇮🇸"),
    Rule(listOf("denmark", "copenhagen"), "🇩🇰"),
    Rule(listOf("sweden", "stockholm"), "🇸🇪"),
    Rule(listOf("norway", "oslo"), "🇳🇴"),
    Rule(listOf("ireland", "dublin"), "🇮🇪"),
    Rule(listOf("usa", "united states", "new york", "los angeles", "san francisco", "vegas", "chicago", "miami", "honolulu", "seattle", "hawaii"), "🇺🇸"),
    Rule(listOf("canada", "toronto", "vancouver", "banff", "montreal"), "🇨🇦"),
    Rule(listOf("mexico", "cancun"), "🇲🇽"),
    Rule(listOf("brazil", "rio"), "🇧🇷"),
    Rule(listOf("argentina", "buenos aires"), "🇦🇷"),
    Rule(listOf("peru", "lima", "cusco"), "🇵🇪"),
    Rule(listOf("uae", "dubai", "abu dhabi"), "🇦🇪"),
    Rule(listOf("qatar", "doha"), "🇶🇦"),
    Rule(listOf("egypt", "cairo"), "🇪🇬"),
    Rule(listOf("morocco", "marrakech"), "🇲🇦"),
    Rule(listOf("south africa", "cape town"), "🇿🇦"),
    Rule(listOf("kenya", "nairobi"), "🇰🇪"),
    Rule(listOf("tanzania", "zanzibar"), "🇹🇿"),
    // fallbacks by vibe
    Rule(listOf("beach", "island", "coast"), "🏝️"),
    Rule(listOf("mountain", "alps", "ski"), "🏔️"),
    Rule(listOf("desert"), "🏜️"),
    Rule(listOf("cruise"), "🚢")
)

fun destinationVisualFor(destination: String): DestinationVisual {
    val normalized = destination.lowercase()
    val emoji = RULES.firstOrNull { rule -> rule.keywords.any { kw -> kw in normalized } }?.emoji
        ?: "✈️"
    val hash = (destination.ifBlank { "default" }).hashCode()
    val (start, end) = GRADIENTS[(hash.mod(GRADIENTS.size) + GRADIENTS.size).mod(GRADIENTS.size)]
    return DestinationVisual(emoji = emoji, gradientStart = start, gradientEnd = end)
}
