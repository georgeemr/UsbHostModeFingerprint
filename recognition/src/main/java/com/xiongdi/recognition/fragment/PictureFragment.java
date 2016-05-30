package com.xiongdi.recognition.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.xiongdi.recognition.R;

/**
 * Created by moubiao on 2016/3/23.
 * 头像
 */
public class PictureFragment extends Fragment {
    private static ImageView pictureIMG;
    private String imgPath = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            imgPath = savedInstanceState.getString("imgPath");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (imgPath != null) {
            outState.putString("imgPath", imgPath);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.picture_layout, container, false);
        pictureIMG = (ImageView) view.findViewById(R.id.picture_img);
        setPicture(imgPath);

        return view;
    }

    public void setPicture(String imgUrl) {
        imgPath = imgUrl;
        if (pictureIMG != null && imgUrl != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            Bitmap bitmap = BitmapFactory.decodeFile(imgUrl, options);
            pictureIMG.setImageBitmap(bitmap);
        }
    }
}
