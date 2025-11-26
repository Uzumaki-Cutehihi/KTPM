## Tổng Quan Hiện Trạng
- Monolith Spring MVC (Thymeleaf, WebSocket) tại `src/` với bảo mật form/OAuth2 và trang người dùng/admin.
  - Cấu hình bảo mật: `src/main/java/com/scar/lms/config/SecurityConfiguration.java:39`–`:78`
  - WebSocket STOMP `/ws`: `src/main/java/com/scar/lms/config/WebSocketConfiguration.java:21`–`:33`
  - Luồng sách (search/list/detail/rating/borrow/return/favourite): `src/main/java/com/scar/lms/controller/BookController.java:57`, `:77`, `:124`, `:142`, `:167`, `:288`, `:307`, `:336`
- Kiến trúc microservices (Spring Boot) tại `services/` với Gateway định tuyến `/api/*`: `services/gateway/src/main/resources/application.yml:12`–`:53`
  - Catalog: CRUD sách, sự kiện cho Search; đã bổ sung Ratings và phân trang Books
  - IAM: đăng ký/đăng nhập JWT, đã bổ sung Favourites và CRUD Users
  - Borrowing: loan create/return, thống kê; đã bổ sung phân trang Loans
  - Search: Elasticsearch, Kafka consumer cho book events
  - Notification: email + Kafka consumers; đã thêm lưu trữ MongoDB
  - Media: Minio uploads
- Frontend (React, Vite + Tailwind) tại `bookvault-frontend/`, SPA tiêu thụ Gateway `/api/*`.
  - API base: `VITE_API_URL` dùng trong `src/config/api.js`
  - Trang chính: Home, Books, BookDetail, Search, Login/Register, Profile, Favourites, Borrowed/History, Admin skeleton, Chat (SockJS/STOMP)

## Thiếu Sót & Ưu Tiên
1. Bảo mật thống nhất JWT giữa Gateway và services (verify public key; role/authority mapping)
2. Hoàn thiện CRUD Admin UI + phân trang/sort/filter server-side
3. REST đồng bộ cho favourites/rating (đã thêm backend; cần mở rộng UI và validations)
4. Hoàn thiện APIs người dùng (IAM: `users/me`, đổi mật khẩu, profile/avatar)
5. Tích hợp đầy đủ Search (index, query, đồng bộ Kafka)
6. Thông báo (Notification) UI và APIs quản trị
7. Hoàn thiện Media upload từ frontend (cùng CORS và xác thực)
8. Bổ sung tests unit/integration (backend với Testcontainers; frontend với Vitest/RTL) và CI cho từng service
9. Giám sát/Observability (Prometheus/Actuator chuẩn hoá) và logging
10. Tối ưu hiệu năng (pagination, N+1, cache where appropriate)

## Backend – Hoàn Thiện
### IAM
- Thêm endpoints người dùng:
  - `GET /api/iam/v1/users/me`, `PUT /api/iam/v1/users/me`, `PUT /api/iam/v1/users/me/password`
  - Trả về claims từ JWT để frontend suy ra role
- CORS cho frontend Vite
- Migrations đảm bảo `favourites`, indexes: `services/iam/src/main/resources/db/migration/V2__favourites.sql`

### Catalog
- Ratings đã thêm (entity/repo/controller, migration):
  - `services/catalog/src/main/java/com/scar/bookvault/catalog/rating/*`
  - `services/catalog/src/main/resources/db/migration/V2__ratings.sql`
- Phân trang Books: `BookController.listPaged` `services/catalog/.../BookController.java:22`
- Bổ sung validations (ISBN pattern, unique checks) và DTOs

### Borrowing
- `GET /api/borrowing/v1/loans` phân trang: controller/service đã mở rộng
- Kiểm tra quantity/book availability qua Catalog (đã có WebClient): `services/borrowing/src/main/java/com/scar/bookvault/borrowing/service/LoanService.java:124`–`:144`
- Lịch sử mượn theo user, trạng thái, tính phí trễ (hiển thị trong UI)

### Search
- Đảm bảo Kafka consumer lập chỉ mục đầy đủ khi book created/updated/deleted
- `SearchController` hỗ trợ query/paged: `services/search/src/main/java/com/scar/bookvault/search/api/SearchController.java:60`

### Notification
- Đã thêm MongoDB lưu thông báo; hoàn thiện endpoints quản trị:
  - `GET/POST/PUT/DELETE /api/notification/v1/notifications`
- CORS và xác thực nếu cần

### Media
- Đảm bảo upload endpoint và phản hồi URL; CORS và xác thực

### Gateway & Bảo Mật
- Gateway verify JWT bằng public key (định tuyến, role-based): `services/gateway/src/main/resources/application.yml:61`–`:64`
- Thêm filter JWT cho routes yêu cầu auth; map `ROLE_USER/ROLE_ADMIN`
- Phát hành public key từ IAM: `services/iam/openapi.yaml:20`–`:26`; frontend decode dùng `jwt-decode`

## Frontend – Hoàn Thiện
- UI Tailwind (đã tích hợp): tinh chỉnh toàn bộ components/pages để đồng nhất
- Admin UI:
  - Users: bảng phân trang/sort/search, form create/update/delete
  - Books: bảng CRUD, filter, import từ Google Books (mirror monolith `BookController.searchAPI` `src/main/java/com/scar/lms/controller/BookController.java:57`)
  - Loans: bảng phân trang, return, filter theo trạng thái
  - Notifications: list/gửi/xoá
- Người dùng:
  - Hồ sơ: hiển thị từ JWT + gọi IAM `/users/me`; upload avatar qua Media
  - Favourites: list (IAM favourites paged) + toggle
  - Rating: list + submit (Catalog ratings)
- Tìm kiếm: gọi Search service (paged + filters UI), đồng bộ với Catalog
- Chat: SockJS/STOMP kết nối monolith `/ws` (đã có), UI Tailwind
- Interceptors/Errors: xử lý 401/403, toaster thông báo

## Dữ Liệu & Migration
- Kiểm tra schema Postgres cho các services; thêm migration cho mọi bảng mới (favourites/ratings)
- Docker Compose cập nhật MongoDB (đã thêm) và các môi trường Kafka/Elasticsearch/Minio hoạt động

## Sự Kiện & Đồng Bộ
- Catalog → Kafka (created/updated/deleted)
- Borrowing → Kafka (loan.created/returned/overdue)
- Notification/Search consume tương ứng, index/lưu thông báo

## Kiểm Thử & Chất Lượng
- Backend:
  - Unit tests cho service layer (Catalog/IAM/Borrowing/Notification)
  - Integration tests với Testcontainers (Postgres/Mongo/Kafka stub)
- Frontend:
  - Vitest + RTL cho pages/services (Books, Admin Users/Books/Borrows, Favourites/Ratings)
  - Mock axios instance và kiểm tra phân trang/render
- Coverage mục tiêu ≥70%

## CI/CD
- GitHub Actions cho từng service (build + test + Docker build push)
- Frontend pipeline (đã có), cập nhật secrets `VITE_API_URL` và deploy artifacts
- Optional: Helm charts (đã có dưới `platform/helm/`) tích hợp CD

## Observability & Vận Hành
- Actuator health/info/prometheus (đã có ở nhiều services); thống nhất expose `/actuator`
- Logging cấu hình JSON, correlation IDs (Gateway thêm `X-Trace-Id` đã có: `services/gateway/src/main/resources/application.yml:9`–`:11`)
- CORS chặt chẽ cho Frontend domain

## Lộ Trình Thực Hiện
1. Chuẩn hoá bảo mật JWT qua Gateway và services; thêm endpoints IAM `/users/me`
2. Hoàn thiện Admin APIs (IAM/Catalog/Borrowing/Notification) + pagination
3. Bổ sung UI Admin Tailwind và các trang người dùng (Favourites/Ratings/Profile)
4. Hoàn thiện Search Integration và UI tìm kiếm
5. Media upload & Notification UI
6. Viết tests backend/frontend và thiết lập CI cho mỗi service
7. Kiểm thử end-to-end và tối ưu hiệu năng/pagination

## Ràng Buộc & Giả Định
- Gateway phục vụ tất cả REST dưới `http://localhost:8080`
- JWT chứa thông tin role và username; frontend chỉ dựa vào token và IAM `/users/me`
- MongoDB dùng cho Notification; Postgres cho core services; Elasticsearch cho Search; Minio cho Media

Nếu bạn xác nhận kế hoạch, mình sẽ bắt đầu lần lượt: bảo mật JWT + IAM `/users/me`, hoàn thiện Admin APIs, cập nhật UI Tailwind, rồi viết tests/CI cho từng phần để đảm bảo hệ thống chạy ổn end-to-end.