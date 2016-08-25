package com.xiongdi.recognition.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.j256.ormlite.stmt.QueryBuilder;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.bean.Person;
import com.xiongdi.recognition.db.PersonDao;
import com.xiongdi.recognition.util.DisplayUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    private TextView mVoteTotalTV, mGenderTotalTV, mVotedTV, mNotVoteTV, mMenTV, mWomenTV;
    private PieChart mProportionChart, mGenderChart;

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
        mProportionChart = (PieChart) findViewById(R.id.vote_proportion_chart);
        if (mProportionChart != null) {
            mProportionChart.setCenterTextSize(DisplayUtil.sp2px(getApplicationContext(), 24f));
            mProportionChart.setCenterText(getString(R.string.count_proportion_type));
            mProportionChart.setDescription(null);
        }
        mGenderChart = (PieChart) findViewById(R.id.gender_proportion_chart);
        if (mGenderChart != null) {
            mGenderChart.setCenterTextSize(DisplayUtil.sp2px(getApplicationContext(), 24f));
            mGenderChart.setCenterText(getString(R.string.count_gender_type));
            mGenderChart.setDescription(null);
        }

        mVoteTotalTV = (TextView) findViewById(R.id.vote_total_tv);
        mGenderTotalTV = (TextView) findViewById(R.id.gender_total_tv);
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
                        List<Float> votePro = new ArrayList<>();
                        votePro.add(((mCheckedCount / (float) mTotalCount) * 100));
                        votePro.add(((mNotCheckedCount / (float) mTotalCount) * 100));
                        List<String> source = new ArrayList<>();
                        source.add(getString(R.string.voted_person));
                        source.add(getString(R.string.not_voted_person));
                        PieData voteData = setPieChartData(votePro, source);
                        mProportionChart.setData(voteData);
                        mVoteTotalTV.setText(String.format(getString(R.string.total_voter_quantity), mTotalCount));
                        mVotedTV.setText(String.format(getString(R.string.voted_number), mCheckedCount));
                        mNotVoteTV.setText(String.format(getString(R.string.not_vote_number), mNotCheckedCount));

                        List<Float> genderPro = new ArrayList<>();
                        genderPro.add(((mMenCount / (float) mTotalCount) * 100));
                        genderPro.add(((mWomenCount / (float) mTotalCount) * 100));
                        List<String> genderSource = new ArrayList<>();
                        genderSource.add(getString(R.string.voted_person));
                        genderSource.add(getString(R.string.not_voted_person));
                        PieData genderData = setPieChartData(genderPro, genderSource);
                        mGenderChart.setData(genderData);
                        mGenderTotalTV.setText(String.format(getString(R.string.total_voter_quantity), mTotalCount));
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

    /**
     * 设置饼状图的数据及样式
     */
    private PieData setPieChartData(List<Float> proportion, List<String> name) {
        List<PieEntry> yValue = new ArrayList<>();
        for (int i = 0; i < proportion.size(); i++) {
            yValue.add(new PieEntry(proportion.get(i), name.get(i)));
        }
        PieDataSet pieDataSet = new PieDataSet(yValue, null);
        pieDataSet.setSliceSpace(3f);
        pieDataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.VORDIPLOM_COLORS) {
            colors.add(c);
        }
        for (int c : ColorTemplate.JOYFUL_COLORS) {
            colors.add(c);
        }
        for (int c : ColorTemplate.COLORFUL_COLORS) {
            colors.add(c);
        }
        for (int c : ColorTemplate.LIBERTY_COLORS) {
            colors.add(c);
        }
        for (int c : ColorTemplate.PASTEL_COLORS) {
            colors.add(c);
        }

        colors.add(ColorTemplate.getHoloBlue());
        pieDataSet.setColors(colors);

        pieDataSet.setValueLinePart1OffsetPercentage(80.f);
        pieDataSet.setValueLinePart1Length(0.2f);
        pieDataSet.setValueLinePart2Length(0.4f);
        pieDataSet.setYValuePosition(PieDataSet.ValuePosition.INSIDE_SLICE);

        PieData pieData = new PieData(pieDataSet);
        pieData.setValueFormatter(new PercentFormatter());
        pieData.setValueTextSize(11f);
        pieData.setValueTextColor(Color.BLACK);

        return pieData;
    }
}
