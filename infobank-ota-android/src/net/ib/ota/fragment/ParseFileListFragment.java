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

package net.ib.ota.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.ProgressCallback;

import net.ib.ota.Const;
import net.ib.ota.R;
import net.ib.ota.adapter.ParseFileListAdapter;
import net.ib.ota.util.CustomToast;
import net.ib.ota.util.ExternalStorage;
import net.ib.ota.util.Log;
import net.ib.ota.util.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ohjongin on 13. 7. 8.
 */
public class ParseFileListFragment extends ListFragment implements Const {
    protected ParseFileListAdapter mListAdapter;
    protected ArrayList<ParseObject> mFileList = new ArrayList<ParseObject>();
    protected static boolean mUpdateChecked = false;
    protected boolean mCanceled = false;

    public static ParseFileListFragment newInstance(CharSequence label) {
        ParseFileListFragment f = new ParseFileListFragment();
        Bundle b = new Bundle();
        b.putCharSequence("label", label);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apk_list, null, false);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListAdapter = new ParseFileListAdapter(getActivity(), getListView(), mFileList);
        setListAdapter(mListAdapter);

        onRefresh();

        setHasOptionsMenu(true);
        //registerForContextMenu(this.getListView());
    }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        super.onResume();
        if (mListAdapter != null) mListAdapter.notifyDataSetChanged();
        if (ParseUser.getCurrentUser() != null && ParseUser.getCurrentUser().isAuthenticated() && !mUpdateChecked) {
            mUpdateChecked = true;
            ParseObject obj = getLastestVersion();
            if (obj == null) return;
            showToast("새로운 업데이트가 있습니다.", CustomToast.TYPE_INFO);

            final boolean is_mandatory = obj.getBoolean("mandatory");
            AlertDialog.Builder ab = (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) ?
                    new AlertDialog.Builder(getActivity()) :
                    new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_DARK);

            ab.setTitle(R.string.action_self_update)
                    .setMessage(obj.getString("description"))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dlg, int i) {
                            dlg.dismiss();
                            onSelfUpdate();
                        }
                    })
                    .setCancelable(!is_mandatory);;
            if (!is_mandatory) {
                ab.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dlg, int i) {
                        dlg.cancel();
                    }
                });
            }

            AlertDialog dlg = ab.create();
            dlg.show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @SuppressLint("NewApi")
    @Override
    public void onListItemClick(ListView l, View v, final int position, long id) {
        super.onListItemClick(l, v, position, id);

        final ParseObject po = (ParseObject) mListAdapter.getItem(position);

        AlertDialog.Builder ab = (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) ?
                new AlertDialog.Builder(getActivity()) :
                new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_DARK);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View layout = inflater.inflate(R.layout.dialog_install_body, null);
        LinearLayout ll = (LinearLayout)layout.findViewById(R.id.body);

        String changelog_xml = po.getString("changeLog");
        if (!TextUtils.isEmpty(changelog_xml)) {
            ArrayList<String> changeLogList = Utils.getChangeLog(changelog_xml);
            for(int i = 0; i < changeLogList.size(); i++) {
                TextView tv = new TextView(getActivity());
                tv.setText(changeLogList.get(i));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                ll.addView(tv);

                if (i < changeLogList.size() - 1) {
                    View line = new View(getActivity());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
                    lp.setMargins(3, 10, 3, 10);
                    line.setLayoutParams(lp);
                    //line.setBackgroundColor(0x5533b5e5);
                    line.setBackgroundColor(Color.DKGRAY);
                    ll.addView(line);
                }
            }
        } else {
            TextView tv = new TextView(getActivity());
            tv.setText(getString(R.string.no_changelog));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            ll.addView(tv);
        }

        AlertDialog dlg = ab.setTitle(R.string.ask_install)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dlg, int i) {
                        dlg.dismiss();
                        onInstall(position);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dlg, int i) {
                        dlg.cancel();
                    }
                })
                .setView(layout)
                .create();

        dlg.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = this.getActivity().getMenuInflater();
        inflater.inflate(R.menu.fragment_apk_list, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.action_install:
                //onActionInstall(info.position);
                return true;

            case R.id.action_upload:
               //onActionUpload(info.position);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                mFileList.clear();
                mListAdapter.notifyDataSetChanged();
                onRefresh();
                break;
            case R.id.action_self_update:
                onSelfUpdate();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onInstall(int position) {
        final ProgressDialog dlg = new ProgressDialog(getActivity());
        final ParseObject po = (ParseObject) mListAdapter.getItem(position);
        final File file = new File(ExternalStorage.getApplicationCacheDir(getActivity()), po.getString("filename"));

        final int size = (po.getInt("bytes") > 0) ? po.getInt("bytes") : 100;

        if (file.exists()) {
            showToast(getString(R.string.install_selected));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            final ParseFile parseFile = po.getParseFile("file");
            mCanceled = false;
            dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dlg.setMessage(getActivity().getString(R.string.downloading));
            dlg.setCancelable(false);
            dlg.setMax(size);
            dlg.setButton(getActivity().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    showToast(getString(R.string.download_canceled), Toast.LENGTH_SHORT);
                    mCanceled = true;
                    parseFile.cancel();
                }
            });

            dlg.show();

            parseFile.getDataInBackground(
                    new GetDataCallback() {
                        @Override
                        public void done(byte[] bytes, ParseException e) {
                            BufferedOutputStream bos;

                            try {
                                bos = new BufferedOutputStream(new FileOutputStream(file));
                                bos.write(bytes);
                                bos.flush();
                                bos.close();
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            }
                        }
                    },
                    new ProgressCallback() {
                        public void done(Integer percentDone) {
                            // Update your progress spinner here. percentDone will be between 0 and 100.
                            dlg.setProgress(size * percentDone / 100);
                            if (percentDone >= 100) {
                                dlg.dismiss();

                                if (!mCanceled) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                                mCanceled = false;
                            }
                        }
                    }
            );
        }
    }

    protected void onRefresh() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FileList");
        query.whereExists("file");
        query.orderByDescending("timestamp");
        query.addDescendingOrder("createdAt");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(final List<ParseObject> objList, ParseException e) {
                if (e == null) {
                    Log.d("Retrieved " + objList.size() + " files");

                    mFileList.clear();
                    mFileList.addAll(objList);
                    if (mFileList.size() < 1) {
                        getView().findViewById(android.R.id.empty).setVisibility(View.INVISIBLE);
                        showToast(getString(R.string.no_file), CustomToast.TYPE_WARNING);
                    }
                    mListAdapter.notifyDataSetChanged();
                } else {
                    showToast("오류\n" + e.getMessage(), CustomToast.TYPE_WARNING);
                    Log.d("Error: " + e.getMessage());
                }
            }
        });
    }

    protected ParseObject getLastestVersion() {
        ParseObject lastestVersion = null;
        PackageInfo pInfo = null;

        try {
            pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        int versionCode = pInfo.versionCode;

        ParseQuery<ParseObject> query = new ParseQuery("OtaClient");
        query.whereGreaterThan("versionCode", versionCode);
        query.orderByDescending("versionCode");
        try {
            List<ParseObject> objList = query.find();
            if (objList.size() > 0) {
                lastestVersion = objList.get(0);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return lastestVersion;
    }

    protected void onSelfUpdate() {
        final ProgressDialog dlg = new ProgressDialog(getActivity());

        PackageInfo pInfo = null;
        try {
            pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        int versionCode = pInfo.versionCode;

        ParseQuery<ParseObject> query = new ParseQuery("OtaClient");
        query.whereGreaterThan("versionCode", versionCode);
        query.orderByDescending("versionCode");
        try {
            if (query.count() < 1) {
                showToast("현재 버전(" + pInfo.versionName + ")이 최신입니다.", CustomToast.TYPE_INFO);
                return;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objList, ParseException e) {
                if (objList.size() < 1) {
                    Log.e("Query result is Empty!!!");
                    return;
                }

                ParseObject obj = objList.get(0);

                final File file = new File(ExternalStorage.getApplicationCacheDir(getActivity()), obj.getString("versionName"));
                final int size = (obj.getInt("bytes") > 0) ? obj.getInt("bytes") : 100;

                if (file.exists()) {
                    showToast(getString(R.string.install_selected));
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    mCanceled = false;
                    final ParseFile parseFile = obj.getParseFile("file");
                    dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    dlg.setMessage(getActivity().getString(R.string.downloading));
                    dlg.setCancelable(false);
                    dlg.setMax(size);
                    dlg.setButton(getActivity().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            showToast(getString(R.string.upload_canceled), Toast.LENGTH_SHORT);
                            mCanceled = true;
                            parseFile.cancel();
                        }
                    });

                    dlg.show();

                    parseFile.getDataInBackground(
                            new GetDataCallback() {
                                @Override
                                public void done(byte[] bytes, ParseException e) {
                                    BufferedOutputStream bos;

                                    try {
                                        bos = new BufferedOutputStream(new FileOutputStream(file));
                                        bos.write(bytes);
                                        bos.flush();
                                        bos.close();
                                    } catch (IOException ioe) {
                                        ioe.printStackTrace();
                                    }
                                }
                            },
                            new ProgressCallback() {
                                public void done(Integer percentDone) {
                                    // Update your progress spinner here. percentDone will be between 0 and 100.
                                    dlg.setProgress(size * percentDone / 100);
                                    if (percentDone >= 100) {
                                        dlg.dismiss();

                                        if (!mCanceled) {
                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                                            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(intent);
                                        }
                                        mCanceled = false;
                                    }
                                }
                            }
                    );
                }
            }
        });
    }

    protected void showToast(String msg) {
        CustomToast.makeText(getActivity(), msg, 0, Toast.LENGTH_LONG).show();
    }

    protected void showToast(String msg, int type) {
        CustomToast.makeText(getActivity(), msg, type,  Toast.LENGTH_LONG).show();
    }
}
