# TaskFlow – Smart To-Do List

TaskFlow is a highly sophisticated, production-ready, offline-first Android organizer built using Android Studio, Kotlin, and Jetpack Compose. It implements custom database persistence, active push reminders, custom visual charts, preferences storage, and high-fidelity transitions following the Material Design 3 guidelines.

## 🚀 Key Highlights & Features

### 📅 Core Smart Tasks Engine
- **Task Parameters**: Manage task Title, Description, category binders, color badges, due dates, reminder timers, custom repeats, and favorite/pinned states.
- **Dynamic Organization**: Easily duplicate, archive, restore, or bulk edit/complete/delete selections.
- **Voice Quick Add Parser**: Speak or type simple command statements to create tasks dynamically (e.g. `add slides due tomorrow priority high`).

### 📊 Advanced Interactive Charts
- **Task Distribution Donut Chart**: Beautiful vector analytics rendering slice arcs for Completed, Pending, and Overdue tasks.
- **Weekly Trend Bar Chart**: Stunning custom canvas bars representing actual completions over the last 7 days.
- **Streaks tracker**: Automatically calculates and showcases consecutive productive days.

### 🗓️ Responsive Agenda Calendar
- **Interactive Grid**: Monthly visual schedule tracker. Tapping any date brings up all scheduled items, and overdue tasks are highlighted with high-contrast warning badges.

### ⚙️ Customized Settings
- **Aesthetic Modes**: Seamless Dark, Light, or System Theme configuration toggles using DataStore.
- **Dynamic Accessibility Scale**: Slide-adjustable custom font text multipliers applied system-wide.
- **Backup & Portability**: Backup/restore the primary local database file seamlessly, and export or import data using CSV format.

---

## 🛠️ Technology Stack & Architecture

- **UI Framework**: Modern Jetpack Compose & StateFlow.
- **Navigation**: Compose Navigation with adaptive arguments.
- **Local Persistence**: Room SQLite database with preloaded categories.
- **Task Scheduling**: WorkManager CoroutineWorkers for active push reminders.
- **Preferences Engine**: Jetpack DataStore Preferences.
- **Design System**: Material Design 3 theme tokens, custom adaptive brand icons, and soft dynamic borders.
