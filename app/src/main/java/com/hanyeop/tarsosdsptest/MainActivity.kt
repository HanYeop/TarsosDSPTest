package com.hanyeop.tarsosdsptest

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import com.hanyeop.tarsosdsptest.databinding.ActivityMainBinding
import kotlin.math.pow


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        binding.lifecycleOwner = this

        init()
    }

    private fun init(){
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        )
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                111
            )

        binding.btnRecord.setOnClickListener {
            record()
        }
        binding.btnStop.setOnClickListener {
//            audioThread.
        }
    }

    var pitchInHz : Float = 0.0f
    lateinit var audioThread: Thread

    private fun record(){
        val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0)
        val pdh = PitchDetectionHandler { res, e ->
            // get pitch value calculated by DSP library
            pitchInHz = res.pitch
            // run this code (function) whenever pitchInHz changes value
            runOnUiThread { processPitch(pitchInHz) }
            Log.d("test5", "record: $pitchInHz")
        }
        val pitchProcessor: AudioProcessor =
            PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050.0f, 1024, pdh)
        dispatcher.addAudioProcessor(pitchProcessor)

        // create thread with DSP functionality
        audioThread = Thread(dispatcher, "Audio Thread")
        // start this thread
        audioThread.start()
    }

    private fun processPitch(pitchInHz: Float) {

        binding.tvPitch.text =
            if (pitchInHz != -1.0f) "${pitchInHz.toString()} Hz"
            else "Unknown frequency"

        binding.tvNote.text = if (pitchInHz != -1.0f) calcNearestTone(pitchInHz) else "Unknown note"
    }

    private fun calcNearestTone(freq: Float) : String{
        val sound :Array<String> = arrayOf("A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#")
        val ratio:Double = 2.0.pow((1.0/12.0))
        val base_frequency = 55.0
        val get_note = {i: Int -> ratio.pow(i) * base_frequency} // get note frequency, i is index of note in regards to A(55 Hz) --> get_note(0) gives 55Hz

        if (freq < get_note(0)) return "Unknown Note"
        if (freq >= get_note(0) && freq < (get_note(1) - get_note(0)) / 2 ){
            return sound[0]
        }

        var tone:String = "X"
        var index:Int = 1
        var curNoteFreq = 0.0
        while (curNoteFreq < 1000.0){
            curNoteFreq = get_note(index)
            val past_freq_diff = (curNoteFreq - get_note(index - 1)) / 2
            val future_freq_diff = (get_note(index + 1) - curNoteFreq) / 2
            if (freq >= curNoteFreq - past_freq_diff && freq < curNoteFreq + future_freq_diff){
                tone = sound[index % 12]
                break // note found, exit while loop
            }
            index++
        }
        return if (tone != "X"){
            tone
        } else {
            "Unknown Note"
        }
    }
}