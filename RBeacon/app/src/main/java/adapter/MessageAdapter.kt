package adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.*
import com.info.rajsharma.rbeacon.*
import kotlinx.android.synthetic.main.my_message.view.*


internal const val VIEW_TYPE_MY_MESSAGE = 1

class MessageAdapter (val context: Context) : RecyclerView.Adapter<MessageViewHolder>() {

    private val messages: ArrayList<String> = ArrayList()

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages.get(position)
        holder?.bind(message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MyMessageViewHolder(LayoutInflater.from(context).inflate(R.layout.my_message, parent, false))
    }

    fun addMessage(message: String) {
        addMessages(arrayListOf(message))
    }


    fun removeAllMessages() {
        messages.removeAll(messages)
        notifyDataSetChanged()
    }

    fun addMessages(msgs: java.util.ArrayList<String>){
        messages.addAll(msgs)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_MY_MESSAGE
    }

    inner class MyMessageViewHolder (view: View) : MessageViewHolder(view) {
        private var messageText: TextView = view.txtMyMessage

        override fun bind(message: String) {
            messageText.text = "${adapterPosition}:- " + message
        }
    }
}
