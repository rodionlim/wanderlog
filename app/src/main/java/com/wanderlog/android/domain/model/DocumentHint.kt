package com.wanderlog.android.domain.model

enum class DocumentHint(val label: String) {
    FLIGHT("Flight"),
    HOTEL("Hotel"),
    CAR_RENTAL("Car rental"),
    ACTIVITY("Activity"),
    CRUISE("Cruise"),
    TRAIN("Train");

    /** Extra instruction appended to the OpenAI system prompt. */
    fun promptAddendum(): String = when (this) {
        FLIGHT -> "The user says this document is a FLIGHT booking/confirmation. Focus extraction on flight numbers, origin/destination airports, departure/arrival times, and booking reference. Return flights only unless other categories are unambiguously present."
        HOTEL -> "The user says this document is a HOTEL/ACCOMMODATION booking. Focus extraction on hotel name, address, check-in, check-out, booking reference, total accommodation price, and any host phone number or host contact number when present. Return hotels only unless other categories are unambiguously present."
        CAR_RENTAL -> "The user says this document is a CAR RENTAL booking. Return it as an activity with title like 'Car rental – <company>', and use dateTime for pickup, notes for dropoff/vehicle details. Return activities only unless other categories are unambiguously present."
        ACTIVITY -> "The user says this document is an ACTIVITY, EXCURSION, TOUR, or RESTAURANT booking. Focus extraction on activity title, location, and date/time. Return activities only unless other categories are unambiguously present."
        CRUISE -> "The user says this document is a CRUISE booking. Return it as an activity with title like 'Cruise – <ship>', location set to the departure port, dateTime set to boarding time, and all ports of call in notes."
        TRAIN -> "The user says this document is a TRAIN booking. Return it as a flight with origin/destination set to station names and flightNumber set to the train number."
    }
}
