package teleblock.ui.view;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.blankj.utilcode.util.LanguageUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;

import org.greenrobot.eventbus.EventBus;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.databinding.ViewUserPersonalBinding;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.AboutLinkCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.voip.VoIPHelper;
import org.telegram.ui.ContactAddActivity;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.ProfileActivity;

import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.ui.adapter.CommonChatsAdapter;
import timber.log.Timber;

/**
 * 创建日期：2022/6/9
 * 描述：用户简介-个人页
 */
public class UserPersonalView extends FrameLayout implements View.OnClickListener, OnItemClickListener {

    private ViewUserPersonalBinding binding;
    private ChatActivity fragment;
    private TLRPC.User user;
    private TLRPC.UserFull userInfo;
    private AboutLinkCell aboutLinkCell;
    private boolean loading;
    private CommonChatsAdapter commonChatsAdapter;

    public UserPersonalView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private void initView() {
        binding = ViewUserPersonalBinding.inflate(LayoutInflater.from(getContext()), this, true);
        Theme.createProfileResources(getContext());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        commonChatsAdapter = new CommonChatsAdapter();
        commonChatsAdapter.setOnItemClickListener(this);
        binding.recyclerView.setAdapter(commonChatsAdapter);

        binding.tvCloseDialog.setOnClickListener(this);
        binding.llUsername.setOnClickListener(this);
        binding.llProfileMessage.setOnClickListener(this);
        binding.llProfileVoice.setOnClickListener(this);
        binding.llProfileMainPage.setOnClickListener(this);
        binding.llProfileSecret.setOnClickListener(this);
        binding.llProfileAddFriend.setOnClickListener(this);
        binding.tvUserSearch.setOnClickListener(this);

        binding.tvMsg.setText(LocaleController.getString("user_personal_message", R.string.user_personal_message));
        binding.tvVoice.setText(LocaleController.getString("user_personal_voice", R.string.user_personal_voice));
        binding.tvMainPage.setText(LocaleController.getString("user_personal_main_page", R.string.user_personal_main_page));
        binding.tvCommune.setText(LocaleController.getString("user_personal_secret_chat", R.string.user_personal_secret_chat));
        binding.tvAddFriend.setText(LocaleController.getString("user_personal_add_friend", R.string.user_personal_add_friend));
        binding.tvIntroduction.setText(LocaleController.getString("user_personal_introduction", R.string.user_personal_introduction));
        binding.tvProfileExpand.setText(LocaleController.getString("fg_textview_expand", R.string.fg_textview_expand));
        binding.tvLastMsgDate.setText(LocaleController.getString("user_personal_last_msg_date", R.string.user_personal_last_msg_date));
        binding.tvOnlineTimeTitle.setText(LocaleController.getString("user_personal_online_time", R.string.user_personal_online_time));
        binding.tvCommonChats.setText(LocaleController.getString("user_personal_common_group", R.string.user_personal_common_group));
    }

    public void setData(ChatActivity fragment, TLRPC.User user, TLRPC.UserFull userInfo) {
        this.fragment = fragment;
        this.user = user;
        this.userInfo = userInfo;
        BackupImageView avatarImageView = new BackupImageView(getContext());
        avatarImageView.getImageReceiver().setRoundRadius(AndroidUtilities.dp(44));
        binding.flAvatar.removeAllViews();
        binding.flAvatar.addView(avatarImageView);
        AvatarDrawable avatarDrawable = new AvatarDrawable(user);
        avatarDrawable.setColor(Theme.getColor(Theme.key_avatar_backgroundInProfileBlue));
        avatarImageView.setForUserOrChat(user, avatarDrawable);
        String name = String.valueOf(Emoji.replaceEmoji(UserObject.getUserName(user), null, AndroidUtilities.dp(24), false));
        binding.tvFullName.setText(name);
        if (!TextUtils.isEmpty(user.username)) {
            binding.llUsername.setVisibility(VISIBLE);
            binding.tvUsername.setText("@" + user.username);
        }
        if (user.contact) {
            binding.ivAddFriend.setSelected(true);
            binding.tvAddFriend.setText(LocaleController.getString("user_personal_edit_friend", R.string.user_personal_edit_friend));
        }
        binding.tvOnlineTimeContent.setText(LocaleController.formatUserStatus(fragment.getCurrentAccount(), user));

        if (userInfo != null) {
            if (!TextUtils.isEmpty(userInfo.about)) {
                if (aboutLinkCell == null) {
                    aboutLinkCell = new AboutLinkCell(getContext(), fragment, true) {
                        @Override
                        protected void didPressUrl(String url) {
                            openUrl(url);
                            EventBus.getDefault().post(new MessageEvent(EventBusTags.DISMISS_DIALOG));
                        }

                        @Override
                        protected void didResizeEnd() {
                        }

                        @Override
                        protected void didResizeStart() {
                        }
                    };
                    binding.flProfileInfo.addView(aboutLinkCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.NO_GRAVITY, -2, 0, 0, 0));
                }
                aboutLinkCell.setText(userInfo.about, true);
            } else {
                binding.llProfileTitle.setVisibility(GONE);
            }
            fragment.getMediaDataController().searchMessagesInChat("", fragment.getDialogId(), fragment.getMergeDialogId(), fragment.getClassGuid(), 0, fragment.getThreadId(), false, user, null, false);
            getChats(0, 100);
        } else {
            queryServerSearch(name);
        }
    }

    private void queryServerSearch(String name) {
        long channelId = ChatObject.isChannel(fragment.getCurrentChat()) ? fragment.getCurrentChat().id : 0;
        if (channelId == 0) return;
        TLRPC.TL_channels_getParticipants req = new TLRPC.TL_channels_getParticipants();
        req.filter = new TLRPC.TL_channelParticipantsSearch();
        req.filter.q = name;
        req.limit = 10;
        req.offset = 0;
        req.channel = fragment.getMessagesController().getInputChannel(channelId);
        fragment.getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TLRPC.TL_channels_channelParticipants res = (TLRPC.TL_channels_channelParticipants) response;
                fragment.getMessagesController().putUsers(res.users, false);
                fragment.getMessagesController().putChats(res.chats, false);
//                for (TLRPC.ChannelParticipant participant:res.participants){
//                    if (participant.peer.user_id==user.id){
//                        Timber.i("加群时间-->"+LocaleController.formatJoined(participant.date));
//                    }
//                }
                // 重新请求之前报错的接口
                for (TLRPC.User user1 : res.users) {
                    if (user1.id == user.id) {
                        user = fragment.getMessagesController().getUser(user.id);
                        fragment.getMessagesController().loadFullUser(user, fragment.getClassGuid(), true);
                        break;
                    }
                }
            }
        }));
    }

    private void openUrl(String url) {
        if (url.startsWith("@")) {
            fragment.getMessagesController().openByUserName(url.substring(1), fragment, 0);
        } else if (url.startsWith("#")) {
            DialogsActivity fragment = new DialogsActivity(null);
            fragment.setSearchString(url);
            fragment.presentFragment(fragment);
        } else if (url.startsWith("/")) {
            fragment.chatActivityEnterView.setCommand(null, url, false, false);
        }
    }

    private void getChats(long max_id, final int count) {
        if (loading) {
            return;
        }
        TLRPC.TL_messages_getCommonChats req = new TLRPC.TL_messages_getCommonChats();
        req.user_id = fragment.getMessagesController().getInputUser(user);
        if (req.user_id instanceof TLRPC.TL_inputUserEmpty) {
            return;
        }
        req.limit = count;
        req.max_id = max_id;
        loading = true;
        fragment.getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TLRPC.messages_Chats res = (TLRPC.messages_Chats) response;
                fragment.getMessagesController().putChats(res.chats, false);
                commonChatsAdapter.setList(res.chats);
                if (res.chats.isEmpty()) binding.tvCommonChats.setVisibility(GONE);
                EventBus.getDefault().post(new MessageEvent(EventBusTags.SHOW_DIALOG));
            }
            loading = false;
        }));
    }

    public void setSearchData(Integer count, long date) {
        String format = LocaleController.getString("user_personal_msg_num", R.string.user_personal_msg_num);
        String formatStr = String.format(format, count);
        binding.tvUserSearch.setText(formatStr);

        String text = LocaleController.getString("user_personal_last_msg_date", R.string.user_personal_last_msg_date);
        String language = LocaleController.getInstance().getCurrentLocale().getLanguage();
        String pattern = "zh".equals(language) ? "yyyy/MM/dd HH:mm" : "MM/dd/yyyy HH:mm";
        binding.tvLastMsgDate.setText(text + TimeUtils.millis2String(date * 1000, TimeUtils.getSafeDateFormat(pattern)));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_close_dialog:
                EventBus.getDefault().post(new MessageEvent(EventBusTags.DISMISS_DIALOG));
                break;
            case R.id.ll_username:
            case R.id.ll_profile_message:
                Bundle args = new Bundle();
                args.putLong("user_id", user.id);
                if (!fragment.getMessagesController().checkCanOpenChat(args, fragment)) {
                    return;
                }
                ChatActivity chatActivity = new ChatActivity(args);
                chatActivity.setPreloadedSticker(fragment.getMediaDataController().getGreetingsSticker(), false);
                fragment.presentFragment(chatActivity);
                EventBus.getDefault().post(new MessageEvent(EventBusTags.DISMISS_DIALOG));
                break;
            case R.id.ll_profile_voice:
                // 代码来自：ProfileActivity#1815
                if (user != null) {
                    VoIPHelper.startCall(user, false, userInfo != null && userInfo.video_calls_available, fragment.getParentActivity(), userInfo, fragment.getAccountInstance());
                }
                break;
            case R.id.ll_profile_secret:
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), null);
                builder.setTitle(LocaleController.getString("AreYouSureSecretChatTitle", R.string.AreYouSureSecretChatTitle));
                builder.setMessage(LocaleController.getString("AreYouSureSecretChat", R.string.AreYouSureSecretChat));
                builder.setPositiveButton(LocaleController.getString("Start", R.string.Start), (dialogInterface, i) -> {
                    fragment.getSecretChatHelper().startSecretChat(fragment.getParentActivity(), user);
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                fragment.showDialog(builder.create());
                break;
            case R.id.ll_profile_add_friend:
                args = new Bundle();
                args.putLong("user_id", user.id);
                args.putBoolean("addContact", !user.contact);
                fragment.presentFragment(new ContactAddActivity(args));
                EventBus.getDefault().post(new MessageEvent(EventBusTags.DISMISS_DIALOG));
                break;
            case R.id.ll_profile_main_page:
                // 代码来自：ChatActivity#25355
                args = new Bundle();
                args.putLong("user_id", user.id);
                ProfileActivity profileActivity = new ProfileActivity(args);
                profileActivity.setPlayProfileAnimation(fragment.getCurrentUser() != null && fragment.getCurrentUser().id == user.id ? 1 : 0);
                AndroidUtilities.setAdjustResizeToNothing(fragment.getParentActivity(), fragment.getClassGuid());
                fragment.presentFragment(profileActivity);
                EventBus.getDefault().post(new MessageEvent(EventBusTags.DISMISS_DIALOG));
                break;
            case R.id.tv_user_search:
                fragment.openSearchWithText(null);
                fragment.searchUserButton.performClick();
                fragment.searchUserMessages(user, null);
                EventBus.getDefault().post(new MessageEvent(EventBusTags.DISMISS_DIALOG));
                break;
        }
    }

    @Override
    public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
        TLRPC.Chat chat = (TLRPC.Chat) adapter.getItem(position);
        Bundle args = new Bundle();
        args.putLong("chat_id", chat.id);
        if (!fragment.getMessagesController().checkCanOpenChat(args, fragment)) {
            return;
        }
        fragment.presentFragment(new ChatActivity(args));
        EventBus.getDefault().post(new MessageEvent(EventBusTags.DISMISS_DIALOG));
    }
}