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

package net.ib.ota.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.ib.ota.R;

public class CustomToast {
    public static final int TYPE_INFO = -1;
    public static final int TYPE_WARNING = -2;
    
    public static View getToastView(Context context, String msg, int icon_id) {
        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View toast_view = li.inflate(R.layout.custom_toast, null);
        ImageView iv_icon = (ImageView)toast_view.findViewById(R.id.iv_icon);
        TextView tv_msg = (TextView)toast_view.findViewById(R.id.tv_msg);
        iv_icon.setImageResource(icon_id);
        tv_msg.setText(msg);
        
        return toast_view;
    }

    public static View getToastView(Context context, String msg) {
        return getToastView(context, msg, R.drawable.ic_launcher);
    }
    
    public static Toast makeText(Context context, String msg, int icon_id, int duration) {
        switch (icon_id) {
            case TYPE_INFO:
                icon_id = R.drawable.ic_launcher;
                break;
            case TYPE_WARNING:
                icon_id = R.drawable.ic_launcher;
                break;
            default:
                icon_id = R.drawable.ic_launcher;
                break;
        }

        View view = getToastView(context, msg, icon_id);
        
        Toast toast = new Toast(context);              
        toast.setDuration(duration);
        toast.setView(view);
        return toast;
    }    
    
    public static Toast makeText(Context context, int msg_id, int duration) {
        return makeText(context, context.getString(msg_id), R.drawable.ic_launcher, duration);
    }  
}
