package im.zego.common.util;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by cc on 2019/03/08.
 * <p>
 * AppLogger
 * <p>
 * 管理app日志, 通过 {@link #i(Class, String, Object...)}
 * 输出日志到悬浮窗口。
 * <p>
 * 用法如下:
 * <p>
 * AppLogger.getInstance().i(AppLogger.class, "test out info log");
 * <p>
 * AppLogger.getInstance().e(AppLogger.class, "test out error log");
 * <p>
 * AppLogger.getInstance().w(AppLogger.class, "test out warn log");
 * <p>
 * AppLogger.getInstance().d(AppLogger.class, "test out debug log");
 */
public class AppLogger {

    private static final String TAG = "AppLogger";

    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    static private AppLogger sInstance;

    static public AppLogger getInstance() {
        if (sInstance == null) {
            synchronized (AppLogger.class) {
                if (sInstance == null) {
                    sInstance = new AppLogger();
                }
            }
        }
        return sInstance;
    }

    public void e(String msgFormat, Object... args) {
        String message = String.format(msgFormat, args);
        log(message, LogLevel.ERROR);
    }

    public void i(String msgFormat, Object... args) {
        String message = String.format(msgFormat, args);
        log(message, LogLevel.INFO);
    }

    public void w(String msgFormat, Object... args) {
        String message = String.format(msgFormat, args);
        log(message, LogLevel.WARN);
    }

    public void d(String msgFormat, Object... args) {
        String message = String.format(msgFormat, args);
        log(message, LogLevel.DEBUG);
    }

    private void log(String message, LogLevel logLevel) {
        String message_with_time = String.format("[ %s ][ %s ] %s", TimeUtil.getLogStr(), logLevel.name(), message);
        switch (logLevel) {
            case INFO:
                Log.i(TAG, message_with_time);
                break;
            case WARN:
                Log.w(TAG, message_with_time);
                break;
            case DEBUG:
                Log.d(TAG, message_with_time);
                break;
            case ERROR:
                Log.d(TAG, message_with_time);
                break;
        }

        // 之所以通过反射来调用，是为了避免客户在copy代码的时候还需要依赖一堆悬浮日志视图的代码。
        // The reason why it is called by reflection is to avoid customers needing to rely on a bunch of floating log view code when copying code.
        try {
            Class<?> floatingView = Class.forName("im.zego.common.widgets.log.FloatingView");
            Method method = floatingView.getMethod("get");
            Object object = method.invoke(null);
            Method addLogMethod = object.getClass().getMethod("addLog", String.class);
            addLogMethod.invoke(object, message_with_time);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void clearLog() {
        try {
            Class<?> floatingView = Class.forName("im.zego.common.widgets.log.FloatingView");
            Method method = floatingView.getMethod("get");
            Object object = method.invoke(null);
            Method addLogMethod = object.getClass().getMethod("clearLog");
            addLogMethod.invoke(object);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
