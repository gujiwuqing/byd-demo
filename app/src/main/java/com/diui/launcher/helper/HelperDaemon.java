package com.diui.launcher.helper;

import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;

import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Shell-uid binder 守护进程入口。由 app 通过 ADB 启动：
 *   CLASSPATH=<apk> setsid app_process /system/bin \
 *     --nice-name=diui_helper com.diui.launcher.helper.HelperDaemon <appUid>
 *
 * 生命周期：
 *   1. 从 args[0] 解析 expectedUid。
 *   2. 获取单实例文件锁——已占用则退出。
 *   3. 反射获取 autoservice IBinder。
 *   4. 通过 ServiceManager.addService 注册 binder stub 为 SERVICE_NAME。
 *   5. 打印 READY，Looper.loop() 保活。
 *
 * Hidden-API 说明：此 daemon 运行在 app_process（tool context）而非普通 app 进程，
 * hidden-API 限制只对 app 进程生效，因此这里可直接反射，无需 HiddenApiBypass。
 */
public class HelperDaemon {

    private static final String TAG = "HelperDaemon";
    private static final String LOCK_FILE = "/data/local/tmp/diui_helper.lock";

    private static IBinder autoserviceBinder;
    private static String autoserviceDescriptor = "";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("ERR: usage: HelperDaemon <appUid>");
            // 用 exitProcess 而非 return：app_process 的 binder threadpool 是非 daemon 线程，
            // 裸 return 会让 JVM 挂起。
            System.exit(2);
        }

        final int expectedUid;
        try {
            expectedUid = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("ERR: invalid appUid: " + args[0]);
            System.exit(2);
            return;
        }

        // 单实例锁
        try {
            RandomAccessFile lockFile = new RandomAccessFile(LOCK_FILE, "rw");
            FileChannel channel = lockFile.getChannel();
            FileLock lock = channel.tryLock();
            if (lock == null) {
                System.out.println("ALREADY_RUNNING");
                System.exit(0);
                return;
            }
        } catch (Exception e) {
            System.err.println("ERR: lock failed: " + e.getMessage());
            System.exit(3);
            return;
        }

        // 解析 autoservice Binder
        try {
            Class<?> smCls = Class.forName("android.os.ServiceManager");
            Method getService = smCls.getMethod("getService", String.class);
            autoserviceBinder = (IBinder) getService.invoke(null, "autoservice");
            if (autoserviceBinder == null) {
                System.err.println("ERR: autoservice not found");
                System.exit(3);
                return;
            }
            try {
                autoserviceDescriptor = autoserviceBinder.getInterfaceDescriptor();
            } catch (Exception ignored) {}
        } catch (Exception e) {
            System.err.println("ERR: resolve autoservice failed: " + e.getMessage());
            System.exit(3);
            return;
        }

        // 准备主 Looper（binder 线程需要）
        Looper.prepareMainLooper();

        // 注册 binder stub
        IBinder helperBinder = new Binder() {
            @Override
            protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
                // uid 鉴权：只允许本 app 调用
                if (Binder.getCallingUid() != expectedUid) {
                    Log.w(TAG, "Rejected transact from uid " + Binder.getCallingUid());
                    return false;
                }

                try {
                    data.enforceInterface(HelperBinderProtocol.DESCRIPTOR);
                } catch (Exception e) {
                    return false;
                }

                switch (code) {
                    case HelperBinderProtocol.TX_PING:
                        if (reply != null) reply.writeInt(0);
                        return true;

                    case HelperBinderProtocol.TX_READ: {
                        int tx = data.readInt();
                        int dev = data.readInt();
                        int fid = data.readInt();
                        int[] result = execAutoservice(tx, dev, fid, 0, false);
                        if (reply != null) {
                            reply.writeInt(result[0]);
                            reply.writeInt(result[1]);
                        }
                        return true;
                    }

                    case HelperBinderProtocol.TX_WRITE: {
                        int dev = data.readInt();
                        int fid = data.readInt();
                        int val = data.readInt();
                        if (!WriteAllowlist.isWriteAllowed(dev)) {
                            if (reply != null) {
                                reply.writeInt(-2);
                                reply.writeInt(0);
                            }
                            return true;
                        }
                        int[] result = execAutoservice(
                                HelperBinderProtocol.AUTO_TX_SET_INT, dev, fid, val, true);
                        if (reply != null) {
                            reply.writeInt(result[0]);
                            reply.writeInt(result[1]);
                        }
                        return true;
                    }

                    default:
                        return false;
                }
            }
        };

        try {
            Class<?> smCls = Class.forName("android.os.ServiceManager");
            Method addService = smCls.getMethod("addService", String.class, IBinder.class);
            addService.invoke(null, HelperBinderProtocol.SERVICE_NAME, helperBinder);
        } catch (Exception e) {
            System.err.println("ERR: addService failed: " + e.getMessage());
            System.exit(3);
            return;
        }

        System.out.println("READY");
        Log.i(TAG, "HelperDaemon ready, uid gate=" + expectedUid
                + " autoservice=" + autoserviceDescriptor);

        Looper.loop();
    }

    /**
     * 直接对 autoservice Binder 做 transact，解析 Parcel 返回码与值。
     * 返回 [status, value]：status=0 成功（getInt），setInt 返回原始码；<0 错误。
     */
    private static int[] execAutoservice(int tx, int dev, int fid, int val, boolean isWrite) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            if (autoserviceDescriptor != null && !autoserviceDescriptor.isEmpty()) {
                data.writeInterfaceToken(autoserviceDescriptor);
            }
            data.writeInt(dev);
            data.writeInt(fid);
            if (isWrite) data.writeInt(val);

            boolean ok = autoserviceBinder.transact(tx, data, reply, 0);
            if (!ok) return new int[]{-1, 0};

            // autoservice 的 Parcel 响应：前 4 字节是异常码（0=正常），其后是返回值
            int exception = reply.readInt();
            if (exception != 0) return new int[]{-exception, 0};

            int value = reply.readInt();
            return new int[]{0, value};
        } catch (Exception e) {
            Log.w(TAG, "autoservice transact failed: " + e.getMessage());
            return new int[]{-1, 0};
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
