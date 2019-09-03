
package com.mediatek.camera.mode.slr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

public class GaussianBlur {
    private RenderScript mRenderScript;
    private ScriptIntrinsicBlur mScriptBlur;

    public GaussianBlur(Context context) {
        long time = System.currentTimeMillis();
        mRenderScript = RenderScript.create(context);
        mScriptBlur = ScriptIntrinsicBlur.create(mRenderScript, Element.U8_4(mRenderScript));
        Log.i("test", "GaussianBlur,create render:" + (System.currentTimeMillis() - time));

    }

    public void destoryBlur() {
        if (mScriptBlur != null) {
            mScriptBlur.destroy();
            mScriptBlur = null;
        }
        if (mRenderScript != null) {
            mRenderScript.destroy();
            mRenderScript = null;
        }
    }

    public Bitmap small(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(0.1f, 0.1f);
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);
        return resizeBmp;
    }

    public Bitmap blurBitmap(Context context, Bitmap sentBitmap) {
        if (sentBitmap == null || mScriptBlur == null || mRenderScript == null) {
            return null;
        }
        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        long time = System.currentTimeMillis();
        final Allocation input = Allocation.createFromBitmap(mRenderScript, sentBitmap,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        final Allocation output = Allocation.createTyped(mRenderScript, input.getType());
        time = System.currentTimeMillis();
        mScriptBlur.setRadius(4);
        mScriptBlur.setInput(input);
        mScriptBlur.forEach(output);
        output.copyTo(bitmap);
        Log.i("test", "blurBitmap,render bitmap:" + (System.currentTimeMillis() - time));
        return bitmap;
    }

    public Bitmap blurBitmap(Bitmap sentBitmap, int radius) {
        if (sentBitmap == null || mScriptBlur == null || mRenderScript == null) {
            return null;
        }
        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        long time = System.currentTimeMillis();
        final Allocation input = Allocation.createFromBitmap(mRenderScript, sentBitmap,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        final Allocation output = Allocation.createTyped(mRenderScript, input.getType());
        time = System.currentTimeMillis();
        mScriptBlur.setRadius(radius);
        mScriptBlur.setInput(input);
        mScriptBlur.forEach(output);
        output.copyTo(bitmap);
        input.destroy();
        output.destroy();
        Log.i("test", "blurBitmap,render bitmap:" + (System.currentTimeMillis() - time));
        return bitmap;
    }
}
