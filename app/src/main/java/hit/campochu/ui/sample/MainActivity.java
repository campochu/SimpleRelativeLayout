package hit.campochu.ui.sample;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

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
                mTitle += "哈哈";
                ((TextView) findViewById(R.id.fl_title)).setText(mTitle);
                ((TextView) findViewById(R.id.rl_title)).setText(mTitle);
                ((TextView) findViewById(R.id.ll_title)).setText(mTitle);

            }
        }, 500);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                PerformanceLogger.showStatis();

            }
        }, 1000);
    }
}
