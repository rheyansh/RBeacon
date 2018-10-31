package adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.info.rajsharma.rbeacon.*
import kotlinx.android.synthetic.main.message_list_item.view.*
import kotlinx.android.synthetic.main.my_message.view.*

private const val VIEW_TYPE_GENERAL = 1
private const val VIEW_TYPE_SRI_BEACON = 2


class MessageListAdapter (val context: Context) : RecyclerView.Adapter<MessageViewHolder>() {

    var items = mutableListOf<Any>()

    var onItemClick: ((sriBeacon: RBeacon) -> Unit)? = null

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {

        val item = items.get(position)

        if (item is RBeacon) {
            holder?.bind(item)
        } else {
            holder?.bind(item as String)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {

        if (viewType == VIEW_TYPE_SRI_BEACON) {
            return BeaconMessageViewHolder(LayoutInflater.from(context).inflate(R.layout.message_list_item, parent, false))
        }
        return NormalMessageViewHolder(LayoutInflater.from(context).inflate(R.layout.my_message, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {

        val item = items.get(position)

        if (item is String) {
            return VIEW_TYPE_GENERAL
        }
        return VIEW_TYPE_SRI_BEACON
    }

    inner class BeaconMessageViewHolder (view: View) : MessageViewHolder(view) {
        private var messageText: TextView = view.titleTextView

        override fun bind(beacon: RBeacon) {
            messageText.text = beacon.bMessage
        }

        init {
            itemView.setOnClickListener {

                val item = items.get(adapterPosition)

                if (item is RBeacon) {
                    onItemClick?.invoke(item)
                }
            }
        }
    }

    inner class NormalMessageViewHolder (view: View) : MessageViewHolder(view) {
        private var messageNormalText: TextView = view.txtMyMessage

        override fun bind(message: String) {
            messageNormalText.text = "${adapterPosition}:- " + message
        }
    }

    fun addMessage(message: String) {
        addMessages(arrayListOf(message))
    }

    fun addMessages(msgs: ArrayList<String>){
        items.addAll(msgs)
        notifyDataSetChanged()
    }

    fun removeAllMessages() {
        items.removeAll(items)
        notifyDataSetChanged()
    }

    fun addAllBeacons() {
        items = StaticData.getBeaconList().toMutableList()
        notifyDataSetChanged()
    }

}

open class MessageViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    open fun bind(message: String) {}
    open fun bind(beacon: RBeacon) {}

}