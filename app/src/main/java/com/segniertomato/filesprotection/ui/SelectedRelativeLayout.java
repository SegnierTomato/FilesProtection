package com.segniertomato.filesprotection.ui;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.RelativeLayout;


public class SelectedRelativeLayout extends RelativeLayout implements Checkable {

    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
    private boolean mIsChecked;

    public SelectedRelativeLayout(Context context) {
        super(context);
    }

    public SelectedRelativeLayout(Context context, AttributeSet attr) {
        super(context, attr);
    }

    public SelectedRelativeLayout(Context context, AttributeSet attr, int defStyleAttr) {
        super(context, attr, defStyleAttr);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        boolean wasChecked = mIsChecked;
        mIsChecked = checked;

        if (wasChecked ^ mIsChecked) {
            refreshDrawableState();
        }
    }

    @Override
    public void toggle() {
        setChecked(!mIsChecked);
    }

    @Override
    public boolean performClick(){
        toggle();
        return  super.performClick();
    }
}
