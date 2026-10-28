package com.aurora.store.patch

import com.aurora.store.BuildConfig

object UtilPatch {
    fun isBydFlavour(): Boolean {
        return BuildConfig.FLAVOR == ConstantsPatch.FLAVOUR_BYD
    }

    /**
     * Aurora Store Jetour: las actualizaciones de la app se buscan en
     * github.com/MainthorCar/AuroraJetour (último release, archivo release_feed.json).
     */
    @Suppress("RedundantSuspendModifier")
    suspend fun getGithubRepoUrl(): String? = ConstantsPatch.SELF_UPDATE_FEED_URL
}
