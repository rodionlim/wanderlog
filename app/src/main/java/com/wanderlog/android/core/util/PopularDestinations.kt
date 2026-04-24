package com.wanderlog.android.core.util

object PopularDestinations {
    val ALL: List<String> = listOf(
        // Australia / NZ
        "Sydney, Australia",
        "Melbourne, Australia",
        "Perth, Australia",
        "Brisbane, Australia",
        "Gold Coast, Australia",
        "Cairns, Australia",
        "Hobart, Australia",
        "Auckland, New Zealand",
        "Queenstown, New Zealand",
        // Southeast Asia
        "Singapore",
        "Bangkok, Thailand",
        "Phuket, Thailand",
        "Chiang Mai, Thailand",
        "Bali, Indonesia",
        "Jakarta, Indonesia",
        "Kuala Lumpur, Malaysia",
        "Ho Chi Minh City, Vietnam",
        "Hanoi, Vietnam",
        "Manila, Philippines",
        "Siem Reap, Cambodia",
        // East Asia
        "Tokyo, Japan",
        "Kyoto, Japan",
        "Osaka, Japan",
        "Seoul, South Korea",
        "Taipei, Taiwan",
        "Kaohsiung, Taiwan",
        "Hong Kong",
        "Shanghai, China",
        "Beijing, China",
        // South Asia
        "Mumbai, India",
        "Delhi, India",
        "Goa, India",
        "Colombo, Sri Lanka",
        "Malé, Maldives",
        "Kathmandu, Nepal",
        // Europe
        "London, UK",
        "Edinburgh, UK",
        "Paris, France",
        "Nice, France",
        "Rome, Italy",
        "Florence, Italy",
        "Venice, Italy",
        "Barcelona, Spain",
        "Madrid, Spain",
        "Lisbon, Portugal",
        "Amsterdam, Netherlands",
        "Berlin, Germany",
        "Munich, Germany",
        "Prague, Czech Republic",
        "Vienna, Austria",
        "Zurich, Switzerland",
        "Interlaken, Switzerland",
        "Athens, Greece",
        "Santorini, Greece",
        "Istanbul, Turkey",
        "Reykjavik, Iceland",
        "Copenhagen, Denmark",
        "Stockholm, Sweden",
        "Oslo, Norway",
        "Dublin, Ireland",
        // Americas
        "New York, USA",
        "Los Angeles, USA",
        "San Francisco, USA",
        "Las Vegas, USA",
        "Chicago, USA",
        "Miami, USA",
        "Honolulu, USA",
        "Seattle, USA",
        "Toronto, Canada",
        "Vancouver, Canada",
        "Banff, Canada",
        "Mexico City, Mexico",
        "Cancun, Mexico",
        "Rio de Janeiro, Brazil",
        "Buenos Aires, Argentina",
        "Lima, Peru",
        "Cusco, Peru",
        // Middle East / Africa
        "Dubai, UAE",
        "Abu Dhabi, UAE",
        "Doha, Qatar",
        "Cairo, Egypt",
        "Marrakech, Morocco",
        "Cape Town, South Africa",
        "Nairobi, Kenya",
        "Zanzibar, Tanzania"
    )

    fun suggestions(query: String, limit: Int = 8): List<String> {
        if (query.isBlank()) return ALL.take(limit)
        val q = query.trim()
        return ALL.asSequence()
            .filter { it.contains(q, ignoreCase = true) }
            .sortedBy { it.indexOf(q, ignoreCase = true) }
            .take(limit)
            .toList()
    }
}
