package com.echo.launcher

sealed class CommandResult {
    data class LaunchApp(val app: InstalledApp) : CommandResult()
    data class WebSearch(val query: String) : CommandResult()
    data class NotUnderstood(val raw: String) : CommandResult()
}

object VoiceCommandHandler {

    private val OPEN_PATTERN = Regex("^(open|launch|start|go to)\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val SEARCH_PATTERN = Regex("^(search for|search|find|look up|google)\\s+(.+)$", RegexOption.IGNORE_CASE)

    fun parse(raw: String, apps: List<InstalledApp>): CommandResult {
        val text = raw.trim()
        if (text.isEmpty()) return CommandResult.NotUnderstood(raw)

        OPEN_PATTERN.find(text)?.let { m ->
            val target = m.groupValues[2].trim()
            findApp(target, apps)?.let { return CommandResult.LaunchApp(it) }
            return CommandResult.WebSearch(target)
        }

        SEARCH_PATTERN.find(text)?.let { m ->
            return CommandResult.WebSearch(m.groupValues[2].trim())
        }

        // No explicit verb — try matching the whole phrase to an app name first,
        // then fall back to a web search.
        findApp(text, apps)?.let { return CommandResult.LaunchApp(it) }
        return CommandResult.WebSearch(text)
    }

    private fun findApp(target: String, apps: List<InstalledApp>): InstalledApp? {
        val t = target.lowercase()
        return apps.firstOrNull { it.label.lowercase() == t }
            ?: apps.firstOrNull { it.label.lowercase().contains(t) }
            ?: apps.firstOrNull { t.contains(it.label.lowercase()) }
    }
}
