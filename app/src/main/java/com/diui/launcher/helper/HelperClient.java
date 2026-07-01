package com.diui.launcher.helper;

import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

/**
 * HelperDaemon 的 binder 客户端。Daemon 注册为 diui_helper binder service
 * (ServiceManager.getService + IBinder.transact)。
 *
 * read()/write()/isAvailable() 在任何失败时返回 -1/false：daemon 未注册、
 * binder 死亡、transact 被 daemon 的 uid 门控拒绝、或 autoservice 错误。
 * 调用方须把 -1/false 当作"通道不可用，稍后重试"。
 */
public class HelperClient {

    private static final String TAG = "HelperClient";

    private volatile IBinder cached;

    public HelperClient() {
    }

    public boolean isAvailable() {
        return isAlive();
    }

    /** 清除缓存并重新探测 daemon 是否存活（spawn 后调用）。 */
    public void checkAvailability() {
        cached = null;
        isAlive();
    }

    public boolean isAlive() {
        IBinder binder = ensureBinder();
        if (binder == null) return false;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(HelperBinderProtocol.DESCRIPTOR);
            boolean ok = binder.transact(HelperBinderProtocol.TX_PING, data, reply, 0);
            return ok && reply.dataAvail() >= 4 && reply.readInt() == 0;
        } catch (DeadObjectException e) {
            cached = null;
            return false;
        } catch (Exception e) {
            Log.w(TAG, "ping failed: " + e.getMessage());
            return false;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public int getInt(int dev, int fid) {
        long[] r = read(HelperBinderProtocol.AUTO_TX_GET_INT, dev, fid);
        return r == null ? -1 : (int) r[1];
    }

    public float getFloat(int dev, int fid) {
        long[] r = read(HelperBinderProtocol.AUTO_TX_GET_FLOAT, dev, fid);
        if (r == null) return -1.0f;
        return Float.intBitsToFloat((int) r[1]);
    }

    public boolean setInt(int dev, int fid, int val) {
        Long status = writeStatus(dev, fid, val);
        return status != null && status >= 0;
    }

    public Long writeStatus(int dev, int fid, int val) {
        IBinder binder = ensureBinder();
        if (binder == null) return null;
        for (int attempt = 0; attempt < 2; attempt++) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(HelperBinderProtocol.DESCRIPTOR);
                data.writeInt(dev);
                data.writeInt(fid);
                data.writeInt(val);
                boolean ok = binder.transact(HelperBinderProtocol.TX_WRITE, data, reply, 0);
                if (!ok) return null;
                if (reply.dataAvail() < 4) return null;
                return (long) reply.readInt();
            } catch (DeadObjectException e) {
                cached = null;
                binder = ensureBinder();
                if (binder == null) return null;
            } catch (Exception e) {
                Log.w(TAG, "write failed: " + e.getMessage());
                return null;
            } finally {
                data.recycle();
                reply.recycle();
            }
        }
        return null;
    }

    private long[] read(int tx, int dev, int fid) {
        IBinder binder = ensureBinder();
        if (binder == null) return null;
        for (int attempt = 0; attempt < 2; attempt++) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeInterfaceToken(HelperBinderProtocol.DESCRIPTOR);
                data.writeInt(tx);
                data.writeInt(dev);
                data.writeInt(fid);
                boolean ok = binder.transact(HelperBinderProtocol.TX_READ, data, reply, 0);
                if (!ok) return null;
                if (reply.dataAvail() < 4) return null;
                int status = reply.readInt();
                int value = reply.dataAvail() >= 4 ? reply.readInt() : 0;
                if (status != 0) return null;
                return new long[]{0, value};
            } catch (DeadObjectException e) {
                cached = null;
                binder = ensureBinder();
                if (binder == null) return null;
            } catch (Exception e) {
                Log.w(TAG, "read failed: " + e.getMessage());
                return null;
            } finally {
                data.recycle();
                reply.recycle();
            }
        }
        return null;
    }

    private IBinder ensureBinder() {
        if (cached != null && cached.isBinderAlive()) return cached;
        IBinder b = resolveBinder();
        cached = b;
        return b;
    }

    private IBinder resolveBinder() {
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            return (IBinder) sm.getMethod("getService", String.class)
                    .invoke(null, HelperBinderProtocol.SERVICE_NAME);
        } catch (Exception e) {
            Log.w(TAG, "getService failed: " + e.getMessage());
            return null;
        }
    }
}
