package com.xiongdi.recognition.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.xiongdi.recognition.R;

/**
 * Created by moubiao on 2016/6/23.
 * 采集指纹的dialog
 */
public class GatherFingerDialogFragment extends DialogFragment implements View.OnClickListener {
    private GatherResultCallback mResultCallback;
    private ImageView fingerIMG;
    Button saveBT, againBT;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View dialogLayout = inflater.inflate(R.layout.gather_fingerprint_pop_layout, container, false);
        fingerIMG = (ImageView) dialogLayout.findViewById(R.id.gather_fingerprint_img);
        Button cancelBT = (Button) dialogLayout.findViewById(R.id.cancel_gather_bt);
        saveBT = (Button) dialogLayout.findViewById(R.id.save_gather_bt);
        saveBT.setEnabled(false);
        againBT = (Button) dialogLayout.findViewById(R.id.gather_again_bt);
        againBT.setEnabled(false);

        cancelBT.setOnClickListener(this);
        saveBT.setOnClickListener(this);
        againBT.setOnClickListener(this);

        return dialogLayout;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_gather_bt:
                mResultCallback.gatherFailed();
                dismiss();
                break;
            case R.id.save_gather_bt:
                mResultCallback.gatherSuccess();
                dismiss();
                break;
            case R.id.gather_again_bt:
                mResultCallback.gatherAgain();
                break;
            default:
                break;
        }
    }

    public void setResultCallback(GatherResultCallback resultCallback) {
        mResultCallback = resultCallback;
    }

    public void setFingerprint(Bitmap bitmap) {
        fingerIMG.setImageBitmap(bitmap);
        saveBT.setEnabled(true);
        againBT.setEnabled(true);
    }

    public interface GatherResultCallback {
        void gatherFailed();

        void gatherSuccess();

        void gatherAgain();
    }
}
