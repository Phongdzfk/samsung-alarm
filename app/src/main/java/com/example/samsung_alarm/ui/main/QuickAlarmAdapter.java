package com.example.samsung_alarm.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.samsung_alarm.R;
import com.example.samsung_alarm.data.model.Alarm;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class QuickAlarmAdapter extends RecyclerView.Adapter<QuickAlarmAdapter.Holder> {
    interface Listener { void delete(Alarm alarm); }
    private final Listener listener;
    private List<Alarm> items=new ArrayList<>();
    QuickAlarmAdapter(Listener listener){this.listener=listener;}
    void submit(List<Alarm> alarms){items=alarms;notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quick_alarm,parent,false));}
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){Alarm alarm=items.get(position);long remaining=Math.max(0,(alarm.triggerAtMillis-System.currentTimeMillis()+59_999L)/60_000L);holder.title.setText(alarm.label);holder.detail.setText(holder.itemView.getContext().getString(R.string.quick_alarm_detail,DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(alarm.triggerAtMillis)),remaining));holder.delete.setOnClickListener(v->listener.delete(alarm));}
    @Override public int getItemCount(){return items.size();}
    static final class Holder extends RecyclerView.ViewHolder{final TextView title,detail;final ImageButton delete;Holder(View view){super(view);title=view.findViewById(R.id.quickItemTitle);detail=view.findViewById(R.id.quickItemDetail);delete=view.findViewById(R.id.quickItemDelete);}}
}
