package com.mediatek.camera.mode.slr;

import android.app.Activity;
import android.content.Context;

import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.ICameraMode;
import com.mediatek.camera.ISettingCtrl;
import com.mediatek.camera.ISettingRule;
import com.mediatek.camera.platform.ICameraDeviceManager;
import com.mediatek.camera.platform.Parameters;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.setting.SettingItem;
import com.mediatek.camera.setting.SettingUtils;
import com.mediatek.camera.setting.rule.CommonRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlrPreviewRule implements ISettingRule {

    private ICameraDeviceManager mICameraDeviceManager;
    private ICameraContext mICameraContext;
    private ICameraDeviceManager.ICameraDevice mICameraDevice;
    private Parameters mParameters;
    private ISettingCtrl mISettingCtrl;
    private List<String> mConditions = new ArrayList<String>();
    private ICameraDeviceManager deviceManager;
    private Activity mActivity;

    public SlrPreviewRule(ICameraContext cameraContext) {
        mICameraDeviceManager = cameraContext.getCameraDeviceManager();
        mICameraContext = cameraContext;

        mActivity = mICameraContext.getActivity();
        mISettingCtrl = mICameraContext.getSettingController();
    }

    @Override
    public void execute() {
        String value = mISettingCtrl.getSettingValue(SettingConstants.KEY_SLR);


        int index = mConditions.indexOf(value);
        deviceManager = mICameraContext
                .getCameraDeviceManager();

        mICameraDevice = deviceManager.getCameraDevice(deviceManager
                .getCurrentCameraId());

        mParameters = mICameraDevice.getParameters();

        final SettingItem picPatioSetting = mISettingCtrl.getSetting(SettingConstants.KEY_PICTURE_RATIO);

        if (index == -1) {
            picPatioSetting.getListPreference().setEnabled(true);
            String ratio = mISettingCtrl.getSettingValue(SettingConstants.KEY_PICTURE_RATIO);
            SettingUtils.setPreviewSize(mActivity, mICameraDevice.getParameters(), ratio);
        } else {
            picPatioSetting.getListPreference().setEnabled(false);
            mParameters.setPreviewSize(960, 720);
            setPreviewFrameRate(mActivity, mParameters, -1);
        }
    }

    public static void setPreviewFrameRate(Context context, Parameters parameters,
                                           int supportedFramerate) {
        List<Integer> frameRates = null;
        if (supportedFramerate > 0) {
            frameRates = new ArrayList<Integer>();
            frameRates.add(supportedFramerate);
        }

        if (frameRates == null) {
            // Reset preview frame rate to the maximum because it may be lowered
            // by
            // video camera application.
            frameRates = parameters.getSupportedPreviewFrameRates();
        }

        if (frameRates != null) {
            Integer max = Collections.max(frameRates);
            parameters.setPreviewFrameRate(max);
//            Log.d(TAG, "setPreviewFrameRate max = " + max + " frameRates = " + frameRates);
        }
    }

    @Override
    public void addLimitation(String condition, List<String> result, MappingFinder mappingFinder) {
        mConditions.add(condition);
    }
}
