package search.youtube.gosling.james.searchyoutube;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import search.youtube.gosling.james.searchyoutube.Adapters.YoutubeAdapter;
import search.youtube.gosling.james.searchyoutube.Connector.YoutubeConnector;
import search.youtube.gosling.james.searchyoutube.models.VideoItem;


public class youtubeActivity extends AppCompatActivity {

    private YoutubeAdapter youtubeAdapter;
    private RecyclerView mRecyclerView;
    private ProgressDialog mProgressDialog;
    private Handler handler;
    private List<VideoItem> searchResults;
    String keyword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube);
        mProgressDialog = new ProgressDialog(this);
        getSupportActionBar().setTitle("Youtube");
        mRecyclerView = (RecyclerView) findViewById(R.id.videos_recycler_view);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        handler = new Handler();
        Bundle unpacker = getIntent().getExtras();
        keyword = unpacker.getString("keyword");
        mProgressDialog.setMessage("Finding videos for " +keyword);
        mProgressDialog.show();
        searchOnYoutube(keyword);
    }

    private void searchOnYoutube(final String keywords) {
        new Thread() {
            public void run() {
                YoutubeConnector yc = new YoutubeConnector(youtubeActivity.this);
                searchResults = yc.search(keywords);
                handler.post(new Runnable() {
                    public void run() {
                        fillYoutubeVideos();
                        mProgressDialog.dismiss();
                    }
                });
            }
        }.start();
    }

    private void fillYoutubeVideos() {
        youtubeAdapter = new YoutubeAdapter(getApplicationContext(), searchResults);
        mRecyclerView.setAdapter(youtubeAdapter);
        youtubeAdapter.notifyDataSetChanged();
    }
}