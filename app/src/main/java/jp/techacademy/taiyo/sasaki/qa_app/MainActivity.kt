package jp.techacademy.taiyo.sasaki.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() , NavigationView.OnNavigationItemSelectedListener {

    private var mGenre = 0

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private var mFavoriteList: ArrayList<Favorite> = ArrayList<Favorite>()
    private var mFavoriteQuestionUidList: ArrayList<String> = ArrayList<String>()
    private lateinit var mAdapter: QuestionsListAdapter

    private var mGenreRef: DatabaseReference? = null
    private var mFavoriteRef: DatabaseReference? = null

    private val mFavoriteEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            val map = dataSnapshot.value as Map<String, String>
            val key = dataSnapshot.key ?: ""
            val questionUid = map["questionUid"] ?: ""

            val favorite = Favorite(key, questionUid)
            mFavoriteList.add(favorite)
            mAdapter.notifyDataSetChanged()

            val myApp = QAApp.getInstance()
            myApp.setFavorite(mFavoriteList)
            myApp.checkFavorite()
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {

        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

        }

        override fun onCancelled(error: DatabaseError) {

        }
    }

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answer"] as Map<String, String>?
            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val temp = answerMap[key] as Map<String, String>
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    answerArrayList.add(answer)
                }
            }

            val question = Question(title, body, name, uid, dataSnapshot.key ?: "", mGenre, bytes, answerArrayList)
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            //変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    //このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }

                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {

        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }

    }

    private var mContentsListener= object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            Log.d("onAdd", "$dataSnapshot")
            val data = dataSnapshot.value as Map<String, Any>
            for (id in data.keys) {
                for (questionUid in mFavoriteQuestionUidList) {
                    if (id == questionUid) {
                        val map = data[id] as Map<String, String>
                        val title = map["title"] ?: ""
                        val body = map["body"] ?: ""
                        val name = map["name"] ?: ""
                        val uid = map["uid"] ?: ""
                        val imageString = map["image"] ?: ""
                        val bytes =
                            if (imageString.isNotEmpty()) {
                                Base64.decode(imageString, Base64.DEFAULT)
                            } else {
                                byteArrayOf()
                            }

                        val answerArrayList = ArrayList<Answer>()
                        val answerMap = map["answer"] as Map<String, String>?
                        if (answerMap != null) {
                            for (key in answerMap.keys) {
                                val temp = answerMap[key] as Map<String, String>
                                val answerBody = temp["body"] ?: ""
                                val answerName = temp["name"] ?: ""
                                val answerUid = temp["uid"] ?: ""
                                val answer = Answer(answerBody, answerName, answerUid, key)
                                answerArrayList.add(answer)
                            }
                        }

                        val question = Question(title, body, name, uid, questionUid, dataSnapshot.key!!.toInt(), bytes, answerArrayList)
                        mQuestionArrayList.add(question)
                        mAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

        }

        override fun onChildRemoved(snapshot: DataSnapshot) {

        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

        }

        override fun onCancelled(error: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //idがtoolbarのインポート宣言により取得されているので
        //id名でActionBarのサポートを依頼
        setSupportActionBar(toolbar)

        //fabにClickリスナーを登録
        fab.setOnClickListener { view ->
            //ジャンルを選択していない場合 (mGenre == 0)はエラーを表示するだけ
            if (mGenre == 0 or 5) {
                Snackbar.make(view, getString(R.string.question_no_select_genre), Snackbar.LENGTH_LONG).show()
            } else {
                //ログイン済みのユーザーを取得する
                val user = FirebaseAuth.getInstance().currentUser

                //ログインしていなければログイン画面に遷移させる
                if (user == null) {
                    val intent = Intent(applicationContext, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    //ジャンルを渡して質問作成画面に遷移させる
                    val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                    intent.putExtra("genre", mGenre)
                    startActivity(intent)
                }
            }

        }

        //ナビゲーションドロワーの設定
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.app_name, R.string.app_name)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        //Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        //ListViewの準備
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        listView.setOnItemClickListener {parent, view, position, id ->
            if (mFavoriteRef != null) {
                mFavoriteRef!!.removeEventListener(mFavoriteEventListener)
            }
            //Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // 1:趣味を規定の選択とする
        if (mGenre == 0) {
            onNavigationItemSelected(nav_view.menu.getItem(0))
        }

        //ログインしていないときにお気に入りボタンを非表示にする
        var user = FirebaseAuth.getInstance().currentUser
        if (user == null) {  //ログインしていない
            val menu = nav_view.menu.getItem(4)
            menu.isVisible = false
        } else {  //ログインしている
            val menu = nav_view.menu.getItem(4)
            menu.isVisible = true

            //ログインしているユーザーのお気に入りにリスナー登録
            mFavoriteList.clear()
            val myApp = QAApp.getInstance()
            myApp.setFavorite(mFavoriteList)

            if (mFavoriteRef != null) {
                mFavoriteRef!!.removeEventListener(mFavoriteEventListener)
            }
            mFavoriteRef = mDatabaseReference.child(FavoritesPATH).child(user.uid)
            mFavoriteRef!!.addChildEventListener(mFavoriteEventListener)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.nav_hobby) {
            toolbar.title = getString(R.string.menu_hobby_label)
            mGenre = 1
        } else if (id == R.id.nav_life) {
            toolbar.title = getString(R.string.menu_life_label)
            mGenre = 2
        } else if (id == R.id.nav_health) {
            toolbar.title = getString(R.string.menu_health_label)
            mGenre = 3
        } else if (id == R.id.nav_computer) {
            toolbar.title = getString(R.string.menu_computer_label)
            mGenre = 4
        } else if (id == R.id.nav_favorite) {
            toolbar.title = getString(R.string.menu_favorite_label)
            mGenre = 5
        }

        drawer_layout.closeDrawer(GravityCompat.START)

        //質問のリストをクリアしてから再度AdapterをListViewにセットしなおす
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

        if (mGenre == 5) {
            //お気に入りリストのquestionUidリストを取得
            val myApp = QAApp.getInstance()
            mFavoriteQuestionUidList = myApp.createQuestionUidList()

            //全ジャンルを有しているcontentsにリスナーを登録し、お気に入りリストのquestionUidを持つQuestionを探す
            if (mGenreRef != null) {
                mGenreRef!!.removeEventListener(mEventListener)
            }
            mGenreRef = mDatabaseReference.child(ContentsPATH)
            mGenreRef!!.addChildEventListener(mContentsListener)

        } else {
            //選択したジャンルにリスナーを登録する
            if (mGenreRef != null) {
                mGenreRef!!.removeEventListener(mEventListener)
            }
            mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
            mGenreRef!!.addChildEventListener(mEventListener)
        }
        return true
    }

}