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

app.get('/api/bookings', async (req, res) => {
    try {
        const pool = await getPool();
        const result = await pool.request()
            .query('SELECT * FROM Bookings ORDER BY BookingID DESC');
        const formattedData = result.recordset.map(row => ({
            id: row.BookingID.toString(),
            bookingCode: row.BookingCode,
            guestName: row.GuestName,
            guestPhone: row.PhoneNumber,
            tableSummary: row.TableSummary,
            totalAmount: parseFloat(row.TotalAmount),
            status: row.CurrentStatus
        }));
        res.json({ success: true, bookings: formattedData });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, error: "Lỗi lấy danh sách đặt bàn" });
    }
});

app.post('/api/bookings', async (req, res) => {
    try {
        const { guestName, phoneNumber, tableNumber, depositAmount, note } = req.body;
        if (!guestName || !phoneNumber || !tableNumber) {
            return res.status(400).json({ success: false, message: "Vui lòng nhập đầy đủ thông tin!" });
        }
        const pool = await getPool();
        const randomCode = "BK" + Math.floor(1000 + Math.random() * 9000);
        const tableSummaryText = `Bàn ${tableNumber.trim()}`;
        await pool.request()
            .input('Code', sql.VarChar, randomCode)
            .input('Name', sql.NVarChar, guestName)
            .input('Phone', sql.VarChar, phoneNumber)
            .input('Summary', sql.NVarChar, tableSummaryText)
            .input('Amount', sql.Decimal, parseFloat(depositAmount || 0))
            .query(`INSERT INTO Bookings (BookingCode, GuestName, PhoneNumber, TableSummary, TotalAmount, CurrentStatus)
                    VALUES (@Code, @Name, @Phone, @Summary, @Amount, 'pending')`);
        res.status(201).json({ success: true, message: "Đặt bàn thành công!" });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: "Lỗi server khi lưu dữ liệu!" });
    }
});

app.put('/api/bookings/status', async (req, res) => {
    try {
        const { id, status } = req.body;
        const pool = await getPool();
        await pool.request()
            .input('Status', sql.VarChar, status)
            .input('ID', sql.Int, parseInt(id))
            .query('UPDATE Bookings SET CurrentStatus = @Status WHERE BookingID = @ID');
        res.json({ success: true, message: "Cập nhật trạng thái thành công!" });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: "Lỗi cập nhật trạng thái" });
    }
});

app.put('/api/bookings/:id', async (req, res) => {
    try {
        const { id } = req.params;
        const { guestName, phoneNumber, tableNumber, depositAmount, status } = req.body;
        const pool = await getPool();
        const tableSummaryText = `Bàn ${tableNumber.trim()}`;
        await pool.request()
            .input('ID', sql.Int, parseInt(id))
            .input('Name', sql.NVarChar, guestName)
            .input('Phone', sql.VarChar, phoneNumber)
            .input('Summary', sql.NVarChar, tableSummaryText)
            .input('Amount', sql.Decimal, parseFloat(depositAmount || 0))
            .input('Status', sql.VarChar, status)
            .query(`UPDATE Bookings SET
                        GuestName = @Name,
                        PhoneNumber = @Phone,
                        TableSummary = @Summary,
                        TotalAmount = @Amount,
                        CurrentStatus = @Status
                    WHERE BookingID = @ID`);
        res.json({ success: true, message: "Cập nhật đặt bàn thành công!" });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: "Lỗi cập nhật: " + error.message });
    }
});

app.delete('/api/bookings/:id', async (req, res) => {
    try {
        const { id } = req.params;
        const pool = await getPool();
        await pool.request()
            .input('ID', sql.Int, parseInt(id))
            .query('DELETE FROM Bookings WHERE BookingID = @ID');
        res.json({ success: true, message: "Xóa đặt bàn thành công!" });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: "Lỗi xóa: " + error.message });
    }
});

app.get('/api/tables', async (req, res) => {
    try {
        const pool = await getPool();
        const result = await pool.request()
            .query('SELECT * FROM Tables ORDER BY TableNumber ASC');
        const formattedData = result.recordset.map(row => ({
            id: row.TableID.toString(),
            tableNumber: row.TableNumber,
            capacity: row.Capacity,
            status: row.CurrentStatus
        }));
        res.json({ success: true, tables: formattedData });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: "Lỗi lấy danh sách bàn" });
    }
});

app.put('/api/tables/status', async (req, res) => {
    try {
        const { id, status } = req.body;
        const pool = await getPool();
        await pool.request()
            .input('Status', sql.NVarChar, status)
            .input('ID', sql.Int, parseInt(id))
            .query('UPDATE Tables SET CurrentStatus = @Status WHERE TableID = @ID');
        res.json({ success: true, message: "Cập nhật trạng thái bàn thành công!" });
    } catch (error) {
        console.error(error);
        res.status(500).json({ success: false, message: "Lỗi cập nhật: " + error.message });
    }
});

getPool()
    .then(() => {
        app.listen(PORT, () => {
            console.log(`Server chạy tại: http://localhost:${PORT}`);
        });
    })
    .catch(err => {
        console.error('Không thể kết nối SQL Server:', err);
        process.exit(1);
    });