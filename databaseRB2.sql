
CREATE DATABASE RestaurantDB;
GO

USE RestaurantDB;
GO

--Tạo bảng chứa dữ liệu tổng quan nhà hàng (Lưu ý: Dùng NVARCHAR cho chuỗi tiếng Việt)
CREATE TABLE dashboard_stats (
    id INT IDENTITY(1,1) PRIMARY KEY,
    availableTables INT DEFAULT 14,
    occupiedTables INT DEFAULT 6,
    reservedTables INT DEFAULT 5,
    cleaningTables INT DEFAULT 2,
    guestsIn INT DEFAULT 28,
    guestsOut INT DEFAULT 22,
    revenue NVARCHAR(50) DEFAULT N'5,600,000 đ',
    alerts NVARCHAR(MAX)
);
GO
-- 1. XÓA CÁC BẢNG VÀ TRIGGER CŨ NẾU ĐANG TỒN TẠI ĐỂ TRÁNH XUNG ĐỘT
DROP TRIGGER IF EXISTS trg_AfterInsertBooking;
DROP TABLE IF EXISTS Bookings;
DROP TABLE IF EXISTS Tables;
GO

-- 2. TẠO LẠI BẢNG QUẢN LÝ DANH SÁCH BÀN ĂN (Chuẩn hóa không dấu, viết thường)
CREATE TABLE Tables (
    TableID INT IDENTITY(1,1) PRIMARY KEY,
    TableNumber VARCHAR(50) NOT NULL UNIQUE,       -- Lưu dạng: '1', '2' hoặc 'ban 01' (Không lo lỗi dấu)
    Capacity INT DEFAULT 4,                         
    CurrentStatus NVARCHAR(50) DEFAULT N'Trống'     -- Giữ nguyên trạng thái hiển thị tiếng Việt
);

-- 3. TẠO LẠI BẢNG QUẢN LÝ LỊCH ĐẶT BÀN
CREATE TABLE Bookings (
    BookingID INT IDENTITY(1,1) PRIMARY KEY,
    GuestName NVARCHAR(100) NOT NULL,               
    PhoneNumber VARCHAR(20) NOT NULL,                
    TableNumber VARCHAR(50) NOT NULL,              -- Đồng bộ kiểu dữ liệu với bảng Tables
    DepositAmount INT DEFAULT 0,                    
    Note NVARCHAR(500) NULL,                        
    CreatedAt DATETIME DEFAULT GETDATE(),           
    
    CONSTRAINT FK_Bookings_Tables FOREIGN KEY (TableNumber) 
    REFERENCES Tables(TableNumber) ON UPDATE CASCADE ON DELETE CASCADE
);
GO

-- 4. TỰ ĐỘNG ĐỔI TRẠNG THÁI SƠ ĐỒ BÀN (Trigger tự động đồng bộ)
CREATE TRIGGER trg_AfterInsertBooking
ON Bookings
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Tables
    SET CurrentStatus = N'Đã đặt'
    FROM Tables t
    INNER JOIN inserted i ON LOWER(TRIM(t.TableNumber)) = LOWER(TRIM(i.TableNumber));
END;
GO

-- 5. CHÈN DỮ LIỆU MẪU ĐƠN GIẢN (Từ bàn 1 đến bàn 12)
INSERT INTO Tables (TableNumber, CurrentStatus) VALUES 
('1', N'Trống'), ('2', N'Trống'), ('3', N'Trống'), ('4', N'Trống'),
('5', N'Trống'), ('6', N'Trống'), ('7', N'Trống'), ('8', N'Trống'),
('9', N'Trống'), ('10', N'Trống'), ('11', N'Trống'), ('12', N'Trống');
GO
-- Xóa bảng cũ đi để làm sạch cấu trúc
DROP TABLE IF EXISTS Bookings;
GO

-- Tạo lại bảng Bookings chuẩn 100% theo giao diện Android của bạn
CREATE TABLE Bookings (
    BookingID INT IDENTITY(1,1) PRIMARY KEY,
    BookingCode VARCHAR(20) NOT NULL UNIQUE,       -- Mã đặt bàn (Ví dụ: BK9102)
    GuestName NVARCHAR(100) NOT NULL,              -- Tên khách hàng
    PhoneNumber VARCHAR(20) NOT NULL,               -- Số điện thoại
    TableSummary NVARCHAR(250) NOT NULL,           -- Tóm tắt: Ví dụ 'Bàn 02' hoặc số '2'
    TotalAmount DECIMAL(18,2) DEFAULT 0,           -- Tiền cọc / Tổng tiền
    CurrentStatus VARCHAR(50) DEFAULT 'pending'     -- Trạng thái: pending, checked_in, checked_out
);
GO

-- Chèn sẵn dữ liệu mẫu thực tế để mở màn hình lên là thấy khách luôn, không bị trống!
INSERT INTO Bookings (BookingCode, GuestName, PhoneNumber, TableSummary, TotalAmount, CurrentStatus) VALUES
('BK9102', N'Nguyễn Văn A', '0901234567', N'Bàn 01 | 4 Người', 1500000, 'checked_in'),
('BK9103', N'Trần Thị B', '0918888888', N'Bàn 02 | 2 Người', 450000, 'pending'),
('BK9104', N'Lê Hoàng C', '0987654321', N'Bàn 05 | 12 Người', 5600000, 'checked_out');
GO
SELECT COLUMN_NAME, DATA_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'Tables'
SELECT TableID, TableNumber, CurrentStatus FROM Tables ORDER BY TableNumber
-- Đồng bộ toàn bộ về tiếng Anh
UPDATE Tables SET CurrentStatus = 'available' WHERE CurrentStatus = 'Trống'
UPDATE Tables SET CurrentStatus = 'occupied' WHERE CurrentStatus = 'Đang ăn'

-- Kiểm tra lại
SELECT TableID, TableNumber, CurrentStatus FROM Tables ORDER BY TableNumber

-- Tạo bảng Invoices lưu hóa đơn thanh toán
CREATE TABLE Invoices (
    InvoiceID INT IDENTITY(1,1) PRIMARY KEY,
    BookingID INT NOT NULL,
    BookingCode NVARCHAR(20),
    GuestName NVARCHAR(100),
    GuestPhone NVARCHAR(20),
    TableSummary NVARCHAR(200),
    TotalAmount DECIMAL(18,2),
    PaymentMethod NVARCHAR(20), -- 'cash' hoặc 'transfer'
    PaidAt DATETIME DEFAULT GETDATE(),
    Note NVARCHAR(500)
);

-- Thêm cột PaymentMethod và PaidAt vào Bookings để theo dõi
ALTER TABLE Bookings ADD PaymentMethod NVARCHAR(20) NULL;
ALTER TABLE Bookings ADD PaidAt DATETIME NULL;

-- Thêm cột ngày giờ đặt bàn vào Bookings
ALTER TABLE Bookings ADD BookingDate DATE NULL;
ALTER TABLE Bookings ADD BookingTime NVARCHAR(10) NULL;
ALTER TABLE Bookings ADD DepositPaid BIT DEFAULT 0; -- Khách đã thanh toán cọc chưa

ALTER TABLE Bookings
ADD CustomerUsername NVARCHAR(100) NULL;
select*from dbo.dashboard_stats
select*from dbo.Bookings
select*from dbo.Invoices
select*from dbo.Tables


DROP TABLE IF EXISTS MenuItems;
DROP TABLE IF EXISTS BookingOrderItems;
CREATE TABLE MenuItems(
    FoodID INT IDENTITY(1,1) PRIMARY KEY,
    FoodName NVARCHAR(100),
    Price DECIMAL(18,2),
    Category NVARCHAR(50),
    IsAvailable BIT DEFAULT 1
);

INSERT INTO MenuItems
VALUES
(N'Cơm gà',50000,N'Cơm',1),
(N'Bò lúc lắc',120000,N'Món chính',1),
(N'Pepsi',15000,N'Nước uống',1);


CREATE TABLE BookingOrderItems(
    OrderItemID INT IDENTITY(1,1) PRIMARY KEY,
    BookingID INT,
    FoodID INT,
    Quantity INT,
    UnitPrice DECIMAL(18,2),
    CreatedAt DATETIME DEFAULT GETDATE()
);


INSERT INTO MenuItems
(FoodName,Price,Category)

VALUES
(N'Cơm gà',50000,N'Cơm'),
(N'Bò lúc lắc',120000,N'Món chính'),
(N'Pepsi',15000,N'Nước uống'),
(N'Lẩu thái',350000,N'Lẩu');

SELECT COUNT(*) AS TotalTables
FROM Tables

SELECT * FROM Tables

-- THÊM A7 -> A20

INSERT INTO Tables (TableNumber, Capacity, CurrentStatus)
VALUES
('A7',4,N'Trống'),
('A8',4,N'Trống'),
('A9',4,N'Trống'),
('A10',4,N'Trống'),
('A11',4,N'Trống'),
('A12',4,N'Trống'),
('A13',4,N'Trống'),
('A14',4,N'Trống'),
('A15',4,N'Trống'),
('A16',4,N'Trống'),
('A17',4,N'Trống'),
('A18',4,N'Trống'),
('A19',4,N'Trống'),
('A20',4,N'Trống');

-- THÊM B7 -> B10

INSERT INTO Tables (TableNumber, Capacity, CurrentStatus)
VALUES
('B7',4,N'Trống'),
('B8',4,N'Trống'),
('B9',4,N'Trống'),
('B10',4,N'Trống');

SELECT * FROM MenuItems

SELECT * FROM MenuItems

DELETE FROM MenuItems;

INSERT INTO MenuItems
(FoodName, Category, Price, IsAvailable)
VALUES

(N'Bò bít tết Mỹ',      N'Món chính', 320000, 1),
(N'Sườn cừu nướng',     N'Món chính', 380000, 1),
(N'Cá hồi áp chảo',     N'Món chính', 290000, 1),
(N'Pizza Hải Sản',      N'Món chính', 250000, 1),
(N'Mì Ý Carbonara',     N'Món chính', 180000, 1),
(N'Burger Bò Phô Mai',  N'Món chính', 170000, 1),

(N'Salad Caesar',       N'Khai vị',   120000, 1),
(N'Súp Bí Đỏ',          N'Khai vị',    90000, 1),
(N'Khoai Tây Chiên',    N'Khai vị',    80000, 1),

(N'Tiramisu',           N'Tráng miệng', 90000, 1),
(N'Cheesecake',         N'Tráng miệng', 95000, 1),

(N'Coca Cola',          N'Nước uống',  25000, 1),
(N'Pepsi',              N'Nước uống',  25000, 1),
(N'Nước Cam',           N'Nước uống',  45000, 1),
(N'Rượu Vang Đỏ',       N'Nước uống', 450000, 1);

SELECT * FROM BookingOrderItems

SELECT * FROM Bookings

	SELECT TOP 1 * FROM BookingOrderItems

	SELECT TOP 5 * FROM Bookings

	SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME='Bookings'

ALTER TABLE Bookings
ADD CustomerUsername NVARCHAR(100) NULL;

ALTER TABLE Bookings
ADD isMerged INT DEFAULT 0;

ALTER TABLE Bookings
ADD TableType NVARCHAR(50) NULL;

ALTER TABLE Bookings
ADD BookingDate DATE NULL;

ALTER TABLE Bookings
ADD BookingTime NVARCHAR(20) NULL;

SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME='Bookings'

ALTER TABLE Bookings
ADD CustomerUsername NVARCHAR(100) NULL;

ALTER TABLE Bookings
ADD isMerged INT DEFAULT 0;

ALTER TABLE Bookings
ADD TableType NVARCHAR(50) NULL;

ALTER TABLE Bookings
ADD BookingDate DATE NULL;

ALTER TABLE Bookings
ADD BookingTime NVARCHAR(20) NULL;

SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME='Bookings'