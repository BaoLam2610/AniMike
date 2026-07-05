package com.lambao.animike.domain.model

import androidx.compose.runtime.Immutable

// thumbnailUrl (độ phân giải vừa) cho grid preview, fullUrl (độ phân giải lớn
// nhất có — large_image_url ưu tiên) cho viewer full-screen. TÁCH 2 field
// (trước đây chỉ 1 url dùng chung cho cả 2 nơi) để: (1) grid không tải ảnh
// nặng chỉ để hiển thị thumbnail nhỏ, (2) viewer LUÔN hiện đúng bản nét nhất
// thay vì phụ thuộc tình cờ vào việc large_image_url có tồn tại hay không.
@Immutable
data class Picture(
    val thumbnailUrl: String,
    val fullUrl: String,
)
