package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

enum class GameFilterTab {
    ALL,
    CONTINUE_PLAYING,
    FAVORITES,
    POPULAR,
    RECENTLY_ADDED,
    FREE_TO_PLAY,
    TOUCH_FRIENDLY,
    GAME_PASS
}

@androidx.compose.runtime.Immutable
data class XboxCloudSyncStatus(
    val gamertag: String? = null,
    val avatarUrl: String? = null,
    val subscriptionTier: String? = null,
    val cloudServiceOnline: Boolean = true,
    val cloudLatencyMs: Int? = null,
    val totalCloudCatalogCount: Int = 0,
    val syncedCatalogCount: Int = 0,
    val jumpBackInIds: List<String> = emptyList(),
    val lastSyncTimeMs: Long = 0L
)

@androidx.compose.runtime.Immutable
data class GameBookmark(
    val id: String,
    val title: String,
    val category: String,
    val playUrl: String,
    val freeToPlay: Boolean = false,
    val supportsTouch: Boolean = false,
    val accentColorHex: Long = 0xFF107C10,
    val badge: String? = null,
    val drawableResId: Int? = null,
    val rating: String = "4.8",
    val categoryEn: String = category,
    val badgeEn: String? = badge,
    val imageUrl: String? = null,
    val heroImageUrl: String? = null,
    val description: String? = null,
    val publisher: String? = null,
    val isPopular: Boolean = false,
    val isRecentlyAdded: Boolean = false
) {
    val monogram: String = run {
        var firstChar: Char? = null
        var secondChar: Char? = null
        var inWord = false
        for (i in 0 until title.length) {
            val c = title[i]
            if (c.isLetterOrDigit()) {
                if (!inWord) {
                    inWord = true
                    if (firstChar == null) {
                        firstChar = c.uppercaseChar()
                    } else if (secondChar == null) {
                        secondChar = c.uppercaseChar()
                        break
                    }
                }
            } else if (c.isWhitespace()) {
                inWord = false
            }
        }
        when {
            firstChar != null && secondChar != null -> "$firstChar$secondChar"
            firstChar != null -> "$firstChar"
            else -> "XB"
        }
    }

    val secondaryColorHex: Long = when ((title.hashCode() and 0x7FFFFFFF) % 4) {
        0 -> 0xFF00E5FFL
        1 -> 0xFF107C10L
        2 -> 0xFF7C4DFFL
        else -> 0xFF00B4D8L
    }

    fun localizedCategory(isEn: Boolean): String = if (isEn) {
        if (category == "Game tùy chỉnh") "Custom Game" else categoryEn
    } else category

    fun localizedBadge(isEn: Boolean): String? = if (isEn) {
        if (badge == "Tự thêm") "Custom" else badgeEn
    } else badge
}

// Kho game để trống hoàn toàn trước khi quét; chỉ tải thông tin (metadata) từ Xbox Cloud, không tải ảnh để tiết kiệm dữ liệu
val INITIAL_XBOX_GAMES: List<GameBookmark> = emptyList()
private const val FORTNITE_PRODUCT_ID = "BT5P2X999VH2"

object GameLibraryRepository {
    private const val PREFS_NAME = "cloudplay_custom_games_prefs"
    private const val RECENT_PREFS_NAME = "cloudplay_recent_game_prefs"
    private const val KEY_CUSTOM_GAMES_JSON = "custom_games_json"
    private const val KEY_SYNCED_GAMES_JSON = "synced_xbox_games_json_v4"
    private const val SYNCED_GAMES_CACHE_FILE_NAME = "xbox_synced_games_v4.json"
    private const val KEY_LAST_CATALOG_SYNC_MS = "last_catalog_sync_ms"
    private const val KEY_FAVORITE_IDS = "favorite_game_ids"
    private const val KEY_LAST_PLAYED_ID = "last_played_game_id"
    private const val KEY_PLAYED_GAME_IDS_LIST = "played_game_ids_ordered_v1"
    private const val KEY_XBOX_GAMERTAG = "xbox_gamertag"
    private const val KEY_XBOX_AVATAR_URL = "xbox_avatar_url"
    private const val KEY_XBOX_SUB_TIER = "xbox_sub_tier"
    private const val KEY_XBOX_TOTAL_CLOUD = "xbox_total_cloud_count"
    private const val KEY_XBOX_JUMP_BACK_IDS = "xbox_jump_back_ids"

    private const val CLOUD_CATALOG_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=29a81209-df6f-41fd-a528-2ae6b91f719c&language=en-us&market=US"
    private const val FREE_TO_PLAY_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=51f14e5d-bdcb-4e04-b9cb-76e5057702df&language=en-us&market=US"
    private const val TOUCH_CATALOG_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=9c86f07a-f3e8-45ad-82a0-a1f759597059&language=en-us&market=US"
    private const val POPULAR_CLOUD_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=6a589fa0-d493-472b-8e20-3813699d7056&language=en-us&market=US"
    private const val RECENTLY_ADDED_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=06323672-b8c8-43cc-b0de-32d5a9834749&language=en-us&market=US"

    // 8 SIGL thể loại chính thức trên trang play.xbox.com (GenresRow)
    private const val GENRE_INDIE_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=36f22fa5-3d2e-4b1d-818f-4308ab0ffa2e&language=en-us&market=US"
    private const val GENRE_FAMILY_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=c51f789c-cc6c-4f31-b9ed-0cc97b04d455&language=en-us&market=US"
    private const val GENRE_SHOOTERS_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=725d2704-860d-4bdf-a827-ac93dc729a96&language=en-us&market=US"
    private const val GENRE_ACTION_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=f913b4be-6ca1-44ac-946a-1a481602595c&language=en-us&market=US"
    private const val GENRE_FIGHTING_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=d34a6cdb-e678-4193-89a1-0dc86360cfa7&language=en-us&market=US"
    private const val GENRE_STRATEGY_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=67aaebe2-6215-4258-aeab-2c837bc7e34c&language=en-us&market=US"
    private const val GENRE_SIMULATION_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=cd6e5cbb-9f14-4d0d-bb38-a4b22b10f403&language=en-us&market=US"
    private const val GENRE_RPG_SIGL_URL =
        "https://catalog.gamepass.com/sigls/v2?id=455e5b52-9454-41ad-a00a-9f2d5e9d6549&language=en-us&market=US"

    // Danh sách 95 ProductId chính thức từ SIGL "Truyền phát miễn phí với quảng cáo" (FresnoMain: 51f14e5d-bdcb-4e04-b9cb-76e5057702df) của Xbox Cloud Gaming
    private val OFFICIAL_FREE_TO_PLAY_PRODUCT_IDS = setOf(
        "BT5P2X999VH2", "9NNFG8BQRCXL", "9P4837D6TSFD", "9PMFDKG5F9R7", "9P22THR57FV7",
        "9N8PMW7QMD3D", "9P6LNN3KZ75R", "9N872NBW5XG5", "9N0QWXC09MPT", "9N76QGDS5GJR",
        "9PL3KW681MSD", "9PGLFS9MB9CG", "9PK8PHLCQDF6", "BX1DTCBD1HXJ", "9NZQPT0MWTD0",
        "BVM002M8HH0S", "9N1RRVLK0JCL", "9NVJWR50KRXX", "9PC5KKHSMCT5", "9PGD71CMDS0Z",
        "BQF41W70ZLSV", "9PP5G1F0C2B6", "BPS3XF74B9V1", "9N4X0WFZ2STM", "9N7TFFRRZCC9",
        "9PPBQVV15N4P", "C3B1V55CDL0C", "9P41GS5W1C6L", "9N9L211HC9LQ", "BQL8L17RV09Z",
        "9NLX1C7RRBRP", "9PMR3T9NSF8W", "9NGN7D8GJN02", "9NSSDJRBX2HD", "9P8FBXKMCLWC",
        "9P96KKM6TFBN", "9NR81PCSLLB6", "9NHKL695M9F2", "9PDC9X7BKZR3", "BP5HKF86C1NJ",
        "9NX2FLJQ8QBN", "9PF2ZS4XB4JH", "9NKVBTRRWH6V", "9N11JM7F5CXK", "9PJFMVBP16CT",
        "C1ZT6N30L1WH", "C57L9GR0HHB7", "BWC95BZPFBS7", "9NV8W1KJSQLL", "9PF5Q1B0FHSL",
        "9N1BZS0V3K0C", "9P4S1BPJLPHZ", "C59QBPB8P1XJ", "BPHSZ44NGDB2", "9N9HW342BZ2X",
        "BP4S3XLJG78M", "9P8SQQJ5T94B", "9MW715J1P29D", "9MZL842T57QN", "9PGHVX9B5RZS",
        "BTNTWQD5V8WG", "9PF432CVQBXT", "9MW1V43D3VZN", "9NNVK1DTR206", "9NHFDZRD753R",
        "C2MHS238PDNS", "9P4W8H2HNLN4", "9P0VF4BSN88L", "9N872VFSPPQL", "9PCWTRXGSHHJ",
        "9PNPGV4WKS47", "BSRXXBC5L1BV", "9NCD341CHC8G", "9NHXKP9579JX", "9N4M72L6KGC8",
        "9P4D41K02WB8", "9NKZ0RBTNB9B", "9NHLV358CD75", "9MTVJ3HHTQGS", "9NBL0XKVCN5L",
        "9NZJHZ16G17J", "9N3MRPP8KKRS", "BNZ2TD4Q2PVD", "9PLFMN1C160R", "9NRMXFFD3K13",
        "9NHQ5CB04K4P", "9PDWDPMWXNDX", "9N0K0Q39X4NT", "9MVTGWDJD7TP", "9NZ47R1HNPQ1",
        "BQT2R5FK3P7R", "9NLF1ZKQ269R", "9NRL0NM003WW", "BQZKFWSZX483", "9P5CV7QW54SC"
    )

    // Danh sách 126 ProductId chính thức từ SIGL "Chơi bằng cảm ứng" (CloudPlayWithTouch: 9c86f07a-f3e8-45ad-82a0-a1f759597059)
    private val OFFICIAL_TOUCH_PRODUCT_IDS = setOf(
        "9PD5BM2Z8C4L", "9NZDHXL9SJJ8", "9NG07QJNK38J", "9NCJWHHMVHR0", "9N8NJ74FZTG9",
        "9PK8PHLCQDF6", "9NSM674QVLBS", "9MWXF3SRD7QN", "9P77PG7DK0W2", "9N4X0WFZ2STM",
        "BRG51C5MWFSG", "C2P985H1H42H", "C4B8XR1LCXR5", "9MVN4ND41DD3", "9N6PHQ93Z451",
        "9N7V5R3VLMJZ", "9PKJTP6DTND3", "9N9X9PC8V9CP", "9N255K81XBD3", "9N431PX143P8",
        "9NDJLXD2X2DM", "BRRVN5198461", "9NGCFDHR2MV3", "9NJWFF2KHL6H", "9N9C8SV0S16W",
        "9MW53ZKZH168", "9MZNS9NZ97PF", "9NLRT31Z4RWM", "9NSF0BGH8D86", "9P8DL6W0JBB8",
        "9PJTHRNVH62H", "BRJNRZ9N734V", "BVJLKDG2TX8H", "C3D891Z6TNQM", "C4GH8N6ZXG5L",
        "9PCR3Z2MSM4T", "9N8C07SS8C9D", "9NJCVGS6T30K", "9NWDPFPPK30V", "9MSVPJCHHRPR",
        "9P8S1MN4066K", "9N7TFFRRZCC9", "9P20JCF7BV93", "BT5P2X999VH2", "9P8RMKXRML7D",
        "9N51FH42VQDS", "9PMBB7NGHV95", "BW9TWC8L4JCS", "BZGJRJC1FGF3", "C07KJZRH0L7S",
        "C350VBGGCL6P", "C4Z7QM8FSXM2", "BSDX4FNNC0DK", "9NNNMBXFFG4W", "BX03760D0QGN",
        "9N42SSSX2MTG", "9MZN3SMXN824", "9N2HB4MWG7HM", "9N6639H7VGH4", "9NF83PRZK6K3",
        "9NN3HCKW5TPC", "9NR7XDNVP5SW", "9NX6K9HN4F4K", "9P008L2LS87F", "9P1P5Q3BNM7J",
        "9P2N57MC619K", "9P7SL78VHVMF", "9P9VNSQK1W3K", "9PC4RWP34M2D", "9PHK9D8CLQCG",
        "9PJD2KMX7TZ6", "9PMQDM08SNK9", "BPRPQSKXTD1L", "BQ9T0JF0D3L4", "BQR7QS0F8SJ3",
        "BR46KM4D5B9L", "BRT1K9FV4LRR", "BS1NPTPJGD4G", "BS34VNW7H61F", "BSJG7TTSWVJ2",
        "BSLTQT4L0RLV", "BSXSL6WBJ0V7", "BW6B077FCH11", "BX27S00SKW1V", "C1ZWH2BZ9TSF",
        "C2B4T86TXLRS", "C2DQXX0CB42F", "C2WKJJ9F5936", "C2XNJC9WK15X", "C521HDXRTS7F",
        "C5HHPG1TXDNG", "9NBR2VXT87SJ", "9NGH1FK0RJGL", "9P4KMR76PLLQ", "9PN3VDFTB5HZ",
        "BQND4NQXFFV2", "BV0K9LMLQ9W5", "C2Q32JM0BPZL", "C42KCJCLX6MX", "9NSD4HTWM8HT",
        "9NTFPTBXDDT3", "9NLB6V0GNC9P", "9PP837L3Q75D", "9PLFD17M47K3", "9NCT9L7NFHX7",
        "9NVRJS95FLM9", "9NP6V9LFVJ87", "9PKWHT7G60WQ", "9NXVC0482QS5", "C3QWNCV55VLL",
        "9NMBJQ0265ZK", "BV9CWVQWNS4P", "BRZZLBF5T245", "C47GZZBMR5WG", "BNCZHKWRZ7BR",
        "BNRH7BRC1D02", "BP73LMGB1DFD", "BRJLJ6RPLQTJ", "BS9SX4Q6XJRF", "BTJ0T8C04ZBV",
        "BZPR04V49BMH", "C2PB7STLJGS0", "C48LBRJJCP2L", "C4G76555T4QN", "C4HZC7LJG6PX",
        "C4VKLMG1HLZW"
    )

    // Danh sách 56 ProductId chính thức từ SIGL "Mới thêm gần đây" (RecentlyAdded: 06323672-b8c8-43cc-b0de-32d5a9834749)
    private val OFFICIAL_RECENTLY_ADDED_PRODUCT_IDS = setOf(
        "9MTGCZ84M503", "9P1S707BPW6W", "9NWWL1CBWFCG", "9N5DDTDLTWCL", "9WZDNCRFHV60",
        "9MZZWGTF7V65", "9NRBW3FJSJWT", "9P743P6195Q5", "9N74KFV7253H", "9N8RS7CW8CGL",
        "9N8RKWJT32L4", "9N9K5QP9G3GL", "9NMPVJ7TCFD0", "9N4NKMXDZCX6", "9MW4MFG0192L",
        "9PBG4MTM3JJC", "9PKFL1XJT73S", "9NGP0DJVKSFL", "9PGSDFM3JD85", "9MVNCSJ14754",
        "9NRQVQ7ZRM7Z", "9PNRLJNDPP4G", "9NK039GWVTVC", "9P7S9F02293J", "9PPH71DV44T7",
        "9N4ZMKC3GX4M", "9PKZRN4RJRC9", "9NQVHHW88XW9", "9P861V1542BL", "9P2FF14JZLL3",
        "9P55XDLRJDHB", "9PPF72ZH6RP8", "9N72DFC6GL91", "9NFB47H2SDRX", "9P5QV5XX8S9H",
        "9N4PT8HGCDHQ", "9NCJWHHMVHR0", "9N8NJ74FZTG9", "9NB6BVL3MTG4", "9PM3J18QM4Q3",
        "9MZ1RZTJTZKB", "9N1N0NQNGK9W", "9N5K7HWXL4Q6", "9N944N1WP67T", "9PLH7R84GJWP",
        "9PJ6FV3130QZ", "9NXSNQR7CM1Q", "9PK0DZ68LGC0", "9N7358JZPX1Z", "9PK8PHLCQDF6",
        "9N79TQTZTQQ4", "9P1H8K2V0NNP", "9PCJT6TLCGGV", "9PL89B9HCZ9W", "9P402RWR63H4",
        "9P9GX5D8H9DW"
    )

    // Danh sách 55 ProductId chính thức từ SIGL "Phổ biến nhất trên đám mây" (CloudMostPopular: 6a589fa0-d493-472b-8e20-3813699d7056)
    private val OFFICIAL_POPULAR_PRODUCT_IDS = setOf(
        "BT5P2X999VH2", "9N8KMNW6942X", "9NNFG8BQRCXL", "9MVXMVT8ZKWC", "C125W9BG2K0V",
        "9N431PX143P8", "9N4PT8HGCDHQ", "C1C4DZJPBC2V", "9PB3RHWXB9MR", "9NVD16NP4J8T",
        "BPQZT43FWD49", "C0N22P73QZ60", "9P2FF14JZLL3", "9NLKS78C4F3Z", "9NKX70BBCDRN",
        "BX1DTCBD1HXJ", "9MZ1RZTJTZKB", "9N1N0NQNGK9W", "9P9FTXPKQ35P", "9PGSDFM3JD85",
        "9N3D82TFF6C4", "9N8PMW7QMD3D", "BX3M8L83BBRW", "9NDF1F263RZ4", "9P3PT7PQJD0M",
        "BXBJQ1932138", "BQ1W1T1FC14W", "9P22THR57FV7", "9PMFDKG5F9R7", "BSR9NLHVF1KL",
        "C3KLDKZBHNCZ", "9NMCDQ6RCJKH", "9NH8K4B707H0", "C0GWTPD0S8S1", "C3B1V55CDL0C",
        "9NMKF7LPVX44", "C08JXNK0VG5L", "9PP5G1F0C2B6", "BPS3XF74B9V1", "9N872NBW5XG5",
        "9NS86BQ33SPX", "9PJVJ4WF031H", "9MT5NJ5W7B8Z", "9NK4NTBFWW81", "BNKDKQXMXRR2",
        "9NG07QJNK38J", "C37XBX7DCBZ0", "9NBR63PT4FZC", "BS6WJ2L56B10", "9NKV34XDW014",
        "9PJ6FV3130QZ", "9PK8PHLCQDF6", "C3D891Z6TNQM", "9PGD71CMDS0Z", "9PGRV33VMSL1"
    )

    // 8 Tập hợp ProductId thể loại chính thức từ trang play.xbox.com (GenresRow)
    private val OFFICIAL_INDIE_IDS = setOf(
        "9MSVBF0KZFVW", "9MSVVM5NS9L6", "9MT19Q3CRMQZ", "9MVNCSJ14754", "9MW15L8FRT9P", "9MW4MFG0192L", "9MW79BJVF3QX", "9MW9469V91LM", "9MWXF3SRD7QN", "9MXHL3TT792F",
        "9MZN3SMXN824", "9MZNS9NZ97PF", "9MZZWGTF7V65", "9N020KFQG301", "9N072VV7MFK7", "9N0CCPMBLJQ8", "9N0P25WJHM8C", "9N0WDPMXNHWN", "9N116V0599HB", "9N16RZ0S50V2",
        "9N18L56XHK8Z", "9N2PS831S84N", "9N36BLQF3FX6", "9N3DKKRHSJBT", "9N3F1LW9HC6C", "9N3KQH8183GD", "9N4ZMKC3GX4M", "9N51NR4GX891", "9N5K7HWXL4Q6", "9N5PKH0KW2L1",
        "9N5VHX8KMVNV", "9N66JJQFX3RX", "9N72DFC6GL91", "9N79TQTZTQQ4", "9N7CJX93ZGWN", "9N7MVHP2R7VP", "9N7P8GVMF6SS", "9N8BRH7PGJMJ", "9N8H4RRSRV2S", "9N9K5QP9G3GL",
        "9NBFGKQLMV33", "9NBLGGH43KZB", "9NCDMPCPDTQF", "9ND19JDMBS3R", "9ND3HTTQ0GKB", "9NDJX252Q5BW", "9NF5S7MLM8XT", "9NF6XPSBTTGB", "9NFB47H2SDRX", "9NG07QJNK38J",
        "9NGCFDHR2MV3", "9NGH1FK0RJGL", "9NGLST31DG26", "9NH019ZFFHTV", "9NH0MSG29X6L", "9NHCT85MKHCV", "9NHKL695M9F2", "9NJ67TQQ51Z0", "9NJC29JG70LM", "9NJJVQZ3BVPX",
        "9NJRSC0B0K88", "9NK039GWVTVC", "9NKHKBQJSSC3", "9NKV34XDW014", "9NL4714VTLRS", "9NLRT31Z4RWM", "9NM4WQ7TTH3J", "9NM6NMBJ7ZK4", "9NMB4T25W817", "9NNHK7K8RV9C",
        "9NNKJNRKFT0G", "9NP86K57SQM3", "9NQ6Z171XZX8", "9NQMVR9WL9FW", "9NR0GCBSLDW4", "9NR6NJ401NBS", "9NRBW3FJSJWT", "9NRH78B682L8", "9NRHQLZX3900", "9NS86BQ33SPX",
        "9NSFGM8J6MBJ", "9NT3WC6RKBVQ", "9NTRS771L8HL", "9NTXGKBW2HSM", "9NTZH69PZSH0", "9NV8W1KJSQLL", "9NVV0M74W05C", "9NWWL1CBWFCG", "9NXSNQR7CM1Q", "9NXVPTB0FXSZ",
        "9NZG72SH3H4W", "9NZPKBFF0CV9", "9P008L2LS87F", "9P1H8K2V0NNP", "9P1P5Q3BNM7J", "9P2HHT3VS7MP", "9P30BFHZ1BKN", "9P43RGDPJ62T", "9P45GGDTFNSM", "9P47S7NJGWZL",
        "9P4NV23Q6QC2", "9P59J86GHDGL", "9P5B8CPQ9S7F", "9P5QV5XX8S9H", "9P66TM1HLGLX", "9P6X0VL8BXZF", "9P743P6195Q5", "9P7S9F02293J", "9P81K2C8RBF4", "9P86LLDCL5CC",
        "9P8DL6W0JBB8", "9P8LR42PTRGJ", "9P8NKVVB0XQJ", "9P8NSTDMZ11P", "9P8V7HR160B4", "9P8X6TVW9ZW8", "9P8XJRLCLH2P", "9P8XKV2D5DZ5", "9P9FJCK32PMT", "9P9GX5D8H9DW",
        "9P9VNSQK1W3K", "9PB86W3JK8Z5", "9PC4RWP34M2D", "9PCJT6TLCGGV", "9PD5BM2Z8C4L", "9PD9SZGNCMDZ", "9PDC8J1BSV8B", "9PDCN8Z4HKRN", "9PDDP6ML6XHF", "9PDKDBQGG2N2",
        "9PF432CVQBXT", "9PFNXM9G4N83", "9PFRTPRQ569Q", "9PFWZMT737GG", "9PGFSLP8GWQK", "9PHK9D8CLQCG", "9PJD2KMX7TZ6", "9PK087LNGJC5", "9PKZRN4RJRC9", "9PL89B9HCZ9W",
        "9PMHGHXLGLJM", "9PNRLJNDPP4G", "9PPF72ZH6RP8", "9PPH71DV44T7", "9PPKR9N31BW6", "9PPP5344B3LS", "9PPR10S4JNLV", "BNBWFH89FCF1", "BPQZT43FWD49", "BPR2TBS2KMQJ",
        "BQ9T0JF0D3L4", "BQND4NQXFFV2", "BQVQTL3PCH05", "BRJNRZ9N734V", "BSLTQT4L0RLV", "BSMZH25V6V46", "BTQMHK78ZPF9", "BVJLKDG2TX8H", "BWT2GPVWCH2F", "BX1DTCBD1HXJ",
        "BXZWT67JBZJH", "C042M8GWKS34", "C17GQF31D617", "C37XBX7DCBZ0", "C3D891Z6TNQM", "C3LW50BQJ878", "C41ZDFQ82M1G", "C521HDXRTS7F"
    )

    private val OFFICIAL_FAMILY_IDS = setOf(
        "9MZ4GBWX9GND", "9MZN3SMXN824", "9N16RZ0S50V2", "9N21ZCV0JW6V", "9N2J0FGV30DX", "9N36BLQF3FX6", "9N3DKKRHSJBT", "9N431PX143P8", "9N436FQM30HX", "9N4NKMXDZCX6",
        "9N51NR4GX891", "9N5DDTDLTWCL", "9N5K7HWXL4Q6", "9N6JF50HZFW9", "9N6Z8DQXSQWH", "9N74KFV7253H", "9N7MVHP2R7VP", "9N8BRH7PGJMJ", "9N8CD0XZKLP4", "9N8RS7CW8CGL",
        "9N93V5M3TG4N", "9N9PDJ7Z8S2R", "9NBFGKQLMV33", "9NBLGGH43KZB", "9NBLHDB0WLHZ", "9NCJWHHMVHR0", "9ND3HTTQ0GKB", "9NDC5LJS839S", "9NDJLXD2X2DM", "9NF5S7MLM8XT",
        "9NFQ1XMXDLN6", "9NGLST31DG26", "9NH019ZFFHTV", "9NJRSC0B0K88", "9NK13T4QFPQW", "9NKBDN695GTT", "9NKX70BBCDRN", "9NM6NMBJ7ZK4", "9NMCDQ6RCJKH", "9NMF3KXT4P4G",
        "9NNPSTWT06VM", "9NP6V9LFVJ87", "9NQVHHW88XW9", "9NRBW3FJSJWT", "9NS86BQ33SPX", "9NSDM7MBGG8L", "9NSF0BGH8D86", "9NVD16NP4J8T", "9NXVC0482QS5", "9NXVPTB0FXSZ",
        "9P0RGJND5SR0", "9P1H8K2V0NNP", "9P2VGCTBMM52", "9P45GGDTFNSM", "9P6X0VL8BXZF", "9P7S9F02293J", "9P822XLW9PB8", "9P86LLDCL5CC", "9P88K6NQW12H", "9P8H220G3S4P",
        "9P9FTXPKQ35P", "9PDKDBQGG2N2", "9PF2ZS4XB4JH", "9PJ06TZX4NMH", "9PJRQHMCFQGS", "9PLF73SS7M6J", "9PLKVSWR299F", "9PLMGFWCFZ9G", "9PLVVTDHS1P0", "9PM3J18QM4Q3",
        "9PMQDM08SNK9", "9PMX3SK2RMG4", "9PNRSC0J6DT8", "9WZDNCRFHV60", "BNCZHKWRZ7BR", "BNSDHDK45KBR", "BRG51C5MWFSG", "BRRVN5198461", "BS1NPTPJGD4G", "BSJG7TTSWVJ2",
        "BSMZH25V6V46", "BSXSL6WBJ0V7", "BVCRSR6XSDW5", "BVJLKDG2TX8H", "BW85KQB8Q31M", "BX27S00SKW1V", "C1ZWH2BZ9TSF", "C2P985H1H42H", "C37XBX7DCBZ0", "C3SQQG5Q6Z64",
        "C4GH8N6ZXG5L", "C4VKLMG1HLZW"
    )

    private val OFFICIAL_SHOOTER_IDS = setOf(
        "9MT8PTGVHX2P", "9MTGCZ84M503", "9MW715J1P29D", "9MWXF3SRD7QN", "9MXHL3TT792F", "9MXND4PQLK3W", "9MZ1RZTJTZKB", "9N11JM7F5CXK", "9N1N0NQNGK9W", "9N4PT8HGCDHQ",
        "9N51NR4GX891", "9N5PKH0KW2L1", "9N6639H7VGH4", "9N6J4RNFPKKL", "9N8KMNW6942X", "9N9K5QP9G3GL", "9N9PCZWHVP2L", "9NBLGGH4PBBM", "9NC7NRD0CM11", "9NCDMPCPDTQF",
        "9NF6XPSBTTGB", "9NHKL695M9F2", "9NKHKBQJSSC3", "9NL4714VTLRS", "9NLB6V0GNC9P", "9NLZZ74TFD7P", "9NM50L0DXX1W", "9NNFG8BQRCXL", "9NP1P1WFS0LB", "9NQGN8TGNT8P",
        "9NQMVR9WL9FW", "9NQQ8B4PJR25", "9NRH78B682L8", "9NSD4HTWM8HT", "9NSL68D814GC", "9NT3WC6RKBVQ", "9NVJ4L71DF5Q", "9NVQBQ3F6W9W", "9NZP7NKR2QWG", "9P0T51BDDWVT",
        "9P1Z43KRNQD4", "9P2FF14JZLL3", "9P43RGDPJ62T", "9P45GGDTFNSM", "9P46M5JQ2PTF", "9P47S7NJGWZL", "9P4D0K92BM7V", "9P4KMR76PLLQ", "9P4NV23Q6QC2", "9P55XDLRJDHB",
        "9P59J86GHDGL", "9P5B8CPQ9S7F", "9P5QV5XX8S9H", "9P5S26314HWQ", "9P5Z4530318L", "9P7L9H478GGV", "9P81K2C8RBF4", "9P8FBXKMCLWC", "9P8JRMWRQP4H", "9P8NKVVB0XQJ",
        "9P8PS6993LSL", "9P9278ZMGQLJ", "9P9FJCK32PMT", "9P9VNSQK1W3K", "9PB3RHWXB9MR", "9PB86W3JK8Z5", "9PBG4MTM3JJC", "9PBNGCVBGML0", "9PDCN8Z4HKRN", "9PF432CVQBXT",
        "9PGD71CMDS0Z", "9PH9X0760B0T", "9PJBKF27C16M", "9PKF2LDFPXTN", "9PKWHT7G60WQ", "9PLBH9J0XGR8", "9PLCL2VB2SC0", "9PLSCHRN5715", "9PNGFSX7S117", "BNRH7BRC1D02",
        "BP15SF17LH13", "BP73LMGB1DFD", "BPKDQSSFQ9WV", "BPL68T0XK96W", "BPN08ZDRPSFK", "BPNDKQN84N6L", "BPR2TBS2KMQJ", "BPRPQSKXTD1L", "BPV4GXTDCNSH", "BQMVWCMB8P59",
        "BQT21VXFS52F", "BR27BSZ2M3SR", "BR7X7MVBBQKM", "BRGPD72KHM3Q", "BRJLJ6RPLQTJ", "BRRC2BP0G9P0", "BRRVN5198461", "BRZHFR5XBLQS", "BS6WJ2L56B10", "BSHMMGRP84N4",
        "BSR9NLHVF1KL", "BSXDD75Q561F", "BSXLFN5QQZSC", "BT5P2X999VH2", "BT9FFLG51VVG", "BTJ0T8C04ZBV", "BTKN78H28RPV", "BV0K9LMLQ9W5", "BV2ZVP7PJZWL", "BV6BRSLFJW3W",
        "BV9CWVQWNS4P", "BVFDTJ1XF6CS", "BVQ3V30ZXVT5", "BX1DTCBD1HXJ", "BX3JNK07Z6QK", "BXBJQ1932138", "BXRB4ZH2GJHK", "BXZWT67JBZJH", "BZ2N7TQ0XCF2", "BZ6W9LRPC26W",
        "BZF7N4FQWHNR", "BZPR04V49BMH", "C0GWTPD0S8S1", "C0KJ40T9QD86", "C0SWGV4560W1", "C1DDW4PRF6K5", "C1SDBNRFXT1D", "C20WW4W29FQ1", "C29HQ887KH4B", "C2DCJ95ZXBMS",
        "C2M8HBNVPT1T", "C2N9CS4FS1QR", "C2PB7STLJGS0", "C3KLDKZBHNCZ", "C3LW50BQJ878", "C3QH42WRGM3R", "C3VVNLQQ54F8", "C40860J5R2MP", "C421ZX7RCG0W", "C4HZC7LJG6PX",
        "C4LLMHFQ1BXQ", "C57L9GR0HHB7"
    )

    private val OFFICIAL_FIGHTING_IDS = setOf(
        "9N2HB4MWG7HM", "9N7271QN4SGB", "9N76QGDS5GJR", "9N79TQTZTQQ4", "9N7CJX93ZGWN", "9N7GCF5SGCXC", "9N944N1WP67T", "9NH8K4B707H0", "9NQ6Z171XZX8", "9NSM674QVLBS",
        "9NX6XGVJ0J3D", "9P8SQQJ5T94B", "9PC4RWP34M2D", "9PKZRN4RJRC9", "9PN0LC4CNLPT", "BPQZT43FWD49", "C3B1V55CDL0C", "C4F7ZHLN6B2J"
    )

    private val OFFICIAL_STRATEGY_IDS = setOf(
        "9MVN4ND41DD3", "9MX7DMHK13SC", "9MZNS9NZ97PF", "9N034X5G08B7", "9N255K81XBD3", "9N3DWR830CVN", "9N42SSSX2MTG", "9N4ZMKC3GX4M", "9N6PHQ93Z451", "9N7V5R3VLMJZ",
        "9NBZ7Z3JRXQ8", "9NF0D13RPX5L", "9NG07QJNK38J", "9NGH1FK0RJGL", "9NGP0DJVKSFL", "9NJC29JG70LM", "9NMPVJ7TCFD0", "9NN3HCKW5TPC", "9NPGPDCXWJQ7", "9NPKGVXPFRSX",
        "9NT4X7P8B9NB", "9NTXGKBW2HSM", "9NVC9KWJVLT0", "9NVRJS95FLM9", "9NWWL1CBWFCG", "9NXVPTB0FXSZ", "9P1P5Q3BNM7J", "9P2HHT3VS7MP", "9P30BFHZ1BKN", "9P5B8CPQ9S7F",
        "9P88K6NQW12H", "9P8NSTDMZ11P", "9P8V7HR160B4", "9P9GX5D8H9DW", "9PB3RHWXB9MR", "9PBG4MTM3JJC", "9PC4RWP34M2D", "9PGSDFM3JD85", "9PHH1ZWRSZ41", "9PHKWCSZ0SQJ",
        "9PJ06TZX4NMH", "9PJBKF27C16M", "9PJD2KMX7TZ6", "9PK087LNGJC5", "9PK0DZ68LGC0", "9PKJTP6DTND3", "9PKWHT7G60WQ", "9PPH71DV44T7", "BPRPQSKXTD1L", "BQ4M1XWXFKHB",
        "BQ9T0JF0D3L4", "BQND4NQXFFV2", "BR46KM4D5B9L", "BS34VNW7H61F", "BWT2GPVWCH2F", "BXBJQ1932138", "C2484ZR2BWNQ", "C2N9CS4FS1QR", "C3D891Z6TNQM", "C41ZDFQ82M1G",
        "C42KCJCLX6MX", "C4CBF3FHHCND", "C4HZC7LJG6PX", "C521HDXRTS7F", "C5DZFZ6NC5SQ", "C5JGRR1LFVW8"
    )

    private val OFFICIAL_SIMULATION_IDS = setOf(
        "9MT19Q3CRMQZ", "9MT5KSV3RCWD", "9MXHL3TT792F", "9MZ4GBWX9GND", "9N034X5G08B7", "9N0CCPMBLJQ8", "9N0WDPMXNHWN", "9N2PS831S84N", "9N3DKKRHSJBT", "9N436FQM30HX",
        "9N4J937WLFPZ", "9N4NKMXDZCX6", "9N4ZMKC3GX4M", "9N51NR4GX891", "9N5DDTDLTWCL", "9N74KFV7253H", "9N8BRH7PGJMJ", "9N8RKWJT32L4", "9N8RS7CW8CGL", "9N93V5M3TG4N",
        "9NBFGKQLMV33", "9NBLGGH43KZB", "9NBZ7Z3JRXQ8", "9NCT9L7NFHX7", "9ND19JDMBS3R", "9ND3HTTQ0GKB", "9NDC5LJS839S", "9NDJX252Q5BW", "9NF5S7MLM8XT", "9NGLST31DG26",
        "9NH019ZFFHTV", "9NH8K4B707H0", "9NHKL695M9F2", "9NLFZVMSQ7CC", "9NMB4T25W817", "9NMCDQ6RCJKH", "9NMF3KXT4P4G", "9NNPSTWT06VM", "9NP6V9LFVJ87", "9NR0GCBSLDW4",
        "9NRBW3FJSJWT", "9NRHQLZX3900", "9NS86BQ33SPX", "9NSF0BGH8D86", "9NSFGM8J6MBJ", "9NT4X7P8B9NB", "9NTXGKBW2HSM", "9NVV0M74W05C", "9NWPW7F19FS5", "9NX7HCWL13Z9",
        "9NZQPT0MWTD0", "9P0NPLQ9P1DB", "9P1P5Q3BNM7J", "9P2VGCTBMM52", "9P45GGDTFNSM", "9P822XLW9PB8", "9P86LLDCL5CC", "9P8XJRLCLH2P", "9P9446P7D9TZ", "9P9FJCK32PMT",
        "9P9GX5D8H9DW", "9PB86W3JK8Z5", "9PC1KTWW5PB5", "9PC4RWP34M2D", "9PDC8J1BSV8B", "9PDDP6ML6XHF", "9PF432CVQBXT", "9PFNXM9G4N83", "9PJ06TZX4NMH", "9PJRQHMCFQGS",
        "9PK5WS0HXQKQ", "9PLFD17M47K3", "9PLKVSWR299F", "9PLVVTDHS1P0", "9PMQDM08SNK9", "9PNRSC0J6DT8", "9PPH71DV44T7", "BNKDKQXMXRR2", "BPRPQSKXTD1L", "BQ4M1XWXFKHB",
        "BQVQTL3PCH05", "BR46KM4D5B9L", "BRJNRZ9N734V", "BTNPS60N3114", "BV9CWVQWNS4P", "BVJLKDG2TX8H", "BWT2GPVWCH2F", "BX1DTCBD1HXJ", "BXBJQ1932138", "C2484ZR2BWNQ",
        "C2Q32JM0BPZL", "C350VBGGCL6P", "C3D891Z6TNQM", "C3LW50BQJ878", "C3QWNCV55VLL", "C41ZDFQ82M1G", "C4GH8N6ZXG5L", "C5DZFZ6NC5SQ", "C5JGRR1LFVW8"
    )

    private val OFFICIAL_RPG_IDS = setOf(
        "9MSVPJCHHRPR", "9MT5NJ5W7B8Z", "9MVN4ND41DD3", "9MZNS9NZ97PF", "9N255K81XBD3", "9N2PS831S84N", "9N3DWR830CVN", "9N3KQH8183GD", "9N4W31HSMVVD", "9N5JRWWGMS1R",
        "9N5PKH0KW2L1", "9N5VHX8KMVNV", "9N6PHQ93Z451", "9N732VFPMWKT", "9N76QGDS5GJR", "9N7TFFRRZCC9", "9N7V5R3VLMJZ", "9N8C07SS8C9D", "9N8NJ74FZTG9", "9N9C8SV0S16W",
        "9N9X9PC8V9CP", "9NB6BVL3MTG4", "9NCJSXWZTP88", "9NCNBWZSHZR5", "9NDJX252Q5BW", "9NGH1FK0RJGL", "9NJRSC0B0K88", "9NJWFF2KHL6H", "9NK4NTBFWW81", "9NKFS43DLFFL",
        "9NKV34XDW014", "9NLB6V0GNC9P", "9NMNMS626R59", "9NPRHD8GQKQS", "9NQGN8TGNT8P", "9NQMVR9WL9FW", "9NQR437K7PQH", "9NRQVQ7ZRM7Z", "9NS86BQ33SPX", "9NSF0BGH8D86",
        "9NT0PNTL6B79", "9NT3WC6RKBVQ", "9NT4X7P8B9NB", "9NTRS771L8HL", "9NVC9KWJVLT0", "9NVRJS95FLM9", "9NWDPFPPK30V", "9NXSNQR7CM1Q", "9NZDHXL9SJJ8", "9P1P5Q3BNM7J",
        "9P20JCF7BV93", "9P30BFHZ1BKN", "9P33VJGVPHVP", "9P402RWR63H4", "9P41GS5W1C6L", "9P77PG7DK0W2", "9P7SL78VHVMF", "9P7TLX79S7N7", "9P81K2C8RBF4", "9P822XLW9PB8",
        "9P861V1542BL", "9P86LLDCL5CC", "9P8DL6W0JBB8", "9P8LR42PTRGJ", "9P8RMKXRML7D", "9P8S1MN4066K", "9P8XJRLCLH2P", "9P9278ZMGQLJ", "9P9VNSQK1W3K", "9PD9SZGNCMDZ",
        "9PDC9X7BKZR3", "9PDDP6ML6XHF", "9PJD2KMX7TZ6", "9PJTHRNVH62H", "9PK0DZ68LGC0", "9PK8PHLCQDF6", "9PKF2LDFPXTN", "9PKJTP6DTND3", "9PKWHT7G60WQ", "9PLH7R84GJWP",
        "9PM3J18QM4Q3", "9PN3VDFTB5HZ", "9PPR10S4JNLV", "BNBWFH89FCF1", "BP73LMGB1DFD", "BQ1W1T1FC14W", "BQ9Q620NC614", "BQ9T0JF0D3L4", "BQND4NQXFFV2", "BQVQTL3PCH05",
        "BR27BSZ2M3SR", "BR46KM4D5B9L", "BR74RLMH966K", "BRGPD72KHM3Q", "BRJLJ6RPLQTJ", "BRKX5CRMRTC2", "BS34VNW7H61F", "BS6WJ2L56B10", "BS9SX4Q6XJRF", "BSDX4FNNC0DK",
        "BSR9NLHVF1KL", "BSZM480TSWGP", "BTNPS60N3114", "BVQ3V30ZXVT5", "BVTKN6CQ8W5F", "BW9TWC8L4JCS", "BX3JNK07Z6QK", "BXVCFBJBNS17", "BZ92FB6PWXFJ", "BZGJRJC1FGF3",
        "BZPR04V49BMH", "C042M8GWKS34", "C0QDQ0KX4HDV", "C29HQ887KH4B", "C2B4T86TXLRS", "C2Q32JM0BPZL", "C2WKJJ9F5936", "C3D891Z6TNQM", "C3KLDKZBHNCZ", "C3L4J3H7H12R",
        "C3LW50BQJ878", "C47GZZBMR5WG", "C4CBF3FHHCND", "C4F7ZHLN6B2J", "C4G76555T4QN", "C4VLMWJWM7BG", "C521HDXRTS7F", "C5DZFZ6NC5SQ", "C5JGRR1LFVW8", "C5K89TFLSV19"
    )

    private val ACCENT_PALETTE = longArrayOf(
        0xFF107C10L, 0xFF0074E4L, 0xFFFF007AL, 0xFF00B4D8L,
        0xFF38B000L, 0xFF7209B7L, 0xFFF77F00L, 0xFF2D6A4FL
    )
    private val SLUG_NON_ALNUM_REGEX = Regex("[^a-z0-9]+")
    private val XBOX_CLOUD_SUFFIX_REGEX = Regex("\\s*[•|\\-–—]\\s*Xbox Cloud Gaming.*", RegexOption.IGNORE_CASE)
    private val XBOX_PIPE_SUFFIX_REGEX = Regex("\\s*\\|?\\s*Xbox.*", RegexOption.IGNORE_CASE)
    private val XBOX_BULLET_SUFFIX_REGEX = Regex("\\s*[•|\\-–—]\\s*Xbox.*", RegexOption.IGNORE_CASE)
    private val WHITESPACE_COLLAPSE_REGEX = Regex("\\s+")

    @Volatile
    private var prefs: SharedPreferences? = null
    @Volatile
    private var recentPrefs: SharedPreferences? = null
    @Volatile
    private var syncedGamesCacheFile: java.io.File? = null
    private val cacheFileLock = Any()
    @Volatile
    private var userModifiedFavoritesEarly = false
    @Volatile
    private var userModifiedLastPlayedEarly = false
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var cachedAllCloudProductIds: List<String> = emptyList()
    @Volatile
    private var cachedFreeToPlayIds: Set<String> = OFFICIAL_FREE_TO_PLAY_PRODUCT_IDS
    @Volatile
    private var cachedTouchIds: Set<String> = OFFICIAL_TOUCH_PRODUCT_IDS
    @Volatile
    private var cachedPopularIds: Set<String> = OFFICIAL_POPULAR_PRODUCT_IDS
    @Volatile
    private var cachedRecentIds: Set<String> = OFFICIAL_RECENTLY_ADDED_PRODUCT_IDS
    @Volatile
    private var cachedIndieIds: Set<String> = OFFICIAL_INDIE_IDS
    @Volatile
    private var cachedFamilyIds: Set<String> = OFFICIAL_FAMILY_IDS
    @Volatile
    private var cachedShooterIds: Set<String> = OFFICIAL_SHOOTER_IDS
    @Volatile
    private var cachedFightingIds: Set<String> = OFFICIAL_FIGHTING_IDS
    @Volatile
    private var cachedStrategyIds: Set<String> = OFFICIAL_STRATEGY_IDS
    @Volatile
    private var cachedSimulationIds: Set<String> = OFFICIAL_SIMULATION_IDS
    @Volatile
    private var cachedRpgIds: Set<String> = OFFICIAL_RPG_IDS

    private val _games = MutableStateFlow(INITIAL_XBOX_GAMES)
    val games: StateFlow<List<GameBookmark>> = _games.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _lastPlayedId = MutableStateFlow<String?>(null)
    val lastPlayedId: StateFlow<String?> = _lastPlayedId.asStateFlow()

    private val _playedGameIds = MutableStateFlow<List<String>>(emptyList())
    val playedGameIds: StateFlow<List<String>> = _playedGameIds.asStateFlow()

    private val _isSyncingCatalog = MutableStateFlow(false)
    val isSyncingCatalog: StateFlow<Boolean> = _isSyncingCatalog.asStateFlow()

    private val _isLoadingInitialCatalog = MutableStateFlow(false)
    val isLoadingInitialCatalog: StateFlow<Boolean> = _isLoadingInitialCatalog.asStateFlow()

    private val _isSearchingLiveXbox = MutableStateFlow(false)
    val isSearchingLiveXbox: StateFlow<Boolean> = _isSearchingLiveXbox.asStateFlow()

    private val _xboxStatus = MutableStateFlow(XboxCloudSyncStatus())
    val xboxStatus: StateFlow<XboxCloudSyncStatus> = _xboxStatus.asStateFlow()

    /**
     * Initializes persistent storage for custom user-added games, auto-synced Xbox Cloud catalog games,
     * favorites, Xbox profile status, and last played game via SharedPreferences.
     */
    @Volatile
    private var initStarted = false
    @Volatile
    private var initialDiskLoadCompleted = false

    fun init(context: Context) {
        if (!initStarted) {
            initStarted = true
            val appCtx = context.applicationContext
            syncedGamesCacheFile = java.io.File(appCtx.filesDir, SYNCED_GAMES_CACHE_FILE_NAME)
            // Gán tham chiếu SharedPreferences ngay lập tức (<0.2ms, không chờ nạp XML) để các hàm gọi sớm
            // như toggleFavorite / recordLastPlayed luôn có đối tượng prefs hợp lệ.
            val sharedPrefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val legacyRecentPrefs = appCtx.getSharedPreferences(RECENT_PREFS_NAME, Context.MODE_PRIVATE)
            prefs = sharedPrefs
            recentPrefs = legacyRecentPrefs
            _isLoadingInitialCatalog.value = true
            ioScope.launch {
                // Khởi tạo sẵn Coil ImageLoader trên luồng IO để lần gọi AsyncImage đầu tiên trên UI không bị chặn
                try {
                    coil.Coil.imageLoader(appCtx)
                } catch (_: Throwable) {
                }

                val t0 = android.os.SystemClock.elapsedRealtime()
                // Chờ SharedPreferencesImpl nạp xong file XML trên luồng IO thay vì chặn luồng chính
                val savedFavs = sharedPrefs.getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
                val tAfterPrefsAwait = android.os.SystemClock.elapsedRealtime()
                if (!userModifiedFavoritesEarly) {
                    _favoriteIds.value = savedFavs.toSet()
                }

                val savedLast = sharedPrefs.getString(KEY_LAST_PLAYED_ID, null)
                    ?: legacyRecentPrefs.getString(KEY_LAST_PLAYED_ID, null)
                val savedPlayedRaw = sharedPrefs.getString(KEY_PLAYED_GAME_IDS_LIST, "") ?: ""
                val savedPlayedList = savedPlayedRaw
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                if (!userModifiedLastPlayedEarly) {
                    if (_lastPlayedId.value == null) {
                        _lastPlayedId.value = savedPlayedList.firstOrNull() ?: savedLast
                    }
                    if (_playedGameIds.value.isEmpty()) {
                        _playedGameIds.value = if (savedPlayedList.isNotEmpty()) {
                            savedPlayedList
                        } else if (!savedLast.isNullOrBlank()) {
                            listOf(savedLast)
                        } else {
                            emptyList()
                        }
                    }
                }

                val lastSyncMs = sharedPrefs.getLong(KEY_LAST_CATALOG_SYNC_MS, 0L)
                val savedGamertag = sharedPrefs.getString(KEY_XBOX_GAMERTAG, null)?.takeIf { it.isNotBlank() }
                val savedTier = sharedPrefs.getString(KEY_XBOX_SUB_TIER, null)?.takeIf { it.isNotBlank() }
                val savedTotalCloud = sharedPrefs.getInt(KEY_XBOX_TOTAL_CLOUD, 0)
                val savedJumpBackRaw = sharedPrefs.getString(KEY_XBOX_JUMP_BACK_IDS, "") ?: ""
                val savedJumpBack = savedJumpBackRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val savedAvatar = sharedPrefs.getString(KEY_XBOX_AVATAR_URL, null)?.takeIf { it.isNotBlank() }

                _xboxStatus.value = XboxCloudSyncStatus(
                    gamertag = savedGamertag,
                    avatarUrl = savedAvatar,
                    subscriptionTier = savedTier,
                    cloudServiceOnline = true,
                    totalCloudCatalogCount = savedTotalCloud.coerceAtLeast(_games.value.size),
                    syncedCatalogCount = _games.value.size,
                    jumpBackInIds = savedJumpBack,
                    lastSyncTimeMs = lastSyncMs
                )

                val customList: List<GameBookmark>
                val syncedList: List<GameBookmark>
                try {
                    val tParseStart = android.os.SystemClock.elapsedRealtime()
                    val earlyDiscovered = _games.value.filter { !it.id.startsWith("custom_") }
                    customList = loadGamesArrayFromPrefs(sharedPrefs, KEY_CUSTOM_GAMES_JSON)
                    val diskSynced = loadSyncedGamesFromFileOrMigratePrefs(sharedPrefs)
                    syncedList = (diskSynced + earlyDiscovered)
                        .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                    val tParseEnd = android.os.SystemClock.elapsedRealtime()
                    rebuildMergedGames(customList, syncedList)
                    initialDiskLoadCompleted = true
                    val tMergeEnd = android.os.SystemClock.elapsedRealtime()
                    PerformanceMonitor.recordInitMilestone(
                        "GameLibraryRepository (IO): xmlLoad=${tAfterPrefsAwait - t0}ms, jsonParse=${tParseEnd - tParseStart}ms, merge=${tMergeEnd - tParseEnd}ms (tổng=${_games.value.size} game)"
                    )
                } finally {
                    initialDiskLoadCompleted = true
                    _isLoadingInitialCatalog.value = false
                }

                val now = System.currentTimeMillis()
                val missingCoverArt = syncedList.any { it.imageUrl.isNullOrBlank() }
                val targetMinSynced = (savedTotalCloud.coerceAtMost(550) - 20).coerceAtLeast(0)
                val isCatalogTooSmall = syncedList.size < 120
                val needsAutoFullScan = syncedList.isEmpty() ||
                    missingCoverArt ||
                    isCatalogTooSmall ||
                    (targetMinSynced > 0 && syncedList.size < targetMinSynced) ||
                    (now - lastSyncMs) > 6 * 3600 * 1000L
                if (needsAutoFullScan) {
                    kotlinx.coroutines.delay(if (isCatalogTooSmall) 300L else 3_500L)
                    if (!isStreamActive && !com.example.vpn.VpnManager.isStreamActive()) {
                        syncLiveXboxCatalog(
                            force = missingCoverArt || isCatalogTooSmall,
                            maxCatalogGames = 600
                        )
                    }
                }
            }
        }
    }

    private fun rebuildMergedGames(
        customList: List<GameBookmark>,
        syncedList: List<GameBookmark>
    ) {
        val existingIds = HashSet<String>((customList.size + syncedList.size) * 2)
        val existingProductIds = HashSet<String>((customList.size + syncedList.size) * 2)
        val merged = ArrayList<GameBookmark>(customList.size + syncedList.size)

        fun addUnique(item: GameBookmark) {
            val idKey = item.id.lowercase(Locale.ROOT)
            val isCustom = idKey.startsWith("custom_")
            val pid = extractProductId(item.playUrl)
            val isFortnite = idKey == "fortnite" || pid.equals(FORTNITE_PRODUCT_ID, ignoreCase = true)
            if (existingIds.contains(idKey)) return
            if (pid.isNotBlank() && existingProductIds.contains(pid)) return
            existingIds.add(idKey)
            if (pid.isNotBlank()) existingProductIds.add(pid)

            val trulyFree = isFreeToPlayTitle(item.title, pid, item.playUrl) || (isCustom && item.freeToPlay)
            val trulyTouch = if (isCustom) item.supportsTouch else isTouchSupportedTitle(pid, item.playUrl)
            val trulyRecent = if (isCustom) false else isRecentlyAddedTitle(pid, item.playUrl)
            val trulyPopular = if (isCustom) false else isPopularTitle(pid, item.playUrl)
            val (resolvedCatVi, resolvedCatEn) = if (isCustom) {
                item.category to item.categoryEn
            } else {
                resolveCategoryBilingual(pid, item.category, item.categoryEn)
            }

            val finalId = if (isFortnite) "fortnite" else item.id
            val finalItem = if (
                item.id != finalId ||
                item.drawableResId != null ||
                item.freeToPlay != trulyFree ||
                item.supportsTouch != trulyTouch ||
                item.isRecentlyAdded != trulyRecent ||
                item.isPopular != trulyPopular ||
                item.category != resolvedCatVi ||
                item.categoryEn != resolvedCatEn
            ) {
                item.copy(
                    id = finalId,
                    drawableResId = null,
                    freeToPlay = trulyFree,
                    supportsTouch = trulyTouch,
                    isRecentlyAdded = trulyRecent,
                    isPopular = trulyPopular,
                    category = resolvedCatVi,
                    categoryEn = resolvedCatEn
                )
            } else {
                item
            }
            merged.add(finalItem)
        }

        customList.forEach { addUnique(it) }
        syncedList.forEach { addUnique(it) }

        // Đảm bảo Fortnite luôn có mặt trong danh mục
        if (!existingIds.contains("fortnite") && !existingProductIds.contains(FORTNITE_PRODUCT_ID)) {
            merged.add(
                0,
                GameBookmark(
                    id = "fortnite",
                    title = "Fortnite",
                    category = "Bắn súng • Miễn phí",
                    categoryEn = "Shooter • Free to Play",
                    playUrl = "https://www.xbox.com/en-US/play/games/fortnite/$FORTNITE_PRODUCT_ID",
                    freeToPlay = true,
                    supportsTouch = true,
                    accentColorHex = 0xFF0074E4L,
                    badge = "Cảm ứng Epic • Miễn phí",
                    badgeEn = "Epic Touch • Free",
                    drawableResId = null,
                    rating = "4.9",
                    imageUrl = "https://store-images.s-microsoft.com/image/apps.50670.70702278257994163.1052c4f6-c429-424b-9b56-0303b1420312.69b75902-e543-496f-9464-480b3795328d?w=260&q=70",
                    heroImageUrl = "https://store-images.s-microsoft.com/image/apps.50670.70702278257994163.1052c4f6-c429-424b-9b56-0303b1420312.69b75902-e543-496f-9464-480b3795328d?w=520&q=70",
                    description = "Chơi Fortnite miễn phí trên Xbox Cloud Gaming chỉ cần tài khoản Microsoft, không cần gói Game Pass.",
                    publisher = "Epic Games",
                    isPopular = true,
                    isRecentlyAdded = false
                )
            )
        }

        _games.value = merged
        _xboxStatus.value = _xboxStatus.value.copy(
            syncedCatalogCount = merged.size,
            totalCloudCatalogCount = _xboxStatus.value.totalCloudCatalogCount.coerceAtLeast(merged.size)
        )
    }

    fun extractProductId(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val clean = url.substringBefore("?").substringBefore("#").trimEnd('/')
        // Hỗ trợ cả Giao diện mới (play.xbox.com/products/<productId>/<slug>)
        val afterProducts = clean.substringAfter("/products/", "").trim('/')
        if (afterProducts.isNotBlank()) {
            val firstSeg = afterProducts.substringBefore("/").uppercase(Locale.ROOT)
            if (firstSeg.length in 10..14 && firstSeg.all { it.isLetterOrDigit() }) {
                return firstSeg
            }
        }
        val afterGames = clean.substringAfter("/play/games/", "")
            .ifBlank { clean.substringAfter("/play/launch/", "") }
            .ifBlank { clean.substringAfter("/games/", "") }
            .ifBlank { clean.substringAfter("/launch/", "") }
            .trim('/')
        if (afterGames.isBlank()) return ""
        val segments = afterGames.split("/").filter { it.isNotBlank() }
        for (seg in segments.asReversed()) {
            val candidate = seg.uppercase(Locale.ROOT)
            if (candidate.length == 12 && candidate.all { it.isLetterOrDigit() } && candidate.any { it.isDigit() }) {
                return candidate
            }
        }
        val lastSeg = segments.lastOrNull()?.uppercase(Locale.ROOT).orEmpty()
        if (lastSeg.length in 10..14 && lastSeg.all { it.isLetterOrDigit() } && lastSeg.any { it.isDigit() }) {
            return lastSeg
        }
        return ""
    }

    fun extractSlug(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val clean = url.substringBefore("?").substringBefore("#").trimEnd('/')
        val afterProducts = clean.substringAfter("/products/", "").trim('/')
        if (afterProducts.isNotBlank()) {
            val secondSeg = afterProducts.substringAfter("/", "").substringBefore("/").lowercase(Locale.ROOT).trim()
            if (secondSeg.isNotBlank()) return secondSeg
        }
        val afterGames = clean.substringAfter("/play/games/", "")
            .ifBlank { clean.substringAfter("/play/launch/", "") }
            .ifBlank { clean.substringAfter("/games/", "") }
            .ifBlank { clean.substringAfter("/launch/", "") }
            .trim('/')
        if (afterGames.isBlank()) return ""
        val firstSeg = afterGames.substringBefore("/").lowercase(Locale.ROOT).trim()
        // Nếu segment đầu tiên là ProductId 12 ký tự (ví dụ /launch/BT5P2X999VH2) thì không coi là slug
        if (firstSeg.length == 12 && firstSeg.all { it.isLetterOrDigit() } && firstSeg.any { it.isDigit() }) {
            return ""
        }
        return firstSeg
    }

    private fun slugifyTitle(title: String): String {
        return title.lowercase(Locale.ROOT)
            .replace(SLUG_NON_ALNUM_REGEX, "-")
            .trim('-')
            .ifBlank { "xbox-game" }
    }

    private fun formatSlugToTitle(slug: String): String {
        if (slug.isBlank()) return "Xbox Cloud Game"
        return slug.split("-")
            .filter { it.isNotBlank() && it != "standard" && it != "edition" && it != "xbox" && it != "series" }
            .joinToString(" ") { word ->
                word.replaceFirstChar { ch -> ch.uppercaseChar() }
            }
            .ifBlank { "Xbox Cloud Game" }
    }

    fun toggleFavorite(gameId: String) {
        userModifiedFavoritesEarly = true
        val current = _favoriteIds.value.toMutableSet()
        if (!current.add(gameId)) {
            current.remove(gameId)
        }
        val updated = current.toSet()
        _favoriteIds.value = updated
        prefs?.edit()?.putStringSet(KEY_FAVORITE_IDS, updated)?.apply()
    }

    fun recordLastPlayed(gameId: String) {
        val cleanId = gameId.trim()
        if (cleanId.isBlank()) return
        userModifiedLastPlayedEarly = true
        _lastPlayedId.value = cleanId
        val updatedList = (listOf(cleanId) + _playedGameIds.value.filterNot { it.equals(cleanId, ignoreCase = true) })
            .take(24)
        _playedGameIds.value = updatedList
        prefs?.edit()
            ?.putString(KEY_LAST_PLAYED_ID, cleanId)
            ?.putString(KEY_PLAYED_GAME_IDS_LIST, updatedList.joinToString(","))
            ?.apply()
        recentPrefs?.edit()?.putString(KEY_LAST_PLAYED_ID, cleanId)?.apply()
    }

    fun removePlayedGame(gameId: String) {
        val cleanId = gameId.trim()
        if (cleanId.isBlank()) return
        userModifiedLastPlayedEarly = true
        val updatedList = _playedGameIds.value.filterNot { it.equals(cleanId, ignoreCase = true) }
        _playedGameIds.value = updatedList
        val nextLast = updatedList.firstOrNull()
        _lastPlayedId.value = nextLast
        prefs?.edit()
            ?.putString(KEY_LAST_PLAYED_ID, nextLast)
            ?.putString(KEY_PLAYED_GAME_IDS_LIST, updatedList.joinToString(","))
            ?.apply()
        recentPrefs?.edit()?.putString(KEY_LAST_PLAYED_ID, nextLast)?.apply()
    }

    fun clearPlayedGames() {
        userModifiedLastPlayedEarly = true
        _playedGameIds.value = emptyList()
        _lastPlayedId.value = null
        prefs?.edit()
            ?.remove(KEY_LAST_PLAYED_ID)
            ?.remove(KEY_PLAYED_GAME_IDS_LIST)
            ?.apply()
        recentPrefs?.edit()?.remove(KEY_LAST_PLAYED_ID)?.apply()
    }

    fun recordLastPlayedByUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val result = onWebGamePlayedOrDiscovered(
            rawTitle = null,
            rawUrl = trimmed,
            imageUrl = null,
            markAsLastPlayed = true
        )
        if (result == null && trimmed.contains("fortnite", ignoreCase = true)) {
            recordLastPlayed("fortnite")
        }
    }

    /**
     * Updates the user's live Xbox Live profile detected from the signed-in Xbox Cloud WebView.
     */
    fun onXboxProfileSynced(gamertag: String?, avatarUrl: String?, subscriptionTier: String?) {
        val cleanTag = gamertag?.trim()?.takeIf {
            it.isNotBlank() &&
                !it.equals("Sign in", ignoreCase = true) &&
                !it.equals("Đăng nhập", ignoreCase = true) &&
                !it.equals("Xbox", ignoreCase = true) &&
                it.length in 2..28
        } ?: return
        val cleanAvatar = normalizeImageUrl(avatarUrl, widthPx = 120, quality = 70)
        val cleanTier = subscriptionTier?.trim()?.takeIf { it.isNotBlank() } ?: "Xbox Cloud Gaming"
        val current = _xboxStatus.value
        if (current.gamertag == cleanTag && current.avatarUrl == cleanAvatar && current.subscriptionTier == cleanTier) return

        val updated = current.copy(
            gamertag = cleanTag,
            avatarUrl = cleanAvatar ?: current.avatarUrl,
            subscriptionTier = cleanTier
        )
        _xboxStatus.value = updated
        prefs?.edit()
            ?.putString(KEY_XBOX_GAMERTAG, updated.gamertag)
            ?.putString(KEY_XBOX_AVATAR_URL, updated.avatarUrl ?: "")
            ?.putString(KEY_XBOX_SUB_TIER, updated.subscriptionTier ?: "")
            ?.apply()
    }

    /**
     * Syncs the user's "Jump back in" (Recently played on Xbox Cloud) carousel detected from xbox.com/play.
     */
    fun onXboxJumpBackInSynced(jsonArrayStr: String) {
        if (jsonArrayStr.isBlank()) return
        ioScope.launch {
            try {
                val arr = JSONArray(jsonArrayStr)
                if (arr.length() == 0) return@launch
                val jumpIds = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val rawUrl = obj.optString("url", "").trim()
                    val rawTitle = obj.optString("title", "").trim()
                    val rawImg = obj.optString("imageUrl", "").trim().takeIf { it.isNotBlank() }
                    if (rawUrl.isBlank()) continue
                    val bookmark = onWebGamePlayedOrDiscovered(
                        rawTitle = rawTitle,
                        rawUrl = rawUrl,
                        imageUrl = rawImg,
                        markAsLastPlayed = false
                    )
                    if (bookmark != null && bookmark.id !in jumpIds) {
                        jumpIds.add(bookmark.id)
                    }
                }
                if (jumpIds.isNotEmpty()) {
                    val mergedJumpIds = (jumpIds + _xboxStatus.value.jumpBackInIds).distinct().take(10)
                    _xboxStatus.value = _xboxStatus.value.copy(jumpBackInIds = mergedJumpIds)
                    prefs?.edit()?.putString(KEY_XBOX_JUMP_BACK_IDS, mergedJumpIds.joinToString(","))?.apply()
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Automatically detects any game opened or launched inside the Xbox Cloud WebView,
     * matches or adds its metadata to GameLibraryRepository, and records it as the last played game
     * so "Chơi tiếp / Continue" on HomeScreen always shows the exact game played on the web.
     */
    fun onWebGamePlayedOrDiscovered(
        rawTitle: String?,
        rawUrl: String,
        imageUrl: String? = null,
        category: String? = null,
        supportsTouch: Boolean? = null,
        markAsLastPlayed: Boolean = true
    ): GameBookmark? {
        val cleanUrl = rawUrl.trim()
        val isXboxGameUrl = cleanUrl.contains("/play/games/", ignoreCase = true) ||
            cleanUrl.contains("/play/launch/", ignoreCase = true) ||
            cleanUrl.contains("/products/", ignoreCase = true) ||
            cleanUrl.contains("play.xbox.com", ignoreCase = true)

        val cleanedTitle = rawTitle
            ?.replace(XBOX_CLOUD_SUFFIX_REGEX, "")
            ?.replace(XBOX_PIPE_SUFFIX_REGEX, "")
            ?.trim()
            .orEmpty()
        val hasValidTitle = cleanedTitle.isNotBlank() &&
            !cleanedTitle.equals("Xbox Cloud Gaming", ignoreCase = true) &&
            !cleanedTitle.equals("Xbox Cloud Game", ignoreCase = true) &&
            !cleanedTitle.equals("Xbox Game", ignoreCase = true) &&
            !cleanedTitle.equals("Fortnite | Xbox Cloud Gaming", ignoreCase = true)

        val validImg = normalizeImageUrl(imageUrl, widthPx = 260, quality = 70)
        val productId = extractProductId(cleanUrl)
        val slug = extractSlug(cleanUrl)

        val currentList = _games.value
        val matched = currentList.firstOrNull { item ->
            val itemPid = extractProductId(item.playUrl)
            val itemSlug = extractSlug(item.playUrl)
            (productId.isNotBlank() && itemPid.equals(productId, ignoreCase = true)) ||
                (slug.length > 2 && itemSlug.equals(slug, ignoreCase = true)) ||
                (slug.length > 2 && slug.contains(item.id.replace("_", "-"), ignoreCase = true)) ||
                (hasValidTitle && item.title.equals(cleanedTitle, ignoreCase = true))
        } ?: if (cleanUrl.contains("fortnite", ignoreCase = true) || cleanedTitle.contains("fortnite", ignoreCase = true)) {
            currentList.firstOrNull { it.id == "fortnite" }
        } else null

        if (matched != null) {
            var updatedItem = matched
            if (validImg != null && matched.imageUrl.isNullOrBlank()) {
                updatedItem = updatedItem.copy(imageUrl = validImg)
            }
            if (hasValidTitle && matched.title.startsWith("Xbox ", ignoreCase = true)) {
                updatedItem = updatedItem.copy(title = cleanedTitle)
            }
            if (updatedItem != matched) {
                val updatedList = currentList.map { if (it.id == matched.id) updatedItem else it }
                _games.value = updatedList
                saveSyncedGames(updatedList)
            }
            if (markAsLastPlayed) {
                recordLastPlayed(matched.id)
            }
            if ((updatedItem.imageUrl.isNullOrBlank() || updatedItem.description.isNullOrBlank()) && productId.isNotBlank()) {
                fetchSingleProductMetadataAsync(productId)
            }
            return updatedItem
        }

        if (!isXboxGameUrl && !hasValidTitle) return null
        if (productId.isBlank() && slug.isBlank() && !hasValidTitle) return null

        val resolvedTitle = if (hasValidTitle) cleanedTitle else formatSlugToTitle(slug)
        val resolvedSlug = if (slug.isNotBlank()) slug else slugifyTitle(resolvedTitle)
        val canonicalPlayUrl = if (productId.isNotBlank()) {
            "https://www.xbox.com/en-US/play/games/$resolvedSlug/$productId"
        } else if (isXboxGameUrl) {
            cleanUrl.replace("/play/launch/", "/play/games/").substringBefore("?")
        } else {
            return null
        }

        val newId = if (productId.isNotBlank()) {
            "xbox_${productId.lowercase(Locale.ROOT)}"
        } else {
            "xbox_${resolvedSlug.replace("-", "_")}"
        }

        val accent = ACCENT_PALETTE[(resolvedTitle.hashCode() and 0x7FFFFFFF) % ACCENT_PALETTE.size]
        val isFreeCloud = isFreeToPlayTitle(resolvedTitle, productId, canonicalPlayUrl)
        val isTouchCloud = supportsTouch ?: isTouchSupportedTitle(productId, canonicalPlayUrl)
        val isRecentCloud = isRecentlyAddedTitle(productId, canonicalPlayUrl)
        val isPopularCloud = isPopularTitle(productId, canonicalPlayUrl)
        val (resolvedCatVi, resolvedCatEn) = resolveCategoryBilingual(
            productId = productId,
            fallbackVi = category ?: "Hành động & Phiêu lưu",
            fallbackEn = category ?: "Action & Adventure"
        )
        val newBookmark = GameBookmark(
            id = if (isFreeCloud && productId.equals(FORTNITE_PRODUCT_ID, ignoreCase = true)) "fortnite" else newId,
            title = resolvedTitle,
            category = resolvedCatVi,
            categoryEn = resolvedCatEn,
            playUrl = canonicalPlayUrl,
            freeToPlay = isFreeCloud,
            supportsTouch = isTouchCloud,
            accentColorHex = accent,
            badge = when {
                isFreeCloud -> "Miễn phí • Cloud"
                isRecentCloud -> "Mới lên kệ • Cloud"
                isTouchCloud -> "Cảm ứng • Cloud"
                else -> "Xbox Cloud"
            },
            badgeEn = when {
                isFreeCloud -> "Free • Cloud"
                isRecentCloud -> "New on Cloud"
                isTouchCloud -> "Touch • Cloud"
                else -> "Xbox Cloud"
            },
            rating = "4.8",
            imageUrl = validImg,
            heroImageUrl = validImg,
            isPopular = isPopularCloud,
            isRecentlyAdded = isRecentCloud
        )

        val updatedList = currentList + newBookmark
        _games.value = updatedList
        saveSyncedGames(updatedList)

        if (markAsLastPlayed) {
            recordLastPlayed(newBookmark.id)
        }
        if (productId.isNotBlank()) {
            fetchSingleProductMetadataAsync(productId)
        }
        return newBookmark
    }

    /**
     * Đánh dấu Miễn phí (freeToPlay = true) theo đúng danh sách chính thức "Truyền phát miễn phí với quảng cáo"
     * (Stream for free with ads - SIGL FresnoMain: 51f14e5d-bdcb-4e04-b9cb-76e5057702df, gồm 95 tựa game).
     */
    private fun isFreeToPlayTitle(
        title: String,
        productId: String = "",
        playUrl: String = ""
    ): Boolean {
        val pid = productId.ifBlank { extractProductId(playUrl) }.uppercase(Locale.ROOT)
        if (pid.isNotBlank() && (pid in cachedFreeToPlayIds || pid in OFFICIAL_FREE_TO_PLAY_PRODUCT_IDS)) {
            return true
        }
        val cleanTitle = title.trim()
        return cleanTitle.equals("Fortnite", ignoreCase = true) ||
            cleanTitle.contains("007 First Light", ignoreCase = true) ||
            cleanTitle.contains("3on3 FreeStyle", ignoreCase = true) ||
            cleanTitle.contains("Albion Online", ignoreCase = true) ||
            cleanTitle.equals("Aniimo", ignoreCase = true) ||
            cleanTitle.contains("Asphalt Legends", ignoreCase = true) ||
            cleanTitle.equals("Blindfire", ignoreCase = true)
    }

    /**
     * Kiểm tra chính xác game có hỗ trợ cảm ứng trên Xbox Cloud hay không theo SIGL chính thức
     * CloudPlayWithTouch (9c86f07a-f3e8-45ad-82a0-a1f759597059, 126 game).
     */
    fun isTouchSupportedTitle(
        productId: String = "",
        playUrl: String = ""
    ): Boolean {
        val pid = productId.ifBlank { extractProductId(playUrl) }.uppercase(Locale.ROOT)
        if (pid.isBlank()) return playUrl.contains("fortnite", ignoreCase = true)
        return pid in cachedTouchIds || pid in OFFICIAL_TOUCH_PRODUCT_IDS || pid == FORTNITE_PRODUCT_ID
    }

    /**
     * Kiểm tra game thuộc danh mục "Mới thêm gần đây" (RecentlyAdded: 06323672-b8c8-43cc-b0de-32d5a9834749, 56 game).
     */
    fun isRecentlyAddedTitle(
        productId: String = "",
        playUrl: String = ""
    ): Boolean {
        val pid = productId.ifBlank { extractProductId(playUrl) }.uppercase(Locale.ROOT)
        if (pid.isBlank()) return false
        return pid in cachedRecentIds || pid in OFFICIAL_RECENTLY_ADDED_PRODUCT_IDS
    }

    /**
     * Kiểm tra game thuộc danh mục "Phổ biến nhất" (CloudMostPopular: 6a589fa0-d493-472b-8e20-3813699d7056).
     */
    fun isPopularTitle(
        productId: String = "",
        playUrl: String = ""
    ): Boolean {
        val pid = productId.ifBlank { extractProductId(playUrl) }.uppercase(Locale.ROOT)
        if (pid.isBlank()) return playUrl.contains("fortnite", ignoreCase = true)
        return pid in cachedPopularIds || pid in OFFICIAL_POPULAR_PRODUCT_IDS || pid == FORTNITE_PRODUCT_ID
    }

    /**
     * Kiểm tra game có thuộc 1 trong 8 danh mục thể loại chính thức trên trang play.xbox.com (GenresRow) hay không:
     * INDIE, FAMILY, SHOOTER, ACTION, FIGHTING, STRATEGY, SIMULATION, RPG.
     */
    fun matchesOfficialGenre(game: GameBookmark, genreCode: String): Boolean {
        if (genreCode.isBlank() || genreCode.equals("ALL", ignoreCase = true)) return true
        val pid = extractProductId(game.playUrl).uppercase(Locale.ROOT)
        return when (genreCode.uppercase(Locale.ROOT)) {
            "INDIE" -> (pid.isNotBlank() && (pid in cachedIndieIds || pid in OFFICIAL_INDIE_IDS)) ||
                game.category.contains("Indie", ignoreCase = true) ||
                game.categoryEn.contains("Indie", ignoreCase = true)
            "FAMILY" -> (pid.isNotBlank() && (pid in cachedFamilyIds || pid in OFFICIAL_FAMILY_IDS)) ||
                game.category.contains("Gia đình", ignoreCase = true) ||
                game.categoryEn.contains("Family", ignoreCase = true)
            "SHOOTER" -> (pid.isNotBlank() && (pid in cachedShooterIds || pid in OFFICIAL_SHOOTER_IDS)) ||
                game.category.contains("Bắn súng", ignoreCase = true) ||
                game.categoryEn.contains("Shooter", ignoreCase = true) ||
                game.categoryEn.contains("Battle Royale", ignoreCase = true)
            "FIGHTING" -> (pid.isNotBlank() && (pid in cachedFightingIds || pid in OFFICIAL_FIGHTING_IDS)) ||
                game.category.contains("Đối kháng", ignoreCase = true) ||
                game.categoryEn.contains("Fighting", ignoreCase = true)
            "STRATEGY" -> (pid.isNotBlank() && (pid in cachedStrategyIds || pid in OFFICIAL_STRATEGY_IDS)) ||
                game.category.contains("Chiến thuật", ignoreCase = true) ||
                game.categoryEn.contains("Strategy", ignoreCase = true)
            "SIMULATION" -> (pid.isNotBlank() && (pid in cachedSimulationIds || pid in OFFICIAL_SIMULATION_IDS)) ||
                game.category.contains("Mô phỏng", ignoreCase = true) ||
                game.categoryEn.contains("Simulation", ignoreCase = true)
            "RPG" -> (pid.isNotBlank() && (pid in cachedRpgIds || pid in OFFICIAL_RPG_IDS)) ||
                game.category.contains("Nhập vai", ignoreCase = true) ||
                game.categoryEn.contains("RPG", ignoreCase = true) ||
                game.categoryEn.contains("Role Playing", ignoreCase = true)
            "ACTION" -> game.category.contains("Hành động", ignoreCase = true) ||
                game.category.contains("Phiêu lưu", ignoreCase = true) ||
                game.categoryEn.contains("Action", ignoreCase = true) ||
                game.categoryEn.contains("Adventure", ignoreCase = true) ||
                (pid.isNotBlank() &&
                    pid !in cachedFightingIds &&
                    pid !in cachedStrategyIds &&
                    pid !in cachedSimulationIds &&
                    pid !in cachedFamilyIds)
            else -> true
        }
    }

    private fun resolveCategoryBilingual(
        productId: String,
        fallbackVi: String = "",
        fallbackEn: String = ""
    ): Pair<String, String> {
        val pid = productId.trim().uppercase(Locale.ROOT)
        if (pid.isNotBlank()) {
            val tagsVi = ArrayList<String>(3)
            val tagsEn = ArrayList<String>(3)
            if (pid in cachedShooterIds || pid in OFFICIAL_SHOOTER_IDS) {
                tagsVi.add("Bắn súng")
                tagsEn.add("Shooter")
            }
            if (pid in cachedRpgIds || pid in OFFICIAL_RPG_IDS) {
                tagsVi.add("Nhập vai (RPG)")
                tagsEn.add("RPG")
            }
            if (pid in cachedFightingIds || pid in OFFICIAL_FIGHTING_IDS) {
                tagsVi.add("Đối kháng")
                tagsEn.add("Fighting")
            }
            if (pid in cachedStrategyIds || pid in OFFICIAL_STRATEGY_IDS) {
                tagsVi.add("Chiến thuật")
                tagsEn.add("Strategy")
            }
            if (pid in cachedSimulationIds || pid in OFFICIAL_SIMULATION_IDS) {
                tagsVi.add("Mô phỏng")
                tagsEn.add("Simulation")
            }
            if (pid in cachedIndieIds || pid in OFFICIAL_INDIE_IDS) {
                if (tagsVi.size < 2) {
                    tagsVi.add("Indie")
                    tagsEn.add("Indie")
                }
            }
            if (pid in cachedFamilyIds || pid in OFFICIAL_FAMILY_IDS) {
                if (tagsVi.size < 2) {
                    tagsVi.add("Gia đình")
                    tagsEn.add("Family")
                }
            }
            if (tagsVi.isEmpty()) {
                tagsVi.add("Hành động & Phiêu lưu")
                tagsEn.add("Action & Adventure")
            } else if (tagsVi.size == 1 &&
                pid !in cachedFightingIds &&
                pid !in cachedStrategyIds &&
                pid !in cachedSimulationIds &&
                pid !in cachedFamilyIds
            ) {
                tagsVi.add("Hành động")
                tagsEn.add("Action")
            }
            return tagsVi.take(2).joinToString(" • ") to tagsEn.take(2).joinToString(" • ")
        }
        if (fallbackVi.isNotBlank() && fallbackVi != "Xbox Cloud Game Pass") {
            return fallbackVi to fallbackEn.ifBlank { fallbackVi }
        }
        return "Hành động & Phiêu lưu" to "Action & Adventure"
    }

    /**
     * Ingests a batch of game cards scraped live from the Xbox Cloud web DOM (metadata only, no image downloads).
     */
    fun ingestDiscoveredWebGames(jsonArrayStr: String) {
        if (jsonArrayStr.isBlank() || isStreamActive || com.example.vpn.VpnManager.isStreamActive()) return
        ioScope.launch {
            try {
                if (isStreamActive || com.example.vpn.VpnManager.isStreamActive()) return@launch
                val arr = JSONArray(jsonArrayStr)
                if (arr.length() == 0) return@launch
                val currentList = _games.value.toMutableList()
                var changed = false

                for (i in 0 until arr.length()) {
                    if (isStreamActive || com.example.vpn.VpnManager.isStreamActive()) return@launch
                    val obj = arr.optJSONObject(i) ?: continue
                    val rawUrl = obj.optString("url", "").trim()
                    val rawTitle = obj.optString("title", "").trim()
                    val rawImg = normalizeImageUrl(obj.optString("imageUrl", ""), widthPx = 260, quality = 70)
                    val webFreeFlag = obj.optBoolean("freeToPlay", false)
                    if (rawUrl.isBlank()) continue

                    val pid = extractProductId(rawUrl)
                    val slug = extractSlug(rawUrl)
                    if (pid.isBlank() && slug.isBlank()) continue

                    val existingIdx = currentList.indexOfFirst { item ->
                        val itemPid = extractProductId(item.playUrl)
                        val itemSlug = extractSlug(item.playUrl)
                        (pid.isNotBlank() && itemPid.equals(pid, ignoreCase = true)) ||
                            (slug.length > 2 && itemSlug.equals(slug, ignoreCase = true))
                    }

                    if (existingIdx >= 0) {
                        val existing = currentList[existingIdx]
                        val updatedImg = if (rawImg != null && existing.imageUrl.isNullOrBlank()) rawImg else existing.imageUrl
                        val updatedFree = existing.freeToPlay || webFreeFlag || isFreeToPlayTitle(existing.title, pid, existing.playUrl)
                        val updatedTouch = isTouchSupportedTitle(pid, existing.playUrl)
                        if (updatedImg != existing.imageUrl || updatedFree != existing.freeToPlay || updatedTouch != existing.supportsTouch) {
                            currentList[existingIdx] = existing.copy(
                                imageUrl = updatedImg,
                                freeToPlay = updatedFree,
                                supportsTouch = updatedTouch
                            )
                            changed = true
                        }
                    } else if (rawImg != null || pid.isNotBlank()) {
                        val cleanTitle = rawTitle
                            .replace(XBOX_BULLET_SUFFIX_REGEX, "")
                            .trim()
                            .ifBlank { formatSlugToTitle(slug) }
                        if (cleanTitle.equals("Xbox Cloud Game", ignoreCase = true)) continue
                        val canonicalUrl = if (pid.isNotBlank()) {
                            "https://www.xbox.com/en-US/play/games/${slug.ifBlank { slugifyTitle(cleanTitle) }}/$pid"
                        } else {
                            rawUrl.replace("/play/launch/", "/play/games/").substringBefore("?")
                        }
                        val id = if (pid.isNotBlank()) "xbox_${pid.lowercase(Locale.ROOT)}" else "xbox_${slug.replace("-", "_")}"
                        if (currentList.any { it.id == id }) continue
                        val accent = ACCENT_PALETTE[(cleanTitle.hashCode() and 0x7FFFFFFF) % ACCENT_PALETTE.size]
                        val isFree = webFreeFlag || isFreeToPlayTitle(cleanTitle, pid, canonicalUrl)
                        val isTouch = isTouchSupportedTitle(pid, canonicalUrl)
                        val isRecent = isRecentlyAddedTitle(pid, canonicalUrl)
                        val isPop = isPopularTitle(pid, canonicalUrl)
                        val (catVi, catEn) = resolveCategoryBilingual(pid)
                        currentList.add(
                            GameBookmark(
                                id = id,
                                title = cleanTitle,
                                category = catVi,
                                categoryEn = catEn,
                                playUrl = canonicalUrl,
                                freeToPlay = isFree,
                                supportsTouch = isTouch,
                                accentColorHex = accent,
                                badge = when {
                                    isFree -> "Miễn phí • Cloud"
                                    isRecent -> "Mới lên kệ • Cloud"
                                    isTouch -> "Cảm ứng • Cloud"
                                    else -> "Game Pass"
                                },
                                badgeEn = when {
                                    isFree -> "Free • Cloud"
                                    isRecent -> "New on Cloud"
                                    isTouch -> "Touch • Cloud"
                                    else -> "Game Pass"
                                },
                                rating = "4.8",
                                imageUrl = rawImg,
                                heroImageUrl = rawImg,
                                isPopular = isPop,
                                isRecentlyAdded = isRecent
                            )
                        )
                        changed = true
                    }
                }

                if (changed && !isStreamActive && !com.example.vpn.VpnManager.isStreamActive()) {
                    _games.value = currentList
                    saveSyncedGames(currentList)
                    _xboxStatus.value = _xboxStatus.value.copy(syncedCatalogCount = currentList.size)
                }
            } catch (_: Exception) {
            }
        }
    }

    @Volatile
    private var isStreamActive: Boolean = false
    private var catalogSyncJob: kotlinx.coroutines.Job? = null

    fun setStreamActive(active: Boolean) {
        isStreamActive = active
        if (active) {
            catalogSyncJob?.cancel()
            _isSyncingCatalog.value = false
        }
    }

    /**
     * Tự động quét toàn bộ kho game Xbox Cloud theo từng lô (streaming batches),
     * bao gồm cả kho "Truyền phát miễn phí với quảng cáo" (95 game FresnoMain), kho Cloud Game Pass (572+ game),
     * danh mục Mới thêm gần đây (56 game), Chơi bằng cảm ứng (126 game) và 8 thể loại chính thức trên play.xbox.com.
     */
    fun syncLiveXboxCatalog(force: Boolean = false, maxCatalogGames: Int = 650) {
        if (_isSyncingCatalog.value && catalogSyncJob?.isActive == true) return
        if (!force && (isStreamActive || com.example.vpn.VpnManager.isStreamActive())) return
        if (force) {
            isStreamActive = false
        }
        catalogSyncJob?.cancel()
        catalogSyncJob = ioScope.launch {
            _isSyncingCatalog.value = true
            val startPingMs = System.currentTimeMillis()
            var anyBatchAdded = false
            try {
                if (!force && (isStreamActive || com.example.vpn.VpnManager.isStreamActive())) return@launch

                val (allCloudIds, freeIds, touchIds, popularIds, recentIds) = coroutineScope {
                    val allDeferred = async {
                        fetchSiglProductIdsSafe(CLOUD_CATALOG_SIGL_URL, maxCount = 650)
                    }
                    val freeDeferred = async {
                        fetchSiglProductIdsSafe(FREE_TO_PLAY_SIGL_URL, maxCount = 150).toSet()
                    }
                    val touchDeferred = async {
                        fetchSiglProductIdsSafe(TOUCH_CATALOG_SIGL_URL, maxCount = 350).toSet()
                    }
                    val popDeferred = async {
                        fetchSiglProductIdsSafe(POPULAR_CLOUD_SIGL_URL, maxCount = 55).toSet()
                    }
                    val recDeferred = async {
                        fetchSiglProductIdsSafe(RECENTLY_ADDED_SIGL_URL, maxCount = 100).toSet()
                    }
                    Quintuple(
                        allDeferred.await(),
                        freeDeferred.await(),
                        touchDeferred.await(),
                        popDeferred.await(),
                        recDeferred.await()
                    )
                }

                // Đồng bộ ngầm 8 SIGL thể loại chính thức trên play.xbox.com
                ioScope.launch {
                    try {
                        val indie = fetchSiglProductIdsSafe(GENRE_INDIE_SIGL_URL, 250).toSet()
                        if (indie.isNotEmpty()) cachedIndieIds = OFFICIAL_INDIE_IDS + indie
                        val family = fetchSiglProductIdsSafe(GENRE_FAMILY_SIGL_URL, 200).toSet()
                        if (family.isNotEmpty()) cachedFamilyIds = OFFICIAL_FAMILY_IDS + family
                        val shooter = fetchSiglProductIdsSafe(GENRE_SHOOTERS_SIGL_URL, 300).toSet()
                        if (shooter.isNotEmpty()) cachedShooterIds = OFFICIAL_SHOOTER_IDS + shooter
                        val fighting = fetchSiglProductIdsSafe(GENRE_FIGHTING_SIGL_URL, 100).toSet()
                        if (fighting.isNotEmpty()) cachedFightingIds = OFFICIAL_FIGHTING_IDS + fighting
                        val strategy = fetchSiglProductIdsSafe(GENRE_STRATEGY_SIGL_URL, 200).toSet()
                        if (strategy.isNotEmpty()) cachedStrategyIds = OFFICIAL_STRATEGY_IDS + strategy
                        val sim = fetchSiglProductIdsSafe(GENRE_SIMULATION_SIGL_URL, 200).toSet()
                        if (sim.isNotEmpty()) cachedSimulationIds = OFFICIAL_SIMULATION_IDS + sim
                        val rpg = fetchSiglProductIdsSafe(GENRE_RPG_SIGL_URL, 250).toSet()
                        if (rpg.isNotEmpty()) cachedRpgIds = OFFICIAL_RPG_IDS + rpg
                    } catch (_: Exception) {
                    }
                }

                if (!force && isStreamActive) return@launch
                val catalogRtt = (System.currentTimeMillis() - startPingMs).toInt().coerceIn(15, 2500)

                if (allCloudIds.isNotEmpty()) {
                    cachedAllCloudProductIds = allCloudIds
                }
                if (freeIds.isNotEmpty()) {
                    cachedFreeToPlayIds = OFFICIAL_FREE_TO_PLAY_PRODUCT_IDS + freeIds
                }
                if (touchIds.isNotEmpty()) {
                    cachedTouchIds = OFFICIAL_TOUCH_PRODUCT_IDS + touchIds
                }
                if (popularIds.isNotEmpty()) {
                    cachedPopularIds = OFFICIAL_POPULAR_PRODUCT_IDS + popularIds
                }
                if (recentIds.isNotEmpty()) {
                    cachedRecentIds = OFFICIAL_RECENTLY_ADDED_PRODUCT_IDS + recentIds
                }

                val effectiveAllIds = if (allCloudIds.isNotEmpty()) allCloudIds else cachedAllCloudProductIds
                val prioritizedCloudIds = (
                    listOf(FORTNITE_PRODUCT_ID) +
                        cachedFreeToPlayIds.toList() +
                        cachedRecentIds.toList() +
                        cachedPopularIds.toList() +
                        effectiveAllIds +
                        cachedTouchIds.toList()
                    )
                    .map { it.uppercase(Locale.ROOT) }
                    .distinct()
                    .take(maxCatalogGames.coerceAtLeast(660))

                if (prioritizedCloudIds.isEmpty()) {
                    _xboxStatus.value = _xboxStatus.value.copy(cloudServiceOnline = false)
                    return@launch
                }

                val totalCloud = prioritizedCloudIds.size.coerceAtLeast(effectiveAllIds.size)
                _xboxStatus.value = _xboxStatus.value.copy(
                    cloudServiceOnline = true,
                    cloudLatencyMs = catalogRtt,
                    totalCloudCatalogCount = totalCloud.coerceAtLeast(_games.value.size)
                )

                val sharedPrefs = prefs
                val inMemoryGames = _games.value
                val customList = if (inMemoryGames.isNotEmpty()) {
                    inMemoryGames.filter { it.id.startsWith("custom_") }
                } else if (sharedPrefs != null) {
                    loadGamesArrayFromPrefs(sharedPrefs, KEY_CUSTOM_GAMES_JSON)
                } else {
                    emptyList()
                }
                val diskSynced = if (sharedPrefs != null) {
                    loadSyncedGamesFromFileOrMigratePrefs(sharedPrefs)
                } else {
                    emptyList()
                }
                val memorySynced = inMemoryGames.filter { !it.id.startsWith("custom_") }
                val existingSynced = (diskSynced + memorySynced)
                    .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }

                val existingWithImages = existingSynced.filter { !it.imageUrl.isNullOrBlank() }
                if (existingWithImages.isNotEmpty()) {
                    rebuildMergedGames(customList, existingWithImages)
                }

                val existingPids = existingWithImages
                    .mapNotNull { extractProductId(it.playUrl).takeIf { id -> id.isNotBlank() }?.uppercase(Locale.ROOT) }
                    .toSet()

                val missingIds = prioritizedCloudIds.filter { it !in existingPids }
                val idsToFetch = if (missingIds.isNotEmpty()) {
                    missingIds
                } else if (force && existingWithImages.size < 120) {
                    prioritizedCloudIds
                } else {
                    emptyList()
                }

                val runningSynced = existingWithImages.toMutableList()
                var completedGroups = 0
                // Tải theo từng cặp 2 lô (mỗi lô 30 game = 60 game/đợt) với fieldsTemplate=browse + gzip nhẹ gấp 20 lần
                for (batchGroup in idsToFetch.chunked(30).chunked(2)) {
                    if (!force && isStreamActive) break
                    val groupResults = coroutineScope {
                        batchGroup.map { batch ->
                            async {
                                fetchDisplayCatalogProductsSafe(
                                    bigIds = batch,
                                    touchIds = cachedTouchIds,
                                    popularIds = cachedPopularIds,
                                    recentIds = cachedRecentIds
                                )
                            }
                        }.flatMap { it.await() }
                    }
                    if (!force && isStreamActive) break
                    if (groupResults.isNotEmpty()) {
                        anyBatchAdded = true
                        runningSynced.addAll(groupResults)
                        val progressiveDeduped = runningSynced
                            .filter { !it.imageUrl.isNullOrBlank() }
                            .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                        rebuildMergedGames(customList, progressiveDeduped)
                        val currentCount = _games.value.size
                        _xboxStatus.value = _xboxStatus.value.copy(
                            cloudServiceOnline = true,
                            cloudLatencyMs = catalogRtt,
                            totalCloudCatalogCount = totalCloud.coerceAtLeast(currentCount),
                            syncedCatalogCount = currentCount
                        )
                        completedGroups++
                        if (completedGroups % 2 == 0) {
                            saveSyncedGames(_games.value)
                        }
                    }
                }

                if (runningSynced.isNotEmpty()) {
                    val finalDeduped = runningSynced
                        .filter { !it.imageUrl.isNullOrBlank() }
                        .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                    rebuildMergedGames(customList, finalDeduped)
                }

                saveSyncedGames(_games.value)
                val nowMs = System.currentTimeMillis()
                val finalCount = _games.value.size
                _xboxStatus.value = _xboxStatus.value.copy(
                    cloudServiceOnline = true,
                    cloudLatencyMs = catalogRtt,
                    totalCloudCatalogCount = totalCloud.coerceAtLeast(finalCount),
                    syncedCatalogCount = finalCount,
                    lastSyncTimeMs = nowMs
                )
                sharedPrefs?.edit()
                    ?.putLong(KEY_LAST_CATALOG_SYNC_MS, nowMs)
                    ?.putInt(KEY_XBOX_TOTAL_CLOUD, totalCloud.coerceAtLeast(finalCount))
                    ?.apply()
            } catch (_: Exception) {
                if (anyBatchAdded) {
                    saveSyncedGames(_games.value)
                } else {
                    _xboxStatus.value = _xboxStatus.value.copy(cloudServiceOnline = _games.value.size > 2)
                }
            } finally {
                _isSyncingCatalog.value = false
            }
        }
    }

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    /**
     * Loads the next batch of unsynced Xbox Cloud games from Microsoft's full Cloud SIGL feed.
     */
    fun loadMoreXboxCatalogGames(batchSize: Int = 40) {
        if (_isSyncingCatalog.value || isStreamActive || com.example.vpn.VpnManager.isStreamActive()) return
        ioScope.launch {
            _isSyncingCatalog.value = true
            try {
                if (cachedAllCloudProductIds.isEmpty()) {
                    cachedAllCloudProductIds = fetchSiglProductIdsSafe(CLOUD_CATALOG_SIGL_URL, maxCount = 650)
                }
                val existingPids = _games.value
                    .mapNotNull { extractProductId(it.playUrl).takeIf { id -> id.isNotBlank() }?.uppercase(Locale.ROOT) }
                    .toSet()

                val nextBatchIds = cachedAllCloudProductIds
                    .filter { it.uppercase(Locale.ROOT) !in existingPids }
                    .take(batchSize)

                if (nextBatchIds.isEmpty()) {
                    syncLiveXboxCatalog(force = true, maxCatalogGames = 650)
                    return@launch
                }

                val newBookmarks = mutableListOf<GameBookmark>()
                nextBatchIds.chunked(25).forEach { chunk ->
                    newBookmarks.addAll(
                        fetchDisplayCatalogProductsSafe(
                            bigIds = chunk,
                            touchIds = cachedTouchIds,
                            popularIds = cachedPopularIds,
                            recentIds = cachedRecentIds
                        )
                    )
                }

                if (newBookmarks.isNotEmpty()) {
                    val current = _games.value
                    val merged = (current + newBookmarks).distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                    _games.value = merged
                    saveSyncedGames(merged)
                    _xboxStatus.value = _xboxStatus.value.copy(
                        syncedCatalogCount = merged.size,
                        totalCloudCatalogCount = cachedAllCloudProductIds.size.coerceAtLeast(merged.size)
                    )
                }
            } catch (_: Exception) {
            } finally {
                _isSyncingCatalog.value = false
            }
        }
    }

    /**
     * Searches Microsoft's live DisplayCatalog API by keyword, resolves lightweight game metadata,
     * adds discovered Xbox games to the local library, and returns matching results via callback.
     */
    fun searchMicrosoftXboxCatalog(
        query: String,
        onResult: (List<GameBookmark>) -> Unit = {}
    ) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) {
            onResult(emptyList())
            return
        }
        ioScope.launch {
            _isSearchingLiveXbox.value = true
            try {
                val encoded = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
                val suggestUrl =
                    "https://displaycatalog.mp.microsoft.com/v7.0/productFamilies/autosuggest?market=US&languages=en-us&productFamilyNames=Games&query=$encoded"
                val conn = (URL(suggestUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }
                val discoveredIds = mutableListOf<String>()
                try {
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val root = JSONObject(body)
                        val resultsArr = root.optJSONArray("Results")
                        if (resultsArr != null) {
                            for (i in 0 until resultsArr.length()) {
                                val group = resultsArr.optJSONObject(i) ?: continue
                                val prods = group.optJSONArray("Products") ?: continue
                                for (j in 0 until prods.length()) {
                                    val p = prods.optJSONObject(j) ?: continue
                                    val pid = p.optString("ProductId", "").trim().uppercase(Locale.ROOT)
                                    if (pid.length == 12 && pid.all { it.isLetterOrDigit() } && pid !in discoveredIds) {
                                        discoveredIds.add(pid)
                                        if (discoveredIds.size >= 16) break
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }

                if (discoveredIds.isNotEmpty()) {
                    val fetched = fetchDisplayCatalogProducts(
                        bigIds = discoveredIds,
                        touchIds = cachedTouchIds,
                        popularIds = cachedPopularIds,
                        recentIds = cachedRecentIds
                    )
                    if (fetched.isNotEmpty()) {
                        val current = _games.value
                        val merged = (current + fetched).distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                        _games.value = merged
                        saveSyncedGames(merged)
                        _xboxStatus.value = _xboxStatus.value.copy(syncedCatalogCount = merged.size)
                        onResult(fetched)
                        return@launch
                    }
                }
                onResult(emptyList())
            } catch (_: Exception) {
                onResult(emptyList())
            } finally {
                _isSearchingLiveXbox.value = false
            }
        }
    }

    private fun fetchSingleProductMetadataAsync(productId: String) {
        ioScope.launch {
            try {
                val results = fetchDisplayCatalogProducts(
                    bigIds = listOf(productId.uppercase(Locale.ROOT)),
                    touchIds = cachedTouchIds,
                    popularIds = cachedPopularIds,
                    recentIds = cachedRecentIds
                )
                val fetched = results.firstOrNull() ?: return@launch
                val current = _games.value
                var changed = false
                val updated = current.map { item ->
                    if (extractProductId(item.playUrl).equals(productId, ignoreCase = true)) {
                        changed = true
                        item.copy(
                            title = if (item.title.startsWith("Xbox ", ignoreCase = true)) fetched.title else item.title,
                            category = if (item.category == "Xbox Cloud Game Pass") fetched.category else item.category,
                            categoryEn = if (item.categoryEn == "Xbox Cloud Game Pass") fetched.categoryEn else item.categoryEn,
                            imageUrl = item.imageUrl ?: fetched.imageUrl,
                            heroImageUrl = item.heroImageUrl ?: fetched.heroImageUrl,
                            description = item.description ?: fetched.description,
                            publisher = item.publisher ?: fetched.publisher,
                            rating = fetched.rating
                        )
                    } else item
                }
                if (changed) {
                    _games.value = updated
                    saveSyncedGames(updated)
                }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun fetchSiglProductIdsSafe(siglUrl: String, maxCount: Int): List<String> {
        for (attempt in 0..1) {
            try {
                val list = fetchSiglProductIds(siglUrl, maxCount)
                if (list.isNotEmpty()) return list
            } catch (_: Exception) {
            }
            if (attempt == 0) {
                kotlinx.coroutines.delay(350L)
            }
        }
        return emptyList()
    }

    private fun fetchSiglProductIds(siglUrl: String, maxCount: Int): List<String> {
        val conn = (URL(siglUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            val ids = ArrayList<String>(maxCount)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim().uppercase(Locale.ROOT)
                if (id.length == 12 && id.all { it.isLetterOrDigit() }) {
                    ids.add(id)
                    if (ids.size >= maxCount) break
                }
            }
            ids
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun fetchDisplayCatalogProductsSafe(
        bigIds: List<String>,
        touchIds: Set<String>,
        popularIds: Set<String> = emptySet(),
        recentIds: Set<String> = emptySet()
    ): List<GameBookmark> {
        if (bigIds.isEmpty()) return emptyList()
        for (attempt in 0..1) {
            try {
                val items = fetchDisplayCatalogProducts(
                    bigIds = bigIds,
                    touchIds = touchIds,
                    popularIds = popularIds,
                    recentIds = recentIds
                )
                if (items.isNotEmpty()) return items
            } catch (_: Exception) {
            }
            if (attempt == 0) {
                kotlinx.coroutines.delay(350L)
            }
        }
        return emptyList()
    }

    private fun fetchDisplayCatalogProducts(
        bigIds: List<String>,
        touchIds: Set<String>,
        popularIds: Set<String> = emptySet(),
        recentIds: Set<String> = emptySet()
    ): List<GameBookmark> {
        if (bigIds.isEmpty()) return emptyList()
        val joined = bigIds.joinToString(",")
        val apiUrl = "https://displaycatalog.mp.microsoft.com/v7.0/products?bigIds=$joined&market=US&languages=en-us&fieldsTemplate=browse&MS-CV=DGU1mcuYo0WMMp+F.1"
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 9000
            readTimeout = 10000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "gzip")
        }
        return try {
            if (conn.responseCode != 200) return emptyList()
            val stream = if (conn.contentEncoding?.contains("gzip", ignoreCase = true) == true) {
                java.util.zip.GZIPInputStream(conn.inputStream)
            } else {
                conn.inputStream
            }
            val body = stream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val products = root.optJSONArray("Products") ?: return emptyList()
            val results = ArrayList<GameBookmark>(products.length())

            for (i in 0 until products.length()) {
                val prod = products.optJSONObject(i) ?: continue
                val productId = prod.optString("ProductId", "").trim().uppercase(Locale.ROOT)
                if (productId.isBlank()) continue

                val locProps = prod.optJSONArray("LocalizedProperties")?.optJSONObject(0) ?: continue
                val rawTitle = locProps.optString("ProductTitle", "").trim()
                if (rawTitle.isBlank()) continue

                val cleanTitle = rawTitle
                    .replace(" (Xbox Series X|S)", "", ignoreCase = true)
                    .replace(" - Standard Edition", "", ignoreCase = true)
                    .replace(" Standard Edition", "", ignoreCase = true)
                    .trim()

                val shortDesc = locProps.optString("ShortDescription", "").trim()
                    .ifBlank { locProps.optString("ProductDescription", "").trim() }
                    .replace(WHITESPACE_COLLAPSE_REGEX, " ")
                    .take(280)
                    .takeIf { it.isNotBlank() }

                val publisherName = locProps.optString("PublisherName", "").trim()
                    .ifBlank { locProps.optString("DeveloperName", "").trim() }
                    .takeIf { it.isNotBlank() }

                val imagesArr = locProps.optJSONArray("Images")
                var posterUrl: String? = null
                var superHeroUrl: String? = null
                var boxArtUrl: String? = null
                var fallbackImgUrl: String? = null

                if (imagesArr != null) {
                    for (j in 0 until imagesArr.length()) {
                        val imgObj = imagesArr.optJSONObject(j) ?: continue
                        val purpose = imgObj.optString("ImagePurpose", "")
                        val uri = normalizeImageUrl(
                            imgObj.optString("Uri", ""),
                            widthPx = if (purpose.equals("SuperHeroArt", ignoreCase = true)) 520 else 260,
                            quality = 70
                        ) ?: continue
                        when (purpose) {
                            "Poster" -> if (posterUrl == null) posterUrl = uri
                            "BoxArt" -> if (boxArtUrl == null) boxArtUrl = uri
                            "SuperHeroArt", "TitledHeroArt" -> if (superHeroUrl == null) superHeroUrl = uri
                            "Tile", "FeaturePromotionalSquareArt" -> if (fallbackImgUrl == null) fallbackImgUrl = uri
                        }
                    }
                }

                val bestCoverUrl = posterUrl ?: boxArtUrl ?: superHeroUrl ?: fallbackImgUrl
                val bestHeroUrl = superHeroUrl ?: posterUrl ?: boxArtUrl ?: fallbackImgUrl
                if (bestCoverUrl.isNullOrBlank()) continue

                val props = prod.optJSONObject("Properties")
                val rawCat = props?.optString("Category", "")?.takeIf { it.isNotBlank() }
                    ?: props?.optJSONArray("Categories")?.optString(0, "")
                    ?: ""
                val (fallbackVi, fallbackEn) = if (rawCat.isNotBlank()) mapCategoryBilingual(rawCat) else ("" to "")
                val (catVi, catEn) = resolveCategoryBilingual(productId, fallbackVi, fallbackEn)

                var ratingStr = "4.8"
                val usageData = prod.optJSONArray("MarketProperties")
                    ?.optJSONObject(0)
                    ?.optJSONArray("UsageData")
                if (usageData != null) {
                    for (u in 0 until usageData.length()) {
                        val uObj = usageData.optJSONObject(u) ?: continue
                        val avg = uObj.optDouble("AverageRating", 0.0)
                        if (avg >= 3.5) {
                            ratingStr = String.format(Locale.US, "%.1f", avg.coerceIn(4.2, 4.9))
                            break
                        }
                    }
                }

                val isFortnite = productId.equals(FORTNITE_PRODUCT_ID, ignoreCase = true) ||
                    cleanTitle.equals("Fortnite", ignoreCase = true)
                val slug = if (isFortnite) "fortnite" else slugifyTitle(cleanTitle)
                val playUrl = "https://www.xbox.com/en-US/play/games/$slug/$productId"
                val isFree = isFreeToPlayTitle(cleanTitle, productId, playUrl)
                val isTouch = productId in touchIds || isTouchSupportedTitle(productId, playUrl)
                val isPop = productId in popularIds || isPopularTitle(productId, playUrl)
                val isRecent = productId in recentIds || isRecentlyAddedTitle(productId, playUrl)
                val accent = ACCENT_PALETTE[(cleanTitle.hashCode() and 0x7FFFFFFF) % ACCENT_PALETTE.size]

                val badgeVi = when {
                    isFortnite -> "Cảm ứng Epic • Miễn phí"
                    isFree -> "Miễn phí • Cloud"
                    isRecent -> "Mới lên kệ • Cloud"
                    isPop -> "Top Hot • Xbox"
                    isTouch -> "Cảm ứng • Cloud"
                    else -> "Game Pass"
                }
                val badgeEn = when {
                    isFortnite -> "Epic Touch • Free"
                    isFree -> "Free • Cloud"
                    isRecent -> "New on Cloud"
                    isPop -> "Popular • Xbox"
                    isTouch -> "Touch • Cloud"
                    else -> "Game Pass"
                }

                results.add(
                    GameBookmark(
                        id = if (isFortnite) "fortnite" else "xbox_${productId.lowercase(Locale.ROOT)}",
                        title = cleanTitle,
                        category = catVi,
                        categoryEn = catEn,
                        playUrl = playUrl,
                        freeToPlay = isFree,
                        supportsTouch = isTouch,
                        accentColorHex = accent,
                        badge = badgeVi,
                        badgeEn = badgeEn,
                        drawableResId = null,
                        rating = ratingStr,
                        imageUrl = bestCoverUrl,
                        heroImageUrl = bestHeroUrl,
                        description = shortDesc,
                        publisher = publisherName,
                        isPopular = isPop,
                        isRecentlyAdded = isRecent
                    )
                )
            }
            results
        } finally {
            conn.disconnect()
        }
    }

    private fun normalizeImageUrl(raw: String?, widthPx: Int = 260, quality: Int = 70): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val httpsUrl = when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.removePrefix("http://")
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> return null
        }
        return if (httpsUrl.contains("store-images.s-microsoft.com", ignoreCase = true) && !httpsUrl.contains("?")) {
            "$httpsUrl?w=$widthPx&q=$quality"
        } else {
            httpsUrl
        }
    }

    private fun mapCategoryBilingual(rawCategory: String): Pair<String, String> {
        val c = rawCategory.lowercase(Locale.ROOT)
        return when {
            c.contains("shooter") -> "Bắn súng" to "Shooter"
            c.contains("role playing") || c.contains("rpg") -> "Nhập vai (RPG)" to "RPG"
            c.contains("strategy") -> "Chiến thuật" to "Strategy"
            c.contains("simulation") -> "Mô phỏng" to "Simulation"
            c.contains("fighting") -> "Đối kháng" to "Fighting"
            c.contains("family") || c.contains("platformer") -> "Gia đình" to "Family"
            c.contains("indie") -> "Indie" to "Indie"
            else -> "Hành động & Phiêu lưu" to "Action & Adventure"
        }
    }

    private fun loadGamesArrayFromPrefs(sharedPrefs: SharedPreferences, key: String): List<GameBookmark> {
        val rawJson = try {
            sharedPrefs.getString(key, null)
        } catch (_: Exception) {
            null
        } ?: return emptyList()
        return parseGamesJsonArray(rawJson)
    }

    private fun writeTextAtomically(targetFile: java.io.File, content: String) {
        synchronized(cacheFileLock) {
            val parent = targetFile.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val tmpFile = java.io.File(parent ?: targetFile.parentFile, "${targetFile.name}.tmp")
            tmpFile.writeText(content, Charsets.UTF_8)
            if (!tmpFile.renameTo(targetFile)) {
                targetFile.writeText(content, Charsets.UTF_8)
                tmpFile.delete()
            }
        }
    }

    private fun loadSyncedGamesFromFileOrMigratePrefs(sharedPrefs: SharedPreferences): List<GameBookmark> {
        val cacheFile = syncedGamesCacheFile
        if (cacheFile != null && cacheFile.exists() && cacheFile.length() > 2L) {
            val fileJson = try {
                synchronized(cacheFileLock) {
                    cacheFile.readText(Charsets.UTF_8)
                }
            } catch (_: Exception) {
                null
            }
            if (!fileJson.isNullOrBlank()) {
                val parsed = parseGamesJsonArray(fileJson)
                if (parsed.isNotEmpty()) {
                    if (sharedPrefs.contains(KEY_SYNCED_GAMES_JSON)) {
                        sharedPrefs.edit().remove(KEY_SYNCED_GAMES_JSON).apply()
                    }
                    return parsed
                }
            }
        }
        val legacyJson = try {
            sharedPrefs.getString(KEY_SYNCED_GAMES_JSON, null)
        } catch (_: Exception) {
            null
        }
        if (!legacyJson.isNullOrBlank()) {
            try {
                if (cacheFile != null) {
                    writeTextAtomically(cacheFile, legacyJson)
                }
                sharedPrefs.edit().remove(KEY_SYNCED_GAMES_JSON).apply()
            } catch (_: Exception) {
            }
            return parseGamesJsonArray(legacyJson)
        }
        return emptyList()
    }

    private fun parseGamesJsonArray(rawJson: String): List<GameBookmark> {
        return try {
            val arr = JSONArray(rawJson)
            val list = ArrayList<GameBookmark>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                val playUrl = obj.optString("playUrl", "").trim()
                if (title.isBlank() || playUrl.isBlank()) continue
                val id = obj.optString("id", "custom_$i")
                val isCustom = id.startsWith("custom_")
                val pid = extractProductId(playUrl)
                val isFortnite = id == "fortnite" || pid.equals(FORTNITE_PRODUCT_ID, ignoreCase = true) || title.equals("Fortnite", ignoreCase = true)
                val rawCat = obj.optString("category", "Hành động & Phiêu lưu")
                val rawCatEn = obj.optString("categoryEn", rawCat)
                val (catVi, catEn) = if (isCustom) {
                    rawCat to rawCatEn
                } else {
                    resolveCategoryBilingual(pid, rawCat, rawCatEn)
                }
                val rawBadge = obj.optString("badge", "").takeIf { it.isNotBlank() }
                val badge = rawBadge?.let { b ->
                    if (b.contains("120", ignoreCase = true)) "Cảm ứng Epic • Miễn phí" else b
                }
                val rawBadgeEn = obj.optString("badgeEn", badge ?: "").takeIf { it.isNotBlank() }
                val badgeEn = rawBadgeEn?.let { b ->
                    if (b.contains("120", ignoreCase = true)) "Epic Touch • Free" else b
                }
                val imgUrl = normalizeImageUrl(obj.optString("imageUrl", ""), widthPx = 260, quality = 70)
                val heroUrl = normalizeImageUrl(obj.optString("heroImageUrl", ""), widthPx = 520, quality = 70) ?: imgUrl
                val isCustomFree = isCustom && obj.optBoolean("freeToPlay", false)
                val isTouch = if (isCustom) obj.optBoolean("supportsTouch", false) else isTouchSupportedTitle(pid, playUrl)
                val isRecent = if (isCustom) false else isRecentlyAddedTitle(pid, playUrl)
                val isPop = if (isCustom) false else isPopularTitle(pid, playUrl)
                list.add(
                    GameBookmark(
                        id = if (isFortnite) "fortnite" else id,
                        title = title,
                        category = catVi,
                        categoryEn = catEn,
                        playUrl = playUrl,
                        freeToPlay = isFreeToPlayTitle(title, pid, playUrl) || isCustomFree,
                        supportsTouch = isTouch,
                        accentColorHex = obj.optLong("accentColorHex", 0xFF107C10L),
                        badge = badge,
                        badgeEn = badgeEn,
                        drawableResId = null,
                        rating = obj.optString("rating", "4.8"),
                        imageUrl = imgUrl,
                        heroImageUrl = heroUrl,
                        description = obj.optString("description", "").takeIf { it.isNotBlank() },
                        publisher = obj.optString("publisher", "").takeIf { it.isNotBlank() },
                        isPopular = isPop,
                        isRecentlyAdded = isRecent
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveCustomGames(allGames: List<GameBookmark>) {
        val sharedPrefs = prefs ?: return
        val customGames = allGames.filter { it.id.startsWith("custom_") }
        saveGamesListToPrefs(sharedPrefs, KEY_CUSTOM_GAMES_JSON, customGames)
    }

    private fun saveSyncedGames(allGames: List<GameBookmark>) {
        if (!initialDiskLoadCompleted) return
        val syncedGames = allGames
            .filter { !it.id.startsWith("custom_") && !it.imageUrl.isNullOrBlank() }
            .take(660)
        if (syncedGames.isEmpty()) return
        ioScope.launch {
            val cacheFile = syncedGamesCacheFile
            val finalToSave = if (syncedGames.size < 100 && cacheFile != null && cacheFile.exists() && cacheFile.length() > 2L) {
                val existingOnDisk = try {
                    val fileJson = synchronized(cacheFileLock) {
                        cacheFile.readText(Charsets.UTF_8)
                    }
                    parseGamesJsonArray(fileJson)
                } catch (_: Exception) {
                    emptyList()
                }
                (syncedGames + existingOnDisk)
                    .filter { !it.id.startsWith("custom_") && !it.imageUrl.isNullOrBlank() }
                    .distinctBy { extractProductId(it.playUrl).ifBlank { it.id } }
                    .take(660)
            } else {
                syncedGames
            }
            val jsonStr = serializeGamesListToJson(finalToSave) ?: return@launch
            if (cacheFile != null) {
                try {
                    writeTextAtomically(cacheFile, jsonStr)
                    prefs?.let { sp ->
                        if (sp.contains(KEY_SYNCED_GAMES_JSON)) {
                            sp.edit().remove(KEY_SYNCED_GAMES_JSON).apply()
                        }
                    }
                    return@launch
                } catch (_: Exception) {
                }
            }
            prefs?.edit()?.putString(KEY_SYNCED_GAMES_JSON, jsonStr)?.apply()
        }
    }

    private fun saveGamesListToPrefs(sharedPrefs: SharedPreferences, key: String, gamesList: List<GameBookmark>) {
        val jsonStr = serializeGamesListToJson(gamesList) ?: return
        sharedPrefs.edit().putString(key, jsonStr).apply()
    }

    private fun serializeGamesListToJson(gamesList: List<GameBookmark>): String? {
        return try {
            val arr = JSONArray()
            for (game in gamesList) {
                val obj = JSONObject().apply {
                    put("id", game.id)
                    put("title", game.title)
                    put("category", game.category)
                    put("categoryEn", game.categoryEn)
                    put("playUrl", game.playUrl)
                    put("freeToPlay", game.freeToPlay)
                    put("supportsTouch", game.supportsTouch)
                    put("accentColorHex", game.accentColorHex)
                    put("badge", game.badge ?: "")
                    put("badgeEn", game.badgeEn ?: "")
                    put("rating", game.rating)
                    put("imageUrl", game.imageUrl ?: "")
                    put("heroImageUrl", game.heroImageUrl ?: "")
                    put("description", game.description ?: "")
                    put("publisher", game.publisher ?: "")
                    put("isPopular", game.isPopular)
                    put("isRecentlyAdded", game.isRecentlyAdded)
                }
                arr.put(obj)
            }
            arr.toString()
        } catch (_: Exception) {
            null
        }
    }

    fun addCustomGame(title: String, url: String) {
        if (title.isBlank() || url.isBlank()) return
        val pid = extractProductId(url)
        val newGame = GameBookmark(
            id = "custom_${System.currentTimeMillis()}",
            title = title.trim(),
            category = "Game tùy chỉnh",
            categoryEn = "Custom Game",
            playUrl = url.trim(),
            freeToPlay = isFreeToPlayTitle(title, pid, url),
            supportsTouch = isTouchSupportedTitle(pid, url),
            accentColorHex = 0xFF00E676,
            badge = "Tự thêm",
            badgeEn = "Custom",
            rating = "5.0"
        )
        val updated = listOf(newGame) + _games.value
        _games.value = updated
        saveCustomGames(updated)
        if (pid.isNotBlank()) {
            fetchSingleProductMetadataAsync(pid)
        }
    }

    fun removeCustomGame(gameId: String) {
        val updated = _games.value.filterNot { it.id == gameId }
        _games.value = updated
        saveCustomGames(updated)
    }
}



