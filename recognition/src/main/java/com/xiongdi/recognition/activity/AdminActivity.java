package com.xiongdi.recognition.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.emptech.Printer;
import com.j256.ormlite.stmt.QueryBuilder;
import com.xiongdi.recognition.R;
import com.xiongdi.recognition.application.App;
import com.xiongdi.recognition.bean.Person;
import com.xiongdi.recognition.db.PersonDao;
import com.xiongdi.recognition.fragment.ListDialogFragment;
import com.xiongdi.recognition.util.ToastUtil;
import com.xiongdi.recognition.widget.numberProgressBar.NumberProgressBar;
import com.xiongdi.recognition.widget.numberProgressBar.OnProgressBarListener;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by moubiao on 2016/8/10.
 * 管理员界面
 */
public class AdminActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "moubiao";
    private final int OPEN_BLUETOOTH_FLAG = 1000;

    private TextView mDownloadTV, mUploadTV, mCountResultTV, mPrintResultTV;
    private ProgressBar mInitProgress;
    private NumberProgressBar mProgressBar;
    private View mMaskView;
    private Button mCancelBT, mSureBT;

    private Timer mTimer;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mPrinter = false;
    private String mAddress = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_layout);

        initData();
        initView();
        setInnerListener();
    }

    private void initData() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initView() {
        mInitProgress = (ProgressBar) findViewById(R.id.init_pro);
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
        Intent intent = new Intent(AdminActivity.this, CountActivity.class);
        startActivity(intent);
    }

    private void printResult() {
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "onCreate: device not support bluetooth");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, OPEN_BLUETOOTH_FLAG);
        } else {
            chooseDevice();
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case OPEN_BLUETOOTH_FLAG:
                    chooseDevice();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 选择蓝牙设备
     */
    private void chooseDevice() {
        if (mPrinter) {
            print(mAddress);
        } else {
            Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
            List<BluetoothDevice> deviceList = new ArrayList<>();
            if (devices.size() > 0) {
                for (BluetoothDevice device : devices) {
                    deviceList.add(device);
                }

                ListDialogFragment dialog = new ListDialogFragment();
                dialog.setData(deviceList);
                dialog.setSelectListener(new ListDialogFragment.SelectListener() {
                    @Override
                    public void selectItem(BluetoothDevice device) {
                        mAddress = device.getAddress();
                        print(mAddress);
                    }
                });
                dialog.show(getSupportFragmentManager(), "choose");
            } else {
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
            }
        }
    }

    /**
     * 打印数据
     */
    private void print(String address) {
        if (!mPrinter) {
            displayInitProgress(true);
            mPrinter = Printer.init(address);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayInitProgress(false);
                        }
                    });
                }
            }, 3000);
            if (!mPrinter) {
                ToastUtil.getInstance().showToast(this, "printer init failed!");
                return;
            }
        }

        App.getThreadPoolExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    PersonDao personDao = new PersonDao(AdminActivity.this);
                    QueryBuilder<Person, Integer> queryBuilder = personDao.getQueryBuilder();
                    long totalQuantity = personDao.getQuantity();
                    long votedQuantity = queryBuilder.where().eq("mChecked", 1).countOf();
                    Printer.xCenter();
                    Printer.setRowSpacing(10);
                    Printer.pText(getString(R.string.vote_result));
                    Printer.pText(String.format(getString(R.string.total_voter_quantity), totalQuantity));
                    Printer.pText(String.format(getString(R.string.has_voted_quantity), votedQuantity));
                    Printer.pText(String.format(getString(R.string.not_voted_quantity), totalQuantity - votedQuantity));
                    Printer.pLF();
                    Printer.pLF();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void displayInitProgress(boolean state) {
        mInitProgress.setVisibility(state ? View.VISIBLE : View.GONE);
        mPrintResultTV.setVisibility(state ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Printer.uninit();
    }
}
