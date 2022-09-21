package teleblock.util;

import android.content.Context;
import android.os.Bundle;

import com.flurry.android.FlurryAgent;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.hjq.http.EasyHttp;
import com.hjq.http.lifecycle.ApplicationLifecycle;

import java.util.HashMap;
import java.util.Map;

import teleblock.manager.LoginManager;
import teleblock.network.api.AppTrackApi;
import timber.log.Timber;

/**
 * Time:2022/7/6
 * Author:Perry
 * Description：数据埋点
 */
public class EventUtil {

    public enum Even {
                第一次打开("first_open"),
        欢迎页展示("start_page_show"),
        欢迎页开始按钮点击("start_button_click"),
        欢迎页注册按钮点击("register_button_click"),
        电话号码页面展示("phone_number_show"),
        电话号码页面下一步("phone_number_next"),
        确认验证码页面展示("check_code_show"),
        Telegram新用户完善资料("telegram_new_user_info"),
        app聊天页面展示("chat_page_show"),

                侧边无痕模式启用("no_read_open"),
        登录无痕模式启用("no_read_login_open"),
        聊天无痕模式启用("no_read_chat_open"),
        聊天翻译("translate_chat"),
        清除消息点击("numberclean_click"),
        搜索点击("home_search_click"),
        加号点击("topbar_more_click"),

                好友点击("friends_message_click"),
        与我相关点击("aboutme_click"),
        非联系人点击("not_contact_click"),
        好工具点击("tool_click"),
        好工具清理缓存点击("tool_cleancache_click"),
        好工具扫一扫("tool_scan_click"),
        好工具邀请好友("tool_invite_click"),
        好工具消息分组("tool_folder_click"),
        好工具官方群("tool_official_group_click"),
        好工具官方频道("tool_official_channel_click"),
        信息分组加号点击("folder_add_click"),

                频道点击("channel_tab_click"),
        联系人点击("contacts_tab_click"),
        setting点击("settings_tab_click"),

                频道页面展示("channel_tab_show"),
        频道点赞click("channel_tab_like_click"),
        频道转发click("channel_tab_send_click"),
        频道评论click("channel_tab_comment_click"),

                好评弹窗展示("comment_dialog_show"),

                表情包收藏点击("sticker_collect"),
        表情包收藏界面展示("sticker_collect_page_show"),
        表情包输入框星星点击("sticker_collect_text_click"),
        表情包收藏界面发送("sticker_collect_page_send"),

                电话号码页语言选择("phonenumber_language_click"),
        侧边栏语言点击("menu_language_click"),
        ;

        public String eventId;

        Even(String eventId) {
            this.eventId = eventId;
        }
    }

    /**
     * 埋点
     *
     * @param context
     * @param data
     */
    public static void track(Context context, Even event, Map<String, Object> data) {
        try {
            if (data == null) data = new HashMap<>();
            if (!data.containsKey("uid")) {
                data.put("uid", LoginManager.getUserId());
                data.put("token", LoginManager.getUserToken() + "");
            }

            Timber.tag("TrackEvent").d(event.name() + "(" + event.eventId + ")" + " ->" + JsonUtil.parseObjToJson(data));

                        Map<String, String> map = new HashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                map.put(entry.getKey(), (String) entry.getValue());
            }
            FlurryAgent.logEvent(event.eventId, map);

                        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
            Bundle bundle = new Bundle();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                bundle.putString(entry.getKey(), (String) entry.getValue());
            }
            mFirebaseAnalytics.logEvent(event.eventId, bundle);

                        String name = event.name();
            String key = event.eventId;
            String dataJson = new Gson().toJson(data);
            EasyHttp.post(new ApplicationLifecycle())
                    .api(new AppTrackApi()
                            .setKey(key)
                            .setName(name)
                            .setEvent_key(key)
                            .setEvent_name(name)
                            .setData(dataJson))
                    .request(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
