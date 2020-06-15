package com.paulomach.domotico

import android.content.Context
import android.graphics.Color.rgb
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.PersistableBundle
import android.os.StrictMode
import android.text.Editable
import android.text.format.Formatter
import android.text.method.ScrollingMovementMethod
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText

import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import android.os.StrictMode.ThreadPolicy as ThreadPolicy1


class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).build()
    private val textView by lazy { findViewById<TextView>(R.id.text) }
    private val timerButton by lazy { findViewById<Button>(R.id.timer) }
    private val statusButton by lazy { findViewById<Button>(R.id.status) }
    private val urlText by lazy { findViewById<TextInputEditText>(R.id.url_config) }
    private val checkBox by lazy { findViewById<CheckBox>(R.id.checkBox) }
    private var baseURL = String()
    private var timerState = String()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StrictMode.setThreadPolicy(ThreadPolicy1.Builder().permitAll().build())

        setContentView(R.layout.activity_main)

        textView.movementMethod = ScrollingMovementMethod()

        fab?.setOnClickListener {
            update_status()
            textView.text =
                "Read Status at " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + "\n" +
                        textView.text
        }

        statusButton?.setOnClickListener {
            run(
                baseURL + "json.htm?type=command&param=switchlight&idx=2&switchcmd=Toggle", "On"
            )
            update_status()
        }

        timerButton?.setOnClickListener {
            var command = String()
            if (timerState == "On") {
                command = "disabletimer"
            } else {
                command = "enabletimer"
            }

            run(
                baseURL + "json.htm?type=command&param=" + command + "&idx=3", "On"
            )
            update_status()
        }

    }


    @SuppressWarnings("deprecation")
    fun getUrl() {
        val wf = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val winfo = wf.connectionInfo as WifiInfo

        val ip = Formatter.formatIpAddress(winfo.ipAddress)

        if (checkBox.isChecked) {
            baseURL = urlText.text.toString()
        } else {
            baseURL = if (ip.contains("168.86") || ip.contains("168.15")) {
                "http://raspberrypi:8080/"
            } else {
                "https://paulohome.hopto.org:8888/"
            }
            urlText.text = Editable.Factory.getInstance().newEditable(baseURL)
        }
        textView.text = "Using endpoint as " + baseURL
    }

    override fun onStart() {
        super.onStart()
        update_status()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun parse(text: String, target: String) {
        try {
            val json = JSONObject(text)

            if (target == "update") {
                val aux = json.get("result")
                var aux2 = String()
                aux2 = ((aux as JSONArray).get(0) as JSONObject).get("Status") as String
                if (aux2 == "Off") {
                    statusButton.setBackgroundColor(rgb(135, 135, 135))
                    statusButton.text = "Boiler OFF"
                    statusButton.setTextColor(rgb(255, 255, 255))
                } else {
                    statusButton.setBackgroundColor(rgb(255, 0, 0))
                    statusButton.text = "Boiler ON"
                    statusButton.setTextColor(rgb(255, 255, 255))
                }
            } else if (target == "timer") {
                val aux = json.get("result")
                var aux2 = String()
                aux2 = ((aux as JSONArray).get(1) as JSONObject).get("Active") as String
                if (aux2 == "true") {
                    timerButton.setBackgroundColor(rgb(0, 170, 0))
                    timerButton.text = "Timer Active"
                    timerButton.setTextColor(rgb(255, 255, 255))
                    timerState = "On"
                } else {
                    timerButton.setBackgroundColor(rgb(135, 135, 135))
                    timerButton.text = "Timer Inactive"
                    timerButton.setTextColor(rgb(255, 255, 255))
                    timerState = "Off"
                }
            } else {
                textView.text = json.toString()
            }

        } catch (e: Exception) {
            textView.text = "Error on parse"
        }
    }

    fun run(url: String, target: String) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Basic cGF1bG86b3JsYW5kbzQ=")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                parse(response.body()!!.string(), target)
            }
        } catch (e: Exception) {
            textView.text = "TIMEOUT on connecting to host:\n" + baseURL
        }
    }

    fun update_status() {
        getUrl()
        run(baseURL + "json.htm?type=devices&rid=2", "update")
        run(baseURL + "json.htm?type=schedules", "timer")
    }
}
