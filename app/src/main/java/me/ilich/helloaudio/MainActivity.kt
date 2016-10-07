package me.ilich.helloaudio

import android.media.AudioFormat
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import java.nio.ShortBuffer

class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val SAMPLERATE = 44100
        const val RECORDER_SAMPLERATE = SAMPLERATE
        const val PLAYER_SAMPLERATE = SAMPLERATE
        const val RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
        const val PLAYER_CHANNELS = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val RECORDER_AUDIO_ENCODING = AUDIO_ENCODING
        const val PLAYER_AUDIO_ENCODING = AUDIO_ENCODING
    }

    var bufferSize: Int = 0
    val buffer: ShortBuffer = ShortBuffer.allocate(1024 * 1024)

    val writeToBuffer: (ShortArray, Int) -> (Unit) = {
        byteArray: ShortArray, length: Int ->
        buffer.put(byteArray, 0, length)

        Log.v("Sokolov", "readed = $length")
        //fos.write(buffer, 0, length)
    }
    val readFromBuffer: (ShortArray) -> (Int) = {
        array: ShortArray ->
        val curPos = buffer.position()
        val shouldRead = Math.min(array.size, bufferSize - curPos)
        if (shouldRead > 0) {
            buffer.get(array, 0, shouldRead)
            buffer.position() - curPos
        } else {
            0
        }
    }

    var recordRunnable: RecorderRunnable? = null
    var playRunnable: PlayerRunnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

/*        val rates = intArrayOf(8000, 11025, 22050, 44100, 48000, 96000)
        val chans = intArrayOf(AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO)
        val encs = intArrayOf(*//*AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT,*//* AudioFormat.ENCODING_PCM_FLOAT)
        for (enc in encs) {
            for (ch in chans) {
                for (rate in rates) {
                    val t = AudioRecord.getMinBufferSize(rate, ch, enc)
                    if (t != AudioRecord.ERROR && t != AudioRecord.ERROR_BAD_VALUE) {
                        Log.v("Sokolov", "rate = $rate ch = $ch enc = $enc")
                    }
                }
            }
        }*/

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.rec_start -> {
                buffer.clear()
                recordRunnable = RecorderRunnable(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, writeToBuffer)
                Thread(recordRunnable).start()
            }
            R.id.rec_finish -> {
                Log.v("Sokolov", "buffer pos = ${buffer.position()}")
                recordRunnable?.working = false
                recordRunnable = null
            }
            R.id.play_start -> {
                bufferSize = buffer.position()
                buffer.rewind()
                playRunnable = PlayerRunnable(PLAYER_SAMPLERATE, PLAYER_CHANNELS, PLAYER_AUDIO_ENCODING, readFromBuffer)
                Thread(playRunnable).start()
            }
            R.id.play_finish -> {
                //playRunnable.working = false
            }
            R.id.process -> {
                Log.v("Sokolov", "begin process")

                val durationMs = 5000
                val freqHz1 = 850
                val freqHz2 = 1550
                var b = false
                val count = (44100.0 * 2.0 * (durationMs / 1000.0)).toInt() and 1.inv()
                val samples = ShortArray(count)
                var i = 0
                while (i < count) {
                    if (i % 5000 == 0) {
                        b = !b
                    }
                    val freqHz = when (b) {
                        true -> freqHz1
                        false -> freqHz2
                    }
                    val sample = (Math.sin(2.0 * Math.PI * i.toDouble() / (44100.0 / freqHz)) * 0x7FFF).toShort()
                    samples[i + 0] = sample
                    samples[i + 1] = sample
                    i += 2
                }
                buffer.put(samples)
                Log.v("Sokolov", "samples count = ${samples.size}")

                Log.v("Sokolov", "end process")
            }
        }
    }

}
