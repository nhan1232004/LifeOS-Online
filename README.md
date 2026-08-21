# 🚀 LifeOS Online - Personal Productivity & Workspace Suite

> **Live Demo**: [https://lifeos-online-2026-v1.netlify.app](https://lifeos-online-2026-v1.netlify.app)

**LifeOS Online** is an all-in-one personal productivity and life management Single Page Application (SPA) inspired by industry-leading tools like Notion, Trello, and TickTick. 

Built with **Vanilla JavaScript (ES6+)** without heavy UI framework overhead, LifeOS delivers near-instantaneous response times (<50ms initial load), smooth animations, and a responsive PWA experience across desktop and mobile devices.

---

## ✨ Key Features

### 📊 1. Overview & Smart Analytics Dashboard
- **KPI Widgets & Analytics**: Real-time stats tracking completed tasks, income/expenses, notes created, and habit consistency.
- **AI Weekly Digest**: Automated 7-day summary with a typewriter animation effect providing personalized productivity insights.

### 🗂️ 2. Advanced Project Management (Project Pro)
- **Multi-View Engine**: Seamlessly switch between **Kanban Board** (Drag & Drop), **List View**, and **Gantt Chart**.
- **Interactive Gantt View**: Visual timeline management with start and due date binding and a customized Dark Mode interface.
- **Sub-tasks & Auto Progress**: Nested sub-task support inside project items with dynamic progress bar calculation based on completion.

### 📝 3. Rich Text Editing & Knowledge Base
- **Block-Based Rich Editor (Editor.js)**: Supports headers, lists, checklists, code blocks, quotes, and delimiters.
- **AI Workspace Tools**:
  - *Smart Categorization*: Automatic note tag suggestion based on title and content context.
  - *Inline AI Writer*: AI text continuation and prompt completion tool directly within the editor.
- **Backlinks & Markdown Export**: Automatic cross-note reference detection and one-click `.md` file export.

### 💰 4. Personal Finance Tracker
- Income & expense categorization, net balance calculation, and financial breakdown visualizations via Chart.js.
- One-click CSV export for external analysis in Excel or Google Sheets.

### ⏱️ 5. Focus & Habit Suite
- **Pomodoro Timer**: Customizable focus timer with sound alerts and full-screen focus state.
- **Habit & Goal Tracker**: Streak tracking with GitHub-style contribution heatmaps and quarterly goal mapping.
- **Vocabulary Flashcards**: Spaced repetition flashcard system for language acquisition and exam prep.

---

## 🛠️ Tech Stack & Dependencies

- **Frontend Core**: HTML5, CSS3 (Modern Flexbox/Grid, Custom Properties, Glassmorphism UI), Pure Vanilla JavaScript (ES6+).
- **Third-Party Libraries**:
  - `Editor.js` - Block-styled Rich Text Editor.
  - `Frappe Gantt` - Interactive Gantt Chart rendering.
  - `Chart.js` - Data visualization & financial analytics.
  - `Lucide Icons` - Modern SVG icon system.
- **State & Storage**: `LocalStorage API` adapter with JSON backup export/import engine.
- **PWA & Deployment**: Netlify CDN, Service Worker for offline capability.

---

## 💡 Engineering Highlights

1. **Zero-Framework Overhead**: Eliminates React/Vue/Angular dependencies to maintain an extremely small bundle size (<250KB), loading in under 100ms.
2. **Modular Architecture**: Clean separation of concerns with isolated ES modules (`notes.js`, `todos.js`, `goals.js`, `export.js`, etc.) for maintainability and scalability.
3. **Mobile-First UX & PWA Integration**:
   - Adaptive breakpoints handling dynamic layout shifts.
   - Dedicated mobile Bottom Navigation Bar and Floating Action Button (FAB) for touch devices.
4. **Native Drag & Drop API**: Implemented interactive Kanban cards using native HTML5 Drag and Drop API without external drag libraries.

---

## 🚀 Local Setup & Installation

No complex `npm install` or build steps required. Run directly in your browser:

1. Clone the repository:
   ```bash
   git clone https://github.com/nhan1232004/LifeOS-Online.git
   ```
2. Open `index.html` directly in any web browser (or use the **Live Server** extension in VS Code).

---

## 📁 Directory Structure

```text
LifeOS-Online/
├── index.html              # Main Single Page Application shell
├── manifest.json           # Progressive Web App (PWA) configuration
├── sw.js                   # Service Worker for offline caching
├── css/
│   └── style.css           # Design tokens, layouts & responsive rules
└── js/
    ├── main.js             # Core state manager, router & render engine
    ├── workspace.js        # Workspace selector & storage adapter
    ├── projViews.js        # Multi-view renderer (Board, List, Gantt)
    └── modules/
        ├── cmd.js          # Quick Command Palette (Ctrl+K)
        ├── export.js       # JSON/CSV data backup & export engine
        ├── goals.js        # Goal tracking logic
        ├── habits.js       # Habit tracker & heatmap rendering
        ├── journal.js      # Daily journal & mood logger
        ├── mocktests.js    # Practice test & quiz engine
        ├── notes.js        # Editor.js integration & AI writing logic
        ├── pomodoro.js     # Focus timer
        ├── todos.js        # Todo & Task management
        └── vocab.js        # Spaced repetition flashcards engine
```

---

## 📄 License & Contact

Developed as a personal portfolio project demonstrating modern Frontend Engineering practices.

- **Developer**: Nhan (nhan1232004)
- **Live Demo**: [lifeos-online-2026-v1.netlify.app](https://lifeos-online-2026-v1.netlify.app)
