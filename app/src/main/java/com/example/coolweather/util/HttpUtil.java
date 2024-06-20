package com.example.coolweather.util;

import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {
    //工具类
    //发送网络请求，处理服务器响应，实现与服务器的交互

    public static void sendOkHttpRequest(String address,okhttp3.Callback callback) {
//        String address: 表示请求的URL地址。
//        okhttp3.Callback callback: 一个回调接口，用于处理服务器的响应。
//        创建OkHttpClient类的对象，用于发送和接收网络请求
        OkHttpClient okHttpClient=new OkHttpClient();
//        通过传入的的URL地址构建请求对象
        Request request=new Request.Builder().url(address).build();
//        使用异步方式发送请求，并将服务器的响应通过传入的回调接口处理
        okHttpClient.newCall(request).enqueue(callback);
    }
}

/*
    enqueue方法是OkHttp库中的一个方法，用于异步发送HTTP请求。
    与同步方法execute不同，enqueue不会阻塞调用线程，而是在请求完成或发生错误时通过回调接口通知调用者。
    这种方式适用于需要在后台执行网络请求而不影响用户界面线程的场景
*/

/*
    enqueue()方法解析：
        onFailure：当请求因网络问题或其他原因失败时，onFailure方法会被调用，传递异常信息。
        onResponse：当服务器返回响应时，onResponse方法会被调用，传递响应对象。
        需要注意的是，在这个方法内可以检查响应是否成功（通过response.isSuccessful()），然后处理响应内容。
*/
