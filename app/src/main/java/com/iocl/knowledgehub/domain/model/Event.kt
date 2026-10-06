package com.iocl.knowledgehub.domain.model

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val venue: String = "",
    val bannerUrl: String = "",
    val date: String = "",
    val time: String = "",
    val registrationLink: String = "",
    val reminderTime: Long = 0,
    val category: EventCategory = EventCategory.EVENT
)

enum class EventCategory {
    MEETING, EVENT, HOLIDAY, TRAINING
}
