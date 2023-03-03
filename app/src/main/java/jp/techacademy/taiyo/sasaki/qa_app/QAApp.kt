package jp.techacademy.taiyo.sasaki.qa_app

import android.app.Application
import android.util.Log

class QAApp: Application() {

    private var favoriteList: ArrayList<Favorite> = ArrayList()

    override fun onCreate() {
        super.onCreate()
    }

    fun setFavorite(list: ArrayList<Favorite>) {
        favoriteList = list
    }

    fun findBy(questionUid: String) :Boolean {
        if (favoriteList.size > 0) {
            for (index in 0..favoriteList.size - 1) {
                if (favoriteList[index].questionUid == questionUid) {
                    return true
                }
            }
        }
        return false
    }

    fun addFavorite(favorite: Favorite) {
        favoriteList.add(favorite)
    }

    fun removeFavorite(questionUid: String){  //引数のquestionUidを持っているFavoriteクラスがあれば削除する
        var index :Int? = null
        if (favoriteList.size > 0) {
            for (i in 0..favoriteList.size-1) {
                if (favoriteList[i].questionUid == questionUid) {
                   index = i
                }
            }
            if (index != null) {
                favoriteList.remove(favoriteList[index])
            }
        }
    }

    fun findKey(questionUid: String) :String? {  //見つからなかったらnullを返す
        if (favoriteList.size > 0) {
            for (i in 0..favoriteList.size-1) {
                if (favoriteList[i].questionUid == questionUid) {
                    return favoriteList[i].key
                }
            }
        }
        return null
    }

    fun createQuestionUidList() :ArrayList<String> {
        var questionUidList: ArrayList<String> = ArrayList()
        if (favoriteList.size > 0) {
            for (i in 0..favoriteList.size-1) {
                questionUidList.add(favoriteList[i].questionUid)
            }
        }
        return questionUidList
    }

    fun checkFavorite() {
        Log.d("check", "$favoriteList")
    }

    companion object {
        private var instance: QAApp? = null

        fun getInstance(): QAApp {
            if (instance == null)
                instance = QAApp()

            return instance!!
        }
    }

}