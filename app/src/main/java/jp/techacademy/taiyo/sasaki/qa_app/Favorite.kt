package jp.techacademy.taiyo.sasaki.qa_app

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Favorite (var key: String = "", var questionUid: String = "",) {

    @Exclude
    fun toMap() : Map<String, String> {
        return mapOf(
            "questionUid" to questionUid,
        )
    }
}