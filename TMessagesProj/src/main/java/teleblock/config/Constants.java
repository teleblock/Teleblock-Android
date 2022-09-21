package teleblock.config;

import com.blankj.utilcode.util.LanguageUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;

import java.util.Arrays;

import teleblock.model.ui.ToolsTabData;
import teleblock.util.ManifestUtil;

/**
 * Created by LSD on 2021/3/22.
 * Desc
 */
public class Constants {
    //需要显示广告的渠道
    public static boolean showOnlineAd() {
        String channel = ManifestUtil.getChannel(ApplicationLoader.applicationContext);
        String[] adChannel = new String[]{"official", "official_video", "chinese_dianbao_zhifeiji"};//chinese_kanpian2新版，不要广告  "chinese_kanpian":被下架了
        if (Arrays.asList(adChannel).contains(channel)) {
            return !UserConfig.getInstance(UserConfig.selectedAccount).vip;
        }
        return false;
    }

    public static boolean hideView() {
        String channel = ManifestUtil.getChannel(ApplicationLoader.applicationContext);
        String[] adChannel = new String[]{"official", "official_video", "chinese_dianbao2", "nicegram", "chinese_dianbao_zhifeiji", "teleblock"};
        if (Arrays.asList(adChannel).contains(channel)) {
            return true;
        }
        return false;
    }

    public static boolean hideBotHandlerButton() {
        String channel = ManifestUtil.getChannel(ApplicationLoader.applicationContext);
        String[] adChannel = new String[]{"official", "official_video", "nicegram", "teleblock"};
        if (Arrays.asList(adChannel).contains(channel)) {
            return true;
        }
        return false;
    }

    /**
     * 官方频道
     */
    public static String getOfficialChannel() {
        String channel = ManifestUtil.getChannel(ApplicationLoader.applicationContext);
        if ("nicegram".equals(channel)) return "https://t.me/nicegramchannel";
        if (LanguageUtils.getSystemLanguage().getLanguage().equals("zh")) {
            return "https://t.me/TeleblockAppChannel";
        } else {
            return "https://t.me/TeleblockChannel";
        }
    }

    /**
     * 官方群组
     */
    public static String getOfficialGroup() {
        String channel = ManifestUtil.getChannel(ApplicationLoader.applicationContext);
        if ("nicegram".equals(channel)) return "https://t.me/nicegramgroup";
        if (LanguageUtils.getSystemLanguage().getLanguage().equals("zh")) {
            return "https://t.me/TeleblockApp";
        } else {
            return "https://t.me/TeleblockGroup";
        }
    }

    public static void init() {
        if (AppConfig.DEBUG) {
            String channel = ManifestUtil.getChannel(ApplicationLoader.applicationContext);
            if ("official_video".equals(channel)) { // rush_tg
                AppConfig.AD_UNIT.SLIP_FEED_NATIVE_ID = "ca-app-pub-3749632185254636/4797708875";
                AppConfig.AD_UNIT.CHANNEL_FEED_NATIVE_ID = "ca-app-pub-3749632185254636/3017723347";
                AppConfig.AD_UNIT.BOTTOM_BANNER_ID = "ca-app-pub-3749632185254636/6531719371";
            } else if ("chinese_dianbao_zhifeiji".equals(channel)) { // 纸飞机
                AppConfig.AD_UNIT.SLIP_FEED_NATIVE_ID = "ca-app-pub-5580211019129351/1713285774";
                AppConfig.AD_UNIT.CHANNEL_FEED_NATIVE_ID = "ca-app-pub-5580211019129351/6390897384";
                AppConfig.AD_UNIT.BOTTOM_BANNER_ID = "ca-app-pub-5580211019129351/9629899271";
            } else if ("nicegram".equals(channel)) { // nicegram
                AppConfig.AD_UNIT.SLIP_FEED_NATIVE_ID = "ca-app-pub-5580211019129351/1444921955";
                AppConfig.AD_UNIT.CHANNEL_FEED_NATIVE_ID = "ca-app-pub-5580211019129351/6389809642";
                AppConfig.AD_UNIT.BOTTOM_BANNER_ID = "ca-app-pub-5580211019129351/9610869359";
            }
        }
    }
}
