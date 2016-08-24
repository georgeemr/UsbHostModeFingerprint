package com.xiongdi.recognition.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.xiongdi.recognition.R;

/**
 * Created by moubiao on 2016/8/23.
 * 统计结果界面
 */
public class CountActivity extends AppCompatActivity {
    private final String TAG = "moubiao";

    private TextView mVotedTV, mNotVoteTV, mMenTV, mWomenTV;
    private int votedQuantity, notVoteQuantity, menQuantity, womenQuantity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.count_layout);

        initData();
        initView();
    }

    private void initData() {

    }

    private void initView() {
        mVotedTV = (TextView) findViewById(R.id.voted_tv);
        mNotVoteTV = (TextView) findViewById(R.id.not_vote_tv);
        mMenTV = (TextView) findViewById(R.id.men_tv);
        mWomenTV = (TextView) findViewById(R.id.women_tv);
    }
}
