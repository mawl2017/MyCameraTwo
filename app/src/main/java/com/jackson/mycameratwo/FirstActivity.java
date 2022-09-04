package com.jackson.mycameratwo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import butterknife.ButterKnife;

public class FirstActivity extends AppCompatActivity {


    public static final int PERMISSION_REQ_ID = 21;
    //请求权限的集合
    public static final String[] REQUEST_PERMISSION = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        ImageView iv_pic=findViewById(R.id.iv_pic);

        iv_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(FirstActivity.this,MainActivity.class));

            }
        });


        ButterKnife.bind(this);
        //申请动态权限
        if (checkSelfPermission(REQUEST_PERMISSION[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUEST_PERMISSION[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUEST_PERMISSION[2], PERMISSION_REQ_ID)) {
        }

        findViewById(R.id.btn_size).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LinearLayout.LayoutParams linearParams =(LinearLayout.LayoutParams) iv_pic.getLayoutParams(); //取控件textView当前的布局参数 linearParams.height = 20;// 控件的高强制设成20

                linearParams.width = 100;// 控件的宽强制设成30

                iv_pic.setLayoutParams(linearParams); //使设置好的布局参数应用到控件
            }
        });

    }


    private boolean checkSelfPermission(String permission, int request_code) {
        //判断是否有权限
        if (ContextCompat.checkSelfPermission(FirstActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(FirstActivity.this, REQUEST_PERMISSION, request_code);
        }
        return true;
    }


}