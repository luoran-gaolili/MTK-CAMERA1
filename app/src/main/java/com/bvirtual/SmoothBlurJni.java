package com.bvirtual;//应该是自己定义的so，所以包名必须与原来一致，因为so库中的方法与原来的包名有关

import android.graphics.Bitmap;

/**
 * Created by azmohan on 16-9-2.
 */
public class SmoothBlurJni {
    static {
        System.loadLibrary("SmoothBlur");
    }

    public static native void smoothRender(Bitmap blurBitmap, Bitmap oriBitmap, BlurInfo info);
}
