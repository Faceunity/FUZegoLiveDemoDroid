package com.zego.livedemo5;

import android.app.Activity;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.zego.livedemo5.tools.CommonTools.printLog;
import static com.zego.livedemo5.tools.CommonTools.sleep;
import static com.zego.livedemo5.tools.PermissionTool.allowPermissionIfNeed;

/**
 * Created by cier on 2017/5/23.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ConnectToMoreAnchorTest {
    private Activity mActivity;

    @Rule
    public ActivityTestRule<MainActivity> mMainActicity=new ActivityTestRule<MainActivity>(MainActivity.class);

    @BeforeClass
    public static void init(){
        printLog("ConnectToMoreAnchorTest->init()");
    }

    @AfterClass
    public static void unInit(){
        printLog("ConnectToMoreAnchorTest->uInit()");
    }

    @Before
    public void setup(){
        printLog("ConnectToMoreAnchorTest->setup()");
        mActivity=mMainActicity.getActivity();
//        sleep(30000);//暂停30s手动点击获取权限，否则部分手机会因无法获取权限而退出
    }

    @Test
    public void testStart(){
        joinToMoreAnchor(10);
    }

    public void joinToMoreAnchor(int minute){
        onView(withText(PerformanceInstrumentedTest.moreAnchorRoomName)).perform(click());
        sleep(10000);
        onView(withId(R.id.tv_publish_control)).perform(click());
        sleep(60*1000*minute);
    }

}
