package com.duan.musicoco.play;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.duan.musicoco.R;
import com.duan.musicoco.aidl.IPlayControl;
import com.duan.musicoco.aidl.Song;
import com.duan.musicoco.app.MediaManager;
import com.duan.musicoco.app.SongInfo;
import com.duan.musicoco.service.PlayController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by DuanJiaNing on 2017/6/22.
 */

public class PlayListAdapter extends BaseAdapter {

    private static final String TAG = "PlayListAdapter";
    private final List<SongInfo> songs;

    private final IPlayControl control;
    private final PlayActivity activity;

    private final View.OnClickListener removeClicklistener;
    private final View.OnClickListener itemClickListener;

    private final MediaManager mediaManager;

    public PlayListAdapter(final PlayActivity activity, final IPlayControl control) {
        this.activity = activity;
        this.control = control;
        this.mediaManager = MediaManager.getInstance(activity);
        this.removeClicklistener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (int) v.getTag(R.id.play_list_item_remove_position);
                Song s = new Song((String) v.getTag(R.id.play_list_item_remove_path));
                try {
                    //如果移除当前正在播放曲目服务端会自动跳到下一首
                    control.remove(s);
                    if (position < activity.currentIndex)
                        activity.currentIndex--;
                    updateData();
                    notifyDataSetChanged();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };

        this.itemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = (int) v.getTag(R.id.play_list_item_position);
                if (pos == activity.currentIndex) {
                    Log.d(TAG, "onClick: same song");
                    try {
                        if (control.status() != PlayController.STATUS_PLAYING)
                            control.resume();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    return;
                }

                if (pos < activity.currentIndex)
                    activity.isPre = true;

                SongInfo in = (SongInfo) getItem(pos);
                try {
                    control.play(new Song(in.getData()));
                    notifyDataSetChanged();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };

        this.songs = new ArrayList<>();
        updateData();
    }

    private void updateData() {
        try {
            //获得播放列表，注意要从服务器获取播放列表
            List<Song> ss = control.getPlayList();
            songs.clear();
            for (Song s : ss)
                //从客户端保存的数据中得到信息，有必要的话要刷新客户端的数据
                songs.add(mediaManager.getSongInfo(s));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = activity.getLayoutInflater().inflate(R.layout.activity_play_list_item, null);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.play_list_item_name);
            holder.arts = (TextView) convertView.findViewById(R.id.play_list_item_arts);
            holder.remove = (ImageButton) convertView.findViewById(R.id.play_list_item_remove);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        convertView.setTag(R.id.play_list_item_position, position);
        convertView.setOnClickListener(itemClickListener);

        SongInfo info = (SongInfo) getItem(position);
        holder.name.setText(info.getTitle());
        holder.arts.setText(info.getArtist());

        holder.remove.setTag(R.id.play_list_item_remove_position, position);
        holder.remove.setTag(R.id.play_list_item_remove_path, info.getData());
        holder.remove.setOnClickListener(removeClicklistener);

        Drawable drawable = null;
        int colorN = activity.getResources().getColor(R.color.white_d_d_d);
        int colorA = activity.getResources().getColor(R.color.white_d_d);

        if (position == activity.currentIndex) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawable = activity.getDrawable(R.drawable.ic_volume_up_black_24dp);
            } else
                drawable = activity.getResources().getDrawable(R.drawable.ic_volume_up_black_24dp);
            colorN = colorA = Color.BLACK;
        }

        if (drawable != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    drawable.setTint(activity.getColor(R.color.colorPrimary));
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    drawable.setTint(Color.RED);
                }
            }
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        holder.name.setCompoundDrawables(drawable, null, null, null);

        holder.name.setTextColor(colorN);
        holder.arts.setTextColor(colorA);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.remove.getDrawable().setTint(colorA);
        }

        return convertView;
    }

    public final class ViewHolder {
        TextView name;
        TextView arts;
        ImageButton remove;
    }

}
