//package com.example.gps_gyro;
///*
// * 本工程GPSLocation的功能是使用GPS实时定位,实时显示手机的经纬度
// */
////import introduction.android.gpsLocation.R;
//
//import android.Manifest;
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.location.LocationListener;
//import android.location.LocationManager;  //
//import android.location.Location;   //
//import android.os.Bundle;
//import android.provider.Settings;
//import android.util.Log;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.core.app.ActivityCompat;
//
//public class LocationActivity extends Activity {
//    private Button btn_listen;
//    private TextView tv_01, tv_02;
//    LocationManager lm;  //
//    Location loc;  //
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_location);
//
//        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);  //
//        //检测GPS状态（是否开启）
//        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {  //若未打开GPS
//            Toast.makeText(LocationActivity.this, "请开启GPS服务", Toast.LENGTH_LONG).show();
//            Intent myintent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            startActivity(myintent);  //运行手机的设置程序
//        }
//
//        btn_listen = (Button) findViewById(R.id.btn_listen);
//        tv_01 = (TextView) findViewById(R.id.tv_01);
//        tv_02 = (TextView) findViewById(R.id.tv_02);
//
//        btn_listen.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try {
//                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new MyLocationListener());
//                } catch (SecurityException unlikely) {
//                    Log.e("error loction", "Lost location permission. Could not request updates. " +
//                            unlikely);
//                }
//
//            }
//        });
//    }
//    //位置监听器方法
//    class MyLocationListener implements LocationListener{  //位置监听器，作为方法参数
//        @Override
//        public void onLocationChanged(Location loc) {
//            // TODO Auto-generated method stub
//            tv_01.setText("经度："+loc.getLongitude());
//            tv_02.setText("纬度："+loc.getLatitude());
//        }
//        @Override
//        public void onProviderDisabled(String provider) {
//            //当provider被用户关闭时调用
//            Log.i("GpsLocation","provider被关闭！");
//        }
//        @Override
//        public void onProviderEnabled(String provider) {
//            //当provider被用户开启后调用
//            Log.i("GpsLocation","provider被开启！");
//        }
//        @Override
//        public void onStatusChanged(String provider, int status, Bundle extras) {
//            //当provider的状态在OUT_OF_SERVICE、TEMPORARILY_UNAVAILABLE和AVAILABLE之间发生变化时调用
//            Log.i("GpsLocation","provider状态发生改变！");
//        }
//    }
//}

package com.example.gps_gyro;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.os.Handler;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
//implements View.OnClickListener,
public class LocationActivity extends AppCompatActivity implements SensorEventListener, CompoundButton.OnCheckedChangeListener {


    /*
    页面渲染
     */
    public TextView tv_lat, tv_lon, tv_altitude, tv_accuracy,
            tv_speed, tv_address, tv_provider, tv_accx,
            tv_accy, tv_accz, tv_gyrox, tv_gyroy, tv_gyroz;
    private TextView mMagneticField;
    public Switch sw_dataupdate, sw_acc;

    //传感器
    private SensorManager AccSensorManager, GyroSensorManager, FieldSensorManager;
    private LocationManager lm;

    /*
    网络发送
     */
    //编码
    static String charset = "GBK";
    //定时器
    private Handler handler;
    private Runnable runnable;

    /*
    传感器数据
     */
    float[] SensorsData = new float[3*3];

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gps_layout);
        //textView
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_speed = findViewById(R.id.tv_speed);
        tv_address = findViewById(R.id.tv_address);
        tv_provider = findViewById(R.id.tv_provider);
        sw_dataupdate = findViewById(R.id.sw_dataupdate);
        sw_acc = findViewById(R.id.sw_Accelerometer);
        tv_accx = findViewById(R.id.tv_accx);
        tv_accy = findViewById(R.id.tv_accy);
        tv_accz = findViewById(R.id.tv_accz);
        tv_gyrox = findViewById(R.id.tv_gyrox);
        tv_gyroy = findViewById(R.id.tv_gyroy);
        tv_gyroz = findViewById(R.id.tv_gyroz);
        mMagneticField = findViewById(R.id.txtOne);

        //GPS
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!isGpsAble(lm)) {
            Toast.makeText(LocationActivity.this, "请打开GPS", Toast.LENGTH_SHORT).show();
            openGPS2();
        }
        if (ContextCompat.checkSelfPermission(LocationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {//未开启定位权限
            //开启定位权限,200是标识码
            ActivityCompat.requestPermissions(LocationActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
        } else {
            startLocation();
            Toast.makeText(LocationActivity.this, "已开启定位权限", Toast.LENGTH_LONG).show();
        }
        //线性加速度计
        AccSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sw_acc.setOnCheckedChangeListener(this);
        //陀螺仪
        GyroSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //地磁场
        FieldSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //网络发送定时器
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                //todo what you want
                Network.SendMagTCP(LocationActivity.this, SensorsData);
                handler.postDelayed(runnable, 1000);
            }
        };
        sw_dataupdate.setOnCheckedChangeListener(this);
    }

    protected void onPause()
    {
        super.onPause();
        AccSensorManager.unregisterListener(this);
    }

    protected void onResume()
    {
        super.onResume();
    }
    protected void onStop()
    {
        super.onStop();
        AccSensorManager.unregisterListener(this);

    }

    //菜单显示
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.location_activity, menu);
        return true;
    }
    //菜单点击任务绑定
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //case判断建议常量，google建议用if代替
        int item_id = item.getItemId();
        if(item_id == R.id.set_IpPort){
            Intent intent = new Intent(LocationActivity.this, SetBasicData.class);
            startActivity(intent);
        }
        else if(item_id == R.id.remove_item){
            Toast.makeText(this, "Nothing happen", Toast.LENGTH_SHORT).show();
        }
        return true;
    }
    //传感器
    @SuppressLint("SetTextI18n")
    public void onSensorChanged(SensorEvent event)
    {
        if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER)
        {
            float[] values=event.values;
            SensorsData[0] = values[0];
            SensorsData[1] = values[1];
            SensorsData[2] = values[2];
            tv_accx.setText(Float.toString(values[0]));
            tv_accy.setText(Float.toString(values[1]));
            tv_accz.setText(Float.toString(values[2]));

        }
        else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE)
        {
            float[] values=event.values;
            SensorsData[3] = values[0];
            SensorsData[4] = values[1];
            SensorsData[5] = values[2];
            tv_gyrox.setText(Float.toString(values[0]));
            tv_gyroy.setText(Float.toString(values[1]));
            tv_gyroz.setText(Float.toString(values[2]));
        }
        else if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD)
        {
            float x=event.values[0];
            float y=event.values[1];
            float z=event.values[2];
            SensorsData[6] = x;
            SensorsData[7] = y;
            SensorsData[8] = z;
            StringBuffer buffer = new StringBuffer();
            buffer.append("X方向的磁场为：").append(String.format("%.2f", x)).append("\n");
            buffer.append("Y方向的磁场为：").append(String.format("%.2f", y)).append("\n");
            buffer.append("Z方向的磁场为：").append(String.format("%.2f", z)).append("\n");
            mMagneticField.setText(buffer);
        }
    }
    public void onAccuracyChanged(Sensor sensor,int accuracy)
    {//不用处理，空着就行
        return;
    }

    //button
//    public void onClick(View v)
//    {
//        Toast.makeText(LocationActivity.this, "按下按钮", Toast.LENGTH_SHORT).show();
//        if(v.getId()==R.id.start_acc_button)
//        {
//            Toast.makeText(LocationActivity.this, "注册监听", Toast.LENGTH_SHORT).show();
//            AccSensorManager.unregisterListener(this,AccSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
//            AccSensorManager.registerListener(this,
//                    AccSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//                    SensorManager.SENSOR_DELAY_NORMAL);
//        }
//        if(v.getId()==R.id.start_acc_button)
//        {
//            AccSensorManager.unregisterListener(this);
//        }
//    }
    //switch

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
        // TODO Auto-generated method stub
        //开启传感器
        if(arg0==sw_acc){
            Toast.makeText(this, "Monitored switch is " + (arg1 ? "on" : "off"),
                    Toast.LENGTH_SHORT).show();
            if(arg1){
                //加速度计
                AccSensorManager.unregisterListener(this,AccSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
                AccSensorManager.registerListener(this,
                        AccSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL);
                //陀螺仪
                GyroSensorManager.unregisterListener(this,GyroSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
                GyroSensorManager.registerListener(this,
                        GyroSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                        SensorManager.SENSOR_DELAY_NORMAL);
                //地磁场
                FieldSensorManager.unregisterListener(this,FieldSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
                FieldSensorManager.registerListener(this,
                        FieldSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
            else{
                AccSensorManager.unregisterListener(this);
            }
        }
        //开始传输
        else if(arg0 == sw_dataupdate) {
            if(arg1) {
                handler.postDelayed(runnable, 1000);//触发定时器
            }
            else{
                handler.removeCallbacks(runnable);
            }
        }


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 200://刚才的识别码
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){//用户同意权限,执行我们的操作
                    startLocation();//开始定位
                }else{//用户拒绝之后,当然我们也可以弹出一个窗口,直接跳转到系统设置页面
                    Toast.makeText(LocationActivity.this,"未开启定位权限,请手动到设置去开启权限",Toast.LENGTH_LONG).show();
                }
                break;
            default:break;
        }
    }

    //定义一个更新显示的方法
    @SuppressLint("SetTextI18n")
    private void updateShow(Location location) {
        if (location != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("当前的位置信息：\n");
            double a = location.getLongitude();
//            if(a<=0){
//                a = location.getLongitude()*-1;
//            }
            tv_lon.setText(String.valueOf(a));
            tv_lat.setText(String.valueOf(location.getLatitude()));
            tv_altitude.setText(String.valueOf(location.getAltitude()));
            tv_speed.setText(String.valueOf(location.getSpeed()));
            tv_provider.setText(String.valueOf(location.getProvider()));
            tv_accuracy.setText(String.valueOf(location.getAccuracy()));
        }
    }

    @SuppressLint("MissingPermission")
    private  void startLocation(){
        //从GPS获取最近的定位信息
        Location lc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        updateShow(lc);
        //设置间隔两秒获得一次GPS定位信息
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 8, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // 当GPS定位信息发生改变时，更新定位
                updateShow(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
                // 当GPS LocationProvider可用时，更新定位
                updateShow(lm.getLastKnownLocation(provider));
            }

            @Override
            public void onProviderDisabled(String provider) {
                updateShow(null);
            }
        });
    }

    private boolean isGpsAble(LocationManager lm) {
        return lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ? true : false;
    }

    //打开设置页面让用户自己设置
    private void openGPS2() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, 0);
    }
}

class Network {
    static boolean TCPConect = false;
    static TCPClient[] mThread = {null};
    static String IP, PortStr;
    static int Port;
    public static void SendMagTCP(Context context, float[] data){
        if(TCPConect)
        {
            mThread[0].setMsg(String.valueOf(data[0]));
            mThread[0].sendSocket();
        }
        else
        {
            IP = SPUtils.get(context, "ip", "192.168.0.1").toString();;
            PortStr = SPUtils.get(context, "port", "7850").toString();;
            Port = Integer.valueOf(PortStr);
            mThread[0] = new TCPClient(IP, Port, String.valueOf(data[0])+"\n");
            mThread[0].sendSocket();
            TCPConect = true;
        }
    }

}
