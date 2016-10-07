package me.ilich.helloaudio

import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.util.Log

class PlayerRunnable(
        val sampleRate: Int,
        val channels: Int,
        val encoding: Int,
        val onData: (ShortArray) -> (Int)
) : Runnable {

    override fun run() {
        val bufferSize = getBufferSize()
        val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate, channels, encoding, bufferSize, AudioTrack.MODE_STREAM)
        audioTrack.play()
        val buffer = ShortArray(bufferSize)

        var readed = onData(buffer)
        while (readed != 0) {
            audioTrack.write(buffer, 0, readed)
            readed = onData(buffer)
        }
        audioTrack.release()
    }

    private fun getBufferSize(): Int {
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channels, encoding)
        Log.v("Sokolov", "bufferSize = $bufferSize")
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e("Sokolov", "error")
            throw RuntimeException()
        }
        return bufferSize
    }

}