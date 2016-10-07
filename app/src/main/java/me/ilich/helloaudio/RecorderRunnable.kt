package me.ilich.helloaudio

import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log

class RecorderRunnable(
        val sampleRate: Int,
        val channels: Int,
        val encoding: Int,
        val onData: (FloatArray, Int) -> (Unit)
) : Runnable {

    var working = true

    private fun getBufferSize(): Int {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channels, encoding)
        Log.v("Sokolov", "bufferSize = $bufferSize")
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e("Sokolov", "error")
            throw RuntimeException()
        }
        return bufferSize
    }

    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val bufferSize = getBufferSize()
        Log.v("Sokolov", "begin")
        val audio = AudioRecord(MediaRecorder.AudioSource.MIC, MainActivity.RECORDER_SAMPLERATE,
                MainActivity.RECORDER_CHANNELS, MainActivity.RECORDER_AUDIO_ENCODING, bufferSize)
        if (audio.state != AudioRecord.STATE_INITIALIZED) {
            Log.v("Sokolov", "not initialized")
            return
        }

        val buffer = FloatArray(bufferSize)

        audio.startRecording()
        Log.v("Sokolov", "start")
        while (working) {
            val readed = audio.read(buffer, 0, bufferSize, AudioRecord.READ_BLOCKING)
            when (readed) {
                AudioRecord.ERROR_INVALID_OPERATION -> Log.w("Sokolov", "ERROR_INVALID_OPERATION")
                AudioRecord.ERROR_BAD_VALUE -> Log.w("Sokolov", "ERROR_BAD_VALUE")
                AudioRecord.ERROR_DEAD_OBJECT -> Log.w("Sokolov", "ERROR_DEAD_OBJECT")
                AudioRecord.ERROR -> Log.w("Sokolov", "ERROR")
                else -> {
                    onData(buffer, readed)
                }
            }
        }
        Log.v("Sokolov", "stop")
        audio.stop()
        audio.release()
        Log.v("Sokolov", "end")
    }

}
