package com.jackson.mycameratwo;

import android.util.Size;

/**
 * @author Jackson
 * @date 2022/8/14.
 * NickName：mawl2022-07-27
 * description：
 */
public class CameraBean {



    private String CameraId;

    private boolean IsFlashSupport;

    private Size PreviewSize;

    //摄像头类型 1是 前置 ，2是 后置
    private int CameraType;


    public CameraBean(String cameraId,boolean isFlashSupport,Size previewSize,int cameraType) {
        CameraId = cameraId;
        IsFlashSupport=isFlashSupport;
        PreviewSize=previewSize;
        CameraType=cameraType;
    }

    public int getCameraType() {
        return CameraType;
    }

    public void setCameraType(int cameraType) {
        CameraType = cameraType;
    }

    public Size getPreviewSize() {
        return PreviewSize;
    }

    public void setPreviewSize(Size previewSize) {
        PreviewSize = previewSize;
    }

    public boolean isFlashSupport() {
        return IsFlashSupport;
    }

    public void setFlashSupport(boolean flashSupport) {
        IsFlashSupport = flashSupport;
    }

    public String getCameraId() {
        return CameraId;
    }

    public void setCameraId(String cameraId) {
        CameraId = cameraId;
    }
}
