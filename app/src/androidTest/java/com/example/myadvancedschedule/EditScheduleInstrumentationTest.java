package com.example.myadvancedschedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

import androidx.test.core.app.ActivityScenario;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class EditScheduleInstrumentationTest {

    private static final String TEST_USER_ID = "test-user";

    private final List<Lesson> seedLessons = new ArrayList<>();

    @Before
    public void setUp() {
        seedLessons.clear();

        // Use two days to verify isolation: current selected day vs other day.
        // English day names must match days_array_english.
        seedLessons.add(new Lesson("l1", "Math", "John Smith", "301", "Monday", 1, "08:00", "08:45"));
        seedLessons.add(new Lesson("l2", "English", "Alice Brown", "302", "Monday", 2, "08:50", "09:35"));
        seedLessons.add(new Lesson("l3", "Physics", "Bob White", "201", "Tuesday", 1, "08:00", "08:45"));

        for (Lesson l : seedLessons) {
            l.setScheduleType("school");
        }

        FirestoreHelper.setInMemoryLessonsForTests(TEST_USER_ID, seedLessons);
    }

    @After
    public void tearDown() {
        FirestoreHelper.clearInMemoryLessonsForTests();
    }

    @Test(timeout = 20000)
    public void editOneLesson_updatesOnlySelectedDayAndPersistsOnRecreate() {
        ActivityScenario<TestHostActivity> scenario = ActivityScenario.launch(TestHostActivity.class);
        scenario.onActivity(activity -> {
            SchoolScheduleFragment fragment = new SchoolScheduleFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(TestHostActivity.CONTAINER_ID, fragment, "SCHOOL")
                    .commitNow();

            // Select Monday tab explicitly (not dependent on "today").
            TabLayout tabs = fragment.getView().findViewById(R.id.tabDays);
            int mondayIndex = indexForDay(tabs, "Monday");
            assertTrue(mondayIndex >= 0);
            tabs.getTabAt(mondayIndex).select();

            View btnEdit = fragment.getView().findViewById(R.id.btnEditSchedule);
            btnEdit.performClick();

            // Edit first editable lesson (period 1 => "Math").
            View recycler = fragment.getView().findViewById(R.id.recyclerLessons);
            View edit1 = findChildWithIdContaining(recycler, 0, R.id.editSubject);
            assertTrue(edit1 != null);
            assertTrue(edit1 instanceof com.google.android.material.textfield.TextInputEditText);
            com.google.android.material.textfield.TextInputEditText editSubject1 =
                    (com.google.android.material.textfield.TextInputEditText) edit1;
            assertEquals("Math", editSubject1.getText() != null ? editSubject1.getText().toString() : "");

            editSubject1.setText("Math Updated");

            View btnSave = fragment.getView().findViewById(R.id.btnSaveChanges);
            btnSave.performClick();

            // UI should reflect changes immediately in read-only mode.
            View recyclerAfterSave = fragment.getView().findViewById(R.id.recyclerLessons);
            View textSubject1View = findChildWithIdContaining(recyclerAfterSave, 0, R.id.textSubject);
            assertTrue(textSubject1View != null);
            TextView textSubject1 = (TextView) textSubject1View;
            assertEquals("Subject: Math Updated",
                    textSubject1.getText() != null ? textSubject1.getText().toString() : "");

            // Verify in-memory persistence updated for Monday only.
            assertTrue(isLessonSubject("Monday", "l1", "Math Updated"));
            assertTrue(isLessonSubject("Tuesday", "l3", "Physics"));
        });

        // Restart simulation: recreate fragment in a new host activity.
        // (We still use the same in-memory store; this mimics persistence across app restart.)
        ActivityScenario<TestHostActivity> scenario2 = ActivityScenario.launch(TestHostActivity.class);
        scenario2.onActivity(activity2 -> {
            SchoolScheduleFragment fragment2 = new SchoolScheduleFragment();
            activity2.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(TestHostActivity.CONTAINER_ID, fragment2, "SCHOOL2")
                    .commitNow();

            TabLayout tabs2 = fragment2.getView().findViewById(R.id.tabDays);
            int mondayIndex2 = indexForDay(tabs2, "Monday");
            assertTrue(mondayIndex2 >= 0);
            tabs2.getTabAt(mondayIndex2).select();

            View btnEdit2 = fragment2.getView().findViewById(R.id.btnEditSchedule);
            btnEdit2.performClick();

            View recycler2 = fragment2.getView().findViewById(R.id.recyclerLessons);
            View edit2 = findChildWithIdContaining(recycler2, 0, R.id.editSubject);
            assertTrue(edit2 != null);
            com.google.android.material.textfield.TextInputEditText editSubjectReload =
                    (com.google.android.material.textfield.TextInputEditText) edit2;

            assertEquals("Math Updated",
                    editSubjectReload.getText() != null ? editSubjectReload.getText().toString() : "");
        });
    }

    @Test(timeout = 20000)
    public void editMultipleLessons_updatesAllEditedLessons() {
        ActivityScenario<TestHostActivity> scenario = ActivityScenario.launch(TestHostActivity.class);
        scenario.onActivity(activity -> {
            SchoolScheduleFragment fragment = new SchoolScheduleFragment();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(TestHostActivity.CONTAINER_ID, fragment, "SCHOOL")
                    .commitNow();

            TabLayout tabs = fragment.getView().findViewById(R.id.tabDays);
            int mondayIndex = indexForDay(tabs, "Monday");
            assertTrue(mondayIndex >= 0);
            tabs.getTabAt(mondayIndex).select();

            fragment.getView().findViewById(R.id.btnEditSchedule).performClick();

            View recycler = fragment.getView().findViewById(R.id.recyclerLessons);

            // Edit both editable lessons: period 1 and period 2.
            View edit1 = findChildWithIdContaining(recycler, 0, R.id.editSubject);
            View edit2 = findChildWithIdContaining(recycler, 1, R.id.editSubject);
            assertTrue(edit1 != null);
            assertTrue(edit2 != null);
            com.google.android.material.textfield.TextInputEditText editSubject1 =
                    (com.google.android.material.textfield.TextInputEditText) edit1;
            com.google.android.material.textfield.TextInputEditText editSubject2 =
                    (com.google.android.material.textfield.TextInputEditText) edit2;

            editSubject1.setText("Math Updated 2");
            editSubject2.setText("English Updated 2");

            fragment.getView().findViewById(R.id.btnSaveChanges).performClick();

            View recyclerAfterSave = fragment.getView().findViewById(R.id.recyclerLessons);
            View textSubject1View = findChildWithIdContaining(recyclerAfterSave, 0, R.id.textSubject);
            assertTrue(textSubject1View != null);
            TextView textSubject1 = (TextView) textSubject1View;
            assertEquals("Subject: Math Updated 2",
                    textSubject1.getText() != null ? textSubject1.getText().toString() : "");

            assertTrue(isLessonSubject("Monday", "l1", "Math Updated 2"));
            assertTrue(isLessonSubject("Monday", "l2", "English Updated 2"));
        });
    }

    private int indexForDay(TabLayout tabs, String dayName) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Object tag = tabs.getTabAt(i).getTag();
            if (dayName.equals(tag)) return i;
        }
        return -1;
    }

    private boolean isLessonSubject(String day, String docId, String expectedSubject) {
        for (Lesson l : FirestoreHelper.getInMemoryLessonsSnapshot()) {
            if (docId.equals(l.getId()) && day.equals(l.getDay())) {
                String s = l.getSubject() != null ? l.getSubject() : "";
                return expectedSubject.equals(s);
            }
        }
        return false;
    }

    /**
     * Finds a child view inside RecyclerView item children by the target ID, ordered top-to-bottom.
     * This assumes the edit form shows in sequential adapter order (which it does for the selected day).
     */
    private View findChildWithIdContaining(View recyclerView, int viewIndex, int targetId) {
        int found = 0;
        for (int i = 0; i < ((androidx.recyclerview.widget.RecyclerView) recyclerView).getChildCount(); i++) {
            View item = ((androidx.recyclerview.widget.RecyclerView) recyclerView).getChildAt(i);
            View target = item.findViewById(targetId);
            if (target != null && target.getVisibility() == View.VISIBLE) {
                if (targetId == R.id.editSubject) {
                    View parentLayout = item.findViewById(R.id.layoutEditSubject);
                    if (parentLayout == null || parentLayout.getVisibility() != View.VISIBLE) continue;
                }
                if (found == viewIndex) return target;
                found++;
            }
        }
        // If not found as visible, fall back to first match.
        found = 0;
        for (int i = 0; i < ((androidx.recyclerview.widget.RecyclerView) recyclerView).getChildCount(); i++) {
            View item = ((androidx.recyclerview.widget.RecyclerView) recyclerView).getChildAt(i);
            View target = item.findViewById(targetId);
            if (target != null) {
                if (targetId == R.id.editSubject) {
                    View parentLayout = item.findViewById(R.id.layoutEditSubject);
                    if (parentLayout == null || parentLayout.getVisibility() != View.VISIBLE) continue;
                }
                if (found == viewIndex) return target;
                found++;
            }
        }
        return null;
    }
}

