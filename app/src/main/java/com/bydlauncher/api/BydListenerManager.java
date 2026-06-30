package com.bydlauncher.api;

import android.util.Log;

import com.bydlauncher.model.VehicleStatus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class BydListenerManager {

    private static final String TAG = "BydListenerManager";

    public interface OnStateChanged {
        void onChanged();
    }

    private OnStateChanged callback;
    private final VehicleStatus sharedStatus;
    private boolean bodyworkListenerActive = false;
    private boolean acListenerActive = false;

    public BydListenerManager(VehicleStatus sharedStatus) {
        this.sharedStatus = sharedStatus;
    }

    public void setCallback(OnStateChanged callback) {
        this.callback = callback;
    }

    public VehicleStatus getSharedStatus() { return sharedStatus; }

    public void registerBodyworkListener(Object bodyworkDevice) {
        if (bodyworkDevice == null) return;
        try {
            Class<?> listenerClass = Class.forName(
                    "android.hardware.bydauto.bodywork.AbsBYDAutoBodyworkListener");

            Object proxy = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    new BodyworkHandler());

            Method register = bodyworkDevice.getClass().getMethod(
                    "registerListener", listenerClass);
            register.invoke(bodyworkDevice, proxy);
            bodyworkListenerActive = true;
            Log.i(TAG, "Bodywork listener registered");
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Bodywork listener class not found, falling back to polling");
        } catch (Exception e) {
            Log.w(TAG, "Failed to register bodywork listener, falling back to polling", e);
        }
    }

    public void registerAcListener(Object acDevice) {
        if (acDevice == null) return;
        try {
            Class<?> listenerClass = Class.forName(
                    "android.hardware.bydauto.ac.AbsBYDAutoAcListener");

            Object proxy = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    new AcHandler());

            Method register = acDevice.getClass().getMethod(
                    "registerListener", listenerClass);
            register.invoke(acDevice, proxy);
            acListenerActive = true;
            Log.i(TAG, "AC listener registered");
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "AC listener class not found, falling back to polling");
        } catch (Exception e) {
            Log.w(TAG, "Failed to register AC listener, falling back to polling", e);
        }
    }

    public boolean isBodyworkListenerActive() { return bodyworkListenerActive; }
    public boolean isAcListenerActive() { return acListenerActive; }

    private void notifyChanged() {
        if (callback != null) callback.onChanged();
    }

    private class BodyworkHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            try {
                switch (name) {
                    case "onDoorStateChanged":
                        if (args != null && args.length >= 2) {
                            int area = (int) args[0];
                            int state = (int) args[1];
                            boolean open = (state == 1);
                            switch (area) {
                                case 1: sharedStatus.doorLeftFrontOpen = open; break;
                                case 2: sharedStatus.doorRightFrontOpen = open; break;
                                case 3: sharedStatus.doorLeftRearOpen = open; break;
                                case 4: sharedStatus.doorRightRearOpen = open; break;
                                case 5: sharedStatus.hoodOpen = open; break;
                                case 6: sharedStatus.trunkOpen = open; break;
                            }
                            notifyChanged();
                        }
                        break;
                    case "onWindowStateChanged":
                        if (args != null && args.length >= 2) {
                            int area = (int) args[0];
                            int state = (int) args[1];
                            switch (area) {
                                case 1: sharedStatus.windowFL = state; break;
                                case 2: sharedStatus.windowFR = state; break;
                                case 3: sharedStatus.windowRL = state; break;
                                case 4: sharedStatus.windowRR = state; break;
                            }
                            notifyChanged();
                        }
                        break;
                    case "onAutoSystemStateChanged":
                        if (args != null && args.length >= 1) {
                            sharedStatus.isLocked = ((int) args[0] == 1);
                            notifyChanged();
                        }
                        break;
                    case "toString":
                        return "BydListenerManager.BodyworkHandler";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                }
            } catch (Exception e) {
                Log.w(TAG, "Bodywork callback error: " + name, e);
            }
            return null;
        }
    }

    private class AcHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            try {
                switch (name) {
                    case "onAcStartStateChanged":
                        if (args != null && args.length >= 1) {
                            sharedStatus.acOn = ((int) args[0] == 1);
                            notifyChanged();
                        }
                        break;
                    case "onAcTemperatureChanged":
                        if (args != null && args.length >= 2) {
                            int zone = (int) args[0];
                            int temp = (int) args[1];
                            if (zone == 1) sharedStatus.acTemp = temp;
                            else if (zone == 4) sharedStatus.outsideTemp = temp;
                            notifyChanged();
                        }
                        break;
                    case "onAcWindLevelChanged":
                        if (args != null && args.length >= 1) {
                            sharedStatus.acWindLevel = (int) args[0];
                            notifyChanged();
                        }
                        break;
                    case "onAcCycleModeChanged":
                        if (args != null && args.length >= 1) {
                            sharedStatus.acCycleMode = (int) args[0];
                            notifyChanged();
                        }
                        break;
                    case "toString":
                        return "BydListenerManager.AcHandler";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                }
            } catch (Exception e) {
                Log.w(TAG, "AC callback error: " + name, e);
            }
            return null;
        }
    }
}
