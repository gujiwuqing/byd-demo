package com.diui.launcher.helper;

import android.os.IBinder;

/**
 * Binder 线协议，由 app 进程内的 HelperClient 和 shell-uid 的 HelperDaemon 共享。
 *
 * Daemon 通过 ServiceManager.addService 注册为 SERVICE_NAME；
 * app 通过 ServiceManager.getService + IBinder.transact 调用。
 *
 * Parcel 布局（请求 data 先 writeInterfaceToken(DESCRIPTOR)）：
 *   TX_PING  : 请求无参数                          -> 响应 writeInt(status)
 *   TX_READ  : writeInt(tx), writeInt(dev), writeInt(fid)
 *                                                   -> 响应 writeInt(status), writeInt(value)
 *   TX_WRITE : writeInt(dev), writeInt(fid), writeInt(value)
 *                                                   -> 响应 writeInt(status), writeInt(value)
 *
 * status/value 透传 autoservice transact 的原始返回码：
 *   getInt 成功   → status=0, value=读到的值
 *   setInt 真实动作 → status=1
 *   setInt 空操作   → status=0
 *   错误           → status<0
 */
public final class HelperBinderProtocol {

    public static final String SERVICE_NAME = "diui_helper";
    public static final String PROCESS_NAME = "diui_helper";
    public static final String DESCRIPTOR = "com.diui.launcher.helper.IHelper";

    public static final int TX_PING = IBinder.FIRST_CALL_TRANSACTION;       // 1
    public static final int TX_READ = IBinder.FIRST_CALL_TRANSACTION + 1;   // 2
    public static final int TX_WRITE = IBinder.FIRST_CALL_TRANSACTION + 2;  // 3

    /** autoservice transact code：5=getInt, 7=getFloat, 6=setInt */
    public static final int AUTO_TX_GET_INT = 5;
    public static final int AUTO_TX_GET_FLOAT = 7;
    public static final int AUTO_TX_SET_INT = 6;

    private HelperBinderProtocol() {}
}
