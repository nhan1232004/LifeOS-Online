# 🚀 Hướng dẫn Deploy LifeOS Online

## Bước 1 – Tạo Firebase Project (5 phút, miễn phí)

### 1.1 Tạo project
1. Vào **[console.firebase.google.com](https://console.firebase.google.com)**
2. Nhấn **"Add project"** → Đặt tên (VD: `my-lifeos-app`)
3. Tắt Google Analytics → Nhấn **Create project**

### 1.2 Kích hoạt Authentication
1. Sidebar → **Build → Authentication**
2. Nhấn **"Get started"**
3. Tab **Sign-in method** → Chọn **Google** → Enable → Save

### 1.3 Kích hoạt Firestore Database
1. Sidebar → **Build → Firestore Database**
2. Nhấn **"Create database"**
3. Chọn **"Start in test mode"** → Chọn location gần nhất → **Done**

### 1.4 Lấy Firebase Config
1. Sidebar → **Project Settings** (bánh răng ⚙️)
2. Cuộn xuống phần **"Your apps"** → Nhấn **</>** (Web)
3. Đặt tên app → Nhấn **Register app**
4. Copy đoạn config (trông như sau):

```js
const firebaseConfig = {
  apiKey: "AIzaSy...",
  authDomain: "my-lifeos-app.firebaseapp.com",
  projectId: "my-lifeos-app",
  storageBucket: "my-lifeos-app.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abc123"
};
```

---

## Bước 2 – Cập nhật Config vào code

Mở file `index.html`, tìm dòng có `PASTE_YOUR_API_KEY` và thay bằng config của bạn:

```js
const firebaseConfig = {
  apiKey:            "AIzaSy...",        // ← Dán vào đây
  authDomain:        "my-lifeos.firebaseapp.com",
  projectId:         "my-lifeos",
  storageBucket:     "my-lifeos.appspot.com",
  messagingSenderId: "123456789",
  appId:             "1:123456789:web:abc123",
};
```

> **Lưu ý:** Thay đúng 6 giá trị. Xoá phần `|| "PASTE_YOUR..."` đi luôn.

---

## Bước 3 – Deploy lên Internet (chọn 1 trong 3 cách)

### 🟢 Cách A: Netlify Drop (Nhanh nhất – 30 giây)
1. Vào **[netlify.com/drop](https://app.netlify.com/drop)**
2. Kéo thả **thư mục `LifeOS-Online`** vào trang
3. Netlify tự tạo URL dạng `https://random-name.netlify.app`
4. Để tùy chỉnh domain: **Site settings → Change site name**

### 🔵 Cách B: Firebase Hosting (Khuyên dùng)
```bash
npm install -g firebase-tools
firebase login
firebase init hosting      # Chọn project của bạn, public folder = "."
firebase deploy
```
→ URL: `https://my-lifeos-app.web.app`

### 🟣 Cách C: GitHub Pages (Miễn phí vĩnh viễn)
1. Tạo repository mới trên GitHub
2. Upload thư mục `LifeOS-Online` lên
3. Settings → Pages → Source: `main branch`
4. URL: `https://username.github.io/repo-name`

---

## Bước 4 – Thêm Authorized Domain (quan trọng!)

Sau khi có URL online:
1. Firebase Console → **Authentication → Settings → Authorized domains**
2. Nhấn **Add domain**
3. Nhập domain của bạn (VD: `random-name.netlify.app`)

---

## ✅ Kiểm tra hoạt động

1. Mở URL trên điện thoại
2. Nhấn **"Đăng nhập với Google"**
3. Thêm một sự kiện hoặc chi tiêu
4. Mở lại trên máy tính → Dữ liệu tự động xuất hiện! 🎉

---

## 💡 Tính năng của LifeOS Online

| Tính năng | Mô tả |
|-----------|-------|
| 🔐 Đăng nhập Google | Bảo mật, không cần mật khẩu |
| ☁️ Real-time sync | Dữ liệu cập nhật ngay lập tức |
| 📅 Lịch tháng | Xem và thêm sự kiện trực tiếp |
| 🗓 Lịch tuần | View theo giờ chi tiết |
| 📋 Kanban Board | Quản lý dự án 4 cột |
| ✅ Todo list | Việc cần làm theo ngày |
| 💰 Thu chi | Thêm thu nhập & chi tiêu nhanh |
| 📊 Thống kê | Biểu đồ phân tích tài chính |

---

## 🆓 Chi phí

- **Firebase Free tier (Spark):** Đủ dùng cá nhân
  - Firestore: 1GB lưu trữ, 50K đọc/ngày
  - Authentication: Không giới hạn
  - Hosting: 10GB bandwidth/tháng

---

## ❓ Gặp lỗi?

**Lỗi "Auth domain not authorized":**
→ Thêm domain vào Firebase Console (Bước 4)

**Lỗi "Permission denied":**
→ Đảm bảo Firestore ở chế độ "Test mode" (Bước 1.3)

**Không thấy nút Google sign-in:**
→ Kiểm tra lại Firebase Config (Bước 2)
