package com.example.focusapp.domain.usecase

import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.RewardProgress

/**
 * CalculateFocusRewardUseCase
 * ------------------------------
 * Domain-layer rule: turns a list of past [FocusSession]s into a
 * [RewardProgress] (streak + points).
 *
 * NOTE: the team has not yet finalized the reward formula - the plan
 * currently mixes two different ideas (streak/completion-based rewards
 * vs. an easier bar for users who get distracted more often). Decide which
 * model to use and implement it here.
 *
 * TODO: to be implemented later - currently always returns a default/empty
 * result.
 */
class CalculateFocusRewardUseCase {

    fun execute(sessions: List<FocusSession>): RewardProgress {
        return RewardProgress()
    }
}
