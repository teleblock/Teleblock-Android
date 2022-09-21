package teleblock.config;


import org.telegram.messenger.BuildConfig;

/**
 * Time:2022/6/20
 * Author:Perry
 * Description：app配置信息
 */
public interface AppConfig {

    String LOG_TAG = "Telegram";

    boolean DEBUG = BuildConfig.DEBUG;

    int OS_TYPE = 2;

    class NetworkConfig {
        //请求Host
        public static String API_BASE_URL = DEBUG ?  "https://testing.teleblock.io/api/v1" : "https://api.teleblock.io/api/v1";
        //加密name
        public final static String WG_NAME = "teleblock";
        //加密KEY
        public final static String WG_KEY = "ERFEijdfyueysttg493jdf33DEUJEe4943djfEJRREivideoldruere";
    }

    class MkKey {
        //成员人数
        public final static String MEMBER_COUNTS = "member_counts";
        //设备号
        public final static String DEVICE_ID = "device_id";
        //区号
        public final static String COUNTRY_CODE = "country_code";
        //sim识别码
        public final static String SIM_CODE = "sim_code";
        //登录返回数据
        public final static String LOGIN_DATA = "login_data";
        //关键词
        public final static String COIN_KEYWORDS = "coin_Keywords";
        //货币数据
        public final static String COIN_LIST = "coin_list";
        //系统配置数据
        public final static String SYSTEM_MSG = "system_msg";
        //视频播放速度
        public final static String PLAY_SPEED = "play_speed";
        //最后一个播放的id
        public final static String LAST_PLAY_ID = "last_play_id";
        //最后一个视频来着dialog
        public final static String LAST_PLAY_ITEM_DIALOGID = "last_play_item_dialogid";
        //最后一个视频的发布时间
        public final static String LAST_PLAY_ITEM_TIME = "last_play_item_time";
        //是否显示用户评价弹窗
        public final static String NEVER_EVALUATE = "never_evaluate";
        //app跳转google商店评论显示配置数据
        public final static String EVALUATE_APP_ENTITY = "evaluate_app_entity";
        //是否显示清除未读消息弹窗
        public final static String SHOW_CLEAR_UNREADMSG = "show_clear_unreadmsg";
        //上次登录的tgid
        public final static String LAST_LOGIN_TGID = "last_login_tgid";
        //是否开启置顶列表按钮
        public final static String IF_OPEN_TOPPING = "if_open_topping";
        //是否开启所有归档列表按钮
        public final static String IF_OPEN_ARCHIVE = "if_open_archive";
        //是否开启群组人数筛选按钮
        public final static String IF_OPEN_PEOPLE_FILTER = "if_open_people_filter";
        //群组人数筛选-默认30人
        public final static String PEOPLE_FILTER_NUM = "people_filter_num";
        //黑名单会话ID
        public final static String BLACK_CHAT_ID_LIST = "black_chat_id_list";
        //白名单会话ID
        public final static String WHITE_CHAT_ID_LIST = "white_chat_id_list";
    }


    //广告UNIT_ID支持服务器配置，本地缓存
    class AD_UNIT {
        //视频流原生广告
        public static String SLIP_FEED_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110";
        //channel信息流原生广告
        public static String CHANNEL_FEED_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110";
        //底部banner广告
        public static String BOTTOM_BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
        //通用banner广告
        public static String COMMON_BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    }

    class HomeTab {
        public static final int TAB_CHAT = 0;
        public static final int TAB_VIDEO = 1;
        public static final int TAB_CHANNEL = 2;
        public static final int TAB_CONTACT = 3;
        public static final int TAB_MY = 4;
    }

    public static void init() {
        if (DEBUG) {//forTest
            //NetworkConfig.API_BASE_URL = "https://api.teleblock.io/api/v1";
        }
    }
}
