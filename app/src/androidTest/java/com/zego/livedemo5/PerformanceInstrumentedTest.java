package com.zego.livedemo5;

import android.app.Activity;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.ViewActions;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.zego.livedemo5.tools.CommonTools.printLog;
import static com.zego.livedemo5.tools.CommonTools.sleep;
import static com.zego.livedemo5.tools.PermissionTool.allowPermissionIfNeed;

/**
 * Created by cier on 2017/5/22.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PerformanceInstrumentedTest {

    public static String moreAnchorRoomName="testMoreAnchor";
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
    public void startTest(){
        testMoreAnchor(12);
    }

    /**
     * 开启一个连麦房间并等待加入
     * @param minute
     */
    public void testMoreAnchor(int minute){
        printLog("testMoreAnchor()");
        startPublish(moreAnchorRoomName,R.id.tv_select_more_anchors);
//        onView(withText(R.string.hint)).inRoot(isDialog()).check(matches(isDisplayed()));
//        waitForDialog(mActivity);
        sleep(60*1000*minute);
        endPublish();
    }

    public void startPublish(String publishName,int publishType){
        //设置房间名
        onView(withId(R.id.et_publish_title)).perform(clearText(),replaceText(publishName), closeSoftKeyboard());
        //点击开始按钮
        onView(withId(R.id.btn_start_publish)).perform(click());
        sleep(1000);
        //选择主播类型
        onView(withId(publishType)).perform(click());
    }

    public void endPublish(){
//        pressBack();
        onView(withId(R.id.tv_close)).perform(click());
        onView(withText(R.string.Yes)).perform(click());
    }

    public static boolean waitForDialog(Context context){
        UiDevice uiDevice=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiObject dialog=uiDevice.findObject(new UiSelector().className("android.widget.TextView").text(context.getString(R.string.hint)).resourceId(context.getPackageName()+":id/alertTitle"));
        while(true){
            if(dialog.exists()){
                break;
            }
        }
        onView(withText("同意")).perform(click());
        return true;
    }

    public static boolean waitForPublishSuccess(long timeout, Context context) {
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiObject settingTab = uiDevice.findObject(new UiSelector().className("android.widget.TextView").text(context.getString(R.string.publish_setting)).resourceId(context.getPackageName() + ":id/tv_publish_settings"));
        long usedTime = 0;
        boolean isTimeout = true;
        while (usedTime <= timeout) {
            try {
                if (settingTab.isEnabled()) {
                    isTimeout = false;
                    printLog("publish success, interrupt waiting");
                    break;
                }

                printLog("waiting for publish success");
            } catch (UiObjectNotFoundException e) {
                printLog("exception: %s", e);
            }

            sleep(500);
            usedTime += 500;
        }
        return isTimeout;
    }

}
