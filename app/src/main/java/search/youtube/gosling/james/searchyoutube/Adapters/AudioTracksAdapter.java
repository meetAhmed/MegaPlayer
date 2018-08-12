package search.youtube.gosling.james.searchyoutube.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import search.youtube.gosling.james.searchyoutube.R;
import search.youtube.gosling.james.searchyoutube.store.StorageUtil;
import search.youtube.gosling.james.searchyoutube.models.AudioItem;
import search.youtube.gosling.james.searchyoutube.store.storeRoom;

public class AudioTracksAdapter extends RecyclerView.Adapter<AudioTracksAdapter.myHolder> {

    Context context;
    ArrayList<AudioItem> audioItemArrayList;
    private OnItemClickListener listener;
    public static int index = -9;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }


    public AudioTracksAdapter(ArrayList<AudioItem> AudioItems, OnItemClickListener listener) {
        this.audioItemArrayList = AudioItems;
        this.listener = listener;
    }

    @Override
    public myHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_track_row, null);
        myHolder obj = new myHolder(v);
        return obj;
    }

    @Override
    public void onBindViewHolder(final myHolder holder, final int position) {

        holder.chapterName.setText(audioItemArrayList.get(position).getTitle());
        holder.chapterCode.setText(audioItemArrayList.get(position).getArtist());
        holder.trackLength.setText(audioItemArrayList.get(position).getDurationInString());

        Bitmap cover = storeRoom.coverPicture(audioItemArrayList.get(position).getUrl());
        if (cover != null) {
            holder.thumb.setImageBitmap(cover);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(position);
            }
        });

        int storageIndex = new StorageUtil(context).loadAudioIndex();

        if (position == index || position == storageIndex) {
            holder.parentLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.background_grey));
        } else {
            holder.parentLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }
    }

    @Override
    public int getItemCount() {
        return audioItemArrayList.size();
    }

    public class myHolder extends RecyclerView.ViewHolder {
        TextView chapterName, chapterCode, trackLength;
        ImageView thumb;
        public LinearLayout parentLayout;

        public myHolder(View itemView) {
            super(itemView);
            context = itemView.getContext();

            chapterCode = (TextView) itemView.findViewById(R.id.chapterCode);
            chapterName = (TextView) itemView.findViewById(R.id.chapterName);
            trackLength = (TextView) itemView.findViewById(R.id.trackLength);
            thumb = (ImageView) itemView.findViewById(R.id.thumb);
            parentLayout = (LinearLayout) itemView.findViewById(R.id.parentLayout);
        }
    }
}