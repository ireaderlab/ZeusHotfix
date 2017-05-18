package com.zeushotfix.wrap;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Process;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;

/**
 * 补丁管理类，主要负责加载版本管理
 * 如果软件被加固，请将除了InstallHotfixService之外的其他代码放到不加固的包名下
 * 由此框架来加载加固的壳
 * Created by huangjian on 2017/2/15.
 */
final class HotfixLoaderUtil {
    private static final String HOTFIX_NEW_PATHINFO_PATH = "newPathinfo";           //主进程正在使用的安装信息的存储文件名
    private static final String HOTFIX_OLD_PATHINFO_PATH = "oldPathinfo";           //非主进程正在使用的安装信息的存储文件名
    private static final String APPLICATION_NAME = "android.app.Application";       //加固的application,第三方使用需要修改
    private static final String APPLICATION_NAME_DEBUG = "android.app.Application"; //原始apk的application，用来普通调试用的,第三方使用需要修改

    /**
     * 初始化补丁框架
     *
     * @param applicationProxy
     * @param context
     * @return true表明加载了补丁框架， false表明没有加载补丁框架
     */
    protected static Application attachBaseContext(Application applicationProxy, final Context context) {
        Util.createDir(Util.getInsideHotfixPath(context));
        Util.createDir(Util.getSdcardHotfixPath(context));
        //判断是否是主进程
        boolean isMainProcess = true;
        if (!context.getPackageName().equals(getCurProcessName(context))) {
            isMainProcess = false;
        }
        final String pathInfo = readPathInfo(context, isMainProcess);
        //当前Resouces指向的apk为补丁文件夹即当前已经加载过补丁了，防止认为多次调用，或者如果补丁apk不存在，直接退出
        if (Util.isEmpty(pathInfo) ||
                context.getPackageResourcePath().equals(Util.getHotfixApkPath(context, pathInfo)) ||
                !Util.fileExists(Util.getHotfixApkPath(context, pathInfo))) {
            return loadApplication(applicationProxy, context);
        }
        HotfixMeta HotfixMeta = getHotfixMeta(context, pathInfo);
        HotfixMeta currentMeta = getCurrentApkMeta(context);

        //校验配置信息是否支持加载补丁
        if (HotfixMeta == null ||
                currentMeta == null ||
                HotfixMeta.getVersion() < 1 ||
                HotfixMeta.getVersionCode() < 1 ||
                HotfixMeta.getVersion() != currentMeta.getVersion() ||//如果补丁的version code小于当前版本则不加载补丁
                currentMeta.getVersionCode() > HotfixMeta.getVersionCode()) {
            //补丁文件损坏或者补丁版本比宿主还低，这时清理清理补丁
            if (isMainProcess) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //清空补丁目录下所有文件和文件夹
                        Util.deleteFilesInDirectory(Util.getSdcardHotfixPath(context));
                        Util.deleteFilesInDirectory(Util.getInsideHotfixPath(context));
                    }
                }).start();
            }
            return loadApplication(applicationProxy, context);
        }
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        Object packageInfo = Util.getField(context, "mPackageInfo");
        try {
            //设置当前的ClassLoader为补丁apk创建的ClassLoader
            DexClassLoader dexClassLoader = new DexClassLoader(Util.getHotfixApkPath(context, pathInfo),
                    Util.getInsideHotfixVersionPath(context, pathInfo),
                    Util.getNativeLibPath(context, pathInfo),
                    context.getClassLoader().getParent());
            Util.setField(packageInfo, "mClassLoader", dexClassLoader);
            // 设置mResDir为补丁apk的路径，如果mResources为null，则系统会使用该地址创建一个Resources
            Util.setField(packageInfo, "mResDir", Util.getHotfixApkPath(context, pathInfo));
            // 设置mAppDir为补丁apk的路径，它影响context.getPackageCodePath()结果
            Util.setField(packageInfo, "mAppDir", Util.getHotfixApkPath(context, pathInfo));
            //设置mLibDir影响存放so文件路径地址
            Util.setField(packageInfo, "mLibDir", Util.getNativeLibPath(context, pathInfo));
            //设置mResources为null，以便系统使用之前设置的mResDir创建新的Resources
            Util.setField(packageInfo, "mResources", null);
            applicationInfo.sourceDir = Util.getHotfixApkPath(context, pathInfo);
            applicationInfo.publicSourceDir = Util.getHotfixApkPath(context, pathInfo);
            applicationInfo.nativeLibraryDir = Util.getNativeLibPath(context, pathInfo);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // 获取mActivityThread
        Object activityThread = Util.getField(packageInfo, "mActivityThread");
        // 设置mResources参数
        Class<? extends Object> packInfoClass = packageInfo.getClass();
        Class[] arrayOfClass = new Class[1];
        arrayOfClass[0] = activityThread.getClass();

        // 让系统自己设置Resources，该Resources为补丁apk的Resources
        try {
            packInfoClass.getDeclaredMethod(
                    "getResources",
                    arrayOfClass).invoke(packageInfo,
                    new Object[]{activityThread});
        } catch (Exception e) {
        }
        String versionName = HotfixMeta.getVersionName();
        int versionCode = HotfixMeta.getVersionCode();
        // 获取sPackageManager
        Object packageManager;
        try {
            packageManager = activityThread.getClass()
                    .getMethod("getPackageManager", new Class[0])
                    .invoke(null, new Object[0]);
            //因为packageManager是IBinder接口对象，这里可以设置动态代理
            Object packageManagerPoxy = Proxy.newProxyInstance(
                    applicationProxy.getClass().getClassLoader(),
                    packageManager.getClass().getInterfaces(),
                    new PackageManagerInvocation(packageManager, context, versionCode, versionName, pathInfo));
            Util.setField(activityThread, "sPackageManager", packageManagerPoxy);
            Util.setField(applicationProxy.getPackageManager(), "mPM", packageManagerPoxy);
        } catch (Exception exception1) {
            exception1.printStackTrace();
        }
        final boolean finalIsMainProcess = isMainProcess;
        new Thread(new Runnable() {
            @Override
            public void run() {
                //降低线程优先级防止阻塞UI线程
                Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
                //进行签名校验，校验失败则杀死进程
                try {
                    Signature[] hotfixSignature = Util.getApkSignature(Util.getHotfixApkPath(context, pathInfo), context);
                    Signature[] currentSignature = Util.getPackageSignature(context.getPackageName(), context);
                    if (!hotfixSignature[0].equals(currentSignature[0])) {
                        Process.killProcess(Process.myPid());
                    }
                } catch (Exception e) {
                    Process.killProcess(Process.myPid());
                }
                if (finalIsMainProcess) {
                    //记录主进程当前使用补丁版本
                    Util.writeString(Util.getOtherPathInfoPath(context, HOTFIX_NEW_PATHINFO_PATH), pathInfo);
                    //如果当前加载的补丁版本与其他进程的补丁版本不一致，则重启其他进程
                    if (Util.fileExists(Util.getOtherPathInfoPath(context, HOTFIX_OLD_PATHINFO_PATH))) {
                        String oldPathInfo = Util.readString(Util.getOtherPathInfoPath(context, HOTFIX_OLD_PATHINFO_PATH));
                        if (Util.isEmpty(oldPathInfo) && !pathInfo.equals(oldPathInfo)) {
                            Util.resetPackagerOtherProcess(context);
                        }
                    }
                    //主进程则清除其他无效的补丁以释放存储空间
                    clearOldHotfix(context, pathInfo);
                } else {
                    //记录非主进程使用的版本，这个值这个时候是等于主进程加载的补丁的版本
                    //如果主进程使用新版本补丁之后就会读这个值，如果不一致就会重启非主进程
                    Util.writeString(Util.getOtherPathInfoPath(context, HOTFIX_OLD_PATHINFO_PATH), pathInfo);
                }
            }
        }).start();
        return loadApplication(applicationProxy, context);
    }

    /**
     * 加载真实的application
     *
     * @param applicationProxy
     * @param context
     * @return
     */
    protected static Application loadApplication(Application applicationProxy, Context context) {
        Application realApplication = loadApplication(applicationProxy, context, APPLICATION_NAME);
        if (realApplication == null) {
            realApplication = loadApplication(applicationProxy, context, APPLICATION_NAME_DEBUG);
        }
        return realApplication;
    }

    /**
     * 加载真实的application
     *
     * @param applicationProxy 当前代理的application
     * @param context
     * @param applicationName  真实的application的完整名字，包括包名
     * @return
     */
    private static Application loadApplication(Application applicationProxy, final Context context, String applicationName) {
        Application delegateApplication;
        ArrayList<Application> allApplications;
        Class<?> delegateClass;
        try {
            delegateClass = context.getClassLoader().loadClass(applicationName);
            delegateApplication = (Application) delegateClass.newInstance();
        } catch (Exception e1) {
            return null;
        }

        Object mPackageInfo = Util.getField(context, "mPackageInfo");
        // 获取mActivityThread
        Object mActivityThread = Util.getField(mPackageInfo, "mActivityThread");
        Util.setField(context, "mOuterContext", delegateApplication);
        Util.setField(mPackageInfo, "mApplication", delegateApplication);
        Util.setField(mActivityThread, "mInitialApplication", delegateApplication);
        allApplications = (ArrayList<Application>) Util.getField(mActivityThread, "mAllApplications");
        if (allApplications != null) {
            for (int i = 0; i < allApplications.size(); i++) {
                if (allApplications.get(i) == applicationProxy) {
                    allApplications.set(i, delegateApplication);
                }
            }
        }
        try {
            Method attach = Application.class.getDeclaredMethod("attach", new Class[]{Context.class});
            attach.setAccessible(true);
            attach.invoke(delegateApplication, new Object[]{context});
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return delegateApplication;
    }

    /**
     * 清除老版本的补丁
     *
     * @param context
     */
    private static void clearOldHotfix(Context context, String pathInfo) {
        if (TextUtils.isEmpty(pathInfo)) return;
        File pluginDir = new File(Util.getSdcardHotfixPath(context));
        if (pluginDir.exists() && pluginDir.isDirectory()) {
            File[] list = pluginDir.listFiles();
            if (list == null) return;
            for (File f : list) {
                String fileFullName = f.getName();
                if (!fileFullName.equalsIgnoreCase(pathInfo)) {
                    Util.deleteDirectorySafe(f);
                    File cacheFile = new File(Util.getInsideHotfixVersionPath(context, fileFullName));
                    if (cacheFile.exists()) {
                        cacheFile.delete();
                    }
                }

            }
        }
    }

    /**
     * 获取当前进程的进程名
     *
     * @param context
     * @return
     */
    private static String getCurProcessName(Context context) {
        int pid = Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    /**
     * 读取补丁的安装路径信息
     *
     * @param context
     * @param isMainProcess 是否是主进程
     * @return
     */
    private static String readPathInfo(Context context, boolean isMainProcess) {
        String result = null;
        String path;
        if (!isMainProcess && Util.fileExists(Util.getOtherPathInfoPath(context, HOTFIX_NEW_PATHINFO_PATH))) {//如果是主进程则读取最后一次安装的补丁
            path = Util.getOtherPathInfoPath(context, HOTFIX_NEW_PATHINFO_PATH);
        } else {//如果不是主进程则读取上次主进程加载的补丁
            path = Util.getPathInfoPath(context);
        }
        if (!Util.fileExists(path)) {
            return result;
        }
        result = Util.readString(path);
        return result;
    }

    /**
     * 获取某个版本补丁的配置信息
     *
     * @param context
     * @param pathInfo
     * @return
     */
    private static HotfixMeta getHotfixMeta(Context context, String pathInfo) {
        HotfixMeta result = null;
        if (!Util.fileExists(Util.getMetaFilePath(context, pathInfo))) {
            return result;
        }
        String metaString = Util.readString(Util.getMetaFilePath(context, pathInfo));
        result = convertToHotfixMeta(metaString);
        return result;
    }

    /**
     * 把字符串转换为配置信息类
     *
     * @param meta
     * @return
     */
    private static HotfixMeta convertToHotfixMeta(String meta) {
        try {
            JSONObject json = new JSONObject(meta);
            int verion = json.optInt("version", -1);
            int versionCode = json.optInt("versionCode", -1);
            String versionName = json.optString("versionName", "");
            HotfixMeta HotfixMeta = new HotfixMeta();
            HotfixMeta.setVersion(verion);
            HotfixMeta.setVersionCode(versionCode);
            HotfixMeta.setVersionName(versionName);
            return HotfixMeta;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取当前宿主的配置信息
     *
     * @param context
     * @return
     */
    private static HotfixMeta getCurrentApkMeta(Context context) {
        HotfixMeta HotfixMeta = null;
        try {
            PackageInfo localPackageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            if (localPackageInfo != null && localPackageInfo.applicationInfo != null && localPackageInfo.applicationInfo.metaData != null) {
                HotfixMeta = new HotfixMeta();
                HotfixMeta.setVersion(localPackageInfo.applicationInfo.metaData.getInt(Util.HOTFIX_VERISON, 0));
                HotfixMeta.setVersionCode(localPackageInfo.versionCode);
                HotfixMeta.setVersionName(localPackageInfo.versionName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return HotfixMeta;
    }
}
