package com.mediatek.camera.mode.slr;

import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.mode.PhotoMode;
import com.mediatek.camera.mode.pip.PipPictureSizeRule;
import com.mediatek.camera.setting.SettingConstants;

public class SlrPhotoMode extends PhotoMode {
    public SlrPhotoMode(ICameraContext cameraContext) {
        super(cameraContext);
        setPhotoRule();
    }

    private void setPhotoRule() {
        SlrPreviewRule mSlrPreviewSizeRule = new SlrPreviewRule(mICameraContext);
        mSlrPreviewSizeRule.addLimitation("on", null, null);
        mISettingCtrl.addRule(SettingConstants.KEY_SLR,
                SettingConstants.KEY_PICTURE_RATIO, mSlrPreviewSizeRule);

        SlrPictureRule pictureSizeRule = new SlrPictureRule(
                mICameraContext);
        pictureSizeRule.addLimitation("on", null, null);
        mISettingCtrl.addRule(SettingConstants.KEY_SLR,
                SettingConstants.KEY_PICTURE_SIZE, pictureSizeRule);

    }

    public CameraModeType getCameraModeType() {
        return CameraModeType.EXT_MODE_SLR;
    }

    @Override
    public boolean execute(ActionType type, Object... arg) {
        return super.execute(type, arg);
    }

    @Override
    public void resume() {
        super.resume();
    }

}
