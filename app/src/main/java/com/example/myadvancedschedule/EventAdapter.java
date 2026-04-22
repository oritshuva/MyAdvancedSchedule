package com.example.myadvancedschedule;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Recycler adapter for event rows in schedule/event lists.
// It keeps row rendering lightweight so scrolling remains smooth on long lists.

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private Context context;
    private List<Event> events;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventAdapter(Context context, List<Event> events, OnEventClickListener listener) {
        // Adapter is driven externally by list owner, so no internal data fetching occurs here.
        this.context = context;
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvTitle.setText(event.getTitle());
        holder.tvTime.setText(event.getStartTime() + " - " + event.getEndTime());

        // Status icon communicates completion state at a glance without opening details.
        if (event.isPassed()) {
            holder.ivStatus.setImageResource(R.drawable.ic_check_circle);
        } else {
            holder.ivStatus.setImageResource(R.drawable.ic_circle_outline);
        }

        // Note icon appears only when supplemental context exists, reducing visual noise.
        if (event.getNote() != null && !event.getNote().isEmpty()) {
            holder.ivNote.setVisibility(View.VISIBLE);
        } else {
            holder.ivNote.setVisibility(View.GONE);
        }

        // Reminder icon indicates that a notification has been configured for this event.
        if (event.getReminderTime() != null && !event.getReminderTime().isEmpty()) {
            holder.ivReminder.setVisibility(View.VISIBLE);
        } else {
            holder.ivReminder.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView ivStatus, ivNote, ivReminder;
        TextView tvTitle, tvTime;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStatus = itemView.findViewById(R.id.ivStatus);
            ivNote = itemView.findViewById(R.id.ivNote);
            ivReminder = itemView.findViewById(R.id.ivReminder);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
