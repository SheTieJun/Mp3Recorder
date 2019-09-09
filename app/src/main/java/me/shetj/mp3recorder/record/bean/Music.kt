package me.shetj.mp3recorder.record.bean

class Music {
    var url:String?=null
    var name :String ?=null
    var imgUrl:String ?=null
    var duration:Long  = 0

    @JvmOverloads
    constructor( name: String?,url: String?= null,imgUrl:String? = null) {
        this.url = url
        this.name = name
        this.imgUrl = imgUrl
    }

    constructor()

}