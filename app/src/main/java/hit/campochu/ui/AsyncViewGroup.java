package hit.campochu.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ckb on 17/11/14.
 */

public abstract class AsyncViewGroup extends ViewGroup {
    private boolean mEnable;

    private int mMeasuredWidthSpec;
    private int mMeasuredHeightSpec;
    private int mCachedWidthSpec = Integer.MIN_VALUE;
    private int mCachedHeightSpec = Integer.MIN_VALUE;
    private static ExecutorService sAsyncExecutor;

    private volatile AtomicInteger mRequestCounter = new AtomicInteger(0);

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
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        if (mEnable) {
            mRequestCounter.incrementAndGet();
            startAsyncMeasure();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mEnable) {
            int requestCount = mRequestCounter.getAndSet(0);
            synchronized (mLock) {
                if (requestCount == 0
                        && !cacheMeasureSpec(widthMeasureSpec, heightMeasureSpec)
                        && !childrenChanged()) {
                    setMeasuredDimension(mMeasuredWidthSpec, mMeasuredHeightSpec);
                    return;
                }
                measureInner(widthMeasureSpec, heightMeasureSpec);
            }
        } else {
            measureInner(widthMeasureSpec, heightMeasureSpec);
        }
        setMeasuredDimension(mMeasuredWidthSpec, mMeasuredHeightSpec);
    }

    protected abstract void measureInner(int widthMeasureSpec, int heightMeasureSpec);

    protected abstract boolean childrenChanged();

    protected final void setMeasureResult(int widthSpec, int heightSpec) {
        mMeasuredWidthSpec = widthSpec;
        mMeasuredHeightSpec = heightSpec;
    }

    private void startAsyncMeasure() {
        if (sAsyncExecutor != null && !sAsyncExecutor.isShutdown()) {
            sAsyncExecutor.execute(mAsyncMeasure);
        }
    }

    private Runnable mAsyncMeasure = new Runnable() {
        @Override
        public void run() {
            synchronized (mLock) {
                if (mRequestCounter.get() == 0) {
                    return;
                }
                mRequestCounter.set(0);
                if (isMeasureSpecCached()) {
                    measureInner(mCachedWidthSpec, mCachedHeightSpec);
                }
                if (mRequestCounter.get() > 0) {
                    startAsyncMeasure();
                }
            }
        }
    };


    public boolean cacheMeasureSpec(int widthSpec, int heightSpec) {
        int oldWidth = mCachedWidthSpec;
        int oldHeight = mCachedHeightSpec;
        mCachedWidthSpec = widthSpec;
        mCachedHeightSpec = heightSpec;
        return oldWidth != widthSpec || oldHeight != heightSpec;
    }

    public boolean isMeasureSpecCached() {
        return mCachedWidthSpec != Integer.MIN_VALUE && mCachedHeightSpec != Integer.MIN_VALUE;
    }

    public static void setAsyncExecutor(ExecutorService asyncExecutor) {
        sAsyncExecutor = asyncExecutor;
    }
}
