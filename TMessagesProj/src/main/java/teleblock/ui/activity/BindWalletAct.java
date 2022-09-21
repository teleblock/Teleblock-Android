package teleblock.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import com.blankj.utilcode.util.ActivityUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.databinding.ActBindWalletBinding;
import org.telegram.ui.ActionBar.BaseFragment;

import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.wallet.WCSessionManager;

/**
 * Time:2022/8/4
 * Author:Perry
 * Description：绑定钱包页面
 */
public class BindWalletAct extends BaseFragment {

    private ActBindWalletBinding binding;

    @Override
    public boolean onFragmentCreate() {
        EventBus.getDefault().register(this);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public View createView(Context context) {
        removeActionbarViews();
        binding = ActBindWalletBinding.inflate(LayoutInflater.from(context));
        initView();
        initData();
        return fragmentView = binding.getRoot();
    }

    private void initView() {
        binding.tvTitle.setText(LocaleController.getString("act_bindwallet_linkwallet", R.string.act_bindwallet_linkwallet));
        binding.tvTitleTips.setText(LocaleController.getString("act_bindwallet_select_linkwallet", R.string.act_bindwallet_select_linkwallet));
        binding.tvCancel.setText(LocaleController.getString("act_bindwallet_cancel", R.string.act_bindwallet_cancel));

        binding.tvWalletMetaMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WCSessionManager.getInstance().resetSession();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(WCSessionManager.getInstance().config.toWCUri()));
                intent.setPackage("io.metamask");
                ActivityUtils.startActivity(intent);
            }
        });
        binding.tvCancel.setOnClickListener(view -> {
            finishFragment();
        });
    }

    private void initData() {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessage(MessageEvent event) {
        switch (event.getType()) {
            case EventBusTags.WALLET_CONNECT_APPROVED:
                finishFragment();
                break;
            case EventBusTags.WALLET_CONNECT_CLOSED:

                break;
        }
    }
}
