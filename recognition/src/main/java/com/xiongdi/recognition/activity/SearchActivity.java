package com.xiongdi.recognition.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.adapter.SearchResultAdapter;
import com.xiongdi.recognition.application.MainApplication;
import com.xiongdi.recognition.bean.Person;
import com.xiongdi.recognition.db.PersonDao;
import com.xiongdi.recognition.widget.searchView.SearchAdapter;
import com.xiongdi.recognition.widget.searchView.SearchHistoryTable;
import com.xiongdi.recognition.widget.searchView.SearchItem;
import com.xiongdi.recognition.widget.searchView.SearchView;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by moubiao on 2016/6/12.
 * 搜索界面
 */
public class SearchActivity extends AppCompatActivity {
    private static final int SEARCH_CODE = 100001;

    private SearchView mSearchView;
    private SearchHistoryTable mHistoryDatabase;
    private ListView searchResultLV;

    private SearchResultAdapter mResultAdapter;
    private List<Person> mPersonList;

    private PersonDao mPersonDao;
    private SearchHandler mSearchHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_layout);

        initData();
        initView();
        setInnerListener();
    }

    private void initData() {
        mPersonDao = new PersonDao(getApplicationContext());
        mSearchHandler = new SearchHandler(this);
    }

    private void initView() {
        mSearchView = (SearchView) findViewById(R.id.search_view);
        setSearchView();

        searchResultLV = (ListView) findViewById(R.id.search_result_lv);
        View headView = LayoutInflater.from(this).inflate(R.layout.search_result_item, searchResultLV, false);
        searchResultLV.addHeaderView(headView, null, false);
        View emptyView = findViewById(R.id.empty_view);
        searchResultLV.setEmptyView(emptyView);
        mPersonList = new ArrayList<>();
        mResultAdapter = new SearchResultAdapter(this, mPersonList);
        searchResultLV.setAdapter(mResultAdapter);
    }

    private void setSearchView() {
        mHistoryDatabase = new SearchHistoryTable(this);
        mSearchView.setTheme(SearchView.THEME_LIGHT, true);
        mSearchView.setAnimationDuration(SearchView.ANIMATION_DURATION);
        mSearchView.setShadowColor(ContextCompat.getColor(this, R.color.search_shadow_layout));
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mHistoryDatabase.addItem(new SearchItem(query));
                mSearchView.close(false);
                new SearchThread(query).start();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        mSearchView.setOnOpenCloseListener(new SearchView.OnOpenCloseListener() {
            @Override
            public void onOpen() {
            }

            @Override
            public void onClose() {
            }
        });

        List<SearchItem> suggestionsList = new ArrayList<>();
        suggestionsList.add(new SearchItem("00001"));
        suggestionsList.add(new SearchItem("00002"));
        suggestionsList.add(new SearchItem("00003"));

        SearchAdapter searchAdapter = new SearchAdapter(this, suggestionsList);
        searchAdapter.setOnItemClickListener(new SearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                mSearchView.close(false);
                TextView textView = (TextView) view.findViewById(R.id.textView_item_text);
                String query = textView.getText().toString();
                new SearchThread(query).start();
            }
        });
        mSearchView.setAdapter(searchAdapter);
    }

    private void setInnerListener() {
        searchResultLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainApplication mainApplication = (MainApplication) getApplication();
                mainApplication.setPerson(mPersonList.get(position - 1));
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

    private class SearchThread extends Thread {
        private String queryText;

        public SearchThread(String queryText) {
            this.queryText = queryText;
        }

        @Override
        public void run() {
            List<Person> result;
            QueryBuilder<Person, Integer> queryBuilder = mPersonDao.getQueryBuilder();
            Where<Person, Integer> where = queryBuilder.where();
            try {
                where.eq("ID_NO", queryText);
                PreparedQuery<Person> preparedQuery = where.prepare();
                result = mPersonDao.query(preparedQuery);
                mPersonList.clear();
                mPersonList.addAll(result);
                Message message = Message.obtain();
                message.what = SEARCH_CODE;
                mSearchHandler.sendMessage(message);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static class SearchHandler extends Handler {
        private WeakReference<SearchActivity> mWeakReference;

        public SearchHandler(SearchActivity activity) {
            mWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final SearchActivity activity = mWeakReference.get();
            switch (msg.what) {
                case SEARCH_CODE:
                    activity.mResultAdapter.notifyDataSetChanged();
                    break;
                default:
                    break;
            }
        }
    }
}
