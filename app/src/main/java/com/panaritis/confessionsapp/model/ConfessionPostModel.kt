package com.panaritis.confessionsapp.model

class ConfessionPostModel(
    var id: Int = 0,
    var post_title: String = "",
    var post_content: String = "",
    var time_of_post: String = "",
    var comments: List<CommentModel> = mutableListOf(),
    var isButtonOpen: Boolean = false
)