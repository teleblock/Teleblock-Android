package teleblock.util;

import android.text.TextUtils;

import com.blankj.utilcode.util.ResourceUtils;
import com.tencent.mmkv.MMKV;

import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import teleblock.config.AppConfig;
import teleblock.model.SystemEntity;
import teleblock.model.wallet.NFTInfo;
import teleblock.model.wallet.WalletInfo;

public class MMKVUtil {

    private static MMKV kv;

    public static String getKeyWithUser(String key) {
        StringBuilder stringBuilder = new StringBuilder(key);
        TLRPC.User currentUser = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
        if (currentUser != null && !TextUtils.isEmpty(currentUser.phone)) {
            stringBuilder.append(currentUser.phone);
        }
        return stringBuilder.toString();
    }

    private static MMKV getMMKV() {
        if (kv == null) {
            kv = MMKV.defaultMMKV();
        }
        return kv;
    }

    public static Boolean saveValue(String key, Object value) {
        if (value instanceof String) {
            return getMMKV().encode(key, (String) value);
        } else if (value instanceof Integer) {
            return getMMKV().encode(key, (Integer) value);
        } else if (value instanceof Float) {
            return getMMKV().encode(key, (Float) value);
        } else if (value instanceof Boolean) {
            return getMMKV().encode(key, (Boolean) value);
        } else if (value instanceof Long) {
            return getMMKV().encode(key, (Long) value);
        } else {
            return getMMKV().encode(key, value.toString());
        }
    }

    public static String getString(String key) {
        return getMMKV().decodeString(key, "");
    }

    public static Integer getInt(String key) {
        return getMMKV().decodeInt(key, 0);
    }

    public static Float getFloat(String key) {
        return getMMKV().decodeFloat(key, 0.0f);
    }

    public static Boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public static Boolean getBoolean(String key, boolean defaultValue) {
        return getMMKV().decodeBool(key, defaultValue);
    }

    public static Long getLong(String key) {
        return getMMKV().decodeLong(key, 0L);
    }

    public static void removeValue(String key) {
        getMMKV().removeValueForKey(key);
    }

    public static void clearAll() {
        getMMKV().clearAll();
    }


    /**********************************************************************************************/

    /**
     * 成员人数统计
     *
     * @param map
     */
    public static void setMemberCounts(Map<Long, Integer> map) {
        saveValue(getKeyWithUser(AppConfig.MkKey.MEMBER_COUNTS), JsonUtil.parseObjToJson(map));
    }

    public static Map<Long, Integer> getMemberCounts() {
        String json = getString(getKeyWithUser(AppConfig.MkKey.MEMBER_COUNTS));
        return JsonUtil.parseJsonToMap(json, Long.class, Integer.class);
    }

    /**
     * 存储系统配置信息
     *
     * @param data
     */
    public static void setSystemMsg(SystemEntity data) {
        saveValue(AppConfig.MkKey.SYSTEM_MSG, JsonUtil.parseObjToJson(data));
    }

    public static SystemEntity getSystemMsg() {
        String json = getString(AppConfig.MkKey.SYSTEM_MSG);
        if (TextUtils.isEmpty(json)) {
            //TODO: 2022/8/12  发布前记得改Pro数据配置
            if (BuildConfig.DEBUG) {
                json = ResourceUtils.readAssets2String("sysconfig/dev.json");
            } else {
                json = ResourceUtils.readAssets2String("sysconfig/pro.json");
            }
        }
        return JsonUtil.parseJsonToBean(json, SystemEntity.class);
    }

    /**
     * 设置视频播放速度
     *
     * @param speed
     */
    public static void setPlaySpeed(float speed) {
        saveValue(AppConfig.MkKey.PLAY_SPEED, speed);
    }

    /**
     * 获取视频播放速度
     */
    public static Float getPlaySpeed() {
        float speed = getFloat(AppConfig.MkKey.PLAY_SPEED);
        if (speed == 0.0f) {
            speed = 1.0f;
        }
        return speed;
    }

    /**
     * 最后一个播放的id
     *
     * @param dialogId
     * @param id
     */
    public static void setLastPlayId(long dialogId, int id) {
        saveValue(AppConfig.MkKey.LAST_PLAY_ID, id);
    }

    public static int getLastPlayId(long dialogId) {
        return getInt(AppConfig.MkKey.LAST_PLAY_ID);
    }

    /**
     * 最后一个视频来着dialog
     *
     * @param currentDialog
     * @param dialogId
     */
    public static void setLastPlayItemDialogId(long currentDialog, long dialogId) {
        saveValue(AppConfig.MkKey.LAST_PLAY_ITEM_DIALOGID, dialogId);
    }

    public static long getLastPlayItemDialogId(long currentDialog) {
        return getLong(AppConfig.MkKey.LAST_PLAY_ITEM_DIALOGID);
    }

    public static void startRegister(boolean b) {
        saveValue("startRegister", b);
    }

    public static boolean startRegister() {
        return getBoolean("startRegister");
    }

    /**
     * firstLogin
     *
     * @param login
     */
    public static void firstLogin(boolean login) {
        saveValue("firstLogin", login);
    }

    public static boolean firstLogin() {
        return getBoolean("firstLogin", true);
    }

    /***
     * 不允许任何人看我的手机号码
     * @param set
     */
    public static void disallowAllSeePhone(boolean set) {
        saveValue(getKeyWithUser("disallowAllSeePhone"), set);
    }

    public static boolean disallowAllSeePhone() {
        return getMMKV().decodeBool(getKeyWithUser("disallowAllSeePhone"), true);
    }

    /**
     * 任何人看我的手机号码设置是否应用
     *
     * @param b
     */
    public static void seePhoneApply(boolean b) {
        saveValue(getKeyWithUser("seePhoneApply"), b);
    }

    public static boolean seePhoneApply() {
        return getBoolean(getKeyWithUser("seePhoneApply"));
    }

    public static void setTranslateCode(String TranslateCode) {
        saveValue("TranslateCode", TranslateCode);
    }

    public static String getTranslateCode() {
        return getString("TranslateCode");
    }

    /**
     * 是否开启全局无痕模式
     *
     * @return
     */
    public static boolean isOpenStealthMode() {
        return getMMKV().decodeBool(getKeyWithUser("StealthMode"));
    }

    public static void setStealthMode(boolean stealthMode) {
        saveValue(getKeyWithUser("StealthMode"), stealthMode);
    }

    /**
     * 是否tg新用户
     *
     * @return
     */
    public static boolean telegramNewUser() {
        return getBoolean(getKeyWithUser("telegramNewUser"));
    }

    public static void telegramNewUser(boolean stealthMode) {
        saveValue(getKeyWithUser("telegramNewUser"), stealthMode);
    }

    /**
     * 登录时选择的语言
     *
     * @return
     */
    public static int loginSelectLanguage() {
        return getMMKV().decodeInt("loginSelectLanguage", -1);
    }

    public static void loginSelectLanguage(int value) {
        saveValue("loginSelectLanguage", value);
    }

    /**
     * 最后一个视频的发布时间
     *
     * @param dialogId
     * @param time
     */
    public static void setLastPlayItemTime(long dialogId, long time) {
        saveValue(AppConfig.MkKey.LAST_PLAY_ITEM_TIME, time);
    }

    public static long getLastPlayItemTime(long dialogId) {
        return getLong(AppConfig.MkKey.LAST_PLAY_ITEM_TIME);
    }

    /**
     * 是否不再显示评价
     *
     * @return
     */
    public static boolean isNeverEvaluate() {
        return getBoolean(AppConfig.MkKey.NEVER_EVALUATE);
    }

    public static void setNeverEvaluate(boolean neverEvaluate) {
        saveValue(AppConfig.MkKey.NEVER_EVALUATE, neverEvaluate);
    }

    /**
     * app跳转google商店评论显示配置数据
     *
     * @param evaluateApp
     */
    public static void setEvaluateApp(String evaluateApp) {
        saveValue(AppConfig.MkKey.EVALUATE_APP_ENTITY, evaluateApp);
    }

    public static String getEvaluateApp() {
        String json = getString(AppConfig.MkKey.EVALUATE_APP_ENTITY);
        if (json.isEmpty()) {
            return "{}";
        }
        return json;
    }

    /**
     * 聊天翻译开关
     *
     * @return
     */
    public static boolean chatTranslationSwitch() {
        return getMMKV().decodeBool(getKeyWithUser("chatTranslationSwitch"), true);
    }

    public static void chatTranslationSwitch(boolean mSwitch) {
        saveValue(getKeyWithUser("chatTranslationSwitch"), mSwitch);
    }

    /**
     * 是否添加过默认分组
     */
    public static boolean addedDefaultFilters() {
        return getBoolean(getKeyWithUser("addedDefaultFilters"));
    }

    public static void addedDefaultFilters(boolean b) {
        saveValue(getKeyWithUser("addedDefaultFilters"), b);
    }

    /**
     * 连接的钱包地址
     */
    public static String connectedWalletAddress() {
        return getString(getKeyWithUser("connectedWalletAddress"));
    }

    public static void connectedWalletAddress(String s) {
        saveValue(getKeyWithUser("connectedWalletAddress"), s);
    }

    /**
     * 加入置顶开关
     */
    public static boolean ifOpenTopping() {
        return getBoolean(getKeyWithUser(AppConfig.MkKey.IF_OPEN_TOPPING), true);
    }

    public static void setIfOpenTopping(boolean b) {
        saveValue(getKeyWithUser(AppConfig.MkKey.IF_OPEN_TOPPING), b);
    }

    /**
     * 加入归档列表开关
     */
    public static boolean ifOpenArchive() {
        return getBoolean(getKeyWithUser(AppConfig.MkKey.IF_OPEN_ARCHIVE), true);
    }

    public static void setIfOpenArchive(boolean b) {
        saveValue(getKeyWithUser(AppConfig.MkKey.IF_OPEN_ARCHIVE), b);
    }

    /**
     * 群人数筛选开关
     */
    public static boolean ifOpenPeopleFilter() {
        return getBoolean(getKeyWithUser(AppConfig.MkKey.IF_OPEN_PEOPLE_FILTER), true);
    }

    public static void setIfOpenPeopleFilter(boolean b) {
        saveValue(getKeyWithUser(AppConfig.MkKey.IF_OPEN_PEOPLE_FILTER), b);
    }

    /**
     * 群人数筛选
     */
    public static int groupFilterPeopleNum() {
        int num = getInt(getKeyWithUser(AppConfig.MkKey.PEOPLE_FILTER_NUM));
        if (num != 0) {
            return num;
        } else {
            return 30;
        }
    }

    public static void setGroupFilterPeopleNum(int num) {
        saveValue(getKeyWithUser(AppConfig.MkKey.PEOPLE_FILTER_NUM), num);
    }

    /**
     * 黑名单列表
     *
     * @return
     */
    public static List<Long> blackChatList() {
        String json = getString(getKeyWithUser(AppConfig.MkKey.BLACK_CHAT_ID_LIST));
        if (json.isEmpty()) {
            return new ArrayList<>();
        } else {
            return JsonUtil.parseJsonToList(json, Long.class);
        }
    }

    public static void setBlackChatList(List<Long> chatIds) {
        saveValue(getKeyWithUser(AppConfig.MkKey.BLACK_CHAT_ID_LIST), JsonUtil.parseObjToJson(chatIds));
    }

    /**
     * 白名单列表
     */
    public static List<Long> whiteChatList() {
        String json = getString(getKeyWithUser(AppConfig.MkKey.WHITE_CHAT_ID_LIST));
        if (json.isEmpty()) {
            return new ArrayList<>();
        } else {
            return JsonUtil.parseJsonToList(json, Long.class);
        }
    }

    public static void setWhiteChatList(List<Long> chatIds) {
        saveValue(getKeyWithUser(AppConfig.MkKey.WHITE_CHAT_ID_LIST), JsonUtil.parseObjToJson(chatIds));
    }

    /**
     * 钱包相关信息
     */
    public static void setWalletInfo(WalletInfo data) {
        saveValue(getKeyWithUser("walletInfo"), JsonUtil.parseObjToJson(data));
    }

    public static WalletInfo getWalletInfo() {
        String json = getString(getKeyWithUser("walletInfo"));
        return JsonUtil.parseJsonToBean(json, WalletInfo.class);
    }
}
