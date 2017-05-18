# ZeusHotfix

**简单的android APP热更新方案**

## 使用方法

1. 将项目中的zeushotfix下的类拷贝到你的项目中。
2. 在你的项目的AndroidManifest.xml中添加如下代码：
    <service android:name="com.zeushotfix.inside.InstallHotfixService" android:process=":hotfix"/>
    <meta-data android:name="HOTFIX_VERSION" android:value="1" />
    
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    InstallHotfixService为安装的service，在独立进程中进行安装，安装完成后自动杀死当前进程，对主进程无影响
    HOTFIX_VERSION为当前的热修复版本，只有该值相同的apk才能进行更新，每次更新了以下信息时请增加该值：
        
        1.AndroidManifest.xml中的除了version code 和version name的其他信息;
        2.Window启动动画，即activity/Dialog/window/popupWindow;
        3.Activity的theme中的style;
        4.通知栏图标;
        5.桌面widget资源;
        6.apk名称;

3. 将HotfixLoaderUtil.java中的APPLICATION_NAME替换为你apk中的Application,如果你没有自己的application则不用替换，如果apk加固了就替换为加固后的Application。
    APPLICATION_NAME_DEBUG为调试时的Application，如果没加固就跟APPLICATION_NAME相同即可。
3. 在你的项目中添加新apk的下载逻辑，请自行实现。
4. 将下载完成的apk进行安装，调用方式类似MainActivity.java中的copyHotfixApkAndInstall()方法，具体请参考该方法实现。
5. 安装完成后，重启主进程即可使用安装的新版本apk。
6. 如果apk被加固，请将com.zeushotfix.wrap放到加固壳的外部，inside与wrap代码是相互隔离的，方法不能相互调用，所以有重复代码，如果不需要加固，直接删除其中重复的类即可。
7. 该框架主要提供两个方法，即补丁apk的安装和加载。HotfixLoaderUtil.java中加载补丁attachBaseContext()方法和HotfixInstallerUtil.java中的安装补丁installHotfix()方法。

## 支持特性与优点

1. 支持将低版本apk静默升级到高版本apk，version code可以不变或者提高
2. 适配了从android 2.3到最新的7.1。
3. 支持各方的加固方案，但是需要与加固方配合，将ZeusHotfix.warp所在的包名不进行加固，并且Application必须是ZeusHotfix中的ZeusHotfixApplicationProxy。
4. 对性能无明显影响，在应用补丁时，启动速度会降低150ms。
5. 当在2分钟内，使用的补丁包崩溃了两次，则会把补丁包卸载，防止因为补丁包导致的重大问题。

## 不支持的特性

1. 不支持高版本中apk更新activity跳转动画。
2. 不支持高版本中更改AndroidManifest.xml。
3. 不支持高版本中更改通知栏图标。
4. 不支持更改其他进程需要访问的资源(apk图标、Manifest、apk名称、window/dialog/popupWindow动画，桌面小图标资源等)。
5. AndroidManifest中的HOTFIX_VERSION为补丁版本，一旦以上资源发生变化，请增加HOTFIX_VERSION，只有HOTFIX_VERSION一致的情况下，才能使用新版本apk。

## 欢迎加群交流讨论

> QQ群：`558449447`，添加请注明来自`ZeusHotfix`
>
> <a target="_blank" href="http://shang.qq.com/wpa/qunwpa?idkey=4464e9ee4fc8b05ee3c4eeb4f4be97469c1cfe46cded6b00f4a887ebebb60916"><img border="0" src="http://pub.idqqimg.com/wpa/images/group.png" alt="Android技术交流分享" title="Android技术交流分享"></a>
>
> 欢迎各位使用并提交PR，该项目会持续维护。
>
>
> 另：欢迎加入[掌阅](http://www.zhangyue.com/jobs)大家庭，一起研究Android新技术。简历请发送`huangjian@zhangyue.com`,注明应聘方向。
>
# LICENSE

```
MIT LICENSE 
Copyright (c) 2016 zhangyue

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
