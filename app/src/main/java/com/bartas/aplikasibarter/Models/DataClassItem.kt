package com.bartas.aplikasibarter.Models

class DataClassItem {
    var id: String? = null
    var kategori: String? = null
    var userId: String? = null
    var nama: String? = null
    var harga: String? = null
    var imageUrl: String? = null
    var deskripsi: String? = null
    var lokasi: String? = null
    var favorite: Boolean = false

    constructor(id: String?, userId: String?, nama: String?, harga: String?, imageUrl: String?, deskripsi: String?, lokasi: String, favorite: Boolean, kategori: String?) {
        this.id = id
        this.userId = userId
        this.nama = nama
        this.harga = harga
        this.imageUrl = imageUrl
        this.deskripsi = deskripsi
        this.lokasi = lokasi
        this.favorite = favorite
        this.kategori = kategori
    }

    constructor() {

    }
}