package com.xiongdi.recognition.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.xiongdi.recognition.R;

/**
 * Created by moubiao on 2016/8/10.
 * 管理员界面
 */
public class AdminActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "moubiao";

    private TextView mDownloadTV, mUploadTV, mCountResultTV, mPrintResultTV;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_layout);

        initView();
        setInnerListener();
    }

    private void initView() {
        mDownloadTV = (TextView) findViewById(R.id.download_tv);
        mUploadTV = (TextView) findViewById(R.id.upload_tv);
        mCountResultTV = (TextView) findViewById(R.id.count_result_tv);
        mPrintResultTV = (TextView) findViewById(R.id.print_result_tv);
    }

    private void setInnerListener() {
        mDownloadTV.setOnClickListener(this);
        mUploadTV.setOnClickListener(this);
        mCountResultTV.setOnClickListener(this);
        mPrintResultTV.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.download_tv:

                break;
            case R.id.upload_tv:

                break;
            case R.id.count_result_tv:

                break;
            case R.id.print_result_tv:

                break;
            default:
                break;
        }
    }
}
