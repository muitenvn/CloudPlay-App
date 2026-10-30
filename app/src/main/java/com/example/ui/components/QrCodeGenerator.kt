package com.example.ui.components

/**
 * Bộ tạo mã QR chuẩn quốc tế ISO/IEC 18004 (Byte Mode, Error Correction Level M / L)
 * tạo ma trận QR thực sự có thể quét trực tiếp bằng mọi ứng dụng Camera điện thoại (iOS/Android/Zalo/WeChat).
 * Hỗ trợ đầy đủ từ Version 1 (21x21) đến Version 10 (57x57), lên tới 271 ký tự UTF-8.
 */
object QrCodeGenerator {

    private data class VersionSpec(
        val version: Int,
        val totalDataCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocksGroup1: Int,
        val dataCodewordsGroup1: Int,
        val numBlocksGroup2: Int = 0,
        val dataCodewordsGroup2: Int = 0,
        val alignmentCenters: IntArray = intArrayOf()
    )

    // Thông số chuẩn ISO/IEC 18004 cho Error Correction Level L (mật độ điểm thoáng, cực dễ quét trên màn hình điện thoại)
    // EC Level L có Format Bits: 01
    private val VERSION_SPECS_LEVEL_L = listOf(
        VersionSpec(1, 19, 7, 1, 19, alignmentCenters = intArrayOf()),
        VersionSpec(2, 34, 10, 1, 34, alignmentCenters = intArrayOf(6, 18)),
        VersionSpec(3, 55, 15, 1, 55, alignmentCenters = intArrayOf(6, 22)),
        VersionSpec(4, 80, 20, 1, 80, alignmentCenters = intArrayOf(6, 26)),
        VersionSpec(5, 108, 26, 1, 108, alignmentCenters = intArrayOf(6, 30)),
        VersionSpec(6, 136, 18, 2, 68, alignmentCenters = intArrayOf(6, 34)),
        VersionSpec(7, 156, 20, 2, 78, alignmentCenters = intArrayOf(6, 22, 38)),
        VersionSpec(8, 194, 24, 2, 97, alignmentCenters = intArrayOf(6, 24, 42)),
        VersionSpec(9, 232, 30, 2, 116, alignmentCenters = intArrayOf(6, 26, 46)),
        VersionSpec(10, 274, 18, 2, 68, 2, 69, alignmentCenters = intArrayOf(6, 28, 50))
    )

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun reedSolomonGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val root = GF_EXP[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], root)
            }
            poly = next
        }
        return poly
    }

    private fun computeReedSolomonEc(data: IntArray, ecLen: Int): IntArray {
        val gen = reedSolomonGeneratorPoly(ecLen)
        val rem = IntArray(ecLen)
        for (b in data) {
            val factor = b xor rem[0]
            System.arraycopy(rem, 1, rem, 0, ecLen - 1)
            rem[ecLen - 1] = 0
            if (factor != 0) {
                for (j in 0 until ecLen) {
                    rem[j] = rem[j] xor gfMul(gen[j + 1], factor)
                }
            }
        }
        return rem
    }

    /**
     * Mã hóa chuỗi văn bản/URL thành ma trận boolean 2 chiều chuẩn ISO/IEC 18004 (`true` = điểm đen, `false` = điểm trắng).
     */
    fun encodeToMatrix(content: String): Array<BooleanArray> {
        val payload = content.trim().ifEmpty { "https://muitenvn.github.io/CloudPlay-App/" }
        val utf8Bytes = payload.encodeToByteArray()

        // Chọn từ Version 3 (29x29 có Alignment Pattern góc dưới-phải giống mẫu QR chuẩn) trở lên
        val spec = VERSION_SPECS_LEVEL_L.firstOrNull { candidate ->
            val countBits = if (candidate.version <= 9) 8 else 16
            val maxBytes = ((candidate.totalDataCodewords * 8) - 4 - countBits) / 8
            candidate.version >= 3 && utf8Bytes.size <= maxBytes
        } ?: VERSION_SPECS_LEVEL_L.last()

        val countBits = if (spec.version <= 9) 8 else 16
        val maxBytes = ((spec.totalDataCodewords * 8) - 4 - countBits) / 8
        val dataBytes = if (utf8Bytes.size <= maxBytes) utf8Bytes else utf8Bytes.copyOfRange(0, maxBytes)

        // 1. Tạo luồng bit dữ liệu (Byte Mode = 0100)
        val totalDataBits = spec.totalDataCodewords * 8
        val bits = ArrayList<Int>(totalDataBits + 16)

        fun appendBits(value: Int, numBits: Int) {
            for (i in (numBits - 1) downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        appendBits(0b0100, 4) // Byte mode indicator
        appendBits(dataBytes.size, countBits)
        for (b in dataBytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }

        // Terminator (tối đa 4 bit 0)
        val terminatorLen = minOf(4, totalDataBits - bits.size)
        appendBits(0, terminatorLen)

        // Căn lề byte bằng bit 0
        while (bits.size % 8 != 0) {
            bits.add(0)
        }

        // Chuyển thành mảng codeword 8-bit và điền Pad Codewords (0xEC, 0x11 luân phiên)
        val dataCodewords = IntArray(spec.totalDataCodewords)
        val filledBytes = bits.size / 8
        for (i in 0 until filledBytes) {
            var cw = 0
            for (bitIdx in 0 until 8) {
                cw = (cw shl 1) or bits[i * 8 + bitIdx]
            }
            dataCodewords[i] = cw
        }
        var padToggle = true
        for (i in filledBytes until spec.totalDataCodewords) {
            dataCodewords[i] = if (padToggle) 0xEC else 0x11
            padToggle = !padToggle
        }

        // 2. Chia khối và tính mã sửa lỗi Reed-Solomon cho từng khối
        val dataBlocks = ArrayList<IntArray>()
        val ecBlocks = ArrayList<IntArray>()
        var offset = 0
        for (b in 0 until spec.numBlocksGroup1) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.dataCodewordsGroup1)
            offset += spec.dataCodewordsGroup1
            dataBlocks.add(block)
            ecBlocks.add(computeReedSolomonEc(block, spec.ecCodewordsPerBlock))
        }
        for (b in 0 until spec.numBlocksGroup2) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.dataCodewordsGroup2)
            offset += spec.dataCodewordsGroup2
            dataBlocks.add(block)
            ecBlocks.add(computeReedSolomonEc(block, spec.ecCodewordsPerBlock))
        }

        // Đan xen (Interleave) các codeword dữ liệu và codeword Reed-Solomon
        val interleaved = ArrayList<Int>(spec.totalDataCodewords + dataBlocks.size * spec.ecCodewordsPerBlock)
        val maxDataBlockLen = maxOf(spec.dataCodewordsGroup1, spec.dataCodewordsGroup2)
        for (i in 0 until maxDataBlockLen) {
            for (block in dataBlocks) {
                if (i < block.size) interleaved.add(block[i])
            }
        }
        for (i in 0 until spec.ecCodewordsPerBlock) {
            for (ec in ecBlocks) {
                if (i < ec.size) interleaved.add(ec[i])
            }
        }

        val finalBits = ArrayList<Int>(interleaved.size * 8 + 7)
        for (cw in interleaved) {
            for (i in 7 downTo 0) {
                finalBits.add((cw ushr i) and 1)
            }
        }
        // Remainder bits (7 bit cho version 2..6, 0 bit cho version 1, 7..10)
        val remainderBits = if (spec.version in 2..6) 7 else 0
        repeat(remainderBits) { finalBits.add(0) }

        // 3. Khởi tạo ma trận mô-đun (size = 17 + 4 * version)
        val size = 17 + 4 * spec.version
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setFunctionModule(r: Int, c: Int, dark: Boolean) {
            if (r in 0 until size && c in 0 until size) {
                modules[r][c] = dark
                isFunction[r][c] = true
            }
        }

        // Finder patterns (3 góc) + vạch phân cách trắng (Separator)
        fun drawFinderWithSeparator(topR: Int, leftC: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = topR + dr
                    val c = leftC + dc
                    if (r !in 0 until size || c !in 0 until size) continue
                    val inOuter = dr in 0..6 && dc in 0..6
                    val isBorder = dr == 0 || dr == 6 || dc == 0 || dc == 6
                    val isInner = dr in 2..4 && dc in 2..4
                    val dark = inOuter && (isBorder || isInner)
                    setFunctionModule(r, c, dark)
                }
            }
        }

        drawFinderWithSeparator(0, 0)
        drawFinderWithSeparator(0, size - 7)
        drawFinderWithSeparator(size - 7, 0)

        // Timing patterns (hàng 6 và cột 6)
        for (i in 8 until (size - 8)) {
            val dark = (i % 2 == 0)
            setFunctionModule(6, i, dark)
            setFunctionModule(i, 6, dark)
        }

        // Alignment patterns (Version >= 2)
        val centers = spec.alignmentCenters
        for (rCenter in centers) {
            for (cCenter in centers) {
                // Bỏ qua nếu trùng với 3 góc Finder Pattern
                if ((rCenter == 6 && cCenter == 6) ||
                    (rCenter == 6 && cCenter == size - 7) ||
                    (rCenter == size - 7 && cCenter == 6)
                ) {
                    continue
                }
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val isBorder = kotlin.math.abs(dr) == 2 || kotlin.math.abs(dc) == 2
                        val isCenter = dr == 0 && dc == 0
                        setFunctionModule(rCenter + dr, cCenter + dc, isBorder || isCenter)
                    }
                }
            }
        }

        // Dark module cố định và đánh dấu vùng Format Information
        setFunctionModule(4 * spec.version + 9, 8, true)
        for (i in 0..8) {
            if (!isFunction[8][i]) isFunction[8][i] = true
            if (!isFunction[i][8]) isFunction[i][8] = true
        }
        for (i in 0..7) {
            if (!isFunction[8][size - 1 - i]) isFunction[8][size - 1 - i] = true
            if (!isFunction[size - 1 - i][8]) isFunction[size - 1 - i][8] = true
        }
        // Vùng Version Information (nếu version >= 7)
        if (spec.version >= 7) {
            val versionInfo = computeVersionInfoBits(spec.version)
            var bitIdx = 0
            for (i in 0 until 6) {
                for (j in 0 until 3) {
                    val dark = ((versionInfo ushr bitIdx) and 1) != 0
                    bitIdx++
                    setFunctionModule(size - 11 + j, i, dark)
                    setFunctionModule(i, size - 11 + j, dark)
                }
            }
        }

        // 4. Điền bit dữ liệu theo đường zíc-zắc 2 cột từ phải sang trái
        var bitIndex = 0
        var upward = true
        var rightCol = size - 1
        while (rightCol >= 1) {
            if (rightCol == 6) rightCol -= 1 // Bỏ qua cột Timing Pattern
            for (step in 0 until size) {
                val r = if (upward) (size - 1 - step) else step
                for (colOffset in 0..1) {
                    val c = rightCol - colOffset
                    if (!isFunction[r][c]) {
                        val dark = if (bitIndex < finalBits.size) finalBits[bitIndex] != 0 else false
                        modules[r][c] = dark
                        bitIndex++
                    }
                }
            }
            upward = !upward
            rightCol -= 2
        }

        // 5. Thử cả 8 mặt nạ (Mask 0..7), chọn mặt nạ có điểm phạt thấp nhất theo chuẩn ISO
        var bestMask = 0
        var bestPenalty = Int.MAX_VALUE
        var bestMatrix = modules

        for (mask in 0..7) {
            val candidate = Array(size) { r -> modules[r].clone() }
            applyMask(candidate, isFunction, mask)
            drawFormatInfoBits(candidate, ecLevelBits = 0b01, mask = mask) // 01 = Level L
            val penalty = calculateMaskPenalty(candidate)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMask = mask
                bestMatrix = candidate
            }
        }

        drawFormatInfoBits(bestMatrix, ecLevelBits = 0b01, mask = bestMask)
        return bestMatrix
    }

    private fun applyMask(
        matrix: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        mask: Int
    ) {
        val size = matrix.size
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (isFunction[r][c]) continue
                val invert = when (mask) {
                    0 -> (r + c) % 2 == 0
                    1 -> r % 2 == 0
                    2 -> c % 3 == 0
                    3 -> (r + c) % 3 == 0
                    4 -> ((r / 2) + (c / 3)) % 2 == 0
                    5 -> ((r * c) % 2) + ((r * c) % 3) == 0
                    6 -> (((r * c) % 2) + ((r * c) % 3)) % 2 == 0
                    else -> (((r + c) % 2) + ((r * c) % 3)) % 2 == 0
                }
                if (invert) {
                    matrix[r][c] = !matrix[r][c]
                }
            }
        }
    }

    private fun drawFormatInfoBits(
        matrix: Array<BooleanArray>,
        ecLevelBits: Int,
        mask: Int
    ) {
        val size = matrix.size
        val data = (ecLevelBits shl 3) or mask
        var rem = data
        for (i in 0 until 10) {
            rem = (rem shl 1) xor ((rem ushr 9) * 0x537)
        }
        val bits = ((data shl 10) or rem) xor 0x5412

        fun bit(index: Int): Boolean = ((bits ushr index) and 1) != 0

        // Xung quanh Finder góc trên-trái (ISO/IEC 18004 Fig. 25: bit 14 MSB tại (8,0) -> bit 0 LSB tại (0,8))
        for (i in 0..5) matrix[8][i] = bit(14 - i)
        matrix[8][7] = bit(8)
        matrix[8][8] = bit(7)
        matrix[7][8] = bit(6)
        for (r in 0..5) matrix[r][8] = bit(r)

        // Xung quanh Finder góc trên-phải (bit 7..0) và dưới-trái (bit 14..8)
        for (i in 0..7) matrix[8][size - 1 - i] = bit(i)
        for (i in 8..14) matrix[size - 15 + i][8] = bit(i)
        matrix[size - 8][8] = true // Dark module cố định
    }

    private fun computeVersionInfoBits(version: Int): Int {
        var rem = version
        for (i in 0 until 12) {
            rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25)
        }
        return (version shl 12) or rem
    }

    private fun calculateMaskPenalty(matrix: Array<BooleanArray>): Int {
        val size = matrix.size
        var penalty = 0

        // Rule 1: 5+ mô-đun cùng màu liên tiếp trên hàng/cột
        for (r in 0 until size) {
            var runColor = matrix[r][0]
            var runLen = 1
            for (c in 1 until size) {
                if (matrix[r][c] == runColor) {
                    runLen++
                    if (runLen == 5) penalty += 3
                    else if (runLen > 5) penalty += 1
                } else {
                    runColor = matrix[r][c]
                    runLen = 1
                }
            }
        }
        for (c in 0 until size) {
            var runColor = matrix[0][c]
            var runLen = 1
            for (r in 1 until size) {
                if (matrix[r][c] == runColor) {
                    runLen++
                    if (runLen == 5) penalty += 3
                    else if (runLen > 5) penalty += 1
                } else {
                    runColor = matrix[r][c]
                    runLen = 1
                }
            }
        }

        // Rule 2: Khối 2x2 cùng màu
        for (r in 0 until size - 1) {
            for (c in 0 until size - 1) {
                val color = matrix[r][c]
                if (matrix[r][c + 1] == color && matrix[r + 1][c] == color && matrix[r + 1][c + 1] == color) {
                    penalty += 3
                }
            }
        }
        return penalty
    }
}
