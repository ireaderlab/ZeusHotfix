package com.zeushotfix.wrap;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;

/**
 * Created by huangjian on 2017/5/17.
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String CRASH_TIME = "hotfix_crash_time";      //补丁崩溃时存放崩溃时间的文件的文件名

    private static CrashHandler instance;
    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;  // 系统默认的UncaughtException处理类

    private CrashHandler() {
    }

    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static CrashHandler getInstance() {
        if (instance == null) {
            synchronized (CrashHandler.class) {
                if (instance == null) {
                    instance = new CrashHandler();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        onCrash(mContext);
        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        }
    }


    /**
     * 获取记录崩溃时间的文件的地址
     *
     * @param context context
     * @return 记录崩溃时间的文件的地址
     */
    private String getCrashTimeFilePath(Context context) {
        try {
            return context.getExternalCacheDir().getAbsolutePath() + File.separator + CRASH_TIME;
        } catch (Throwable e) {
            return Environment.getExternalStorageDirectory().getPath() + "/Android/data/" + context.getPackageName() + "/cache/" + CRASH_TIME;
        }
    }

    /**
     * 当发生崩溃时调用，防止错误的补丁导致软件无法启动
     *
     * @param context context
     */
    public void onCrash(Context context) {
        String cacheDir;
        try {
            cacheDir = context.getExternalCacheDir().getAbsolutePath();
        } catch (Throwable e) {
            cacheDir = Environment.getExternalStorageDirectory().getPath() + "/Android/data/" + context.getPackageName() + "/cache/";
        }
        if (context.getPackageCodePath().startsWith(cacheDir)) {
            long nowTime = System.currentTimeMillis();
            String crashTimeString = Util.readString(getCrashTimeFilePath(context));
            if (!TextUtils.isEmpty(crashTimeString)) {
                long crashTime = Long.valueOf(crashTimeString);
                long delTime = nowTime - crashTime;
                if (delTime > 0 && delTime < 2 * 60 * 1000) {//2分钟内再次发生崩溃
                    Util.deleteDirectorySafe(new File(Util.getInsideHotfixPath(context)));
                    Util.deleteDirectorySafe(new File(Util.getSdcardHotfixPath(context)));
                } else {
                    Util.writeString(getCrashTimeFilePath(context), String.valueOf(nowTime));
                }
            } else {
                Util.writeString(getCrashTimeFilePath(context), String.valueOf(nowTime));
            }
        }
    }
}
