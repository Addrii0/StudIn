import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.R
import com.example.studin.classes.User

class UserAdapter(private val userList: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {


    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.textUserName)
        val userDescriptionTextView: TextView = itemView.findViewById(R.id.textUserDescription)
        val userSkillsTextView: TextView = itemView.findViewById(R.id.textUserSkills)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // Inflar el layout de list_item_user.xml
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_user, parent, false)
        return UserViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.userNameTextView.text = currentUser.name
        holder.userDescriptionTextView.text = currentUser.description
        holder.userSkillsTextView.text = currentUser.skills.joinToString(", ")

        }

    override fun getItemCount() = userList.size
}