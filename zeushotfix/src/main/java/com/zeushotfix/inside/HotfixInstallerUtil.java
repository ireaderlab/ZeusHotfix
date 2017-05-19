package com.zeushotfix.inside;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import dalvik.system.DexClassLoader;

/**
 * Created by huangjian on 2017/2/15.
 * 补丁的安装工具类。这里只负责安装，加载由外部的壳来加载。
 * 如果需要支持加固，请将此类和其调用到的方法拷贝到被加固的代码中
 */
final public class HotfixInstallerUtil {
    /**
     * 安装补丁
     *
     * @param context context
     * @param hotfixPath 补丁地址
     * @return true,成功
     */
    public static synchronized boolean installHotfix(Context context, String hotfixPath) {
        if (!Util.fileExists(hotfixPath)) return false;
        //校验要安装的补丁和当前使用的是否支持热修复，否则应提示用户安装apk
        HotfixMeta HotfixMeta = getHotfixMeta(context, hotfixPath);
        HotfixMeta currentMeta = getCurrentHotfixMeta(context);
        //校验获取的配置信息是否正确，是否可以静默更新
        if (HotfixMeta == null ||
                currentMeta == null ||
                HotfixMeta.getVersion() < 1 ||
                HotfixMeta.getVersionCode() < 1 ||
                HotfixMeta.getVersion() < currentMeta.getVersion() ||//如果补丁的version小于当前版本则不加载补丁
                currentMeta.getVersionCode() > HotfixMeta.getVersionCode()) {//如果补丁的version code小于当前版本则不安装补丁
            return false;
        }
        //进行签名校验
        try {
            Signature[] hotfixSignature = Util.getApkSignature(hotfixPath, context);
            Signature[] currentSignature = Util.getPackageSignature(context.getPackageName(), context);
            if (!hotfixSignature[0].equals(currentSignature[0])) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //开始安装
        String pathInfo = String.valueOf(System.currentTimeMillis());
        //将补丁文件重命名到沙盒中
        int ret = Util.copy(hotfixPath, Util.getHotfixApkPath(context, pathInfo));
        //拷贝so文件
        if (ret == -1 || !copySoFile(context, pathInfo, Util.getCpuArchitecture())) {
            onInstallFailed(context, pathInfo);
            return false;
        }
        //写入配置信息
        if (!writeMeta(context, pathInfo, HotfixMeta)) {
            onInstallFailed(context, pathInfo);
            return false;
        }
        //预优化补丁dex的加载，使加载补丁时速度更快
        new DexClassLoader(Util.getHotfixApkPath(context, pathInfo),
                Util.getInsideHotfixVersionPath(context, pathInfo),
                "",
                context.getClassLoader().getParent());
        //把安装路径信息写到文件中
        if (!writePathInfo(context, pathInfo)) {
            onInstallFailed(context, pathInfo);
            return false;
        }
        //安装完成删除源文件，节省空间
        Util.delete(hotfixPath);
        return true;
    }

    /**
     * 安装失败时删除错误的补丁
     * @param context
     * @param pathInfo
     */
    private static void onInstallFailed(Context context, String pathInfo){
        Util.deleteDirectorySafe(new File(Util.getInsideHotfixPath(context) + pathInfo + File.separator));
        Util.deleteDirectorySafe(new File(Util.getSdcardHotfixDirPath(context, pathInfo)));
    }

    /**
     * 将补丁的路径信息写入文件系统
     *
     * @param context context
     * @param pathInfo pathinfo
     * @return true,成功
     */
    private static boolean writePathInfo(Context context, String pathInfo) {
        String infoPath = Util.getInsideHotfixPath(context) + Util.HOTFIX_PATHINFO_PATH;
        File file = new File(infoPath);
        FileOutputStream out = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new FileOutputStream(file);
            out.write(pathInfo.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {

                }
            }
        }
        return true;
    }

    /**
     * 把补丁的meta信息写入文件系统
     *
     * @param context
     * @param pathInfo
     * @param meta
     * @return
     */
    private static boolean writeMeta(Context context, String pathInfo, HotfixMeta meta) {
        String metaFilePath = Util.getMetaFilePath(context, pathInfo);
        File file = new File(metaFilePath);
        FileOutputStream out = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new FileOutputStream(file);
            out.write(meta.getJsonString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * 拷贝so信息
     *
     * @param context
     * @param pathInfo
     * @param cpuType
     * @return
     */
    private static boolean copySoFile(Context context, String pathInfo, int cpuType) {
        String insideLibPath = Util.getInsideHotfixPath(context) + pathInfo + File.separator;
        Util.createDir(insideLibPath);
        String apkLibPath = Util.getLibFile(cpuType);
        //首先将apk中libs文件夹下的一级so文件拷贝
        return Util.unzipFileByRegForPlug(Util.getHotfixApkPath(context, pathInfo), insideLibPath, apkLibPath, null);
    }

    /**
     * 获取补丁的配置信息
     *
     * @param context
     * @param hotfixPath
     * @return
     */
    private static HotfixMeta getHotfixMeta(Context context, String hotfixPath) {
        if (!Util.fileExists(hotfixPath)) return null;
        HotfixMeta HotfixMeta = new HotfixMeta();
        PackageInfo localPackageInfo = context.getPackageManager()
                .getPackageArchiveInfo(
                        hotfixPath,
                        PackageManager.GET_META_DATA);
        if (localPackageInfo != null) {
            HotfixMeta.setVersionCode(localPackageInfo.versionCode);
            HotfixMeta.setVersionName(localPackageInfo.versionName);
            //某些rom用此种方法获取的meta data为null
            if (localPackageInfo.applicationInfo != null && localPackageInfo.applicationInfo.metaData != null) {
                HotfixMeta.setVersion(localPackageInfo.applicationInfo.metaData.getInt(Util.HOTFIX_VERISON, 0));
            } else {
                try {
                    Class clazz = Class.forName("android.content.pm.PackageParser");
                    Object packag = null;
                    try {//5.0以上系统
                        java.lang.reflect.Method parsePackageMethod = clazz.getMethod(
                                "parsePackage", File.class, int.class);
                        Object packageParser = clazz.newInstance();
                        packag = parsePackageMethod.invoke(packageParser, new File(hotfixPath), PackageManager.GET_META_DATA);
                    } catch (Throwable e) {//5.0以下
                        java.lang.reflect.Method parsePackageMethod = clazz.getMethod(
                                "parsePackage",
                                File.class,
                                String.class,
                                DisplayMetrics.class,
                                int.class);
                        Object packageParser = clazz.getConstructor(String.class).newInstance("");
                        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
                        localDisplayMetrics.setToDefaults();
                        packag = parsePackageMethod.invoke(packageParser,
                                new File(hotfixPath),
                                null,
                                localDisplayMetrics,
                                PackageManager.GET_META_DATA);
                    }
                    if (packag != null) {
                        Bundle appMetaData = (Bundle) Util.getField(packag, "mAppMetaData");
                        if (appMetaData != null) {
                            HotfixMeta.setVersion(appMetaData.getInt(Util.HOTFIX_VERISON, 0));
                        }
                    }
                } catch (Throwable e) {

                }
            }
        }
        return HotfixMeta;
    }

    /**
     * 获取当前apk的配置信息
     *
     * @param context
     * @return
     */
    private static HotfixMeta getCurrentHotfixMeta(Context context) {
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
