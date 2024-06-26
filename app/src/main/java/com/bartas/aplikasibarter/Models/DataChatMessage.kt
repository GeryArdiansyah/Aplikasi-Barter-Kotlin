package com.bartas.aplikasibarter.Models

import android.os.Parcel
import android.os.Parcelable

data class DataChatMessage(
    val senderUid: String?,
    val receiverUid: String?,
    val userName: String?,
    val message: String?,
    val timestamp: String?,
    val photoUrl: String? = null,
    val lastActiveTime: Long = 0 // Add lastActiveTime attribute with default value
) : Parcelable {
    // No-argument constructor required by Firebase
    constructor() : this("", "", "", "", "", "", 0)

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(senderUid)
        parcel.writeString(receiverUid)
        parcel.writeString(userName)
        parcel.writeString(message)
        parcel.writeString(timestamp)
        parcel.writeString(photoUrl)
        parcel.writeLong(lastActiveTime)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DataChatMessage> {
        override fun createFromParcel(parcel: Parcel): DataChatMessage {
            return DataChatMessage(parcel)
        }

        override fun newArray(size: Int): Array<DataChatMessage?> {
            return arrayOfNulls(size)
        }
    }
}
