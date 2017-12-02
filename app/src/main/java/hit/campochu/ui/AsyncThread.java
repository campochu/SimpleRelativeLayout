package hit.campochu.ui;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Created by ckb on 17/11/7.
 */

public class AsyncThread extends HandlerThread {

    private Handler mHandler = null;

    public AsyncThread(String name) {
        super(name);
    }

    public AsyncThread(String name, int priority) {
        super(name, priority);
    }

    public void post(Runnable task) {
        getHandler().post(task);
    }

    public void postDelay(Runnable task, long delay) {
        getHandler().postDelayed(task, delay);
    }

    public void remove(Runnable task) {
        getHandler().removeCallbacks(task);
    }

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(getLooper());
        }
        return mHandler;
    }


}
