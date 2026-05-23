package com.detector.roothook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.detector.roothook.util.EmulatorDetector;
import java.util.List;

public class DetectionAdapterEmulator extends RecyclerView.Adapter<DetectionAdapterEmulator.ViewHolder> {

    private final List<EmulatorDetector.DetectionResult> results;

    public DetectionAdapterEmulator(List<EmulatorDetector.DetectionResult> results) {
        this.results = results;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_detection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmulatorDetector.DetectionResult result = results.get(position);
        holder.tvName.setText(result.name);
        holder.tvDetail.setText(result.detail);
        if (result.detected) {
            holder.tvStatus.setText("⚠ 异常");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_orange_dark));
        } else {
            holder.tvStatus.setText("✅ 正常");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetail, tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDetail = itemView.findViewById(R.id.tv_detail);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}
