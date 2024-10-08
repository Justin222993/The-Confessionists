package com.panaritis.confessionsapp

import androidx.recyclerview.widget.DiffUtil
import com.panaritis.confessionsapp.model.ConfessionPostModel

class PostDiffCallback(
    private val oldList: List<ConfessionPostModel>,
    private val newList: List<ConfessionPostModel>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
