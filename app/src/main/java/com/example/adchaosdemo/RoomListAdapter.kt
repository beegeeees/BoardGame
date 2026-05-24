package com.example.adchaosdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RoomListAdapter(
    private val myNickname: String,
    private val onClick: (DemoRoom) -> Unit
) : RecyclerView.Adapter<RoomListAdapter.RoomViewHolder>() {

    private val items = mutableListOf<DemoRoom>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(items[position], myNickname)
    }

    override fun getItemCount(): Int = items.size

    fun submit(data: List<DemoRoom>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    class RoomViewHolder(
        itemView: View,
        private val onClick: (DemoRoom) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val roomCodeText: TextView = itemView.findViewById(R.id.roomItemCode)
        private val roomHostText: TextView = itemView.findViewById(R.id.roomItemHost)
        private val roomCountText: TextView = itemView.findViewById(R.id.roomItemCount)

        fun bind(room: DemoRoom, myNickname: String) {
            roomCodeText.text = itemView.context.getString(R.string.lobby_room_code, room.roomCode)
            val isMineHost = room.hostNickname == myNickname || room.hostNickname == "본인"
            if (isMineHost) {
                roomHostText.visibility = View.GONE
            } else {
                roomHostText.visibility = View.VISIBLE
                roomHostText.text = itemView.context.getString(R.string.room_host_format, room.hostNickname)
            }
            roomCountText.text = itemView.context.getString(
                R.string.room_count_format,
                room.currentCount,
                room.maxCount
            )
            itemView.setOnClickListener { onClick(room) }
        }
    }
}
