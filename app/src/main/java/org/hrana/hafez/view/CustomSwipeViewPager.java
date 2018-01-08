package org.hrana.hafez.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Enable or disable swiping between viewpager screens.
 */
public class CustomSwipeViewPager extends ViewPager {
    private boolean isSwipeEnabled;

    public CustomSwipeViewPager(Context context) {
        super(context);
    }

    public CustomSwipeViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Override whether can swipe between pages
        return isSwipeEnabled && super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Override whether can swipe between pages
        return isSwipeEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean executeKeyEvent(KeyEvent event) {
        // Override swiping by keyboard arrows--for emulator, tv, or tablet
        return isSwipeEnabled && super.executeKeyEvent(event);
    }

    public void setSwipeEnabled(boolean swipeEnabled) {
        this.isSwipeEnabled = swipeEnabled;
    }

    public boolean isSwipeEnabled() {
        return isSwipeEnabled;
    }

}
