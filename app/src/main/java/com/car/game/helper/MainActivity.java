package com.car.game.helper;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.app.PendingIntent;
import android.widget.Toast;
import android.os.Build;
import java.util.HashMap;

public class MainActivity extends Activity {

    private static final String ACTION_USB_PERMISSION = "com.car.game.helper.USB_PERMISSION";
    private UsbManager usbManager;
    private BroadcastReceiver usbReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        
        // 注册权限监听广播
        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_USB_PERMISSION.equals(intent.action)) {
                    synchronized (this) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            launchTargetApp();
                        } else {
                            Toast.makeText(context, "请允许权限以使手柄生效", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        };
        registerReceiver(usbReceiver, new IntentFilter(ACTION_USB_PERMISSION));

        // 开始搜索手柄
        searchUsbDevices();
    }

    private void searchUsbDevices() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice device : deviceList.values()) {
            // 判断是否为 HID 设备或特定厂商手柄
            if (device.getDeviceClass() == 3 || device.getVendorId() == 0x20D6 || device.getVendorId() == 0x045E) {
                if (!usbManager.hasPermission(device)) {
                    // 请求权限
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        flags |= PendingIntent.FLAG_MUTABLE;
                    }
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), flags);
                    usbManager.requestPermission(device, pi);
                } else {
                    launchTargetApp();
                }
                return;
            }
        }
        Toast.makeText(this, "未找到手柄，请确认已插入接收器", Toast.LENGTH_SHORT).show();
    }

    private void launchTargetApp() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.cocav.tiemu");
            if (intent != null) {
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "未找到酷咖游戏 App", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usbReceiver != null) unregisterReceiver(usbReceiver);
    }
}
