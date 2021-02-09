package me.shetj.mp3recorder.record.bean

import android.net.Uri

class MusicQ {
    var url:Uri?=null
    var name :String ?=null
    var imgUrl:String ?=null
    var duration:Long  = 0

    @JvmOverloads
    constructor( name: String?,url: Uri?= null,imgUrl:String? = null) {
        this.url = url
        this.name = name
        this.imgUrl = imgUrl
    }

    constructor()

}