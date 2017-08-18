package com.zego.livedemo5.testPerformance;

import com.zego.livedemo5.PerformanceInstrumentedTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Created by cier on 2017/5/22.
 * 性能检查
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({PerformanceInstrumentedTest.class})
public class PerformanceTest {
    public final static int RUNTIME=20;
}
