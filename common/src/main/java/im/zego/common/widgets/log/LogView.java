package im.zego.common.widgets.log;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import im.zego.common.R;
import im.zego.common.adapter.LogAdapter;
import im.zego.common.util.AppLogger;
import im.zego.common.util.CpuUtil;


public class LogView extends FrameLayout {

    private static final String LF = "\n";
    private RecyclerView recyclerView;
    public LogAdapter logAdapter;
    private TextView txCpuInfo, cpuClockFrequency;
    private String eachCpuInfo = "[ CPU\t%d\t\t当前频率\t%s\t\t最大频率\t%s\t\t最小频率\t%s ]" + LF;
    private StringBuffer mCPUHeaderText;
    private StringBuffer mPowerHeaderText;
    private int mCPUCoreNum;
    private List<String> cpuMaxList = new ArrayList<String>();
    private List<String> cpuMinList = new ArrayList<String>();

    public LogView(@NonNull Context context) {
        super(context, null);
        inflate(context, R.layout.log_view, this);
        recyclerView = findViewById(R.id.logListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        logAdapter = new LogAdapter();
        recyclerView.setAdapter(logAdapter);
        txCpuInfo = findViewById(R.id.tx_cpu_info);
        cpuClockFrequency = findViewById(R.id.cpu_clock_frequency);
        Button button = findViewById(R.id.clear_log);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AppLogger.getInstance().clearLog();
            }
        });
        Button shareBtn = findViewById(R.id.share_log);
        // initial CPU header text
        mCPUHeaderText = new StringBuffer();
        mCPUCoreNum = CpuUtil.getNumCores();
        mCPUHeaderText.append("CPU 核心数:" + mCPUCoreNum + LF);
        mCPUHeaderText.append("[ (KHz)\t\t\t\t\t\t\t\tCurrent\t\t\t\t\t\t\t\t\t\t\tMax\t\t\t\t\t\t\t\t\t\t\t\t\tMin\t\t\t\t\t\t\t\t]" + LF);
        for (int i = 0; i < mCPUCoreNum; i++) {
            cpuMaxList.add(getKHz(CpuUtil.getMaxCpuFreq(i)));
            cpuMinList.add(getKHz(CpuUtil.getMinCpuFreq(i)));
        }

        // initial Power header text
        mPowerHeaderText = new StringBuffer();
        mPowerHeaderText.append("----------" + LF);
        mPowerHeaderText.append("Battery INFO" + LF);
    }

    private String getKHz(String hzStr) {
        try {
            int hz = Integer.parseInt(hzStr);
            DecimalFormat df = new DecimalFormat("###mhz");
            hzStr = df.format(hz / 1000);
            int hzStrLength = hzStr.length();
            if (hzStrLength <= 6) {
                int length = 7 - hzStrLength;
                for (int i = 0; i < length; i++) {
                    hzStr = "\t" + hzStr;
                }
            }
            return hzStr;
        } catch (Exception e) {
            return "\tstopped ";
        }
    }

    /**
     * 更新视窗 cpu信息
     */
    public void setTxCpuInfo(String cpuInfo) {
        if (txCpuInfo != null) {
            txCpuInfo.setText(cpuInfo);
        }
        // 更新cpu时钟频率
        updateCpuFrequency();
    }

    private void updateCpuFrequency() {
        StringBuffer sb = new StringBuffer();

        sb.append(mCPUHeaderText.toString());
        for (int i = 0; i < mCPUCoreNum; i++) {
            sb.append(String.format(eachCpuInfo, i + 1,
                    getKHz(CpuUtil.getCurCpuFreq(i)),
                    cpuMaxList.get(i),
                    cpuMinList.get(i)
            ));
        }
        // 同时更新cpu频率信息
        if (cpuClockFrequency != null) {
            cpuClockFrequency.setText(sb.toString());
        }
    }

    /**
     * 日志列表滚动到最底部。
     */
    public void scrollToBottom() {
        if (recyclerView != null) {
            int itemCount = logAdapter.getItemCount();
            if (itemCount > 10)
                recyclerView.scrollToPosition(itemCount - 1);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }


}
