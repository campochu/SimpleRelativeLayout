package hit.campochu.ui.sample;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.Executors;

import hit.campochu.ui.AsyncViewGroup;
import hit.campochu.ui.R;
import hit.campochu.ui.performance.PerformanceLogger;


public class MainActivity extends AppCompatActivity {

    private String mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTitle = "Hello World!";
        findViewById(R.id.frame_layout_root).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postChange();
            }
        });
        AsyncViewGroup.setAsyncExecutor(Executors.newCachedThreadPool());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        PerformanceLogger.showStatis();
    }

    private void postChange() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                testFlCase();
            }
        }, 500);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                PerformanceLogger.showStatis();

            }
        }, 1000);
    }

    private void testFlCase() {
        mTitle += "哈哈";
        ((TextView) findViewById(R.id.test1).findViewById(R.id.fl_title)).setText(mTitle);
        ((TextView) findViewById(R.id.test2).findViewById(R.id.fl_title)).setText(mTitle);
        ((TextView) findViewById(R.id.test3).findViewById(R.id.fl_title)).setText(mTitle);
//        inflateStatis();
    }

    private void testRlCase() {
        mTitle += "哈哈";
        ((TextView) findViewById(R.id.test4).findViewById(R.id.rl_title)).setText(mTitle);
        ((TextView) findViewById(R.id.test5).findViewById(R.id.rl_title)).setText(mTitle);
        ((TextView) findViewById(R.id.test6).findViewById(R.id.rl_title)).setText(mTitle);
//        inflateStatis();
    }

    private void testLlRlFlCase() {
        mTitle += "哈哈";
        ((TextView) findViewById(R.id.fl_title)).setText(mTitle);
        ((TextView) findViewById(R.id.rl_title)).setText(mTitle);
        ((TextView) findViewById(R.id.ll_title)).setText(mTitle);
//        inflateStatis();
    }

    private void inflateStatis() {
        PerformanceLogger.get("Linear_Measure_Inflate").start();
        getLayoutInflater().inflate(R.layout.linear_layout_demo, null);
        PerformanceLogger.get("Linear_Measure_Inflate").end();
        PerformanceLogger.get("Relative_Measure_Inflate").start();
        getLayoutInflater().inflate(R.layout.relative_layout_demo, null);
        PerformanceLogger.get("Relative_Measure_Inflate").end();
        PerformanceLogger.get("Flat_Measure_Inflate").start();
        getLayoutInflater().inflate(R.layout.fl_layout_demo, null);
        PerformanceLogger.get("Flat_Measure_Inflate").end();
    }
}
