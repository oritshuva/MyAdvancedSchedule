## 1. General Project Description

MyAdvancedSchedule is an Android application that helps students manage their **school day**, **after‑school activities**, and **personal tasks** in one place.  
The app is designed primarily for middle‑school and high‑school students, but it can also be used by teachers or parents who want to track a student’s weekly schedule.

- **What the application does**  
  MyAdvancedSchedule lets the user:
  - Set up a **weekly school timetable** (days, lesson times, subjects, teachers, classrooms).
  - View a **daily school schedule** in a clear timetable layout.
  - Add and manage **tasks** (to‑dos and homework).
  - Add and manage **after‑school events** such as sports, meetings, and personal plans.
  - Get **reminders** and **share** tasks and events through other apps (WhatsApp, messages, email, etc.).

- **Who the users are**  
  - Students who want one organized place for all their school lessons, tasks, and after‑school activities.  
  - Optionally, parents or teachers who want to help students plan and follow their weekly routine.

- **What problem it solves**  
  Many students have their timetable on paper or scattered across different apps. MyAdvancedSchedule:
  - Centralizes **lessons, tasks, and after‑school events** in one application.
  - Prevents confusion about where to be and what to do next.
  - Reduces the chance of forgetting homework, tests, or activities.
  - Provides a **visual timetable** that is easy to understand at a glance.

- **Main features**
  - **Secure login and registration** with Firebase Authentication.
  - **Setup wizard** to configure the weekly timetable (days, start time, lesson length, breaks, and max lessons).
  - **School schedule screen** showing today’s lessons in a timetable‑style list.
  - **After‑school schedule screen** for events that happen after regular lessons.
  - **Tasks screen** for managing homework and to‑dos.
  - **Reminders** using Android notifications.
  - **Sharing** for tasks and after‑school events via Android’s share intent.
  - Data stored securely in **Firebase Firestore**, per authenticated user.

---

## 2. Application Flow

This section explains how the user moves through the application from start to finish.

### 2.1 Welcome and Authentication

1. **WelcomeActivity**
   - This is the launcher screen defined in the Android manifest.
   - It checks if a user is already authenticated with Firebase:
     - If **not logged in**, it shows a welcome UI with options to **Log in** or **Create account**.
     - If **logged in**, it skips ahead to the main part of the app.

2. **LoginActivity**
   - Allows an existing user to sign in with **email and password**.
   - On success, the user is directed to **SetupScheduleActivity** the first time, or to **MainActivity** once a schedule already exists.
   - Uses Firebase Authentication to validate the credentials.

3. **RegisterActivity**
   - Allows a new user to create an account with **name, email, and password**.
   - On successful registration, the user is also directed to **SetupScheduleActivity** to configure their timetable.

### 2.2 Schedule Setup Wizard

4. **SetupScheduleActivity**
   - A two‑step wizard that sets up the user’s weekly timetable.

   **Step 1 – FrameSetupFragment**
   - Asks the user:
     - Which **days of the week** they study (Sunday–Saturday).
     - **Start time** of the school day (e.g. 08:00).
     - **Lesson duration** (e.g. 45 minutes).
     - **Average break duration**.
     - **Maximum number of lessons** per day.
   - These values are stored in a data object (`FrameSetupData`).

   **Step 2 – DayScheduleFragment (one per selected day)**
   - For each selected day, a `DayScheduleFragment` is created with:
     - The day name (e.g. Monday).
     - A list of **TimeSlot** objects generated from the start time, lesson duration, and break duration.
   - Each fragment shows a vertical list of **lesson setup cards** (subject, teacher, classroom) for each time slot.
   - The user fills in the subject, teacher, and classroom for each lesson period.

   **Saving the schedule**
   - When the user finishes the wizard, `SetupScheduleActivity`:
     - Gathers the `Lesson` objects from all `DayScheduleFragment` instances.
     - Saves them into Firestore using `FirestoreHelper.addLesson`.
     - Navigates to **MainActivity**.

### 2.3 Main Application Screens

5. **MainActivity**
   - The central screen once the user has a schedule.
   - Contains:
     - A **Toolbar** with the app name and user information.
     - A **TabLayout + ViewPager2** with three tabs:
       1. **Tasks** (`TasksFragment`)
       2. **School** (`SchoolScheduleFragment`)
       3. **After School** (`AfterSchoolScheduleFragment`)
   - The **overflow menu** allows:
     - Adding a new lesson via `AddLessonDialogFragment`.
     - Logging out of the app.

6. **TasksFragment**
   - Shows a list of tasks in a RecyclerView.
   - Uses `TaskAdapter` to bind `Task` objects to card views.
   - Has a **FloatingActionButton (FAB)** to add a new task via `AddTaskDialogFragment`.
   - Each task card can:
     - Mark the task as complete or incomplete.
     - Delete the task.
     - Share the task via other apps.
     - Trigger a reminder notification.

7. **SchoolScheduleFragment**
   - Displays **today’s school lessons** in a timetable‑style list using `LessonCardAdapter`.
   - A header card shows:
     - Current **day name** and a title such as “Monday - Today Schedule”.
     - Current **date**.
   - Below the header, each lesson appears as a **MaterialCardView** with:
     - Lesson number.
     - Start and end times.
     - Subject, teacher, and classroom.
   - **Empty lesson slots** are shown as “Free period / חלון” cards so gaps are visible.

8. **AfterSchoolScheduleFragment**
   - Displays **after‑school lessons/events** (those with `scheduleType = "after_school"`).
   - Similar header card shows the current day and date with “After School Schedule”.
   - An **information text** explains that this screen is for activities such as homework, sports, meetings, and personal plans.
   - A **FAB (“Add Event”)** opens an event dialog where the user can add:
     - Title, start time, end time, description, and optional location.
   - Each event appears as a card and can be:
     - Shared through other apps.
     - Used with reminders (depending on configuration).

### 2.4 Logout and Exit

- From **MainActivity**, the user can:
  - Log out via the menu, which clears the FirebaseAuth session and returns them to `LoginActivity`.
  - Exit the application normally via the system back/home buttons.

---

## 3. Project Architecture

MyAdvancedSchedule follows a **modular, component‑based architecture** typical for Android apps. The main elements are:

- **Activities**: High‑level controllers for major parts of the app (welcome, auth, setup wizard, main tabs).
- **Fragments**: Modular UI controllers hosted inside activities (tabs and wizard steps).
- **RecyclerViews + Adapters**: Efficient list rendering for lessons, tasks, and schedule cards.
- **Models**: Plain Java classes representing data (Lesson, Task, Event, TimeSlot, User).
- **Firebase / Firestore**: Backend for authentication and data persistence.
- **Helper classes**: For Firestore operations, date calculations, and schedule logic.

### 3.1 Activities

- `WelcomeActivity`: Entry point; decides whether to show login/registration or go directly to MainActivity.
- `LoginActivity` / `RegisterActivity`: Handle user authentication with Firebase.
- `SetupScheduleActivity`: Hosts the 2‑step schedule setup wizard (FrameSetupFragment + multiple DayScheduleFragment instances).
- `MainActivity`: Hosts the main tabs (tasks, school schedule, after‑school schedule) via ViewPager2 and TabLayout.

Activities are thin; most logic is delegated to Fragments and helper classes.

### 3.2 Fragments

- `FrameSetupFragment`: First step of the setup wizard; collects global timetable settings.
- `DayScheduleFragment`: Second step; collects lessons for a specific day using a list of lesson cards.
- `TasksFragment`: Displays and manages user tasks.
- `SchoolScheduleFragment`: Shows today’s school lessons in a formatted timetable.
- `AfterSchoolScheduleFragment`: Shows after‑school lessons/events.

Each fragment manages its own view hierarchy and coordinates with `FirestoreHelper` or higher‑level Activities when required.

### 3.3 RecyclerViews and Adapters

- `RecyclerView` is used for:
  - The list of lesson setup cards (wizard).
  - The daily school schedule.
  - The after‑school schedule.
  - The tasks list.

- Adapters:
  - `LessonSetupCardAdapter`: For the setup wizard; binds each `TimeSlot` to a card allowing subject, teacher, and classroom input.
  - `LessonCardAdapter`: For the school and after‑school schedules; binds `Lesson` objects into timetable cards.
  - `TaskAdapter`: For the tasks screen; binds `Task` objects to cards with actions.

These adapters encapsulate binding logic and provide callbacks to fragments for actions such as task completion and deletion.

### 3.4 Firebase and Firestore

- **Firebase Authentication** (`FirebaseAuth`):
  - Used in `LoginActivity` and `RegisterActivity` to authenticate users via email/password.
  - The authenticated user’s UID is used as a key to store and query data in Firestore.

- **Firestore** (`FirebaseFirestore`):
  - `FirestoreHelper` centralizes all access to:
    - `lessons` collection.
    - `tasks` collection.
    - `subjects` collection.
  - This ensures a single, consistent API for reading and writing data.

### 3.5 Helper and Utility Classes

- `FirestoreHelper`: Handles all Firestore CRUD operations for lessons, tasks, and subjects.
- `ScheduleFragmentHelper`: Provides date and day utilities (e.g., “what is today’s day name?”).
- `TimeSlot`: Represents a start–end time pair for lessons.
- `ReminderUtils`: Creates notification channels and lightweight task reminders.

---

## 4. Project Folder Structure

The relevant parts of the project are structured as follows (simplified):

- `app/`
  - `src/main/java/com/example/myadvancedschedule/`
    - **Activities**:
      - `WelcomeActivity.java`
      - `LoginActivity.java`
      - `RegisterActivity.java`
      - `SetupScheduleActivity.java`
      - `MainActivity.java`
    - **Fragments**:
      - `FrameSetupFragment.java`
      - `DayScheduleFragment.java`
      - `TasksFragment.java`
      - `SchoolScheduleFragment.java`
      - `AfterSchoolScheduleFragment.java`
    - **Adapters**:
      - `LessonSetupCardAdapter.java`
      - `LessonCardAdapter.java`
      - `TaskAdapter.java`
    - **Models**:
      - `Lesson.java`
      - `Task.java`
      - `Event.java` (for legacy or extended event handling)
      - `TimeSlot.java`
      - `FrameSetupData.java`
      - `User.java` (if present)
    - **Firebase / Firestore helpers**:
      - `FirestoreHelper.java`
      - `FirebaseHelper.java` (legacy events helper, not used in main flow)
    - **Dialogs / UI helpers**:
      - `AddLessonDialogFragment.java`
      - `AddTaskDialogFragment.java`
      - `AddAfterSchoolEventDialogFragment.java`
    - **Utilities**:
      - `ScheduleFragmentHelper.java`
      - `ReminderUtils.java`

  - `src/main/res/layout/`
    - Activity and fragment layouts:
      - `activity_main.xml`
      - `activity_login.xml`
      - `activity_register.xml`
      - `activity_setup_schedule.xml`
      - `fragment_tasks.xml`
      - `fragment_schedule.xml` (for both school and after‑school tabs)
      - `fragment_day_schedule.xml`
    - Item / card layouts:
      - `item_lesson_card.xml`
      - `item_lesson_setup_card.xml`
      - `item_task.xml`
      - `item_event.xml` (legacy)
    - Dialog layouts:
      - `dialog_add_lesson.xml`
      - `dialog_add_task.xml`
      - `dialog_add_after_school_event.xml`

  - `src/main/res/values/`
    - `strings.xml` – all user‑visible text.
    - `colors.xml`, `colors_schedule.xml` – color definitions including subject indicators.
    - `styles.xml` – themes and Material styles.

This structure separates **logic (Java code)** from **presentation (XML layouts)** and groups files by their roles (activities, fragments, adapters, models, firebase helpers, etc.).

---

## 5. File‑by‑File Explanation (Key Files)

This section explains the responsibilities and interactions of the most important files.

### 5.1 MainActivity.java

- Hosts the main part of the app once the user is logged in and has a schedule.
- Sets up:
  - The **Toolbar** with the app title and, optionally, the user’s display name.
  - A **ViewPager2** and **TabLayout** with three tabs:
    - Tasks (index 0) → `TasksFragment`
    - School (index 1) → `SchoolScheduleFragment`
    - After School (index 2) → `AfterSchoolScheduleFragment`
- Loads fragments via an inner `MainPagerAdapter` (`FragmentStateAdapter`).
- Overflow menu options:
  - **Add lesson**: Opens `AddLessonDialogFragment`.
  - **Logout**: Signs out using FirebaseAuth and returns to `LoginActivity`.

### 5.2 WelcomeActivity.java

- Launcher activity defined in the manifest.
- Checks if a Firebase user is already authenticated:
  - If yes, navigates directly to `MainActivity`.
  - If no, shows a welcome UI with options to log in or register.
- Ensures users always pass through authentication before accessing data.

### 5.3 LoginActivity.java

- Provides UI for logging in with **email and password**.
- Uses `FirebaseAuth.signInWithEmailAndPassword`.
- On success:
  - Navigates to **SetupScheduleActivity** (if the user is new or has no schedule yet) or **MainActivity**.
- On failure:
  - Shows error messages via Toast and/or an error TextView.

### 5.4 RegisterActivity.java

- Provides UI for creating a new account.
- Uses `FirebaseAuth.createUserWithEmailAndPassword`.
- Validates:
  - Name (optional/required depending on configuration).
  - Email format.
  - Password length and confirmation.
- On success:
  - Navigates to `SetupScheduleActivity` to create the initial timetable.

### 5.5 SetupScheduleActivity.java

- Controls the **two‑step setup wizard**:
  - Step 1: Hosted in `FrameSetupFragment`.
  - Step 2: Multiple `DayScheduleFragment` instances (one per chosen day).
- Stores user input from Step 1 in a `FrameSetupData` object.
- Uses `computeTimeSlots` to generate `ArrayList<TimeSlot>` for each day.
- Creates `DayScheduleFragment` instances, each responsible for one day.
- On completion:
  - Gathers `Lesson` objects from all `DayScheduleFragment` instances.
  - Saves them to Firestore via `FirestoreHelper.addLesson`.
  - Moves the user to `MainActivity`.

### 5.6 FrameSetupFragment.java

- Part of the setup wizard (Step 1).
- UI elements:
  - Checkboxes for each day of the week.
  - Text fields for:
    - Start time of the school day.
    - Lesson duration.
    - Break duration.
    - Maximum number of lessons per day.
- Validates the input and returns a `FrameSetupData` object to `SetupScheduleActivity`.

### 5.7 DayScheduleFragment.java

- Part of the setup wizard (Step 2, repeated for each selected day).
- Receives:
  - The day name (e.g. “Monday”).
  - A list of `TimeSlot` objects (start–end times for each lesson).
- Builds an initial list of `Lesson` objects with the times filled and empty subject/teacher/classroom.
- Uses `LessonSetupCardAdapter` with a RecyclerView to show one editable card per time slot.
- Exposes a `getLessons()` method so `SetupScheduleActivity` can retrieve the final list of lessons for that day.

### 5.8 TasksFragment.java

- Manages the tasks tab in `MainActivity`.
- UI elements:
  - RecyclerView for the list of tasks.
  - Empty‑state layout for when there are no tasks.
  - FloatingActionButton to add tasks.
- Uses:
  - `TaskAdapter` to display each `Task` with title, due time, completion status, and action buttons.
  - `FirestoreHelper.getTasks` to load tasks for the current user.
  - `FirestoreHelper.updateTask` and `deleteTask` to persist changes.

### 5.9 SchoolScheduleFragment.java

- Shows **today’s school lessons**.
- UI based on `fragment_schedule.xml`:
  - A header card with:
    - Day name and a title string (e.g. “Monday - Today Schedule”).
    - The formatted current date.
  - A RecyclerView for lesson cards.
  - An empty view for when there are no lessons.
- Uses:
  - `ScheduleFragmentHelper.getTodayDayName` to get the current day.
  - `FirestoreHelper.getLessonsForToday("school", today, ...)` to load lessons.
  - `LessonCardAdapter` to show each lesson as a timetable card.

### 5.10 AfterSchoolScheduleFragment.java

- Shows after‑school lessons/events for today (`scheduleType = "after_school"`).
- Similar UI structure to `SchoolScheduleFragment`, but with:
  - Different title string (“After School Schedule”).
  - Additional info text explaining the purpose of the screen.
  - FloatingActionButton to **add events** via `AddAfterSchoolEventDialogFragment`.
- Uses:
  - `FirestoreHelper.getLessonsForToday("after_school", today, ...)` to load event‑like lessons.
  - `LessonCardAdapter` to display them.
  - Optional sharing integration via `LessonCardAdapter` callbacks.

### 5.11 LessonCardAdapter.java

- RecyclerView adapter used by both schedule fragments.
- Responsibilities:
  - Accepts a list of `Lesson` objects.
  - Sorts lessons by `period` to create a clear daily timeline.
  - Automatically inserts synthetic “Free period / חלון” lessons when there are gaps between numeric periods.
  - Binds each lesson to `item_lesson_card.xml`, which includes:
    - A colored vertical subject indicator.
    - Lesson number and time column.
    - Subject, teacher, and classroom fields, formatted with labels.
  - Optionally exposes a share callback when used in the After School screen.

### 5.12 TaskAdapter.java

- RecyclerView adapter used in `TasksFragment`.
- Responsibilities:
  - Holds a list of `Task` objects.
  - Binds each task to `item_task.xml`, which includes:
    - Checkbox for completion state.
    - Task title and due time.
    - Action buttons for share, reminder, and delete.
  - Exposes an `OnTaskActionListener` so `TasksFragment` can:
    - Update the task in Firestore when checked/unchecked.
    - Delete the task from Firestore when the delete button is pressed.

### 5.13 AddLessonDialogFragment.java

- Dialog for **adding or editing a single lesson** from within `MainActivity`.
- UI:
  - Spinners for subject, day, period, start time, end time.
  - Text fields for teacher and classroom.
  - Special handling for subject:
    - The subject spinner is populated from Firestore (`subjects` collection).
    - An “Other…” option reveals a text field for a new subject, which can be stored back to Firestore.
- Logic:
  - Validates the inputs.
  - Creates or updates a `Lesson` object.
  - Calls `FirestoreHelper.addLesson` or `updateLesson`.

### 5.14 FirestoreHelper.java

- Central Firestore access layer.
- Responsibilities:
  - **Lessons**:
    - `addLesson`, `updateLesson`, `deleteLesson`.
    - `getAllLessons` (for a user).
    - `getLessonsForToday(scheduleType, dayName, listener)`, which filters all lessons by day and `scheduleType` (“school” or “after_school”).
  - **Tasks**:
    - `addTask`, `getTasks`, `updateTask`, `deleteTask`.
  - **Subjects**:
    - `getSubjects` to populate the subject dropdown.
    - `addSubject` to save new subject names.
- Uses the currently logged‑in user’s UID to scope data to that user.

### 5.15 ReminderUtils.java

- Utility class for lightweight reminder notifications.
- Responsibilities:
  - Creates a notification channel for reminders (`myadvancedschedule_reminders`).
  - Shows simple notifications for tasks (and can be extended for events).

---

## 6. User Interface (UI)

The UI is defined using **XML layout files** and follows **Material Design** guidelines. Key UI elements:

- **Activities**:
  - Each activity has a corresponding `activity_*.xml` layout with root elements such as `CoordinatorLayout` or `ConstraintLayout`.
  - For example, `activity_main.xml` contains:
    - `AppBarLayout` with `Toolbar` and `TabLayout`.
    - `ViewPager2` for the fragments.

- **Fragments**:
  - `fragment_tasks.xml`: Contains a header card, RecyclerView for tasks, an empty view layout, and a FAB for adding new tasks.
  - `fragment_schedule.xml`: Shared layout for `SchoolScheduleFragment` and `AfterSchoolScheduleFragment`. Includes:
    - A header `MaterialCardView` with day title and date.
    - An info text (especially for After School).
    - A RecyclerView for lesson/event cards.
    - An empty view with icon and text.
    - Optionally a FAB for adding events (After School).
  - `fragment_day_schedule.xml`: Used by the setup wizard for per‑day lesson setup.

- **RecyclerViews and cards**:
  - `item_lesson_card.xml`:
    - `MaterialCardView` with:
      - A narrow vertical `View` used as a colored subject indicator.
      - Lesson number and time text fields.
      - Subject, teacher, and classroom text fields.
  - `item_lesson_setup_card.xml`:
    - `MaterialCardView` with the time slot and input fields (subject, teacher, classroom).
  - `item_task.xml`:
    - `MaterialCardView` with checkbox, title, due time, and action icons for share/reminder/delete.

- **Dialogs**:
  - `dialog_add_lesson.xml` for the Add/Edit Lesson dialog.
  - `dialog_add_task.xml` for adding a task.
  - `dialog_add_after_school_event.xml` for adding after‑school events.

All layouts use **Material components** such as `MaterialCardView`, `TextInputLayout`, and the Material design FAB to provide a consistent and modern UI.

---

## 7. Firebase and Firestore

The application uses two main Firebase services:

### 7.1 Firebase Authentication

- Provides user login and registration using email and password.
- Used in:
  - `LoginActivity` to sign in.
  - `RegisterActivity` to create a new user.
  - `WelcomeActivity` to check if a user is already logged in.
- Once the user is authenticated, the app retrieves their **UID** and uses it to read/write Firestore documents that belong only to that user.

### 7.2 Firestore Database

- Cloud Firestore is used as the main database.
- Collections:
  - `lessons`:
    - Stores all school and after‑school lessons for each user.
  - `tasks`:
    - Stores the user’s tasks and to‑dos.
  - `subjects`:
    - Stores user‑specific subject names for dropdowns.

### 7.3 How Data Is Saved and Loaded

- All Firestore access is centralized in `FirestoreHelper`.
- **Saving lessons**:
  - When the setup wizard or Add Lesson dialog creates a `Lesson` object, it calls `FirestoreHelper.addLesson(userId, lesson, listener)`.
  - `updateLesson` is used for editing existing lessons.
- **Loading lessons**:
  - `FirestoreHelper.getAllLessons` retrieves all of a user’s lessons.
  - `getLessonsForToday(scheduleType, todayDayName, listener)`:
    - Calls `getAllLessons` and filters by:
      - `scheduleType` (default “school” or explicitly “after_school”).
      - `day` field matching the desired day name.
    - The result is passed back to `SchoolScheduleFragment` or `AfterSchoolScheduleFragment`.

- **Saving tasks**:
  - Adding a task:
    - `AddTaskDialogFragment` creates a `Task` object and calls `FirestoreHelper.addTask(userId, task, listener)`.
  - Updating a task’s completed status:
    - `TasksFragment` calls `updateTask` when the user toggles the checkbox.
  - Deleting a task:
    - `TasksFragment` calls `deleteTask(taskId, listener)` when the delete action is triggered.

- **Loading tasks**:
  - `TasksFragment` calls `FirestoreHelper.getTasks(listener)` to retrieve all tasks for the current user and then displays them via `TaskAdapter`.

- **Subjects**:
  - `FirestoreHelper.getSubjects` loads:
    - Subject names from `subjects` collection.
    - Additional unique subject names from the user’s existing lessons.
  - `FirestoreHelper.addSubject` saves newly created subject names from the “Other…” option.

---

## 8. Data Structure

### 8.1 Lesson

`Lesson` represents a single lesson or an after‑school event.

Main fields:
- `id`: Firestore document ID.
- `subject`: Name of the subject or event (e.g. “Math”, “Basketball practice”).
- `teacher`: Name of the teacher or a description field for events.
- `classroom`: Classroom number or a location string for events.
- `day`: Day of the week in English (e.g. “Monday”).
- `period`: Integer corresponding to the lesson number.
- `startTime`: Lesson start time (e.g. "08:30").
- `endTime`: Lesson end time (e.g. "09:15").
- `scheduleType`: `"school"` or `"after_school"` (used for filtering).

### 8.2 Task

`Task` represents a to‑do item or homework.

Main fields:
- `id`: Firestore document ID.
- `title`: Short description of the task.
- `dueTime`: Due time (can be a simple string like “14:30” or “Tomorrow 10:00”).
- `completed`: Boolean flag indicating if the task is done.

### 8.3 Event (legacy / extended model)

`Event` is a more detailed model for events:

Main fields:
- `id`: Firestore document ID.
- `title`: Title of the event.
- `startTime`, `endTime`: Times of the event.
- `day`: Day name.
- `type`: `"school"` or `"after_school"`.
- `note`: optional note text.
- `reminderTime`: optional reminder time.
- `isPassed`: whether the event time has already passed.
- `userId`: UID for the owner of the event.
- `timestamp`: creation or last updated time.

In the main flow, after‑school events are often stored via the `Lesson` model with `scheduleType="after_school"` for consistency with the timetable.

---

## 9. Main Features

### 9.1 Managing Weekly Schedule

- A two‑step setup wizard allows the user to define:
  - Which days they study.
  - Start time, lesson duration, and break duration.
  - Maximum number of lessons.
- Per‑day screens let the user fill in subjects, teachers, and classrooms for each time slot.
- The result is a **complete weekly timetable** stored in Firestore.

### 9.2 Daily School Schedule

- The School tab shows **today’s schedule**:
  - Each lesson as a card with lesson number, time, subject, teacher, and classroom.
  - Free periods as clearly marked “Free period / חלון” cards.
  - A header card with the current day and date.
- The layout visually resembles school timetable apps with a vertical timeline style and subject color indicators.

### 9.3 Tasks Management

- The Tasks tab allows the user to:
  - Add tasks with title and due time.
  - Mark tasks as completed.
  - Delete tasks.
  - Share tasks through other apps.
  - Get reminder notifications for tasks.
- Tasks are stored in Firestore so they are tied to the authenticated user.

### 9.4 After‑School Events

- The After School tab allows the user to:
  - Add events with title, start and end times, description, and optional location.
  - View today’s after‑school events in a timeline‑like layout.
  - Share events via Android’s share intent.
  - (Can be extended to show reminders or additional status icons.)
- Events are stored in Firestore using the same `lessons` collection but marked with `scheduleType="after_school"`.

### 9.5 Reminders

- The app uses Android notifications to remind users about tasks.
- `ReminderUtils` simplifies notification channel creation and notification display.
- This can be expanded to support event reminders in a similar manner.

### 9.6 Sharing

- Tasks and after‑school events can be shared via Android’s **share intent** to:
  - WhatsApp.
  - SMS messages.
  - Email.
  - Any other compatible app installed on the device.

---

## 10. Technologies Used

- **Programming Language**
  - **Java**: All Activities, Fragments, Adapters, and helpers are written in Java.

- **Android Platform**
  - **Android Studio**: Primary development environment.
  - **AndroidX libraries**: Modern Android support libraries for compatibility and UI components.
  - **RecyclerView**: Efficient list component for schedules and tasks.
  - **ViewPager2 + TabLayout**: For the main tabbed interface.
  - **Material Design components**:
    - `MaterialCardView`, `TextInputLayout`, `FloatingActionButton`, and Material themes.

- **Firebase**
  - **Firebase Authentication**: Handles user login and registration.
  - **Cloud Firestore**: Main NoSQL database for lessons, tasks, subjects, and events.

- **Other Android APIs**
  - **SharedPreferences** (used or available for configuration and one‑time screens).
  - **Notification APIs**: To create and display reminders.

---

## 11. Summary

MyAdvancedSchedule is a complete Android application that helps students manage their **school timetable**, **tasks**, and **after‑school events** in one unified interface.  
It guides the user through a **setup wizard** to define the weekly schedule, and then provides three main tabs—Tasks, School, and After School—for daily use.

From a technical perspective, the project demonstrates:
- How to build a multi‑screen Android app using **Activities, Fragments, RecyclerView, and Material Design**.
- How to integrate **Firebase Authentication** for secure login.
- How to design a **Firestore data model** and a helper class (`FirestoreHelper`) to manage all database operations.
- How to build a **timetable UI** using RecyclerView cards, color indicators, and dynamic data.
- How to add **reminders** and **sharing** features using Android notifications and intents.

Overall, the project showcases modern Android development practices and provides a practical solution to a real‑world problem: helping students keep track of their lessons, tasks, and after‑school activities in a clear and organized way.

