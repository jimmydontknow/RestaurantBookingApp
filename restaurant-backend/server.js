const express = require('express');
const cors = require('cors');
const sql = require('mssql');

const app = express();

app.use(cors());
app.use(express.json());

const PORT = 3000;

const config = {
    user: 'sa',
    password: 'phamminhnhat@2006',
    server: 'localhost',
    instanceName: 'MINHNHAT',
    database: 'RestaurantDB',
    options: {
        encrypt: false,
        trustServerCertificate: true
    }
};

let pool;

async function getPool() {
    if (!pool) {
        pool = await sql.connect(config);
        console.log('Đã kết nối thành công tới Database SQL Server!');
    }
    return pool;
}

// ======================================================
// GET BOOKINGS
// ======================================================

app.get('/api/bookings', async (req, res) => {
    try {

        const pool = await getPool();

        const result = await pool.request()
            .query(`
                SELECT *
                FROM Bookings
                ORDER BY BookingID DESC
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
            totalAmount: parseFloat(row.TotalAmount || 0),
            depositAmount: parseFloat(row.TotalAmount || 0),
            status: row.CurrentStatus,
            bookingDate: row.BookingDate
                ? row.BookingDate.toISOString().split('T')[0]
                : '',
            bookingTime: row.BookingTime || '',
            depositPaid:
                row.DepositPaid === true ||
                row.DepositPaid === 1
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

        await pool.request()
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
                parseFloat(depositAmount || 0)
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

        res.status(201).json({
            success: true,
            message: "Đặt bàn thành công!"
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
                    ORDER BY TableNumber ASC
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
                parseFloat(depositAmount || 0)
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
                ORDER BY TableNumber ASC
            `);

        const data = result.recordset.map(row => ({
            id: row.TableID.toString(),
            tableNumber: row.TableNumber,
            capacity: row.Capacity,
            status: row.CurrentStatus,
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
// GET INVOICES
// ======================================================

app.get('/api/invoices', async (req, res) => {

    try {

        const pool = await getPool();

        const result = await pool.request()
            .query(`
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
            totalAmount: parseFloat(row.TotalAmount),
            paymentMethod: row.PaymentMethod,
            paidAt: row.PaidAt,
            note: row.Note || ''
        }));

        res.json({
            success: true,
            invoices: data
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi hóa đơn"
        });
    }
});

// ======================================================
// CREATE INVOICE
// ======================================================

app.post('/api/invoices', async (req, res) => {

    try {

        const {
            bookingId,
            bookingCode,
            guestName,
            guestPhone,
            tableSummary,
            totalAmount,
            paymentMethod,
            note
        } = req.body;

        const pool = await getPool();

        await pool.request()
            .input(
                'BookingID',
                sql.Int,
                parseInt(bookingId)
            )
            .input(
                'BookingCode',
                sql.VarChar,
                bookingCode
            )
            .input(
                'GuestName',
                sql.NVarChar,
                guestName
            )
            .input(
                'GuestPhone',
                sql.VarChar,
                guestPhone || ''
            )
            .input(
                'TableSummary',
                sql.NVarChar,
                tableSummary
            )
            .input(
                'TotalAmount',
                sql.Decimal(18, 2),
                parseFloat(totalAmount)
            )
            .input(
                'PaymentMethod',
                sql.VarChar,
                paymentMethod
            )
            .input(
                'Note',
                sql.NVarChar,
                note || ''
            )
            .query(`
               INSERT INTO Invoices
               (
                   BookingID,
                   BookingCode,
                   GuestName,
                   GuestPhone,
                   TableSummary,
                   TotalAmount,
                   PaymentMethod,
                   Note,
                   PaidAt
               )
               VALUES
               (
                   @BookingID,
                   @BookingCode,
                   @GuestName,
                   @GuestPhone,
                   @TableSummary,
                   @TotalAmount,
                   @PaymentMethod,
                   @Note,
                   GETDATE()
               )
            `);

        await pool.request()
            .input(
                'ID',
                sql.Int,
                parseInt(bookingId)
            )
            .input(
                'PaymentMethod',
                sql.VarChar,
                paymentMethod
            )
            .query(`
                UPDATE Bookings
                SET
                    CurrentStatus='checked_out',
                    PaymentMethod=@PaymentMethod,
                    PaidAt=GETDATE()
                WHERE BookingID=@ID
            `);

        res.status(201).json({
            success: true,
            message: "Thanh toán thành công!"
        });

    } catch (error) {

        console.error(error);

        res.status(500).json({
            success: false,
            message: "Lỗi thanh toán: " + error.message
        });
    }
});

// ======================================================
// DASHBOARD STATS
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

getPool()
    .then(() => {

        app.listen(PORT, () => {

            console.log(
                `Server chạy tại: http://localhost:${PORT}`
            );
        });

    })
    .catch(err => {

        console.error(
            'Không thể kết nối SQL Server:',
            err
        );

        process.exit(1);
    });