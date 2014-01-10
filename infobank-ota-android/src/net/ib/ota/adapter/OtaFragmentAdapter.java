package net.ib.ota.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.viewpagerindicator.IconPagerAdapter;

import net.ib.ota.Const;
import net.ib.ota.R;
import net.ib.ota.fragment.MessagetongListFragment;
import net.ib.ota.fragment.PrototypeListFragment;
import net.ib.ota.util.Log;

/**
 * Created by ohjongin on 13. 12. 11.
 */
public class OtaFragmentAdapter extends FragmentPagerAdapter implements Const, IconPagerAdapter {
    protected static final String[] mTitle = new String[] { "메시지통", "프로토타입" };
    protected static final String[] mCategory = new String[] { "messagetong", "prototype" };
    protected Fragment mFragments[] = new Fragment[mTitle.length];

    public OtaFragmentAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitle[position % mTitle.length];
    }

    @Override
    public int getIconResId(int index) {
        return R.drawable.ic_launcher;
    }

    @Override
    public int getCount() {
        return mTitle.length;
    }

    @Override
    public Fragment getItem(int position) {
        if (position < 0 || position >= mTitle.length) {
            Log.e("Invalid index(" + position + ")!!!");
            return null;
        }

        Fragment fragment = mFragments[position];
        if (fragment == null) {
            switch (position) {
                case 0:
                    fragment = MessagetongListFragment.newInstance(mTitle[position], mCategory[position]);
                    break;
                case 1:
                    fragment = PrototypeListFragment.newInstance(mTitle[position], mCategory[position]);
                    break;
                default:
                    break;
            }
            mFragments[position] = fragment;
        }

        return fragment;
    }

    public Fragment getFragment(int position) {
        return (position >= 0 && position < mTitle.length) ? mFragments[position] : null;
    }
}
