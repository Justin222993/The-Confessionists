package com.panaritis.confessionsapp.database

import android.content.Context
import android.os.Handler
import android.widget.Toast
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import com.google.gson.Gson

class DatabaseHelper() {

              //This should be communicating with domain name, So I am hiding the IP Address of the server
    val url: String = "http://(SERVER-IP):5000";

    fun getUrlToServer(): String{
        return this.url
    }

    data class ConfessionPostModel(
        val post_title: String,
        val post_content: String,
        val time_of_post: String
    )

    fun addConfessionPost(confessions: ConfessionPostModel, context: Context) {
        val executor: Executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val url = getUrlToServer() + "/confessionPost"

            val gson = Gson()
            val json = gson.toJson(confessions)

            val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json)

            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                println(response.body()?.string() ?: "Empty response")
            } catch (e: IOException) {
                val message = when (e) {
                    is java.net.UnknownHostException -> "Server not found"
                    is java.net.ConnectException -> "Connection refused"
                    is java.net.SocketTimeoutException -> "Connection timed out"
                    else -> "Server error occurred"
                }
                showServerErrorToast(context, message)
                e.printStackTrace()
            }
        }
    }

    data class ConfessionCommentModel(
        val id: Int,
        val comment_content: String,
        val time_of_comment: String
    )

    fun addComment(comment: ConfessionCommentModel, context: Context) {
        val executor: Executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val url = getUrlToServer() + "/confessionComment"

        val gson = Gson()
            val json = gson.toJson(comment)

            val requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json)

            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                println(response.body()?.string() ?: "Empty response")
            } catch (e: IOException) {
                val message = when (e) {
                    is java.net.UnknownHostException -> "Server not found"
                    is java.net.ConnectException -> "Connection refused"
                    is java.net.SocketTimeoutException -> "Connection timed out"
                    else -> "Server error occurred"
                }
                showServerErrorToast(context, message)
                e.printStackTrace()
            }
        }
    }

    fun showServerErrorToast(context: Context, message: String) {
        Handler(context.mainLooper).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}


