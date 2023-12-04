package com.example.musicplayer.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.utils.MediaPlayerHandler
import com.example.musicplayer.models.SongOnline
import com.example.musicplayer.utils.SongOnlineAdapter
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_online_playlist.ivAddSong
import kotlinx.android.synthetic.main.content_main.progressBarLoading
import kotlinx.android.synthetic.main.controls.buttonNext
import kotlinx.android.synthetic.main.controls.buttonPlayPause
import kotlinx.android.synthetic.main.controls.buttonPrevious
import kotlinx.android.synthetic.main.controls.songTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnlinePlaylistActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    private lateinit var mediaPlayerHandler: MediaPlayerHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_playlist)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        listFiles()

        ivAddSong.setOnClickListener {
            startActivity(
                Intent(this, AddOnlineSongActivity::class.java)
            )
        }
    }

    private fun listFiles() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val databaseRef = FirebaseDatabase.getInstance().getReference("songs")
            val songData = mutableListOf<SongOnline>()
            withContext(Dispatchers.Main) {
                progressBarLoading.visibility = View.VISIBLE
            }
            databaseRef.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                    val song = dataSnapshot.getValue(SongOnline::class.java)
                    song?.let {
                        songData.add(it)
                        val songOnlineAdapter = SongOnlineAdapter(songData, this@OnlinePlaylistActivity)

                        val seekBar = findViewById<SeekBar>(R.id.seekBar)
                        mediaPlayerHandler = MediaPlayerHandler(this@OnlinePlaylistActivity, seekBar)
                        mediaPlayerHandler.setSongs(songData)

                        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                if (fromUser) {
                                    mediaPlayerHandler.seekTo(progress)
                                }
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                                // Do nothing
                            }

                            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                // Do nothing
                            }
                        })

                        buttonPlayPause?.setOnClickListener {
                            if (mediaPlayerHandler.isPlaying()) {
                                mediaPlayerHandler.pauseSong()
                                buttonPlayPause.setImageResource(R.drawable.ic_play)
                            } else {
                                if (mediaPlayerHandler.getMediaPlayer() != null) {
                                    mediaPlayerHandler.resumeSong()
                                    buttonPlayPause.setImageResource(R.drawable.ic_pause)
                                } else {
                                    mediaPlayerHandler.playSong(songData[mediaPlayerHandler.currentSongIndex])
                                    buttonPlayPause.setImageResource(R.drawable.ic_pause)
                                }
                            }
                        }

                        buttonNext.setOnClickListener {
                            mediaPlayerHandler.playNextSong()
                            songTitle.text = songData[mediaPlayerHandler.currentSongIndex].name
                        }

                        buttonPrevious.setOnClickListener {
                            mediaPlayerHandler.playPreviousSong()
                            songTitle.text = songData[mediaPlayerHandler.currentSongIndex].name
                        }

                        songOnlineAdapter.setOnItemClickCallback(object :
                            SongOnlineAdapter.SongClicked {
                            override fun onSongClicked(song: SongOnline, position: Int) {
                                songTitle.text = song.name
                                mediaPlayerHandler.currentSongIndex = position
                                mediaPlayerHandler.playSong(song)
                                buttonPlayPause.setImageResource(R.drawable.ic_pause)
                            }
                        })

                        recyclerView.apply {
                            adapter = songOnlineAdapter
                            layoutManager = LinearLayoutManager(this@OnlinePlaylistActivity)
                        }
                        progressBarLoading.visibility = View.GONE
                    }
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                    // Handle child changed
                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                    // Handle child removed
                }

                override fun onChildMoved(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                    // Handle child moved
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle cancelled
                    Toast.makeText(this@OnlinePlaylistActivity, databaseError.message, Toast.LENGTH_LONG).show()
                    progressBarLoading.visibility = View.GONE
                }
            })
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@OnlinePlaylistActivity, e.message, Toast.LENGTH_LONG).show()
                progressBarLoading.visibility = View.GONE
            }
        }
    }
}