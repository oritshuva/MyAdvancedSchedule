# MyAdvancedSchedule – Project Analysis

This document summarizes the structure, architecture, data flow, and current state of the Android project **MyAdvancedSchedule** after analyzing the full workspace.

---

## 1. Project overview

- **Type:** Android app (Java, Gradle with Kotlin DSL).
- **Min SDK:** 24 | **Target/Compile SDK:** 34.
- **Namespace:** `com.example.myadvancedschedule`.
- **Backend:** Firebase (Auth, Firestore, optional Messaging); Google Sign-in (Credentials API).
- **Build:** `build.gradle.kts` (root + app), version catalog in `gradle/libs.versions.toml`.

---

## 2. High-level architecture

### 2.1 Entry and navigation flow

| Step | Screen | Role |
|------|--------|------|
| 1 | **WelcomeActivity** (launcher) | If not logged in → Login / Create Account; if logged in → MainActivity. |
| 2 | **LoginActivity** / **RegisterActivity** | Email/password auth; on success → **SetupScheduleActivity**. |
| 3 | **SetupScheduleActivity** | Wizard: Step 1 = weekly frame (days, times, lengths); Step 2 = one fragment per day to fill lessons; Save → writes to Firestore **lessons** collection, then → **MainActivity**. |
| 4 | **MainActivity** | Tabs: Tasks, School schedule, After-school schedule. Menu: Add lesson, Logout. |

So the intended flow **Login → SetupScheduleActivity → MainActivity** is implemented. **SetupScheduleActivity** is declared in `AndroidManifest.xml`.

### 2.2 Main components

- **Activities:** Welcome, Login, Register, SetupSchedule, Main.
- **Fragments:** FrameSetupFragment (Step 1), DayScheduleFragment (Step 2 per day), SchoolScheduleFragment, AfterSchoolScheduleFragment, TasksFragment; dialogs: AddLessonDialogFragment, AddTaskDialogFragment.
- **Data / backend:** FirestoreHelper (lessons, tasks, subjects), Firebase Auth.
- **Models:** Lesson, Task, User, Event, TimeSlot, FrameSetupData.

---

## 3. Data layer

### 3.1 Firestore

- **Collections:** `lessons` (userId, subject, teacher, classroom, day, period, startTime, endTime, scheduleType), `tasks`, `subjects`.
- **FirestoreHelper** is the single point for lessons/tasks/subjects: add, update, delete, getAllLessons, getLessonsForToday(scheduleType, dayName).
- Setup wizard saves each lesson via **FirestoreHelper.addLesson()** into the same `lessons` collection, so data is consistent with MainActivity and schedule tabs.

### 3.2 Lesson model

- **Lesson:** id, subject, teacher, classroom, day, period, startTime, endTime, scheduleType ("school" | "after_school").
- Constructors: (subject, teacher, classroom, day, period, startTime, endTime) and (id, …) for existing; empty for Firestore.

---

## 4. Key flows (current behavior)

### 4.1 Setup wizard (SetupScheduleActivity)

1. **Step 1 – FrameSetupFragment:** Checkboxes for days (Sun–Sat), start time, lesson duration, break duration, max lessons. **FrameSetupData** is built and validated.
2. **Time slots:** **SetupScheduleActivity.computeTimeSlots()** generates start/end times (e.g. 08:00–08:45, 08:55–09:40) from frame data.
3. **Step 2 – DayScheduleFragment per day:** Each fragment gets `dayName` and `ArrayList<TimeSlot>`. **buildLessonsFromSlots()** creates **Lesson** objects with the 7-argument constructor (subject/teacher/classroom empty, day, period, startTime, endTime). **LessonSetupCardAdapter** shows cards and syncs subject/teacher/classroom back into the lesson list; **getLessons()** returns that list.
4. **Save:** Collects lessons from all day fragments and saves them one-by-one via **FirestoreHelper.addLesson()**, then navigates to MainActivity.

### 4.2 Main screen (MainActivity)

- **ViewPager2** with three tabs: Tasks, School, After School.
- **SchoolScheduleFragment** and **AfterSchoolScheduleFragment** call **FirestoreHelper.getLessonsForToday("school" | "after_school", todayDayName, …)** and display results with **LessonCardAdapter** (display only; no edit/delete on items).
- **TasksFragment** loads tasks, shows **TaskAdapter**, FAB opens **AddTaskDialogFragment**.
- **Add lesson** is in the overflow menu and opens **AddLessonDialogFragment** (add or edit if `existingLesson != null`). No edit/delete triggered from the schedule list itself (see gaps below).

### 4.3 Add/Edit lesson (AddLessonDialogFragment)

- Subject (spinner + “Other”), teacher, classroom, day, period, start/end time.
- Uses **FirestoreHelper** for add/update and **getSubjects()** for the dropdown; new “Other” subjects can be stored in `subjects` collection.
- Strings used (e.g. `add_lesson_title`, `edit_lesson_title`, `save`, `cancel`, `error_saving_lesson`, `error_empty_subject`, etc.) are present in `res/values/strings.xml`.

---

## 5. Unused or alternate code

- **LessonAdapter:** Defines `OnItemClickListener` (onItemClick, onItemLongClick) and uses `item_lesson` layout, but is **not referenced** anywhere. Schedule tabs use **LessonCardAdapter** only.
- **FirebaseHelper** (singleton), **SignupActivity**, **AddEventActivity**, **Event**, **EventAdapter:** Use a different helper and are **not** in `AndroidManifest.xml`. They look like an alternate or legacy path (e.g. events collection, different auth flow). The active flow uses **FirestoreHelper** and **RegisterActivity**.

---

## 6. Gaps and recommendations (vs spec / UX)

| Area | Current state | Recommendation |
|------|----------------|----------------|
| **Register – name** | Name is required (`error_empty_name`). | Make name optional so registration can succeed without it. |
| **Login – error message** | Only Toast on failure. | Add a **TextView** in `activity_login.xml` and set it on login failure for a persistent on-screen error. |
| **Edit/Delete from schedule** | Add lesson from menu; **AddLessonDialogFragment** supports edit when passed `existingLesson`, but schedule tabs (**LessonCardAdapter**) have no click/long-click to open edit or delete. | Either use **LessonAdapter** (or add callbacks to **LessonCardAdapter**) so that tap = edit (open dialog with existing lesson), long-press = delete with confirmation; or document that edit/delete are only via menu if that’s intentional. |
| **Edit dialog – Update/Delete** | Dialog shows Save/Cancel for both add and edit. | In edit mode, show **Update** and **Delete** (and keep delete confirmation dialog when Delete is tapped). |
| **WelcomeActivity – first-time setup** | Logged-in users always go to MainActivity. | Optionally: if user has no lessons yet (e.g. first time after register), redirect to **SetupScheduleActivity** instead of MainActivity. |
| **Logout** | Implemented in MainActivity (menu); confirmation dialog uses hardcoded "Logout" / "Are you sure...". | Consider using `R.string.logout` and a string for the confirmation message. |

---

## 7. File and dependency summary

- **Java sources:** ~25+ classes under `app/src/main/java/.../` (activities, fragments, adapters, helpers, models).
- **Layouts:** activity_*, fragment_*, dialog_*, item_*, menu (e.g. `main_menu.xml` with Add lesson + Logout).
- **Gradle:** Firebase BOM, Auth, Firestore, Messaging; AppCompat, Material, ViewPager2, RecyclerView; Credentials + Google ID for sign-in.
- **Existing spec doc:** `SPEC_ANALYSIS_AND_RECOMMENDATIONS.md` describes required flows and fixes; several of those (e.g. flow, SetupScheduleActivity in manifest, Step 1 frame, saving to `lessons` via FirestoreHelper) are already in place. The analysis above aligns with that doc and notes what’s done vs. what remains (e.g. Register name optional, login error TextView, edit/delete from list, Update/Delete in dialog).

---

## 8. Summary

The project implements a clear **Welcome → Login/Register → SetupSchedule (2 steps) → MainActivity** flow, with Firestore **lessons** as the single source of truth and **FirestoreHelper** used for both the setup wizard and the main app. The setup wizard’s Step 1 (frame) and Step 2 (per-day lessons with **LessonSetupCardAdapter** and **getLessons()**) are consistent with the **Lesson** model and Firestore. Remaining work is mainly: optional name on register, visible login error, edit/delete from schedule list, and Update/Delete in the lesson dialog when editing.
