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