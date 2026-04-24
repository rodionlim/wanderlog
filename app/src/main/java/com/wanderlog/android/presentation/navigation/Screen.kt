package com.wanderlog.android.presentation.navigation

sealed class Screen(val route: String) {
    data object TripList : Screen("trip_list")

    data object TripForm : Screen("trip_form?tripId={tripId}") {
        fun createRoute(tripId: String? = null) =
            if (tripId != null) "trip_form?tripId=$tripId" else "trip_form"
        const val ARG_TRIP_ID = "tripId"
    }

    data object Itinerary : Screen("itinerary/{tripId}") {
        fun createRoute(tripId: String) = "itinerary/$tripId"
        const val ARG_TRIP_ID = "tripId"
    }

    data object Map : Screen("map/{tripId}?dayId={dayId}") {
        fun createRoute(tripId: String, dayId: String? = null) =
            if (dayId != null) "map/$tripId?dayId=$dayId" else "map/$tripId"
        const val ARG_TRIP_ID = "tripId"
        const val ARG_DAY_ID = "dayId"
    }

    data object Budget : Screen("budget/{tripId}") {
        fun createRoute(tripId: String) = "budget/$tripId"
        const val ARG_TRIP_ID = "tripId"
    }

    data object Packing : Screen("packing/{tripId}") {
        fun createRoute(tripId: String) = "packing/$tripId"
        const val ARG_TRIP_ID = "tripId"
    }

    data object AiGenerate : Screen("ai_generate/{tripId}") {
        fun createRoute(tripId: String) = "ai_generate/$tripId"
        const val ARG_TRIP_ID = "tripId"
    }

    data object Settings : Screen("settings")

    data object ShareImport : Screen("share_import")

    data object Attachments : Screen("attachments/{tripId}") {
        fun createRoute(tripId: String) = "attachments/$tripId"
        const val ARG_TRIP_ID = "tripId"
    }

    data object AttachmentViewer : Screen("attachment_viewer/{attachmentId}") {
        fun createRoute(attachmentId: String) = "attachment_viewer/$attachmentId"
        const val ARG_ATTACHMENT_ID = "attachmentId"
    }
}
