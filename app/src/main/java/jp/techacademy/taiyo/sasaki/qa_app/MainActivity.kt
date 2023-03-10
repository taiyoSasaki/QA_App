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

            //??????????????????Question?????????
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    //???????????????????????????????????????????????????????????????(Answer)??????
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

        //id???toolbar????????????????????????????????????????????????????????????
        //id??????ActionBar????????????????????????
        setSupportActionBar(toolbar)

        //fab???Click?????????????????????
        fab.setOnClickListener { view ->
            //?????????????????????????????????????????? (mGenre == 0)?????????????????????????????????
            if (mGenre == 0 or 5) {
                Snackbar.make(view, getString(R.string.question_no_select_genre), Snackbar.LENGTH_LONG).show()
            } else {
                //????????????????????????????????????????????????
                val user = FirebaseAuth.getInstance().currentUser

                //?????????????????????????????????????????????????????????????????????
                if (user == null) {
                    val intent = Intent(applicationContext, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    //????????????????????????????????????????????????????????????
                    val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                    intent.putExtra("genre", mGenre)
                    startActivity(intent)
                }
            }

        }

        //??????????????????????????????????????????
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.app_name, R.string.app_name)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        //Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        //ListView?????????
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        listView.setOnItemClickListener {parent, view, position, id ->
            if (mFavoriteRef != null) {
                mFavoriteRef!!.removeEventListener(mFavoriteEventListener)
            }
            //Question??????????????????????????????????????????????????????????????????
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // 1:?????????????????????????????????
        if (mGenre == 0) {
            onNavigationItemSelected(nav_view.menu.getItem(0))
        }

        //?????????????????????????????????????????????????????????????????????????????????
        var user = FirebaseAuth.getInstance().currentUser
        if (user == null) {  //???????????????????????????
            val menu = nav_view.menu.getItem(4)
            menu.isVisible = false
        } else {  //????????????????????????
            val menu = nav_view.menu.getItem(4)
            menu.isVisible = true

            //???????????????????????????????????????????????????????????????????????????
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

        //????????????????????????????????????????????????Adapter???ListView????????????????????????
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

        if (mGenre == 5) {
            //???????????????????????????questionUid??????????????????
            val myApp = QAApp.getInstance()
            mFavoriteQuestionUidList = myApp.createQuestionUidList()

            //?????????????????????????????????contents?????????????????????????????????????????????????????????questionUid?????????Question?????????
            if (mGenreRef != null) {
                mGenreRef!!.removeEventListener(mEventListener)
            }
            mGenreRef = mDatabaseReference.child(ContentsPATH)
            mGenreRef!!.addChildEventListener(mContentsListener)

        } else {
            //??????????????????????????????????????????????????????
            if (mGenreRef != null) {
                mGenreRef!!.removeEventListener(mEventListener)
            }
            mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
            mGenreRef!!.addChildEventListener(mEventListener)
        }
        return true
    }

}