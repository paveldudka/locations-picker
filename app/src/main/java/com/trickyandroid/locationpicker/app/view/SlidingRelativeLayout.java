package com.trickyandroid.locationpicker.app.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

/**
 * Created by paveld on 4/13/14.
 */
public class SlidingRelativeLayout extends RelativeLayout {

    public SlidingRelativeLayout(Context context) {
        super(context);
    }

    public SlidingRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidingRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private ViewTreeObserver.OnPreDrawListener preDrawListener = null;

    public void setYFraction(final float fraction) {
        if (getHeight() == 0) {
            if (preDrawListener != null) {
                getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
            }
            preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
                    setYFraction(fraction);
                    return true;
                }
            };
            getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            return;
        }
        float translationY = getHeight() * fraction;
        setTranslationY(translationY);
    }

    public float getYFraction() {
        if (getHeight() == 0) {
            return 0;
        }
        return getTranslationY() / getHeight();
    }
}
