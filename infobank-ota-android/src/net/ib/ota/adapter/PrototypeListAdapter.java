package net.ib.ota.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseImageView;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import net.ib.ota.R;
import net.ib.ota.util.ExternalStorage;
import net.ib.ota.util.Log;
import net.ib.ota.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ohjongin on 13. 12. 18.
 */
public class PrototypeListAdapter extends ParseFileListAdapter {
    public PrototypeListAdapter(Context context, int layout_res_id, ArrayList<ParseObject> file_list) {
        super(context, layout_res_id, file_list);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;

        if (convertView == null) {
            convertView = mInflater.inflate(mLayoutResId, null);
            viewHolder = createViewHolder(convertView, position);
            convertView.setTag(viewHolder);
        } else {
            if (convertView.getTag() == null) {
                viewHolder = createViewHolder(convertView, position);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
        }

        ParseObject po = getItem(position);
        if (po == null || viewHolder == null) {
            Log.e("ParseObject is NULL!!");
            return convertView;
        }

        String changelog_xml = po.getString("changeLog");
        ArrayList<String> changeLogList = new ArrayList<String>();
        if (!TextUtils.isEmpty(changelog_xml)) {
            changeLogList = Utils.getChangeLog(changelog_xml);
        }

        viewHolder.tv_subtitle.setText(po.getString("size"));

        if (po.getDate("modified") != null) {
            viewHolder.tv_timestamp.setText(getDateTimeString(po.getDate("modified").getTime()));
        }


        final String package_name = po.getString("package_name");
        final ViewHolder view_holder_final = viewHolder;

        String filename = po.getString("filename").toLowerCase();
        String ext_path = ExternalStorage.getApplicationCacheDir(getBaseContext());
        File file = new File(ext_path, filename);
        viewHolder.tv_title.setText("[#" + po.getInt("buildNumber") + "] " + Utils.getTitle(filename));
        viewHolder.iv_downloaded.setVisibility(file.exists() ? View.VISIBLE : View.INVISIBLE);
        viewHolder.iv_icon.setPlaceholder(getApplicationDrawable(getContext(), package_name, file));

        if (!TextUtils.isEmpty(package_name) && !mIconFileMap.containsKey(package_name)) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("AppInfo");
            query.whereEqualTo("package_name", package_name);
            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(final List<ParseObject> objList, ParseException e) {
                    if (e == null) {
                        if (objList.size() > 0) {
                            mIconFileMap.put(package_name, objList.get(0).getParseFile("file"));
                            view_holder_final.iv_icon.setParseFile(objList.get(0).getParseFile("file"));
                            view_holder_final.iv_icon.loadInBackground(new GetDataCallback() {
                                @Override
                                public void done(byte[] data, ParseException e) {
                                }
                            });
                        }
                    }
                }
            });
        } else if (!mDrawableMap.containsKey(package_name)) {
            view_holder_final.iv_icon.setParseFile(mIconFileMap.get(package_name));
            view_holder_final.iv_icon.loadInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] data, ParseException e) {
                }
            });
        }

        int stable_icon_id = getBaseContext().getResources().getIdentifier("ic_stable_" + po.getString("stable_level"), "drawable", getBaseContext().getPackageName());
        if (stable_icon_id > 0) {
            viewHolder.iv_stable.setImageResource(stable_icon_id);
            viewHolder.iv_stable.setVisibility(View.VISIBLE);
        } else {
            viewHolder.iv_stable.setImageResource(0);
            viewHolder.iv_stable.setVisibility(View.INVISIBLE);
        }

        if (changeLogList.size() > 0) {
            viewHolder.tv_changelog.setText(changeLogList.get(0) + ((changeLogList.size() > 1) ? (" 외 " + (changeLogList.size() - 1) + "건") : ""));
            viewHolder.tv_changelog.setVisibility(View.VISIBLE);
        }


        return convertView;
    }

    protected ViewHolder createViewHolder(View convertView, int position) {
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.tv_title = (TextView) convertView.findViewById(R.id.tv_title);
        viewHolder.tv_changelog = (TextView) convertView.findViewById(R.id.tv_changelog);
        viewHolder.tv_subtitle = (TextView) convertView.findViewById(R.id.tv_subtitle);
        viewHolder.tv_timestamp = (TextView) convertView.findViewById(R.id.tv_timestamp);
        viewHolder.iv_icon = (ParseImageView) convertView.findViewById(R.id.iv_icon);
        viewHolder.iv_stable = (ImageView) convertView.findViewById(R.id.iv_stable);
        viewHolder.iv_downloaded = (ImageView)convertView.findViewById(R.id.iv_downloaded);

        return viewHolder;
    }

    protected static final class ViewHolder {
        public TextView tv_title;
        public TextView tv_changelog;
        public TextView tv_subtitle;
        public TextView tv_timestamp;
        public ParseImageView iv_icon;
        public ImageView iv_stable;
        public ImageView iv_downloaded;
    }
}
