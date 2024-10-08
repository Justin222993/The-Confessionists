package com.panaritis.confessionsapp

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.panaritis.confessionsapp.model.ConfessionPostModel
import java.util.Locale

class PostAdapter(private var posts: List<ConfessionPostModel>, private val commentClickListener: (Int, String, String, EditText) -> Unit) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post, commentClickListener)
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    fun getCommentCount(postId: Int): Int {
        val post = posts.find { it.id == postId }
        return post?.comments?.size ?: 0
    }

    fun addPosts(newPosts: List<ConfessionPostModel>) {
        val startPosition = posts.size
        val mutablePosts = posts.toMutableList()
        mutablePosts.addAll(newPosts)
        posts = mutablePosts
        notifyItemRangeInserted(startPosition, newPosts.size)
    }

    fun updatePosts(newPosts: List<ConfessionPostModel>) {
        val statesMap = mutableMapOf<Int, Boolean>()
        posts.forEach { post ->
            statesMap[post.id] = post.isButtonOpen
        }

        val diffCallback = PostDiffCallback(posts, newPosts)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)

        posts = newPosts
        posts.forEach { post ->
            post.isButtonOpen = statesMap[post.id] ?: false
        }

    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.commentsRecyclerView)
        private val adapter: CommentAdapter = CommentAdapter(emptyList())
        private val showHideCommentsButton: Button = itemView.findViewById(R.id.showHideCommentsButton)

        fun bind(post: ConfessionPostModel, commentClickListener: (Int, String, String, EditText) -> Unit) {
            itemView.findViewById<TextView>(R.id.titleTextView).text = post.post_title
            itemView.findViewById<TextView>(R.id.contentTextView).text = post.post_content
            itemView.findViewById<TextView>(R.id.timeTextView).text = "Posted on " + post.time_of_post

            val commentEditText = itemView.findViewById<EditText>(R.id.commentEditText)

            itemView.findViewById<Button>(R.id.commentButton).setOnClickListener {
                val comment = commentEditText.text.toString()
                val postId = post.id
                val currentTime = getCurrentTime()
                commentClickListener.invoke(postId, comment, currentTime, commentEditText)
            }

            val noCommentsTextView = itemView.findViewById<TextView>(R.id.noCommentsTextView)

            if (post.comments.isNotEmpty()) {
                adapter.updateComments(post.comments)
                recyclerView.visibility = View.VISIBLE
                noCommentsTextView.visibility = View.GONE
                showHideCommentsButton.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.GONE
                noCommentsTextView.visibility = View.VISIBLE
                showHideCommentsButton.visibility = View.GONE
                post.isButtonOpen = false
            }

            if (!post.isButtonOpen) {
                showHideCommentsButton.text = "Show Comments"
                recyclerView.visibility = View.GONE
                post.isButtonOpen = false
            } else {
                showHideCommentsButton.text = "Hide Comments"
                recyclerView.visibility = View.VISIBLE
                post.isButtonOpen = true
            }

            showHideCommentsButton.setOnClickListener {
                if (recyclerView.visibility == View.VISIBLE) {
                    recyclerView.visibility = View.GONE
                    showHideCommentsButton.text = "Show Comments"
                    post.isButtonOpen = false
                } else {
                    recyclerView.visibility = View.VISIBLE
                    showHideCommentsButton.text = "Hide Comments"
                    post.isButtonOpen = true
                }
            }

            recyclerView.layoutManager = LinearLayoutManager(itemView.context)
            recyclerView.adapter = adapter
            adapter.updateComments(post.comments)
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
}
