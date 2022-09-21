package teleblock.ui.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.ruffian.library.widget.RTextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.databinding.ViewTabHomeBinding;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;
import java.util.HashMap;

import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.ui.activity.CommonToolsActivity;
import teleblock.ui.activity.DialogListActivity;
import teleblock.util.EventUtil;
import teleblock.util.MMKVUtil;
import teleblock.util.TGLog;
import teleblock.util.TelegramUtil;

/**
 * 创建日期：2022/4/19
 * 描述：
 */
public class HomeTopView extends FrameLayout implements View.OnClickListener {

    private DialogsActivity fragment;
    public ViewTabHomeBinding binding;

    public HomeTopView(@NonNull DialogsActivity fragment) {
        super(fragment.getParentActivity());
        EventBus.getDefault().register(this);
        this.fragment = fragment;
        initView();
        initData();
        setOnClickListener(v -> {
        });
    }

    private void initView() {
        binding = ViewTabHomeBinding.inflate(LayoutInflater.from(getContext()), this, true);
        binding.tvLinkWallet.setOnClickListener(this);
        binding.vHomeTool.setOnClickListener(this);
        binding.vHomeContact.setOnClickListener(this);
        binding.vHomeRelatedMe.setOnClickListener(this);
        binding.vHomeNonContact.setOnClickListener(this);
    }

    public void initData() {
        binding.tvHomeTab.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        binding.tvHomeContact.setText(LocaleController.getString("home_contact", R.string.home_contact));
        binding.tvHomeRelatedMe.setText(LocaleController.getString("home_relatedme", R.string.home_relatedme));
        binding.tvHomeNonContact.setText(LocaleController.getString("home_unknown_sender", R.string.home_unknown_sender));
        binding.tvHomeTool.setText(LocaleController.getString("home_commontools", R.string.home_commontools));
        binding.tvHomeTab.setText(LocaleController.getString("home_msg_group", R.string.home_msg_group));
    }

    private void setUnreadToTv(RTextView tv, int unRead) {
        tv.setVisibility(unRead <= 0 ? INVISIBLE : VISIBLE);
        tv.setText(String.valueOf(unRead));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_link_wallet:

                break;
            case R.id.v_home_contact:
                EventUtil.track(getContext(), EventUtil.Even.好友点击, new HashMap<>());
                Bundle args = new Bundle();
                args.putInt("type", 1);
                fragment.presentFragment(new DialogListActivity(args));
                break;
            case R.id.v_home_related_me:
                EventUtil.track(getContext(), EventUtil.Even.与我相关点击, new HashMap<>());
                args = new Bundle();
                args.putInt("type", 2);
                fragment.presentFragment(new DialogListActivity(args));
                break;
            case R.id.v_home_non_contact:
                EventUtil.track(getContext(), EventUtil.Even.非联系人点击, new HashMap<>());
                args = new Bundle();
                args.putInt("type", 3);
                fragment.presentFragment(new DialogListActivity(args));
                break;
            case R.id.v_home_tool:
                EventUtil.track(getContext(), EventUtil.Even.好工具点击, new HashMap<>());
                fragment.presentFragment(new CommonToolsActivity());
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessage(MessageEvent event) {
        switch (event.getType()) {
            case EventBusTags.UPDATE_UNREAD_COUNT:
                if ("1".equals(event.getFrom())) {
                    setUnreadToTv(binding.tvContactUnread, (Integer) event.getData());
                } else if ("2".equals(event.getFrom())) {
                    setUnreadToTv(binding.tvRelatedMeUnread, (Integer) event.getData());
                } else if ("3".equals(event.getFrom())) {
                    setUnreadToTv(binding.tvNonContactUnread, (Integer) event.getData());
                } else { // 全部已读
                    setUnreadToTv(binding.tvContactUnread, (Integer) event.getData());
                    setUnreadToTv(binding.tvRelatedMeUnread, (Integer) event.getData());
                    setUnreadToTv(binding.tvNonContactUnread, (Integer) event.getData());
                }
                break;
        }
    }
}