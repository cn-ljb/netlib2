package net.ljb.kt.file.request

interface FileMediaType {

    companion object {
        const val MEDIA_TYPE_MP4 = "audio/mp4"
        const val MEDIA_TYPE_MP3 = "audio/mp3"
        const val MEDIA_TYPE_PNG = "image/png"
        const val MEDIA_TYPE_JPG = "image/jpeg"
        const val MEDIA_TYPE_GIF = "image/gif"
        const val MEDIA_TYPE_XML = "application/xml"
        const val MEDIA_TYPE_JSON = "application/json"
    }
}