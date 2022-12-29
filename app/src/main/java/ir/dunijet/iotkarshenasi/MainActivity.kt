package ir.dunijet.iotkarshenasi

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import ir.dunijet.iotkarshenasi.databinding.ActivityMainBinding
import ir.dunijet.iotkarshenasi.databinding.DialogGetIpBinding
import ir.dunijet.iotkarshenasi.databinding.DialogRgbLampBinding
import ir.dunijet.iotkarshenasi.databinding.DialogSpeakerVolumeBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    var dismissByAction = true
    var ip = "null"
    var packet = "null"

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtConnecting.setOnClickListener {
            ifNotConnected()
        }

        binding.cardLamp1.setOnClickListener {
            if (binding.txtLamp1.text == "ON") {
                Toast.makeText(this, "make lamp 1 off", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "make lamp 1 on", Toast.LENGTH_SHORT).show()
            }
        }
        binding.cardLamp2.setOnClickListener {
            if (binding.txtLamp1.text == "ON") {
                Toast.makeText(this, "make lamp 2 off", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "make lamp 2 on", Toast.LENGTH_SHORT).show()
            }
        }
        binding.cardLamp3.setOnClickListener {
            if (binding.txtLamp1.text == "ON") {
                Toast.makeText(this, "make lamp 3 off", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "make lamp 3 on", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardSpeaker.setOnClickListener {
            val dialog = AlertDialog.Builder(this).create()


            val view = DialogSpeakerVolumeBinding.inflate(layoutInflater)
            view.sliderSpeaker.value = packet.split("/")[10].first().toString().toInt().toFloat()

            dialog.setView(view.root)

            view.btnDoneSpeaker.setOnClickListener {
                val valueSpeaker = view.sliderSpeaker.value.toInt()
                Toast.makeText(this, "change value speaker to " + valueSpeaker, Toast.LENGTH_SHORT)
                    .show()
            }

            dialog.show()
        }

        binding.cardRGB.setOnClickListener {
            val dialog = AlertDialog.Builder(this).create()

            val view = DialogRgbLampBinding.inflate(layoutInflater)
            view.sliderRed.value = packet.split("/")[1].toInt().toFloat()
            view.sliderGreen.value = packet.split("/")[2].toInt().toFloat()
            view.sliderBlue.value = packet.split("/")[3].toInt().toFloat()

            dialog.setView(view.root)

            view.btnDoneRGB.setOnClickListener {
                val r = view.sliderRed.value.toInt()
                val g = view.sliderGreen.value.toInt()
                val b = view.sliderBlue.value.toInt()
                Toast.makeText(
                    this,
                    "change rgb to " + r + " , " + g + " , " + b,
                    Toast.LENGTH_SHORT
                ).show()
            }

            dialog.show()
        }

        binding.txtPlainText.setOnClickListener {
            Toast.makeText(this, "clicked on me", Toast.LENGTH_SHORT).show()
            binding.imgRGB.setImageResource(R.drawable.pic_online)

        }

    }

    override fun onResume() {
        super.onResume()

        afterWhile {
            ifNotConnected()
        }

    }

    private fun ifNotConnected() {

        if (ip == "null" && binding.txtConnecting.text == "Disconnected") {

            binding.txtConnecting.text = "Connecting..."
            binding.txtConnecting.rightIcon(R.drawable.ic_connectiong)

            val dialog = AlertDialog.Builder(this).create()
            val view = DialogGetIpBinding.inflate(layoutInflater)

            dialog.setView(view.root)
            dialog.setOnDismissListener {
                if (dismissByAction == true) {
                    binding.txtConnecting.text = "Disconnected"
                    binding.txtConnecting.rightIcon(R.drawable.ic_connection_failed)
                    dismissByAction = true
                }

            }

            view.btnDoneConnection.setOnClickListener {

                if (view.edtConnection.text.toString().isNotEmpty()) {

                    if (view.edtConnection.text.toString().split(".").size == 4) {

                        ip = view.edtConnection.text.toString()
                        getDataFromEsp {

                            if (it == "error") {

                                Toast.makeText(this, "error connecting...", Toast.LENGTH_SHORT)
                                    .show()

                                dismissByAction = true
                                dialog.dismiss()

                            } else {

                                dismissByAction = false
                                dialog.dismiss()

                                binding.txtConnecting.text = "Connected"
                                binding.txtConnecting.rightIcon(R.drawable.ic_connection_ok)

                                packet = it
                                afterWhile {
                                    resolvePacket()
                                    startAutomaticUpdating()
                                }

                            }

                        }

                    } else {
                        Toast.makeText(this, "please write true ip", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Toast.makeText(this, "please write ip", Toast.LENGTH_SHORT).show()
                }

            }

            dialog.show()
        }
    }

    fun getDataFromEsp(Response: (String) -> Unit) {
        // Response.invoke("0/148/254/100/1368436083157/27/56/1/0/1/8")

        AndroidNetworking.get("http://" + ip + "/api")
            .setPriority(Priority.LOW)
            .build()
            .getAsString(object : StringRequestListener {
                override fun onResponse(response: String?) {
                    Response.invoke(response ?: "error")
                }

                override fun onError(anError: ANError?) {
                    Response.invoke("error -> " + anError?.message)
                }
            })
    }

    @SuppressLint("SetTextI18n", "ResourceAsColor")
    fun resolvePacket() {

        val worker = packet.split("/")

        if (worker[0] == "0") {
            binding.status1.setImageResource(R.drawable.pic_offline)
            binding.status2.setImageResource(R.drawable.pic_offline)
        } else {
            binding.status1.setImageResource(R.drawable.pic_online)
            binding.status2.setImageResource(R.drawable.pic_online)
        }

        binding.tvTempeatur.text = worker[5] + " " + 0x00B0.toChar() + "C"

        binding.tvHumadity.text = "humadity " + worker[6] + "%"

        binding.tvLamp1Value.text = worker[1]
        binding.tvLamp2Value.text = worker[2]
        binding.tvLamp3Value.text = worker[3]

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val color = Color.rgb(
                normalizeNumber(worker[1].toFloat()),
                normalizeNumber(worker[2].toFloat()),
                normalizeNumber(worker[3].toFloat())
            )
            binding.imgRGB.setBackgroundColor(color)
        }

        if (worker[7] == "1") {
            binding.txtLamp1.text = "ON"
        } else {
            binding.txtLamp1.text = "OFF"
        }

        if (worker[8] == "1") {
            binding.txtLamp2.text = "ON"
        } else {
            binding.txtLamp2.text = "OFF"
        }

        if (worker[9] == "1") {
            binding.txtLamp3.text = "ON"
        } else {
            binding.txtLamp3.text = "OFF"
        }

        binding.tvSpeaker.text = worker[10].first().toString() + "/10"

        binding.tvTime.text = getTime(worker[4])
        binding.tvDate.text = getDate(worker[4])

        binding.txtPlainText.text = packet + "Interval 2 Seconds"

    }

    fun getDate(epoch: String): String {
        val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH)
        return (simpleDateFormat.format(epoch.toLong() * 1000L)).split(",")[0]
    }

    fun getTime(epoch: String): String {
        val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH)
        return ((simpleDateFormat.format(epoch.toLong() * 1000L)).split(",")[1]).substring(1)
    }

    fun startAutomaticUpdating() {

        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {

                getDataFromEsp {

                    runOnUiThread {
                        if (it == "error") {
                            Toast.makeText(
                                this@MainActivity,
                                "error connecting...",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            packet = it
                            resolvePacket()
                        }
                    }

                }

            }

        }, 2000, 3000)

    }

}

fun TextView.rightIcon(@DrawableRes id: Int = 0) {
    this.setCompoundDrawablesWithIntrinsicBounds(0, 0, id, 0)
}

fun afterWhile(DoNow: () -> Unit) {

    Handler(Looper.getMainLooper()).postDelayed({
        DoNow.invoke()
    }, 400)

}

fun normalizeNumber(value: Float): Float {
    return ((1.0f * value) / 255.0f)
}