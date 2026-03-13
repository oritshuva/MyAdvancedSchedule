package com.example.myadvancedschedule;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvCurrentDay, tvCurrentDate;
    private Button btnPrevDay, btnNextDay, btnAddSchool, btnAddAfter, btnInviteFriend;
    private RecyclerView rvSchoolEvents, rvAfterSchoolEvents;
    private EventAdapter schoolAdapter, afterSchoolAdapter;
    private List<Event> schoolEvents, afterSchoolEvents;

    private Calendar currentCalendar;
    private String[] daysOfWeek = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentCalendar = Calendar.getInstance();

        initViews();
        setupRecyclerViews();
        setupButtons();
        updateDateDisplay();
        loadEvents();
    }

    private void initViews() {
        tvCurrentDay = findViewById(R.id.tvCurrentDay);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        btnPrevDay = findViewById(R.id.btnPrevDay);
        btnNextDay = findViewById(R.id.btnNextDay);
        btnAddSchool = findViewById(R.id.btnAddSchool);
        btnAddAfter = findViewById(R.id.btnAddAfter);
        btnInviteFriend = findViewById(R.id.btnInviteFriend);
        rvSchoolEvents = findViewById(R.id.rvSchoolEvents);
        rvAfterSchoolEvents = findViewById(R.id.rvAfterSchoolEvents);
    }

    private void setupRecyclerViews() {
        schoolEvents = new ArrayList<>();
        afterSchoolEvents = new ArrayList<>();

        schoolAdapter = new EventAdapter(this, schoolEvents, this::onEventClick);
        afterSchoolAdapter = new EventAdapter(this, afterSchoolEvents, this::onEventClick);

        rvSchoolEvents.setLayoutManager(new LinearLayoutManager(this));
        rvSchoolEvents.setAdapter(schoolAdapter);

        rvAfterSchoolEvents.setLayoutManager(new LinearLayoutManager(this));
        rvAfterSchoolEvents.setAdapter(afterSchoolAdapter);
    }

    private void setupButtons() {
        btnPrevDay.setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_MONTH, -1);
            updateDateDisplay();
            loadEvents();
        });

        btnNextDay.setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_MONTH, 1);
            updateDateDisplay();
            loadEvents();
        });

        btnAddSchool.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            intent.putExtra("type", "school");
            intent.putExtra("day", getCurrentDay());
            startActivity(intent);
        });

        btnAddAfter.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEventActivity.class);
            intent.putExtra("type", "after_school");
            intent.putExtra("day", getCurrentDay());
            startActivity(intent);
        });

        btnInviteFriend.setOnClickListener(v -> shareInviteLink());
    }

    private void updateDateDisplay() {
        int dayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK);
        String hebrewDay = daysOfWeek[(dayOfWeek + 5) % 7]; // Convert to Hebrew week

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dateStr = dateFormat.format(currentCalendar.getTime());

        tvCurrentDay.setText("יום " + hebrewDay);
        tvCurrentDate.setText(dateStr);
    }

    private String getCurrentDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(currentCalendar.getTime());
    }

    private void loadEvents() {
        String currentDay = getCurrentDay();

        // Load school events
        FirebaseHelper.getInstance().getUserEvents(currentDay, "school")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    schoolEvents.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        schoolEvents.add(event);
                    }
                    schoolAdapter.notifyDataSetChanged();
                });

        // Load after school events
        FirebaseHelper.getInstance().getUserEvents(currentDay, "after_school")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    afterSchoolEvents.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        afterSchoolEvents.add(event);
                    }
                    afterSchoolAdapter.notifyDataSetChanged();
                });
    }

    private void onEventClick(Event event) {
        Intent intent = new Intent(this, AddEventActivity.class);
        intent.putExtra("event", event);
        intent.putExtra("edit_mode", true);
        startActivity(intent);
    }

    private void shareInviteLink() {
        String inviteText = "הצטרף אליי באפליקציית ניהול הזמן שלי!\n" +
                "https://myadvancedschedule.page.link/invite";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, inviteText);
        startActivity(Intent.createChooser(shareIntent, "הזמן חבר"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            FirebaseHelper.getInstance().getAuth().signOut();
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }
}
