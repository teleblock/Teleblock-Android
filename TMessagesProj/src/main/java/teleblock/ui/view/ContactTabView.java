package teleblock.ui.view;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.KeyboardUtils;

import org.greenrobot.eventbus.EventBus;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.databinding.ViewTabContactBinding;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.ContactsActivity;
import org.telegram.ui.DialogsActivity;

import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;

/**
 * 创建日期：2022/4/19
 * 描述：联系人界面
 */
public class ContactTabView extends FrameLayout {
    private DialogsActivity mBaseFragment;
    private ViewTabContactBinding binding;

    private BackupImageView avatarImageView;
    private ContactsActivity contactsActivity;

    public ContactTabView(@NonNull DialogsActivity dialogsActivity) {
        super(dialogsActivity.getParentActivity());
        this.mBaseFragment = dialogsActivity;
        initView();
        setVisibility(GONE);
    }

    private void initView() {
        binding = ViewTabContactBinding.inflate(LayoutInflater.from(getContext()), this, true);
        setOnClickListener(v -> {
        });
        binding.avatarFrameContent.setOnClickListener(view -> {
            EventBus.getDefault().post(new MessageEvent(EventBusTags.OPEN_DRAWER));
        });
        binding.contactMain.setPadding(0, AndroidUtilities.statusBarHeight, 0, 0);
        binding.tvTitle.setText(LocaleController.getString("view_title_text", R.string.view_title_text));
        binding.tvTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

        //titleBar
        avatarImageView = new BackupImageView(getContext());
        avatarImageView.getImageReceiver().setRoundRadius(AndroidUtilities.dp(20));
        binding.avatarFrame.addView(avatarImageView);
        binding.ivSort.setOnClickListener(v -> {
            SharedConfig.toggleSortContactsByName();
            boolean sortByName = SharedConfig.sortContactsByName;
            binding.ivSort.getHelper().setIconNormal(getResources().getDrawable(sortByName ? R.drawable.ic_contacts_time : R.drawable.ic_contacts_name));
            contactsActivity.sortContact();
        });
        binding.ivSearch.setOnClickListener(v -> {
            binding.searchLayout.setVisibility(VISIBLE);
            binding.barLayout.setVisibility(GONE);
            contactsActivity.searchListener.onSearchExpand();
            AndroidUtilities.runOnUIThread(() -> {
                binding.etSearch.requestFocus();
                KeyboardUtils.showSoftInput();
                mBaseFragment.bottomTabsHeight = 0;
            }, 200);
        });
        binding.ivClose.setOnClickListener(v -> {
            binding.searchLayout.setVisibility(GONE);
            binding.barLayout.setVisibility(VISIBLE);
            contactsActivity.searchListener.onSearchCollapse();
            binding.etSearch.setText("");
            KeyboardUtils.hideSoftInput(this);
        });
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (contactsActivity != null) {
                    contactsActivity.searchListener.onTextChanged(binding.etSearch);
                }
            }
        });

    }

    public void initData() {
        if (contactsActivity == null || contactsActivity.isFinished) {
            Bundle args = new Bundle();
            args.putBoolean("destroyAfterSelect", true);
            args.putString("entry", "home");
            contactsActivity = new ContactsActivity(args);
            contactsActivity.onFragmentCreate();
            contactsActivity.setParentFragment(mBaseFragment);
            LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            binding.contactContent.addView(contactsActivity.getFragmentView(), layoutParams);
        }
        updateUserInfo();
    }

    public void changeStealth(boolean show) {
        binding.ivStealthMode.setVisibility(show ? VISIBLE : GONE);
    }

    public void updateUserInfo() {
        TLRPC.User user = mBaseFragment.getUserConfig().getCurrentUser();
        if (user != null) {
            AvatarDrawable avatarDrawable = new AvatarDrawable(user);
            avatarDrawable.setColor(Theme.getColor(Theme.key_avatar_backgroundInProfileBlue));
            avatarImageView.setForUserOrChat(user, avatarDrawable);
        }
    }
}