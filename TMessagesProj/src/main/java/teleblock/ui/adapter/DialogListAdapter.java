package teleblock.ui.adapter;

import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;

public class DialogListAdapter extends BaseQuickAdapter<TLRPC.Dialog, DialogListAdapter.MyViewHolder> {

    private boolean canPin;
    private ActionBarLayout parentLayout;
    private DialogsActivity parentFragment;
    private ArrayList<Long> selectedDialogs = new ArrayList<>();

    public DialogListAdapter() {
        this(false, null);
    }

    public DialogListAdapter(boolean canPin, ActionBarLayout parentLayout) {
        super(R.layout.item_dialog_list);
        this.canPin = canPin;
        this.parentLayout = parentLayout;
        if (parentLayout != null) {
            for (int a = 0, N = parentLayout.fragmentsStack.size(); a < N; a++) {
                BaseFragment fragment = parentLayout.fragmentsStack.get(a);
                if (fragment instanceof DialogsActivity) {
                    parentFragment = (DialogsActivity) fragment;
                    break;
                }
            }
        }
    }


    @Override
    protected void convert(@NonNull MyViewHolder helper, TLRPC.Dialog dialog) {
        DialogCell cell = helper.dialogCell;
        cell.setChecked(selectedDialogs.contains(dialog.id), true);
        cell.setDialog(dialog, 0, 0, canPin, parentLayout != null);
    }

    public void updateSelect(ArrayList<Long> selectedDialogs) {
        this.selectedDialogs = selectedDialogs;
        notifyDataSetChanged();
    }

    public class MyViewHolder extends BaseViewHolder {

        private View vLine;
        private DialogCell dialogCell;

        public MyViewHolder(View view) {
            super(view);
            vLine = view.findViewById(R.id.v_line);
            vLine.setBackgroundColor(Theme.getColor(Theme.key_divider));

            dialogCell = new DialogCell(parentFragment, getContext(), true, false, UserConfig.selectedAccount, null);
            FrameLayout frameLayout = (FrameLayout) view;
            frameLayout.addView(dialogCell, 0);

        }
    }
}
