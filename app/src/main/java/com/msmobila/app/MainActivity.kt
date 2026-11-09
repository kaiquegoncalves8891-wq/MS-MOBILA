package com.msmobila.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var stage: FrameLayout
    private lateinit var status: TextView
    private val prefs by lazy { getSharedPreferences("ms_mobila", MODE_PRIVATE) }
    private val markers = mutableListOf<Marker>()

    data class Marker(val label: String, val x: Float, val y: Float)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        stage.post { loadProfile() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(15, 16, 20))
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val title = TextView(this).apply {
            text = "MS MOBILA"
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(42), 1f))
        status = TextView(this).apply {
            text = "EDITOR DE CONTROLES • PROTÓTIPO"
            textSize = 11f
            setTextColor(Color.LTGRAY)
        }
        header.addView(status)
        page.addView(header)

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listOf("ANALÓGICO", "MIRA", "TIRO", "PULAR", "AGACHAR", "SALVAR", "CARREGAR").forEach { name ->
            val b = Button(this).apply {
                text = name
                textSize = 10f
                isAllCaps = false
                setOnClickListener {
                    when (name) {
                        "SALVAR" -> saveProfile()
                        "CARREGAR" -> loadProfile()
                        else -> addMarker(name)
                    }
                }
            }
            tools.addView(b, LinearLayout.LayoutParams(0, dp(46), 1f).apply {
                marginEnd = dp(4)
            })
        }
        page.addView(tools)

        stage = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(32, 34, 40))
        }
        page.addView(stage, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { topMargin = dp(8); bottomMargin = dp(8) })

        val sensitivity = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        sensitivity.addView(label("SENSIBILIDADE X"))
        val sx = SeekBar(this).apply { max = 200; progress = 100 }
        sensitivity.addView(sx, LinearLayout.LayoutParams(0, dp(38), 1f))
        sensitivity.addView(label("Y"))
        val sy = SeekBar(this).apply { max = 200; progress = 100 }
        sensitivity.addView(sy, LinearLayout.LayoutParams(0, dp(38), 1f))
        page.addView(sensitivity)
        setContentView(page)
    }

    private fun label(s: String) = TextView(this).apply {
        text = s
        textSize = 11f
        setTextColor(Color.WHITE)
        setPadding(dp(5), 0, dp(5), 0)
    }

    private fun addMarker(name: String) {
        val size = if (name == "ANALÓGICO") dp(78) else dp(54)
        val v = TextView(this).apply {
            text = name
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.rgb(205, 35, 45))
            elevation = dp(5).toFloat()
        }
        val lp = FrameLayout.LayoutParams(size, size)
        lp.leftMargin = (stage.width / 2 - size / 2).coerceAtLeast(0)
        lp.topMargin = (stage.height / 2 - size / 2).coerceAtLeast(0)
        stage.addView(v, lp)
        makeDraggable(v)
        status.text = "$name adicionado • arraste para posicionar"
    }

    private fun makeDraggable(view: View) {
        var downX = 0f
        var downY = 0f
        var startLeft = 0
        var startTop = 0
        view.setOnTouchListener { v, event ->
            val lp = v.layoutParams as FrameLayout.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startLeft = lp.leftMargin; startTop = lp.topMargin
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.leftMargin = (startLeft + event.rawX - downX).toInt()
                        .coerceIn(0, (stage.width - v.width).coerceAtLeast(0))
                    lp.topMargin = (startTop + event.rawY - downY).toInt()
                        .coerceIn(0, (stage.height - v.height).coerceAtLeast(0))
                    v.layoutParams = lp
                    true
                }
                else -> true
            }
        }
    }

    private fun saveProfile() {
        val arr = JSONArray()
        for (i in 0 until stage.childCount) {
            val v = stage.getChildAt(i)
            if (v is TextView) {
                val lp = v.layoutParams as FrameLayout.LayoutParams
                val obj = JSONObject()
                obj.put("label", v.text.toString())
                obj.put("x", if (stage.width > 0) lp.leftMargin.toFloat() / stage.width else 0.5f)
                obj.put("y", if (stage.height > 0) lp.topMargin.toFloat() / stage.height else 0.5f)
                arr.put(obj)
            }
        }
        prefs.edit().putString("profile", arr.toString()).apply()
        status.text = "Perfil salvo neste celular"
    }

    private fun loadProfile() {
        val raw = prefs.getString("profile", null)
        if (raw.isNullOrBlank()) {
            status.text = "Adicione controles e toque em SALVAR"
            return
        }
        stage.removeAllViews()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val name = obj.optString("label", "CONTROLE")
            val size = if (name == "ANALÓGICO") dp(78) else dp(54)
            val v = TextView(this).apply {
                text = name
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundColor(Color.rgb(205, 35, 45))
                elevation = dp(5).toFloat()
            }
            val lp = FrameLayout.LayoutParams(size, size)
            lp.leftMargin = (obj.optDouble("x", 0.5).toFloat() * stage.width).toInt()
                .coerceIn(0, (stage.width - size).coerceAtLeast(0))
            lp.topMargin = (obj.optDouble("y", 0.5).toFloat() * stage.height).toInt()
                .coerceIn(0, (stage.height - size).coerceAtLeast(0))
            stage.addView(v, lp)
            makeDraggable(v)
        }
        status.text = "Perfil carregado"
    }
}
