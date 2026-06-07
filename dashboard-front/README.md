# EduCore — Teacher Dashboard

منصة EduCore التعليمية — لوحة تحكم المعلم

## Setup Instructions

### 1. Install Dependencies

```bash
cd "C:\Users\PC\Desktop\kher\Website creation prompt\educore"
npm install
```

### 2. Run Dev Server

```bash
npm start
# or
ng serve --open
```

App runs at: http://localhost:4200

### 3. Backend

Make sure your Spring Boot backend is running at: `http://localhost:8081`

---

## Project Structure

```
educore/
├── src/
│   ├── app/
│   │   ├── guards/          # Auth & Guest route guards
│   │   ├── interceptors/    # JWT HTTP interceptor
│   │   ├── layout/          # Shell, Sidebar, Topbar
│   │   │   ├── shell/
│   │   │   ├── sidebar/
│   │   │   └── topbar/
│   │   ├── models/          # TypeScript interfaces
│   │   ├── pages/           # All page components
│   │   │   ├── login/
│   │   │   ├── forgot-password/
│   │   │   ├── dashboard/
│   │   │   ├── courses/
│   │   │   ├── sessions/
│   │   │   ├── quizzes/
│   │   │   ├── students/
│   │   │   ├── staff/
│   │   │   ├── wallet/
│   │   │   ├── coupons/
│   │   │   ├── attendance/
│   │   │   ├── profile/
│   │   │   └── notifications/
│   │   ├── services/        # ApiService, AuthService
│   │   ├── app.component.ts
│   │   ├── app.config.ts
│   │   └── app.routes.ts
│   ├── index.html           # RTL + Cairo font
│   ├── main.ts
│   └── styles.css           # Tailwind + global styles
├── angular.json
├── tailwind.config.js
├── postcss.config.js
└── package.json
```

## Features

| Page | Route | Features |
|---|---|---|
| Login | `/login` | Phone + password, loading state |
| Forgot Password | `/forgot-password` | 3-step OTP flow |
| Dashboard | `/dashboard` | Stats, bar chart, pending students |
| Courses | `/courses` | Grid cards, CRUD modal, pagination |
| Sessions | `/sessions` | Two-panel, course→session→week |
| Quizzes | `/quizzes` | Filter bar, table, stats modal |
| Students | `/students` | 3 tabs: pending/active/banned |
| Staff | `/staff` | Table, permissions, CRUD |
| Wallet | `/wallet` | Top-up form, transactions |
| Coupons | `/coupons` | Tabs, usage bar, CRUD |
| Attendance | `/attendance` | QR + timer, live counter, search |
| Profile | `/profile` | Edit form, change password |
| Notifications | `/notifications` | Unread highlight, mark read |
