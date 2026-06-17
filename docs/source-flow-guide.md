# Tài liệu nhanh: source Kotlin và luồng hoạt động

## 1. Cấu trúc điều hướng chung

`MainActivity.kt` là điểm vào của ứng dụng. File này dựng theme Compose, bật `StrictMode` ở bản debug để phát hiện thao tác nặng trên UI thread, sau đó gọi `RootNavigation()`.

`RootNavigation()` bắt đầu ở route `session_gate`. Màn hình này đọc `SharedPreferences` với khóa `current_role` để tự đưa người dùng vào đúng giao diện:

- `admin`, `manager`, `receptionist` -> `admin_main`
- `employee`, `staff` -> `employee_main`
- `customer` -> `customer_main`
- chưa đăng nhập -> `login`

Admin và nhân viên có cây điều hướng riêng. Khách hàng có cây điều hướng riêng trong `CustomerMainScreen.kt`.

## 2. Ý nghĩa các file trong thư mục `screens`

`BookingItem.kt`

- Data class dùng chung để biểu diễn một đơn đặt bàn.
- Chứa thông tin như mã đơn, tên khách, số điện thoại, bàn, trạng thái, ngày giờ và tổng tiền.

`LoginScreen.kt`

- Màn hình đăng nhập/đăng ký.
- Lưu tài khoản cục bộ bằng `SharedPreferences`.
- Sau khi đăng nhập thành công, lưu `current_username`, `current_fullName`, `current_phoneNumber`, `current_role`.
- Dựa vào role để chuyển sang admin, nhân viên hoặc khách hàng.

`DashboardScreen.kt`

- Màn hình tổng quan của admin.
- Gọi API `/api/dashboard/stats`.
- Hiển thị các thống kê nhanh như tổng đơn, doanh thu, trạng thái bàn.

`BookingsListScreen.kt`

- Danh sách đơn đặt bàn cho admin và nhân viên.
- Tham số `canManageBookings` quyết định có được sửa/hủy/xóa/xác nhận đơn hay không.
- Tham số `canOrderFood` quyết định có được vào màn hình gọi món hay không.
- Admin dùng với quyền đầy đủ, nhân viên dùng quyền hạn chế.
- Gọi API `/api/bookings`, `/api/bookings/status`, `/api/tables/status`.

`CreateBookingScreen.kt`

- Màn hình admin/lễ tân tạo đặt bàn.
- Tải danh sách bàn từ `/api/tables`.
- Cho chọn khu A/B, chọn một hoặc nhiều bàn, nhập thông tin khách, ngày giờ, tiền cọc và ghi chú.
- Khi tạo thành công, gửi đơn lên `/api/bookings`, sau đó cập nhật bàn sang trạng thái đã đặt.

`EditBookingScreen.kt`

- Màn hình chỉnh sửa một đơn đặt bàn.
- Tải dữ liệu đơn từ `/api/bookings`.
- Gửi cập nhật qua `/api/bookings/{bookingId}`.

`CustomerMainScreen.kt`

- File lớn nhất vì chứa toàn bộ giao diện khách hàng.
- `CustomerMainScreen`: dựng bottom navigation cho khách.
- `CustomerBookingScreen`: khách đặt bàn, chọn sơ đồ bàn, chọn món trước khi xác nhận.
- `CustomerOrdersScreen`: khách xem đơn của mình, gọi món tiếp, hủy đơn hoặc xóa đơn đã hủy.
- `CustomerMenuScreen`: gọi món cho một đơn đã có.
- `CustomerTableViewScreen`: khách xem sơ đồ bàn ở chế độ chỉ xem.
- Form đặt bàn khách dùng `rememberSaveable` để không mất dữ liệu khi xoay màn hình.
- Gọi các API chính: `/api/customers/lookup`, `/api/tables`, `/api/menu`, `/api/bookings`, `/api/order-items/add`.

`SoDoBanScreen.kt`

- Sơ đồ bàn cho admin/nhân viên.
- Data class `TableData` biểu diễn bàn.
- Tải bàn từ `/api/tables`.
- Có thể đổi trạng thái bàn qua `/api/tables/status`.
- Đây là màn hình đồng bộ trạng thái bàn giữa đặt bàn, lễ tân, thanh toán và khách hàng.

`TableMapScreen.k.kt`

- Màn hình sơ đồ bàn dạng layout khác.
- Dùng `TableMapItem`.
- Gọi `/api/tables-layout`.
- Hiện tại không phải màn hình chính trong navigation mới, nhưng vẫn là source hỗ trợ sơ đồ bàn.

`LeTanScreen.kt`

- Màn hình lễ tân cũ/đơn giản.
- Dùng cho giao diện tiếp nhận, nhưng luồng chính hiện nay đang dùng `BookingsListScreen.kt` ở tab Lễ tân.

`PaymentScreen.kt`

- Màn hình thanh toán cho một đơn.
- Gọi `/api/bookings/{bookingId}/payment-summary` để lấy tiền món, giảm giá, tiền cọc, tổng cần thanh toán.
- Gửi thanh toán lên `/api/invoices`.
- Sau thanh toán, backend cập nhật đơn sang `checked_out` và trả bàn về `available`.
- Có hàm `printPaymentInvoice()` dùng Android Print Framework để in hóa đơn hoặc bản xem trước.

`InvoiceListScreen.kt`

- Lịch sử hóa đơn/thanh toán.
- Gọi `/api/invoices`.
- Cho tìm theo mã đơn, tên khách, SĐT và lọc theo ngày.
- Mỗi hóa đơn có nút `Chi tiết` và `In hóa đơn`.
- Nút in tải chi tiết hóa đơn rồi dùng lại `printPaymentInvoice()`.

`InvoiceDetailScreen.kt`

- Màn hình xem chi tiết một hóa đơn đã thanh toán.
- Gọi `/api/invoices/{invoiceId}`.
- Hiển thị thông tin khách, bàn, món đã gọi, giảm giá, tiền cọc, tổng thanh toán, ghi chú.
- Có nút `In hóa đơn` để in lại khi nhân viên quên in ở bước thanh toán.

`DataTransferScreen.kt`

- Màn hình sao lưu và khôi phục dữ liệu trong mục `Thêm`.
- Hiển thị tiến độ, trạng thái xử lý và file JSON gần nhất.
- Cho xuất bản sao JSON và khôi phục dữ liệu tham chiếu như bàn/thực đơn.

`DataTransferViewModel.kt`

- ViewModel cho `DataTransferScreen`.
- Dùng `SavedStateHandle` để giữ trạng thái tiến độ khi xoay màn hình.
- Dùng `ExecutorService` để chạy tác vụ nền, tránh làm đứng UI.
- Gọi các API `/api/bookings`, `/api/tables`, `/api/menu`, `/api/invoices`, `/api/admin/import-reference-data`.

`StaffCustomerLookupScreen.kt`

- Màn hình nhân viên tra cứu khách hàng bằng số điện thoại.
- Gọi `/api/customers/lookup`.
- Cho biết khách là vãng lai hay thân thiết, số lần ghé, tổng chi tiêu và phần trăm giảm giá.

`ProfileScreen.kt`

- Màn hình thông tin tài khoản.
- Có các component phụ để hiển thị header, thẻ thông tin, mục hành động và nút đăng xuất.

`ThemScreen.kt`

- Tab `Thêm` của admin.
- Dẫn tới các chức năng mở rộng như cấu hình, quản lý nhân sự, sao lưu/khôi phục dữ liệu, thông tin đồ án và đăng xuất.

## 3. Luồng Admin / Quản lý

1. Admin đăng nhập ở `LoginScreen`.
2. `RootNavigation` đọc role và đưa vào `AdminMainScreen`.
3. Admin có các tab:
   - `Trang chủ`: xem thống kê ở `DashboardScreen`.
   - `Lễ tân`: xem và xử lý đơn ở `BookingsListScreen`.
   - `Sơ đồ`: xem/sửa trạng thái bàn ở `SoDoBanScreen`.
   - `Thanh toán`: xem hóa đơn, chi tiết hóa đơn và in lại ở `InvoiceListScreen`.
   - `Thêm`: sao lưu dữ liệu, thông tin hệ thống, đăng xuất ở `ThemScreen`.
4. Khi tạo đơn mới, admin vào `CreateBookingScreen`, chọn khu/bàn và gửi đơn lên backend.
5. Khi khách đến, admin/lễ tân đổi trạng thái đơn sang phục vụ, bàn sang đang dùng.
6. Khi thanh toán, admin vào `PaymentScreen`, xác nhận thanh toán, backend tạo hóa đơn, trả bàn về trống.
7. Nếu quên in hóa đơn lúc thanh toán, admin vào tab `Thanh toán`, chọn `In hóa đơn` hoặc `Chi tiết` rồi in lại.

## 4. Luồng Nhân viên

1. Nhân viên đăng nhập với role `employee`.
2. `RootNavigation` đưa vào `EmployeeMainScreen`.
3. Nhân viên có các tab:
   - `Đơn`: xem đơn trong `BookingsListScreen`, quyền quản lý bị hạn chế.
   - `Sơ đồ`: chỉnh trạng thái bàn trong `SoDoBanScreen`.
   - `Hóa đơn`: xem, xem chi tiết và in hóa đơn trong `InvoiceListScreen`.
   - `Tra khách`: tìm khách theo SĐT trong `StaffCustomerLookupScreen`.
   - `Đăng xuất`.
4. Nhân viên được gọi món cho đơn đang phục vụ.
5. Nhân viên được thanh toán và xem hóa đơn.
6. Nhân viên không được chỉnh sửa/xóa đơn như admin vì `canManageBookings = false`.

## 5. Luồng Khách hàng

1. Khách đăng nhập hoặc đăng ký ở `LoginScreen`.
2. `RootNavigation` đưa vào `CustomerMainScreen`.
3. Khách có các tab:
   - `Đặt bàn`: nhập thông tin, chọn khu, chọn bàn, chọn món ăn trước khi xác nhận.
   - `Đơn của tôi`: xem các đơn theo tài khoản hoặc số điện thoại.
   - `Sơ đồ bàn`: xem trạng thái bàn ở chế độ chỉ xem.
   - `Đăng xuất`.
4. Khi khách nhập SĐT, app gọi `/api/customers/lookup` để kiểm tra khách cũ/thân thiết.
5. Nếu có lịch sử, backend tính giảm giá dựa trên số lần ghé/tổng chi tiêu.
6. Khi xác nhận đặt bàn, app tạo booking, lưu món đã chọn vào `BookingOrderItems`, cập nhật bàn sang đã đặt.
7. Sau khi có đơn, khách có thể vào `Đơn của tôi` để gọi món tiếp hoặc hủy đơn nếu còn trạng thái chờ.

## 6. Luồng dữ liệu chính

Đặt bàn:

`UI form -> /api/bookings -> bảng Bookings -> /api/tables/status -> bảng Tables`

Gọi món:

`UI chọn món -> /api/order-items/add -> bảng BookingOrderItems`

Thanh toán:

`PaymentScreen -> /api/bookings/{id}/payment-summary -> /api/invoices -> bảng Invoices -> cập nhật Bookings checked_out -> cập nhật Tables available`

In lại hóa đơn:

`InvoiceListScreen -> /api/invoices/{invoiceId} -> InvoiceDetailScreen hoặc printPaymentInvoice()`

Tra khách thân thiết:

`SĐT -> /api/customers/lookup -> tính visitCount, totalSpent, discountPercent`

Sao lưu:

`DataTransferScreen -> DataTransferViewModel -> ExecutorService -> gọi API -> ghi file JSON`

## 7. Điểm cần nhớ khi bảo trì

- `10.0.2.2:3001` là địa chỉ emulator Android dùng để gọi backend chạy trên máy tính.
- Role được lưu trong `SharedPreferences`, không phải database thật.
- Trạng thái bàn phải đồng bộ qua `/api/tables/status`.
- Hóa đơn đã thanh toán không nên tạo lại; backend có logic chống trùng hóa đơn theo `BookingID`.
- Màn hình khách dùng `rememberSaveable` để tránh mất form khi xoay màn hình.
- Tác vụ sao lưu dùng `ViewModel + SavedStateHandle + ExecutorService` để không làm treo giao diện.
