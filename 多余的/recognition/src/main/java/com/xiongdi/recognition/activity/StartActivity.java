package com.xiongdi.recognition.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.xiongdi.recognition.R;

/**
 * Created by moubiao on 2016/3/22.
 */
public class StartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);

        ImageView logoIMG = (ImageView) this.findViewById(R.id.logo_img);
        AlphaAnimation logoAni = new AlphaAnimation(0.1f, 1.0f);
        logoAni.setDuration(2000);
        if (logoIMG != null) {
            logoIMG.setAnimation(logoAni);
        }

        logoAni.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Intent it = new Intent(StartActivity.this, LoginActivity.class);
                startActivity(it);

                finish();
            }
        });
    }
}
