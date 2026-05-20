-- 1. Tạo Database tên là RestaurantDB
CREATE DATABASE RestaurantDB;
GO

USE RestaurantDB;
GO

-- 2. Tạo bảng chứa dữ liệu tổng quan nhà hàng (Lưu ý: Dùng NVARCHAR cho chuỗi tiếng Việt)
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

-- 3. Chèn một dòng dữ liệu ban đầu để hệ thống chạy nền có sẵn dữ liệu gốc
INSERT INTO dashboard_stats (alerts) 
VALUES (N'3 bàn vừa trả khách cần nhân viên dọn dẹp.');
GO