package com.mediatek.camera.mode.slr;

import android.app.Activity;
import android.hardware.Camera;
import android.util.Log;

import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.ISettingCtrl;
import com.mediatek.camera.ISettingRule;
import com.mediatek.camera.platform.ICameraDeviceManager;
import com.mediatek.camera.platform.Parameters;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.setting.SettingUtils;
import com.mediatek.camera.setting.preference.ListPreference;

import java.util.ArrayList;
import java.util.List;

public class SlrPictureRule implements ISettingRule {

    private ICameraContext mCameraContext;
    private ISettingCtrl mISettingCtrl;
    private List<String> mConditions = new ArrayList<String>();
    private ICameraDeviceManager deviceManager;
    private ICameraDeviceManager.ICameraDevice mCamDevice;
    private Parameters mParameters;
    private Activity mActivity;

    public SlrPictureRule(ICameraContext cameraContext) {
        mCameraContext = cameraContext;
    }

    @Override
    public void execute() {
        mISettingCtrl = mCameraContext.getSettingController();
        deviceManager = mCameraContext.getCameraDeviceManager();
        mCamDevice = deviceManager.getCameraDevice(deviceManager
                .getCurrentCameraId());

        String conditionValue = mISettingCtrl
                .getSettingValue(SettingConstants.KEY_SLR);
        int index = conditionSatisfied(conditionValue);
        mParameters = mCamDevice.getParameters();
        mActivity = mCameraContext.getActivity();

        final ListPreference listPreference = mISettingCtrl.getListPreference(SettingConstants.KEY_PICTURE_SIZE);
        if (index == -1) {
            List<Camera.Size> supported = mParameters.getSupportedPictureSizes();
            String pictureSize = mISettingCtrl.getSettingValue(SettingConstants.KEY_PICTURE_SIZE);
            String ratio = mISettingCtrl.getSettingValue(SettingConstants.KEY_PICTURE_RATIO);
            SettingUtils.setCameraPictureSize(pictureSize, supported,
                    mParameters, ratio, mActivity);
            listPreference.setEnabled(true);
        } else {
            listPreference.setEnabled(false);
            setPictureSize();
        }
    }

    private void setPictureSize() {
        // Set Bottom sensor Picture size.
        List<Camera.Size> supported = mParameters.getSupportedPictureSizes();
        // SettingUtils.setPictureSize(value, supported, mParameters, );
        String pictureSize = mISettingCtrl.getSettingValue(SettingConstants.KEY_PICTURE_SIZE);
        String ratio = mISettingCtrl.getSettingValue(SettingConstants.KEY_PICTURE_RATIO);
        /*SettingUtils.setCameraPictureSize("3264x2448", supported,
                mParameters, "1.3333", mActivity);*/
        mParameters.setPictureSize(1024, 768);
    }


    private int conditionSatisfied(String conditionValue) {
        int index = mConditions.indexOf(conditionValue);
        return index;
    }

    @Override
    public void addLimitation(String condition, List<String> result, MappingFinder mappingFinder) {
        mConditions.add(condition);
    }
}
