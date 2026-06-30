package com.bydlauncher.helper;

import java.util.HashSet;
import java.util.Set;

public final class WriteAllowlist {

    private static final Set<Integer> ALLOWED_WRITE_DEVS = new HashSet<>();

    static {
        ALLOWED_WRITE_DEVS.add(1000); // 空调
        ALLOWED_WRITE_DEVS.add(1001); // 车身（车窗/灯光）
        ALLOWED_WRITE_DEVS.add(1041); // 门锁
    }

    public static boolean isWriteAllowed(int deviceType) {
        return ALLOWED_WRITE_DEVS.contains(deviceType);
    }

    private WriteAllowlist() {}
}
