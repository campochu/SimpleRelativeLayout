package hit.campochu.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

import hit.campochu.ui.performance.PerformanceLogger;

/**
 * Created by ckb on 17/12/7.
 */

public class RecommendationLayout extends SimpleRelativeLayout {

    public RecommendationLayout(Context context) {
        super(context);
    }

    public RecommendationLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecommendationLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        PerformanceLogger.get("FL_Measure").start();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        PerformanceLogger.get("FL_Measure").end();
    }

    @Override
    protected void onFreeStyle(int width, int height) {
        layoutVerticalCenter(new Rect(0, 0, width, height),
                R.id.fl_sub_title_logo,
                R.id.fl_sub_title,
                R.id.fl_title,
                R.id.fl_third_title);
    }

}
