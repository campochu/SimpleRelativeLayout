package hit.campochu.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ckb on 17/11/14.
 */

public abstract class AsyncViewGroup extends ViewGroup {

    private boolean mEnable;

    private int mWidth;
    private int mHeight;

    protected volatile Async mAsync;

    private Object mLock = new Object();


    public AsyncViewGroup(Context context) {
        this(context, null);
    }

    public AsyncViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AsyncViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void setEnable(boolean enable) {
        mEnable = enable;
        if (mEnable && mAsync == null) {
            mAsync = new Async();
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        if (mEnable) {
            mAsync.mTag.incrementAndGet();
            mAsync.post(mAsyncMeasure);
        }
    }

    private Runnable mAsyncMeasure = new Runnable() {
        @Override
        public void run() {
            synchronized (mLock) {
                if (mAsync.mTag.get() == 0) {
                    return;
                }
                mAsync.mTag.set(0);
                if (mAsync.cached()) {
                    measureInner(mAsync.mCachedWidthSpec, mAsync.mCachedHeightSpec);
                }

                if (mAsync.mTag.get() > 0) {
                    mAsync.post(this);
                }
            }
        }
    };

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mEnable) {
            int tag = mAsync.mTag.getAndSet(0);
            synchronized (mLock) {
                if (tag == 0
                        && !mAsync.cache(widthMeasureSpec, heightMeasureSpec)
                        && !checkChange()) {
                    setMeasuredDimension(mWidth, mHeight);
                    return;
                }
                measureInner(widthMeasureSpec, heightMeasureSpec);
            }
        } else {
            measureInner(widthMeasureSpec, heightMeasureSpec);
        }
        setMeasuredDimension(mWidth, mHeight);
    }

    protected abstract void measureInner(int widthMeasureSpec, int heightMeasureSpec);

    protected final void setMeasured(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    protected abstract boolean checkChange();

    private static class Async {

        private static AsyncThread sAsyncThread;

        private int mCachedWidthSpec = Integer.MIN_VALUE;
        private int mCachedHeightSpec = Integer.MIN_VALUE;

        private volatile AtomicInteger mTag = new AtomicInteger(0);

        public Async() {
            if (sAsyncThread == null) {
                sAsyncThread = new AsyncThread("flat_layout_async");
                sAsyncThread.start();
            }
        }

        public boolean cache(int widthSpec, int heightSpec) {
            int oldWidth = mCachedWidthSpec;
            int oldHeight = mCachedHeightSpec;
            mCachedWidthSpec = widthSpec;
            mCachedHeightSpec = heightSpec;
            return oldWidth != widthSpec || oldHeight != heightSpec;
        }

        public boolean cached() {
            return mCachedWidthSpec != Integer.MIN_VALUE && mCachedHeightSpec != Integer.MIN_VALUE;
        }

        public void post(Runnable task) {
            sAsyncThread.post(task);
        }
    }

    public void setGuessMeasure(int widthSpec, int heightSpec) {
        if (mAsync != null) {
            mAsync.cache(widthSpec, heightSpec);
        }
    }

}
