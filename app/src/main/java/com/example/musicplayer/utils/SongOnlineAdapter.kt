package com.example.musicplayer.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.models.SongOnline

class SongOnlineAdapter(private val songs: List<SongOnline>, private val context: Context) :
    RecyclerView.Adapter<SongOnlineAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songName: TextView = view.findViewById(R.id.textViewSongTitle)
        val artistName: TextView = view.findViewById(R.id.textViewArtistName)
        // Add other views if needed
    }

    private lateinit var onItemClickCallback: SongClicked

    fun setOnItemClickCallback(onItemClickCallback: SongClicked) {
        this.onItemClickCallback = onItemClickCallback
    }

    interface SongClicked {
        fun onSongClicked(song: SongOnline, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.songName.text = song.name
        holder.artistName.text = song.artist
        // Bind other views if needed

        holder.itemView.setOnClickListener {
            onItemClickCallback.onSongClicked(songs[position], position)
        }
    }

    override fun getItemCount() = songs.size
}