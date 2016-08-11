package com.xiongdi.recognition.widget.maskView;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by moubiao on 2016/8/11.
 */
public class MaskView extends FrameLayout {
    public MaskView(Context context) {
        super(context);
    }

    public MaskView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return true;
    }
}
