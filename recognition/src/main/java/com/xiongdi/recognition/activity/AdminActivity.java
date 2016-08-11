package com.xiongdi.recognition.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.xiongdi.recognition.R;
import com.xiongdi.recognition.widget.numberProgressBar.NumberProgressBar;
import com.xiongdi.recognition.widget.numberProgressBar.OnProgressBarListener;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by moubiao on 2016/8/10.
 * 管理员界面
 */
public class AdminActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "moubiao";

    private TextView mDownloadTV, mUploadTV, mCountResultTV, mPrintResultTV;
    private NumberProgressBar mProgressBar;
    private View mMaskView;
    private Button mCancelBT, mSureBT;

    private Timer mTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_layout);

        initData();
        initView();
        setInnerListener();
    }

    private void initData() {
    }

    private void initView() {
        mDownloadTV = (TextView) findViewById(R.id.download_tv);
        mUploadTV = (TextView) findViewById(R.id.upload_tv);
        mCountResultTV = (TextView) findViewById(R.id.count_result_tv);
        mPrintResultTV = (TextView) findViewById(R.id.print_result_tv);
        mProgressBar = (NumberProgressBar) findViewById(R.id.connect_data_progress_bar);
        mMaskView = findViewById(R.id.mask_view);
        mCancelBT = (Button) findViewById(R.id.cancel_station_bt);
        mSureBT = (Button) findViewById(R.id.sure_station_bt);
    }

    private void setInnerListener() {
        mDownloadTV.setOnClickListener(this);
        mUploadTV.setOnClickListener(this);
        mCountResultTV.setOnClickListener(this);
        mPrintResultTV.setOnClickListener(this);
        mCancelBT.setOnClickListener(this);
        mSureBT.setOnClickListener(this);
        mProgressBar.setOnProgressBarListener(new OnProgressBarListener() {
            @Override
            public void onProgressChange(int current, int max) {
                if (current == max) {
                    mTimer.cancel();
                    mTimer = null;
                    setProgressVisibility(false);
                    mProgressBar.setProgress(0);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_station_bt:
                Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
                startActivity(intent);
                break;
            case R.id.sure_station_bt:
                verifyStationID();
                break;
            case R.id.download_tv:
                downloadData();
                break;
            case R.id.upload_tv:
                uploadData();
                break;
            case R.id.count_result_tv:
                countResult();
                break;
            case R.id.print_result_tv:
                printResult();
                break;
            default:
                break;
        }
    }

    private void verifyStationID() {
        hideMask();
    }

    private void downloadData() {
        startProgress();
        setProgressVisibility(true);
    }

    private void uploadData() {
        startProgress();
        setProgressVisibility(true);
    }

    private void countResult() {

    }

    private void printResult() {

    }

    private void setProgressVisibility(boolean display) {
        mProgressBar.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    private void hideMask() {
        mMaskView.setVisibility(View.GONE);
    }

    private void startProgress() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.incrementProgressBy(1);
                    }
                });
            }
        }, 0, 80);
    }
}
