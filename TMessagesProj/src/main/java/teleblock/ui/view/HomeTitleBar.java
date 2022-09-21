package teleblock.ui.view;

import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.databinding.ViewHomeTitleBinding;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.DialogsActivity;

import java.util.HashMap;

import teleblock.config.AppConfig;
import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.ui.activity.ChatListActivity;
import teleblock.ui.dialog.ClearUnReadMessageDialog;
import teleblock.ui.popup.HomeOptionPopup;
import teleblock.util.EventUtil;
import teleblock.util.MMKVUtil;
import timber.log.Timber;

/**
 * 创建日期：2022/7/4
 * 描述：
 */
public class HomeTitleBar extends FrameLayout implements View.OnClickListener {

    private DialogsActivity fragment;
    private ViewHomeTitleBinding binding;
    private BackupImageView avatarImageView;
    private ClearUnReadMessageDialog mClearUnReadMessageDialog;
    private Handler delayHandle = new Handler();

    public HomeTitleBar(@NonNull DialogsActivity fragment) {
        super(fragment.getParentActivity());
        this.fragment = fragment;
        initView();
        initData();
        setOnClickListener(v -> {
        });
    }

    private void initView() {
        binding = ViewHomeTitleBinding.inflate(LayoutInflater.from(getContext()), this, true);
        binding.viewStatusBar.getLayoutParams().height = AndroidUtilities.statusBarHeight;
        binding.rlTitleBar.getLayoutParams().height = ActionBar.getCurrentActionBarHeight();

        avatarImageView = new BackupImageView(getContext());
        avatarImageView.getImageReceiver().setRoundRadius(AndroidUtilities.dp(20));
        binding.flHomeAvatar.addView(avatarImageView);
        binding.tvNickname.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        binding.tvNickname.setOnClickListener(v -> {
            if (!BuildConfig.DEBUG) return;
            fragment.presentFragment(new ChatListActivity());
        });

        binding.homeAvatarFrameContent.setOnClickListener(this);
        binding.ivHomeSearch.setOnClickListener(this);
        binding.ivHomeAdd.setOnClickListener(this);
        binding.ivHomeClean.setOnClickListener(this);

        //初始化清理未读弹窗
        mClearUnReadMessageDialog = new ClearUnReadMessageDialog(getContext(), () -> {
            MMKVUtil.saveValue(AppConfig.MkKey.SHOW_CLEAR_UNREADMSG, true);
            clearAllUnreadMsg();
        });
    }

    public void addDownloadView(View view) {
        if (view == null) return;
        binding.downloadFrame.addView(view);
    }

    private void initData() {
        updateUserInfo();
    }

    public void updateUserInfo() {
        TLRPC.User user = fragment.getUserConfig().getCurrentUser();
        if (user != null) {
            binding.tvNickname.setText("Hi, " + UserObject.getUserName(user));
            AvatarDrawable avatarDrawable = new AvatarDrawable(user);
            avatarDrawable.setColor(Theme.getColor(Theme.key_avatar_backgroundInProfileBlue));
            avatarImageView.setForUserOrChat(user, avatarDrawable);
        }
    }

    public void changeStealth(boolean show) {
        binding.ivStealthMode.setVisibility(show ? VISIBLE : GONE);
    }

    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title) && title.startsWith("LOC_ERR")) {
            binding.tvNickname.setText("Hi, " + UserObject.getUserName(fragment.getUserConfig().getCurrentUser()));
        } else {
            binding.tvNickname.setText(title);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.home_avatar_frame_content:
                EventBus.getDefault().post(new MessageEvent(EventBusTags.OPEN_DRAWER));
                break;
            case R.id.iv_home_search:
                EventUtil.track(getContext(), EventUtil.Even.搜索点击, new HashMap<>());
                fragment.searchItem.performClick();
                break;
            case R.id.iv_home_add:
                EventUtil.track(getContext(), EventUtil.Even.加号点击, new HashMap<>());
                HomeOptionPopup homeOptionPopup = new HomeOptionPopup(fragment);
                homeOptionPopup.showPopupWindow(v);
                break;
            case R.id.iv_home_clean://显示清除未读提示框
                EventUtil.track(getContext(), EventUtil.Even.清除消息点击, new HashMap<>());
                if (!MMKVUtil.getBoolean(AppConfig.MkKey.SHOW_CLEAR_UNREADMSG)) {
                    if (mClearUnReadMessageDialog != null) {
                        mClearUnReadMessageDialog.show();
                    }
                } else {
                    clearAllUnreadMsg();
                }
                break;
        }
    }

    private void clearAllUnreadMsg() {
        MessagesStorage.getInstance(UserConfig.selectedAccount).readAllDialogs(-1);
        EventBus.getDefault().post(new MessageEvent(EventBusTags.UPDATE_UNREAD_COUNT, 0));
    }

}