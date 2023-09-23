/*
MIT License

Copyright (c) 2022 Pierre Hébert

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package net.pierrox.lightning_launcher.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;

import net.pierrox.lightning_launcher.data.Utils;
import net.pierrox.lightning_launcher_extreme.R;

public class EditBarHiderView extends ImageButton implements Runnable {
    private static final long ANIM_DURATION = 200;

    private final Bitmap mBitmap;
    private float mAngle;
    private Matrix mMatrix;
    private long mAnimStart;
    private boolean mAnimDirection;

    public EditBarHiderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mBitmap = Utils.createIconFromText(getResources().getDimensionPixelSize(R.dimen.eb_text_size), "#", Color.BLACK);
        setImageBitmap(mBitmap);
        setScaleType(ImageView.ScaleType.MATRIX);

        mMatrix = new Matrix();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateMatrix();
    }

    @Override
    public void run() {
        long delta = AnimationUtils.currentAnimationTimeMillis() - mAnimStart;
        if (delta > ANIM_DURATION) {
            delta = ANIM_DURATION;
        }
        float angle = 180f * delta / ANIM_DURATION;
        if (!mAnimDirection) {
            angle = 180 - angle;
        }
        setEditBarHiderRotation(angle);
        if (delta == ANIM_DURATION) {
            mMatrix = null;
        } else {
            post(this);
        }
    }

    private void setEditBarHiderRotation(float angle) {
        mAngle = angle;
        updateMatrix();
    }

    private void updateMatrix() {
        int dw = mBitmap.getWidth() / 2;
        int dh = mBitmap.getHeight() / 2;
        if (mMatrix == null) {
            mMatrix = new Matrix();
        } else {
            mMatrix.reset();
        }
        mMatrix.postRotate(mAngle, dw, dh);
        mMatrix.postTranslate(getWidth() / 2 - dw, getHeight() / 2 - dh);

        setImageMatrix(mMatrix);
    }

    public void setArrowDirection(boolean open) {
        if (mAnimDirection != open) {
            mAnimDirection = open;
            removeCallbacks(this);
            mAnimStart = AnimationUtils.currentAnimationTimeMillis();
            run();
        }
    }
}
