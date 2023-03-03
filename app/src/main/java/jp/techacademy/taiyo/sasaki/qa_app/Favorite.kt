package jp.techacademy.taiyo.sasaki.qa_app

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Favorite (var key: String = "", var title: String = "", var name: String = "", var questionUid: String = "", var image: String = "") {

    @Exclude
    fun toMap() : Map<String, String> {
        return mapOf(
            "title" to title,
            "name" to name,
            "questionUid" to questionUid,
            "image" to image
        )
    }
}