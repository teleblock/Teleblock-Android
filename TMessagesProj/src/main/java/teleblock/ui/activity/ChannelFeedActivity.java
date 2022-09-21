package teleblock.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.databinding.ActivityChannelFeedBinding;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;

import teleblock.model.ChannelFeedEntity;
import teleblock.ui.fragment.ChannelFeedFragment;
import teleblock.video.KKVideoDataManager;


/**
 * Created by LSD on 2021/5/3.
 * Desc频道聚合载体Activity
 */
public class ChannelFeedActivity extends BaseFragment {

    private ActivityChannelFeedBinding mActivityChannelFeedBinding;
    private ChannelFeedEntity entity;
    private AvatarDrawable avatarDrawable = new AvatarDrawable();
    private BackupImageView avatarImageView;

    public ChannelFeedActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        if (getArguments() != null) {
            entity = (ChannelFeedEntity) arguments.getSerializable("entity");
        }
        return true;
    }

    @Override
    public View createView(Context context) {
        removeActionbarViews();
        mActivityChannelFeedBinding = ActivityChannelFeedBinding.inflate(LayoutInflater.from(context));
        fragmentView = mActivityChannelFeedBinding.getRoot();
        initView();
        return fragmentView;
    }

    private void initView() {
        mActivityChannelFeedBinding.ll.setPadding(0, AndroidUtilities.statusBarHeight, 0, 0);
        avatarImageView = new BackupImageView(getParentActivity());

        TLRPC.Chat chat = KKVideoDataManager.getInstance().getChat(entity.chatId);
        if (chat == null) return;
        mActivityChannelFeedBinding.tvGroupName.setText(chat.title);
        avatarDrawable.setInfo(chat);

        mActivityChannelFeedBinding.ivBack.setOnClickListener(view -> {
            finishFragment();
        });

        if (avatarImageView != null) {
            avatarImageView.setRoundRadius(AndroidUtilities.dp(50));
            avatarImageView.setImage(ImageLocation.getForChat(chat, ImageLocation.TYPE_SMALL), "50_50", avatarDrawable, chat);
            mActivityChannelFeedBinding.groupAvstarLayout.addView(avatarImageView);
        }

        ChannelFeedFragment channelPage = new ChannelFeedFragment(this, entity, "channelFeedActivity");
        mActivityChannelFeedBinding.feedFrame.addView(channelPage);
    }

}
