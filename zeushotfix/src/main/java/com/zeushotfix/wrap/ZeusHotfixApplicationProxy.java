package com.zeushotfix.wrap;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

/**
 * Created by huangjian on 2017/2/15.
 */
public class ZeusHotfixApplicationProxy extends Application {
    Application realApplication = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        CrashHandler.getInstance().init(this);
        realApplication = HotfixLoaderUtil.attachBaseContext(this, base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        realApplication.onCreate();
    }

    @Override
    public void onConfigurationChanged(Configuration paramConfiguration) {
        super.onConfigurationChanged(paramConfiguration);
        if (realApplication != null) {
            realApplication.onConfigurationChanged(paramConfiguration);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (realApplication != null) {
            realApplication.onLowMemory();
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (realApplication != null) {
            realApplication.onTerminate();
        }
    }

    @Override
    public void onTrimMemory(int paramInt) {
        try {
            super.onTrimMemory(paramInt);
            if (realApplication != null) {
                realApplication.onTrimMemory(paramInt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
