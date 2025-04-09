package com.example.asm2

import android.os.Parcel
import android.os.Parcelable

data class Instrument(
    val name: String,
    val price: Int,
    val imageRes: Int,
    val description: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(price)
        parcel.writeInt(imageRes)
        parcel.writeString(description)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Instrument> {
        override fun createFromParcel(parcel: Parcel): Instrument {
            return Instrument(parcel)
        }

        override fun newArray(size: Int): Array<Instrument?> {
            return arrayOfNulls(size)
        }
    }
}