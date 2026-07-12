package com.lambao.animike.data.repository

import com.lambao.animike.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

// MVP-Debug Đợt 1 — thống kê + xoá cache cho tab "Cache" của màn Debug. Đọc
// bảng generic qua sqlite_master (KHÔNG liệt kê tay ~23 bảng, tự bắt kịp khi
// thêm entity mới). CHỈ dùng ở DEBUG build (màn Debug gate BuildConfig.DEBUG).
interface DebugRepository {
    suspend fun tableStats(): List<TableStat>

    // Chi tiết 1 bảng: schema (cột) + dòng mẫu — cho màn xem "đang cache gì"
    // (Đợt 2). limit=null nghĩa là "Tất cả" (bỏ SQL LIMIT, user tự chọn ở UI
    // sau khi thấy totalRowCount, xem Đợt 3).
    suspend fun tableDetail(name: String, limit: Int?): TableDetail

    // Xoá 1 bảng cụ thể (kể cả favorite/tracking — có confirm ở UI).
    suspend fun clearTable(name: String)

    // Xoá MỌI bảng cache — LOẠI TRỪ favorite/tracking (dữ liệu user thật từ
    // v17, xem DatabaseModule). Đây là hành động "reset cache" an toàn.
    suspend fun clearCacheTables()
}

data class TableStat(
    val name: String,
    val rowCount: Int,
    // favorite/tracking = dữ liệu user thật, KHÔNG bị "Xoá cache" đụng tới; UI
    // tô khác màu + bắt confirm khi xoá riêng.
    val isUserData: Boolean,
)

data class ColumnInfo(val name: String, val type: String, val primaryKey: Boolean)

data class TableDetail(
    val name: String,
    val columns: List<ColumnInfo>,
    // Tổng số dòng THẬT của bảng (không bị cắt bởi limit) — UI hiện trước khi
    // user chọn "Tất cả" để biết trước quy mô (Đợt 3).
    val totalRowCount: Int,
    // Mỗi dòng là list value ĐÃ căn theo thứ tự columns; null = giá trị NULL,
    // BLOB hiện "<blob N B>" (không dựng byte thô lên UI).
    val rows: List<List<String?>>,
)

// Bảng chứa dữ liệu user thật (không phải cache) — xem cảnh báo v17 ở
// DatabaseModule. clearCacheTables() bỏ qua 2 bảng này.
private val USER_DATA_TABLES = setOf("favorite", "tracking")

class DebugRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : DebugRepository {

    // ⚠️ Truy cập writableDatabase trực tiếp bỏ qua invalidation tracker của
    // Room — sau khi xoá, các Flow observeXxx() đang mở KHÔNG tự refresh ngay;
    // chấp nhận được cho công cụ DEBUG (mở lại màn là thấy). Không dùng cho
    // luồng app thật.
    override suspend fun tableStats(): List<TableStat> = withContext(Dispatchers.IO) {
        userTables().map { name ->
            val count = database.openHelper.writableDatabase
                .query("SELECT COUNT(*) FROM \"$name\"")
                .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
            TableStat(name = name, rowCount = count, isUserData = name in USER_DATA_TABLES)
        }.sortedWith(compareBy({ it.isUserData }, { it.name }))
    }

    override suspend fun tableDetail(name: String, limit: Int?): TableDetail = withContext(Dispatchers.IO) {
        // Bảng không tồn tại → trả rỗng thay vì ném (màn chi tiết hiện "trống").
        if (name !in userTables()) {
            return@withContext TableDetail(name = name, columns = emptyList(), totalRowCount = 0, rows = emptyList())
        }
        val db = database.openHelper.writableDatabase

        val totalRowCount = db.query("SELECT COUNT(*) FROM \"$name\"")
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

        // PRAGMA table_info trả các cột: cid(0) name(1) type(2) notnull(3)
        // dflt_value(4) pk(5). pk != 0 nghĩa là khoá chính.
        val columns = db.query("PRAGMA table_info(\"$name\")").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        ColumnInfo(
                            name = cursor.getString(1),
                            type = cursor.getString(2),
                            primaryKey = cursor.getInt(5) != 0,
                        ),
                    )
                }
            }
        }

        // limit=null ("Tất cả") → bỏ hẳn mệnh đề LIMIT thay vì truyền số cực
        // lớn — LazyColumn ở UI chỉ compose item đang hiển thị nên vài nghìn
        // dòng vẫn mượt (Đợt 3, góp ý user).
        val limitClause = if (limit != null) " LIMIT $limit" else ""
        val rows = db.query("SELECT * FROM \"$name\"$limitClause").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        (0 until cursor.columnCount).map { i ->
                            when (cursor.getType(i)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> null
                                android.database.Cursor.FIELD_TYPE_BLOB -> "<blob ${cursor.getBlob(i).size} B>"
                                else -> cursor.getString(i)
                            }
                        },
                    )
                }
            }
        }
        TableDetail(name = name, columns = columns, totalRowCount = totalRowCount, rows = rows)
    }

    override suspend fun clearTable(name: String) = withContext(Dispatchers.IO) {
        // Chỉ cho xoá bảng CÓ THẬT (chống SQL injection dù name vốn từ danh
        // sách nội bộ) — name không khớp thì bỏ qua im lặng.
        if (name in userTables()) {
            database.openHelper.writableDatabase.execSQL("DELETE FROM \"$name\"")
        }
    }

    override suspend fun clearCacheTables() = withContext(Dispatchers.IO) {
        val db = database.openHelper.writableDatabase
        userTables().filter { it !in USER_DATA_TABLES }.forEach { name ->
            db.execSQL("DELETE FROM \"$name\"")
        }
    }

    // Bảng do app tạo — loại bảng nội bộ của SQLite/Room (sqlite_*, room_*,
    // android_metadata).
    private fun userTables(): List<String> =
        database.openHelper.writableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' " +
                "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' AND name != 'android_metadata'",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
}
