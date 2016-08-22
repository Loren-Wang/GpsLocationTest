package com.gpslocationtest.later;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 创建时间：2016.8.19
 * 创建人：王亮
 * 说明：独立定位服务
 */
public class MyServiceTest extends Service {
    private LocationManager locationManager;
    private Context context;
    private long locationTimeInterval = 15000;
    private long saveTimeInterval = 10000;
    private long lastSaveRTime = 0;
    private long lastSaveCTime = 0;
    private String lastSaveState = "R";
    private String saveR = "R";
    private String saveC = "C";
    private Location lastLocationDate;
    private String TAG = "MyService";

    private static final String LOCATION_GPS = "gps";
    private static final String LOCATION_NETWORK = "network";
    private static final String LOCATION_PASSIVE = "passive";

    private static final int NETTYPE_WIFI = 1;
    private static final int NETTYPE_CMWAP = 2;
    private static final int NETTYPE_CMNET = 3;

    //向服务中发送定位时间间隔请求的传输键值标记
    public static String TAG_SEND_LOCATION_TIMEINTERVAL_TO_SERVICE = "tag_send_location_timeinterval_to_service";
    public static String LOCATION_BROADCASTRECEIVER_ACTION = "";//广播接收器的action
    private String saveLogPath = Environment.getExternalStorageDirectory().getPath() + "/GpsLocationTest/" + "log.txt";
    private String saveGpsLocationPath = Environment.getExternalStorageDirectory().getPath() + "/GpsLocationTest/" + "gps.txt";
    private String saveNetLocationPath = Environment.getExternalStorageDirectory().getPath() + "/GpsLocationTest/" + "net.txt";
    private String saveAllLocationPath = Environment.getExternalStorageDirectory().getPath() + "/GpsLocationTest/" + "manTest.txt";
    private LocationBroadcastReceiver locationBroadcastReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

        //注册广播
        locationBroadcastReceiver = new LocationBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(Integer.MAX_VALUE);
        filter.addAction(LOCATION_BROADCASTRECEIVER_ACTION);
        registerReceiver(locationBroadcastReceiver, filter);

        long nowMillisecond = getMillisecond();
        addContentToFile(saveLogPath, TAG + " {" +
                getFormatedDateTime("yyyy.MM.dd-HH:mm:ss.SSS", nowMillisecond) + "服务已启动，位置广播已注册}\r\n");

    }

    /**
     *locationManager.requestLocationUpdates(LOCATION_GPS, locationTimeInterval, 0, locationGpsListener);
     *  步骤2.1 （2）：由于人的位置是不断变化，我要设置一个位置变化的范围，包括同时满足最小的时间间隔和最小的位移变化，如果两个条件要同时满足，将位置监听器将被触发。实际上该方法有多个参数格式，特别是requestLocationUpdates (long minTime, float minDistance, Criteria criteria, PendingIntent intent)，当位置变化时可调用其他的Activity。 在本例中，我们制定用GPS，则在权限中必须要求精确定位许可。
     *  mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000010秒，为测试方便, 1000/*1公里, onLocationChange/*位置监听器);
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


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
            return START_STICKY;
        }

        //开始进行定位监听
        locationManager.requestLocationUpdates(LOCATION_GPS, locationTimeInterval, 0, locationGpsListener);
        locationManager.requestLocationUpdates(LOCATION_NETWORK, locationTimeInterval, 0, locationNetworkListener);

        thread.start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        locationManager.removeUpdates(locationGpsListener);
        locationManager.removeUpdates(locationNetworkListener);
    }

    private long lastLocationTime = 0;
    private Location locationGpsData;
    private Location locationNetData;
    private boolean isSaveLocationData = false;
    private Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {

            // Get the location manager
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            //开始进行定位监听
            locationManager.requestLocationUpdates(LOCATION_GPS, locationTimeInterval, 0, locationGpsListener);
            locationManager.requestLocationUpdates(LOCATION_NETWORK, locationTimeInterval, 0, locationNetworkListener);

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
            while (true) {
                if (getMillisecond() - lastLocationTime > locationTimeInterval) {
                    lastLocationTime = getMillisecond();
                    locationGpsData = locationManager.getLastKnownLocation(LOCATION_GPS);
                    locationNetData = locationManager.getLastKnownLocation(LOCATION_NETWORK);

                    if(locationGpsData != null && lastSaveState != null && (
                            (lastSaveState.equals(saveC) && getMillisecond() - locationGpsData.getTime() > locationTimeInterval && locationGpsData.getTime() - lastSaveCTime >= saveTimeInterval)
                                    || (lastSaveState.equals(saveR) && getMillisecond() - locationGpsData.getTime() > locationTimeInterval && locationGpsData.getTime() - lastSaveRTime >= saveTimeInterval)
                                    || (lastSaveState.equals(saveR) && getMillisecond() - locationGpsData.getTime() <= locationTimeInterval && locationGpsData.getTime() - lastSaveRTime >= saveTimeInterval)
                                    || (lastSaveState.equals(saveC) && getMillisecond() - locationGpsData.getTime() <= locationTimeInterval && (locationGpsData.getTime()) - lastSaveRTime >= saveTimeInterval ))
                            ) {
                        if (locationGpsData != null) {
                            StringBuffer buffer = new StringBuffer("");
                            long timeMillisecond = getMillisecond();
                            buffer.append("[" + getFormatedDateTime("HH:mm:ss.SSS", timeMillisecond) + "]");
                            buffer.append(" G(" + (checkGpsIsOpen() ? "1" : "0") + ")");
                            if (getMillisecond() - locationGpsData.getTime() > locationTimeInterval) {
                                buffer.append("(c) " + locationGpsData.getLongitude() + "," + locationGpsData.getLatitude());
                                lastSaveCTime = getMillisecond();
                                lastSaveState = saveC;
                            } else {
                                buffer.append("(r) " + locationGpsData.getLongitude() + "," + locationGpsData.getLatitude());
                                lastSaveRTime = getMillisecond();
                                lastSaveState = saveR;
                                lastLocationDate = new Location(locationGpsData);
                            }

                            buffer.append("\r\n");
                            addContentToFile(saveAllLocationPath, String.valueOf(buffer));
                            isSaveLocationData = true;
                        }else {
                            isSaveLocationData = false;
                        }
                    }else {
                        isSaveLocationData = false;
                    }

                    if(locationNetData != null &&lastSaveState != null && (
                            (lastSaveState.equals(saveC) && getMillisecond() - locationNetData.getTime() > locationTimeInterval && locationNetData.getTime() - lastSaveCTime >= saveTimeInterval)
                                    || (lastSaveState.equals(saveR) && getMillisecond() - locationNetData.getTime() > locationTimeInterval && locationNetData.getTime() - lastSaveRTime >= saveTimeInterval)
                                    || (lastSaveState.equals(saveR) && getMillisecond() - locationNetData.getTime() <= locationTimeInterval && locationNetData.getTime() - lastSaveRTime >= saveTimeInterval)
                                    || (lastSaveState.equals(saveC) && getMillisecond() - locationNetData.getTime() <= locationTimeInterval && (locationNetData.getTime()) - lastSaveRTime >= saveTimeInterval ))
                            ) {
                        if (locationNetData != null) {
                            StringBuffer buffer = new StringBuffer("");
                            long timeMillisecond = getMillisecond();
                            buffer.append("[" + getFormatedDateTime("HH:mm:ss.SSS", timeMillisecond) + "]");
                            buffer.append(" N(" + getNetworkType().substring(0, 1) + ")");
                            if (getMillisecond() - locationNetData.getTime() > locationTimeInterval) {
                                buffer.append("(c) " + locationNetData.getLongitude() + "," + locationNetData.getLatitude());
                                lastSaveCTime = getMillisecond();
                                lastSaveState = saveC;
                            } else {
                                buffer.append("(r) " + locationNetData.getLongitude() + "," + locationNetData.getLatitude());
                                lastSaveRTime = getMillisecond();
                                lastSaveState = saveR;
                                lastLocationDate = new Location(locationNetData);
                            }

                            buffer.append("\r\n");
                            addContentToFile(saveAllLocationPath, String.valueOf(buffer));
                            isSaveLocationData = true;
                        }else {
                            isSaveLocationData = false;
                        }
                    }else {
                        isSaveLocationData = false;
                    }

                    if(!isSaveLocationData && lastLocationDate != null){
                        StringBuffer buffer = new StringBuffer("");
                        long timeMillisecond = getMillisecond();
                        buffer.append("[" + getFormatedDateTime("HH:mm:ss.SSS", timeMillisecond) + "]");
                        if(lastLocationDate.getProvider().equals(LOCATION_GPS)){
                            buffer.append(" G(" + (checkGpsIsOpen() ? "1" : "0") + ")");
                            if (getMillisecond() - lastLocationDate.getTime() > locationTimeInterval) {
                                buffer.append("(c) " + lastLocationDate.getLongitude() + "," + lastLocationDate.getLatitude());
                            } else {
                                buffer.append("(r) " + lastLocationDate.getLongitude() + "," + lastLocationDate.getLatitude());
                            }
                            buffer.append("\r\n");
                            addContentToFile(saveAllLocationPath, String.valueOf(buffer));
                        }else if(lastLocationDate.getProvider().equals(LOCATION_NETWORK)) {
                            buffer.append(" N(" + getNetworkType().substring(0, 1) + ")");
                            if (getMillisecond() - lastLocationDate.getTime() > locationTimeInterval) {
                                buffer.append("(c) " + lastLocationDate.getLongitude() + "," + lastLocationDate.getLatitude());
                            } else {
                                buffer.append("(r) " + lastLocationDate.getLongitude() + "," + lastLocationDate.getLatitude());
                            }
                            buffer.append("\r\n");
                            addContentToFile(saveAllLocationPath, String.valueOf(buffer));
                        }
                    }
                }
            }
        }
    });

    /**
     * 定位广播接收器，用来接收activity传递过来的值以修改service中的参数
     */
    private class LocationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
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

            long nowMillisecond = getMillisecond();
            StringBuffer logContent = new StringBuffer(TAG + " {" + getFormatedDateTime("yyyy.MM.dd-HH:mm:ss.SSS", nowMillisecond));
            if (intent != null) {
                locationTimeInterval = intent.getLongExtra(TAG_SEND_LOCATION_TIMEINTERVAL_TO_SERVICE, locationTimeInterval);
                if(locationTimeInterval < 10000){
                    logContent.append("接收到请求定位间隔时间设置，定位时间间隔周期小于10000mm，设置失败\r\n");
                }else {
                    logContent.append("接收到请求定位间隔时间设置，定位间隔时间：" + locationTimeInterval + "ms\r\n");
                    locationManager.requestLocationUpdates(LOCATION_GPS, locationTimeInterval, 0, locationGpsListener);
                    locationManager.requestLocationUpdates(LOCATION_NETWORK, locationTimeInterval, 0,locationNetworkListener);
                }
            } else {
                logContent.append("接收到请求定位间隔时间设置，但是传输错误，设置失败\r\n");
            }
            addContentToFile(saveLogPath, String.valueOf(logContent));
        }
    }

    /**
     * gps定位监听
     * public void onLocationChanged(Location loc) {
     }
     // provider 被用户关闭后调用
     public void onProviderDisabled(String p){

     }
     // provider 被用户开启后调用
     public void onProviderEnabled(String p){

     }
     // provider 状态变化时调用
     public void onStatusChanged(String provider, int status,Bundle extras){ }
     */
    private LocationListener locationGpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location locationGps) {
//            if(lastSaveState != null && (
//                    (lastSaveState.equals(saveC) && getMillisecond() - locationGps.getTime() > locationTimeInterval && locationGps.getTime() - lastSaveCTime >= saveTimeInterval)
//                    || (lastSaveState.equals(saveR) && getMillisecond() - locationGps.getTime() > locationTimeInterval && locationGps.getTime() - lastSaveRTime >= saveTimeInterval)
//                    || (lastSaveState.equals(saveR) && getMillisecond() - locationGps.getTime() <= locationTimeInterval && locationGps.getTime() - lastSaveRTime >= saveTimeInterval)
//                    || (lastSaveState.equals(saveC) && getMillisecond() - locationGps.getTime() <= locationTimeInterval && (locationGps.getTime()) - lastSaveRTime >= saveTimeInterval ))
//            ) {
//                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                        && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                    return;
//                }
//
//                StringBuffer buffer = new StringBuffer("");
//                long timeMillisecond = getMillisecond();
//
//                buffer.append("[" + getFormatedDateTime("HH:mm:ss.SSS", timeMillisecond) + "]");
////            StringBuffer netBuffer = new StringBuffer(buffer);
//
//                buffer.append(" G(" + (checkGpsIsOpen() ? "1" : "0") + ")");
////            netBuffer.append(" N(" + getNetworkType().substring(0,1) + ")");
//
//
//                if (locationGps != null) {
//                    if (getMillisecond() - locationGps.getTime() > locationTimeInterval) {
//                        buffer.append("(c) " + locationGps.getLongitude() + "," + locationGps.getLatitude());
//                    } else {
//                        buffer.append("(r) " + locationGps.getLongitude() + "," + locationGps.getLatitude());
//
//                    }
//                } else {
//                    buffer.append(" null");
//                }
//
////            Location locationNet = locationManager.getLastKnownLocation(LOCATION_NETWORK);
////            if (locationNet != null) {
////                if (getMillisecond() - locationNet.getTime() > locationTimeInterval) {
////                    netBuffer.append("(c) " + locationNet.getLongitude() + "," + locationNet.getLatitude());
////                } else {
////                    netBuffer.append("(r) " + locationNet.getLongitude() + "," + locationNet.getLatitude());
////                }
////            } else {
////                netBuffer.append(" null");
////            }
//
//                buffer.append("\r\n");
////            netBuffer.append("\r\n");
//
//                addContentToFile(saveAllLocationPath, String.valueOf(buffer));
////            addContentToFile(saveAllLocationPath, String.valueOf(netBuffer));
//            }
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
    };

    /**
     * 网络定位监听
     * public void onLocationChanged(Location loc) {
     }
     // provider 被用户关闭后调用
     public void onProviderDisabled(String p){

     }
     // provider 被用户开启后调用
     public void onProviderEnabled(String p){

     }
     // provider 状态变化时调用
     public void onStatusChanged(String provider, int status,Bundle extras){ }
     */
    private LocationListener locationNetworkListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location locationNet) {
//            if(lastSaveState != null && (
//                    (lastSaveState.equals(saveC) && getMillisecond() - locationNet.getTime() > locationTimeInterval && locationNet.getTime() - lastSaveCTime >= saveTimeInterval)
//                            || (lastSaveState.equals(saveR) && getMillisecond() - locationNet.getTime() > locationTimeInterval && locationNet.getTime() - lastSaveRTime >= saveTimeInterval)
//                            || (lastSaveState.equals(saveR) && getMillisecond() - locationNet.getTime() <= locationTimeInterval && locationNet.getTime() - lastSaveRTime >= saveTimeInterval)
//                            || (lastSaveState.equals(saveC) && getMillisecond() - locationNet.getTime() <= locationTimeInterval && (locationNet.getTime()) - lastSaveRTime >= saveTimeInterval ))
//                    ) {
//                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                        && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                    return;
//                }
//
//                StringBuffer buffer = new StringBuffer("");
//                long timeMillisecond = getMillisecond();
//
//                buffer.append("[" + getFormatedDateTime("HH:mm:ss.SSS", timeMillisecond) + "]");
////            StringBuffer gpsBuffer = new StringBuffer(buffer);
//
////            gpsBuffer.append(" G(" + (checkGpsIsOpen() ? "1" : "0") + ")");
//                buffer.append(" N(" + getNetworkType().substring(0, 1) + ")");
//
//
////            Location locationGps = locationManager.getLastKnownLocation(LOCATION_GPS);
////            if (locationGps != null) {
////                if (getMillisecond() - locationGps.getTime() > locationTimeInterval) {
////                    gpsBuffer.append("(c) " + locationGps.getLongitude() + "," + locationGps.getLatitude());
////                } else {
////                    gpsBuffer.append("(r) " + locationGps.getLongitude() + "," + locationGps.getLatitude());
////                }
////            } else {
////                gpsBuffer.append(" null");
////            }
//
//
//                if (locationNet != null) {
//                    if (getMillisecond() - locationNet.getTime() > locationTimeInterval) {
//                        buffer.append("(c) " + locationNet.getLongitude() + "," + locationNet.getLatitude());
//                    } else {
//                        buffer.append("(r) " + locationNet.getLongitude() + "," + locationNet.getLatitude());
//                    }
//                } else {
//                    buffer.append(" null");
//                }
//
//                buffer.append("\r\n");
////            gpsBuffer.append("\r\n");
//
////            addContentToFile(saveAllLocationPath, String.valueOf(gpsBuffer));
//                addContentToFile(saveAllLocationPath, String.valueOf(buffer));
//            }
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
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * 获取当前时间的毫秒值
     * @return
     */
    private long getMillisecond(){
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
    private String getFormatedDateTime(String pattern, long dateTime) {
        SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern);
        return sDateFormat.format(new Date(dateTime + 0));
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
     * 获取当前网络类型
     * @return 0：没有网络   1：WIFI网络   2：WAP网络    3：NET网络
     */

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

    synchronized private void addContentToFile(String path, String content) {
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
    private void checkDirPathIsExistAndBuild(String path) {
        checkDirPathIsExistAndBuild(new File(path));
    }

    /**
     * 检查文件夹是否存在并创建文件夹
     *
     * @param file
     */
    private void checkDirPathIsExistAndBuild(File file) {
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

}
