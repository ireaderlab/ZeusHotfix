package com.zeushotfix.inside;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;

/**
 * 补丁安装的服务
 */
public class InstallHotfixService extends IntentService {

    public static final String HOTFIX_FILE_PATH = "file_path";
    public static final String HOTFIX_INSTALL_RESULT = "install_result";
    public static final String RESULT = "result";

    public InstallHotfixService() {
        super("InstallHotfixService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String filePath = intent.getStringExtra(HOTFIX_FILE_PATH);
        boolean ret = HotfixInstallerUtil.installHotfix(InstallHotfixService.this, filePath);
        //安装结束之后通知接收者安装结果
        Intent retIntent = new Intent(HOTFIX_INSTALL_RESULT);
        retIntent.putExtra(RESULT, ret);
        retIntent.putExtra(HOTFIX_FILE_PATH, filePath);
        sendBroadcast(retIntent);
        stopSelf();
        Process.killProcess(Process.myPid());
    }

}
