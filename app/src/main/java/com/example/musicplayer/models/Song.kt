package com.example.musicplayer.models

import android.os.Parcelable

data class Song(
    val title: String,
    val trackNumber: Int,
    val year: Int,
    val duration: Int,
    val path: String?,
    val albumName: String,
    val artistId: Int,
    val artistName: String
) : Parcelable {
    constructor(parcel: android.os.Parcel) : this(
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeInt(trackNumber)
        parcel.writeInt(year)
        parcel.writeInt(duration)
        parcel.writeString(path)
        parcel.writeString(albumName)
        parcel.writeInt(artistId)
        parcel.writeString(artistName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Song> {
        override fun createFromParcel(parcel: android.os.Parcel): Song {
            return Song(parcel)
        }

        override fun newArray(size: Int): Array<Song?> {
            return arrayOfNulls(size)
        }
    }
}