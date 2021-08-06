package im.zego.common.ui;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.ButterKnife;


/**
 * des: activity 基类
 */
public abstract class AbsBaseActivity extends AppCompatActivity {

    protected Resources mResources;

    protected Handler mHandler;

    protected ProgressDialog mProgressDialog;

    /**
     * 获取内容页面的布局.
     *
     * @return 返回内容页面的布局
     */
    /**
     *      * Get the layout of the content page.
     *      *
     *      * @return returns the layout of the content page
     *      
     */
    protected abstract int getContentViewLayout();

    /**
     * 初始化从外部传递过来的数据. Initialize data passed from outside
     */
    protected abstract void initExtraData(Bundle savedInstanceState);

    /**
     * 初始化子类中的变量. Initialize variables in subclasses
     */
    protected abstract void initVariables(Bundle savedInstanceState);

    /**
     * 初始化子类中的控件. Initialize controls in subclasses
     */
    protected abstract void initViews(Bundle savedInstanceState);

    /**
     * 加载数据. Download Data
     */
    protected abstract void loadData(Bundle savedInstanceState);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewLayout());

        // 初始化butternife
        // Initialize butternife
        ButterKnife.bind(this);

        initExtraData(savedInstanceState);
        initBaseVariables();
        initVariables(savedInstanceState);
        initViews(savedInstanceState);
        loadData(savedInstanceState);
    }

    /**
     * 初始化基类中的变量. Initialize variables in the base class
     */
    private void initBaseVariables() {
        mResources = getResources();
        mHandler = new Handler();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    }


}
