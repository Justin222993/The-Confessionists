package com.panaritis.confessionsapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.panaritis.confessionsapp.database.DatabaseHelper
import com.panaritis.confessionsapp.database.DatabaseHelper.ConfessionPostModel
import java.util.Locale

class AddPostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_post_activity)

        //Animates the gif
        @SuppressLint("MissingInflatedId", "LocalSuppress")
        val imageView: ImageView = findViewById(R.id.imageView)
        Glide.with(this)
            .load(R.drawable.codingbehind)
            .into(imageView)

        val cancelButton: Button = findViewById(R.id.cancelBtn)
        cancelButton.setOnClickListener {
            finish()
        }

        val submitBtn: Button = findViewById(R.id.submitBtn)
        submitBtn.setOnClickListener {

            val postTitleGrabber: EditText = findViewById(R.id.titleEdit)
            val postContentGrabber: EditText = findViewById(R.id.postEdit)
            val title = postTitleGrabber.text.toString()
            val content = postContentGrabber.text.toString()

            if(isConnectedToNetwork()){
                if (title.isEmpty()) {
                    Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
                } else if (content.isEmpty()) {
                    Toast.makeText(this, "Please enter content", Toast.LENGTH_SHORT).show()
                } else if (title.length < 300) {
                    if (content.length < 1000) {
                        val db = DatabaseHelper()
                        val ConfessionPost = ConfessionPostModel(title, content, getCurrentTime())
                        db.addConfessionPost(ConfessionPost, this@AddPostActivity)

                        postTitleGrabber.text.clear()
                        postContentGrabber.text.clear()

                        val intent = Intent(this, ShowPostsActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Content should be under 1000 characters", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Title should be under 300 characters", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isConnectedToNetwork(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        currentFocus?.let { view ->
            if (ev?.action == MotionEvent.ACTION_DOWN) {
                val view = currentFocus
                if (view is EditText || view is Button) {
                    val outRect = Rect()
                    view.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                        view.clearFocus()
                        val imm =
                            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(view.windowToken, 0)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun getCurrentTime(): String {
        val currentTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("MMMM dd'${getDayOfMonthSuffix(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))}' yyyy 'at' HH:mm", Locale.getDefault())
        return dateFormat.format(currentTime)
    }

    private fun getDayOfMonthSuffix(n: Int): String {
        return if (n in 11..13) {
            "th"
        } else when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}
