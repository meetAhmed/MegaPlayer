package search.youtube.gosling.james.searchyoutube.Adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import search.youtube.gosling.james.searchyoutube.Players.YoutubePlayer;
import search.youtube.gosling.james.searchyoutube.R;
import search.youtube.gosling.james.searchyoutube.models.VideoItem;

public class YoutubeAdapter extends RecyclerView.Adapter<YoutubeAdapter.MyViewHolder> {

    private Context mContext;
    private List<VideoItem> mVideoList;

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public ImageView thumbnail;
        public TextView video_title, video_id, video_description;
        public RelativeLayout video_view;

        public MyViewHolder(View view) {

            super(view);

            thumbnail = (ImageView) view.findViewById(R.id.video_thumbnail);
            video_title = (TextView) view.findViewById(R.id.video_title);
            video_id = (TextView) view.findViewById(R.id.video_id);
            video_description = (TextView) view.findViewById(R.id.video_description);
            video_view = (RelativeLayout) view.findViewById(R.id.video_view);
        }
    }

    public YoutubeAdapter(Context mContext, List<VideoItem> mVideoList) {
        this.mContext = mContext;
        this.mVideoList = mVideoList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        final VideoItem singleVideo = mVideoList.get(position);
        holder.video_id.setText("Video ID : " + singleVideo.getId() + " ");
        holder.video_title.setText(singleVideo.getTitle());
        holder.video_description.setText(singleVideo.getDescription());
        Picasso.with(mContext)
                .load(singleVideo.getThumbnailURL())
                .resize(480, 270)
                .centerCrop()
                .into(holder.thumbnail);

        holder.video_view.setOnClickListener(new View.OnClickListener() {

            //onClick method called when the view is clicked
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, YoutubePlayer.class);
                intent.putExtra("VIDEO_ID", singleVideo.getId());
                intent.putExtra("VIDEO_TITLE", singleVideo.getTitle());
                intent.putExtra("VIDEO_DESC", singleVideo.getDescription());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
        });
    }


    @Override
    public int getItemCount() {
        return mVideoList.size();
    }
}