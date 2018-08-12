package search.youtube.gosling.james.searchyoutube.Connector;

import android.content.Context;
import android.util.Log;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import search.youtube.gosling.james.searchyoutube.models.VideoItem;

public class YoutubeConnector {

    private YouTube youtube;
    private YouTube.Search.List query;
    public static final String KEY = "key";
    public static final String PACKAGENAME = "pkg";
    public static final String SHA1 = "sha";
    private static final long MAXRESULTS = 25;
    public YoutubeConnector(Context context) {
        youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {

            //initialize method helps to add any extra details that may be required to process the query
            @Override
            public void initialize(HttpRequest request) throws IOException {
                request.getHeaders().set("X-Android-Package", PACKAGENAME);
                request.getHeaders().set("X-Android-Cert",SHA1);
            }
        }).setApplicationName("SearchYoutube").build();

        try {
            query = youtube.search().list("id,snippet");
            query.setKey(KEY);
            query.setType("video");
            query.setFields("items(id/kind,id/videoId,snippet/title,snippet/description,snippet/thumbnails/high/url)");

        } catch (IOException e) {
            Log.d("YC", "Could not initialize: " + e);
        }
    }

    public List<VideoItem> search(String keywords) {
        query.setQ(keywords);
        query.setMaxResults(MAXRESULTS);

        try {
            SearchListResponse response = query.execute();
            List<SearchResult> results = response.getItems();
            List<VideoItem> items = new ArrayList<VideoItem>();
            if (results != null) {
                items = setItemsList(results.iterator());
            }

            return items;

        } catch (IOException e) {
            Log.d("YC", "Could not search: " + e);
            return null;
        }
    }

    //method for filling our array list
    private static List<VideoItem> setItemsList(Iterator<SearchResult> iteratorSearchResults) {
        List<VideoItem> tempSetItems = new ArrayList<>();
        if (!iteratorSearchResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }
        while (iteratorSearchResults.hasNext()) {
            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();
            if (rId.getKind().equals("youtube#video")) {
                VideoItem item = new VideoItem();
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getHigh();
                Log.i("32434",thumbnail+"");
                Log.i("32434",singleVideo+"");
                item.setId(singleVideo.getId().getVideoId());
                item.setTitle(singleVideo.getSnippet().getTitle());
                item.setDescription(singleVideo.getSnippet().getDescription());
                item.setThumbnailURL(thumbnail.getUrl());
                tempSetItems.add(item);
            }
        }
        return tempSetItems;
    }
}
