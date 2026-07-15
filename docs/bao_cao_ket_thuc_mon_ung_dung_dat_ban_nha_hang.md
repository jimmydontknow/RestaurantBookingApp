# BÁO CÁO KẾT THÚC MÔN HỌC
**Ứng dụng quản lý đặt bàn nhà hàng trên Android**
**Học phần: Lập trình thiết bị di động**
Đề tài: Xây dựng ứng dụng đặt bàn nhà hàng có quản lý bàn, đặt món, thanh toán hóa đơn, phân quyền người dùng và kết nối backend.
Ghi chú: Báo cáo này tổng hợp nội dung lý thuyết từ các bài giảng trên lớp, bỏ qua phần bài tập trong slide, đồng thời liên hệ trực tiếp với project Android đã triển khai.
# Mục lục nội dung
- Chương 1: Giới thiệu môi trường phát triển và nền tảng Android.
- Chương 2: Ứng dụng Android, Activity, vòng đời và điều hướng.
- Chương 3: Giao diện người dùng và các điều khiển.
- Chương 4: Xử lý sự kiện, lưu dữ liệu, lưu trạng thái và xử lý nền.
- Chương 5: Networking, đa phương tiện, liên lạc và vị trí.
- Chương 6: Một số vấn đề thiết bị thật, quyền, bảo mật và kiểm soát truy cập.
- Phân tích, thiết kế và mô tả chức năng ứng dụng đặt bàn nhà hàng.
- Kiểm thử, đánh giá, hạn chế và hướng phát triển.

# 1. Mở đầu
## 1.1 Lý do chọn đề tài
Trong thực tế, nhà hàng cần quản lý nhiều nghiệp vụ diễn ra đồng thời như nhận đặt bàn, xếp bàn, theo dõi trạng thái bàn, gọi món, thanh toán và lưu lịch sử khách hàng. Nếu xử lý thủ công bằng giấy hoặc ghi chú rời rạc, nhân viên dễ nhầm trạng thái bàn, thiếu thông tin đặt cọc, thất lạc món đã gọi hoặc khó kiểm tra khách hàng thân thiết.
Vì vậy, đề tài ứng dụng quản lý đặt bàn nhà hàng được lựa chọn để mô phỏng một hệ thống di động có tính thực tế: khách hàng có thể đặt bàn và gọi món; lễ tân hoặc quản lý có thể theo dõi đơn, sơ đồ bàn, hóa đơn; nhân viên có thể hỗ trợ vận hành với quyền hạn giới hạn. Đề tài phù hợp với nội dung môn Lập trình thiết bị di động vì chạm đến hầu hết kiến thức chính: giao diện, điều hướng, xử lý sự kiện, lưu trạng thái, gọi API, xử lý dữ liệu JSON, phân quyền và bảo mật cơ bản.
## 1.2 Mục tiêu đề tài
- Xây dựng ứng dụng Android phục vụ quy trình đặt bàn nhà hàng.
- Thiết kế giao diện cho ba vai trò: admin/quản lý, nhân viên và khách hàng.
- Quản lý sơ đồ bàn theo khu vực, trạng thái bàn và số lượng bàn.
- Cho phép khách hàng đặt bàn, chọn bàn, đặt cọc và chọn món ăn ngay trong quá trình đặt.
- Cho phép nhân viên/admin gọi món, thanh toán, in/xem hóa đơn và tra cứu khách hàng.
- Kết nối backend Node.js và SQL Server thông qua API để đồng bộ dữ liệu.
- Áp dụng kiến thức bài giảng về xử lý sự kiện, lưu trạng thái, networking, intent, đa phương tiện, liên lạc và vị trí.
- Đảm bảo code có tổ chức, dễ giải thích, có tách lớp xử lý mạng và hạn chế thao tác nặng trên UI thread.

## 1.3 Phạm vi thực hiện
Ứng dụng tập trung vào nghiệp vụ đặt bàn và phục vụ tại nhà hàng. Các chức năng chính gồm đăng nhập, phân quyền, đặt bàn, chọn sơ đồ bàn, gọi món, quản lý đơn, thanh toán, in hóa đơn, quản lý trạng thái bàn, tra cứu khách hàng thân thiết, sao lưu/khôi phục dữ liệu và tiện ích liên hệ/vị trí. Báo cáo không đi sâu vào phần bài tập trong các file bài giảng mà chỉ sử dụng phần kiến thức cốt lõi để giải thích cách áp dụng vào project.
# 2. Cơ sở lý thuyết theo bài giảng
## 2.1 Chương 1 - Môi trường phát triển Android
Bài giảng Chương 1 giới thiệu nền tảng phát triển ứng dụng di động, hệ điều hành Android, môi trường Android Studio, cấu trúc project và các thành phần cơ bản của ứng dụng. Đối với project này, môi trường phát triển chính là Android Studio với Kotlin và Jetpack Compose. Ứng dụng được tổ chức thành các màn hình trong thư mục `screens`, có `MainActivity.kt` làm điểm vào chính, đồng thời kết nối với backend Node.js và SQL Server.
| Nội dung lý thuyết | Cách áp dụng trong project |
| --- | --- |
| Android Studio và Gradle | Project dùng Gradle Kotlin DSL, module app và các dependency Compose/Navigation. |
| Cấu trúc project Android | Code chia thành MainActivity, screens, network, theme và backend riêng. |
| Thành phần ứng dụng | Activity chính quản lý navigation; các màn hình Compose đóng vai trò giao diện nghiệp vụ. |
| Sandboxing | Dữ liệu cục bộ như SharedPreferences nằm trong vùng riêng của app. |

## 2.2 Chương 2 - Ứng dụng, Activity, vòng đời và điều hướng
Chương 2 tập trung vào cấu trúc ứng dụng Android, Activity, vòng đời và điều hướng giữa các màn hình. Trong project, `MainActivity.kt` khởi tạo giao diện và điều hướng gốc. Khi app mở, màn `session_gate` kiểm tra thông tin đăng nhập trong SharedPreferences để chuyển người dùng vào đúng luồng: admin, nhân viên hoặc khách hàng.
Vòng đời Activity liên quan trực tiếp đến việc lưu trạng thái. Khi xoay màn hình, Activity có thể được tạo lại; nếu chỉ lưu dữ liệu bằng biến thường, nội dung form hoặc từ khóa tìm kiếm có thể bị mất. Vì vậy project đã dùng `rememberSaveable`, `SavedStateHandle` và SharedPreferences cho một số trạng thái quan trọng như tìm kiếm đơn, tra cứu khách hàng và phiên đăng nhập.
## 2.3 Chương 3 - Giao diện người dùng
Chương 3 trình bày layout, view hierarchy, common controls, advanced controls, custom layout, WebView và Intent. Project sử dụng Jetpack Compose thay vì XML truyền thống, nhưng tư duy giao diện vẫn tương tự: mỗi màn hình có cấu trúc layout, các control nhập liệu, nút, danh sách, menu điều hướng và component tái sử dụng.
| Nhóm giao diện | Màn hình áp dụng | Vai trò |
| --- | --- | --- |
| Form nhập liệu | CreateBookingScreen, CustomerMainScreen, EditBookingScreen | Nhập thông tin đặt bàn, số điện thoại, ngày giờ, tiền cọc, ghi chú. |
| Danh sách | BookingsListScreen, InvoiceListScreen | Hiển thị đơn đặt bàn và hóa đơn. |
| Sơ đồ/trạng thái | SoDoBanScreen, TableMapScreen.k.kt | Hiển thị bàn theo khu A/B và màu trạng thái. |
| Thanh điều hướng | AdminMainScreen, EmployeeMainScreen, CustomerMainScreen | Phân luồng tính năng theo vai trò. |
| Dialog/Toast | Nhiều màn hình | Thông báo lỗi, xác nhận, phản hồi sau thao tác. |
| Intent | RestaurantUtilitiesScreen | Gọi điện, SMS, email, chia sẻ, mở bản đồ. |

Về mặt thiết kế, giao diện được chia theo vai trò để tránh người dùng thấy quá nhiều chức năng không cần thiết. Admin có đầy đủ tab quản lý; nhân viên có quyền ít hơn; khách hàng chỉ thấy đặt bàn, đơn của tôi và sơ đồ bàn.
## 2.4 Chương 4 - Xử lý sự kiện, lưu trữ, lưu trạng thái và xử lý nền
### 2.4.1 Xử lý sự kiện
Bài giảng nhấn mạnh mô hình Source - Listener - Handler - Response. Trong project, sự kiện thường bắt đầu từ nút bấm hoặc lựa chọn của người dùng: xác nhận đặt bàn, xếp bàn, gọi món, thanh toán, hủy đơn, chọn ảnh, mở bản đồ. Sau khi nhận sự kiện, app validate dữ liệu, gọi backend hoặc cập nhật UI rồi phản hồi bằng Toast/trạng thái hiển thị.
- Nút `Xác nhận đặt bàn`: kiểm tra dữ liệu nhập, bàn đã chọn, tiền cọc và gửi API tạo booking.
- Nút `Xếp bàn`: cập nhật trạng thái đơn và trạng thái bàn.
- Nút `Gọi món`: mở màn chọn món và lưu món vào đơn/hóa đơn.
- Nút `Xác nhận thanh toán`: tính tiền món, trừ tiền cọc, áp dụng giảm giá và tạo hóa đơn.
- Thanh tìm kiếm lễ tân: lọc đơn theo mã, tên khách hoặc số điện thoại.

### 2.4.2 Validate form
Validate form là yêu cầu quan trọng để tránh gửi request sai lên backend. Trong project, các trường cần kiểm tra gồm họ tên, số điện thoại, ngày đặt, giờ đặt, khu vực, số lượng khách, bàn đã chọn và tiền đặt cọc. Nếu dữ liệu thiếu hoặc sai, app hiển thị lỗi để người dùng sửa trước khi tiếp tục.
### 2.4.3 Lưu dữ liệu cục bộ
Theo bài giảng, SharedPreferences phù hợp với dữ liệu nhỏ dạng key-value như trạng thái đăng nhập, cấu hình, role hiện tại. Project hiện lưu phiên đăng nhập và role trong SharedPreferences. Dữ liệu nghiệp vụ chính không lưu cục bộ bằng SQLite/Room trên Android mà lưu ở SQL Server thông qua backend.
| Loại dữ liệu | Nơi lưu trong project | Ghi chú |
| --- | --- | --- |
| Phiên đăng nhập, role, thông tin tài khoản local | SharedPreferences | Dùng để tự chuyển màn khi mở app. |
| Từ khóa tìm kiếm/lọc | rememberSaveable + SharedPreferences | Giữ trạng thái khi xoay màn hình hoặc quay lại màn. |
| Đơn đặt bàn, bàn, món ăn, hóa đơn | SQL Server qua backend | Là dữ liệu nghiệp vụ chính. |
| Dữ liệu export/import | File JSON + backend endpoint | Phục vụ sao lưu/khôi phục dữ liệu. |

### 2.4.4 Lưu trạng thái
Chương 4.3 nêu vấn đề app mất dữ liệu khi xoay màn hình, rời app hoặc process bị kill. Project đã áp dụng `rememberSaveable` cho một số trạng thái UI và `SavedStateHandle` trong ViewModel tra cứu khách hàng. Điều này giúp từ khóa tìm kiếm hoặc kết quả tra cứu không bị mất khi cấu hình thiết bị thay đổi.
### 2.4.5 Xử lý nền
Chương 4.4 yêu cầu không để tác vụ nặng chạy trực tiếp trên UI thread. Project dùng coroutine với `Dispatchers.IO` cho các thao tác mạng, đồng thời có trạng thái loading để tránh người dùng bấm nhiều lần. Màn sao lưu/khôi phục dữ liệu dùng ViewModel và xử lý nền để tránh treo giao diện khi export/import dữ liệu.
## 2.5 Chương 5 - Networking, Multimedia, Telephony và Location
Chương 5 đưa ứng dụng từ chạy cục bộ sang kết nối hệ thống bên ngoài và sử dụng chức năng thật của thiết bị. Project đã áp dụng rõ nhất ở kết nối backend Node.js, xử lý JSON API, chọn ảnh, gọi điện/SMS/email/chia sẻ và mở bản đồ.
| Năng lực Chương 5 | Áp dụng trong project |
| --- | --- |
| Networking | Android gọi API backend qua `http://10.0.2.2:3001`, nhận/trả JSON cho bàn, đơn, món, hóa đơn, khách hàng. |
| API client/repository | Tách `ApiClient.kt` và `RestaurantRepository.kt` để gom base URL, timeout và xử lý lỗi mạng. |
| Multimedia | Màn tiện ích nhà hàng cho phép chọn ảnh món ăn/không gian và hiển thị trong app. |
| Telephony/Contact | Dùng Intent mở gọi điện, SMS, email và chia sẻ thông tin nhà hàng. |
| Location | Mở Google Maps bằng trình duyệt để tránh lỗi app Maps cũ trên emulator. |

Một điểm kỹ thuật quan trọng là `10.0.2.2` là địa chỉ đặc biệt để emulator Android gọi về máy tính thật. Nếu chạy trên điện thoại thật, địa chỉ này cần đổi sang IP LAN của máy chạy backend.
## 2.6 Chương 6 - Thiết bị thật, quyền và bảo mật
Chương 6 nhấn mạnh việc dùng thiết bị thật phải quan tâm đến quyền, cảm biến, thao tác cảm ứng, bảo mật dữ liệu và kiểm soát truy cập. Project không đi sâu vào sensor, nhưng có áp dụng các nguyên tắc bảo mật và quyền tối thiểu trong liên lạc, vị trí, phân quyền và logging.
- Dùng `ACTION_DIAL` thay vì gọi trực tiếp để người dùng xác nhận cuộc gọi, không cần xin quyền CALL_PHONE.
- Mở SMS/email bằng app hệ thống để người dùng kiểm tra nội dung trước khi gửi.
- Mở bản đồ bằng trình duyệt thay vì phụ thuộc vào app Google Maps trên emulator.
- Phân quyền giao diện thành admin, nhân viên và khách hàng.
- Không nên log mật khẩu, số điện thoại, hóa đơn hoặc response chứa thông tin cá nhân.
- Dữ liệu nghiệp vụ quan trọng cần được kiểm soát ở backend, không chỉ ẩn nút trên UI.

# 3. Phân tích yêu cầu hệ thống
## 3.1 Tác nhân sử dụng
| Tác nhân | Nhu cầu chính | Quyền hạn |
| --- | --- | --- |
| Khách hàng | Đặt bàn, chọn bàn, chọn món, xem đơn, hủy đơn | Chỉ thao tác với đơn của mình, xem sơ đồ bàn ở chế độ chỉ đọc. |
| Nhân viên | Xem đơn, chỉnh trạng thái bàn, gọi món, thanh toán, tra khách | Không có đầy đủ quyền quản trị như admin. |
| Admin/Quản lý/Lễ tân | Quản lý toàn bộ đơn, bàn, hóa đơn, dữ liệu và tiện ích | Có quyền đầy đủ trong hệ thống vận hành. |

## 3.2 Yêu cầu chức năng
- Đăng nhập và tự điều hướng theo vai trò.
- Tạo đơn đặt bàn từ phía khách hàng hoặc admin/lễ tân.
- Chọn khu vực bàn: khu A gồm 20 bàn, khu B gồm 10 bàn.
- Hiển thị sơ đồ bàn và đồng bộ trạng thái trống, đã đặt, đang dùng.
- Cho phép gọi món trước hoặc trong quá trình phục vụ.
- Lưu món đã gọi vào hóa đơn của bàn/đơn.
- Tính tổng tiền món ăn, trừ tiền cọc, áp dụng giảm giá khách thân thiết.
- Thanh toán và in/xem chi tiết hóa đơn.
- Tra cứu khách hàng bằng số điện thoại hoặc tên.
- Xóa các đơn đã hủy.
- Export/import dữ liệu phục vụ sao lưu.
- Tiện ích nhà hàng: kiểm tra API, chọn ảnh, liên lạc, mở bản đồ.

## 3.3 Yêu cầu phi chức năng
- Giao diện dễ dùng, phân tách theo vai trò.
- Không bị mất trạng thái tìm kiếm khi xoay màn hình.
- Không treo UI khi gọi API hoặc xử lý dữ liệu.
- Backend là nguồn dữ liệu chính để các giao diện đồng bộ với nhau.
- Thông báo lỗi rõ ràng: lỗi mạng, lỗi server, lỗi dữ liệu.
- Có cấu trúc code dễ bảo trì, tách bớt logic mạng khỏi UI.
- Hạn chế quyền nguy hiểm, ưu tiên Intent để người dùng xác nhận hành động.

# 4. Thiết kế hệ thống
## 4.1 Kiến trúc tổng thể
Project được xây dựng theo mô hình client - server. Ứng dụng Android là client giao tiếp với backend Node.js thông qua REST API. Backend kết nối SQL Server để lưu trữ dữ liệu nghiệp vụ. Cách tổ chức này giúp nhiều giao diện cùng dùng chung một nguồn dữ liệu và tránh tình trạng mỗi máy Android có dữ liệu riêng.
| Lớp | Thành phần | Trách nhiệm |
| --- | --- | --- |
| UI Layer | Các màn hình Compose trong `screens` | Hiển thị dữ liệu, nhận thao tác người dùng, điều hướng. |
| State/ViewModel | `StaffCustomerLookupViewModel`, rememberSaveable | Giữ trạng thái tìm kiếm, loading, kết quả. |
| Network Layer | `ApiClient.kt`, `RestaurantRepository.kt` | Gom URL, gọi API, xử lý lỗi, parse dữ liệu cần thiết. |
| Backend Layer | `restaurant-backend/server.js` | Cung cấp API cho booking, tables, menu, invoices, lookup. |
| Database Layer | SQL Server theo `databaseRB2.sql` | Lưu bàn, đơn, món, hóa đơn, trạng thái và lịch sử. |

## 4.2 Luồng dữ liệu
1. Người dùng thao tác trên màn hình Android, ví dụ nhấn xác nhận đặt bàn.
2. Màn hình kiểm tra dữ liệu nhập và tạo request JSON.
3. App gọi API backend qua HTTP.
4. Backend kiểm tra dữ liệu, ghi hoặc đọc SQL Server.
5. Backend trả kết quả JSON.
6. App cập nhật UI, trạng thái bàn, danh sách đơn hoặc hóa đơn.
7. Người dùng nhận phản hồi bằng Toast, loading state hoặc dữ liệu mới trên màn hình.

## 4.3 Thiết kế dữ liệu chính
| Nhóm dữ liệu | Ý nghĩa |
| --- | --- |
| Tables | Danh sách bàn, số bàn, khu vực, sức chứa, trạng thái. |
| Bookings | Thông tin đặt bàn: khách, số điện thoại, ngày giờ, bàn, tiền cọc, trạng thái. |
| Menu | Danh sách món ăn, giá, danh mục. |
| Order Items | Các món đã gọi theo từng booking. |
| Invoices | Hóa đơn thanh toán, tổng tiền, giảm giá, phương thức thanh toán. |
| Customers/History | Thông tin tra cứu khách hàng thân thiết dựa trên số điện thoại/tên và lịch sử hóa đơn. |

## 4.4 Phân quyền
Phân quyền là điểm quan trọng vì cùng một dữ liệu nhưng mỗi vai trò được thao tác khác nhau. Admin có quyền rộng nhất; nhân viên có quyền vận hành; khách hàng chỉ tương tác với đơn của mình. Cách làm này phù hợp với nội dung Chương 6 về kiểm soát truy cập theo vai trò.
# 5. Mô tả chức năng ứng dụng
## 5.1 Đăng nhập và khôi phục phiên
Khi mở app, `session_gate` đọc role hiện tại từ SharedPreferences. Nếu có phiên đăng nhập, app chuyển thẳng vào giao diện tương ứng. Khi đăng xuất, app xóa các key phiên hiện tại để lần mở sau quay về màn login.
## 5.2 Chức năng khách hàng
- Đặt bàn: nhập họ tên, số điện thoại, ngày giờ, khu vực, số lượng khách, tiền cọc và yêu cầu đặc biệt.
- Chọn sơ đồ bàn: khách chọn bàn theo khu A/B thay vì nhập thủ công.
- Chọn món ngay khi đặt: món được lưu vào đơn và dùng cho hóa đơn sau này.
- Đơn của tôi: xem đơn theo tài khoản/số điện thoại, gọi món thêm, hủy bàn nếu cần.
- Sơ đồ bàn: xem trạng thái bàn đồng bộ với admin, không được chỉnh sửa.

## 5.3 Chức năng admin/quản lý/lễ tân
- Dashboard: xem tổng quan hoạt động.
- Lễ tân: quản lý đơn đặt bàn, tìm kiếm, lọc trạng thái, xếp bàn, gọi món, hủy/xóa đơn hủy.
- Sơ đồ bàn: xem và cập nhật trạng thái bàn.
- Thanh toán: xem hóa đơn, tính tiền, xác nhận thanh toán, in hóa đơn.
- Thêm: sao lưu/khôi phục dữ liệu, tiện ích nhà hàng, đăng xuất.

## 5.4 Chức năng nhân viên
- Xem danh sách đơn và hỗ trợ gọi món.
- Cập nhật trạng thái bàn.
- Xem hóa đơn và thực hiện thanh toán.
- Tra cứu khách hàng để xác định khách vãng lai hay khách thân thiết.
- Không có toàn bộ chức năng quản trị như admin.

## 5.5 Gọi món và hóa đơn
Món ăn có thể được chọn ngay khi đặt bàn hoặc thêm sau trong màn gọi món. Các món đã gọi được lưu lại theo booking để khi mở lại vẫn thấy danh sách cũ. Khi thanh toán, hệ thống lấy tiền món ăn làm cơ sở tính hóa đơn, trừ tiền cọc và giảm giá khách thân thiết nếu có.
## 5.6 Thanh toán, giảm giá và in hóa đơn
Tổng tiền thanh toán được tính theo công thức: tổng tiền món ăn trừ giảm giá và trừ tiền cọc. Tiền cọc chỉ là tiền giữ bàn, không phải doanh thu món ăn. Sau khi thanh toán, đơn chuyển sang trạng thái đã thanh toán/đã rời và bàn được giải phóng. Chức năng in hóa đơn giúp nhân viên in tại thời điểm thanh toán hoặc xem/in lại sau đó.
## 5.7 Tra cứu khách hàng thân thiết
Nhân viên có thể nhập tên hoặc số điện thoại để kiểm tra khách từng ghé nhà hàng chưa. Backend tìm trong lịch sử hóa đơn, trả về số lượt ghé, tổng chi tiêu và mức giảm giá. Mức giảm giá được áp dụng theo giá trị đơn cũ, khoảng 2% đến 7%.
## 5.8 Tiện ích nhà hàng
Màn tiện ích nhà hàng là phần áp dụng Chương 5 vào chức năng thật của app: kiểm tra kết nối API, chọn ảnh món ăn/không gian, mở gọi điện/SMS/email/chia sẻ và mở bản đồ bằng trình duyệt. Việc mở bản đồ bằng web giúp tránh lỗi Google Maps cũ trên emulator.
# 6. Áp dụng kiến thức môn học vào project
| Nội dung môn học | Minh chứng trong project |
| --- | --- |
| Activity, lifecycle, navigation | MainActivity điều hướng theo role; navigation Compose cho các màn hình. |
| UI controls | Form đặt bàn, bottom navigation, button, text field, dropdown, danh sách, card. |
| Event handling | Click xác nhận đặt bàn, xếp bàn, gọi món, thanh toán, tìm kiếm. |
| Validate form | Kiểm tra trường bắt buộc, số điện thoại, số khách, bàn đã chọn, tiền cọc. |
| SharedPreferences | Lưu phiên đăng nhập, role và một số trạng thái nhỏ. |
| State saving | rememberSaveable, SavedStateHandle cho tìm kiếm và tra khách. |
| Background processing | Coroutine Dispatchers.IO khi gọi API hoặc xử lý dữ liệu. |
| Networking API | ApiClient gọi backend Node.js, parse JSON, xử lý lỗi mạng/server. |
| Multimedia | Chọn và hiển thị ảnh trong tiện ích nhà hàng. |
| Telephony/Intent | ACTION_DIAL, SMS, email, share intent. |
| Location | Mở bản đồ bằng link Google Maps web. |
| Security/RBAC | Ba vai trò người dùng, giới hạn quyền nhân viên/khách hàng. |

# 7. Kiểm thử
## 7.1 Các tình huống kiểm thử chính
| Mã | Tình huống | Kết quả mong muốn |
| --- | --- | --- |
| TC01 | Đăng nhập bằng tài khoản admin | Vào giao diện admin có đủ tab quản lý. |
| TC02 | Đăng nhập bằng tài khoản nhân viên | Vào giao diện nhân viên, không thấy chức năng quản trị đầy đủ. |
| TC03 | Khách hàng đặt bàn khu A/B | Đơn được tạo, bàn đổi trạng thái đã đặt. |
| TC04 | Lễ tân xếp bàn | Đơn chuyển trạng thái phục vụ, bàn chuyển đang dùng. |
| TC05 | Gọi món cho booking | Món được lưu và hiển thị lại khi mở màn gọi món. |
| TC06 | Thanh toán hóa đơn | Tổng tiền đúng, trừ tiền cọc, áp dụng giảm giá nếu có. |
| TC07 | Xoay màn hình khi đang tìm kiếm | Từ khóa tìm kiếm không bị mất. |
| TC08 | Mở bản đồ trên emulator | Mở bằng trình duyệt, không kẹt ở màn update Google Maps. |
| TC09 | Backend tắt hoặc mất mạng | App hiển thị thông báo lỗi kết nối rõ ràng. |

## 7.2 Kết quả kiểm thử hiện tại
Trong quá trình phát triển, project đã được build bằng Gradle với task `:app:assembleDebug`. Các lần build gần nhất đều thành công sau khi sửa lỗi compile và lỗi mở bản đồ. Một số kiểm thử UI cần thực hiện trực tiếp trên emulator thông qua Android Studio vì môi trường hiện tại không có sẵn adb để tự cài APK.
# 8. Đánh giá
## 8.1 Ưu điểm
- Đề tài có tính thực tế và bao quát nhiều nghiệp vụ nhà hàng.
- Có phân quyền ba nhóm người dùng rõ ràng.
- Dữ liệu chính được đồng bộ qua backend và SQL Server.
- Có sơ đồ bàn theo khu A/B và trạng thái bàn.
- Có luồng gọi món, hóa đơn, thanh toán, giảm giá khách thân thiết.
- Áp dụng nhiều kiến thức môn học: UI, event, state, API, Intent, multimedia, location.
- Một số phần đã tách lớp xử lý mạng để code dễ bảo trì hơn.

## 8.2 Hạn chế
- Xác thực người dùng hiện còn đơn giản, một số thông tin tài khoản local vẫn dùng SharedPreferences thường.
- Chưa chuyển toàn bộ API call cũ sang cùng một Repository, một số màn vẫn còn gọi URL trực tiếp.
- Chưa có test tự động đầy đủ cho backend và Android UI.
- Chức năng in hóa đơn hiện phù hợp demo/xem trước, chưa tích hợp máy in thật.
- Khi chạy trên điện thoại thật cần cấu hình lại địa chỉ backend thay vì dùng `10.0.2.2`.

## 8.3 Hướng phát triển
- Chuyển toàn bộ networking sang một API client/repository thống nhất hoặc Retrofit.
- Đưa xác thực tài khoản lên backend, hash mật khẩu và dùng token an toàn.
- Bổ sung EncryptedSharedPreferences hoặc DataStore cho dữ liệu nhạy cảm cục bộ.
- Thêm Room/SQLite cache-first cho dữ liệu menu/bàn để app vẫn xem được khi mất mạng.
- Tích hợp máy in hóa đơn Bluetooth/Wi-Fi.
- Bổ sung thống kê doanh thu theo ngày/tháng và báo cáo món bán chạy.
- Tăng kiểm thử: unit test backend, test API, test UI các luồng đặt bàn/thanh toán.

# 9. Kết luận
Ứng dụng quản lý đặt bàn nhà hàng đã thể hiện được quá trình áp dụng kiến thức Lập trình thiết bị di động vào một bài toán thực tế. Project không chỉ dừng ở giao diện tĩnh mà có luồng nghiệp vụ hoàn chỉnh: đặt bàn, chọn bàn, gọi món, quản lý trạng thái, thanh toán, in hóa đơn và tra cứu khách hàng. Các nội dung từ Chương 1 đến Chương 6 đều được liên hệ vào sản phẩm: môi trường Android, điều hướng, giao diện, xử lý sự kiện, lưu trạng thái, xử lý nền, networking, intent, đa phương tiện, vị trí và bảo mật cơ bản.
Mặc dù vẫn còn hạn chế về xác thực, chuẩn hóa toàn bộ tầng mạng và kiểm thử tự động, đề tài đã đạt mục tiêu của một đồ án cuối môn: xây dựng được ứng dụng có tính thực tế, có phân quyền, có kết nối dữ liệu thật và có khả năng mở rộng trong tương lai.
# 10. Tài liệu tham khảo
- Buổi- 1 Chuong 1.pdf - Giới thiệu môi trường phát triển và nền tảng Android.
- Buổi 3 Chuong 1-2.pdf - Phân tích giao diện, layout, control và navigation.
- Chuong 3 - 1.pdf - Giao diện người dùng, XML layout, common controls.
- Chuong 3 - 2.pdf - Advanced controls, custom layout, WebView và Intent.
- Buoi 5 Chuong 4.pdf - Xử lý sự kiện, lưu trữ dữ liệu cục bộ.
- Chương 4.3 - 4.4.pdf - Lưu trạng thái, đa tiến trình, xử lý nền.
- Chuong 5 _API.pdf và Chuong 5 _API (1).pdf - Networking, multimedia, telephony, location.
- Chuong 6.pdf - Một số vấn đề thiết bị thật, quyền, bảo mật và kiểm soát truy cập.
- Source code project RestaurantBookingApp_Broken - ứng dụng đặt bàn nhà hàng Android + Node.js backend + SQL Server.

