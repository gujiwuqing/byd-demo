package com.diui.launcher.api;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 探索 BYD 车机上可用的 android.hardware.bydauto.* 系统类及其方法。
 * 用于辅助开发，输出到 Logcat 供调试。
 */
public class BydApiExplorer {

    private static final String TAG = "BydApiExplorer";

    /**
     * 已知的 BYD API 包名候选
     */
    private static final String[] KNOWN_PACKAGES = {
            "android.hardware.bydauto.ac",
            "android.hardware.bydauto.bodywork",
            "android.hardware.bydauto.statistic",
            "android.hardware.bydauto.doorlock",
            "android.hardware.bydauto.tire",
            "android.hardware.bydauto.vehicle",
            "android.hardware.bydauto.drive",
            "android.hardware.bydauto.gear",
            "android.hardware.bydauto.speed",
            "android.hardware.bydauto.fuel",
            "android.hardware.bydauto.energy",
            "android.hardware.bydauto.motor",
            "android.hardware.bydauto.power",
            "android.hardware.bydauto.battery",
    };

    /**
     * 扫描所有已知的 BYD API 类和方法，输出到日志。
     * 调用此方法后在 Logcat 中过滤 "BydApiExplorer" 即可查看结果。
     */
    public static void exploreAll() {
        Log.i(TAG, "========== BYD API Explorer ==========");
        Log.i(TAG, "开始扫描 android.hardware.bydauto.* 包下的可用类...");

        List<String> foundClasses = new ArrayList<>();

        for (String pkg : KNOWN_PACKAGES) {
            // 尝试常见的类名模式
            String[] candidates = {
                    pkg + ".BYDAuto" + capitalize(lastPart(pkg)) + "Device",
                    pkg + ".BYDAuto" + capitalize(lastPart(pkg)) + "Manager",
                    pkg + ".BYDAuto" + capitalize(lastPart(pkg)),
                    pkg + ".IBYDAuto" + capitalize(lastPart(pkg)),
            };

            for (String className : candidates) {
                if (tryLoadAndLog(className)) {
                    foundClasses.add(className);
                }
            }

            // 尝试更宽泛的类名
            String[] additionalCandidates = {
                    pkg + ".BYDAutoDevice",
                    pkg + ".BYDAutoManager",
                    pkg + ".IBYDAutoDevice",
            };
            for (String className : additionalCandidates) {
                if (tryLoadAndLog(className)) {
                    foundClasses.add(className);
                }
            }
        }

        Log.i(TAG, "========== 扫描完成 ==========");
        Log.i(TAG, "共发现 " + foundClasses.size() + " 个可用类:");
        for (String cls : foundClasses) {
            Log.i(TAG, "  ✓ " + cls);
        }
        Log.i(TAG, "======================================");
    }

    /**
     * 尝试加载指定类并输出其所有方法签名。
     * @return 是否成功加载
     */
    private static boolean tryLoadAndLog(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Log.i(TAG, "✓ 找到类: " + className);
            logMethods(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "✗ 未找到: " + className);
            return false;
        }
    }

    /**
     * 输出类的所有公共方法签名（包括继承的方法）
     */
    private static void logMethods(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        Log.i(TAG, "  方法列表 (" + methods.length + " 个):");

        for (Method method : methods) {
            // 跳过 Object 类的方法
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("    ");

            // 返回类型
            sb.append(method.getReturnType().getSimpleName());
            sb.append(" ");

            // 方法名
            sb.append(method.getName());
            sb.append("(");

            // 参数
            Class<?>[] params = method.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(params[i].getSimpleName());
            }
            sb.append(")");

            Log.i(TAG, sb.toString());
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static String lastPart(String pkg) {
        int dot = pkg.lastIndexOf('.');
        return dot >= 0 ? pkg.substring(dot + 1) : pkg;
    }

    /**
     * 探索指定类的完整方法（用于手动调试）
     */
    public static void exploreClass(String className) {
        Log.i(TAG, "探索类: " + className);
        if (!tryLoadAndLog(className)) {
            Log.w(TAG, "类不存在: " + className);
        }
    }

    private static final String[] KNOWN_DEVICE_CLASSES = {
            "android.hardware.bydauto.ac.BYDAutoAcDevice",
            "android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice",
            "android.hardware.bydauto.doorlock.BYDAutoDoorLockDevice",
            "android.hardware.bydauto.statistic.BYDAutoStatisticDevice",
            "android.hardware.bydauto.vehicle.BYDAutoVehicleInfoDevice",
            "android.hardware.bydauto.drive.BYDAutoDriveDevice",
            "android.hardware.bydauto.tyre.BYDAutoTyreDevice",
            "android.hardware.bydauto.pm25.BYDAutoPm25Device",
            "android.hardware.bydauto.energy.BYDAutoEnergyDevice",
            "android.hardware.bydauto.power.BYDAutoPowerDevice",
            "android.hardware.bydauto.motor.BYDAutoMotorDevice",
            "android.hardware.bydauto.panorama.BYDAutoPanoramaDevice",
            "android.hardware.bydauto.gearbox.BYDAutoGearboxDevice",
    };

    public interface ProbeProgressListener {
        void onProgress(int current, int total, String message);
        void onComplete(String report);
    }

    public static void runFullProbe(android.content.Context context, ProbeProgressListener progressListener) {
        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("=== BYD API Probe Report ===\n");
            report.append("Time: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()).format(new java.util.Date())).append("\n\n");

            int total = KNOWN_DEVICE_CLASSES.length;
            for (int i = 0; i < total; i++) {
                String className = KNOWN_DEVICE_CLASSES[i];
                String shortName = className.substring(className.lastIndexOf('.') + 1);

                if (progressListener != null) {
                    int idx = i;
                    progressListener.onProgress(idx + 1, total, "扫描 " + shortName);
                }

                report.append("--- ").append(shortName).append(" ---\n");

                try {
                    Class<?> clazz = Class.forName(className);
                    report.append("  状态: 可用\n");

                    java.lang.reflect.Method[] methods = clazz.getMethods();
                    report.append("  方法 (").append(methods.length).append("):\n");
                    for (java.lang.reflect.Method m : methods) {
                        if (m.getDeclaringClass() == Object.class) continue;
                        StringBuilder sig = new StringBuilder();
                        sig.append("    ").append(m.getReturnType().getSimpleName())
                                .append(" ").append(m.getName()).append("(");
                        Class<?>[] params = m.getParameterTypes();
                        for (int j = 0; j < params.length; j++) {
                            if (j > 0) sig.append(", ");
                            sig.append(params[j].getSimpleName());
                        }
                        sig.append(")\n");
                        report.append(sig);
                    }

                    Object device = ReflectionHelper.getDeviceInstance(className, context);
                    if (device != null) {
                        report.append("  无参 getter 返回值:\n");
                        for (java.lang.reflect.Method m : methods) {
                            if (m.getDeclaringClass() == Object.class) continue;
                            if (m.getParameterCount() != 0) continue;
                            String name = m.getName();
                            if (!name.startsWith("get") && !name.startsWith("is")
                                    && !name.startsWith("has")) continue;
                            try {
                                Object result = m.invoke(device);
                                report.append("    ").append(name).append("() = ").append(result).append("\n");
                            } catch (Exception e) {
                                report.append("    ").append(name).append("() = ERROR: ")
                                        .append(e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
                                        .append("\n");
                            }
                        }
                    } else {
                        report.append("  getInstance 失败（权限不足）\n");
                    }

                } catch (ClassNotFoundException e) {
                    report.append("  状态: 不可用（类不存在）\n");
                }
                report.append("\n");
            }

            for (String line : report.toString().split("\n")) {
                Log.i(TAG, line);
            }

            if (progressListener != null) {
                progressListener.onComplete(report.toString());
            }
        }, "api-probe").start();
    }
}
