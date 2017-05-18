package com.zeushotfix.wrap;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by huangjian on 2017/2/17.
 * 该类与inside包名下的Util相同，不支持加固时可删除其中之一
 */
class Util {

    public static final int BUF_SIZE = 8192;
    public static final int CPU_AMR = 1;
    public static final int CPU_X86 = 2;
    public static final int CPU_MIPS = 3;
    public static final String HOTFIX_APK_FILE_NAME = "hotfix.jar";                //补丁文件的后缀
    public static final String HOTFIX_PATHINFO_PATH = "pathinfo";                  //补丁安装信息的文件名
    public static final String HOTFIX_VERISON = "HOTFIX_VERSION";      //补丁的静默更新版本，当此版本不一致时，则不支持静默更新

    /**
     * 补丁文件夹的地址，目前放在缓存目录中
     *
     * @param context
     * @return
     */
    public static String getSdcardHotfixPath(Context context) {
        try {
            return context.getExternalCacheDir().getAbsolutePath() + File.separator + "hotfix/";
        } catch (Throwable e) {
            return "/sdcard/Android/data/" + context.getPackageName() + "/cache/hotfix/";
        }
    }

    /**
     * 获取补丁在sd卡上的沙盒目录
     *
     * @param context
     * @param pathInfo
     * @return
     */
    public static String getSdcardHotfixDirPath(Context context, String pathInfo) {
        return getSdcardHotfixPath(context) + pathInfo + "/";
    }

    /**
     * 获取补丁文件的路径
     *
     * @param context
     * @return
     */
    public static String getHotfixApkPath(Context context, String pathInfo) {
        return getSdcardHotfixDirPath(context, pathInfo) + HOTFIX_APK_FILE_NAME;
    }

    /**
     * 获取安装随机数存在文件的路径
     *
     * @param context
     * @return
     */
    public static String getPathInfoPath(Context context) {
        return getInsideHotfixPath(context) + HOTFIX_PATHINFO_PATH;
    }

    /**
     * 获取安装随机数存在文件的路径
     *
     * @param context
     * @return
     */
    public static String getOtherPathInfoPath(Context context, String pathInfoPath) {
        return getInsideHotfixPath(context) + pathInfoPath;
    }

    /**
     * 获取手机存储上的补丁沙盒目录
     *
     * @param context
     * @return
     */
    public static String getInsideHotfixPath(Context context) {
        return context.getFilesDir() + "/hotfix/";
    }

    /**
     * 获取某个版本补丁在手机存储上的沙盒路径
     *
     * @param context
     * @param pathinfo
     * @return
     */
    public static String getInsideHotfixVersionPath(Context context, String pathinfo) {
        return getInsideHotfixPath(context) + pathinfo + File.separator;
    }

    /**
     * 获取当前补丁的so存储目录
     *
     * @param context
     * @return
     */
    public static String getNativeLibPath(Context context, String pathinfo) {
        return getInsideHotfixVersionPath(context, pathinfo) + "/lib/armeabi/";
    }

    /**
     * 获取某个版本补丁的配置信息文件目录
     *
     * @param context
     * @param pathInfo
     * @return
     */
    public static String getMetaFilePath(Context context, String pathInfo) {
        return getInsideHotfixVersionPath(context, pathInfo) + "installed.meta";
    }

    /**
     * 文件是否存在
     *
     * @param filePathName
     * @return
     */
    public static boolean fileExists(String filePathName) {
        if (TextUtils.isEmpty(filePathName)) return false;
        File file = new File(filePathName);
        return (!file.isDirectory() && file.exists());
    }

    /**
     * 描述：判断一个字符串是否为null或空值.
     *
     * @param str 指定的字符串
     * @return true or false
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * 重命名
     *
     * @param filePathName
     * @param newPathName
     */
    public static boolean rename(String filePathName, String newPathName) {
        if (TextUtils.isEmpty(filePathName)) return false;
        if (TextUtils.isEmpty(newPathName)) return false;

        delete(newPathName);

        File file = new File(filePathName);
        File newFile = new File(newPathName);
        File parentFile = newFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        return file.renameTo(newFile);
    }

    /**
     * 删除文件
     */
    public static boolean delete(String filePathName) {
        if (TextUtils.isEmpty(filePathName)) return false;
        File file = new File(filePathName);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * 删除文件夹及其内容
     *
     * @param file
     */
    public static void deleteDirectorySafe(File file) {
        if (file == null || !file.isDirectory()) {
            return;
        }
        File[] paths = file.listFiles();
        if (paths != null) {
            for (File pathF : paths) {
                if (pathF.isDirectory()) {
                    deleteDirectorySafe(pathF);
                } else {
                    deleteFileSafe(pathF);
                }
            }
        }
        deleteFileSafe(file);
    }

    /**
     * 清空文件夹
     *
     * @param dirPath
     */
    public static void deleteFilesInDirectory(String dirPath) {
        File dirF = new File(dirPath);
        File[] files = dirF.listFiles();
        if (files != null && files.length > 0) {
            for (File fileF : files) {
                if (fileF.isFile()) {
                    deleteFileSafe(fileF);
                } else if (fileF.isDirectory()) {
                    deleteDirectorySafe(fileF);
                }
            }
        }
    }

    /**
     * 为防止创建一个正在被删除的文件夹，所以在删除前先重命名该文件夹
     *
     * @param file
     */
    public static void deleteFileSafe(File file) {
        String time = String.valueOf(System.currentTimeMillis());
        File to = new File(file.getAbsolutePath() + time);
        rename(file.getAbsolutePath(), file.getAbsolutePath() + time);
        to.delete();
    }

    /**
     * 创建目录，整个路径上的目录都会创建
     *
     * @param path
     */
    public static void createDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * 创建目录，整个路径上的目录都会创建
     *
     * @param path
     */
    public static File createDirWithFile(String path) {
        File file = new File(path);
        if (!path.endsWith("/")) {
            file = file.getParentFile();
        }
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    /**
     * 复制文件
     *
     * @param fromPathName
     * @param toPathName
     * @return
     */
    public static int copy(String fromPathName, String toPathName) {
        try {
            File newFile = new File(toPathName);
            File parentFile = newFile.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            InputStream from = new FileInputStream(fromPathName);
            return copy(from, toPathName);
        } catch (FileNotFoundException e) {
            return -1;
        }
    }

    /**
     * 复制文件
     *
     * @param from
     * @param toPathName
     * @return
     */
    public static int copy(InputStream from, String toPathName) {
        OutputStream to = null;
        try {
            File newFile = new File(toPathName);
            File parentFile = newFile.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            delete(toPathName);
            to = new BufferedOutputStream(new FileOutputStream(toPathName));
            byte buf[] = new byte[1024];
            int c;
            while ((c = from.read(buf)) > 0) {
                to.write(buf, 0, c);
            }
            return 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        } finally {
            close(to);
            close(from);
        }
    }

    public static void setField(Object paramClass, String paramString,
                                Object newClass) {
        if (paramClass == null || TextUtils.isEmpty(paramString)) return;
        Field field = null;
        Class cl = paramClass.getClass();
        for (; field == null && cl != null; ) {
            try {
                field = cl.getDeclaredField(paramString);
                if (field != null) {
                    field.setAccessible(true);
                }
            } catch (Throwable e) {

            }
            if (field == null) {
                cl = cl.getSuperclass();
            }
        }
        if (field != null) {
            try {
                field.set(paramClass, newClass);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return;
    }

    public static Object getField(Object paramClass, String paramString) {
        if (paramClass == null) return null;
        Field field = null;
        Object object = null;
        Class cl = paramClass.getClass();
        for (; field == null && cl != null; ) {
            try {
                field = cl.getDeclaredField(paramString);
                if (field != null) {
                    field.setAccessible(true);
                }
            } catch (Exception e) {

            }
            if (field == null) {
                cl = cl.getSuperclass();
            }
        }
        try {
            if (field != null)
                object = field.get(paramClass);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     * 将压缩文件中的某个文件成字符串
     *
     * @param zipFile     压缩文件
     * @param fileNameReg 需要获取的文件名
     * @return
     */
    public static String unzipFileStringByRegForPlug(String zipFile, String fileNameReg) {
        String result = null;
        byte[] buffer = new byte[BUF_SIZE];
        InputStream in = null;
        ZipInputStream zipIn = null;
        ByteArrayOutputStream bos = null;
        try {
            File file = new File(zipFile);
            in = new FileInputStream(file);
            zipIn = new ZipInputStream(in);
            ZipEntry entry;
            while (null != (entry = zipIn.getNextEntry())) {
                String zipName = entry.getName();
                if (zipName.equals(fileNameReg)) {
                    int bytes = 0;
                    int count = 0;
                    bos = new ByteArrayOutputStream();

                    while ((bytes = zipIn.read(buffer, 0, BUF_SIZE)) != -1) {
                        bos.write(buffer, 0, bytes);
                        count += bytes;
                    }
                    if (count > 0) {
                        result = bos.toString();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                close(in);
                close(zipIn);
                close(bos);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 将压缩文件中的某个文件夹拷贝到指定文件夹中
     *
     * @param zipFile     压缩文件
     * @param toDir       指定一个存放解压缩文件的文件夹,或者直接指定文件名方法自动识别
     * @param fileNameReg 需要解压的文件夹路径如：res/drawable-hdpi/
     * @return
     */
    public static boolean unzipFileByRegForPlug(String zipFile, String toDir, String fileNameReg, String pathInfo) {
        boolean result = false;
        byte[] buffer = new byte[BUF_SIZE];
        InputStream in = null;
        ZipInputStream zipIn = null;
        try {
            File file = new File(zipFile);
            in = new FileInputStream(file);
            zipIn = new ZipInputStream(in);
            ZipEntry entry = null;
            while (null != (entry = zipIn.getNextEntry())) {
                String zipName = entry.getName();
                String relName = getFinalSoName(toDir + zipName, pathInfo);
                if (zipName.startsWith(fileNameReg)) {
                    File unzipFile = new File(toDir);
                    if (unzipFile.isDirectory()) {
                        createDirWithFile(relName);
                        unzipFile = new File(relName);
                    }
                    FileOutputStream out = new FileOutputStream(unzipFile);
                    int bytes;

                    while ((bytes = zipIn.read(buffer, 0, BUF_SIZE)) != -1) {
                        out.write(buffer, 0, bytes);
                    }
                    out.close();
                }
            }
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        } finally {
            try {
                in.close();
                zipIn.close();
            } catch (Exception e2) {
            }
        }
        return result;
    }

    public static String readString(InputStream is) throws IOException {
        char[] buf = new char[2048];
        Reader r = new InputStreamReader(is, "UTF-8");
        StringBuilder s = new StringBuilder();
        while (true) {
            int n = r.read(buf);
            if (n < 0)
                break;
            s.append(buf, 0, n);
        }
        return s.toString();
    }

    public static String readString(String filePath) {
        if (TextUtils.isEmpty(filePath)) return null;
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) return null;
        try {
            return readString(new FileInputStream(file));
        } catch (IOException e) {
            return null;
        }
    }

    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static String getFinalSoName(String name, String version) {
        if (TextUtils.isEmpty(version)) return name;
        try {
            StringBuilder s = new StringBuilder();
            s.append(name.substring(0, name.lastIndexOf(".")));
            s.append(version);
            s.append(name.substring(name.lastIndexOf(".")));
            return s.toString();
        } catch (Throwable e) {
            e.printStackTrace();
            return name;
        }
    }

    static int sCputype = -1;

    /**
     * 获取cpu类型和架构
     *
     * @return 返回CPU的指令集类型，仅支持arm,x86和mips这三种，arm中不区分armv6，armv7和neon.
     */
    public static int getCpuArchitecture() {
        //我们只支持armeabi，其他so都不存在，所以直接返回arm
//        return CPU_AMR;
        if (sCputype != -1) return sCputype;
        try {
            InputStream is = new FileInputStream("/proc/cpuinfo");
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);
            try {
                String nameProcessor = "Processor";
                String nameModel = "model name";
                while (true) {
                    String line = br.readLine();
                    String[] pair;
                    if (line == null) {
                        break;
                    }
                    pair = line.split(":");
                    if (pair.length != 2)
                        continue;
                    String key = pair[0].trim();
                    String val = pair[1].trim();
                    if (key.compareTo(nameProcessor) == 0) {
                        if (val.contains("ARM")) {
                            sCputype = CPU_AMR;
                        }
                    }

                    if (key.compareToIgnoreCase(nameModel) == 0) {
                        if (val.contains("Intel")) {
                            sCputype = CPU_X86;
                        }
                    }

                    if (key.compareToIgnoreCase(nameProcessor) == 0) {
                        if (val.contains("MIPS")) {
                            sCputype = CPU_MIPS;
                        }
                    }
                }
            } finally {
                br.close();
                ir.close();
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return CPU_AMR;
        }

        return sCputype;
    }

    /**
     * 获取当前应当执行的so文件的存放文件夹
     */
    public static String getLibFile(int cpuType) {
        switch (cpuType) {
            case CPU_AMR:
                return "lib/armeabi";
            case CPU_X86:
                return "lib/x86/";
            case CPU_MIPS:
                return "lib/mips/";
            default:
                return "lib/armeabi/";
        }
    }

    /**
     * 重启当前包名下的其他进程
     *
     * @param context
     */
    public static void resetPackagerOtherProcess(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo ai : am.getRunningAppProcesses()) {
            if (ai.uid == android.os.Process.myUid() && ai.pid != android.os.Process.myPid()) {
                android.os.Process.killProcess(ai.pid);
            }
        }
    }

    public static boolean writeString(String filePath, String info) {
        File file = new File(filePath);
        FileOutputStream out = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new FileOutputStream(file);
            out.write(info.getBytes());
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
     * 获取某个已安装的包的签名信息
     *
     * @param packageName
     * @param context
     * @return
     * @throws Exception
     */
    public static Signature[] getPackageSignature(String packageName, Context context) throws Exception {

        PackageInfo localPackageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        if (localPackageInfo != null && localPackageInfo.signatures != null) {
            return localPackageInfo.signatures;
        }
        return getApkSignature(context.getPackageCodePath(), context);
    }

    /**
     * 获取某个apk的签名信息
     *
     * @param apkPath
     * @param context
     * @return
     * @throws Exception
     */
    public static Signature[] getApkSignature(String apkPath, Context context) throws Exception {

        PackageInfo localPackageInfo = context.getPackageManager()
                .getPackageArchiveInfo(
                        apkPath,
                        PackageManager.GET_SIGNATURES);
        if (localPackageInfo != null && localPackageInfo.signatures != null) {
            return localPackageInfo.signatures;
        }
        Class clazz = Class.forName("android.content.pm.PackageParser");
        Object packag;
        Object packageParser;
        try {
            java.lang.reflect.Method parsePackageMethod = clazz.getMethod(
                    "parsePackage",
                    File.class,
                    String.class,
                    DisplayMetrics.class,
                    int.class);
            packageParser = clazz.getConstructor(String.class).newInstance("");

            DisplayMetrics localDisplayMetrics = new DisplayMetrics();
            localDisplayMetrics.setToDefaults();

            packag = parsePackageMethod.invoke(packageParser,
                    new File(apkPath), null, localDisplayMetrics, PackageManager.GET_SIGNATURES);
        } catch (Throwable e) {
            java.lang.reflect.Method parsePackageMethod = clazz.getMethod(
                    "parsePackage",
                    File.class,
                    int.class);
            packageParser = clazz.newInstance();
            packag = parsePackageMethod.invoke(packageParser,
                    new File(apkPath),
                    PackageManager.GET_SIGNATURES);
        }
        java.lang.reflect.Method collectCertificatesMethod = clazz.getMethod(
                "collectCertificates",
                Class.forName("android.content.pm.PackageParser$Package"),
                int.class);
        collectCertificatesMethod.invoke(packageParser, packag, PackageManager.GET_SIGNATURES);
        Signature signatures[] = (Signature[]) packag.getClass().getField("mSignatures").get(packag);

        return signatures;
    }
}
