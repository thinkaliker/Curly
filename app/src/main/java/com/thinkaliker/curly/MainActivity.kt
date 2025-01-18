package com.thinkaliker.curly

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity() {

    var reqType = ""
    var endpoint = ""
    var headerText = ""
    var responseText = ""
    var resultText = ""
    var prettyPrintFlag = false

    val requestTypes = arrayOf("GET", "POST")
    val endpoints = arrayOf("https://am.i.mullvad.net/json", "https://internetdb.shodan.io", "http://ip-api.com/json", "https://wttr.in/?format=j1", "CUSTOM")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val spinnerId = findViewById<Spinner>(R.id.reqSpinner)

        val arrayAdapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, requestTypes)
        spinnerId.adapter = arrayAdapter
        spinnerId?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (p2 == 1) {
                    Toast.makeText(this@MainActivity, "POST not implemented", Toast.LENGTH_SHORT).show()
                }
                reqType = requestTypes[p2]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //Toast.makeText(this@MainActivity, "none", Toast.LENGTH_SHORT).show()
                reqType = requestTypes[0]
            }
        }

        val endpointId = findViewById<EditText>(R.id.endpoint)

        val presetEndpointSpinner = findViewById<Spinner>(R.id.presetSpinner)
        val endpointArrayAdapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, endpoints)
        presetEndpointSpinner.adapter = endpointArrayAdapter
        presetEndpointSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (p2 != endpoints.size-1) {
                    endpointId.isEnabled = false
                    endpoint = endpoints[p2]
                    endpointId.setText(endpoint)
                } else {
                    endpointId.isEnabled = true
                    endpoint = endpointId.getText().toString()
                }

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                endpoint = endpoints[0]
                endpointId.setText(endpoint)
            }
        }

        val resultId = findViewById<TextView>(R.id.resultArea)
        resultId.setMovementMethod(ScrollingMovementMethod())

        val prettyPrint = findViewById<Switch>(R.id.jsonSwitch)
        prettyPrint?.setOnCheckedChangeListener({ _ , isChecked ->
            prettyPrintFlag = isChecked
            if (isChecked && resultText != "") {
                resultText = headerText + prettyPrintJson(responseText)

            } else {
                resultText = headerText + responseText
            }
            resultId.setText(resultText)
        })



        val sendId = findViewById<Button>(R.id.button)
        sendId?.setOnClickListener {
            Toast.makeText(this@MainActivity, "Sending request", Toast.LENGTH_SHORT).show()
            endpoint = endpointId.getText().toString()
            sendRequest(reqType, endpoint, resultId, prettyPrintFlag)
        }
    }

    fun prettyPrintJson(json : String) : String {
        return JSONObject(json).toString(2)
    }

    fun sendRequest(reqType : String, endpoint : String, textView: TextView, prettyPrintFlag : Boolean) : String {
        val client = OkHttpClient()
        // TODO different request builder based on reqType
        val request = Request.Builder()
            .addHeader("user-agent", "curl")
            .url(endpoint)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            val errorMsg = "Unsuccessful $response"
                            textView.setText(errorMsg)
                        }
                    } else {

                        headerText = ""
                        responseText = ""
                        resultText = ""
                        for ((name, value) in response.headers) {
                            headerText += "$name: $value\n"
                        }
                        resultText += headerText
                        responseText = response.body!!.string()

                        if (prettyPrintFlag) {
                            resultText += prettyPrintJson(responseText)
                        } else {
                            resultText += responseText
                        }

                        //println(responseText)
                        runOnUiThread {
                            textView.setText(resultText)
                        }
                    }
                }
            }
        })
        return ""
    }
}