package com.xiongdi.recognition.widget.AverageViewGroup;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by moubiao on 2016/7/22.
 * 子view间隔相等的水平线性布局
 */
public class AverageLinearLayout extends LinearLayout {

    public AverageLinearLayout(Context context) {
        super(context);
    }

    public AverageLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AverageLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AverageLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int groupWidth = getMeasuredWidth();
        int groupHeight = getMeasuredHeight();

        int childrenCount = getChildCount();
        int visibleChildrenCount = 0;
        int visibleChildIndex = 0;
        int visibleChildrenWidth = 0;
        int visibleChildrenTotalWidth = 0;
        int childWidth;
        int childHeight;
        int averageGap;

        for (int i = 0; i < childrenCount; i++) {
            if (getChildAt(i).getVisibility() != View.GONE) {
                visibleChildrenCount++;
                visibleChildrenTotalWidth += getChildAt(i).getMeasuredWidth();
            }
        }

        averageGap = (groupWidth - visibleChildrenTotalWidth) / (visibleChildrenCount + 1);
        for (int i = 0; i < childrenCount; i++) {
            View childView = getChildAt(i);
            if (childView.getVisibility() != View.GONE) {
                childWidth = childView.getMeasuredWidth();
                childHeight = childView.getMeasuredHeight();
                int childLeft, childTop, childRight, childBottom;
                childLeft = averageGap * (visibleChildIndex + 1) + visibleChildrenWidth;
                childTop = (groupHeight - childHeight) / 2;
                childRight = childLeft + childWidth;
                childBottom = childHeight + childTop;

                childView.layout(childLeft, childTop, childRight, childBottom);

                visibleChildIndex++;
                visibleChildrenWidth += childWidth;
            }
        }
    }
}
