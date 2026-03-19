package com.example.myadvancedschedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EditScheduleInstrumentationTest {
    private static final String TEST_USER_ID = "test-user";

    private final List<Lesson> seedLessons = new ArrayList<>();

    @Before
    public void setUp() {
        seedLessons.clear();

        // Use two days to verify isolation.
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
    public void editOneLesson_updatesOnlyTargetedDay_andPersistsAcrossReload() throws Exception {
        FirestoreHelper firestoreHelper = new FirestoreHelper();

        Lesson updated = new Lesson(
                "l1",
                "Math Updated",
                "John Smith Updated",
                "301 Updated",
                "Monday",
                1,
                "08:00",
                "08:45"
        );
        updated.setScheduleType("school");

        CountDownLatch latch = new CountDownLatch(1);
        firestoreHelper.updateLesson(TEST_USER_ID, updated, new FirestoreHelper.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onFailure(String error) {
                fail("updateLesson failed: " + error);
                latch.countDown();
            }
        });
        assertTrue("updateLesson callback timed out", latch.await(5, TimeUnit.SECONDS));

        List<Lesson> snapshot = FirestoreHelper.getInMemoryLessonsSnapshot();
        assertEquals("No duplicates should be created by editing", seedLessons.size(), snapshot.size());

        Lesson l1 = findById(snapshot, "l1");
        assertNotNull(l1);
        assertEquals("Math Updated", l1.getSubject());
        assertEquals("John Smith Updated", l1.getTeacher());
        assertEquals("301 Updated", l1.getClassroom());

        Lesson l3 = findById(snapshot, "l3");
        assertNotNull(l3);
        assertEquals("Physics", l3.getSubject());

        // Reload check: day filtering must reflect updated subject.
        CountDownLatch reloadLatch = new CountDownLatch(1);
        final List<Lesson>[] mondayLessons = new List[1];
        firestoreHelper.getLessonsForToday("school", "Monday", new FirestoreHelper.OnLessonsLoadedListener() {
            @Override
            public void onLessonsLoaded(List<Lesson> lessons) {
                mondayLessons[0] = lessons;
                reloadLatch.countDown();
            }

            @Override
            public void onError(String error) {
                fail("getLessonsForToday failed: " + error);
                reloadLatch.countDown();
            }
        });

        assertTrue("getLessonsForToday callback timed out", reloadLatch.await(5, TimeUnit.SECONDS));
        assertNotNull(mondayLessons[0]);
        assertTrue(containsSubjectForId(mondayLessons[0], "l1", "Math Updated"));
    }

    @Test(timeout = 20000)
    public void editMultipleLessons_updatesAllEditedLessons() throws Exception {
        FirestoreHelper firestoreHelper = new FirestoreHelper();

        Lesson updatedL1 = new Lesson(
                "l1",
                "Math Updated 2",
                "John Smith Updated 2",
                "301 Updated 2",
                "Monday",
                1,
                "08:00",
                "08:45"
        );
        updatedL1.setScheduleType("school");

        Lesson updatedL2 = new Lesson(
                "l2",
                "English Updated 2",
                "Alice Brown Updated 2",
                "302 Updated 2",
                "Monday",
                2,
                "08:50",
                "09:35"
        );
        updatedL2.setScheduleType("school");

        CountDownLatch latch = new CountDownLatch(2);
        firestoreHelper.updateLesson(TEST_USER_ID, updatedL1, new FirestoreHelper.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onFailure(String error) {
                fail("updateLesson l1 failed: " + error);
                latch.countDown();
            }
        });
        firestoreHelper.updateLesson(TEST_USER_ID, updatedL2, new FirestoreHelper.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onFailure(String error) {
                fail("updateLesson l2 failed: " + error);
                latch.countDown();
            }
        });

        assertTrue("updateLesson callbacks timed out", latch.await(5, TimeUnit.SECONDS));

        List<Lesson> snapshot = FirestoreHelper.getInMemoryLessonsSnapshot();
        assertEquals("No duplicates should be created by editing", seedLessons.size(), snapshot.size());

        assertTrue(containsSubjectForId(snapshot, "l1", "Math Updated 2"));
        assertTrue(containsSubjectForId(snapshot, "l2", "English Updated 2"));
    }

    private static Lesson findById(List<Lesson> lessons, String id) {
        if (lessons == null || id == null) return null;
        for (Lesson l : lessons) {
            if (l != null && id.equals(l.getId())) return l;
        }
        return null;
    }

    private static boolean containsSubjectForId(List<Lesson> lessons, String id, String expectedSubject) {
        Lesson l = findById(lessons, id);
        if (l == null) return false;
        String s = l.getSubject() != null ? l.getSubject() : "";
        return expectedSubject.equals(s);
    }
}

