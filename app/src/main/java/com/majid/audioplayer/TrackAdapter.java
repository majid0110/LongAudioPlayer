package com.majid.audioplayer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class TrackAdapter extends ArrayAdapter<AudioTrack> {

    private final LayoutInflater inflater;
    private int playingPosition = -1;

    public TrackAdapter(Context context, List<AudioTrack> tracks) {
        super(context, 0, tracks);
        inflater = LayoutInflater.from(context);
    }

    public void setPlayingPosition(int pos) {
        this.playingPosition = pos;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_track, parent, false);
            holder = new ViewHolder();
            holder.tvIndex = (TextView) convertView.findViewById(R.id.tv_index);
            holder.tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
            holder.tvArtist = (TextView) convertView.findViewById(R.id.tv_artist);
            holder.tvDuration = (TextView) convertView.findViewById(R.id.tv_duration_item);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AudioTrack track = getItem(position);

        boolean isPlaying = position == playingPosition;

        holder.tvIndex.setText(isPlaying ? "▶" : String.valueOf(position + 1));
        holder.tvIndex.setTextColor(isPlaying ? Color.parseColor("#E94560") : Color.parseColor("#AAAAAA"));

        holder.tvTitle.setText(track.title);
        holder.tvTitle.setTextColor(isPlaying ? Color.parseColor("#E94560") : Color.parseColor("#FFFFFF"));

        holder.tvArtist.setText(track.artist);
        holder.tvDuration.setText(track.getDurationFormatted());

        convertView.setBackgroundColor(
            isPlaying ? Color.parseColor("#1F1F3A") : Color.parseColor("#1A1A2E")
        );

        return convertView;
    }

    static class ViewHolder {
        TextView tvIndex;
        TextView tvTitle;
        TextView tvArtist;
        TextView tvDuration;
    }
}
