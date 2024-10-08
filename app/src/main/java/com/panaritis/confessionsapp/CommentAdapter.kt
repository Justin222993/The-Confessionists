package com.panaritis.confessionsapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.panaritis.confessionsapp.model.CommentModel

class CommentAdapter(private var comments: List<CommentModel>) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    fun updateComments(newComments: List<CommentModel>) {
        comments = newComments
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(comment: CommentModel) {
            itemView.findViewById<TextView>(R.id.commentInfoTextView).text = comment.time_of_comment
            itemView.findViewById<TextView>(R.id.commentTextView).text = comment.comment_content
        }
    }
}
