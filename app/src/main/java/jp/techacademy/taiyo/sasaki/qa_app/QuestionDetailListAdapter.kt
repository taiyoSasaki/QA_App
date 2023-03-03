package jp.techacademy.taiyo.sasaki.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.list_question_detail.view.*


class QuestionDetailListAdapter(context: Context, private val mQuestion: Question) : BaseAdapter() {
    companion object {
        private val TYPE_QUESTION = 0
        private val TYPE_ANSWER = 1
    }

    private var mLayoutInflater: LayoutInflater? = null

    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    //お気に入り追加
    var onClickAddFavorite: ((Question) -> Unit)? = null
    //お気に入り削除
    var onClickRemoveFavorite: ((Question) -> Unit)? = null

    override fun getCount(): Int {
        return 1 + mQuestion.answers.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            TYPE_QUESTION
        } else {
            TYPE_ANSWER
        }
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Any {
        return mQuestion
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        var convertView = view

        if (getItemViewType(position) == TYPE_QUESTION) {
            if (convertView == null) {
                convertView = mLayoutInflater!!.inflate(R.layout.list_question_detail, parent, false)!!
            }
            val body = mQuestion.body
            val name = mQuestion.name

            val bodyTextView = convertView.bodyTextView as TextView
            bodyTextView.text = body

            val nameTextView = convertView.nameTextView as TextView
            nameTextView.text = name

            val user = FirebaseAuth.getInstance().currentUser
            val mDatabaseReference = FirebaseDatabase.getInstance().reference

            if (user != null) {  //ログインされている場合
                val favoriteImageView = convertView.favoriteImageView as ImageView
                favoriteImageView.isVisible = true

                //お気に入り情報の取得
                val myApp = QAApp.getInstance()
                val isFavorite = myApp.findBy(mQuestion.questionUid) //Applicationクラスでお気に入りのQuestionUidを保持、そこから確認

                favoriteImageView.apply {
                    setImageResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border)
                    setOnClickListener {
                        if (isFavorite) {
                            //お気に入り削除発火!
                            onClickRemoveFavorite?.invoke(mQuestion)
                        } else {
                            //お気に入り追加発火!
                            onClickAddFavorite?.invoke(mQuestion)
                        }
                        notifyDataSetChanged()
                    }
                }
            } else {
                val favoriteImageView = convertView.favoriteImageView as ImageView
                favoriteImageView.isVisible = false
            }

            val bytes = mQuestion.imageBytes
            if (bytes.isNotEmpty()) {
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
                val imageView = convertView.findViewById<View>(R.id.imageView) as ImageView
                imageView.setImageBitmap(image)
            }
        } else {
            if (convertView == null) {
                convertView = mLayoutInflater!!.inflate(R.layout.list_answer, parent, false)!!
            }

            val answer = mQuestion.answers[position - 1]
            val body = answer.body
            val name = answer.name

            val bodyTextView = convertView.bodyTextView as TextView
            bodyTextView.text = body

            val nameTextView = convertView.nameTextView as TextView
            nameTextView.text= name
        }

        return convertView
    }

}
