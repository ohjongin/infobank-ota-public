package net.ib.ota.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import com.parse.ParseFile;
import com.parse.ParseObject;

import net.ib.ota.Const;
import net.ib.ota.R;
import net.ib.ota.util.ExternalStorage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by ohjongin on 13. 6. 16.
 */
public class ParseFileListAdapter extends ArrayAdapter<ParseObject> implements Const {
    protected LayoutInflater mInflater;
    protected int mLayoutResId = -1;
    protected static HashMap<String, ParseFile> mIconFileMap = new HashMap<String, ParseFile>();
    protected static HashMap<String, Drawable> mDrawableMap = new HashMap<String, Drawable>();

    public ParseFileListAdapter(Context context, int layout_res_id, ArrayList<ParseObject> file_list) {
        super(context, layout_res_id, file_list);
        mInflater = LayoutInflater.from(context);
        mLayoutResId = layout_res_id;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    protected SimpleDateFormat getSimpleDateFormat() {
        return new SimpleDateFormat("M/d H:mm:ss");
    }

    protected String getDateTimeString(long time_milis) {
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

    protected Drawable getApplicationDrawable(Context context, String package_name, File file_cached) {
        Drawable result = context.getResources().getDrawable(R.drawable.ic_default_app);
        if (mDrawableMap.containsKey(package_name)) {
            result = mDrawableMap.get(package_name);
        } else if (file_cached != null && file_cached.exists()) {
            result = ExternalStorage.getApplicationIcon(getContext(), file_cached.getAbsolutePath());
            mDrawableMap.put(package_name, result);
        }

        return result;
    }

    protected Context getBaseContext() {
        return getContext();
    }
}
