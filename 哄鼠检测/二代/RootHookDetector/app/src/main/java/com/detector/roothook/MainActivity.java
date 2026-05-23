package com.detector.roothook;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.detector.roothook.util.RootDetector;
import com.detector.roothook.util.HookDetector;
import com.detector.roothook.util.EmulatorDetector;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvRootSummary;
    private TextView tvHookSummary;
    private TextView tvEmuSummary;
    private RecyclerView rvRootDetails;
    private RecyclerView rvHookDetails;
    private RecyclerView rvEmuDetails;
    private MaterialCardView cardRoot;
    private MaterialCardView cardHook;
    private MaterialCardView cardEmu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvRootSummary = findViewById(R.id.tv_root_summary);
        tvHookSummary = findViewById(R.id.tv_hook_summary);
        tvEmuSummary = findViewById(R.id.tv_emu_summary);
        rvRootDetails = findViewById(R.id.rv_root_details);
        rvHookDetails = findViewById(R.id.rv_hook_details);
        rvEmuDetails = findViewById(R.id.rv_emu_details);
        cardRoot = findViewById(R.id.card_root);
        cardHook = findViewById(R.id.card_hook);
        cardEmu = findViewById(R.id.card_emu);

        detectEmulator();
        detectRoot();
        detectHook();
    }

    private void detectEmulator() {
        List<EmulatorDetector.DetectionResult> results = EmulatorDetector.detectAll(this);
        boolean isEmulator = false;
        for (EmulatorDetector.DetectionResult r : results) {
            if (r.detected) { isEmulator = true; break; }
        }

        if (isEmulator) {
            tvEmuSummary.setText("⚠ 检测到模拟器环境");
            tvEmuSummary.setTextColor(getColor(android.R.color.holo_orange_dark));
            cardEmu.setCardBackgroundColor(getColor(android.R.color.holo_orange_light));
        } else {
            tvEmuSummary.setText("✅ 真机环境");
            tvEmuSummary.setTextColor(getColor(android.R.color.holo_green_dark));
            cardEmu.setCardBackgroundColor(getColor(android.R.color.holo_green_light));
        }

        rvEmuDetails.setLayoutManager(new LinearLayoutManager(this));
        rvEmuDetails.setAdapter(new DetectionAdapterEmulator(results));
    }

    private void detectRoot() {
        List<RootDetector.DetectionResult> results = RootDetector.detectAll(this);
        boolean isRooted = false;
        for (RootDetector.DetectionResult r : results) {
            if (r.detected) {
                isRooted = true;
                break;
            }
        }

        if (isRooted) {
            tvRootSummary.setText("⚠ 检测到 Root 环境");
            tvRootSummary.setTextColor(getColor(android.R.color.holo_red_dark));
            cardRoot.setCardBackgroundColor(getColor(android.R.color.holo_red_light));
        } else {
            tvRootSummary.setText("✅ 未检测到 Root");
            tvRootSummary.setTextColor(getColor(android.R.color.holo_green_dark));
            cardRoot.setCardBackgroundColor(getColor(android.R.color.holo_green_light));
        }

        rvRootDetails.setLayoutManager(new LinearLayoutManager(this));
        rvRootDetails.setAdapter(new DetectionAdapter(results));
    }

    private void detectHook() {
        List<HookDetector.DetectionResult> results = HookDetector.detectAll(this);
        boolean isHooked = false;
        for (HookDetector.DetectionResult r : results) {
            if (r.detected) {
                isHooked = true;
                break;
            }
        }

        if (isHooked) {
            tvHookSummary.setText("⚠ 检测到 Hook/Xposed 框架");
            tvHookSummary.setTextColor(getColor(android.R.color.holo_red_dark));
            cardHook.setCardBackgroundColor(getColor(android.R.color.holo_red_light));
        } else {
            tvHookSummary.setText("✅ 未检测到 Hook/Xposed");
            tvHookSummary.setTextColor(getColor(android.R.color.holo_green_dark));
            cardHook.setCardBackgroundColor(getColor(android.R.color.holo_green_light));
        }

        rvHookDetails.setLayoutManager(new LinearLayoutManager(this));
        rvHookDetails.setAdapter(new DetectionAdapterHook(results));
    }
}
