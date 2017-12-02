package hit.campochu.ui.performance;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * Created by ckb on 17/10/10.
 */

public class MyRelativeLayout extends RelativeLayout {

    public MyRelativeLayout(Context context) {
        this(context, null);
    }

    public MyRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        PerformanceLogger.get("RL_Measure").start();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        PerformanceLogger.get("RL_Measure").end();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        PerformanceLogger.get("RL_Layout").start();
        super.onLayout(changed, l, t, r, b);
        PerformanceLogger.get("RL_Layout").end();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
