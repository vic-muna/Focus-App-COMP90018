package com.example.app2.model

enum class RewardType {
    SOUND, THEME
}

data class RewardItem(
    val id: String,
    val name: String, // English as requested
    val type: RewardType,
    val thresholdHours: Float,
    val description: String = ""
)

object RewardPresets {
    val ALL_REWARDS = listOf(
        RewardItem("milestone_test", "First Step", RewardType.THEME, 0.01f, "Unlocked after your first short focus session!"),
        RewardItem("milestone_1", "Bronze Focus", RewardType.THEME, 0.1f, "Initial milestone for testing"),
        RewardItem("rainy_cafe", "Rainy Cafe", RewardType.SOUND, 1.0f, "Unlock soothing rain sounds"),
        RewardItem("forest_campfire", "Forest Campfire", RewardType.SOUND, 5.0f, "Unlock crackling fire sounds"),
        RewardItem("space_ambience", "Space Ambience", RewardType.SOUND, 10.0f, "Deep space drone"),
        RewardItem("dark_mode_pro", "Deep Dark Theme", RewardType.THEME, 10.0f, "Professional dark mode"),
        RewardItem("vintage_paper", "Vintage Paper", RewardType.THEME, 20.0f, "Retro writing experience")
    )
}