package com.xiongdi.recognition.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.j256.ormlite.stmt.QueryBuilder;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.bean.Person;
import com.xiongdi.recognition.db.PersonDao;

import java.sql.SQLException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by moubiao on 2016/8/23.
 * 统计结果界面
 */
public class CountActivity extends AppCompatActivity {
    private final String TAG = "moubiao";

    private TextView mVotedTV, mNotVoteTV, mMenTV, mWomenTV;

    private PersonDao mPersonDao;
    private long mTotalCount;
    private long mCheckedCount, mNotCheckedCount;
    private long mMenCount, mWomenCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.count_layout);

        initData();
        initView();
        count();
    }

    private void initData() {
        mPersonDao = new PersonDao(this);
    }

    private void initView() {
        mVotedTV = (TextView) findViewById(R.id.voted_tv);
        mNotVoteTV = (TextView) findViewById(R.id.not_vote_tv);
        mMenTV = (TextView) findViewById(R.id.men_tv);
        mWomenTV = (TextView) findViewById(R.id.women_tv);
    }

    private void count() {
        Observable.create(new Observable.OnSubscribe<Long>() {
            @Override
            public void call(Subscriber<? super Long> subscriber) {
                try {
                    QueryBuilder<Person, Integer> builder = mPersonDao.getQueryBuilder();
                    //总人数
                    mTotalCount = mPersonDao.getQuantity();
                    //投票人数
                    builder.where().eq("mChecked", 0);
                    mCheckedCount = builder.where().eq("mChecked", 1).countOf();
                    mNotCheckedCount = mTotalCount - mCheckedCount;
                    //男女人数
                    mMenCount = builder.where().reset().eq("mGender", "male").countOf();
                    mWomenCount = mTotalCount - mMenCount;

                    subscriber.onCompleted();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {
                        mVotedTV.setText(String.format(getString(R.string.voted_number), mCheckedCount));
                        mNotVoteTV.setText(String.format(getString(R.string.not_vote_number), mNotCheckedCount));
                        mMenTV.setText(String.format(getString(R.string.man_quantity), mMenCount));
                        mWomenTV.setText(String.format(getString(R.string.women_quantity), mWomenCount));
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Long o) {
                    }
                });
    }
}
