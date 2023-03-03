package jp.techacademy.taiyo.sasaki.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.activity_question_detail.progressBar


class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoriteRef: DatabaseReference

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                //同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        //ログインしているユーザーのお気に入りフォルダのパス指定
        mDatabaseReference = FirebaseDatabase.getInstance().reference
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            mFavoriteRef = mDatabaseReference.child(FavoritesPATH).child(user.uid)
        }

        //渡ってきたQuestionのオブジェクトを保存する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        //ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)

        //お気に入りクリックの受け取り
        mAdapter.apply {
            onClickAddFavorite = {
                // プログレスバーを表示する
                progressBar.visibility = View.VISIBLE
                //Firebaseへ追加するときのkeyを取得
                val key = mFavoriteRef.push().key ?: ""

                val favorite = Favorite(key, it.questionUid)
                val data = favorite.toMap()

                val childUpdates = hashMapOf<String, Any>(key to data)
                //Firebaseを更新する
                mFavoriteRef.updateChildren(childUpdates)
                    .addOnSuccessListener {
                        //保存の完了メソッド
                        val myApp = QAApp.getInstance()
                        myApp.addFavorite(favorite)
                        mAdapter.notifyDataSetChanged()
                        //プログレスバーを非表示にする
                        progressBar.visibility = View.GONE
                    }
            }

            onClickRemoveFavorite = {
                // プログレスバーを表示する
                progressBar.visibility = View.VISIBLE
                val questionUid = it.questionUid

                //QAAppのお気に入りリストから削除し、同時にFirebaseから削除するためのkeyを取得
                val myApp = QAApp.getInstance()
                val key = myApp.findKey(questionUid)
                if (key != null) {
                    val childUpdates = hashMapOf<String, Any?>(key to null)
                    //Firebaseを更新する
                    mFavoriteRef.updateChildren(childUpdates)
                        .addOnSuccessListener {
                            //保存の完了メソッド
                            myApp.removeFavorite(questionUid)
                            mAdapter.notifyDataSetChanged()
                            //プログレスバーを非表示にする
                            progressBar.visibility = View.GONE
                        }
                }
                mAdapter.notifyDataSetChanged()
                //プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            //ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                //ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                //Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)
    }
}
