package by.ntnk.msluschedule.utils

sealed class NetworkApiVersion {
    data object ORIGINAL : NetworkApiVersion() {
        const val name = "ORIGINAL"
    }
}