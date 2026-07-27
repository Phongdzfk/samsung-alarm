package com.example.samsung_alarm.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Handler;
import android.os.Looper;
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
    private final Handler clockHandler=new Handler(Looper.getMainLooper());
    private final Runnable clockTick=new Runnable(){@Override public void run(){notifyItemRangeChanged(0,getItemCount(),"clock");scheduleNextClockTick();}};
    private List<Alarm> items=new ArrayList<>();
    QuickAlarmAdapter(Listener listener){this.listener=listener;}
    void submit(List<Alarm> alarms){items=alarms;notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quick_alarm,parent,false));}
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){Alarm alarm=items.get(position);long remaining=Math.max(0,(alarm.triggerAtMillis-System.currentTimeMillis()+59_999L)/60_000L);holder.title.setText(alarm.label);holder.detail.setText(holder.itemView.getContext().getString(R.string.quick_alarm_detail,DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(alarm.triggerAtMillis)),remaining));holder.delete.setOnClickListener(v->listener.delete(alarm));}
    @Override public int getItemCount(){return items.size();}
    @Override public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView){super.onAttachedToRecyclerView(recyclerView);scheduleNextClockTick();}
    @Override public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView){clockHandler.removeCallbacks(clockTick);super.onDetachedFromRecyclerView(recyclerView);}
    private void scheduleNextClockTick(){clockHandler.removeCallbacks(clockTick);long delay=1_000L-(System.currentTimeMillis()%1_000L)+20L;clockHandler.postDelayed(clockTick,delay);}
    static final class Holder extends RecyclerView.ViewHolder{final TextView title,detail;final ImageButton delete;Holder(View view){super(view);title=view.findViewById(R.id.quickItemTitle);detail=view.findViewById(R.id.quickItemDetail);delete=view.findViewById(R.id.quickItemDelete);}}
}
