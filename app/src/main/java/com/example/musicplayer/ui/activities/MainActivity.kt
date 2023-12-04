package com.example.musicplayer.ui.activities

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.musicplayer.R
import com.example.musicplayer.models.Song
import com.example.musicplayer.playback.MusicNotificationManager
import com.example.musicplayer.playback.MusicService
import com.example.musicplayer.playback.PlaybackInfoListener
import com.example.musicplayer.playback.PlayerAdapter
import com.example.musicplayer.ui.blueprints.MainActivityBluePrint
import com.example.musicplayer.utils.RecyclerAdapter
import com.example.musicplayer.utils.SongProvider
import com.example.musicplayer.utils.Utils
import kotlinx.android.synthetic.main.controls.imageViewControl

class MainActivity : MainActivityBluePrint(), View.OnClickListener, RecyclerAdapter.SongClicked {

    private var seekBar: SeekBar? = null
    private var playPause: ImageButton? = null
    private var next: ImageButton? = null
    private var previous: ImageButton? = null
    private var songTitle: TextView? = null
    private var mMusicService: MusicService? = null
    private var mIsBound: Boolean? = null
    private var mPlayerAdapter: PlayerAdapter? = null
    private var mUserIsSeeking = false
    private var mPlaybackListener: PlaybackListener? = null
    private var deviceSongs: MutableList<Song>? = null
    private var mMusicNotificationManager: MusicNotificationManager? = null

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {

            mMusicService = (iBinder as MusicService.LocalBinder).instance
            mPlayerAdapter = mMusicService!!.mediaPlayerHolder
            mMusicNotificationManager = mMusicService!!.musicNotificationManager

            if (mPlaybackListener == null) {
                mPlaybackListener = PlaybackListener()
                mPlayerAdapter!!.setPlaybackInfoListener(mPlaybackListener!!)
            }
            if (mPlayerAdapter != null && mPlayerAdapter!!.isPlaying()) {

                restorePlayerStatus()
            }
            checkReadStoragePermissions()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mMusicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        doBindService()
        setViews()
        initializeSeekBar()

        val ivPlaylistFirebase = findViewById<ImageView>(R.id.ivPlaylistFirebase)
        ivPlaylistFirebase.setOnClickListener {
            startActivity(
                Intent(this, OnlinePlaylistActivity::class.java)
            )
        }
    }

    override fun onPause() {
        super.onPause()
        doUnbindService()
        if (mPlayerAdapter != null && mPlayerAdapter!!.isMediaPlayer()) {
            mPlayerAdapter!!.onPauseActivity()
        }
    }

    override fun onResume() {
        super.onResume()
        doBindService()
        if (mPlayerAdapter != null && mPlayerAdapter!!.isPlaying()) {

            restorePlayerStatus()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            finish()
        }else getMusic()
    }

    private fun setViews() {
        playPause = findViewById(R.id.buttonPlayPause)
        next = findViewById(R.id.buttonNext)
        previous = findViewById(R.id.buttonPrevious)
        seekBar = findViewById(R.id.seekBar)
        songTitle = findViewById(R.id.songTitle)
        playPause!!.setOnClickListener(this)
        next!!.setOnClickListener(this)
        previous!!.setOnClickListener(this)
        deviceSongs = SongProvider.getAllDeviceSongs(this)
    }

    private fun checkReadStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }else getMusic()
    }

    private fun updatePlayingInfo(restore: Boolean, startPlay: Boolean) {

        if (startPlay) {
            mPlayerAdapter!!.getMediaPlayer()?.start()
            Handler().postDelayed({
                mMusicService!!.startForeground(
                    MusicNotificationManager.NOTIFICATION_ID,
                        mMusicNotificationManager!!.createNotification())
            }, 200)
        }

        val selectedSong = mPlayerAdapter!!.getCurrentSong()

        songTitle?.text = selectedSong?.title
        val duration = selectedSong?.duration
        seekBar?.max = duration!!
        imageViewControl?.setImageBitmap(Utils.songArt(selectedSong.path!!, this@MainActivity))

        if (restore) {
            seekBar!!.progress = mPlayerAdapter!!.getPlayerPosition()
            updatePlayingStatus()


            Handler().postDelayed({
                //stop foreground if coming from pause state
                if (mMusicService!!.isRestoredFromPause) {
                    mMusicService!!.stopForeground(false)
                    mMusicService!!.musicNotificationManager!!.notificationManager
                            .notify(
                                MusicNotificationManager.NOTIFICATION_ID,
                                    mMusicService!!.musicNotificationManager!!.notificationBuilder!!.build())
                    mMusicService!!.isRestoredFromPause = false
                }
            }, 200)
        }
    }

    private fun updatePlayingStatus() {
        val drawable = if (mPlayerAdapter!!.getState() != PlaybackInfoListener.State.PAUSED)
            R.drawable.ic_pause
        else
            R.drawable.ic_play
        playPause!!.post { playPause!!.setImageResource(drawable) }
    }

    private fun restorePlayerStatus() {
        seekBar!!.isEnabled = mPlayerAdapter!!.isMediaPlayer()

        if (mPlayerAdapter != null && mPlayerAdapter!!.isMediaPlayer()) {

            mPlayerAdapter!!.onResumeActivity()
            updatePlayingInfo(true, false)
        }
    }

    private fun doBindService() {
        bindService(Intent(this,
                MusicService::class.java), mConnection, Context.BIND_AUTO_CREATE)
        mIsBound = true

        val startNotStickyIntent = Intent(this, MusicService::class.java)
        startService(startNotStickyIntent)
    }

    private fun doUnbindService() {
        if (mIsBound!!) {
            // Detach our existing connection.
            unbindService(mConnection)
            mIsBound = false
        }
    }

    private fun onSongSelected(song: Song, songs: List<Song>) {
        if (!seekBar!!.isEnabled) {
            seekBar!!.isEnabled = true
        }
        try {
            mPlayerAdapter!!.setCurrentSong(song, songs)
            mPlayerAdapter!!.initMediaPlayer()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun skipPrev() {
        if (checkIsPlayer()) {
            mPlayerAdapter!!.instantReset()
        }
    }

    private fun resumeOrPause() {
        if (checkIsPlayer()) {
            mPlayerAdapter!!.resumeOrPause()
        } else {
            val songs = SongProvider.getAllDeviceSongs(this)
            if (songs.isNotEmpty()) {
                onSongSelected(songs[0], songs)

            }
        }
    }

    private fun skipNext() {
        if (checkIsPlayer()) {
            mPlayerAdapter!!.skip(true)
        }
    }

    private fun checkIsPlayer(): Boolean {
        return mPlayerAdapter!!.isMediaPlayer()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.buttonPlayPause -> {
                resumeOrPause()

            }
            R.id.buttonNext -> {
                skipNext()
            }
            R.id.buttonPrevious -> {
                skipPrev()
            }
        }
    }

    private fun initializeSeekBar() {
        seekBar!!.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    var userSelectedPosition = 0

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        mUserIsSeeking = true
                    }

                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

                        if (fromUser) {
                            userSelectedPosition = progress
                        }

                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {

                        if (mUserIsSeeking) {

                        }
                        mUserIsSeeking = false
                        mPlayerAdapter!!.seekTo(userSelectedPosition)
                    }
                })
    }

    override fun onSongClicked(song: Song) {
        onSongSelected(song, deviceSongs!!)
    }

    internal inner class PlaybackListener : PlaybackInfoListener() {

        override fun onPositionChanged(position: Int) {
            if (!mUserIsSeeking) {
                seekBar!!.progress = position
            }
        }

        override fun onStateChanged(@State state: Int) {

            updatePlayingStatus()
            if (mPlayerAdapter!!.getState() != State.PAUSED
                    && mPlayerAdapter!!.getState() != State.PAUSED) {
                updatePlayingInfo(false, true)
            }
        }

        override fun onPlaybackCompleted() {
            //After playback is complete
        }
    }
}
