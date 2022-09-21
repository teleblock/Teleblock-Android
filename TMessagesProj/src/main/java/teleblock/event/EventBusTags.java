/*
 * Copyright 2017 JessYan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package teleblock.event;


public interface EventBusTags {

    // 用户信息已更改
    String USER_INFO_CHANGED = "user_info_changed";
    // 无痕模式已更改
    String STEALTH_MODE_CHANGED = "stealth_mode_changed";
    // 标记为已读
    String MARK_AS_READ = "mark_as_read";
    // 标记为未读
    String MARK_AS_UNREAD = "mark_as_unread";
    // 更新未读数
    String UPDATE_UNREAD_COUNT = "update_unread_count";
    // 更新对话数据
    String UPDATE_DIALOGS_DATA = "update_dialogs_data";
    // 扫一扫
    String OPEN_CAMERA_SCAN = "open_camera_scan";
    // 清除缓存成功
    String CLEAR_CACHE_OK = "clear_cache_ok";
    // 预览主题
    String PRE_THEME_VIEW = "pre_theme_view";
    // 取消弹窗
    String DISMISS_DIALOG = "dismiss_dialog";
    // 显示弹窗
    String SHOW_DIALOG = "show_dialog";
    // 打开侧边栏
    String OPEN_DRAWER = "open_drawer";
    // 侧边栏状态已改变
    String DRAWER_STATE_CHANGED = "drawer_state_changed";
    // 钱包连接已批准
    String WALLET_CONNECT_APPROVED = "wallet_connect_approved";
    // 钱包连接已断开
    String WALLET_CONNECT_CLOSED = "wallet_connect_closed";
    // 上传用户头像
    String UPLOAD_USER_PROFILE = "upload_user_profile";

    String COLLECT_CHANGE = "collect_change";

    String DELETE_VIDEO_OK = "delete_video_ok";

    String DELETE_VIDEO_ITEM = "delete_video_item";

    String DELETE_VIDEO_SELECT = "delete_video_select";

    String VIDEO_SAVE_GALLERY = "video_save_gallery";

    String CHANNEL_WITH_TAG_REFRASH = "channel_with_tag_refrash";

    String CHANNEL_TAG_REFRASH = "channel_tag_refrash";

    //推荐筛选ids
    String RECOMMEND_IDS = "recommend_ids";

}
