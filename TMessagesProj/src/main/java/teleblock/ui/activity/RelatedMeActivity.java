package teleblock.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.R;
import org.telegram.messenger.databinding.ActivityRelatedMeBinding;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.DialogsActivity;

import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.manager.DialogManager;
import teleblock.ui.adapter.DialogListAdapter;

/**
 * 创建日期：2022/6/22
 * 描述：
 */
public class RelatedMeActivity extends BaseFragment implements OnItemClickListener {

    private ActivityRelatedMeBinding binding;
    private DialogListAdapter adminAdapter;
    private DialogListAdapter pinnedAdapter;
    private DialogListAdapter participantAdapter;

    @Override
    public boolean onFragmentCreate() {
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("和我相关的");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        binding = ActivityRelatedMeBinding.inflate(LayoutInflater.from(context));
        fragmentView = binding.getRoot();
        initData();
        return fragmentView;
    }

    private void initData() {
        binding.rvAdmin.setLayoutManager(new LinearLayoutManager(getParentActivity()));
        adminAdapter = new DialogListAdapter();
        adminAdapter.setOnItemClickListener(this);
        binding.rvAdmin.setAdapter(adminAdapter);
        binding.rvPinned.setLayoutManager(new LinearLayoutManager(getParentActivity()));
        pinnedAdapter = new DialogListAdapter(true, parentLayout);
        pinnedAdapter.setOnItemClickListener(this);
        binding.rvPinned.setAdapter(pinnedAdapter);
        binding.rvParticipant.setLayoutManager(new LinearLayoutManager(getParentActivity()));
        participantAdapter = new DialogListAdapter();
        participantAdapter.setOnItemClickListener(this);
        binding.rvParticipant.setAdapter(participantAdapter);
    }

    private void loadData() {
        adminAdapter.setList(DialogManager.getInstance(currentAccount).relatedMeDialogs.get(1));
        pinnedAdapter.setList(DialogManager.getInstance(currentAccount).relatedMeDialogs.get(2));
        participantAdapter.setList(DialogManager.getInstance(currentAccount).relatedMeDialogs.get(3));
    }

    @Override
    public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
        TLRPC.Dialog dialog = (TLRPC.Dialog) adapter.getItem(position);
        if (dialog instanceof TLRPC.TL_dialogFolder) {
            TLRPC.TL_dialogFolder dialogFolder = (TLRPC.TL_dialogFolder) dialog;
            Bundle args = new Bundle();
            args.putInt("folderId", dialogFolder.folder.id);
            presentFragment(new DialogsActivity(args));
        } else {
            gotoChatActivity(dialog);
        }
    }

    private void gotoChatActivity(TLRPC.Dialog dialog) {
        long dialogId = dialog.id;
        if (dialogId == 0) {
            return;
        }
        Bundle args = new Bundle();
        if (DialogObject.isEncryptedDialog(dialogId)) {
            args.putInt("enc_id", DialogObject.getEncryptedChatId(dialogId));
        } else if (DialogObject.isUserDialog(dialogId)) {
            args.putLong("user_id", dialogId);
        } else {
            args.putLong("chat_id", -dialogId);
        }
        presentFragment(new ChatActivity(args));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessage(MessageEvent event) {
        switch (event.getType()) {
            case EventBusTags.UPDATE_DIALOGS_DATA:
                if ("2".equals(event.getFrom())) {
                    loadData();
                }
                break;
        }
    }
}