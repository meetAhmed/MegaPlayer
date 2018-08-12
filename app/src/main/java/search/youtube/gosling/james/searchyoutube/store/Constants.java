package search.youtube.gosling.james.searchyoutube.store;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import search.youtube.gosling.james.searchyoutube.R;

public class Constants {

    public interface ACTION {
        public static String MAIN_ACTION = "search.youtube.gosling.james.searchyoutube.action.main";
        public static String INIT_ACTION = "search.youtube.gosling.james.searchyoutube.action.init";
        public static String PREV_ACTION = "search.youtube.gosling.james.searchyoutube.action.prev";
        public static String PLAY_ACTION = "search.youtube.gosling.james.searchyoutube.action.play";
        public static String NEXT_ACTION = "search.youtube.gosling.james.searchyoutube.action.next";
        public static String STARTFOREGROUND_ACTION = "search.youtube.gosling.james.searchyoutube.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "search.youtube.gosling.james.searchyoutube.action.stopforeground";

    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }

    public static Bitmap getDefaultAlbumArt(Context context) {
        Bitmap bm = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            bm = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.cd, options);
        } catch (Error ee) {
        } catch (Exception e) {
        }
        return bm;
    }
}