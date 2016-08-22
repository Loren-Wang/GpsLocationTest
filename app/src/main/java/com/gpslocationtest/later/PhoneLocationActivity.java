package com.gpslocationtest.later;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.gpslocationtest.R;

public class PhoneLocationActivity extends AppCompatActivity {

    private long timeInterval = 1500;
    private Context context;
    private final String TAG = "PhoneLocationActivity";
    private long exitTime = 0;

    Double lat;
    Double lng;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_location);
        context = this;

        init();

        final Intent intent = new Intent(context,MyServiceTest.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startService(intent);

        final EditText edtTime = (EditText) findViewById(R.id.edtTime);
        Button btnConfirm = (Button) findViewById(R.id.btnConfirm);


        edtTime.setHint("请输入lat");
        edtTime.setText("");

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(edtTime.getHint().toString().equals("请输入lat")) {
                    lat = Double.parseDouble(edtTime.getText().toString());
                }else {
                    lng = Double.parseDouble(edtTime.getText().toString());
                }
                if(lat == null){
                    edtTime.setHint("请输入lat");
                    edtTime.setText("");
                }else if(lng == null){
                    edtTime.setHint("请输入lng");
                    edtTime.setText("");
                }

                if(lat != null && lng != null){
                    LatLng latLng = new LatLng(lat,lng);

//                    // 将google地图、soso地图、aliyun地图、mapabc地图和amap地图// 所用坐标转换成百度坐标
//                    CoordinateConverter converter  = new CoordinateConverter();
//                    converter.from(CoordinateConverter.CoordType.COMMON);
//                    // sourceLatLng待转换坐标
//                    converter.coord(latLng);
//                    LatLng desLatLng = converter.convert();

                    // 将GPS设备采集的原始GPS坐标转换成百度坐标
                    CoordinateConverter converter  = new CoordinateConverter();
                    converter.from(CoordinateConverter.CoordType.GPS);
                    // sourceLatLng待转换坐标
                    converter.coord(latLng);
                    LatLng desLatLng = converter.convert();

                    edtTime.setText("" + desLatLng.longitude + "," +desLatLng.latitude);

                    lat = null;
                    lng = null;
                }
            }
        });

        //百度地图初始化
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());


        LatLng latLng = new LatLng(31.143383,121.218829);


       // 将GPS设备采集的原始GPS坐标转换成百度坐标
        CoordinateConverter converter  = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
       // sourceLatLng待转换坐标
        converter.coord(latLng);
        LatLng desLatLng = converter.convert();

        edtTime.setText("" + desLatLng.longitude + "," +desLatLng.latitude);



//        final Handler handler = new Handler(){
//            @Override
//            public void handleMessage(Message msg) {
//                super.handleMessage(msg);
//                List<Location> locationList = PhoneLocationUtils.getInstance(context).startToLocate(timeInterval);
//                PhoneLocationUtils.getInstance(context).save(locationList,path);
//            }
//        };
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        handler.sendMessage(Message.obtain());
//                        Thread.sleep(timeInterval);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();

    }

    private void init(){

        long millisecond = PhoneLocationService.getMillisecond();
        PhoneLocationService.addContentToFile(PhoneLocationService.savePath,
                PhoneLocationService.getFormatedDateTime("\r\n\r\nyyyy.MM.dd-HH:mm:ss.SSS",millisecond) + " \r\n程序已启动\r\n");
    }

    @Override
    public void onBackPressed() {
        //根据当前显示的页面判断是否是在导航页，如果在则退出使用双次点击后退键退出
        if ((System.currentTimeMillis() - exitTime) > 2000) {//判断两次点击的时间间隔是否大于2000ms
            Toast.makeText(context,"再按一次退出程序",Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();//记录第一次点击后退的时间
            return;//必须返回，否则会向下执行父类方法
        }else {
            finish();
            long millisecond = PhoneLocationService.getMillisecond();
            PhoneLocationService.addContentToFile(PhoneLocationService.savePath,
                    PhoneLocationService.getFormatedDateTime("\r\n\r\nyyyy.MM.dd-HH:mm:ss.SSS", millisecond) + " \r\n程序已关闭\r\n");
        }
        super.onBackPressed();
    }
}
