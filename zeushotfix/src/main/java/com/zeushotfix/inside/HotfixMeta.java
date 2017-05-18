package com.zeushotfix.inside;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by huangjian on 2017/5/17.
 * 该类与wrap包名下的HotfixMeta相同，不支持加固时可删除其中之一
 */
public class HotfixMeta {
    /**
     * 当前的热修复版本，只有该版本一致的宿主和补丁才可以进行静默升级
     */
    private int version;
    /**
     * apk的version code,在此添加时为了快速获取version code，系统提供的方法比较耗时
     */
    private int versionCode;
    /**
     * apk的version code,在此添加时为了快速获取version name，系统提供的方法比较耗时
     */
    private String versionName;

    public int getVersion() {
        return version;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getJsonString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("version", version);
            jsonObject.put("versionCode", versionCode);
            jsonObject.put("versionName", versionName);
            return jsonObject.toString();
        } catch (JSONException e) {

        }
        return null;
    }
}
