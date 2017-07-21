package com.zego.livedemo5;

import android.app.Activity;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.zego.livedemo5.tools.CommonTools.sleep;
import static com.zego.livedemo5.tools.PermissionTool.allowPermissionIfNeed;

/**
 * Created by cier on 2017/5/26.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ClientJoinTest {
    public static String clientJoinRoomName="testClientJoinRoom";
    private Activity mActivity;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule=new ActivityTestRule<MainActivity>(MainActivity.class);

    @BeforeClass
    public static void init(){

    }

    @AfterClass
    public static void unInit(){

    }

    @Before
    public void setup(){
        mActivity=mActivityRule.getActivity();
        onView(withText(mActivity.getResources().getStringArray(R.array.navigation_bar_titles)[1])).perform(click());
//        allowPermissionIfNeed();
        sleep(30000);//暂停30s手动点击获取权限，否则部分手机会因无法获取权限而退出
    }

    @Test
    public void start(){
//        test1(11);
//        test2(11);
        test3(11);
    }

    public void test1(int minute){
        //进入直播间默认设置（摄像头和麦克风都开）
        startPublish(clientJoinRoomName,R.id.tv_select_single_anchor);
        sleep(60*1000*minute);
        endPublish();
    }

    public void test2(int minute){
        //进入直播间仅开启视频
        startPublish(clientJoinRoomName,R.id.tv_select_single_anchor);
        sleep(10000);
        openOrCloseMicrophone();
        sleep(60*1000*minute);
        endPublish();
    }

    public void test3(int minute){
        //进入直播间仅开启麦克风
        startPublish(clientJoinRoomName,R.id.tv_select_single_anchor);
        sleep(10000);
        openOrCloseCamera();
        sleep(60*1000*minute);
        endPublish();
    }

    /**
     * 麦克风默认是打开的
     */
    public void openOrCloseMicrophone(){
        onView(withId(R.id.tv_publish_settings)).perform(click());
        sleep(1000);//点击设置面板上的控件之前需要等待settingPanel动画的完成
        onView(withId(R.id.tb_mic)).perform(click());
        onView(withId(R.id.tv_publish_settings)).perform(click());
    }

    /**
     * 默认情况下是打开前置摄像头的
     */
    public void openOrCloseCamera(){
        onView(withId(R.id.tv_publish_settings)).perform(click());
        sleep(1000);
        onView(withId(R.id.tb_camera)).perform(click());
        onView(withId(R.id.tb_front_cam)).perform(click());
        onView(withId(R.id.tv_publish_settings)).perform(click());
    }

    public void startPublish(String publishName,int publishType){
        //设置房间名
        onView(withId(R.id.et_publish_title)).perform(clearText(),replaceText(publishName), closeSoftKeyboard());
        sleep(1000);
        //点击开始按钮
        onView(withId(R.id.btn_start_publish)).perform(click());
        sleep(1000);
        //选择主播类型
        onView(withId(publishType)).perform(click());
    }

    public void endPublish(){
        onView(withId(R.id.tv_close)).perform(click());
        onView(withText(R.string.Yes)).perform(click());
    }
}
