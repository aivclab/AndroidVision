package dk.aivclab.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import org.pytorch.Module;

import java.util.concurrent.Executor;

import static dk.aivclab.demo.ProjectConstants.PROJECT_TAG;

public abstract class TorchModuleActivity extends AppCompatActivity {
    private static final int UNSET = 0;

    protected HandlerThread mBackgroundThread;
    protected Executor mBackgroundExecutor;
    protected Handler mUIHandler;

    protected TextView mFpsText;
    protected TextView mMsText;
    protected TextView mMsAvgText;
    protected Module mTorchModule;

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("ModuleActivity");
        mBackgroundThread.start();
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
        } catch (InterruptedException e) {
            Log.e(PROJECT_TAG, "Error on stopping background thread", e);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getContentViewLayoutId());

        mFpsText = findViewById(R.id.activity_fps_text);
        mMsText = findViewById(R.id.activity_ms_text);
        mMsAvgText = findViewById(R.id.activity_ms_avg_text);

        mUIHandler = new Handler(getMainLooper());
        //mBackgroundExecutor = new DirectExecutor();
        mBackgroundExecutor = ContextCompat.getMainExecutor(this);
        //mBackgroundExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        //startBackgroundThread();
    }

    @Override
    protected void onDestroy() {
        //mBackgroundExecutor.shutdown();
        mBackgroundExecutor = null;
        //stopBackgroundThread();
        super.onDestroy();
        if (mTorchModule != null) {
            mTorchModule.destroy();
        }
    }

    protected abstract int getContentViewLayoutId();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_model, menu);
        menu.findItem(R.id.action_info).setVisible(getInfoViewCode() != UNSET);
        return true;
    }

    protected int getInfoViewCode() {
        return UNSET;
    }
}
