package com.majid.audioplayer;

public class AudioTrack {
    public final long id;
    public final String title;
    public final String artist;
    public final String album;
    public final long duration; // ms
    public final String path;
    public final long size;

    public AudioTrack(long id, String title, String artist, String album,
                      long duration, String path, long size) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.path = path;
        this.size = size;
    }

    /** Returns formatted duration string HH:MM:SS or MM:SS */
    public String getDurationFormatted() {
        return formatTime((int)(duration / 1000));
    }

    public static String formatTime(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s);
        } else {
            return String.format("%d:%02d", m, s);
        }
    }
}
