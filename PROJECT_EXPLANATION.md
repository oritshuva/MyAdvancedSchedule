# MyAdvancedSchedule – Project Explanation

A clear, structured description of the application for exams and presentations.

---

## 1. General Description of the App

### What the application is called
**MyAdvancedSchedule** – an Android application for managing a personal weekly schedule and daily tasks.

### What its main purpose is
The app helps users:
- **Define and view their weekly schedule** – which days they study, at what times, and which subjects they have.
- **Separate school and after-school activities** – school lessons and after-school lessons are stored and displayed separately.
- **Manage daily tasks** – add to-do items, mark them complete, and see them in one place.
- **Keep everything in the cloud** – all data is stored in Firebase so it can be used from the same account on different devices.

### What problem it solves
- **Disorganization** – students and busy people often juggle many lessons and tasks; the app centralizes schedule and tasks in one place.
- **No single view of “today”** – the app shows today’s school schedule, today’s after-school schedule, and today’s tasks in separate tabs, so the user always knows what’s coming.
- **Manual setup** – instead of typing each lesson one by one, the setup wizard lets users define the time frame once (e.g. “8 lessons, 45 min each, 10 min break”) and then fill in only subject, teacher, and classroom for each slot.

### Who the target users are
- **Students** – to manage their weekly timetable and homework or tasks.
- **Anyone with a recurring weekly schedule** – e.g. tutors, part-time workers, or people with regular after-school activities who want a clear view of their day and tasks.

---

## 2. Main Features of the App

### User authentication (login and registration)
- **Registration** – new users can create an account with email, password, and optionally their name. Passwords must match and be at least 6 characters. Registration uses **Firebase Authentication**.
- **Login** – existing users sign in with email and password. Invalid credentials are reported (e.g. via Toast). On success, the user is sent to the schedule setup wizard (or main screen if they already have a schedule).
- **Session** – if the user is already logged in when opening the app, they go straight to the main screen (Welcome screen checks Firebase Auth and skips login).

### Weekly schedule management
- **Setup wizard** – after first login or registration, the user goes through a two-step setup:
  - **Step 1:** Choose which days they study (e.g. Sunday–Thursday), when the day starts (e.g. 08:00), how long each lesson and break is, and the maximum number of lessons per day. The app then generates time slots (e.g. 08:00–08:45, 08:55–09:40).
  - **Step 2:** For each selected day, the user sees a list of time slots and fills in **subject**, **teacher**, and **classroom** for each slot. They can swipe between days. At the end they tap “Save schedule” and all lessons are saved to **Cloud Firestore**.
- **Viewing the schedule** – on the main screen, two tabs show **today’s** school lessons and **today’s** after-school lessons. Each lesson is shown as a card with subject, teacher, classroom, and time.
- **Adding and editing lessons** – from the main screen menu, the user can open a dialog to add a new lesson (subject, teacher, classroom, day, period, start and end time) or edit an existing one. Data is saved to Firestore.

### Tasks management
- **Tasks screen** – one of the three main tabs shows “Today’s Tasks”. Each task has a title and optional due time. The user can mark tasks as complete (e.g. checkbox). Tasks are loaded from and saved to Firestore.
- **Adding tasks** – a floating action button (FAB) on the tasks screen opens a dialog to add a new task. After saving, the list refreshes.
- **Editing / completing** – task completion state is updated in Firestore when the user toggles the checkbox.

### Adding and editing lessons
- **Add/Edit lesson dialog** – the same dialog is used for both adding and editing. It includes:
  - **Subject** – chosen from a dropdown list. The list is filled from previously used subjects (stored in Firestore). If the subject is not in the list, the user can choose “Other” and type a new subject name; that name can be saved to the subjects list for next time.
  - **Teacher** and **Classroom** – text fields.
  - **Day** – spinner (e.g. Sunday–Saturday).
  - **Period** – spinner (e.g. 1–15).
  - **Start time** and **End time** – spinners with time options (e.g. 07:00–22:00 in 15-minute steps).
- Saving or updating is done via **FirestoreHelper**; the main schedule tabs refresh (e.g. on resume) to show the new or updated lesson.

### Adding and editing tasks
- **Add task dialog** – the user enters a task title and optional due time. The task is saved to the **tasks** collection in Firestore and appears in the tasks list.
- **Editing / completing** – toggling a task’s completed state updates the corresponding document in Firestore.

### Swipe navigation between screens
- **Main screen** – the main screen has three “pages” that the user swipes between: **Tasks**, **School schedule**, and **After-school schedule**. This is implemented with **ViewPager2** and a **TabLayout** with icons and labels.
- **Setup wizard** – in Step 2 of the setup, the user swipes between one page per selected day (e.g. Sunday, Monday, …). Each page is a **Fragment** inside a **ViewPager2**.

### Firebase and Firestore usage
- **Firebase Authentication** – used for creating accounts (email/password) and signing in. The current user’s ID is used to filter all data in Firestore so each user sees only their own lessons and tasks.
- **Cloud Firestore** – used as the database:
  - **lessons** – each lesson is a document with fields: userId, subject, teacher, classroom, day, period, startTime, endTime, scheduleType (school or after_school).
  - **tasks** – each task has userId, title, dueTime, completed.
  - **subjects** – optional list of subject names per user for the lesson subject dropdown.
- All Firestore access is centralized in a **FirestoreHelper** class: add, update, delete, and query for lessons, tasks, and subjects. The app does not talk to Firestore directly from every screen; it uses this helper so that collection names and field names stay consistent.

---

## 3. App Flow

### Step-by-step user journey

1. **Welcome screen (WelcomeActivity)**  
   - App opens on the welcome screen.  
   - If the user is **not** logged in: two buttons are shown – **Login** and **Create account**.  
   - If the user **is** already logged in: the app immediately opens the main screen (MainActivity) and the welcome screen is not shown.

2. **Login**  
   - User taps **Login** and is taken to **LoginActivity**.  
   - They enter email and password and tap the login button.  
   - If credentials are wrong, an error message is shown (e.g. Toast).  
   - If login succeeds, they are sent to **SetupScheduleActivity** (schedule setup wizard) and the login screen is closed.

3. **Register**  
   - User taps **Create account** and is taken to **RegisterActivity**.  
   - They enter name (optional in some versions), email, password, and confirm password.  
   - Validation: name (if required), non-empty email, password length ≥ 6, passwords must match.  
   - On success, they are sent to **SetupScheduleActivity** and the register screen is closed.

4. **Setup schedule (SetupScheduleActivity)**  
   - **Step 1 – Weekly frame:**  
     - User selects which days they study (checkboxes for Sunday–Saturday).  
     - They enter: start time (e.g. 08:00), lesson length in minutes, break length in minutes, maximum lessons per day.  
     - They tap **Next**. The app generates time slots (e.g. 08:00–08:45, 08:55–09:40, …) for each selected day.  
   - **Step 2 – Fill lessons:**  
     - One “page” per selected day. User swipes between days.  
     - On each page, they see a list of time slots and for each slot enter subject, teacher, and classroom.  
     - They can go **Next** to the next day or **Previous** to go back.  
     - On the last day, **Next** becomes **Save schedule**.  
   - **Save:**  
     - The app collects all lessons from all days and saves each one to Firestore (lessons collection) using the current user’s ID.  
     - A progress indicator can be shown. When done, the user is taken to **MainActivity** and the setup screen is closed.

5. **Main screen (MainActivity)**  
   - The main screen has a **toolbar** at the top (e.g. app name and user name) and a **tab bar** with three tabs: **Tasks**, **School**, **After school**.  
   - The user **swipes left/right** or taps a tab to switch between three “pages”:  
     - **Tasks**  
     - **School schedule** (today’s school lessons)  
     - **After-school schedule** (today’s after-school lessons)  
   - The **menu** (e.g. three dots) contains:  
     - **Add lesson** – opens the add/edit lesson dialog.  
     - **Logout** – asks for confirmation, then signs out and returns the user to the login screen.

6. **Tasks screen (first tab)**  
   - Shows a list of the user’s tasks (loaded from Firestore).  
   - Each task shows title and optional due time; the user can mark it complete (e.g. checkbox).  
   - A **floating action button (FAB)** opens the “Add task” dialog.  
   - If there are no tasks, an empty-state message is shown (e.g. “No tasks yet” / “Tap + to add a task”).

7. **School schedule screen (second tab)**  
   - Shows **today’s** lessons where `scheduleType` is “school”.  
   - Each lesson is displayed as a card: subject, teacher, classroom, and time range.  
   - Data is loaded from Firestore (filtered by current user, today’s day name, and schedule type).  
   - If there are no lessons today, an empty-state message is shown.

8. **After-school schedule screen (third tab)**  
   - Same as the school schedule tab, but for lessons with `scheduleType` “after_school”.  
   - Same loading and display logic, only the filter is different.

Throughout the flow, **navigation** is done with **Intents** (e.g. `startActivity(new Intent(this, LoginActivity.class))`) and **finish()** so the user cannot go “back” to login or setup after reaching the main screen. **Fragments** are used inside the setup wizard and inside MainActivity (ViewPager2) to keep each “page” in a separate, reusable component.

---

## 4. Technologies Used

### Android Studio
- The project is developed in **Android Studio**. The app is built with **Gradle** (build files are in Kotlin DSL: `build.gradle.kts`, `settings.gradle.kts`). The **AndroidManifest.xml** declares all activities and which one is the **launcher** (WelcomeActivity).

### Java
- The application code is written in **Java**. All activities, fragments, adapters, and helper classes are Java classes. The minimum SDK is **24**; the target and compile SDK is **34**, so the app uses modern Android APIs while still supporting older devices.

### Firebase Authentication
- **Firebase Authentication** is used for:
  - **Creating accounts** – `FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)`.
  - **Signing in** – `FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)`.
  - **Checking if the user is logged in** – `FirebaseAuth.getInstance().getCurrentUser()` (used on the welcome screen and in MainActivity).
  - **Signing out** – `FirebaseAuth.getInstance().signOut()`.
- The **user ID (UID)** of the current user is used in every Firestore query so that each user only sees and edits their own data.

### Cloud Firestore
- **Cloud Firestore** is the cloud database. The app uses:
  - **Collections:** `lessons`, `tasks`, `subjects`.
  - **Operations:** add document, update document, delete document, and query documents (e.g. “all lessons where userId equals current user”).
  - **Listeners / callbacks** – Firestore operations are asynchronous; the app uses `addOnSuccessListener` and `addOnFailureListener` (or similar) and updates the UI in the callback (e.g. refresh the list, show a toast).
- All Firestore logic is grouped in **FirestoreHelper** so the rest of the app does not depend on Firestore’s API details.

### RecyclerView
- **RecyclerView** is used to display **lists** efficiently:
  - **Tasks** – list of task items (title, due time, checkbox).
  - **Lessons in setup** – list of lesson cards per day (time slot + subject, teacher, classroom).
  - **School and after-school schedule** – list of lesson cards (read-only).
- For each list, an **Adapter** (e.g. `TaskAdapter`, `LessonSetupCardAdapter`, `LessonCardAdapter`) is responsible for creating a **ViewHolder** for each item and **binding** the data (e.g. a `Lesson` or `Task` object) to the item’s views. This reuses views and only updates what changed, which is efficient for long lists.

### ViewPager2
- **ViewPager2** is used for **swipeable pages**:
  - **Main screen** – three pages: Tasks, School schedule, After-school schedule. Each page is a **Fragment**. The adapter is a **FragmentStateAdapter** that returns the correct fragment for each position (0, 1, 2).
  - **Setup wizard Step 2** – one page per selected day; each page is a **DayScheduleFragment**. The list of fragments is built dynamically (e.g. after the user taps “Next” on Step 1).
- **TabLayout** is attached to the ViewPager2 on the main screen so the user can also tap a tab to switch pages. **TabLayoutMediator** connects the tabs to the ViewPager2 pages.

### Fragments
- **Fragments** are used to split the UI into reusable pieces:
  - **FrameSetupFragment** – Step 1 of the setup (checkboxes, time inputs). It returns a **FrameSetupData** object with the user’s choices.
  - **DayScheduleFragment** – one per day in Step 2; receives the day name and list of **TimeSlot**s; shows a RecyclerView of lesson cards and returns the list of **Lesson** objects when the activity asks for them.
  - **TasksFragment** – the tasks tab: loads tasks from Firestore, shows RecyclerView, FAB to add task.
  - **SchoolScheduleFragment** – the school tab: loads today’s school lessons and shows them in a RecyclerView.
  - **AfterSchoolScheduleFragment** – the after-school tab: same as school but for after-school lessons.
  - **AddLessonDialogFragment** and **AddTaskDialogFragment** – dialog-style fragments for adding (and in the lesson case, editing) a single lesson or task.
- Fragments are created and managed by the **Activity** (or by a **FragmentStateAdapter** inside the activity). Data is passed via **arguments** (e.g. `Fragment.newInstance(dayName, timeSlots)`) or by the activity calling methods on the fragment (e.g. `getFrameSetupData()`, `getLessons()`).

### Adapters
- **Adapters** connect **data lists** to **RecyclerView**:
  - **LessonSetupCardAdapter** – used in DayScheduleFragment. Holds a list of `Lesson`; each item shows time slot and EditTexts for subject, teacher, classroom. **TextWatcher**s update the `Lesson` object when the user types. Exposes `getLessons()` so the activity can collect the final list.
  - **LessonCardAdapter** – used in SchoolScheduleFragment and AfterSchoolScheduleFragment. Displays lessons in a read-only card (subject, teacher, classroom, time). The fragment calls `setLessons(list)` when data is loaded from Firestore.
  - **TaskAdapter** – used in TasksFragment. Displays tasks with a checkbox; when the user toggles the checkbox, a callback notifies the fragment so it can update Firestore.
- Concept: the **Activity or Fragment** owns the data and the reference to Firestore; the **Adapter** only knows how to draw one item and how to notify the fragment when the user interacts (e.g. checkbox, or in the future, edit/delete).

### Material Design components
- The app uses **Material Design** components for a consistent, modern look:
  - **MaterialButton** – e.g. on the welcome screen (Login, Create account) and in the setup wizard (Next, Previous, Save schedule).
  - **TextInputEditText** and **TextInputLayout** – for email, password, name, subject “Other”, teacher, classroom, and setup fields (start time, durations). **Hint** and **error** messages are shown using these components.
  - **FloatingActionButton (FAB)** – on the tasks screen to add a task.
  - **Toolbar** – at the top of MainActivity for title and menu.
  - **TabLayout** – with icons and labels for the three main tabs.
  - **ProgressBar** – during login, registration, and when saving the schedule.
- The app theme is defined in **themes.xml** (e.g. `Theme.MyAdvancedSchedule`) and uses Material components so that colors and styles are consistent.

---

## 5. Data Structure

### How data is stored (Firestore collections)

- All data is stored in **Cloud Firestore**. Each document is identified by an **auto-generated ID** (when adding) or by a known ID (when updating or deleting). Every document that belongs to a user includes a **userId** field so the app can filter by the current user.

### Users
- **User accounts** are managed by **Firebase Authentication**, not by a “users” collection in Firestore. Firebase Auth provides:
  - **UID** – unique user ID (used as `userId` in Firestore).
  - **Email** – used for login.
  - **Display name** – optional (e.g. from registration); can be shown in the toolbar.
- If the app had a user profile (e.g. name, photo), it could be stored in a **users** collection with document ID = UID, but in the current version the main data linked to the user are **lessons**, **tasks**, and **subjects**.

### Tasks
- Stored in the **tasks** collection.
- **Fields per document:**
  - **userId** (string) – who owns the task.
  - **title** (string) – task description.
  - **dueTime** (string) – optional, e.g. "14:30".
  - **completed** (boolean) – whether the task is done.
- The **Task** model class in the app has: id, title, dueTime, completed. The **id** is the Firestore document ID.

### Lessons
- Stored in the **lessons** collection.
- **Fields per document:**
  - **userId** (string) – who owns the lesson.
  - **subject** (string) – e.g. "Math", "English".
  - **teacher** (string).
  - **classroom** (string).
  - **day** (string) – e.g. "Monday", "Tuesday".
  - **period** (number) – e.g. 1, 2, 3.
  - **startTime** (string) – e.g. "08:00".
  - **endTime** (string) – e.g. "08:45".
  - **scheduleType** (string) – `"school"` or `"after_school"` so the app can show school and after-school tabs separately.
- The **Lesson** model class has the same fields plus an **id** (Firestore document ID). It has constructors for creating a new lesson (no id) and for loading from Firestore (with id).

### Subjects
- Stored in the **subjects** collection (optional).
- **Purpose:** to populate the **subject dropdown** when adding or editing a lesson, so the user can pick from previously used subjects or add a new one (“Other”).
- **Fields per document:**
  - **userId** (string).
  - **name** (string) – subject name.
- The app may also collect unique subject names from existing **lessons** for the same user and merge them with the subjects collection so the dropdown shows all subjects the user has ever used.

---

## 6. User Interaction

### Choosing subjects from dropdown lists
- In the **Add/Edit lesson** dialog, the **Subject** field is a **Spinner** (dropdown). The options are loaded from Firestore: the **FirestoreHelper.getSubjects()** method fetches subject names from the **subjects** collection and from existing lessons, then returns a sorted list. The dialog builds an **ArrayAdapter** and sets it on the Spinner. If the user has no subjects yet, the list can be empty and the user can still choose “Other” and type a new subject.

### Selecting times
- **Setup wizard Step 1:** The user types the **start time** in a text field (e.g. "08:00"). Lesson duration and break duration are numbers (minutes). The app then **computes** all time slots (e.g. 08:00–08:45, 08:55–09:40) and the user does not select each time manually in Step 1.
- **Add/Edit lesson dialog:** **Start time** and **End time** are **Spinners**. The app fills them with time options (e.g. from 07:00 to 22:00 in 15-minute steps). The user selects one start and one end time. **Day** and **Period** are also spinners (e.g. Sunday–Saturday, and 1–15).

### Adding new subjects
- In the lesson dialog, when the user selects **“Other”** in the subject dropdown, an extra **TextInputEditText** appears so they can type a new subject name. When they save the lesson, the app can call **FirestoreHelper.addSubject(userId, subjectName)** to store that name in the **subjects** collection. Next time they open the dialog, **getSubjects()** will include this name in the dropdown.

### Editing or deleting data
- **Lessons:**  
  - **Add** – from the main menu, “Add lesson” opens the dialog with empty fields; saving calls **FirestoreHelper.addLesson()**.  
  - **Edit** – the same dialog can be opened with an existing **Lesson** object (e.g. passed as an argument). The fields are pre-filled; saving calls **FirestoreHelper.updateLesson()**.  
  - **Delete** – (if implemented) the user could long-press a lesson in the list or tap a delete button in the edit dialog; the app would show a confirmation dialog and then call **FirestoreHelper.deleteLesson(lessonId)**.
- **Tasks:**  
  - **Add** – FAB opens the add task dialog; saving calls **FirestoreHelper.addTask()**.  
  - **Edit / complete** – toggling the checkbox updates the task’s **completed** field via **FirestoreHelper.updateTask()**.  
  - **Delete** – (if implemented) similar to lessons: confirm then **FirestoreHelper.deleteTask(taskId)**.

All of these interactions go through the **FirestoreHelper** so that the same Firestore structure and security rules can be used everywhere. The UI (activities and fragments) only prepares the data (e.g. a `Lesson` or `Task` object) and calls the appropriate helper method with a **listener** to show success or error (e.g. Toast, refresh list).

---

## 7. Architecture Overview

### How the project is organized

The app follows a **traditional Android structure**: **Activities** and **Fragments** for UI, **Adapters** for lists, **Helper classes** for backend logic, and **Layout XML** files for the structure and styling of each screen. There is no separate “ViewModel” or “Repository” layer; the fragments and activities call **FirestoreHelper** and **FirebaseAuth** directly and update the UI in callbacks.

### Activities
- **WelcomeActivity** – launcher; checks auth and navigates to Login/Register or MainActivity.
- **LoginActivity** – email/password login; on success goes to SetupScheduleActivity.
- **RegisterActivity** – email/password registration; on success goes to SetupScheduleActivity.
- **SetupScheduleActivity** – hosts the setup wizard (ViewPager2 with FrameSetupFragment and DayScheduleFragments); collects data and saves to Firestore; then starts MainActivity.
- **MainActivity** – hosts the three main tabs (ViewPager2 with TasksFragment, SchoolScheduleFragment, AfterSchoolScheduleFragment); handles menu (Add lesson, Logout).

Each activity is declared in **AndroidManifest.xml**. The **launcher** is WelcomeActivity. All other activities are started explicitly with **Intent**s.

### Fragments
- **FrameSetupFragment** – Step 1 UI and validation; exposes **getFrameSetupData()**.
- **DayScheduleFragment** – one per day; receives day name and time slots; builds initial lessons and uses **LessonSetupCardAdapter**; exposes **getLessons()**.
- **TasksFragment** – tasks list, FAB, **TaskAdapter**; loads/saves tasks via FirestoreHelper.
- **SchoolScheduleFragment** – today’s school lessons; **LessonCardAdapter**; loads via **getLessonsForToday("school", ...)**.
- **AfterSchoolScheduleFragment** – today’s after-school lessons; same as school but **getLessonsForToday("after_school", ...)**.
- **AddLessonDialogFragment** – dialog for add/edit lesson; uses FirestoreHelper for subjects, addLesson, updateLesson.
- **AddTaskDialogFragment** – dialog for add task; uses FirestoreHelper.addTask.

Fragments are either:
- **Hosted inside an Activity** (e.g. inside ViewPager2 in SetupScheduleActivity or MainActivity), or
- **Shown as dialogs** (DialogFragment), opened by the activity or another fragment with **show(getSupportFragmentManager(), "tag")**.

### Adapters
- **LessonSetupCardAdapter** – RecyclerView adapter for the setup wizard; list of **Lesson**; each row has time + subject/teacher/classroom; **getLessons()** returns the current list (updated by TextWatchers).
- **LessonCardAdapter** – RecyclerView adapter for school and after-school tabs; read-only list of **Lesson**; **setLessons(list)** to refresh.
- **TaskAdapter** – RecyclerView adapter for tasks; list of **Task**; checkbox triggers a callback so the fragment can update Firestore.

Adapters are **not** activities or fragments; they are used **by** fragments (or activities) and only know about one row layout and one type of object (Lesson or Task).

### Helper classes
- **FirestoreHelper** – central class for Firestore:
  - **Lessons:** addLesson, updateLesson, deleteLesson, getAllLessons, getLessonsForToday(scheduleType, dayName).
  - **Tasks:** addTask, updateTask, deleteTask, getTasks.
  - **Subjects:** getSubjects, addSubject.
  - Uses **FirebaseFirestore** and **FirebaseAuth**; all methods take a **userId** (or get it from current user) and use **listeners** (e.g. OnLessonsLoadedListener, OnOperationCompleteListener) to return results asynchronously.
- **ScheduleFragmentHelper** – utility only; e.g. **getTodayDayName()** returns the current day as a string ("Monday", "Tuesday", …) using **Calendar**, so that schedule fragments can filter “today’s” lessons.

Other helpers (e.g. **FirebaseHelper**) may exist for legacy or alternate features but are not the main path used by the activities and fragments described above.

### Layout XML files
- **activity_*.xml** – root layout for each activity (e.g. activity_welcome, activity_login, activity_register, activity_setup_schedule, activity_main). They define the overall structure: toolbar, ViewPager2, buttons, etc.
- **fragment_*.xml** – root layout for each fragment (e.g. fragment_frame_setup, fragment_day_schedule, fragment_tasks, fragment_schedule). They usually contain a RecyclerView, optional empty view, and optional FAB or other controls.
- **dialog_*.xml** – layout for dialog fragments (e.g. dialog_add_lesson, dialog_add_task): title, input fields, and buttons are often added by the fragment code (AlertDialog with setView).
- **item_*.xml** – one row/card layout for RecyclerView (e.g. item_lesson_setup_card, item_lesson_card, item_task). Each adapter inflates one of these for each item.
- **menu/main_menu.xml** – options menu for MainActivity (e.g. Add lesson, Logout).
- **values/strings.xml** – all user-visible strings (labels, hints, errors, toasts) so the app can be translated and kept consistent.
- **values/themes.xml**, **colors.xml** – app theme and colors, used by activities and Material components.

The **Java code** references these layouts by **R.layout.*_name** (e.g. R.layout.activity_main, R.layout.fragment_tasks) and finds views by **R.id.view_id** (e.g. findViewById(R.id.recyclerLessons)). This keeps UI structure in XML and behavior in Java, which is the standard Android approach.

---

## Summary Table

| Topic | Summary |
|-------|--------|
| **App name** | MyAdvancedSchedule |
| **Purpose** | Weekly schedule + daily tasks, stored in Firebase |
| **Users** | Students and anyone with a recurring weekly schedule |
| **Auth** | Firebase Auth (email/password); optional name on register |
| **Schedule** | Setup wizard (frame + per-day fill) → saved to Firestore `lessons` |
| **Tasks** | List + FAB add; completion saved to Firestore `tasks` |
| **Main UI** | ViewPager2 + 3 tabs: Tasks, School, After school |
| **Data** | Firestore: lessons, tasks, subjects; FirestoreHelper for all access |
| **Tech** | Java, Android Studio, Firebase Auth & Firestore, RecyclerView, ViewPager2, Fragments, Adapters, Material Design |
| **Architecture** | Activities (Welcome, Login, Register, Setup, Main) + Fragments + Adapters + FirestoreHelper + XML layouts |

You can use this document as a script for a presentation or as a study guide for your exam. For the exam, practice saying each section in your own words and explaining one flow end-to-end (e.g. “From login to seeing today’s school schedule”) and one technical detail (e.g. “How RecyclerView and the adapter work together” or “How getLessonsForToday filters data”).
