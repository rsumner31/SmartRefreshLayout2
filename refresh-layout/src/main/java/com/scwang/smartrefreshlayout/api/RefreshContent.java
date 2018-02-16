package com.scwang.smartrefreshlayout.api;

import android.view.View;
import android.view.ViewGroup;

/**
 * 刷新内容组件
 * Created by SCWANG on 2017/5/26.
 */

public interface RefreshContent {
    void moveSpinner(int driftValue);
    boolean canScrollUp();
    boolean canScrollDown();
    void measure(int widthSpec, int heightSpec);
    int getMeasuredWidth();
    int getMeasuredHeight();
    void layout(int left, int top, int right, int bottom);

    View getScrollableView();
    ViewGroup.LayoutParams getLayoutParams();
}
