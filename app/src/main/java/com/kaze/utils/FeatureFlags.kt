package com.kaze.utils

import com.kaze.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object FeatureFlags {
    /**
     * Mechanism to allow kenzbilal to test updates before they hit general users.
     */
    fun isBetaTester(userRepository: UserRepository): Flow<Boolean> {
        return kotlinx.coroutines.flow.flowOf(true)
    }
}
