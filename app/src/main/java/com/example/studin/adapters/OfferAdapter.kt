import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studin.R
import com.example.studin.classes.Offer

class OfferAdapter(
    private val offerList: List<Offer>,
    private val onItemClicked: (Offer) -> Unit
) : RecyclerView.Adapter<OfferAdapter.OfferViewHolder>() {


    // ViewHolder que contiene las vistas de list_item_offer.xml
    class OfferViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewOfferTitle)
        val companyTextView: TextView = itemView.findViewById(R.id.textViewCompanyName)
        val locationTextView: TextView = itemView.findViewById(R.id.textViewOfferLocation)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewOfferDescription)

        fun bind(offer: Offer, onItemClicked: (Offer) -> Unit) {
            titleTextView.text = offer.title
            companyTextView.text = offer.companyName
            locationTextView.text = offer.location
            descriptionTextView.text = offer.description

            itemView.setOnClickListener {
                onItemClicked(offer)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offer, parent, false)
        return OfferViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offerList[position]
        holder.bind(offer, onItemClicked)
    }

    override fun getItemCount(): Int {
        return offerList.size
    }
}