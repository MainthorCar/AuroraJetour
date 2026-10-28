package com.aurora.store.patch

object ConstantsPatch {
    const val FLAVOUR_BYD = "jetour"
    const val FLAVOUR_BYD_PACKAGE_NAME = "com.mainthorcar.aurorajetour"

    // Aurora Store Jetour: de dónde se bajan las actualizaciones de la propia app
    const val SELF_UPDATE_REPO_URL = "https://github.com/MainthorCar/AuroraJetour"
    const val SELF_UPDATE_FEED_URL = "${SELF_UPDATE_REPO_URL}/releases/latest/download/release_feed.json"
    const val GITHUB_API_URL = "https://api.github.com/repositories"
    const val GITHUB_APPS_REPO_ID = "1301975707"
    const val UPDATE_APPS_URL = "${GITHUB_API_URL}/${GITHUB_APPS_REPO_ID}/releases/latest"

    const val HIDE_SHOW_WELCOME_DIALOG = "HIDE_SHOW_WELCOME_DIALOG"

    const val PATCH_APP_COMPONENT_FACTORY_CLASS = "com.kangrio.extension.SpoofAppComponentFactory"
    const val META_DATA_SPOOFED_CERTIFICATES = "org.microg.gms.spoofed_certificates"
    const val META_DATA_PATCH_VERSION_CODE = "com.kangrio.extension.patch_version_code"
    const val META_DATA_ENABLE_CRASH_REPORT = "com.kangrio.extensions.enable_crash_report"
}