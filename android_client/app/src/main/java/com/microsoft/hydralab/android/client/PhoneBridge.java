// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PhoneBridge extends WindowBridge {

    private static Point sPoint;
    private static PhoneBridge instance;

    private WindowManager mWindowManager;
    private final WindowManager.LayoutParams mLayoutParams;
    private final List<View> addedViews = new ArrayList<>();
    private TextView floatText;

    @SuppressLint("SetTextI18n")
    public static PhoneBridge getInstance(Context appContext) {
        if (instance == null) {
            TextView view = new TextView(appContext.getApplicationContext());
            view.setText("SN=" + SharedPreferencesUtils.getSharedPreferencesStringValue(
                    appContext, SharedPreferencesUtils.SN_KEY, "unknown"));
            view.setTextSize(8);
            view.setTextColor(Color.RED);
            view.setClickable(false);
            instance = new PhoneBridge(appContext.getApplicationContext(), view, "HOLDER",
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    0, 20);
            instance.floatText = view;
        }
        return instance;
    }

    public static int getScreenWidth(Context context) {
        if (sPoint == null) {
            sPoint = new Point();
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                wm.getDefaultDisplay().getSize(sPoint);
            }
        }
        return sPoint.x;
    }

    public static int getScreenHeight(Context context) {
        if (sPoint == null) {
            sPoint = new Point();
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                wm.getDefaultDisplay().getSize(sPoint);
            }
        }
        return sPoint.y;
    }

    public PhoneBridge(Context applicationContext, View view, String mTag, int width, int height, int x, int y) {
        super(applicationContext, view, mTag, width, height, x, y);
        mWindowManager = (WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.width = width;
        mLayoutParams.height = height;
        mLayoutParams.gravity = gravity;
        mLayoutParams.x = xOffset;
        mLayoutParams.y = yOffset;
        int layout_type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layout_type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layout_type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        mLayoutParams.type = layout_type;
        mLayoutParams.windowAnimations = 0;
    }


    @Override
    public void addViewToWindow() {
        if (mWindowManager == null) {
            return;
        }
        if (mView == null) {
            return;
        }
        boolean hasDrawOverlayPermission = MainActivity.hasDrawOverlayPermission(mApplicationContext);
        if (hasDrawOverlayPermission) {
            mLayoutParams.format = PixelFormat.RGBA_8888;
            mWindowManager.addView(mView, mLayoutParams);
            isMainViewRemoved = false;
        }
    }

    @Override
    protected void removeViewFromWindow() {
        if (mWindowManager == null) {
            return;
        }
        if (mView == null) {
            return;
        }
        if (isMainViewRemoved) {
            return;
        }
        try {
            mWindowManager.removeView(mView);
            isMainViewRemoved = true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        if (mWindowManager == null) {
            return;
        }
        super.destroy();
        for (View view : addedViews) {
            try {
                mWindowManager.removeView(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mWindowManager = null;
    }

    @Override
    public void updateLayout() {
        if (mWindowManager == null) {
            return;
        }
        if (mView == null) {
            return;
        }
        if (isMainViewRemoved) {
            return;
        }
        super.updateLayout();
        mWindowManager.updateViewLayout(mView, mLayoutParams);
    }


    @Override
    public void moveToTopInParent() {
        super.moveToTopInParent();
    }

    @Override
    public void addView(View view) {
        if (mWindowManager == null) {
            return;
        }
        super.addView(view);
        if (addedViews.contains(view)) {
            return;
        }
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(mLayoutParams.type, mLayoutParams.flags, mLayoutParams.format);
        params.gravity = mLayoutParams.gravity;
        ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams) view.getTag();
        params.width = layoutParams.width;
        params.height = layoutParams.height;
        params.x = mLayoutParams.x;
        params.y = mLayoutParams.y;
        mWindowManager.addView(view, params);
        addedViews.add(view);
    }

    @Override
    public void removeViewImmediate(View view) {
        if (mWindowManager == null) {
            return;
        }
        super.removeViewImmediate(view);
        addedViews.remove(view);
        mWindowManager.removeViewImmediate(view);
    }

    @SuppressLint("SetTextI18n")
    public void updateFloatText() {
        floatText.setText("SN=" + SharedPreferencesUtils.getSharedPreferencesStringValue(
                mView.getContext(), SharedPreferencesUtils.SN_KEY, "unknown"));
    }
}
