package search.youtube.gosling.james.searchyoutube.models;

public class AudioItem {

    String name;
    String artist;
    String url;
    String title;
    String durationInLong;
    String durationInString;

    public AudioItem(String name, String artist, String url, String title, String durationInLong, String durationInString) {
        this.name = name;
        this.artist = artist;
        this.url = url;
        this.title = title;
        this.durationInLong = durationInLong;
        this.durationInString = durationInString;
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getDurationInLong() {
        return durationInLong;
    }

    public String getDurationInString() {
        return durationInString;
    }
}
