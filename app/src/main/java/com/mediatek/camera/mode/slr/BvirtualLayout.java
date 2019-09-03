package com.mediatek.camera.mode.slr;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.mediatek.camera.R;

public class BvirtualLayout extends RelativeLayout {
    private BvirtualView mBvirtualView;

    public BvirtualLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBvirtualView = (BvirtualView) findViewById(R.id.bvirtual_view);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        RectF rectF = mBvirtualView.getmPreviewArea();
        mBvirtualView.layout((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
    }
}
