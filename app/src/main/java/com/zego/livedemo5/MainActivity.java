package com.zego.livedemo5;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


import com.tencent.tauth.Tencent;
import com.zego.livedemo5.ui.activities.AboutZegoActivity;
import com.zego.livedemo5.ui.activities.base.AbsBaseActivity;
import com.zego.livedemo5.ui.activities.base.AbsBaseFragment;
import com.zego.livedemo5.ui.fragments.PublishFragment;
import com.zego.livedemo5.ui.fragments.RoomListFragment;
import com.zego.livedemo5.ui.widgets.NavigationBar;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

/**
 * Copyright © 2016 Zego. All rights reserved.
 * des:
 */
public class MainActivity extends AbsBaseActivity implements NavigationBar.NavigationBarListener{

    private List<AbsBaseFragment> mFragments;

    private FragmentPagerAdapter mPagerAdapter;

    private int mTabSelected;

    @Bind(R.id.toolbar)
    public Toolbar toolBar;

    @Bind(R.id.drawerlayout)
    public DrawerLayout drawerLayout;

    private OnSetConfigsCallback mSetConfigsCallback;

    @Bind(R.id.nb)
    public NavigationBar navigationBar;

    @Bind(R.id.vp)
    public ViewPager viewPager;


    @Override
    protected int getContentViewLayout() {
        return R.layout.acvitity_main;
    }

    @Override
    protected void initExtraData(Bundle savedInstanceState) {

    }

    @Override
    protected void initVariables(Bundle savedInstanceState) {
        mTabSelected = 0;
        mFragments = new ArrayList<>();
        mFragments.add(RoomListFragment.newInstance());
        mFragments.add(PublishFragment.newInstance());

        mPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mFragments.get(position);
            }

            @Override
            public int getCount() {
                return mFragments.size();
            }
        };

        mSetConfigsCallback = (OnSetConfigsCallback) getSupportFragmentManager().findFragmentById(R.id.setting_fragment);

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {

            private CharSequence oldTitle;

            private Runnable updateTitleTask = new Runnable() {
                @Override
                public void run() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateTitle();

                            for (int position = 0; position < mPagerAdapter.getCount(); position++) {
                                Fragment fragment = mPagerAdapter.getItem(position);
                                ((OnReInitSDKCallback)fragment).onReInitSDK();
                            }
                        }
                    });
                }
            };

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                oldTitle = toolBar.getTitle();
                toolBar.setTitle(getString(R.string.action_settings));
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                // 当侧边栏关闭时, set配置
                if(mSetConfigsCallback == null) return;

                int errorCode = mSetConfigsCallback.onSetConfig();
                if (errorCode < 0) {
                    drawerLayout.openDrawer(Gravity.LEFT);
                } else if (errorCode > 0) {
                    if (updateTitleTask != null) {
                        ZegoAppHelper.removeTask(updateTitleTask);
                    }
                    ZegoAppHelper.postTask(updateTitleTask);
                } else {
                    toolBar.setTitle(oldTitle);
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        setSupportActionBar(toolBar);
        toolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                    drawerLayout.closeDrawer(Gravity.LEFT);
                } else {
                    drawerLayout.openDrawer(Gravity.LEFT);
                }
            }
        });
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        navigationBar.setNavigationBarListener(this);
        navigationBar.selectTab(0);

        viewPager.setAdapter(mPagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mTabSelected = position;
                navigationBar.selectTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        updateTitle();
    }

    @Override
    protected void loadData(Bundle savedInstanceState) {

    }

    @Override
    public void onTabSelect(int tabIndex) {
        mTabSelected = tabIndex;
        viewPager.setCurrentItem(tabIndex, true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void updateTitle() {
        long currentAppId = ZegoApiManager.getInstance().getAppID();
        String title = ZegoAppHelper.getAppTitle(currentAppId, MainActivity.this);
        toolBar.setTitle(title);
    }

    /**
     * 用户连续点击两次返回键可以退出应用的时间间隔.
     */
    public static final long EXIT_INTERVAL = 1000;

    private long mBackPressedTime;

    /**
     * 退出.
     */
    private void exit() {
        /* 连按两次退出 */
        long currentTime = System.currentTimeMillis();
        if (currentTime - mBackPressedTime > EXIT_INTERVAL) {
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            mBackPressedTime = currentTime;
        } else {
            // 释放Zego sdk
            ZegoApiManager.getInstance().releaseSDK();
            System.exit(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.action_contact_us){
            Tencent.createInstance("", MainActivity.this).startWPAConversation(MainActivity.this, "84328558", "");
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            AboutZegoActivity.actionStart(MainActivity.this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public interface OnSetConfigsCallback {

        /**
         * Setting 页面关闭时调用
         *
         * @return < 0: 数据格式非法; 0: 无修改或者不需要重新初始化SDK; > 0: 需要重新初始化 SDK
         */
        int onSetConfig();
    }

    public interface OnReInitSDKCallback {

        /**
         * 当重新 initSDK 时调用
         */
        void onReInitSDK();
    }
}
