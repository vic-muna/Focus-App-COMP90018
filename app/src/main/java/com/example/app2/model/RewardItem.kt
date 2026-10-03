package com.example.app2.model

import com.example.app2.R

enum class RewardType {
    SOUND, THEME, BACKGROUND
}

data class RewardItem(
    val id: String,
    val name: String,
    val type: RewardType,
    val thresholdHours: Float,
    val description: String = "",
    val rawResId: Int? = null,
    val costPoints: Int = 10
)

object RewardPresets {
    val BACKGROUND_FOREST = RewardItem(
        id = "bg_forest",
        name = "Forest",
        type = RewardType.BACKGROUND,
        thresholdHours = 0.0f,
        description = "A peaceful, green forest background.",
        rawResId = R.raw.forest1,
        costPoints = 0
    )

    val BACKGROUND_BEACH = RewardItem(
        id = "bg_beach",
        name = "Beach",
        type = RewardType.BACKGROUND,
        thresholdHours = 0.0f,
        description = "A relaxing beach background.",
        rawResId = R.raw.beach1,
        costPoints = 10
    )

    val BACKGROUND_STUDYROOM = RewardItem(
        id = "bg_studyroom",
        name = "Study Room",
        type = RewardType.BACKGROUND,
        thresholdHours = 0.0f,
        description = "A cozy study room background.",
        rawResId = R.raw.studyroom1,
        costPoints = 10
    )

    val BACKGROUND_ANIMAL = RewardItem(
        id = "bg_animal",
        name = "Animals",
        type = RewardType.BACKGROUND,
        thresholdHours = 0.0f,
        description = "A lively animal animation background.",
        rawResId = R.raw.animal1,
        costPoints = 10
    )

    val BACKGROUND_NIGHT = RewardItem(
        id = "bg_night",
        name = "Night",
        type = RewardType.BACKGROUND,
        thresholdHours = 0.0f,
        description = "A peaceful night animation background.",
        rawResId = R.raw.night1,
        costPoints = 10
    )

    val BACKGROUND_DESERT = RewardItem(
        id = "bg_desert",
        name = "Desert",
        type = RewardType.BACKGROUND,
        thresholdHours = 0.0f,
        description = "A vast, warm desert background.",
        rawResId = R.raw.desert1,
        costPoints = 10
    )

    val BACKGROUND_EAST = RewardItem(
        id = "bg_east",
        name = "East",
        type = RewardType.BACKGROUND,
        thresholdHours = 0.0f,
        description = "A serene eastern landscape background.",
        rawResId = R.raw.east1,
        costPoints = 10
    )

    val BACKGROUND_SNOW = RewardItem(
        id = "bg_snow",
        name = "Snow",
        type = RewardType.BACKGROUND,
        thresholdHours = 0.0f,
        description = "A peaceful snowy landscape background.",
        rawResId = R.raw.snow1,
        costPoints = 10
    )

    val BACKGROUND_ROCK = RewardItem(
        id = "bg_rock",
        name = "Rock",
        type = RewardType.BACKGROUND,
        thresholdHours = 0.0f,
        description = "A tranquil rocky landscape background.",
        rawResId = R.raw.rock1,
        costPoints = 10
    )

    val ALL_BACKGROUNDS = listOf(
        BACKGROUND_FOREST,
        BACKGROUND_BEACH,
        BACKGROUND_STUDYROOM,
        BACKGROUND_ANIMAL,
        BACKGROUND_NIGHT,
        BACKGROUND_DESERT,
        BACKGROUND_EAST,
        BACKGROUND_SNOW,
        BACKGROUND_ROCK
    )

    val ALL_REWARDS = listOf(
        RewardItem("milestone_test", "First Step", RewardType.THEME, 5.0f, "Unlocked after 5 hours of focus time!"),
        RewardItem("milestone_1", "Bronze Focus", RewardType.THEME, 10.0f, "Unlocked after 10 hours of focus time!")
    )
}