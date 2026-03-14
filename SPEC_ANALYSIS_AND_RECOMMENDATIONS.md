# MyAdvancedSchedule – Specification Analysis & Recommendations

This document compares the current Android project to the required application flow and feature specification, lists inconsistencies and bugs, and suggests concrete code changes.

---

## 1. Application Flow

**Required:** `Login → SetupScheduleActivity → MainActivity`

**Current:**
- **LoginActivity** and **RegisterActivity** both navigate to **MainActivity** on success.
- **SetupScheduleActivity** exists but is **not** in `AndroidManifest.xml`, and is never started after login/register.

**Changes:**
- Add **SetupScheduleActivity** to `AndroidManifest.xml`.
- In **LoginActivity** and **RegisterActivity**, on successful auth start **SetupScheduleActivity** (and `finish()`), not MainActivity.
- Optionally: only start SetupScheduleActivity when the user has no schedule yet (e.g. first login or new user); otherwise go to MainActivity. That requires a way to know if a schedule exists (e.g. Firestore flag or presence of lessons).

---

## 2. Login Activity

**Required:** Email, password, login button, link to Register, error message for invalid credentials.

**Current:**
- UI has email, password, login button, and link to Register.
- Invalid credentials are shown via **Toast** only; no dedicated error **TextView** in the layout.

**Changes:**
- Add a **TextView** (e.g. `textError`) in `activity_login.xml` and set its text on login failure instead of (or in addition to) Toast, so the spec’s “error message” is visible on screen.

---

## 3. Register Activity

**Required:** Email, password, confirm password, **optional** name, register button. On success → SetupScheduleActivity.

**Current:**
- Name is **required**: `registerUser()` returns early with `error_empty_name` if name is empty.
- On success, navigates to **MainActivity**.

**Changes:**
- Treat name as **optional**: remove the check that forces a non-empty name (e.g. remove or relax the `if (TextUtils.isEmpty(name))` block so registration can succeed without a name).
- On successful registration, start **SetupScheduleActivity** and `finish()`, not MainActivity.

---

## 4. SetupScheduleActivity – Overview

**Required:** Wizard between Login and MainActivity with 3 stages:

1. **Step 1 – Weekly frame:** which days, start time, lesson length, break length, max lessons; then generate skeleton (e.g. Lesson 1 08:00–08:45, etc.).
2. **Step 2 – Fill schedule:** ViewPager2 with one fragment per day; each fragment has a RecyclerView of lesson cards (Subject, Teacher, Classroom) with optional auto-complete, break button, duplicate button.
3. **Step 3 – Save:** “Save Entire Schedule” → ProgressBar, build Lesson objects, save to Firestore, then navigate to MainActivity.

**Current:**
- Step 1 is implemented as **SelectLessonsCountFragment**: only “how many lessons per day” with NumberPickers for Sunday–Thursday (no Friday/Saturday, no checkboxes for “which days”).
- No fields for: school start time, lesson duration, break length, or max lessons. No automatic time-slot generation.
- Step 2 uses **FillLessonsFragment** + **LessonEditAdapter**, but:
  - **FillLessonsFragment** builds lessons with `new Lesson(dayName, i, "", "", "")`, which **does not match any constructor** in **Lesson** (compile error).
  - **LessonEditAdapter** uses `R.layout.item_edit_lesson` but the layout file is **item_lesson_edit.xml** (resource name `item_lesson_edit`) → wrong layout reference.
  - **LessonEditAdapter** uses `lesson.getName()`, `lesson.getRoom()` and IDs `editLessonName`, `editRoomNumber`; **Lesson** has `getSubject()`, `getClassroom()` and the layout uses **editSubject**, **editTeacher**, **editClassroom** → API and view IDs are inconsistent; adapter also has no `getLessons()` while **FillLessonsFragment** calls `adapter.getLessons()`.
- **SetupScheduleActivity.saveScheduleToFirebase()** uses `lesson.getPeriodNumber()` but **Lesson** has **getPeriod()**, not `getPeriodNumber()` → compile error. It also saves to `users/{userId}/schedule/weekSchedule` as a single document, while **MainActivity** and **FirestoreHelper** read from the **lessons** collection (per-lesson documents with `userId`). So data saved in setup is **not** visible in MainActivity.

**Changes (high level):**
- **Step 1:** Replace or extend **SelectLessonsCountFragment** (or add a new fragment) so that it includes:
  - Checkboxes for “which days you study” (e.g. Sunday–Saturday).
  - Inputs: school day start time, lesson length (minutes), break length (minutes), max lessons per day.
  - Logic to build a list of time slots (e.g. 08:00–08:45, 08:55–09:40, …) and pass them to Step 2.
- **Step 2:** Ensure each day fragment receives the list of time slots for that day and builds **Lesson** objects with correct constructors (e.g. subject, teacher, classroom, day, period, startTime, endTime). Fix **LessonEditAdapter**: use layout `item_lesson_edit`, bind **editSubject** / **editTeacher** / **editClassroom** to **Lesson**’s subject/teacher/classroom, and implement **getLessons()** so it returns the current list from the adapter (and updates lesson fields from the views). Optionally add AutoCompleteTextView, break button, and duplicate button per spec.
- **Step 3:** When saving:
  - Build **Lesson** objects with the same structure as in **FirestoreHelper** (subject, teacher, classroom, day, period, startTime, endTime, and userId).
  - Save each lesson via **FirestoreHelper.addLesson()** (or an equivalent that writes to the same **lessons** collection and `userId` that MainActivity uses), not to `schedule/weekSchedule`. Use **getPeriod()** (and same field names as FirestoreHelper) so that **MainActivity** and **getAllLessons()** see the new lessons.

---

## 5. MainActivity

**Required:** RecyclerView with **today’s** lessons only, FAB to add lesson, empty state when there are no lessons. Add / edit / delete lesson.

**Current:**
- **loadLessons()** uses **FirestoreHelper.getAllLessons()**, which returns **all** lessons for the user; there is **no filter by “today”** (current weekday).
- So the list is not “today’s schedule” as specified.

**Changes:**
- After loading lessons, **filter by current day** (e.g. map calendar weekday to the same day representation used in your app, e.g. “Monday”, and keep only `lesson.getDay()` matching that). Then set the filtered list on the adapter.

---

## 6. AddLessonDialogFragment

**Required:** Subject, Teacher, Classroom, Day, Start time, End time. Create a Lesson and store in Firestore.

**Current:**
- Dialog has Subject, Teacher, Classroom, Day (spinner), Period (number), Start time, End time. It supports both add and edit (when `existingLesson != null`), and uses **FirestoreHelper** to add/update in the **lessons** collection. This matches the desired add behavior and storage.

**Optional alignment:**
- Spec asks for “Day, Start time, End time” (no explicit “Period”). If you want to drop period for add-dialog, you can derive it from start/end or leave it as internal detail; otherwise current design is acceptable.

---

## 7. EditLessonDialogFragment (and Edit vs Delete)

**Required:** Same fields as Add; buttons **Update** and **Delete**. Separate “Delete confirmation” dialog.

**Current:**
- There is **no** separate **EditLessonDialogFragment**. **MainActivity** uses **AddLessonDialogFragment** for both add and edit (with Save/Cancel). Delete is triggered from the list and confirmed with an **AlertDialog** (which satisfies “Delete confirmation dialog”).

**Changes:**
- Either:
  - Introduce **EditLessonDialogFragment** with the same fields as Add plus **Update** and **Delete** buttons; from MainActivity open this for edit and **AddLessonDialogFragment** only for add; or
  - Keep a single dialog but when `existingLesson != null` show **Update** and **Delete** (and optionally Cancel) instead of only Save/Cancel, and keep the existing delete confirmation when user taps Delete.

---

## 8. Menu (Logout)

**Required:** Menu contains Logout.

**Current:** **main_menu.xml** is inflated and **R.id.action_logout** is handled in **MainActivity** to sign out and go to Login. This matches the spec.

---

## 9. Data Model and Firestore Consistency

- **Lesson** has: id, subject, teacher, classroom, day, period, startTime, endTime. **FirestoreHelper** uses the **lessons** collection and `userId`; it reads/writes subject, teacher, classroom, day, period, startTime, endTime. This is consistent.
- **SetupScheduleActivity** currently writes to **users/{userId}/schedule/weekSchedule** and uses **getPeriodNumber()** (nonexistent). It must be changed to write the same structure as **FirestoreHelper** (e.g. use **getPeriod()** and save each lesson to the **lessons** collection via **FirestoreHelper.addLesson()** or equivalent) so that MainActivity sees the schedule after setup.

---

## 10. Compile / Runtime Bugs Summary

| Location | Issue | Fix |
|----------|--------|-----|
| **FillLessonsFragment** | `new Lesson(dayName, i, "", "", "")` – no such constructor in **Lesson** | Build Lesson with the full constructor (subject, teacher, classroom, day, period, startTime, endTime); get start/end from Step 1 time slots. |
| **SetupScheduleActivity** | `lesson.getPeriodNumber()` | Use **lesson.getPeriod()** (and store `period` in Firestore as in FirestoreHelper). |
| **LessonEditAdapter** | `R.layout.item_edit_lesson` | Use **R.layout.item_lesson_edit** (match actual layout file name). |
| **LessonEditAdapter** | `getName()`, `getRoom()`; IDs `editLessonName`, `editRoomNumber` | Use **getSubject()**, **getClassroom()** and view IDs **editSubject**, **editTeacher**, **editClassroom** to match **Lesson** and **item_lesson_edit.xml**. |
| **LessonEditAdapter** | No **getLessons()** method | Implement **getLessons()** that returns the list and (if needed) syncs from current view values into lesson objects. |
| **MainActivity** | `new LessonAdapter(lessonList, this)` | **LessonAdapter** has only `LessonAdapter(List<Lesson> lessons)`. Use `new LessonAdapter(lessonList)` and **setOnItemClickListener(this)**. |
| **MainActivity** / **LessonAdapter** | **MainActivity** implements **onEditClick** / **onDeleteClick**; **LessonAdapter** interface has **onItemClick** / **onItemLongClick** | Either change adapter interface to **onEditClick** / **onDeleteClick** and use them in the adapter, or keep onItemClick/onItemLongClick and map in MainActivity (e.g. click = edit, long-click = delete). |

---

## 11. Missing or Inconsistent String Resources

**AddLessonDialogFragment** uses:

- `R.string.add_lesson_title` / `R.string.edit_lesson_title` – in **strings.xml** you have **dialog_title_add** / **dialog_title_edit**. Either add the expected names or use the existing ones in code.
- `R.string.save` / `R.string.cancel` – you have **btn_save** / **btn_cancel**. Align names or add **save** / **cancel**.
- `R.string.error_saving_lesson` – add if missing.
- `R.string.error_empty_subject`, **error_empty_teacher**, **error_empty_classroom**, **error_invalid_period**, **error_invalid_time** – add if missing.

**activity_main.xml** references **no_lessons_today**, **add_first_lesson** – ensure they exist in **strings.xml**.

---

## 12. Files to Modify (Checklist)

| File | Changes |
|------|--------|
| **AndroidManifest.xml** | Declare **SetupScheduleActivity**; ensure it can be started from Login/Register. |
| **LoginActivity.java** | On success, start **SetupScheduleActivity**; optionally show error in a TextView. |
| **RegisterActivity.java** | Make name optional; on success start **SetupScheduleActivity**. |
| **SetupScheduleActivity.java** | Fix **getPeriodNumber()** → **getPeriod()**; change save logic to write to **lessons** collection (e.g. via FirestoreHelper) so MainActivity sees data. Optionally add Step 1 UI (days, times, lengths) and wire Step 2 to generated slots. |
| **SelectLessonsCountFragment** (or new Step 1 fragment) | Add “which days”, start time, lesson length, break length, max lessons; compute time slots and pass to Step 2. |
| **FillLessonsFragment.java** | Build **Lesson** list using valid constructors and time slots from Step 1; ensure adapter has correct data. |
| **LessonEditAdapter.java** | Use **R.layout.item_lesson_edit**; bind Subject/Teacher/Classroom (getSubject, getTeacher, getClassroom); implement **getLessons()** and keep list in sync with views. |
| **item_lesson_edit.xml** | Already has editSubject, editTeacher, editClassroom; add time slot label and optional break/duplicate buttons if required by spec. |
| **MainActivity.java** | Filter loaded lessons by **today** before showing in RecyclerView; fix adapter construction to `new LessonAdapter(lessonList)` + **setOnItemClickListener(this)**; align listener interface (onEditClick/onDeleteClick vs onItemClick/onItemLongClick). |
| **LessonAdapter.java** | Expose **onEditClick** / **onDeleteClick** (or keep onItemClick/onItemLongClick and document usage in MainActivity). |
| **strings.xml** | Add or align: add_lesson_title, edit_lesson_title, save, cancel, error_saving_lesson, error_empty_subject, error_empty_teacher, error_empty_classroom, error_invalid_period, error_invalid_time, no_lessons_today, add_first_lesson. |
| **activity_login.xml** | Add a TextView for login error message. |
| **EditLessonDialogFragment** (new, or extend dialog) | Add fragment or dialog variant with Update + Delete buttons when editing. |

---

## 13. Optional / Nice-to-Have (Per Spec)

- **Step 2 – Auto-complete:** Use **AutoCompleteTextView** for subject (and optionally teacher/classroom) with suggestions from previously used Subject+Teacher+Classroom.
- **Step 2 – Break button:** Button on each card to mark lesson as “break” and change card color.
- **Step 2 – Duplicate:** Copy button to duplicate a lesson to another time slot.

These can be added after the flow and data consistency fixes above.

---

If you tell me which part you want to implement first (e.g. “fix flow and Firestore”, “fix Step 1 and Step 2”, or “fix MainActivity and adapter”), I can provide concrete code edits file by file.
