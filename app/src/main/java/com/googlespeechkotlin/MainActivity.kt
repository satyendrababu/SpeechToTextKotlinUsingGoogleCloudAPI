package com.googlespeechkotlin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeRequest
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechRecognitionAlternative
import com.google.cloud.speech.v1.SpeechRecognitionResult
import com.google.cloud.speech.v1.SpeechSettings

import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 123
    }

    private lateinit var speechClient: SpeechClient
    private lateinit var startButton: Button
    private lateinit var textView: TextView

    private var mVoiceRecorder: VoiceRecorder? = null
    private var byteArray: ByteArray = byteArrayOf()


    private val mVoiceCallBack: VoiceRecorder.Callback = object : VoiceRecorder.Callback() {
        override fun onVoiceStart() {

        }

        override fun onVoice(data: ByteArray?, size: Int) {
            byteArray = data?.let { byteArray.plus(it) }!!
            Log.e("kya", "***" + byteArray.toString())

        }

        override fun onVoiceEnd() {
            transcribeRecording(byteArray)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.button)
        textView = findViewById(R.id.tvText)

        startButton.setOnClickListener {
            startButton.text = "Stop"
            startVoiceRecorder()
        }

        //initialize the speech clint
        initializeSpeechClient()


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun initializeSpeechClient() {
        val credentials = GoogleCredentials.fromStream(resources.openRawResource(R.raw.credentials))
        val credentialsProvider = FixedCredentialsProvider.create(credentials)
        speechClient = SpeechClient.create(
            SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        speechClient.close()
    }

    private fun transcribeRecording(data: ByteArray) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = speechClient.recognize(createRecognizeRequestFromVoice(data))
            val results = response.resultsList
            val transcription = processTranscriptionResults(results)
            displayTranscription(transcription)
        }
    }


    private fun displayTranscription(transcription: String) {
        runOnUiThread {
            textView.text = transcription
            stopVoiceRecorder()
            startButton.text = "Start"
        }
    }

    private fun processTranscriptionResults(results: List<SpeechRecognitionResult>): String {
        val stringBuilder = StringBuilder()
        for (result in results) {
            val recData: SpeechRecognitionAlternative = result.alternativesList[0]
            stringBuilder.append(recData.transcript)
        }
        return stringBuilder.toString()
    }


    private fun createRecognizeRequestFromVoice(audioData: ByteArray): RecognizeRequest {
        val audioBytes =
            RecognitionAudio.newBuilder().setContent(ByteString.copyFrom(audioData)).build()
        val config = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setSampleRateHertz(16000)
            .setLanguageCode("en-US")
            .build()
        return RecognizeRequest.newBuilder()
            .setConfig(config)
            .setAudio(audioBytes)
            .build()
    }

    private fun startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder!!.stop()
        }
        mVoiceRecorder = VoiceRecorder(mVoiceCallBack)
        mVoiceRecorder!!.start()
    }

    private fun stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder!!.stop()
            mVoiceRecorder = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "Record audio permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}