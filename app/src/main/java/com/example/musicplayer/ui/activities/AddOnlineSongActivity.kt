package com.example.musicplayer.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.musicplayer.R
import com.example.musicplayer.models.SongOnline
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_add_online_song.progressBarAddSong
import kotlinx.android.synthetic.main.activity_add_online_song.tvSongArtistEdit
import kotlinx.android.synthetic.main.activity_add_online_song.tvSongFileEdit
import kotlinx.android.synthetic.main.activity_add_online_song.tvSongName
import kotlinx.android.synthetic.main.activity_add_online_song.tvSongNameEdit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddOnlineSongActivity : AppCompatActivity() {
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUEST_CODE_PICK_AUDIO = 11

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_online_song)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS
            )
        }

        val ivPlaylistBack = findViewById<ImageView>(R.id.ivPlaylistBack)
        ivPlaylistBack.setOnClickListener {
            onBackPressed()
        }

        val btnAddSong = findViewById<Button>(R.id.btnAddSong)
        btnAddSong.setOnClickListener {
            if (inputError()) {
                addSong()
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        tvSongFileEdit.setOnClickListener {
            openAudioPicker()
        }
    }

    private fun inputError(): Boolean {
        if (tvSongNameEdit.text.toString().isEmpty()) {
            tvSongName.error = "Please enter a song name"
            tvSongName.requestFocus()
            return false
        }

        if (tvSongArtistEdit.text.toString().isEmpty()) {
            tvSongArtistEdit.error = "Please enter an artist name"
            tvSongArtistEdit.requestFocus()
            return false
        }

        if (songFile == null) {
            Toast.makeText(this, "Please select a song", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openAudioPicker()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAudioPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/mpeg" // MIME type for .mp3 files
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_AUDIO)
    }

    private var songFile: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_AUDIO && resultCode == RESULT_OK) {
            songFile = data?.data
            if (songFile == null) {
                Toast.makeText(this, "Failed to get song", Toast.LENGTH_SHORT).show()
                return
            }
            tvSongFileEdit.setText("Song has been selected")
        }
    }

    private fun addSong() = CoroutineScope(Dispatchers.IO).launch {
        val storageRef = Firebase.storage.reference
        val databaseRef = FirebaseDatabase.getInstance().getReference("songs")

        val songId = databaseRef.push().key // generate a unique ID for the song

        val songStorageRef = storageRef.child("songs/$songId")
        val uploadTask = songFile?.let { songStorageRef.putFile(it) }

        uploadTask?.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
            progressBarAddSong.visibility = View.VISIBLE
            progressBarAddSong.progress = progress.toInt()
        }

        uploadTask?.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            songStorageRef.downloadUrl
        }?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                val song = SongOnline(
                    songId!!,
                    tvSongNameEdit.text.toString(),
                    tvSongArtistEdit.text.toString(),
                    downloadUri.toString()
                )

                databaseRef.child(songId).setValue(song)
                progressBarAddSong.visibility = View.GONE
                Toast.makeText(this@AddOnlineSongActivity, "Song added", Toast.LENGTH_LONG)
                    .show()
                startActivity(
                    Intent(this@AddOnlineSongActivity, OnlinePlaylistActivity::class.java)
                )
                finishAffinity()
            } else {
                progressBarAddSong.visibility = View.GONE
                Toast.makeText(
                    this@AddOnlineSongActivity,
                    "Failed to upload song",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}