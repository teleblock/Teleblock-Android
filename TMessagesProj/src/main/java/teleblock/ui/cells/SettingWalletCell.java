package teleblock.ui.cells;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.databinding.ViewSettingWalletBinding;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.ProfileActivity;

import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.ui.activity.WalletHomeAct;
import teleblock.util.MMKVUtil;
import teleblock.util.StringUtil;
import teleblock.util.TelegramUtil;

/**
 * Time:2022/8/3
 * Author:Perry
 * Description：设置里面的钱包布局
 */
public class SettingWalletCell extends ConstraintLayout {

    private ViewSettingWalletBinding binding;
    private ProfileActivity fragment;
    private HeaderCell headerCell;

    public SettingWalletCell(@NonNull Context context, ProfileActivity fragment) {
        super(context);
        EventBus.getDefault().register(this);
        this.fragment = fragment;
    }

    {
        binding = ViewSettingWalletBinding.inflate(LayoutInflater.from(getContext()), this, true);
        initView();
        initData();
    }

    private void initView() {
        binding.tvWalletUnbindTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        binding.tvWalletUnbindTitle.setText(LocaleController.getString("fragment_setting_wallet_unbind_title", R.string.setting_wallet_unbind_title));
        binding.tvLinkWallet.setText(LocaleController.getString("fragment_setting_linkwallet", R.string.setting_linkwallet_title));

        headerCell = new HeaderCell(getContext(), 23, null);
        headerCell.setText(LocaleController.getString("fragment_setting_wallet_unbind_title", R.string.setting_wallet_unbind_title));
        binding.llBindWallet.addView(headerCell,0);

        //已连接钱包
        binding.llBindWallet.setOnClickListener(view -> {
//            fragment.currentTimeMillis = fragment.getConnectionsManager().getCurrentTimeMillis();
//            Bundle args = new Bundle();
//            args.putLong("currentTimeMillis", fragment.currentTimeMillis);
//            args.putString("address", MMKVUtil.connectedWalletAddress());
//            args.putBoolean("userSelf", true);
//            fragment.presentFragment(new WalletHomeAct(args));
            TelegramUtil.walletConnect();
        });
        //未连接钱包
        binding.llLinkWallet.setOnClickListener(view -> {
            TelegramUtil.walletConnect();
        });
    }

    private void initData() {
        String address = MMKVUtil.connectedWalletAddress();
        if (TextUtils.isEmpty(address)) {
            binding.rlUnbindWallet.setVisibility(VISIBLE);
            binding.llBindWallet.setVisibility(GONE);
            headerCell.setVisibility(GONE);
        } else {
            binding.tvWalletAddress.setText(StringUtil.formatAddress(address));
            binding.rlUnbindWallet.setVisibility(GONE);
            binding.llBindWallet.setVisibility(VISIBLE);
            headerCell.setVisibility(VISIBLE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessage(MessageEvent event) {
        switch (event.getType()) {
            case EventBusTags.WALLET_CONNECT_APPROVED:
                initData();
                break;
            case EventBusTags.WALLET_CONNECT_CLOSED:
                break;
        }
    }
}