package com.zeushotfix.wrap;

import android.content.Context;
import android.content.pm.PackageInfo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 动态代理PackageManager，主要是拦截getPackageInfo方法
 */
class PackageManagerInvocation implements InvocationHandler {
    private final Object mPackageManager;
    private Context mContext;
    int mVersionCode;
    String mVersionName;
    String nativeLibraryDir;
    String publicSourceDir;
    String sourceDir;

    public PackageManagerInvocation(Object paramObject, Context context, int versionCode, String versionName, String pathInfo) {
        mPackageManager = paramObject;
        mContext = context;
        mVersionCode = versionCode;
        mVersionName = versionName;
        nativeLibraryDir = Util.getNativeLibPath(mContext, pathInfo);
        publicSourceDir = Util.getHotfixApkPath(mContext, pathInfo);
        sourceDir = Util.getHotfixApkPath(mContext, pathInfo);
    }

    @Override
    public Object invoke(Object paramObject, Method paramMethod,
                         Object[] paramArrayOfObject) {
        Object localObject = null;
        try {
            localObject = paramMethod.invoke(mPackageManager, paramArrayOfObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ((paramMethod.getName().equals("getPackageInfo"))
                && (paramArrayOfObject[0].equals(mContext.getPackageName()))) {

            PackageInfo localPackageInfo = (PackageInfo) localObject;
            if (localPackageInfo != null) {
                localPackageInfo.versionCode = mVersionCode;
                localPackageInfo.versionName = mVersionName;
                if (localPackageInfo.applicationInfo != null) {
                    localPackageInfo.applicationInfo.nativeLibraryDir = nativeLibraryDir;
                    localPackageInfo.applicationInfo.publicSourceDir = publicSourceDir;
                    localPackageInfo.applicationInfo.sourceDir = sourceDir;
                }
            }
        }
        return localObject;
    }
}
