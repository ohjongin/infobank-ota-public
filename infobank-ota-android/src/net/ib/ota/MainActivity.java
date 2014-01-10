package net.ib.ota;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.viewpagerindicator.TitlePageIndicator;

import net.ib.ota.adapter.OtaFragmentAdapter;
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

public class MainActivity extends FragmentActivity implements Const, ViewPager.OnPageChangeListener, View.OnClickListener {
    protected final static boolean DEBUG_LOG = true;

    protected ViewPager mPager;
    protected OtaFragmentAdapter mAdapter;
    protected TitlePageIndicator mIndicator;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!ExternalStorage.isAvailable()) {
            Toast.makeText(this, R.string.no_ext_storage, Toast.LENGTH_LONG).show();
            Log.e("No External Storage!!!");
            finish();
            return;
        }

        if (Utils.isDebuggable(getBaseContext())) {
            Toast.makeText(this, "CAUTION!!!\nRUNNING ON DEBUG MODE !!!", Toast.LENGTH_LONG).show();
            Log.e("=====================================================");
            Log.e("RUNNING ON DEBUG MODE!!! android:debuggable=\"true\"");
            Log.e("=====================================================");
        }

        if (ParseUser.getCurrentUser() == null
                || (ParseUser.getCurrentUser() != null && !ParseUser.getCurrentUser().isAuthenticated())) {
            loginParse();
        } else {
            showToast(getString(R.string.login_ok));
            initViews();
            if (DEBUG_LOG) Log.d("Parse Session Available ==>" + ParseUser.getCurrentUser().getUsername() + ", Auth: " + ParseUser.getCurrentUser().isAuthenticated());
        }
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

    @Override
    public void onClick(View view) {

    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int i) {

    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    protected void initViews() {
        mAdapter = new OtaFragmentAdapter(getSupportFragmentManager());

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(0);

        // Bind the title indicator to the adapter
        mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);

        // continued from above
        mIndicator.setOnPageChangeListener(this);

        findViewById(R.id.layout_empty).setVisibility(View.GONE);
        findViewById(R.id.layout_main).setVisibility(View.VISIBLE);
    }

    @SuppressLint("NewApi")
    protected void onLoginFailed() {
        AlertDialog.Builder ab = (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) ?
                new AlertDialog.Builder(MainActivity.this) :
                new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_HOLO_DARK);

        ab.setTitle("IB OTA 로그인 실패")
                .setMessage("IB OTA 접근 권한을 요청하시겠습니까?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dlg, int i) {
                        dlg.dismiss();

                        ParseUser.logInInBackground("quest", "quest", new LogInCallback() {
                            @Override
                            public void done(ParseUser parseUser, ParseException e) {
                                final String phone = Utils.getPhoneNumber(MainActivity.this);

                                if (TextUtils.isEmpty(phone)) {
                                    showToast("전화번호 정보를 확인할 수 없어서 자동 처리를 할 수 없습니다.");
                                    showToast("빠른 처리를 원하시면 android@infobank.net 으로 연락주시기 바랍니다.");
                                } else {
                                    ParseQuery query = new ParseQuery("AccessRequest");
                                    query.whereEqualTo("phoneNumber", phone);
                                    try {
                                        if (query.count() > 0) {
                                            showToast("접근 권한이 이미 요청된 상태입니다.");
                                            showToast("빠른 처리를 원하시면 android@infobank.net 으로 연락주시기 바랍니다.");
                                        } else {
                                            ParseObject po = new ParseObject("AccessRequest");
                                            ParseACL acl = new ParseACL();
                                            acl.setPublicReadAccess(true);
                                            po.setACL(acl);
                                            po.put("phoneNumber", phone);
                                            po.saveInBackground();
                                            showToast("접근 권한을 요청하였습니다.\n" + phone);
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

    protected void loginParse() {
        final ProgressDialog dlgLogin = ProgressDialog.show(this, getString(R.string.login), getString(R.string.login_desc));
        final TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

        String phoneNumber = Utils.getPhoneNumber(this);
        ParseUser.logInInBackground(phoneNumber, phoneNumber, new LogInCallback() {
            @Override
            public void done(ParseUser user, com.parse.ParseException e) {
                if (user != null) {
                    showToast(getString(R.string.login_ok));
                    initViews();

                    // Hooray! The user is logged in.
                    dlgLogin.dismiss();

                    ParseUser me = ParseUser.getCurrentUser();
                    Calendar calendar = new GregorianCalendar(TimeZone.getDefault(), Locale.getDefault());
                    me.put("lastLogin", calendar.getTime());
                    me.put("IMEI", tm.getDeviceId());
                    try {
                        me.save();
                    } catch (ParseException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    // Signup failed. Look at the ParseException to see what happened.
                    // showToast(getString(R.string.login_fail, CustomToast.TYPE_WARNING) + "\n" + e.getMessage());
                    dlgLogin.dismiss();

                    onLoginFailed();
                }
            }
        });
    }
}
