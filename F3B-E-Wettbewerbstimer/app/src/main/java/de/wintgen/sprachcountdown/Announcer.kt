package de.wintgen.sprachcountdown

/**
 * Reine Logik, wann und was angesagt wird (ohne Android-Abhängigkeiten).
 *
 * Regeln:
 *  - Erste Ansage bei der letzten vollen Minute ≤ Startzeit (bei 3:20 also 3:00)
 *  - danach alle 15 Sekunden (2:45, 2:30, 2:15 … 1:15)
 *  - ab 1 Minute alle 10 Sekunden (1:00, 0:50 … 0:20)
 *  - ab 10 Sekunden jede Sekunde (10, 9 … 1)
 *  - bei 0: „Landen"
 */
object Announcer {

    const val LANDEN = "Landen"

    fun shouldAnnounce(seconds: Int, totalSeconds: Int): Boolean {
        if (seconds < 0) return false
        if (seconds == 0) return true
        if (seconds > totalSeconds) return false
        if (seconds <= 10) return true
        if (seconds <= 60) return seconds % 10 == 0
        val firstAnnouncement = (totalSeconds / 60) * 60
        return seconds % 15 == 0 && seconds <= firstAnnouncement
    }

    fun textFor(seconds: Int): String {
        if (seconds == 0) return LANDEN
        if (seconds <= 10) return seconds.toString()
        val m = seconds / 60
        val s = seconds % 60
        val minPart = when (m) {
            0 -> ""
            1 -> "1 Minute"
            else -> "$m Minuten"
        }
        val secPart = if (s == 0) "" else "$s Sekunden"
        return listOf(minPart, secPart).filter { it.isNotEmpty() }.joinToString(" ")
    }

    /** Anzeige: 03:20 … 00:01, 00:00, danach -00:01, -00:02 … */
    fun displayFor(seconds: Int): String {
        val abs = kotlin.math.abs(seconds)
        val sign = if (seconds < 0) "-" else ""
        return sign + "%02d:%02d".format(abs / 60, abs % 60)
    }

    /**
     * Rechnet Restzeit in Millisekunden in die angezeigte Sekunde um.
     * Positiv wird aufgerundet (3:19.4 → 3:20), damit "0" genau beim Ablauf erscheint.
     * Negativ (Überzeit) wird abgerundet: 0,4 s drüber → -00:00 wird als 0 gezählt,
     * 1,0 s drüber → -1.
     */
    fun secondsFor(remainingMs: Long): Int =
        if (remainingMs > 0) ((remainingMs + 999) / 1000).toInt()
        else -((-remainingMs) / 1000).toInt()
}
