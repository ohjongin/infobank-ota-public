/*
 * Copyright (C) 2013 Infobank corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.ib.ota.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.ParseObject;

import net.ib.ota.Const;
import net.ib.ota.R;
import net.ib.ota.util.ExternalStorage;
import net.ib.ota.util.Log;
import net.ib.ota.util.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by ohjongin on 13. 6. 16.
 */
public class ParseFileListAdapter extends BaseAdapter implements Const {
    protected Context mContext;
    protected ArrayList<ParseObject> mApkList = new ArrayList<ParseObject>();
    protected LayoutInflater mInflater;
    protected ListView mListView;

    public ParseFileListAdapter(Context context, ListView listView, ArrayList<ParseObject> file_list) {
        mContext = context;
        mApkList = file_list;
        mInflater = LayoutInflater.from(context);
        mListView = listView;
    }

    @Override
    public int getCount() {
        return mApkList.size();
    }

    @Override
    public Object getItem(int position) {
        return mApkList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.fragment_apk_list_row, null);
            viewHolder = createViewHolder(convertView, position);
            convertView.setTag(viewHolder);
        } else {
            if (convertView.getTag() == null) {
                viewHolder = createViewHolder(convertView, position);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
        }

        if (mApkList == null)
            return convertView;

        ParseObject po = mApkList.get(position);
        if (po == null || viewHolder == null) {
            Log.e("ParseObject is NULL!!");
            return convertView;
        }

        String changelog_xml = po.getString("changeLog");
        ArrayList<String> changeLogList = new ArrayList<String>();
        if (!TextUtils.isEmpty(changelog_xml)) {
            changeLogList = Utils.getChangeLog(changelog_xml);
            /*if (changeLogList.size() > 0) {
                Log.e("" + changeLogList.get(0));
            }*/
        }

        viewHolder.tv_subtitle.setText(po.getString("size"));

        if (po.getDate("modified") != null) {
            viewHolder.tv_timestamp.setText(getDateTimeString(po.getDate("modified").getTime()));
        }

        String filename = po.getString("filename").toLowerCase();
        viewHolder.tv_title.setText("[#" + po.getInt("buildNumber") + "] " + Utils.getTitle(filename));

        int resId = Utils.getImageResource(filename, po.getCreatedAt());
        viewHolder.iv_icon.setImageResource(resId);

        String ext_path = ExternalStorage.getApplicationCacheDir(getBaseContext());
        File file = new File(ext_path, filename);
        viewHolder.iv_downloaded.setVisibility(file.exists() ? View.VISIBLE : View.INVISIBLE);

        if ((resId == R.drawable.ic_default_app) && file.exists()) {
            viewHolder.iv_icon.setImageDrawable(ExternalStorage.getApplicationIcon(mContext, file.getAbsolutePath()));
        }

        if (changeLogList.size() > 0) {
            viewHolder.tv_changelog.setText(changeLogList.get(0) + ((changeLogList.size() > 1) ? (" 외 " + (changeLogList.size() - 1) + "건") : ""));
            /*String str = changeLogList.get(0);
            for(int i = 1; i < changeLogList.size(); i++) {
                str += "\n";
                str += changeLogList.get(i);
            }
            viewHolder.tv_changelog.setText(str);*/
            viewHolder.tv_changelog.setVisibility(View.VISIBLE);
        }


        return convertView;
    }

    private ViewHolder createViewHolder(View convertView, int position) {
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.tv_title = (TextView) convertView.findViewById(R.id.tv_title);
        viewHolder.tv_changelog = (TextView) convertView.findViewById(R.id.tv_changelog);
        viewHolder.tv_subtitle = (TextView) convertView.findViewById(R.id.tv_subtitle);
        viewHolder.tv_timestamp = (TextView) convertView.findViewById(R.id.tv_timestamp);
        viewHolder.iv_icon = (ImageView) convertView.findViewById(R.id.iv_icon);
        viewHolder.iv_downloaded = (ImageView)convertView.findViewById(R.id.iv_downloaded);

        return viewHolder;
    }

    private static final class ViewHolder {
        public TextView tv_title;
        public TextView tv_changelog;
        public TextView tv_subtitle;
        public TextView tv_timestamp;
        public ImageView iv_icon;
        public ImageView iv_downloaded;
    }

    public SimpleDateFormat getSimpleDateFormat() {
        return new SimpleDateFormat("M/d H:mm:ss");
    }

    public String getDateTimeString(long time_milis) {
        SimpleDateFormat sdf = getSimpleDateFormat();
        String datetime = sdf.format(new Date(time_milis));

        String duration_str = "";
        long duration = System.currentTimeMillis() - time_milis;
        if (duration > 0) {
            if (duration < DateUtils.MINUTE_IN_MILLIS) {
                duration_str = String.format(getBaseContext().getString(R.string.datetime_in_seconds), duration / DateUtils.SECOND_IN_MILLIS);
            } else if (duration < DateUtils.HOUR_IN_MILLIS) {
                duration_str = String.format(getBaseContext().getString(R.string.datetime_in_minutes), duration / DateUtils.MINUTE_IN_MILLIS);
            } else if (duration < DateUtils.HOUR_IN_MILLIS * 24) {
                duration_str = String.format(getBaseContext().getString(R.string.datetime_in_hours), duration / DateUtils.HOUR_IN_MILLIS);
            }
        }

        if (duration_str.length() > 0) {
            datetime = duration_str + ", " + datetime;
        }
        return datetime;
    }

    protected Context getBaseContext() {
        return mContext;
    }
}
