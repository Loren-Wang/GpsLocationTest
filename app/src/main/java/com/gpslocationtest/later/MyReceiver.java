package com.gpslocationtest.later;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MyReceiver extends BroadcastReceiver {
    /*要接收的intent源*/
    //static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    private static final int MODE_PRIVATE = 0;
    private Context context;
    private long timeInterval = 15000;//定位服务定位间隔时间
    public static String savePath = Environment.getExternalStorageDirectory().getPath()+"/GpsLocationTest/" + "location.txt";//定位数据保存地址
    private long nowMillisecond;//当前系统时间的毫秒值
    private int num = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        if (intent.getAction().equals("VIDEO_TIMER")) {
            List<Location> locationList = startToLocate(timeInterval);
            save(locationList,savePath,timeInterval);
            addContentToFile(Environment.getExternalStorageDirectory().getPath()+"/GpsLocationTest/" + "location123.txt",
                    getFormatedDateTime("HH:mm:ss",getMillisecond()) + "  当前循环次数" + num++ + "\r\n");

        }
    }


    /**
     * 定位广播接收器，用来接收activity传递过来的值以修改service中的参数
     */
    private class LocationBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            nowMillisecond = getMillisecond();
            StringBuffer logContent = new StringBuffer( "{" + getFormatedDateTime("yyyy.MM.dd-HH:mm:ss.SSS", nowMillisecond));
            if(intent != null) {
                timeInterval = intent.getLongExtra(TAG_SEND_LOCATION_TIMEINTERVAL_TO_SERVICE,timeInterval);
                logContent.append("接收到请求定位间隔时间设置，定位间隔时间：" + timeInterval + "\r\n");
            }else {
                logContent.append( "接收到请求定位间隔时间设置，但是传输错误，设置失败\r\n");
            }
            addContentToFile(savePath, String.valueOf(logContent));
        }
    }






    private LocationManager locationManager;
    private String provider;


    synchronized public static void addContentToFile(String path, String content) {
        checkDirPathIsExistAndBuild(path);
        try {
            FileWriter fw = new FileWriter(new File(path), true);//以追加的模式将字符写入
            BufferedWriter bw = new BufferedWriter(fw);//又包裹一层缓冲流 增强IO功能
            bw.write(content);
            bw.flush();//将内容一次性写入文件
            bw.close();
        } catch (Exception e) {
        }

    }

    /**
     * 检查文件夹是否存在并创建文件夹
     *
     * @param path
     */
    public static void checkDirPathIsExistAndBuild(String path) {
        checkDirPathIsExistAndBuild(new File(path));
    }

    /**
     * 检查文件夹是否存在并创建文件夹
     *
     * @param file
     */
    public static void checkDirPathIsExistAndBuild(File file) {
        try {
            if (!file.isDirectory()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
            } else {
                if (!file.exists()) {
                    file.mkdirs();
                }
            }
        } catch (Exception e) {
        }
    }


    private static final String LOCATION_GPS = "gps";
    private static final String LOCATION_NETWORK = "network";
    private static final String LOCATION_PASSIVE = "passive";
    private long timeIntervals = 0;


    /**
     * 初始化
     */
    public void initLocationManager() {
        // Get the location manager
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LOCATION_GPS, 1000, 1, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                int i = 0;
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        });

        locationManager.requestLocationUpdates(LOCATION_NETWORK, 1000, 1, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                int i = 0;
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        });
        locationManager.requestLocationUpdates(LOCATION_PASSIVE, 1000, 1, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                int i = 0;
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        });
    }


    public List<Location> startToLocate(long timeInterval) {
        initLocationManager();
        timeIntervals = timeInterval;
        if (!checkGpsIsOpen()) {
            openGps();
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }

        List<Location> locationList = new ArrayList<>();
        locationList.add(locationManager.getLastKnownLocation(LOCATION_PASSIVE));
        locationList.add(locationManager.getLastKnownLocation(LOCATION_GPS));
        locationList.add(locationManager.getLastKnownLocation(LOCATION_NETWORK));
        return locationList;
    }



    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     * @return true 表示开启
     */
    private boolean checkGpsIsOpen() {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps) {
            return true;
        }
        return false;
    }

    /**
     * 打开gps设备
     */
    private void openGps(){
        //打开GPS  www.2cto.com
        Settings.Secure.setLocationProviderEnabled(context.getApplicationContext().getContentResolver(), LocationManager.GPS_PROVIDER, true);
    }
    /**
     * 关闭gps设备
     */
    private void closeGps(){
        //关闭GPS
        Settings.Secure.setLocationProviderEnabled(context.getContentResolver(), LocationManager.GPS_PROVIDER, false);
    }

    /**
     * 获取当前网络类型
     * @return 0：没有网络   1：WIFI网络   2：WAP网络    3：NET网络
     */

    private static final int NETTYPE_WIFI = 1;
    private static final int NETTYPE_CMWAP = 2;
    private static final int NETTYPE_CMNET = 3;
    private String getNetworkType() {
        int netType = 0;
        String netTypeName = null;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return "null";
        }


        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE) {
            String extraInfo = networkInfo.getExtraInfo();
            if(extraInfo != null) {
                if (extraInfo.toLowerCase().equals("cmnet")) {
                    netType = NETTYPE_CMNET;
                    netTypeName = "cmNet";
                } else {
                    netType = NETTYPE_CMWAP;
                    netTypeName = "cmWap";
                }
            }
        } else if (nType == ConnectivityManager.TYPE_WIFI) {
            netType = NETTYPE_WIFI;
            netTypeName = "wifi";
        }

        return netTypeName;
    }

    /**
     * 检查wifi管理器状态，如果管理器为空则返回true，反之返回false
     * @return 如果管理器为空则返回true，反之返回false
     */
    private boolean checkWifiManagerIsNULL(){
        if(wifiManager == null){
            Log.e(getClass().getName(),"wifiManager为空");
            return true;
        }else {
            Log.e(getClass().getName(),"wifiManager不为空");
            return false;
        }
    }

    private WifiManager wifiManager;

    /**获取当期的wifi状态
     */
    private String getWifiState(){
        if(checkWifiManagerIsNULL()){
            Log.e(getClass().getName(),"获取wifi状态失败,wifiManager为空");
            return null;
        }
        switch (wifiManager.getWifiState()){
            case WifiManager.WIFI_STATE_DISABLED:
                Log.e(getClass().getName(),"WIFI网卡不可用");
                return "WIFI网卡不可用";
            case WifiManager.WIFI_STATE_DISABLING:
                Log.e(getClass().getName(),"WIFI正在关闭");
                return "WIFI正在关闭";
            case WifiManager.WIFI_STATE_ENABLED:
                Log.e(getClass().getName(),"WIFI网卡可用");
                return "WIFI网卡可用";
            case WifiManager.WIFI_STATE_ENABLING:
                Log.e(getClass().getName(),"WIFI网卡正在打开");
                return "WIFI网卡正在打开";
            case WifiManager.WIFI_STATE_UNKNOWN:
                Log.e(getClass().getName(),"未知网卡状态");
                return "未知网卡状态";
        }
        return null;
    }


    /**
     * 检测飞行模式状态
     * @return
     */
    private boolean checkFeiXingState() {
        return (Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1 ? true : false);
    }

    /**
     * 检测网络是否可用
     * @return
     */
    private boolean checkNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }



    public void save(List<Location> locationList,String addFilePath,long timeInterval){
        Location locationPassive = null;
        Location locationGps = null;
        Location locationNet = null;    //监视地理位置变化

        if(locationList != null) {
            for (int i = 0; i < locationList.size(); i++) {
                for (int j = 0; j < locationList.size(); j++) {
                    if (locationList.get(j) != null) {
                        if (locationList.get(j).getProvider().equals(LOCATION_GPS)) {
                            locationGps = locationList.get(j);
                        } else if (locationList.get(j).getProvider().equals(LOCATION_NETWORK)) {
                            locationNet = locationList.get(j);
                        } else if (locationList.get(j).getProvider().equals(LOCATION_PASSIVE)) {
                            locationPassive = locationList.get(j);
                        }
                    }
                }
            }
        }


        StringBuffer buffer = new StringBuffer("");
        long timeMillisecond = getMillisecond();

        buffer.append("[" + getFormatedDateTime("HH:mm:ss.SSS", timeMillisecond) + "]");
        buffer.append("  GPS:" + checkGpsIsOpen());
        buffer.append("  NetWork:" + getNetworkType() + "\r\n");


        if (locationGps != null) {
            if (getMillisecond() - locationGps.getTime() > timeInterval) {
                buffer.append("  GPS(c):" + locationGps.getLongitude() + "/" + locationGps.getLatitude());
            } else {
                buffer.append("  GPS(r):" + locationGps.getLongitude() + "/" + locationGps.getLatitude());
            }
        } else {

            buffer.append("  GPS:null");
        }


        if (locationNet != null) {
            if (getMillisecond() - locationNet.getTime() > timeInterval) {
                buffer.append("  Network(c):" + locationNet.getLongitude() + "/" + locationNet.getLatitude());
            } else {
                buffer.append("  Network(r):" + locationNet.getLongitude() + "/" + locationNet.getLatitude());
            }
        } else {
            buffer.append("  Network:null");
        }

        if (locationPassive != null) {
            if (getMillisecond() - locationPassive.getTime() > timeInterval) {
                buffer.append("  passive(c):" + locationPassive.getLongitude() + "/" + locationPassive.getLatitude());
            } else {
                buffer.append("  passive(r):" + locationPassive.getLongitude() + "/" + locationPassive.getLatitude());
            }
        } else {
            buffer.append("  passive:null");
        }

        buffer.append("\r\n");
        addContentToFile(addFilePath, String.valueOf(buffer));
    }


    //向服务中发送定位时间间隔请求的传输键值标记
    public static String TAG_SEND_LOCATION_TIMEINTERVAL_TO_SERVICE = "tag_send_location_timeinterval_to_service";
    public static String LOCATION_BROADCASTRECEIVER_ACTION = "";//广播接收器的action



    /**
     *   根据日期时间获得毫秒数
     * @param dateAndTime  日期时间："201104141302"
     * @param dateAndTimeFormat  日期时间的格式："yyyyMMddhhmm"
     * @return 返回毫秒数
     */
    public long getMillisecond(String dateAndTime,String dateAndTimeFormat){
        SimpleDateFormat sdf = new SimpleDateFormat(dateAndTimeFormat);
        Long millionSeconds = null;
        try {
            millionSeconds = sdf.parse(dateAndTime).getTime();//毫秒
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return millionSeconds;
    }

    /**
     * 获取当前时间的毫秒值
     * @return
     */
    public static long getMillisecond(){
        return new Date().getTime();
    }

    /**
     * yyyy.MM.dd G 'at' hh:mm:ss z 如 '2002-1-1 AD at 22:10:59 PSD'
     * yy/MM/dd HH:mm:ss 如 '2002/1/1 17:55:00'
     * yy/MM/dd HH:mm:ss pm 如 '2002/1/1 17:55:00 pm'
     * yy-MM-dd HH:mm:ss 如 '2002-1-1 17:55:00'
     * yy-MM-dd HH:mm:ss am 如 '2002-1-1 17:55:00 am'
     * @param pattern
     * @param dateTime
     * @return
     */
    public static String getFormatedDateTime(String pattern, long dateTime) {
        SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern);
        return sDateFormat.format(new Date(dateTime + 0));
    }


}
