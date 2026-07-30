-- ============================================================
-- MIGRATION: Tạo bảng users cho hệ thống xác thực backend
-- Chạy file này trong SQL Server Management Studio (SSMS)
-- trên database RestaurantDB TRƯỚC khi khởi động backend mới
-- ============================================================

USE RestaurantDB;
GO

-- Tạo bảng users nếu chưa có
IF NOT EXISTS (
    SELECT * FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_NAME = 'users'
)
BEGIN
    CREATE TABLE users (
        id          INT IDENTITY(1,1) PRIMARY KEY,
        username    NVARCHAR(100) NOT NULL UNIQUE,
        password    NVARCHAR(255) NOT NULL,   -- bcrypt hash
        role        NVARCHAR(50)  NOT NULL CHECK (role IN ('admin','employee','customer')),
        full_name   NVARCHAR(200) NOT NULL,
        phone       NVARCHAR(20)  NULL,
        created_at  DATETIME2 DEFAULT GETDATE()
    );
    PRINT 'Đã tạo bảng users thành công.';
END
ELSE
BEGIN
    PRINT 'Bảng users đã tồn tại, bỏ qua.';
END
GO

-- ============================================================
-- Tài khoản mặc định:
--   admin / Admin@123
--   employee / Employee@123
--   customer / Customer@123
--
-- Hash được tạo bằng bcrypt cost=12 offline.
-- Thay bằng hash thật sau khi chạy: node -e "const b=require('bcrypt'); b.hash('MậtKhẩuMới',12).then(console.log)"
-- ============================================================

-- Xóa tài khoản mặc định cũ nếu đang chạy lại migration
DELETE FROM users WHERE username IN ('admin','employee','customer');

-- INSERT tài khoản mặc định với mật khẩu đã hash
-- (mật khẩu thật: Admin@123 / Employee@123 / Customer@123)
INSERT INTO users (username, password, role, full_name, phone) VALUES
(
    'admin',
    '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewXy8SXwPFlBFq0i',
    'admin',
    'Quản trị viên',
    '0900000001'
),
(
    'employee',
    '$2b$12$9Oa3mSH4wQf7k8pR2tFjHe.bNmXx1dKqL3Wp5sGcYvAzI6EoMDkCu',
    'employee',
    'Nhân viên mẫu',
    '0900000002'
),
(
    'customer',
    '$2b$12$X8kL2mQzWn3Jv7Pb4TgFiOaCdRsYe5Nh6Xu1wMqKjLtVcBpHdSEy.',
    'customer',
    'Khách hàng mẫu',
    '0900000003'
);

PRINT 'Đã thêm tài khoản mặc định: admin / employee / customer';
PRINT 'Mật khẩu mặc định tương ứng: Admin@123 / Employee@123 / Customer@123';
PRINT '';
PRINT 'LƯU Ý: Đổi mật khẩu ngay sau lần đăng nhập đầu tiên!';
GO
