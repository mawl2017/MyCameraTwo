package com.jackson.mycameratwo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG_PREVIEW = "预览";

    //相机设备，java代码中代表Camera的对象，可以关闭相机，向相机硬件端发出请求等等。
    protected CameraDevice mCameraDevice;

    //预览请求的Builder
    private Surface mPreviewSurface;
    private Size mPreviewSize;
    private ImageReader mImageReader;
    private CameraCaptureSession mCaptureSession;
    //    public File mImageFile;
    private String cameraID;
    private TextureView textureView;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    //是否支持 闪光灯
    private boolean isFlashSupport;
    //当前摄像头类型是
    private int cameraType=2;

    //相机是否 预览中
    private boolean previewRunning=false;


    //摄像头集合
    private List<CameraBean> cameraIdList;

    //其他控件的初始化
    private boolean isLight=false;

    public boolean isLight() {
        return isLight;
    }

    public void setLight(boolean light) {
        isLight = light;
    }

    private TextView tv_long,tv_short,tv_wide;
    private TextView tv_cancel;
    private ImageView img_take_light,iv_photo_camre,iv_photo_rotate;
    private Button savePicture;

    private int selIndex=0;

    public int getSelIndex() {
        return selIndex;
    }

    public void setSelIndex(int selIndex) {
        this.selIndex = selIndex;
    }

    //获取手机屏幕尺寸
    private DisplayMetrics dm;
    private int mWidth;
    private int mHeight;

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //当SurefaceTexture可用的时候，设置相机参数并打开相机
            //配置相机参数
            setupCamera(width, height);
            //
            configureTransform(width, height);
            Log.d("mawl","当前相机的 宽高尺寸是 =="+width+"  "+height);
            //打开相机
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            //当SurefaceTexture状态改变时调用此方法
            Log.d("mawl","相机状态 改变");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            //当SurefaceTexture销毁时调用此方法
            Log.d("mawl","相机状态 销毁");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //当SurefaceTexture状态更新时调用此方法
            Log.d("mawl","相机状态 状态更新");
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //v7包下去除标题栏代码：
//        this.getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        initView();
        ButterKnife.bind(this);
        dm=new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mWidth=dm.widthPixels;
        mHeight=dm.heightPixels;
        Log.d("mawl","获取的屏幕分辨率尺寸是=="+mWidth+" x "+mHeight);

        //默认设置开始尺寸

        setTextureView_Size(mWidth/2,mHeight/4);


        textureView.setSurfaceTextureListener(textureListener);

    }

    private void initView() {
        //相机控件
        textureView = findViewById(R.id.textureView);

        tv_long=findViewById(R.id.photo_long);
        tv_short=findViewById(R.id.photo_short);
        tv_wide=findViewById(R.id.photo_wide);
        tv_cancel=findViewById(R.id.photo_cancel);

        iv_photo_camre=findViewById(R.id.img_take_photo);
        img_take_light=findViewById(R.id.img_take_light);
        iv_photo_rotate=findViewById(R.id.img_take_rotate);

        savePicture=findViewById(R.id.savePicture);

        tv_long.setOnClickListener(this);
        tv_short.setOnClickListener(this);
        tv_wide.setOnClickListener(this);
        tv_cancel.setOnClickListener(this);

        iv_photo_camre.setOnClickListener(this);
        img_take_light.setOnClickListener(this);
        iv_photo_rotate.setOnClickListener(this);

        //保存图片 按钮
        savePicture.setOnClickListener(this);

        //摄像头id数量
        cameraIdList=new ArrayList<>();


    }
    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.img_take_light:
                //闪光灯
                if(isLight()==true){
                    //关闭 灯光
                    Log.d("mawl","关闭 闪光灯");
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);

                    setLight(false);
                    img_take_light.setImageResource(R.mipmap.image_close_light);

                }else {
                    //打开灯光
                    Log.d("mawl","打开 闪光灯");
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);

                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);

                    setLight(true);
                    img_take_light.setImageResource(R.mipmap.image_take_light);

                }

                break;

            case R.id.photo_long:
                //长图
                showToast("设置长图");
                selIndex=0;
                setSelIndex(0);
                tv_long.setBackgroundResource(R.drawable.bg_rect_theme_30dp);
                tv_short.setBackgroundResource(R.drawable.bg_rect_theme_30dp_gray);
                tv_wide.setBackgroundResource(R.drawable.bg_rect_theme_30dp_gray);
                break;
            case R.id.photo_short:
                //短图
                showToast("设置短图");
                selIndex=1;
                setSelIndex(1);
                tv_long.setBackgroundResource(R.drawable.bg_rect_theme_30dp_gray);
                tv_short.setBackgroundResource(R.drawable.bg_rect_theme_30dp);
                tv_wide.setBackgroundResource(R.drawable.bg_rect_theme_30dp_gray);

                break;
            case R.id.photo_wide:
                //宽图
                showToast("设置宽图");
                selIndex=2;
                setSelIndex(2);
                tv_long.setBackgroundResource(R.drawable.bg_rect_theme_30dp_gray);
                tv_short.setBackgroundResource(R.drawable.bg_rect_theme_30dp_gray);
                tv_wide.setBackgroundResource(R.drawable.bg_rect_theme_30dp);
                break;
            case R.id.photo_cancel:
                //取消
                finish();
                break;
            case R.id.img_take_photo:
                //拍照
                Toast.makeText(MainActivity.this,"点击了拍照按钮",Toast.LENGTH_SHORT).show();
                capture();

//
//                LinearLayout.LayoutParams linearParams =(LinearLayout.LayoutParams) textureView.getLayoutParams(); //取控件textView当前的布局参数 linearParams.height = 20;// 控件的高强制设成20
//
//                linearParams.width = 100;// 控件的宽强制设成30
//
//                textureView.setLayoutParams(linearParams); //使设置好的布局参数应用到控件

                break;
            case R.id.img_take_rotate:
                //切换摄像头

                if(cameraType==1){
                    //前置 摄像头
                    Log.d("mawl","当前 前置，去切换为 后置摄像头");
                    switchCamera(2);

                }else if(cameraType==2){
                    //后置 摄像头
                    Log.d("mawl","当前后置，去切换为 前置摄像头");
                    switchCamera(1);
                }




                break;

            case R.id.savePicture:
                //保存图片

                break;

        }
    }

    private void showToast(String text) {//简化Toast
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }
    /**
     * 切换摄像头
     */
    private void switchCamera(int cameraType0) {

        cameraType=cameraType0;

            for (CameraBean cameraBean:cameraIdList){

                if(cameraType0==1){
                    //设置前置 摄像头
                    if(cameraBean.getCameraType()==1){
                        mPreviewSize = cameraBean.getPreviewSize();
                        cameraID = cameraBean.getCameraId();
                        mCameraDevice.close();
                        openCamera();
                        break;
                    }

                }else {
                    //设置后置 摄像头
                    if(cameraBean.getCameraType()==2){
                        mPreviewSize = cameraBean.getPreviewSize();
                        cameraID = cameraBean.getCameraId();
                        mCameraDevice.close();
                        openCamera();
                        break;
                    }
                }

            }




    }



    /**
     * 设置摄像头宽高尺寸
     * @param width
     * @param height
     */
    private void setupCamera(int width, int height) {
        // 获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // 遍历所有摄像头
            for (String cameraId : manager.getCameraIdList()) {

                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                //判断是否支持 闪光灯
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);

                // 获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size previewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);

                //给摄像头集合添加  后置摄像头
                if (characteristics.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_FRONT) {
                    //后置摄像头
                    cameraIdList.add(new CameraBean(cameraId,available,previewSize,2));
                }else {
                    //前置 摄像头
                    cameraIdList.add(new CameraBean(cameraId,available,previewSize,1));
                }

                // 默认打开后置摄像头 - 忽略前置摄像头
                if (characteristics.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_FRONT) {

                    //首先默认是 前置摄像头
                    cameraID = cameraId;
                    mPreviewSize=previewSize;
                    isFlashSupport=available;
                    cameraType=2;//代表后置
                    Log.d("mawl","当前相机参数是=="+"当前摄像头ID是=="+cameraID+"   尺寸是=="+mPreviewSize+" 是否闪光灯=="+isFlashSupport);

                }

            }


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //关闭销毁相机
    private void closeCamera() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;

        }
    }

    /**
     * 打开相机
     */
    private void openCamera() {
        Log.d("mawl","初始化 打开相机01");
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        //检查权限
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "请检查权限是否打开", Toast.LENGTH_LONG).show();
                return;
            }
            //打开相机，第一个参数指示打开哪个摄像头，第二个参数stateCallback为相机的状态回调接口，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            manager.openCamera(cameraID, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //实现StateCallback 接口，当相机打开后会回调onOpened方法
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d("mawl","相机状态回调 监听，打开摄像头 开启预览");
            //打开摄像头
            mCameraDevice = camera;
            //开启预览
            startPreview();
            //设置预览 状态为 true
            previewRunning=true;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            //关闭摄像头
            Log.d("mawl","关闭摄像头");
            previewRunning=false;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            //发生错误
            Log.d("mawl","相机 发生错误");
        }
    };

    /**
     * 开启预览
     */
    private void startPreview() {
        Log.d("mawl","相机预览开始");
        setupImageReader();//配置 图片数据流

        SurfaceTexture mSurfaceTexture = textureView.getSurfaceTexture();
        //设置TextureView的缓冲区大小
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //获取Surface显示预览数据
        mPreviewSurface = new Surface(mSurfaceTexture);
        try {
            getPreviewRequestBuilder();
            //创建相机捕获会话，第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession的状态回调接口，
            // 当它创建好后会回调onConfigured方法，第三个参数用来确定Callback在哪个线程执行，为null的话就在当前线程执行
            mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.d("mawl","创建相机捕获 配置 ");
                    mCaptureSession = session;
                    repeatPreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.d("mawl","创建相机捕获 会话");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置反复捕获数据的请求
     */
    private void repeatPreview() {
        Log.d("mawl","设置反复捕获数据的请求");
        mPreviewRequestBuilder.setTag(TAG_PREVIEW);
        mPreviewRequest = mPreviewRequestBuilder.build();
        //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
        try {
            mCaptureSession.setRepeatingRequest(mPreviewRequest, captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    // 创建预览请求的Builder（TEMPLATE_PREVIEW表示预览请求）
    private void getPreviewRequestBuilder() {
        Log.d("mawl","创建预览请求的Builder");
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //设置预览的显示界面
        mPreviewRequestBuilder.addTarget(mPreviewSurface);
        MeteringRectangle[] meteringRectangles = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AF_REGIONS);
        if (meteringRectangles != null && meteringRectangles.length > 0) {
            Log.e("LEE", "PreviewRequestBuilder: AF_REGIONS=" + meteringRectangles[0].getRect().toString());
        }

        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
    }


    private void setupImageReader() {
        Log.d("mawl","配置图片读取流");
//      前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 1);
//      监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {

                //新的images通过ImageReader的surface发送给ImageReader，类似一个队列，需要通过acquireLatestImage()或者acquireNextImage()方法取出Image
                Image image = reader.acquireLatestImage();
                // 开启线程异步保存图片
                new Thread(new ImageSaver(image)).start();
                Log.e("mawl", "保存图片02 ");
            }
        }, null);
    }

    public static class ImageSaver implements Runnable {
        private Image mImage;

        public ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            File mImageFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/myPicture.jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mImageFile);
                fos.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImageFile = null;
                if (fos != null) {
                    try {
                        fos.close();
                        fos = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    /**
     * 拍照
     */
    private void capture() {
        if (mCameraDevice == null){
            return;
        }
        try {
            Log.e("mawl", "正式执行拍照 ");
            //首先我们创建请求拍照的CaptureRequest
            final CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //获取屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCaptureBuilder.addTarget(mPreviewSurface);
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            //设置拍照方向
            Log.d("mawl","拍照的方向是=="+rotation);
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            //停止预览
            mCaptureSession.stopRepeating();


            mCaptureSession.capture(mCaptureBuilder.build(), captureCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }



    //开始拍照，然后回调上面的接口重启预览，因为mCaptureBuilder设置ImageReader作为target，
    // 所以会自动回调ImageReader的onImageAvailable()方法保存图片
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.d("mawl","拍照进度 ");
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {

//            repeatPreview();
            Log.d("mawl","拍照完成结果  "+result);


        }
    };

    private void setTextureView_Size(int width,int height){

//        RelativeLayout rl_pic=findViewById(R.id.rl_pic);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) textureView.getLayoutParams();
        params.width = dip2px(MainActivity.this, width);
        params.height = dip2px(MainActivity.this, height);
        // params.setMargins(dip2px(MainActivity.this, 1), 0, 0, 0); // 可以实现设置位置信息，如居左距离，其它类推
        // params.leftMargin = dip2px(MainActivity.this, 1);
        textureView.setLayoutParams(params);

//        RelativeLayout relativeLayout = new RelativeLayout(this);
//        relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(
//                RelativeLayout.LayoutParams.MATCH_PARENT,
//                RelativeLayout.LayoutParams.MATCH_PARENT));

    }



    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }


    //选择sizeMap中大于并且最接近width和height的size
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        Log.d("mawl","选择sizeMap中大于并且最接近width和height的size");
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Log.d("mawl","配置 configureTransform 角度");
        if (null == textureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    @Override
    protected void onPause() {
        closeCamera();
        super.onPause();
    }
    /**
     * dp转为px
     *
     * @param context  上下文
     * @param dipValue dp值
     * @return
     */
    private int dip2px(Context context, float dipValue) {
        Resources r = context.getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dipValue, r.getDisplayMetrics());
    }


}