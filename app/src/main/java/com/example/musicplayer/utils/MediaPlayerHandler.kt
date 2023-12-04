package com.example.musicplayer.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.SeekBar
import com.example.musicplayer.models.SongOnline
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File

class MediaPlayerHandler(private val context: Context, private val seekBar: SeekBar) {
    private var mediaPlayer: MediaPlayer? = null
    private var songs: List<SongOnline> = emptyList()
    var currentSongIndex = 0

    fun setSongs(songs: List<SongOnline>) {
        this.songs = songs
    }

    fun getMediaPlayer(): MediaPlayer? {
        return mediaPlayer
    }

    fun playSong(song: SongOnline) {
        stopSong()
        val localFile = File.createTempFile("songs", "mp3")
        val storageRef = song.url?.let { Firebase.storage.getReferenceFromUrl(it) }
        storageRef?.getFile(localFile)?.addOnSuccessListener {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(localFile))
                prepare()
                start()
            }
            seekBar.max = mediaPlayer?.duration ?: 0
            updateSeekBar()
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    fun pauseSong() {
        mediaPlayer?.pause()
    }

    fun resumeSong() {
        mediaPlayer?.start()
        updateSeekBar()
    }

    fun stopSong() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun playNextSong() {
        if (songs.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % songs.size
            playSong(songs[currentSongIndex])
        }
    }

    fun playPreviousSong() {
        if (songs.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex - 1 < 0) songs.size - 1 else currentSongIndex - 1
            playSong(songs[currentSongIndex])
        }
    }

    fun seekTo(position: Int) {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.seekTo(position)
        }
    }

    private fun updateSeekBar() {
        val mediaPlayer = this.mediaPlayer ?: return
        seekBar.progress = mediaPlayer.currentPosition
        if (mediaPlayer.isPlaying) {
            seekBar.postDelayed({ updateSeekBar() }, 1000)
        }
    }
}