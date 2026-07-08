const express = require('express');
const cors = require('cors');
const sql = require('mssql');
require('dotenv').config();

const app = express();

app.use(cors());
app.use(express.json());

app.get('/api/health', async (req, res) => {
    try {
        const db = await getPool();
        await db.request().query('SELECT 1 AS ok');
        res.json({
            success: true,
            api: 'online',
            database: 'online',
            uptimeSeconds: Math.floor(process.uptime())
        });
    } catch (error) {
        clearPool();
        res.status(503).json({
            success: false,
            api: 'online',
            database: 'reconnecting',
            message: error.message
        });
    }
});

const PORT = 3001;

const config = {
    user: process.env.DB_USER || 'sa',
    password: process.env.DB_PASSWORD,
    server: process.env.DB_SERVER || 'localhost',
    database: process.env.DB_NAME || 'RestaurantDB',
    options: {
        encrypt: process.env.DB_ENCRYPT === 'true',
        trustServerCertificate: process.env.DB_TRUST_CERT !== 'false',
        instanceName: process.env.DB_INSTANCE || undefined
    }
};

if (!config.password) {
    throw new Error('Thiếu biến môi trường DB_PASSWORD. Hãy tạo restaurant-backend/.env từ .env.example.');
}

let pool = null;
let poolPromise = null;

function clearPool() {
    const oldPool = pool;
    pool = null;
    poolPromise = null;
    if (oldPool) {
        oldPool.close().catch(() => {});
    }
}

async function getPool() {
    if (pool && pool.connected) {
        return pool;
    }

    if (!poolPromise) {
        const nextPool = new sql.ConnectionPool({
            ...config,
            connectionTimeout: 10000,
            requestTimeout: 15000,
            pool: {
                max: 10,
                min: 0,
                idleTimeoutMillis: 30000
            }
        });

        nextPool.on('error', error => {
            console.error('Kết nối SQL Server bị gián đoạn:', error.message);
            if (pool === nextPool) {
                clearPool();
            }
        });

        poolPromise = nextPool.connect()
            .then(connectedPool => {
                pool = connectedPool;
                poolPromise = null;
                console.log('Đã kết nối thành công tới Database SQL Server!');
                return connectedPool;
            })
            .catch(error => {
                poolPromise = null;
                nextPool.close().catch(() => {});
                throw error;
            });
    }

    return poolPromise;
}

async function waitForDatabase() {
    while (!pool || !pool.connected) {
        try {
            await getPool();
        } catch (error) {
            console.error('SQL Server chưa sẵn sàng, thử lại sau 5 giây:', error.message);
            await new Promise(resolve => setTimeout(resolve, 5000));
        }
    }
}

sql.on('error', error => {
    console.error('Lỗi SQL Server toàn cục:', error.message);
    clearPool();
});


function normalizeTableStatus(status) {
    const value = (status || '').toString().trim().toLowerCase();
    if (value === 'occupied' || value === 'đang dùng' || value === 'đang ăn') {
        return 'occupied';
    }
    if (value === 'booked' || value === 'đã đặt') {
        return 'booked';
    }
    return 'available';
}

function extractTableNumbers(summary) {
    return Array.from(
        new Set(((summary || '').toString().toUpperCase().match(/[AB]\d+/g) || []))
    );
}

async function setTablesForSummary(scope, summary, status) {
    const tableNumbers = extractTableNumbers(summary);
    for (const tableNumber of tableNumbers) {
        await paymentRequest(scope)
            .input('TableNumber', sql.VarChar, tableNumber)
            .input('Status', sql.VarChar, normalizeTableStatus(status))
            .query(`
                UPDATE Tables
                SET CurrentStatus = @Status
                WHERE UPPER(TableNumber) = @TableNumber
            `);
    }
}

async function reconcileBookingAndTableStatuses(pool) {
    await ensurePaymentSchema(pool);

    await pool.request().query(`
        UPDATE Tables
        SET CurrentStatus = CASE
            WHEN LOWER(LTRIM(RTRIM(CurrentStatus))) IN ('occupied', N'đang dùng', N'đang ăn')
                THEN 'occupied'
            WHEN LOWER(LTRIM(RTRIM(CurrentStatus))) IN ('booked', N'đã đặt')
                THEN 'booked'
            ELSE 'available'
        END;

        UPDATE b
        SET b.CurrentStatus = 'checked_out',
            b.DepositPaid = 1,
            b.PaymentMethod = COALESCE(i.PaymentMethod, b.PaymentMethod),
            b.PaidAt = COALESCE(i.PaidAt, b.PaidAt)
        FROM Bookings b
        INNER JOIN (
            SELECT BookingID, MAX(PaidAt) AS PaidAt, MAX(PaymentMethod) AS PaymentMethod
            FROM Invoices
            GROUP BY BookingID
        ) i ON i.BookingID = b.BookingID;

        UPDATE Tables SET CurrentStatus = 'available';
    `);

    const activeBookings = await pool.request().query(`
        SELECT BookingID, TableSummary, CurrentStatus
        FROM Bookings
        WHERE CurrentStatus IN ('pending', 'checked_in')
        ORDER BY CASE WHEN CurrentStatus = 'pending' THEN 1 ELSE 2 END,
                 BookingID ASC
    `);

    for (const booking of activeBookings.recordset) {
        const tableStatus = booking.CurrentStatus === 'checked_in'
            ? 'occupied'
            : 'booked';
        await setTablesForSummary(pool, booking.TableSummary, tableStatus);
    }
}

app.post('/api/admin/reconcile-statuses', async (req, res) => {
    try {
        const db = await getPool();
        await reconcileBookingAndTableStatuses(db);
        res.json({
            success: true,
            message: 'Đã đồng bộ trạng thái booking và bàn'
        });
    } catch (error) {
        console.error(error);
        res.status(500).json({
            success: false,
            message: 'Lỗi đồng bộ trạng thái: ' + error.message
        });
    }
});

// ======================================================
// LOOKUP CUSTOMER BY PHONE FOR MEMBER DISCOUNT
// ======================================================

app.get('/api/customers/lookup', async (req, res) => {
    try {
        const phone = (req.query.phone || '').toString().trim();
        const keyword = (req.query.q || phone).toString().trim();
        if (!keyword) {
            return res.status(400).json({ success: false, message: 'Thiếu tên hoặc số điện thoại' });
        }

        const pool = await getPool();
        const result = await pool.request()
            .input('Keyword', sql.NVarChar, keyword)
            .query(`
                SELECT
                    ISNULL(GuestName, '') AS GuestName,
                    ISNULL(GuestPhone, '') AS GuestPhone,
                    COUNT(*) AS VisitCount,
                    ISNULL(SUM(TotalAmount), 0) AS TotalSpent
                FROM Invoices
                WHERE GuestPhone = @Keyword
                   OR GuestName LIKE N'%' + @Keyword + N'%'
                GROUP BY GuestName, GuestPhone
                ORDER BY MAX(PaidAt) DESC
            `);

        const customers = result.recordset.map(row => {
            const visitCount = parseInt(row.VisitCount || 0);
            const totalSpent = parseFloat(row.TotalSpent || 0);
            let discountPercent = 0;
            if (totalSpent >= 5000000) discountPercent = 7;
            else if (totalSpent >= 2000000) discountPercent = 5;
            else if (visitCount > 0 || totalSpent > 0) discountPercent = 2;
            return {
                name: row.GuestName || '',
                phone: row.GuestPhone || '',
                visitCount,
                totalSpent,
                discountPercent
            };
        });

        const exactPhoneCustomer = customers.find(customer => customer.phone === phone);
        const primaryCustomer = exactPhoneCustomer || customers[0] || {
            visitCount: 0,
            totalSpent: 0,
            discountPercent: 0
        };

        res.json({
            success: true,
            found: customers.length > 0,
            visitCount: primaryCustomer.visitCount,
            totalSpent: primaryCustomer.totalSpent,
            discountPercent: primaryCustomer.discountPercent,
            customers
        });
    } catch (error) {
        console.error(error);
        res.status(500).json({
            success: false,
            message: 'Lỗi tra cứu khách hàng: ' + error.message
        });
    }
});
// ======================================================
// GET BOOKINGS
// ======================================================

app.get('/api/bookings', async (req, res) => {
    try {

        const pool = await getPool();

        const result = await pool.request()
            .query(`
                SELECT
                    b.*,
                    ISNULL(o.OrderTotal, 0) AS OrderTotal,
                    CASE WHEN EXISTS (
                        SELECT 1 FROM Invoices i WHERE i.BookingID = b.BookingID
                    ) THEN 1 ELSE 0 END AS HasInvoice
                FROM Bookings b
                OUTER APPLY (
                    SELECT SUM(Quantity * UnitPrice) AS OrderTotal
                    FROM BookingOrderItems
                    WHERE BookingID = b.BookingID
                ) o
                ORDER BY b.BookingID DESC
            `);

        const data = result.recordset.map(row => ({
            id: row.BookingID.toString(),
            bookingCode: row.BookingCode,
            customerUsername: row.CustomerUsername || '',
            guestName: row.GuestName,
            guestPhone: row.PhoneNumber,
            phoneNumber: row.PhoneNumber,
            tableSummary: row.TableSummary,
            tableNumber: row.TableSummary,
            totalAmount: parseFloat(row.OrderTotal || 0),
            depositAmount: 200000,
            orderTotal: parseFloat(row.OrderTotal || 0),
            status: row.CurrentStatus,
            bookingDate: row.BookingDate
                ? row.BookingDate.toISOString().split('T')[0]
                : '',
            bookingTime: row.BookingTime || '',
            depositPaid:
                row.DepositPaid === true ||
                row.DepositPaid === 1,
            hasInvoice: row.HasInvoice === true || row.HasInvoice === 1
        }));

        res.json({
            success: true,
            bookings: data
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            error: "Lỗi lấy danh sách"
        });
    }
});

// ======================================================
// CREATE BOOKING
// ======================================================

app.post('/api/bookings', async (req, res) => {

    try {

        const {
            customerUsername,
            guestName,
            phoneNumber,
            tableNumber,
            depositAmount,
            note,
            bookingDate,
            bookingTime
        } = req.body;

        if (
            !guestName ||
            !phoneNumber ||
            !tableNumber
        ) {
            return res.status(400).json({
                success: false,
                message: "Thiếu thông tin!"
            });
        }

        const pool = await getPool();

        const randomCode =
            "BK" +
            Math.floor(1000 + Math.random() * 9000);

        const tableSummaryText =
            tableNumber.trim();

        const isMergedValue =
            tableSummaryText.includes("Gộp")
                ? 1
                : 0;

        const tableTypeValue =
            tableSummaryText.includes("Gộp")
                ? "ghép"
                : "đơn";

        const insertResult = await pool.request()
            .input(
                'Code',
                sql.VarChar,
                randomCode
            )
            .input(
                'CustomerUsername',
                sql.NVarChar,
                customerUsername || ''
            )
            .input(
                'Name',
                sql.NVarChar,
                guestName
            )
            .input(
                'Phone',
                sql.VarChar,
                phoneNumber
            )
            .input(
                'Summary',
                sql.NVarChar,
                tableSummaryText
            )
            .input(
                'Amount',
                sql.Decimal(18, 2),
                200000
            )
            .input(
                'IsMerged',
                sql.Int,
                isMergedValue
            )
            .input(
                'TableType',
                sql.NVarChar,
                tableTypeValue
            )
            .input(
                'BookingDate',
                sql.Date,
                bookingDate
                    ? new Date(bookingDate)
                    : null
            )
            .input(
                'BookingTime',
                sql.NVarChar,
                bookingTime || ''
            )
            .query(`
                INSERT INTO Bookings
                (
                    BookingCode,
                    CustomerUsername,
                    GuestName,
                    PhoneNumber,
                    TableSummary,
                    TotalAmount,
                    CurrentStatus,
                    isMerged,
                    TableType,
                    BookingDate,
                    BookingTime
                )
                OUTPUT INSERTED.BookingID, INSERTED.BookingCode
                VALUES
                (
                    @Code,
                    @CustomerUsername,
                    @Name,
                    @Phone,
                    @Summary,
                    @Amount,
                    'pending',
                    @IsMerged,
                    @TableType,
                    @BookingDate,
                    @BookingTime
                )
            `);

        const insertedBooking = insertResult.recordset[0];

        res.status(201).json({
            success: true,
            message: "Đặt bàn thành công!",
            bookingId: insertedBooking.BookingID,
            bookingCode: insertedBooking.BookingCode
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi: " + error.message
        });
    }
});

// ======================================================
// UPDATE BOOKING STATUS
// ======================================================

app.put('/api/bookings/status', async (req, res) => {
    try {
        const { id, status } = req.body;
        const pool = await getPool();

        const bookingResult = await pool.request()
            .input('ID', sql.Int, parseInt(id))
            .query(`SELECT * FROM Bookings WHERE BookingID = @ID`);

        const booking = bookingResult.recordset[0];

        if (!booking) {
            return res.status(404).json({
                success: false,
                message: "Không tìm thấy booking!"
            });
        }

        let summary = booking.TableSummary || "";
        let finalSummary = summary;

        // Lấy mã bàn có sẵn trong summary, ví dụ A01, A02, B03
        let tableNumbers = summary.match(/[AB]\d+/g) || [];

        // Nếu bấm Xếp bàn mà summary chưa có mã bàn cụ thể
        // Ví dụ: "Gộp 2 bàn Khu B" hoặc "1 Bàn đơn Khu A"
        if (status === "checked_in" && tableNumbers.length === 0) {
            const zoneMatch = summary.match(/Khu\s*([AB])/i);
            const zone = zoneMatch ? zoneMatch[1].toUpperCase() : "A";

            let tableCount = 1;

            const mergedMatch = summary.match(/Gộp\s*(\d+)/i);
            const singleMatch = summary.match(/(\d+)\s*Bàn/i);

            if (mergedMatch) {
                tableCount = parseInt(mergedMatch[1]);
            } else if (singleMatch) {
                tableCount = parseInt(singleMatch[1]);
            }

            const availableTablesResult = await pool.request()
                .input('ZoneLike', sql.VarChar, zone + '%')
                .input('TakeCount', sql.Int, tableCount)
                .query(`
                    SELECT TOP (@TakeCount) *
                    FROM Tables
                    WHERE TableNumber LIKE @ZoneLike
                      AND CurrentStatus = 'available'
                    ORDER BY
                        LEFT(UPPER(TableNumber), 1),
                        TRY_CONVERT(INT, SUBSTRING(TableNumber, 2, 20)),
                        TableNumber
                `);

            const availableTables = availableTablesResult.recordset;

            if (availableTables.length < tableCount) {
                return res.status(400).json({
                    success: false,
                    message: `Không đủ bàn trống ở Khu ${zone}!`
                });
            }

            tableNumbers = availableTables.map(t => t.TableNumber);

            if (tableCount > 1) {
                finalSummary = `Gộp ${tableCount} bàn ${tableNumbers.join("+")} Khu ${zone}`;
            } else {
                finalSummary = `1 Bàn đơn ${tableNumbers[0]} Khu ${zone}`;
            }
        }

        await pool.request()
            .input('Status', sql.VarChar, status)
            .input('Summary', sql.NVarChar, finalSummary)
            .input('ID', sql.Int, parseInt(id))
            .query(`
                UPDATE Bookings
                SET CurrentStatus = @Status,
                    TableSummary = @Summary
                WHERE BookingID = @ID
            `);

        let tableStatus = null;

        if (status === "checked_in") {
            tableStatus = "occupied";
        } else if (status === "checked_out" || status === "cancelled") {
            tableStatus = "available";
        } else if (status === "pending") {
            tableStatus = "booked";
        }

        if (tableStatus && tableNumbers.length > 0) {
            for (const tableNumber of tableNumbers) {
                await pool.request()
                    .input('TableNumber', sql.VarChar, tableNumber)
                    .input('Status', sql.VarChar, tableStatus)
                    .query(`
                        UPDATE Tables
                        SET CurrentStatus = @Status
                        WHERE TableNumber = @TableNumber
                    `);
            }
        }

        res.json({
            success: true,
            message: "Cập nhật thành công!",
            tableSummary: finalSummary
        });

    } catch (error) {
        console.error(error);
        res.status(500).json({
            success: false,
            message: "Lỗi: " + error.message
        });
    }
});

// ======================================================
// CONFIRM DEPOSIT
// ======================================================

app.put('/api/bookings/deposit', async (req, res) => {

    try {

        const { id } = req.body;

        const pool = await getPool();

        await pool.request()
            .input(
                'ID',
                sql.Int,
                parseInt(id)
            )
            .query(`
                UPDATE Bookings
                SET DepositPaid = 1
                WHERE BookingID = @ID
            `);

        res.json({
            success: true,
            message: "Đã xác nhận thanh toán cọc!"
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi: " + error.message
        });
    }
});

// ======================================================
// UPDATE BOOKING
// ======================================================

app.put('/api/bookings/:id', async (req, res) => {

    try {

        const { id } = req.params;

        const {
            guestName,
            phoneNumber,
            tableNumber,
            depositAmount,
            status
        } = req.body;

        const pool = await getPool();

        const tableSummaryText =
            tableNumber
                ? tableNumber.trim()
                : '';

        const isMergedValue =
            tableSummaryText.includes("Gộp")
                ? 1
                : 0;

        const tableTypeValue =
            tableSummaryText.includes("Gộp")
                ? "ghép"
                : "đơn";

        await pool.request()
            .input(
                'ID',
                sql.Int,
                parseInt(id)
            )
            .input(
                'Name',
                sql.NVarChar,
                guestName
            )
            .input(
                'Phone',
                sql.VarChar,
                phoneNumber
            )
            .input(
                'Summary',
                sql.NVarChar,
                tableSummaryText
            )
            .input(
                'Amount',
                sql.Decimal(18, 2),
                200000
            )
            .input(
                'Status',
                sql.VarChar,
                status
            )
            .input(
                'IsMerged',
                sql.Int,
                isMergedValue
            )
            .input(
                'TableType',
                sql.NVarChar,
                tableTypeValue
            )
            .query(`
                UPDATE Bookings
                SET
                    GuestName=@Name,
                    PhoneNumber=@Phone,
                    TableSummary=@Summary,
                    TotalAmount=@Amount,
                    CurrentStatus=@Status,
                    isMerged=@IsMerged,
                    TableType=@TableType
                WHERE BookingID=@ID
            `);

        res.json({
            success: true,
            message: "Cập nhật thành công!"
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi: " + error.message
        });
    }
});

// ======================================================
// DELETE CANCELLED BOOKINGS
// ======================================================

app.delete('/api/bookings/cancelled', async (req, res) => {

    try {

        const pool = await getPool();

        await pool.request()
            .query(`
                DELETE FROM Bookings
                WHERE CurrentStatus = 'cancelled'
            `);

        res.json({
            success: true,
            message: "Đã xóa đơn hủy!"
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi: " + error.message
        });
    }
});

// ======================================================
// DELETE BOOKING
// ======================================================

app.delete('/api/bookings/:id', async (req, res) => {

    try {

        const { id } = req.params;

        const pool = await getPool();

        await pool.request()
            .input(
                'ID',
                sql.Int,
                parseInt(id)
            )
            .query(`
                DELETE FROM Bookings
                WHERE BookingID = @ID
            `);

        res.json({
            success: true,
            message: "Đã xóa!"
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi: " + error.message
        });
    }
});

// ======================================================
// GET TABLES
// ======================================================

app.get('/api/tables', async (req, res) => {

    try {

        const pool = await getPool();

        const result = await pool.request()
            .query(`
                SELECT *
                FROM Tables
                ORDER BY
                    LEFT(UPPER(TableNumber), 1),
                    TRY_CONVERT(INT, SUBSTRING(TableNumber, 2, 20)),
                    TableNumber
            `);

        const data = result.recordset.map(row => ({
            id: row.TableID.toString(),
            tableNumber: row.TableNumber,
            capacity: row.Capacity,
            status: normalizeTableStatus(row.CurrentStatus),
            zone:
                row.TableNumber.startsWith('A')
                    ? 'A'
                    : 'B'
        }));

        res.json({
            success: true,
            tables: data
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi lấy bàn"
        });
    }
});

// ======================================================
// UPDATE TABLE STATUS
// ======================================================

app.put('/api/tables/status', async (req, res) => {

    try {

        const {
            id,
            status
        } = req.body;

        const pool = await getPool();

        await pool.request()
            .input(
                'Status',
                sql.NVarChar,
                status
            )
            .input(
                'ID',
                sql.Int,
                parseInt(id)
            )
            .query(`
                UPDATE Tables
                SET CurrentStatus = @Status
                WHERE TableID = @ID
            `);

        res.json({
            success: true,
            message: "Cập nhật bàn thành công!"
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi: " + error.message
        });
    }
});


// ======================================================
// GET MENU
// ======================================================

app.get('/api/menu', async (req, res) => {

    try {

        const pool = await getPool();

        const result = await pool.request()
            .query(`
                SELECT
                    FoodID,
                    FoodName,
                    Price,
                    Category,
                    IsAvailable
                FROM MenuItems
                WHERE ISNULL(IsAvailable, 1) = 1
                ORDER BY Category, FoodName
            `);

        const items = result.recordset.map(row => ({
            foodId: row.FoodID,
            foodName: row.FoodName,
            price: parseFloat(row.Price || 0),
            category: row.Category || '',
            isAvailable: row.IsAvailable === true || row.IsAvailable === 1
        }));

        res.json({
            success: true,
            items
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi lấy menu: " + error.message
        });
    }
});

// ======================================================
// IMPORT REFERENCE DATA FROM AN APP BACKUP
// Booking and invoice history is intentionally not overwritten.
// ======================================================

app.post('/api/admin/import-reference-data', async (req, res) => {
    const tables = Array.isArray(req.body.tables) ? req.body.tables : [];
    const menuItems = Array.isArray(req.body.menuItems) ? req.body.menuItems : [];

    if (tables.length === 0 && menuItems.length === 0) {
        return res.status(400).json({
            success: false,
            message: 'Bản sao không có dữ liệu bàn hoặc thực đơn'
        });
    }

    const pool = await getPool();
    const transaction = new sql.Transaction(pool);

    try {
        await transaction.begin();

        for (const table of tables) {
            const tableNumber = String(table.tableNumber || '').trim();
            if (!tableNumber) continue;

            await new sql.Request(transaction)
                .input('TableNumber', sql.VarChar(50), tableNumber)
                .input('Capacity', sql.Int, parseInt(table.capacity, 10) || 4)
                .input(
                    'CurrentStatus',
                    sql.NVarChar(50),
                    normalizeTableStatus(table.status || 'available')
                )
                .query(`
                    IF EXISTS (SELECT 1 FROM Tables WHERE TableNumber = @TableNumber)
                        UPDATE Tables
                        SET Capacity = @Capacity,
                            CurrentStatus = @CurrentStatus
                        WHERE TableNumber = @TableNumber;
                    ELSE
                        INSERT INTO Tables (TableNumber, Capacity, CurrentStatus)
                        VALUES (@TableNumber, @Capacity, @CurrentStatus);
                `);
        }

        for (const item of menuItems) {
            const foodName = String(item.foodName || '').trim();
            if (!foodName) continue;

            await new sql.Request(transaction)
                .input('FoodName', sql.NVarChar(100), foodName)
                .input('Price', sql.Decimal(18, 2), Number(item.price) || 0)
                .input('Category', sql.NVarChar(50), String(item.category || 'Khác'))
                .input('IsAvailable', sql.Bit, item.isAvailable !== false)
                .query(`
                    IF EXISTS (SELECT 1 FROM MenuItems WHERE FoodName = @FoodName)
                        UPDATE MenuItems
                        SET Price = @Price,
                            Category = @Category,
                            IsAvailable = @IsAvailable
                        WHERE FoodName = @FoodName;
                    ELSE
                        INSERT INTO MenuItems (FoodName, Price, Category, IsAvailable)
                        VALUES (@FoodName, @Price, @Category, @IsAvailable);
                `);
        }

        await transaction.commit();
        res.json({
            success: true,
            message: 'Khôi phục dữ liệu tham chiếu thành công',
            tableCount: tables.length,
            menuItemCount: menuItems.length
        });
    } catch (error) {
        if (transaction._aborted !== true) {
            await transaction.rollback().catch(() => {});
        }
        console.error('Import reference data error:', error);
        res.status(500).json({
            success: false,
            message: 'Lỗi khôi phục dữ liệu: ' + error.message
        });
    }
});

// ======================================================
// ADD FOOD TO BOOKING
// ======================================================

app.post('/api/order-items/add', async (req, res) => {

    try {

        const {
            bookingId,
            foodId,
            quantity,
            unitPrice
        } = req.body;

        if (!bookingId || !foodId || !quantity || quantity <= 0) {
            return res.status(400).json({
                success: false,
                message: "Thiếu thông tin món ăn"
            });
        }

        const pool = await getPool();

        const insertResult = await pool.request()
            .input('BookingID', sql.Int, parseInt(bookingId))
            .input('FoodID', sql.Int, parseInt(foodId))
            .input('Quantity', sql.Int, parseInt(quantity))
            .input('UnitPrice', sql.Decimal(18, 2), parseFloat(unitPrice || 0))
            .query(`
                INSERT INTO BookingOrderItems
                (
                    BookingID,
                    FoodID,
                    Quantity,
                    UnitPrice
                )
                VALUES
                (
                    @BookingID,
                    @FoodID,
                    @Quantity,
                    @UnitPrice
                )
            `);

        res.status(201).json({
            success: true,
            message: "Đã thêm món"
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi thêm món: " + error.message
        });
    }
});

app.delete('/api/bookings/:id/order-items', async (req, res) => {
    try {
        const pool = await getPool();
        await pool.request()
            .input('BookingID', sql.Int, parseInt(req.params.id))
            .query(`
                DELETE FROM BookingOrderItems
                WHERE BookingID = @BookingID
            `);

        res.json({
            success: true,
            message: "Đã xóa món đã gọi"
        });
    } catch (error) {
        console.error(error);
        res.status(500).json({
            success: false,
            message: "Lỗi xóa món đã gọi: " + error.message
        });
    }
});

app.get('/api/bookings/:id/order-items', async (req, res) => {
    try {
        const pool = await getPool();
        const result = await pool.request()
            .input('BookingID', sql.Int, parseInt(req.params.id))
            .query(`
                SELECT
                    oi.OrderItemID,
                    oi.BookingID,
                    oi.FoodID,
                    m.FoodName,
                    m.Category,
                    oi.Quantity,
                    oi.UnitPrice,
                    (oi.Quantity * oi.UnitPrice) AS LineTotal
                FROM BookingOrderItems oi
                LEFT JOIN MenuItems m ON m.FoodID = oi.FoodID
                WHERE oi.BookingID = @BookingID
                ORDER BY oi.OrderItemID ASC
            `);

        const items = result.recordset.map(row => ({
            id: row.OrderItemID.toString(),
            bookingId: row.BookingID.toString(),
            foodId: row.FoodID,
            foodName: row.FoodName || '',
            category: row.Category || '',
            quantity: row.Quantity,
            unitPrice: parseFloat(row.UnitPrice || 0),
            lineTotal: parseFloat(row.LineTotal || 0)
        }));

        res.json({
            success: true,
            items,
            total: items.reduce((sum, item) => sum + item.lineTotal, 0)
        });
    } catch (error) {
        console.error(error);
        res.status(500).json({
            success: false,
            message: "Lỗi lấy món đã gọi: " + error.message
        });
    }
});
// ======================================================
// GET INVOICES
// ======================================================

async function ensurePaymentSchema(pool) {
    await pool.request().query(`
        IF COL_LENGTH('Bookings', 'PaymentMethod') IS NULL
            ALTER TABLE Bookings ADD PaymentMethod NVARCHAR(20) NULL;
        IF COL_LENGTH('Bookings', 'PaidAt') IS NULL
            ALTER TABLE Bookings ADD PaidAt DATETIME NULL;
        IF COL_LENGTH('Bookings', 'DepositPaid') IS NULL
            ALTER TABLE Bookings ADD DepositPaid BIT NOT NULL
                CONSTRAINT DF_Bookings_DepositPaid DEFAULT 0;

        IF COL_LENGTH('Invoices', 'FoodSubtotal') IS NULL
            ALTER TABLE Invoices ADD FoodSubtotal DECIMAL(18,2) NULL;
        IF COL_LENGTH('Invoices', 'DiscountPercent') IS NULL
            ALTER TABLE Invoices ADD DiscountPercent DECIMAL(5,2) NULL;
        IF COL_LENGTH('Invoices', 'DiscountAmount') IS NULL
            ALTER TABLE Invoices ADD DiscountAmount DECIMAL(18,2) NULL;
        IF COL_LENGTH('Invoices', 'DepositAmount') IS NULL
            ALTER TABLE Invoices ADD DepositAmount DECIMAL(18,2) NULL;
    `);
}

function paymentRequest(scope) {
    return scope instanceof sql.Transaction
        ? new sql.Request(scope)
        : scope.request();
}

async function calculatePaymentSummary(scope, bookingId) {
    const parsedBookingId = parseInt(bookingId);
    if (!Number.isInteger(parsedBookingId) || parsedBookingId <= 0) {
        const error = new Error('BookingID không hợp lệ');
        error.statusCode = 400;
        throw error;
    }

    const bookingResult = await paymentRequest(scope)
        .input('BookingID', sql.Int, parsedBookingId)
        .query(`
            SELECT BookingID, BookingCode, GuestName, PhoneNumber, TableSummary,
                   CurrentStatus
            FROM Bookings
            WHERE BookingID = @BookingID
        `);

    if (bookingResult.recordset.length === 0) {
        const error = new Error('Không tìm thấy đơn đặt bàn');
        error.statusCode = 404;
        throw error;
    }

    const booking = bookingResult.recordset[0];
    const orderResult = await paymentRequest(scope)
        .input('BookingID', sql.Int, parsedBookingId)
        .query(`
            SELECT
                oi.FoodID AS foodId,
                ISNULL(m.FoodName, N'Món ăn') AS foodName,
                oi.Quantity AS quantity,
                oi.UnitPrice AS unitPrice,
                (oi.Quantity * oi.UnitPrice) AS lineTotal
            FROM BookingOrderItems oi
            LEFT JOIN MenuItems m ON m.FoodID = oi.FoodID
            WHERE oi.BookingID = @BookingID
            ORDER BY oi.OrderItemID ASC
        `);

    const items = orderResult.recordset.map(item => ({
        foodId: parseInt(item.foodId),
        foodName: item.foodName,
        quantity: parseInt(item.quantity || 0),
        unitPrice: parseFloat(item.unitPrice || 0),
        lineTotal: parseFloat(item.lineTotal || 0)
    }));
    const foodSubtotal = items.reduce((sum, item) => sum + item.lineTotal, 0);

    const loyaltyResult = await paymentRequest(scope)
        .input('Phone', sql.NVarChar, booking.PhoneNumber || '')
        .input('BookingID', sql.Int, parsedBookingId)
        .query(`
            SELECT
                COUNT(*) AS VisitCount,
                ISNULL(SUM(TotalAmount), 0) AS TotalSpent
            FROM Invoices
            WHERE GuestPhone = @Phone
              AND BookingID <> @BookingID
        `);

    const loyalty = loyaltyResult.recordset[0] || {};
    const visitCount = parseInt(loyalty.VisitCount || 0);
    const previousSpent = parseFloat(loyalty.TotalSpent || 0);
    let discountPercent = 0;
    if (previousSpent >= 5000000) {
        discountPercent = 7;
    } else if (previousSpent >= 2000000) {
        discountPercent = 5;
    } else if (visitCount > 0 || previousSpent > 0) {
        discountPercent = 2;
    }

    const discountAmount = Math.round(foodSubtotal * discountPercent / 100);
    const amountAfterDiscount = Math.max(0, foodSubtotal - discountAmount);
    const depositAmount = 200000;
    const amountDue = Math.max(0, amountAfterDiscount - depositAmount);

    return {
        bookingId: parsedBookingId,
        bookingCode: booking.BookingCode,
        guestName: booking.GuestName,
        guestPhone: booking.PhoneNumber || '',
        tableSummary: booking.TableSummary,
        status: booking.CurrentStatus,
        items,
        foodSubtotal,
        visitCount,
        previousSpent,
        discountPercent,
        discountAmount,
        amountAfterDiscount,
        depositAmount,
        amountDue
    };
}

app.get('/api/bookings/:id/payment-summary', async (req, res) => {
    try {
        const pool = await getPool();
        await ensurePaymentSchema(pool);
        const summary = await calculatePaymentSummary(pool, req.params.id);
        res.json({ success: true, summary });
    } catch (error) {
        console.error(error);
        res.status(error.statusCode || 500).json({
            success: false,
            message: 'Lỗi tính thanh toán: ' + error.message
        });
    }
});

app.get('/api/invoices', async (req, res) => {
    try {
        const pool = await getPool();
        await ensurePaymentSchema(pool);
        const result = await pool.request().query(`
            SELECT *
            FROM Invoices
            ORDER BY PaidAt DESC
        `);

        const data = result.recordset.map(row => ({
            id: row.InvoiceID.toString(),
            bookingId: row.BookingID.toString(),
            bookingCode: row.BookingCode,
            guestName: row.GuestName,
            guestPhone: row.GuestPhone,
            tableSummary: row.TableSummary,
            foodSubtotal: parseFloat(row.FoodSubtotal || 0),
            discountPercent: parseFloat(row.DiscountPercent || 0),
            discountAmount: parseFloat(row.DiscountAmount || 0),
            depositAmount: parseFloat(row.DepositAmount || 0),
            totalAmount: parseFloat(row.TotalAmount || 0),
            paymentMethod: row.PaymentMethod,
            paidAt: row.PaidAt,
            note: row.Note || ''
        }));
        res.json({ success: true, invoices: data });
    } catch (error) {
        console.error(error);
        res.status(500).json({
            success: false,
            message: 'Lỗi hóa đơn: ' + error.message
        });
    }
});

app.get('/api/invoices/:id', async (req, res) => {
    try {
        const invoiceId = parseInt(req.params.id, 10);
        if (!Number.isInteger(invoiceId) || invoiceId <= 0) {
            return res.status(400).json({
                success: false,
                message: 'Mã hóa đơn không hợp lệ'
            });
        }

        const pool = await getPool();
        await ensurePaymentSchema(pool);
        const result = await pool.request()
            .input('InvoiceID', sql.Int, invoiceId)
            .query(`
                SELECT TOP 1 *
                FROM Invoices
                WHERE InvoiceID = @InvoiceID
            `);

        if (result.recordset.length === 0) {
            return res.status(404).json({
                success: false,
                message: 'Không tìm thấy hóa đơn'
            });
        }

        const stored = result.recordset[0];
        const summary = await calculatePaymentSummary(pool, stored.BookingID);

        res.json({
            success: true,
            invoice: {
                ...summary,
                foodSubtotal: parseFloat(stored.FoodSubtotal || 0),
                discountPercent: parseFloat(stored.DiscountPercent || 0),
                discountAmount: parseFloat(stored.DiscountAmount || 0),
                depositAmount: parseFloat(stored.DepositAmount || 0),
                amountDue: parseFloat(stored.TotalAmount || 0),
                invoiceId: stored.InvoiceID.toString(),
                paidAt: stored.PaidAt,
                paymentMethod: stored.PaymentMethod || 'cash',
                note: stored.Note || ''
            }
        });
    } catch (error) {
        console.error('Invoice detail error:', error);
        res.status(error.statusCode || 500).json({
            success: false,
            message: 'Lỗi tải chi tiết hóa đơn: ' + error.message
        });
    }
});

// ======================================================
// CREATE INVOICE
// ======================================================

app.post('/api/invoices', async (req, res) => {
    const pool = await getPool();
    await ensurePaymentSchema(pool);
    const transaction = new sql.Transaction(pool);

    try {
        const bookingId = parseInt(req.body.bookingId);
        const paymentMethod = req.body.paymentMethod === 'transfer'
            ? 'transfer'
            : 'cash';
        const note = (req.body.note || '').toString().trim();

        await transaction.begin(sql.ISOLATION_LEVEL.SERIALIZABLE);

        const duplicateResult = await paymentRequest(transaction)
            .input('BookingID', sql.Int, bookingId)
            .query(`
                SELECT TOP 1 InvoiceID
                FROM Invoices WITH (UPDLOCK, HOLDLOCK)
                WHERE BookingID = @BookingID
            `);

        if (duplicateResult.recordset.length > 0) {
            const existingResult = await paymentRequest(transaction)
                .input('BookingID', sql.Int, bookingId)
                .query(`
                    SELECT TOP 1 *
                    FROM Invoices
                    WHERE BookingID = @BookingID
                    ORDER BY PaidAt DESC, InvoiceID DESC
                `);
            const existing = existingResult.recordset[0];
            const summary = await calculatePaymentSummary(transaction, bookingId);

            await paymentRequest(transaction)
                .input('ID', sql.Int, bookingId)
                .input('PaymentMethod', sql.NVarChar, existing.PaymentMethod || paymentMethod)
                .input('PaidAt', sql.DateTime, existing.PaidAt || new Date())
                .query(`
                    UPDATE Bookings
                    SET CurrentStatus = 'checked_out',
                        PaymentMethod = @PaymentMethod,
                        PaidAt = @PaidAt,
                        DepositPaid = 1
                    WHERE BookingID = @ID
                `);
            await setTablesForSummary(transaction, summary.tableSummary, 'available');
            await transaction.commit();

            return res.status(200).json({
                success: true,
                alreadyPaid: true,
                message: 'Đơn đã thanh toán, trạng thái đã được đồng bộ',
                invoice: {
                    ...summary,
                    foodSubtotal: parseFloat(existing.FoodSubtotal || summary.foodSubtotal),
                    discountPercent: parseFloat(existing.DiscountPercent || 0),
                    discountAmount: parseFloat(existing.DiscountAmount || 0),
                    depositAmount: parseFloat(existing.DepositAmount || 200000),
                    amountDue: parseFloat(existing.TotalAmount || 0),
                    invoiceId: existing.InvoiceID.toString(),
                    paidAt: existing.PaidAt,
                    paymentMethod: existing.PaymentMethod || paymentMethod,
                    note: existing.Note || ''
                }
            });
        }

        const summary = await calculatePaymentSummary(transaction, bookingId);
        const orderNote = summary.items.length > 0
            ? 'Món đã gọi: ' + summary.items
                .map(item => item.foodName + ' x' + item.quantity)
                .join(', ')
            : 'Chưa gọi món';
        const discountNote = summary.discountPercent > 0
            ? 'Giảm giá hội viên ' + summary.discountPercent +
              '%: -' + summary.discountAmount + ' VND'
            : '';
        const depositNote = 'Đã trừ tiền cọc: -' + summary.depositAmount + ' VND';
        const invoiceNote = [note, orderNote, discountNote, depositNote]
            .filter(Boolean)
            .join(' | ');

        const insertResult = await paymentRequest(transaction)
            .input('BookingID', sql.Int, bookingId)
            .input('BookingCode', sql.NVarChar, summary.bookingCode)
            .input('GuestName', sql.NVarChar, summary.guestName)
            .input('GuestPhone', sql.NVarChar, summary.guestPhone)
            .input('TableSummary', sql.NVarChar, summary.tableSummary)
            .input('FoodSubtotal', sql.Decimal(18, 2), summary.foodSubtotal)
            .input('DiscountPercent', sql.Decimal(5, 2), summary.discountPercent)
            .input('DiscountAmount', sql.Decimal(18, 2), summary.discountAmount)
            .input('DepositAmount', sql.Decimal(18, 2), summary.depositAmount)
            .input('TotalAmount', sql.Decimal(18, 2), summary.amountDue)
            .input('PaymentMethod', sql.NVarChar, paymentMethod)
            .input('Note', sql.NVarChar, invoiceNote)
            .query(`
                INSERT INTO Invoices
                (
                    BookingID, BookingCode, GuestName, GuestPhone,
                    TableSummary, FoodSubtotal, DiscountPercent,
                    DiscountAmount, DepositAmount, TotalAmount,
                    PaymentMethod, Note, PaidAt
                )
                OUTPUT INSERTED.InvoiceID, INSERTED.PaidAt
                VALUES
                (
                    @BookingID, @BookingCode, @GuestName, @GuestPhone,
                    @TableSummary, @FoodSubtotal, @DiscountPercent,
                    @DiscountAmount, @DepositAmount, @TotalAmount,
                    @PaymentMethod, @Note, GETDATE()
                )
            `);

        await paymentRequest(transaction)
            .input('ID', sql.Int, bookingId)
            .input('PaymentMethod', sql.NVarChar, paymentMethod)
            .query(`
                UPDATE Bookings
                SET CurrentStatus = 'checked_out',
                    PaymentMethod = @PaymentMethod,
                    PaidAt = GETDATE(),
                    DepositPaid = 1
                WHERE BookingID = @ID
            `);

        await setTablesForSummary(transaction, summary.tableSummary, 'available');
        await transaction.commit();

        res.status(201).json({
            success: true,
            message: 'Thanh toán thành công!',
            invoice: {
                ...summary,
                invoiceId: insertResult.recordset[0].InvoiceID.toString(),
                paidAt: insertResult.recordset[0].PaidAt,
                paymentMethod,
                note: invoiceNote
            }
        });
    } catch (error) {
        if (transaction._aborted !== true) {
            try {
                await transaction.rollback();
            } catch (_) {
                // Transaction may already be closed.
            }
        }
        console.error(error);
        res.status(error.statusCode || 500).json({
            success: false,
            message: 'Lỗi thanh toán: ' + error.message
        });
    }
});

// ======================================================
// // DASHBOARD STATS
// ======================================================

app.get('/api/dashboard/stats', async (req, res) => {

    try {

        const pool = await getPool();

        const b = await pool.request().query(`
            SELECT
                COUNT(CASE WHEN CurrentStatus='pending' THEN 1 END) as pendingCount,
                COUNT(CASE WHEN CurrentStatus='checked_in' THEN 1 END) as checkedInCount,
                COUNT(CASE WHEN CurrentStatus='checked_out' THEN 1 END) as checkedOutCount,
                ISNULL(
                    SUM(
                        CASE
                            WHEN CurrentStatus='checked_out'
                            THEN TotalAmount
                            ELSE 0
                        END
                    ),
                    0
                ) as revenue
            FROM Bookings
        `);

        const t = await pool.request().query(`
            SELECT
                COUNT(CASE WHEN CurrentStatus='available' THEN 1 END) as availableCount,
                COUNT(CASE WHEN CurrentStatus='occupied' THEN 1 END) as occupiedCount,
                COUNT(CASE WHEN CurrentStatus='booked' THEN 1 END) as bookedCount,
                COUNT(*) as totalTables
            FROM Tables
        `);

        const bRow = b.recordset[0];
        const tRow = t.recordset[0];

        res.json({
            success: true,
            bookings: {
                pending: bRow.pendingCount || 0,
                checkedIn: bRow.checkedInCount || 0,
                checkedOut: bRow.checkedOutCount || 0,
                revenue: parseFloat(bRow.revenue) || 0
            },
            tables: {
                available: tRow.availableCount || 0,
                occupied: tRow.occupiedCount || 0,
                booked: tRow.bookedCount || 0,
                total: tRow.totalTables || 0
            }
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi thống kê"
        });
    }
});

// ======================================================
// START SERVER
// ======================================================

const httpServer = app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server chạy tại: http://localhost:${PORT}`);
    waitForDatabase()
        .then(() => getPool())
        .then(db => reconcileBookingAndTableStatuses(db))
        .then(() => console.log('Đã đồng bộ trạng thái booking và bàn'))
        .catch(error => console.error('Lỗi đồng bộ khi khởi động:', error));
});

httpServer.on('error', error => {
    console.error('Không thể khởi động HTTP server:', error);
    process.exit(1);
});

async function shutdown(signal) {
    console.log(`Đang dừng backend (${signal})...`);
    httpServer.close(async () => {
        if (pool) {
            await pool.close().catch(() => {});
        }
        process.exit(0);
    });
    setTimeout(() => process.exit(1), 5000).unref();
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('unhandledRejection', error => {
    console.error('Unhandled rejection:', error);
});
process.on('uncaughtException', error => {
    console.error('Uncaught exception:', error);
});
