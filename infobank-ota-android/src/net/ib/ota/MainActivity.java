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

package net.ib.ota;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import net.ib.ota.fragment.ParseFileListFragment;
import net.ib.ota.util.CustomToast;
import net.ib.ota.util.ExternalStorage;
import net.ib.ota.util.Log;
import net.ib.ota.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends FragmentActivity implements Const {
    protected final static boolean DEBUG_LOG = false;
    protected static ParseFileListFragment mParseFileListFragment = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            FragmentManager fm = getSupportFragmentManager();
            mParseFileListFragment = (ParseFileListFragment) fm.findFragmentById(R.id.fragment);
            if (mParseFileListFragment == null) {
                mParseFileListFragment = ParseFileListFragment.newInstance("");
                fm.beginTransaction().add(R.id.fragment, mParseFileListFragment).commit();
            }
            return;
        }


        if (!ExternalStorage.isAvailable()) {
            Toast.makeText(this, R.string.no_ext_storage, Toast.LENGTH_LONG).show();
            Log.e("No External Storage!!!");
            finish();
        }

        final ProgressDialog dlg = ProgressDialog.show(this, getString(R.string.login), getString(R.string.login_desc));

        String phoneNumber = Utils.getPhoneNumber(this);
        ParseUser.logInInBackground(phoneNumber, phoneNumber, new LogInCallback() {
            @Override
            public void done(ParseUser user, com.parse.ParseException e) {
                if (user != null) {
                    // Hooray! The user is logged in.
                    // showToast(getString(R.string.login_ok));
                    dlg.dismiss();

                    ParseUser me = ParseUser.getCurrentUser();
                    Calendar calendar = new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault());
                    me.put("lastLogin", calendar.getTime());
                    try {
                        me.save();
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                    }

                    FragmentManager fm = getSupportFragmentManager();
                    ParseFileListFragment old = (ParseFileListFragment)fm.findFragmentById(R.id.fragment);
                    if (old != null) {
                        mParseFileListFragment = old;
                    }
                    else {
                        if (mParseFileListFragment == null) mParseFileListFragment = ParseFileListFragment.newInstance("");
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.add(R.id.fragment, mParseFileListFragment);
                        ft.commit();
                    }
                } else {
                    // Signup failed. Look at the ParseException to see what happened.
                    // showToast(getString(R.string.login_fail, CustomToast.TYPE_WARNING) + "\n" + e.getMessage());
                    dlg.dismiss();

                    AlertDialog.Builder ab = (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) ?
                            new AlertDialog.Builder(MainActivity.this) :
                            new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_HOLO_DARK);

                    ab.setTitle(R.string.login_fail)
                            .setMessage(R.string.request_permission)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dlg, int i) {
                                    dlg.dismiss();

                                    ParseUser.logInInBackground(getString(R.string.guest_id), getString(R.string.guest_id), new LogInCallback() {
                                        @Override
                                        public void done(ParseUser parseUser, ParseException e) {
                                            final String phone = Utils.getPhoneNumber(MainActivity.this);

                                            if (TextUtils.isEmpty(phone)) {
                                                showToast(getString(R.string.phonenumber_unavailable));
                                                showToast(getString(R.string.notice_quick_contact));
                                            } else {
                                                ParseQuery query = new ParseQuery("AccessRequest");
                                                query.whereEqualTo("phoneNumber", phone);
                                                try {
                                                    if (query.count() > 0) {
                                                        showToast(getString(R.string.phonenumber_unavailable));
                                                        showToast(getString(R.string.notice_quick_contact));
                                                    } else {
                                                        ParseObject po = new ParseObject("AccessRequest");
                                                        ParseACL acl = new ParseACL();
                                                        acl.setPublicReadAccess(true);
                                                        po.setACL(acl);
                                                        po.put("phoneNumber", phone);
                                                        po.saveInBackground();
                                                        showToast(getString(R.string.request_done) + "\n" + phone);
                                                    }
                                                } catch (ParseException e1) {
                                                    e1.printStackTrace();
                                                }
                                            }

                                            finish();
                                        }
                                    });
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dlg, int i) {
                                    dlg.cancel();
                                    finish();
                                }
                            }
                            ).create().show();

                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        new Thread(new Runnable() {
            public void run() {
                String ext_path = ExternalStorage.getApplicationCacheDir(getBaseContext());
                File root = new File(ext_path);
                File[] files = root.listFiles();
                if (files != null && files.length > MAX_INTERIM_FILE) {
                    ArrayList<File> file_list = new ArrayList<File>();
                    for(File f : files) {
                        file_list.add(f);
                    }
                    Collections.sort(file_list, fileComparator);

                    for(int i = 0; i < file_list.size(); i++) {
                        File f = file_list.get(i);
                        if (i > MAX_INTERIM_FILE) f.delete();
                        if (DEBUG_LOG) Log.d("[" + i + "] date:" + (new Date(f.lastModified())).toString() + ", name:" + f.getName());
                    }
                }
            }
        }).start();

    }

    /*
        http://stackoverflow.com/questions/7469082/getting-exception-illegalstateexception-can-not-perform-this-action-after-onsa/10261438#10261438
        application working perfectly for API < 11, and Force Closing on API > 11.
        //No call for super(). Bug on API Level > 11.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }

    protected final static Comparator<File> fileComparator = new Comparator<File>() {
        @Override
        public int compare(File obj1, File obj2) {
            return (int)(obj2.lastModified() - obj1.lastModified());

        }
    };

    protected void showToast(String msg) {
        CustomToast.makeText(this, msg, 0,  Toast.LENGTH_LONG).show();
    }

    protected void showToast(String msg, int type) {
        CustomToast.makeText(this, msg, type,  Toast.LENGTH_LONG).show();
    }
}
