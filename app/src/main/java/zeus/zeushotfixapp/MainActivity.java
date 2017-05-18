package zeus.zeushotfixapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Process;
import android.widget.Toast;

import com.zeushotfix.inside.InstallHotfixService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(new Runnable() {
            @Override
            public void run() {
                copyHotfixApkAndInstall();
            }
        }).start();
    }

    /**
     * 把asset目录下的新的apk拷贝到sd卡上
     */
    private void copyHotfixApkAndInstall() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "正在安装修复包", Toast.LENGTH_SHORT).show();
            }
        });
        FileOutputStream out = null;
        InputStream in = null;
        File hotfixApkFile = new File(getExternalCacheDir(), "hotfix.jar");
        //如果sd卡上的新的apk不存在则从asset目录中拷贝
        if (!hotfixApkFile.exists()) {
            try {
                AssetManager am = getResources().getAssets();
                in = am.open("hotfix.apk");
                out = new FileOutputStream(hotfixApkFile, false);
                byte[] temp = new byte[2048];
                int len;
                while ((len = in.read(temp)) > 0) {
                    out.write(temp, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        //调用安装service进行补丁安装
        Intent intent = new Intent(this, InstallHotfixService.class);
        intent.putExtra(InstallHotfixService.HOTFIX_FILE_PATH, hotfixApkFile.getAbsolutePath());
        startService(intent);

        //注册补丁安装完成的接受Receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InstallHotfixService.HOTFIX_INSTALL_RESULT);
        registerReceiver(new RecieveHotfixResult(), intentFilter);
    }

    public static class RecieveHotfixResult extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (InstallHotfixService.HOTFIX_INSTALL_RESULT.equals(action)) {
                String filePath = intent.getStringExtra(InstallHotfixService.HOTFIX_FILE_PATH);
                boolean result = intent.getBooleanExtra(InstallHotfixService.RESULT, false);
                if (result) {
                    Toast.makeText(context, "路径:" + filePath + ",修复包安装" + (result ? "成功" : "失败"), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Process.killProcess(Process.myPid());
    }
}
