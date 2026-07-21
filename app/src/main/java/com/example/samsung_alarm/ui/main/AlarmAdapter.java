package com.example.samsung_alarm.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.samsung_alarm.data.model.Alarm;
import com.example.samsung_alarm.R;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.Holder> {
    public interface Listener { void edit(Alarm alarm); void toggle(Alarm alarm, boolean active); void delete(Alarm alarm); void skipNext(Alarm alarm); }
    private final Listener listener;
    private List<Alarm> alarms = new ArrayList<>();
    public AlarmAdapter(Listener listener) { this.listener = listener; }
    public void submit(List<Alarm> value) { alarms = value == null ? new ArrayList<>() : value; notifyDataSetChanged(); }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Alarm a = alarms.get(position);
        h.time.setText(String.format(Locale.getDefault(), "%02d:%02d", a.hour, a.minute));
        h.label.setText(a.label == null || a.label.trim().isEmpty() ? h.itemView.getContext().getString(R.string.alarm) : a.label);
        String days = days(h.itemView, a);
        if (a.skipUntilMillis > System.currentTimeMillis()) days += "  ·  " + h.itemView.getContext().getString(R.string.skipped_badge);
        h.days.setText(days);
        h.active.setOnCheckedChangeListener(null);
        h.active.setChecked(a.isActive);
        h.active.setOnCheckedChangeListener((button, checked) -> listener.toggle(a, checked));
        h.content.setOnClickListener(v -> listener.edit(a));
        h.delete.setOnClickListener(v -> listener.delete(a));
        h.skip.setVisibility(a.repeats() && a.isActive ? View.VISIBLE : View.GONE);
        h.skip.setContentDescription(h.itemView.getContext().getString(a.skipUntilMillis > System.currentTimeMillis() ? R.string.undo_skip : R.string.skip_next));
        h.skip.setAlpha(a.skipUntilMillis > System.currentTimeMillis() ? .45f : 1f);
        h.skip.setOnClickListener(v -> listener.skipNext(a));
        h.itemView.setAlpha(a.isActive ? 1f : .55f);
    }
    private String days(View view, Alarm a) {
        if (!a.repeats()) return view.getContext().getString(R.string.one_time);
        if (a.mon && a.tue && a.wed && a.thu && a.fri && a.sat && a.sun) return view.getContext().getString(R.string.every_day);
        StringBuilder s = new StringBuilder();
        if (a.mon) s.append(view.getContext().getString(R.string.day_mon)).append("  "); if (a.tue) s.append(view.getContext().getString(R.string.day_tue)).append("  "); if (a.wed) s.append(view.getContext().getString(R.string.day_wed)).append("  ");
        if (a.thu) s.append(view.getContext().getString(R.string.day_thu)).append("  "); if (a.fri) s.append(view.getContext().getString(R.string.day_fri)).append("  "); if (a.sat) s.append(view.getContext().getString(R.string.day_sat)).append("  "); if (a.sun) s.append(view.getContext().getString(R.string.day_sun));
        return s.toString().trim();
    }
    @Override public int getItemCount() { return alarms.size(); }
    static class Holder extends RecyclerView.ViewHolder {
        TextView time, label, days; MaterialSwitch active; ImageButton delete, skip; View content;
        Holder(View v) { super(v); time=v.findViewById(R.id.itemTime); label=v.findViewById(R.id.itemLabel); days=v.findViewById(R.id.itemDays); active=v.findViewById(R.id.itemSwitch); delete=v.findViewById(R.id.itemDelete); skip=v.findViewById(R.id.itemSkip); content=v.findViewById(R.id.itemContent); }
    }
}
