package de.wintgen.sprachcountdown

import android.content.Context
import android.content.res.ColorStateList
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {
        private const val PREFS = "einstellungen"
        private const val KEY_TOTAL = "gesamtzeit_sekunden"
        private const val KEY_ANSAGE = "ton_zeitansage"
        private const val KEY_PIPS = "ton_pips"
        private const val KEY_TREFFER = "ton_treffer"
        private const val DEFAULT_TOTAL_S = 200          // 3 Minuten 20 Sekunden
        private const val TICK_MS = 50L
    }

    private lateinit var ring: RingView
    private lateinit var confetti: ConfettiView
    private lateinit var timeText: TextView
    private lateinit var phaseText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var startLabel: TextView
    private lateinit var startButton: MaterialButton
    private lateinit var stopButton: MaterialButton
    private lateinit var resetButton: MaterialButton
    private lateinit var settingsButton: MaterialButton
    private lateinit var infoButton: MaterialButton

    private lateinit var soundPool: SoundPool
    private var klickId = 0
    private var pipsId = 0
    private var jubelId = 0

    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    private val handler = Handler(Looper.getMainLooper())

    private var totalSeconds = DEFAULT_TOTAL_S
    private val totalMs get() = totalSeconds * 1000L

    // Ton-Einstellungen (dauerhaft gespeichert)
    private var ansageOn = true
    private var pipsOn = true
    private var trefferOn = true

    private var remainingMs = 0L          // Restzeit; negativ = Überzeit
    private var endTimeMs = 0L            // Zielzeitpunkt (elapsedRealtime), solange die Uhr läuft
    private var running = false
    private var lastAnnouncedSecond = Int.MIN_VALUE
    private var lastShownSecond = Int.MIN_VALUE

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            remainingMs = endTimeMs - SystemClock.elapsedRealtime()
            refresh()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ring = findViewById(R.id.ring)
        // Konfetti-Ebene über dem gesamten Bildschirm
        confetti = ConfettiView(this)
        addContentView(
            confetti,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        timeText = findViewById(R.id.timeText)
        phaseText = findViewById(R.id.phaseText)
        subtitleText = findViewById(R.id.subtitleText)
        startLabel = findViewById(R.id.startLabel)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)
        settingsButton = findViewById(R.id.settingsButton)
        infoButton = findViewById(R.id.infoButton)

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        klickId = soundPool.load(this, R.raw.klick, 1)
        pipsId = soundPool.load(this, R.raw.pips, 1)
        jubelId = soundPool.load(this, R.raw.tusch, 1)

        getSharedPreferences(PREFS, Context.MODE_PRIVATE).let {
            totalSeconds = it.getInt(KEY_TOTAL, DEFAULT_TOTAL_S)
            ansageOn = it.getBoolean(KEY_ANSAGE, true)
            pipsOn = it.getBoolean(KEY_PIPS, true)
            trefferOn = it.getBoolean(KEY_TREFFER, true)
        }

        tts = TextToSpeech(this, this)

        startButton.setOnClickListener { it.feedback(); start() }
        stopButton.setOnClickListener { it.feedback(); stop() }
        resetButton.setOnClickListener { it.feedback(); reset() }
        settingsButton.setOnClickListener { it.feedback(); showTimeDialog() }
        infoButton.setOnClickListener { it.feedback(); showInfoDialog() }

        reset()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.GERMAN)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            tts.setSpeechRate(1.1f)
        }
        if (!ttsReady) {
            Toast.makeText(
                this,
                "Deutsche Sprachausgabe nicht verfügbar – bitte in den Android-Einstellungen installieren.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---------- Steuerung ----------

    private fun start() {
        if (running) return
        running = true
        // Anzeige neu auswerten, damit z. B. bei Startzeit 3:00 die erste Ansage sofort kommt
        lastShownSecond = Int.MIN_VALUE
        endTimeMs = SystemClock.elapsedRealtime() + remainingMs
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setButtons()
        handler.post(ticker)
    }

    private fun stop() {
        if (!running) return
        running = false
        handler.removeCallbacks(ticker)
        remainingMs = endTimeMs - SystemClock.elapsedRealtime()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        refresh()
        setButtons()
        // Punktlandung: angehalten, während genau 00:00 angezeigt wird
        if (Announcer.secondsFor(remainingMs) == 0) celebrate()
    }

    private fun celebrate() {
        tts.stop()
        if (trefferOn && jubelId != 0) soundPool.play(jubelId, 1f, 1f, 2, 0, 1f)
        confetti.burst()
        val green = ContextCompat.getColor(this, R.color.phase_calm)
        phaseText.text = "Punktlandung!"
        phaseText.setTextColor(green)
        ring.setRingColor(green)
        timeText.setTextColor(green)
        timeText.animate().cancel()
        timeText.scaleX = 1.25f
        timeText.scaleY = 1.25f
        timeText.animate().scaleX(1f).scaleY(1f).setDuration(600).start()
    }

    private fun reset() {
        running = false
        handler.removeCallbacks(ticker)
        if (::tts.isInitialized) tts.stop()
        if (::confetti.isInitialized) confetti.clear()
        remainingMs = totalMs
        lastAnnouncedSecond = Int.MIN_VALUE
        lastShownSecond = Int.MIN_VALUE
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        subtitleText.text = "Flugzeit " + "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
        refresh()
        setButtons()
    }

    private fun setButtons() {
        val canStart = !running
        startButton.isEnabled = canStart
        startButton.alpha = if (canStart) 1f else 0.35f
        stopButton.isEnabled = running
        stopButton.alpha = if (running) 1f else 0.35f
        // Zeit nur einstellbar, solange die Uhr nicht läuft
        settingsButton.isEnabled = !running
        settingsButton.alpha = if (!running) 1f else 0.35f
        startLabel.text = if (remainingMs != totalMs) "Weiter" else getString(R.string.start)
    }

    // ---------- Anzeige & Ansage ----------

    private fun refresh() {
        ring.progress = (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
        val seconds = Announcer.secondsFor(remainingMs)
        if (seconds == lastShownSecond) return
        lastShownSecond = seconds

        timeText.text = Announcer.displayFor(seconds)
        val (label, colorRes) = when {
            seconds < 0 -> "Überzeit" to R.color.phase_final
            seconds == 0 -> "Landen" to R.color.phase_final
            seconds <= 10 -> "Ansage jede Sekunde" to R.color.phase_final
            seconds <= 60 -> "Ansage alle 10 s" to R.color.phase_warn
            else -> "Ansage alle 15 s" to R.color.phase_calm
        }
        val color = ContextCompat.getColor(this, colorRes)
        phaseText.text = label
        phaseText.setTextColor(color)
        timeText.setTextColor(
            if (seconds < 0) color else ContextCompat.getColor(this, R.color.text_primary)
        )
        ring.setRingColor(color)
        startButton.backgroundTintList = ColorStateList.valueOf(color)

        if (running) {
            if (seconds in 0..10) pulse()
            // Überzeit: jede abgelaufene Sekunde ein kurzer Pips
            if (seconds < 0 && pipsOn && pipsId != 0) soundPool.play(pipsId, 1f, 1f, 1, 0, 1f)
            if (ansageOn && seconds != lastAnnouncedSecond && Announcer.shouldAnnounce(seconds, totalSeconds)) {
                lastAnnouncedSecond = seconds
                speak(Announcer.textFor(seconds))
            }
        }
    }

    private fun pulse() {
        timeText.animate().cancel()
        timeText.scaleX = 1.12f
        timeText.scaleY = 1.12f
        timeText.animate().scaleX(1f).scaleY(1f).setDuration(350).start()
    }

    /** Klickgeräusch + kurze Vibration bei jedem Button */
    private fun View.feedback() {
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        if (klickId != 0) soundPool.play(klickId, 1f, 1f, 1, 0, 1f)
    }

    private fun speak(text: String) {
        if (!ttsReady) return
        // QUEUE_FLUSH: eine neue Ansage unterbricht eine noch laufende,
        // damit die Sekunden-Ansagen am Ende nicht hinterherhinken.
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ansage_$text")
    }

    // ---------- Einstellungen ----------

    private fun showTimeDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_time, null)
        val pickMin = view.findViewById<NumberPicker>(R.id.pickMinutes).apply {
            minValue = 0; maxValue = 30; value = totalSeconds / 60
        }
        val pickSec = view.findViewById<NumberPicker>(R.id.pickSeconds).apply {
            minValue = 0; maxValue = 59; value = totalSeconds % 60
            setFormatter { "%02d".format(it) }
        }
        val cbAnsage = view.findViewById<CheckBox>(R.id.cbAnsage).apply { isChecked = ansageOn }
        val cbPips = view.findViewById<CheckBox>(R.id.cbPips).apply { isChecked = pipsOn }
        val cbTreffer = view.findViewById<CheckBox>(R.id.cbTreffer).apply { isChecked = trefferOn }
        listOf(cbAnsage, cbPips, cbTreffer).forEach { cb -> cb.setOnClickListener { playKlick() } }

        fun saveSounds() {
            ansageOn = cbAnsage.isChecked
            pipsOn = cbPips.isChecked
            trefferOn = cbTreffer.isChecked
            getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ANSAGE, ansageOn)
                .putBoolean(KEY_PIPS, pipsOn)
                .putBoolean(KEY_TREFFER, trefferOn)
                .apply()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Einstellungen")
            .setView(view)
            .setNeutralButton("Standard 3:20") { _, _ -> playKlick(); saveSounds(); saveTotal(DEFAULT_TOTAL_S) }
            .setNegativeButton("Abbrechen") { _, _ -> playKlick() }
            .setPositiveButton("Speichern") { _, _ ->
                playKlick()
                saveSounds()
                val total = pickMin.value * 60 + pickSec.value
                if (total <= 0) {
                    Toast.makeText(this, "Die Zeit muss größer als 0 sein.", Toast.LENGTH_SHORT).show()
                } else {
                    saveTotal(total)
                }
            }
            .show()
    }

    // ---------- Info ----------

    private fun showInfoDialog() {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) { "1.0" }
        val text = """
            Erstellt von: Stefan Wintgen
            Version $version (2026-11-05)

            Funktion
            Countdown für die Flugzeit im F3B-E-Wettbewerb (Standard 3:20) mit Sprachansage:
            • ab der letzten vollen Minute alle 15 Sekunden (3:00, 2:45, 2:30 …)
            • ab 1 Minute alle 10 Sekunden
            • ab 10 Sekunden jede Sekunde
            • bei 0 die Ansage „Landen“

            Danach läuft die Zeit als Überzeit mit Minus weiter (-00:01, -00:02 …), jede Sekunde ertönt ein kurzer Pips.

            Wird die Uhr genau bei 00:00 angehalten, gibt es ein „Tätärätä“ und Konfetti für die Punktlandung.

            Bedienung
            ▶ Start / Weiter   ❚❚ Stopp   ↺ Reset
            ⚙ Zahnrad: Flugzeit einstellen und Töne (Zeitansage, Pipston, Treffer-Zeit) ein- oder ausschalten – wird dauerhaft gespeichert
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.app_name))
            .setMessage(text)
            .setPositiveButton("OK") { _, _ -> playKlick() }
            .show()
    }

    private fun playKlick() {
        if (klickId != 0) soundPool.play(klickId, 1f, 1f, 1, 0, 1f)
    }

    private fun saveTotal(seconds: Int) {
        totalSeconds = seconds
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_TOTAL, seconds).apply()
        reset()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        tts.stop()
        tts.shutdown()
        soundPool.release()
        super.onDestroy()
    }
}
