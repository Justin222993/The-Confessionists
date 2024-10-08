package com.panaritis.confessionsapp

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.panaritis.confessionsapp.database.DatabaseHelper
import com.panaritis.confessionsapp.model.ConfessionPostModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import kotlinx.coroutines.delay

class ShowPostsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private var currentPage = 1
    private var isEndReached = false
    private var isInitialFetchDone = false
    val db = DatabaseHelper()

    private var lastOpenedTimestamp: Long = 0

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            lifecycleScope.launch {
                // Delay for 1000 milliseconds
                delay(1000)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastOpenedTimestamp > 3000) { // Check if the app was opened more than 3 seconds ago
                    if (!isInitialFetchDone) {
                        fetchAndDisplayPosts()
                        isInitialFetchDone = true
                    } else {
                        Toast.makeText(this@ShowPostsActivity, "Connected to Internet", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            if (isConnectedToNetwork()) {
                refreshPosts()
            } else {
                Toast.makeText(this@ShowPostsActivity, "No internet connection", Toast.LENGTH_SHORT).show()
                val refreshButton: Button = findViewById(R.id.refresh)
                refreshButton.visibility = View.VISIBLE
                refreshButton.setOnClickListener {
                    if (isConnectedToNetwork()) {
                        if (!isInitialFetchDone) {
                            try {
                                launch {
                                    fetchAndDisplayPosts()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            refreshPosts()
                        }
                    } else {
                        Toast.makeText(this@ShowPostsActivity, "Refresh failed: No internet connection", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private fun unregisterNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.show_posts_activity)

        registerNetworkCallback()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = PostAdapter(emptyList()) { postId, comment, currentTime, editText ->
            if(isConnectedToNetwork()){
                if (comment.isEmpty()) {
                    Toast.makeText(this@ShowPostsActivity, "Please enter a comment", Toast.LENGTH_SHORT).show()
                } else if (comment.length < 1000) {
                    if (adapter.getCommentCount(postId) < 20) {
                        // Add comment to the database if the limit hasn't been reached
                        val commentModel = DatabaseHelper.ConfessionCommentModel(postId, comment, currentTime)
                        db.addComment(commentModel, this)

                        // Hide keyboard
                        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(editText.windowToken, 0)

                        editText.setText("")

                        // Refresh posts after adding comment, wait 1 second for latency with database
                        lifecycleScope.launch {
                            delay(1000)
                            refreshPosts()
                        }
                    } else {
                        // Notify the user that the comment limit has been reached
                        Toast.makeText(this@ShowPostsActivity, "Comment limit reached (20)", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ShowPostsActivity, "Comment should be under 1000 characters", Toast.LENGTH_SHORT).show()
                }
            }
            else {
                Toast.makeText(this@ShowPostsActivity, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = adapter

        setupScrollListener()

        val addPost: Button = findViewById(R.id.addPost)
        addPost.setOnClickListener {
            val intent = Intent(this, AddPostActivity::class.java)
            startActivity(intent)
        }
    }

    private fun refreshPosts() {
        MainScope().launch {
            try {
                val refreshButton: Button = findViewById(R.id.refresh)
                refreshButton.visibility = View.GONE
                val allPosts = fetchAllLoadedPosts()
                adapter.updatePosts(allPosts)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchAllLoadedPosts(): List<ConfessionPostModel> {
        val allPosts = mutableListOf<ConfessionPostModel>()

        // Loop through pages from 1 to currentPage
        for (page in 1..currentPage) {
            try {
                val newPosts = fetchPostsForPage(page)
                allPosts.addAll(newPosts)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return allPosts
    }

    private var isLoading = false

    private var lastRefreshTime: Long = 0

    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                val endReached = visibleItemCount + firstVisibleItemPosition >= totalItemCount
                        && firstVisibleItemPosition >= 0

                // Check if the user is scrolled to the top or scrolling upwards
                val isScrolledToTop = firstVisibleItemPosition == 0 && dy < 0
                // Calculate the current time
                val currentTime = System.currentTimeMillis()
                // Calculate the time difference since the last refresh
                val timeDifference = currentTime - lastRefreshTime

                if (!isLoading && !isEndReached && endReached) {
                    loadMorePosts()
                }

                // If scrolled to the top or scrolling upwards, and at least 5 seconds have passed since the last refresh
                if (isScrolledToTop && timeDifference >= 1000) {
                    refreshAndShowToast()
                    // Update the last refresh time
                    lastRefreshTime = currentTime
                }
            }
        })
    }

    private fun refreshAndShowToast() {
        MainScope().launch {
            if (isConnectedToNetwork()) {
                refreshPosts()
                val toast = Toast.makeText(this@ShowPostsActivity, "Refreshed", Toast.LENGTH_SHORT)
                toast.show()
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    toast.cancel()
                }, 600)
            } else {
                val toast = Toast.makeText(this@ShowPostsActivity, "Couldn't refresh: No internet connection", Toast.LENGTH_SHORT)
                toast.show()
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    toast.cancel()
                }, 1000)
            }
        }
    }

    private fun isConnectedToNetwork(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun loadMorePosts() {
        MainScope().launch {
            if (isConnectedToNetwork()) {
                if (!isLoading) {
                    isLoading = true
                    currentPage++
                    lifecycleScope.launch {
                        try {
                            val newPosts: List<ConfessionPostModel> = fetchPostsForPage(currentPage)
                            if (newPosts.isNotEmpty()) {
                                adapter.addPosts(newPosts)
                            } else {
                                isEndReached = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            } else {
                val toast = Toast.makeText(this@ShowPostsActivity, "No internet connection", Toast.LENGTH_SHORT)
                toast.show()
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    toast.cancel()
                }, 1200)
            }
        }
    }

    private suspend fun fetchAndDisplayPosts() {
        MainScope().launch {
            if (isConnectedToNetwork()) {
                val refreshButton: Button = findViewById(R.id.refresh)
                refreshButton.visibility = View.GONE
                try {
                    val newPosts: List<ConfessionPostModel> = fetchPostsForPage(1)
                    if (newPosts.isNotEmpty()) {
                        adapter.addPosts(newPosts)
                    } else {
                        isEndReached = true
                    }
                    isInitialFetchDone = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val toast = Toast.makeText(
                    this@ShowPostsActivity,
                    "No internet connection",
                    Toast.LENGTH_SHORT
                )
                toast.show()
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    toast.cancel()
                }, 2000)
            }
        }
    }

    private suspend fun fetchPostsForPage(page: Int): List<ConfessionPostModel> {
        val posts = mutableListOf<ConfessionPostModel>()

        val url = db.getUrlToServer() + "/confessionPost?page=$page"

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                val jsonResponse = response.body()?.string()
                if (!jsonResponse.isNullOrBlank()) {
                    val gson = Gson()
                    try {
                        val postArray = gson.fromJson(jsonResponse, Array<ConfessionPostModel>::class.java)
                        posts.addAll(postArray)
                    } catch (e: JsonSyntaxException) {
                        println("Error parsing JSON: ${e.message}")
                    }
                } else {
                    println("Empty response")
                }
            } catch (e: IOException) {
                val message = when (e) {
                    is java.net.UnknownHostException -> "Server not found"
                    is java.net.ConnectException -> "Connection refused"
                    is java.net.SocketTimeoutException -> "Connection timed out"
                    else -> "Server error occurred"
                }
                db.showServerErrorToast(this@ShowPostsActivity, message)
                e.printStackTrace()
            }
            return@withContext posts
        }
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
}
