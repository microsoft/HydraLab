// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;

public abstract class WindowBridge {
    protected Context mApplicationContext;
    protected View mView;
    protected boolean isMainViewRemoved;
    protected String mTag = "WindowBridge";
    protected int mWidth;
    protected int mHeight;
    protected int gravity = Gravity.BOTTOM | Gravity.END;
    protected int xOffset = 0;
    protected int yOffset;
    private boolean isShow;
    private boolean mainViewAdded = false;

    public WindowBridge(Context mApplicationContext, View view, String tag,
                        int mWidth, int mHeight, int x, int y) {
        this.mApplicationContext = mApplicationContext.getApplicationContext();
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mView = view;
        if (!TextUtils.isEmpty(tag)) {
            this.mTag = tag;
        }
        xOffset = x;
        yOffset = y;
    }

    protected abstract void addViewToWindow();

    protected abstract void removeViewFromWindow();


    public void destroy() {
        removeViewFromWindow();
        mView = null;
        mApplicationContext = null;
    }

    public void show() {
        if (!mainViewAdded) {
            addViewToWindow();
            mainViewAdded = true;
            isShow = true;
        } else {
            if (isShow) {
                return;
            }
            mView.setVisibility(View.VISIBLE);
            isShow = true;
        }
    }

    public void hide() {
        if (!mainViewAdded || !isShow) return;
        mView.setVisibility(View.INVISIBLE);
        isShow = false;
    }

    public View getView() {
        return mView;
    }

    public void setView(View view) {
        removeViewFromWindow();
        this.mView = view;
        addViewToWindow();
    }



    public String getTag() {
        return mTag;
    }

    public void updateRect(Rect rect) {

    }

    public void moveToTopInParent() {
        removeViewFromWindow();
        addViewToWindow();
    }

    public void updateLayout() {

    }

    public void removeViewImmediate(View view) {

    }

    public void addView(View view) {

    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

}
