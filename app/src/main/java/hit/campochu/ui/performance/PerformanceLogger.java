package hit.campochu.ui.performance;

import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import hit.campochu.ui.BuildConfig;

/**
 * Created by ckb on 17/9/29.
 */

public class PerformanceLogger {

    private final static String LOG_TAG = "SocialSdk_Performance";
    private final static boolean ENABLE = BuildConfig.DEBUG;

    private static DecimalFormat sMillisFormatter = new DecimalFormat("###.###ms");
    private static Map<String, PerformanceLogger> sPool = new HashMap<String, PerformanceLogger>();

    private SparseArray<Long> mStartRecord;
    private SparseArray<Long> mEndRecord;
    private AtomicInteger mCounter;

    private String mName;
    private long mWarnBase;
    private long mErrBase;

    private PerformanceLogger(String name) {
        mName = name;
        if (ENABLE) {
            mCounter = new AtomicInteger();
            mStartRecord = new SparseArray<Long>();
            mEndRecord = new SparseArray<Long>();
            mWarnBase = mErrBase = Long.MAX_VALUE;
        }
    }

    public static PerformanceLogger get(String id) {
        PerformanceLogger duration = sPool.get(id);
        if (duration == null) {
            duration = new PerformanceLogger(id);
            sPool.put(id, duration);
        }
        return duration;
    }

    public static void showStatis() {
        for (PerformanceLogger logger : sPool.values()) {
            logger.show();
        }
    }

    public static void clear() {
        sPool.clear();
    }

    public PerformanceLogger start() {
        if (ENABLE) {
            mStartRecord.append(mCounter.incrementAndGet(), SystemClock.elapsedRealtimeNanos());
        }
        return this;
    }

    public PerformanceLogger end() {
        if (ENABLE) {
            mEndRecord.append(mCounter.get(), SystemClock.elapsedRealtimeNanos());
        }
        return this;
    }

    public PerformanceLogger wran(long millis) {
        if (ENABLE) {
            mWarnBase = millis * 1000000;
        }
        return this;
    }

    public PerformanceLogger err(long millis) {
        if (ENABLE) {
            mErrBase = millis * 1000000;
        }
        return this;
    }

    public PerformanceLogger show() {
        if (ENABLE) {
            int size = mEndRecord.size();
            if (size == 0) {
                return this;
            }

            if (size == 1) {
                long duration = getDuration(0);
                Log.println(level(duration), LOG_TAG, mName + " 耗时：" + millis(duration));
                return this;
            }

            long total = 0, average, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            for (int i = 0; i < size; ++i) {
                long duration = getDuration(i);
                max = max < duration ? duration : max;
                min = min > duration ? duration : min;
                total += duration;
            }
            average = total / size;
            Log.println(level(average), LOG_TAG, mName
                    + " 总耗时：" + millis(total)
                    + ",执行次数：" + size
                    + ",最多耗时：" + millis(max)
                    + ",最少耗时：" + millis(min)
                    + ",平均耗时：" + millis(average));
            return this;
        }
        return this;
    }

    public PerformanceLogger reset() {
        if (ENABLE) {
            mStartRecord.clear();
            mEndRecord.clear();
            mWarnBase = mErrBase = Long.MAX_VALUE;
        }
        return this;
    }

    private long getDuration(int index) {
        return mEndRecord.valueAt(index) - mStartRecord.get(mEndRecord.keyAt(index));
    }

    private int level(long duration) {
        if (duration > mErrBase) {
            return Log.ERROR;
        } else if (duration > mWarnBase) {
            return Log.WARN;
        } else {
            return Log.DEBUG;
        }
    }

    private static String millis(long nanoTime) {
        return sMillisFormatter.format(nanoTime * 0.000001D);
    }
}
