package search.youtube.gosling.james.searchyoutube.Players;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;

import search.youtube.gosling.james.searchyoutube.R;
import search.youtube.gosling.james.searchyoutube.Connector.YoutubeConnector;

public class YoutubePlayer extends YouTubeBaseActivity implements OnInitializedListener {

    private YouTubePlayerView playerView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);


        setContentView(R.layout.activity_youtube_player);
        playerView = (YouTubePlayerView)findViewById(R.id.player_view);
        playerView.initialize(YoutubeConnector.KEY, this);

        TextView video_title = (TextView)findViewById(R.id.player_title);
        TextView video_desc = (TextView)findViewById(R.id.player_description);
        TextView video_id = (TextView)findViewById(R.id.player_id);

        video_title.setText(getIntent().getStringExtra("VIDEO_TITLE"));
        video_id.setText("Video ID : "+(getIntent().getStringExtra("VIDEO_ID")));
        video_desc.setText(getIntent().getStringExtra("VIDEO_DESC"));
    }

    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult result) {
        Toast.makeText(this,"Failed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player, boolean restored) {

        if(!restored){
            player.cueVideo(getIntent().getStringExtra("VIDEO_ID"));
        }
    }
}