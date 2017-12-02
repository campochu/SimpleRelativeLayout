package hit.campochu.ui.performance;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by ckb on 17/10/10.
 */

public class MyLinearLayout extends LinearLayout {

    public MyLinearLayout(Context context) {
        this(context, null);
    }

    public MyLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        PerformanceLogger.get("LL_Measure").start();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        PerformanceLogger.get("LL_Measure").end();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        PerformanceLogger.get("LL_Layout").start();
        super.onLayout(changed, l, t, r, b);
        PerformanceLogger.get("LL_Layout").end();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        PerformanceLogger.get("LL_Draw").start();
        super.onDraw(canvas);
        PerformanceLogger.get("LL_Draw").end();
    }
}
