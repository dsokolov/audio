package me.ilich.helloaudio

import android.media.AudioFormat
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.nio.FloatBuffer

class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val SAMPLERATE = 44100
        const val RECORDER_SAMPLERATE = SAMPLERATE
        const val PLAYER_SAMPLERATE = SAMPLERATE
        const val RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO
        const val PLAYER_CHANNELS = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_FLOAT
        const val RECORDER_AUDIO_ENCODING = AUDIO_ENCODING
        const val PLAYER_AUDIO_ENCODING = AUDIO_ENCODING
    }

    var bufferSize: Int = 0
    val buffer: FloatBuffer = FloatBuffer.allocate(1024 * 1024)

    val writeToBuffer: (FloatArray, Int) -> (Unit) = {
        array: FloatArray, length: Int ->
        buffer.put(array, 0, length)

        Log.v("Sokolov", "readed = $length")
        //fos.write(buffer, 0, length)
    }
    val readFromBuffer: (FloatArray) -> (Int) = {
        array: FloatArray ->
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

                val durationMs = 2500
                val freqHz0 = 440
                val freqHz1 = 850
                val freqHz2 = 1550
                var b = false
                val count = (44100.0 * 2.0 * (durationMs / 1000.0)).toInt() and 1.inv()
                val samples = DoubleArray(count)
                var i = 0
                while (i < count) {
                    if (i % 5000 == 0) {
                        b = !b
                    }
                    val freqHz = when (b) {
                    //   else -> freqHz0
                        true -> freqHz1
                        false -> freqHz2
                    }

                    val d = Math.sin(1.5 * Math.PI * i.toDouble() / (44100.0 / freqHz))
                    samples[i] = d
                    //val d2 = lpf(2000.0, d)
/*                    val d2 = d
                    val sample = (d2 * 0x7FFF).toShort()
                    samples[i + 0] = sample
                    samples[i + 1] = sample*/
//                    i += 2
                    i++
                }
                val lpa = fourierLowPassFilter(samples, 400.0, 0.0)
                val a = FloatArray(lpa.size)
                for (i in 0..lpa.size - 1) {
                    a[i] = lpa[i].toFloat()
                }
                buffer.put(a)
                Log.v("Sokolov", "samples count = ${samples.size}")

                Log.v("Sokolov", "end process")
            }
        }
    }

    fun sinc(x: Double): Double = when (x) {
        0.0 -> 1.0
        else -> Math.sin(Math.PI * x) / Math.PI * x
    }

    fun lpf(f: Double, t: Double): Double = 2.0 * f * sinc(2.0 * f * t)

    fun fourierLowPassFilter(data: DoubleArray, lowPass: Double, frequency: Double): DoubleArray {
        //data: input data, must be spaced equally in time.
        //lowPass: The cutoff frequency at which
        //frequency: The frequency of the input data.

        //The apache Fft (Fast Fourier Transform) accepts arrays that are powers of 2.
        var minPowerOf2 = 1
        while (minPowerOf2 < data.size)
            minPowerOf2 = 2 * minPowerOf2

        //pad with zeros
        val padded = DoubleArray(minPowerOf2)
        for (i in data.indices)
            padded[i] = data[i]


        val transformer = FastFourierTransformer(DftNormalization.STANDARD)
        val fourierTransform = transformer.transform(padded, TransformType.FORWARD)

        //build the frequency domain array
        val frequencyDomain = DoubleArray(fourierTransform.size)
        for (i in frequencyDomain.indices)
            frequencyDomain[i] = frequency * i / fourierTransform.size.toDouble()

        //build the classifier array, 2s are kept and 0s do not pass the filter
        val keepPoints = DoubleArray(frequencyDomain.size)
        keepPoints[0] = 1.0
        for (i in 1..frequencyDomain.size - 1) {
            if (frequencyDomain[i] < lowPass)
                keepPoints[i] = 2.0
            else
                keepPoints[i] = 0.0
        }

        //filter the fft
        for (i in fourierTransform.indices)
            fourierTransform[i] = fourierTransform[i].multiply(keepPoints[i].toDouble())

        //invert back to time domain
        val reverseFourier = transformer.transform(fourierTransform, TransformType.INVERSE)

        //get the real part of the reverse
        val result = DoubleArray(data.size)
        for (i in result.indices) {
            result[i] = reverseFourier[i].getReal()
        }

        return result
    }

}
