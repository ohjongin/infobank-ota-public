package net.ib.ota.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.parse.ParseObject;

import net.ib.ota.R;
import net.ib.ota.adapter.MessagetongListAdapter;

import java.util.ArrayList;

/**
 * Created by ohjongin on 13. 12. 18.
 */
public class MessagetongListFragment extends ParseFileListFragment {
    public static MessagetongListFragment newInstance(CharSequence label, CharSequence category) {
        MessagetongListFragment f = new MessagetongListFragment();
        Bundle b = new Bundle();
        b.putCharSequence("label", label);
        b.putCharSequence("category", category);
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
        mListAdapter = (MessagetongListAdapter)getListAdapter();
        if (mListAdapter == null) {
            mListAdapter = new MessagetongListAdapter(getActivity(), R.layout.fragment_apk_list_row, new ArrayList<ParseObject>());
            setListAdapter(mListAdapter);
        }

        onRefresh();

        setHasOptionsMenu(true);
        registerForContextMenu(this.getListView());
    }
}
