package teleblock.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.listener.OnItemLongClickListener;

import net.lucode.hackware.magicindicator.buildins.UIUtil;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.CommonPagerTitleView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.databinding.ActivityDialogMixBinding;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.MenuDrawable;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.FiltersListBottomSheet;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.Premium.LimitReachedBottomSheet;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.FilterCreateActivity;

import java.util.ArrayList;
import java.util.List;

import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.manager.DialogManager;
import teleblock.ui.adapter.DialogListAdapter;
import teleblock.ui.adapter.DialogMixPageAdapter;
import teleblock.ui.dialog.CommonTipsDialog;
import teleblock.widget.MCommonPagerTitleView;

/**
 * 创建日期：2022/6/22
 * 描述：
 */
public class DialogListActivity extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, NotificationCenter.NotificationCenterDelegate {

    private int type;
    private ActivityDialogMixBinding binding;
    private DialogListAdapter friendAdapter;
    private DialogListAdapter pinnedAdapter;
    private DialogListAdapter participantAdapter;
    private DialogListAdapter otherAdapter;
    private ArrayList<Long> selectedDialogs = new ArrayList<>();

    private boolean onlySelect;
    private MenuDrawable menuDrawable;
    private BackDrawable backDrawable;
    private UndoView[] undoView = new UndoView[2];
    private NumberTextView selectedDialogsCountTextView;
    private ArrayList<View> actionModeViews = new ArrayList<>();
    private ActionBarMenuItem deleteItem;
    private ActionBarMenuItem pinItem;
    private ActionBarMenuItem muteItem;
    private ActionBarMenuItem archive2Item;
    private ActionBarMenuSubItem pin2Item;
    private ActionBarMenuSubItem addToFolderItem;
    //    private ActionBarMenuSubItem removeFromFolderItem;
    private ActionBarMenuSubItem archiveItem;
    private ActionBarMenuSubItem clearItem;
    private ActionBarMenuSubItem readItem;
    private ActionBarMenuSubItem blockItem;
    private ActionBarMenuItem cleanAllItem;
    private ActionBarMenuItem deleteOtherViewDialogs;
    private ActionBarMenuItem relateMeItem;

    private final static int pin = 100;
    private final static int read = 101;
    private final static int delete = 102;
    private final static int clear = 103;
    private final static int mute = 104;
    private final static int archive = 105;
    private final static int block = 106;
    private final static int archive2 = 107;
    private final static int pin2 = 108;
    private final static int add_to_folder = 109;
    private final static int remove_from_folder = 110;
    private final static int cleanAll = 111;
    private final static int deleteOther = 112;
    private final static int gotoRelateMe = 113;

    private int canReadCount;
    private int canPinCount;
    private int canMuteCount;
    private int canUnmuteCount;
    private int canClearCacheCount;
    private int canReportSpamCount;
    private int canUnarchiveCount;
    private boolean canDeletePsaSelected;
    private int folderId;
    private int currentTab = 0;
    private String[] tabs = new String[]{
            LocaleController.getString("home_contact", R.string.home_contact),
            LocaleController.getString("home_relatedme", R.string.home_relatedme),
            LocaleController.getString("home_unknown_sender", R.string.home_unknown_sender)
    };
    private CommonNavigator commonNavigator;
    private Runnable deleteRunnable;

    public DialogListActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        EventBus.getDefault().register(this);
        if (getArguments() != null) {
            type = arguments.getInt("type", 0);
        }
        getNotificationCenter().addObserver(this, NotificationCenter.dialogDeleted);
        getNotificationCenter().addObserver(this, NotificationCenter.needDeleteDialog);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        EventBus.getDefault().unregister(this);
        if (currentTab == 2) {
            EventBus.getDefault().post(new MessageEvent(EventBusTags.MARK_AS_READ));
        }
        if (deleteRunnable != null) {
            deleteRunnable.run();
        }
        getNotificationCenter().removeObserver(this, NotificationCenter.dialogDeleted);
        getNotificationCenter().removeObserver(this, NotificationCenter.needDeleteDialog);
    }

    @Override
    public void onResume() {
        super.onResume();
        DialogManager.getInstance(currentAccount).updateAllDialogs(true);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonDrawable(backDrawable = new BackDrawable(false));
        actionBar.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("message_center_content", R.string.message_center_content));
        actionBar.setCastShadows(false);//去除分隔线
        createActionMode(null);
        binding = ActivityDialogMixBinding.inflate(LayoutInflater.from(context));
        fragmentView = binding.getRoot();
        createUndoView();
        initView();
        return fragmentView;
    }

    private void createUndoView() {
        for (int a = 0; a < 2; a++) {
            undoView[a] = new UndoView(getParentActivity());
            ((FrameLayout) fragmentView).addView(undoView[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));
        }
    }

    private void createActionMode(String tag) {
        if (actionBar.actionModeIsExist(tag)) {
            return;
        }
        final ActionBarMenu actionMode = actionBar.createActionMode(false, tag);
        actionMode.setBackgroundColor(Color.TRANSPARENT);
        actionMode.drawBlur = false;

        selectedDialogsCountTextView = new NumberTextView(actionMode.getContext());
        selectedDialogsCountTextView.setTextSize(18);
        selectedDialogsCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        selectedDialogsCountTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
        actionMode.addView(selectedDialogsCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 72, 0, 0, 0));
        selectedDialogsCountTextView.setOnTouchListener((v, event) -> true);

        pinItem = actionMode.addItemWithWidth(pin, R.drawable.msg_pin, AndroidUtilities.dp(54));
        muteItem = actionMode.addItemWithWidth(mute, R.drawable.msg_mute, AndroidUtilities.dp(54));
        archive2Item = actionMode.addItemWithWidth(archive2, R.drawable.msg_archive, AndroidUtilities.dp(54));
        deleteItem = actionMode.addItemWithWidth(delete, R.drawable.msg_delete, AndroidUtilities.dp(54), LocaleController.getString("Delete", R.string.Delete));
        ActionBarMenuItem otherItem = actionMode.addItemWithWidth(0, R.drawable.ic_ab_other, AndroidUtilities.dp(54), LocaleController.getString("AccDescrMoreOptions", R.string.AccDescrMoreOptions));
        archiveItem = otherItem.addSubItem(archive, R.drawable.msg_archive, LocaleController.getString("Archive", R.string.Archive));
        pin2Item = otherItem.addSubItem(pin2, R.drawable.msg_pin, LocaleController.getString("DialogPin", R.string.DialogPin));
        addToFolderItem = otherItem.addSubItem(add_to_folder, R.drawable.msg_addfolder, LocaleController.getString("FilterAddTo", R.string.FilterAddTo));
//        removeFromFolderItem = otherItem.addSubItem(remove_from_folder, R.drawable.msg_removefolder, LocaleController.getString("FilterRemoveFrom", R.string.FilterRemoveFrom));
        readItem = otherItem.addSubItem(read, R.drawable.msg_markread, LocaleController.getString("MarkAsRead", R.string.MarkAsRead));
        clearItem = otherItem.addSubItem(clear, R.drawable.msg_clear, LocaleController.getString("ClearHistory", R.string.ClearHistory));
        blockItem = otherItem.addSubItem(block, R.drawable.msg_block, LocaleController.getString("BlockUser", R.string.BlockUser));

        actionModeViews.add(pinItem);
        actionModeViews.add(archive2Item);
        actionModeViews.add(muteItem);
        actionModeViews.add(deleteItem);
        actionModeViews.add(otherItem);

        if (tag == null) {
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        if (actionBar.isActionModeShowed()) {
                            hideActionMode(true);
                            cleanAllItem.setVisibility(View.GONE);
                        } else {
                            finishFragment();
                        }
                    } else if (id == add_to_folder) {
                        FiltersListBottomSheet sheet = new FiltersListBottomSheet(DialogListActivity.this, selectedDialogs);
                        sheet.setDelegate(filter -> {
                            ArrayList<Long> alwaysShow = FiltersListBottomSheet.getDialogsCount(DialogListActivity.this, filter, selectedDialogs, true, false);
                            int currentCount;
                            if (filter != null) {
                                currentCount = filter.alwaysShow.size();
                            } else {
                                currentCount = 0;
                            }
                            int totalCount = currentCount + alwaysShow.size();
                            if ((totalCount > getMessagesController().dialogFiltersChatsLimitDefault && !getUserConfig().isPremium()) || totalCount > getMessagesController().dialogFiltersChatsLimitPremium) {
                                showDialog(new LimitReachedBottomSheet(DialogListActivity.this, fragmentView.getContext(), LimitReachedBottomSheet.TYPE_CHATS_IN_FOLDER, currentAccount));
                                return;
                            }
                            if (filter != null) {
                                if (!alwaysShow.isEmpty()) {
                                    for (int a = 0; a < alwaysShow.size(); a++) {
                                        filter.neverShow.remove(alwaysShow.get(a));
                                    }
                                    filter.alwaysShow.addAll(alwaysShow);
                                    FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, true, false, DialogListActivity.this, null);
                                }
                                long did;
                                if (alwaysShow.size() == 1) {
                                    did = alwaysShow.get(0);
                                } else {
                                    did = 0;
                                }
                                getUndoView().showWithAction(did, UndoView.ACTION_ADDED_TO_FOLDER, alwaysShow.size(), filter, null, null);
                            } else {
                                presentFragment(new FilterCreateActivity(null, alwaysShow));
                            }
                            hideActionMode(true);
                        });
                        showDialog(sheet);
                    } else if (id == remove_from_folder) {
                        MessagesController.DialogFilter filter = getCurrentDialogFilter();
                        ArrayList<Long> neverShow = FiltersListBottomSheet.getDialogsCount(DialogListActivity.this, filter, selectedDialogs, false, false);

                        int currentCount;
                        if (filter != null) {
                            currentCount = filter.neverShow.size();
                        } else {
                            currentCount = 0;
                        }
                        if (currentCount + neverShow.size() > 100) {
                            showDialog(AlertsCreator.createSimpleAlert(getParentActivity(), LocaleController.getString("FilterAddToAlertFullTitle", R.string.FilterAddToAlertFullTitle), LocaleController.getString("FilterAddToAlertFullText", R.string.FilterAddToAlertFullText)).create());
                            return;
                        }
                        if (!neverShow.isEmpty()) {
                            filter.neverShow.addAll(neverShow);
                            for (int a = 0; a < neverShow.size(); a++) {
                                Long did = neverShow.get(a);
                                filter.alwaysShow.remove(did);
                                filter.pinnedDialogs.delete(did);
                            }
                            FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, false, false, DialogListActivity.this, null);
                        }
                        long did;
                        if (neverShow.size() == 1) {
                            did = neverShow.get(0);
                        } else {
                            did = 0;
                        }
                        getUndoView().showWithAction(did, UndoView.ACTION_REMOVED_FROM_FOLDER, neverShow.size(), filter, null, null);
                        hideActionMode(false);
                    } else if (id == pin || id == read || id == delete || id == clear || id == mute || id == archive || id == block || id == archive2 || id == pin2) {
                        performSelectedDialogsAction(selectedDialogs, id, true);
                        fragmentView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                DialogManager.getInstance(currentAccount).updateAllDialogs(false);
                            }
                        }, 500);
                    }
                }
            });
        }

        //单独的-清除未读
        ActionBarMenu menu = actionBar.createMenu();
        cleanAllItem = menu.addItem(cleanAll, R.drawable.btn_clean_tittle_bar);
        cleanAllItem.setVisibility(View.GONE);
        cleanAllItem.setOnClickListener(view -> {
            if (currentTab == 0) {
                maskAsRead(DialogManager.getInstance(currentAccount).contactDialogs);
            } else if (currentTab == 1) {
                List<TLRPC.Dialog> list = DialogManager.getInstance(currentAccount).relatedMeDialogs.get(2);
                list.addAll(DialogManager.getInstance(currentAccount).relatedMeDialogs.get(3));
                maskAsRead(list);
            } else if (currentTab == 2) {
                maskAsRead(DialogManager.getInstance(currentAccount).nonContactDialogs);
            }
        });

        //单独的-删除陌生人对话
        deleteOtherViewDialogs = menu.addItem(deleteOther, R.drawable.btn_delete_other);
        deleteOtherViewDialogs.setVisibility(View.GONE);
        deleteOtherViewDialogs.setOnClickListener(view -> {
            new CommonTipsDialog(getParentActivity(),
                    LocaleController.getString("act_delete_other_dialog_title", R.string.act_delete_other_dialog_title),
                    LocaleController.getString("act_delete_other_dialog_left_text", R.string.act_delete_other_dialog_left_text),
                    LocaleController.getString("act_delete_other_dialog_right_text", R.string.act_delete_other_dialog_right_text)) {
                @Override
                public void onLeftClick() {
                }

                @Override
                public void onRightClick() {
                    List<TLRPC.Dialog> dialogs = otherAdapter.getData();
                    ArrayList<Long> selectedDialogs = new ArrayList<>();
                    for (TLRPC.Dialog dialog : dialogs) {
                        selectedDialogs.add(dialog.id);
                    }
                    performSelectedDialogsAction(selectedDialogs, delete, false);
                }
            }.show();
        });

        //单独的-删除陌生人对话
        relateMeItem = menu.addItem(gotoRelateMe, R.drawable.home_option_new_relateme);
        relateMeItem.setVisibility(View.GONE);
        relateMeItem.setOnClickListener(view -> {
            presentFragment(new RelatedMeSettingAct());
        });
    }

    private void performSelectedDialogsAction(ArrayList<Long> selectedDialogs, int action, boolean alert) {
        if (getParentActivity() == null) {
            return;
        }
        MessagesController.DialogFilter filter;
        boolean containsFilter = true;
        if (containsFilter) {
            filter = getCurrentDialogFilter();
        } else {
            filter = null;
        }
        int count = selectedDialogs.size();
        int pinnedActionCount = 0;
        if (action == archive || action == archive2) {
            ArrayList<Long> copy = new ArrayList<>(selectedDialogs);
            getMessagesController().addDialogToFolder(copy, canUnarchiveCount == 0 ? 1 : 0, -1, null, 0);
            if (canUnarchiveCount == 0) {
                SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                boolean hintShowed = preferences.getBoolean("archivehint_l", false) || SharedConfig.archiveHidden;
                if (!hintShowed) {
                    preferences.edit().putBoolean("archivehint_l", true).commit();
                }
                int undoAction;
                if (hintShowed) {
                    undoAction = copy.size() > 1 ? UndoView.ACTION_ARCHIVE_FEW : UndoView.ACTION_ARCHIVE;
                } else {
                    undoAction = copy.size() > 1 ? UndoView.ACTION_ARCHIVE_FEW_HINT : UndoView.ACTION_ARCHIVE_HINT;
                }
                getUndoView().showWithAction(0, undoAction, null, () -> getMessagesController().addDialogToFolder(copy, folderId == 0 ? 0 : 1, -1, null, 0));
            } else {
                ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getDialogs(folderId);
                if (dialogs.isEmpty()) {
                    finishFragment();
                }
            }
            hideActionMode(false);
            return;
        } else if ((action == pin || action == pin2) && canPinCount != 0) {
            int pinnedCount = 0;
            int pinnedSecretCount = 0;
            int newPinnedCount = 0;
            int newPinnedSecretCount = 0;
            ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getDialogs(folderId);
            for (int a = 0, N = dialogs.size(); a < N; a++) {
                TLRPC.Dialog dialog = dialogs.get(a);
                if (dialog instanceof TLRPC.TL_dialogFolder) {
                    continue;
                }
                if (isDialogPinned(dialog)) {
                    if (DialogObject.isEncryptedDialog(dialog.id)) {
                        pinnedSecretCount++;
                    } else {
                        pinnedCount++;
                    }
                } else if (!getMessagesController().isPromoDialog(dialog.id, false)) {
                    break;
                }
            }
            int alreadyAdded = 0;
            for (int a = 0; a < count; a++) {
                long selectedDialog = selectedDialogs.get(a);
                TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(selectedDialog);
                if (dialog == null || isDialogPinned(dialog)) {
                    continue;
                }
                if (DialogObject.isEncryptedDialog(selectedDialog)) {
                    newPinnedSecretCount++;
                } else {
                    newPinnedCount++;
                }
                if (filter != null && filter.alwaysShow.contains(selectedDialog)) {
                    alreadyAdded++;
                }
            }
            int maxPinnedCount;
            if (filter != null) {
                maxPinnedCount = 100 - filter.alwaysShow.size();
            } else {
                maxPinnedCount = getMessagesController().maxFolderPinnedDialogsCount;
            }
            if (newPinnedSecretCount + pinnedSecretCount > maxPinnedCount || newPinnedCount + pinnedCount - alreadyAdded > maxPinnedCount) {
                if (folderId != 0 || filter != null) {
                    AlertsCreator.showSimpleAlert(DialogListActivity.this, LocaleController.formatString("PinFolderLimitReached", R.string.PinFolderLimitReached, LocaleController.formatPluralString("Chats", maxPinnedCount)));
                } else {
                    LimitReachedBottomSheet limitReachedBottomSheet = new LimitReachedBottomSheet(this, getParentActivity(), LimitReachedBottomSheet.TYPE_PIN_DIALOGS, currentAccount);
                    showDialog(limitReachedBottomSheet);
                }
                return;
            }
        } else if ((action == delete || action == clear) && count > 1 && alert) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            if (action == delete) {
                builder.setTitle(LocaleController.formatString("DeleteFewChatsTitle", R.string.DeleteFewChatsTitle, LocaleController.formatPluralString("ChatsSelected", count)));
                builder.setMessage(LocaleController.getString("AreYouSureDeleteFewChats", R.string.AreYouSureDeleteFewChats));
            } else {
                if (canClearCacheCount != 0) {
                    builder.setTitle(LocaleController.formatString("ClearCacheFewChatsTitle", R.string.ClearCacheFewChatsTitle, LocaleController.formatPluralString("ChatsSelectedClearCache", count)));
                    builder.setMessage(LocaleController.getString("AreYouSureClearHistoryCacheFewChats", R.string.AreYouSureClearHistoryCacheFewChats));
                } else {
                    builder.setTitle(LocaleController.formatString("ClearFewChatsTitle", R.string.ClearFewChatsTitle, LocaleController.formatPluralString("ChatsSelectedClear", count)));
                    builder.setMessage(LocaleController.getString("AreYouSureClearHistoryFewChats", R.string.AreYouSureClearHistoryFewChats));
                }
            }
            builder.setPositiveButton(action == delete ? LocaleController.getString("Delete", R.string.Delete)
                    : canClearCacheCount != 0 ? LocaleController.getString("ClearHistoryCache", R.string.ClearHistoryCache)
                    : LocaleController.getString("ClearHistory", R.string.ClearHistory), (dialog1, which) -> {
                if (selectedDialogs.isEmpty()) {
                    return;
                }
                ArrayList<Long> didsCopy = new ArrayList<>(selectedDialogs);
                getUndoView().showWithAction(didsCopy, action == delete ? UndoView.ACTION_DELETE_FEW : UndoView.ACTION_CLEAR_FEW, null, null, () -> {
                    if (action == delete) {
                        getMessagesController().setDialogsInTransaction(true);
                        performSelectedDialogsAction(didsCopy, action, false);
                        getMessagesController().setDialogsInTransaction(false);
                        getMessagesController().checkIfFolderEmpty(folderId);
                    } else {
                        performSelectedDialogsAction(didsCopy, action, false);
                    }
                }, null);
                hideActionMode(action == clear);
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            AlertDialog alertDialog = builder.create();
            showDialog(alertDialog);
            TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (button != null) {
                button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
            }
            return;
        } else if (action == block && alert) {
            TLRPC.User user;
            if (count == 1) {
                long did = selectedDialogs.get(0);
                user = getMessagesController().getUser(did);
            } else {
                user = null;
            }
            AlertsCreator.createBlockDialogAlert(DialogListActivity.this, count, canReportSpamCount != 0, user, (report, delete) -> {
                for (int a = 0, N = selectedDialogs.size(); a < N; a++) {
                    long did = selectedDialogs.get(a);
                    if (report) {
                        TLRPC.User u = getMessagesController().getUser(did);
                        getMessagesController().reportSpam(did, u, null, null, false);
                    }
                    if (delete) {
                        getMessagesController().deleteDialog(did, 0, true);
                    }
                    getMessagesController().blockPeer(did);
                }
                hideActionMode(false);
            });
            return;
        }
        int minPinnedNum = Integer.MAX_VALUE;
        if (filter != null && (action == pin || action == pin2) && canPinCount != 0) {
            for (int c = 0, N = filter.pinnedDialogs.size(); c < N; c++) {
                minPinnedNum = Math.min(minPinnedNum, filter.pinnedDialogs.valueAt(c));
            }
            minPinnedNum -= canPinCount;
        }
        boolean scrollToTop = false;
        for (int a = 0; a < count; a++) {
            long selectedDialog = selectedDialogs.get(a);
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(selectedDialog);
            if (dialog == null) {
                continue;
            }
            TLRPC.Chat chat;
            TLRPC.User user = null;

            TLRPC.EncryptedChat encryptedChat = null;
            if (DialogObject.isEncryptedDialog(selectedDialog)) {
                encryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(selectedDialog));
                chat = null;
                if (encryptedChat != null) {
                    user = getMessagesController().getUser(encryptedChat.user_id);
                } else {
                    user = new TLRPC.TL_userEmpty();
                }
            } else if (DialogObject.isUserDialog(selectedDialog)) {
                user = getMessagesController().getUser(selectedDialog);
                chat = null;
            } else {
                chat = getMessagesController().getChat(-selectedDialog);
            }
            if (chat == null && user == null) {
                continue;
            }
            boolean isBot = user != null && user.bot && !MessagesController.isSupportUser(user);
            if (action == pin || action == pin2) {
                if (canPinCount != 0) {
                    if (isDialogPinned(dialog)) {
                        continue;
                    }
                    pinnedActionCount++;
//                    pinDialog(selectedDialog, true, filter, minPinnedNum, count == 1);
                    if (filter != null) {
                        minPinnedNum++;
                        if (encryptedChat != null) {
                            if (!filter.alwaysShow.contains(encryptedChat.user_id)) {
                                filter.alwaysShow.add(encryptedChat.user_id);
                            }
                        } else {
                            if (!filter.alwaysShow.contains(dialog.id)) {
                                filter.alwaysShow.add(dialog.id);
                            }
                        }
                    }
                } else {
                    if (!isDialogPinned(dialog)) {
                        continue;
                    }
                    pinnedActionCount++;
//                    pinDialog(selectedDialog, false, filter, minPinnedNum, count == 1);
                }
            } else if (action == read) {
                if (canReadCount != 0) {
                    EventBus.getDefault().post(new MessageEvent(EventBusTags.MARK_AS_READ, selectedDialog));
                } else {
                    EventBus.getDefault().post(new MessageEvent(EventBusTags.MARK_AS_UNREAD, selectedDialog));
                }
            } else if (action == delete || action == clear) {
                if (count == 1) {
                    if (action == delete && canDeletePsaSelected) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("PsaHideChatAlertTitle", R.string.PsaHideChatAlertTitle));
                        builder.setMessage(LocaleController.getString("PsaHideChatAlertText", R.string.PsaHideChatAlertText));
                        builder.setPositiveButton(LocaleController.getString("PsaHide", R.string.PsaHide), (dialog1, which) -> {
                            getMessagesController().hidePromoDialog();
                            hideActionMode(false);
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                    } else {
                        AlertsCreator.createClearOrDeleteDialogAlert(DialogListActivity.this, action == clear, chat, user, DialogObject.isEncryptedDialog(dialog.id), action == delete, (param) -> {
                            hideActionMode(false);
                            if (action == clear && ChatObject.isChannel(chat) && (!chat.megagroup || !TextUtils.isEmpty(chat.username))) {
                                getMessagesController().deleteDialog(selectedDialog, 2, param);
                            } else {
                                getUndoView().showWithAction(selectedDialog, action == clear ? UndoView.ACTION_CLEAR : UndoView.ACTION_DELETE, deleteRunnable = () -> {
                                    performDeleteOrClearDialogAction(action, selectedDialog, chat, isBot, param);
                                    deleteRunnable = null;
                                });
                            }
                        });
                    }
                    return;
                } else {
                    if (getMessagesController().isPromoDialog(selectedDialog, true)) {
                        getMessagesController().hidePromoDialog();
                    } else {
                        if (action == clear && canClearCacheCount != 0) {
                            getMessagesController().deleteDialog(selectedDialog, 2, false);
                        } else {
                            performDeleteOrClearDialogAction(action, selectedDialog, chat, isBot, false);
                        }
                    }
                }
            } else if (action == mute) {
                if (count == 1 && canMuteCount == 1) {
                    showDialog(AlertsCreator.createMuteAlert(this, selectedDialog, null), dialog12 -> hideActionMode(true));
                    return;
                } else {
                    if (canUnmuteCount != 0) {
                        if (!getMessagesController().isDialogMuted(selectedDialog)) {
                            continue;
                        }
                        getNotificationsController().setDialogNotificationsSettings(selectedDialog, NotificationsController.SETTING_MUTE_UNMUTE);
                    } else {
                        if (getMessagesController().isDialogMuted(selectedDialog)) {
                            continue;
                        }
                        getNotificationsController().setDialogNotificationsSettings(selectedDialog, NotificationsController.SETTING_MUTE_FOREVER);
                    }
                }
            }
        }
        if (action == mute && !(count == 1 && canMuteCount == 1)) {
            BulletinFactory.createMuteBulletin(this, canUnmuteCount == 0, null).show();
        }
        if (action == pin || action == pin2) {
            if (filter != null) {
                FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, true, false, DialogListActivity.this, null);
            } else {
                getMessagesController().reorderPinnedDialogs(folderId, null, 0);
            }
        }
        hideActionMode(action != pin2 && action != pin && action != delete);
    }

    private void performDeleteOrClearDialogAction(int action, long selectedDialog, TLRPC.Chat chat, boolean isBot, boolean revoke) {
        if (action == clear) {
            getMessagesController().deleteDialog(selectedDialog, 1, revoke);
        } else {
            if (chat != null) {
                if (ChatObject.isNotInChat(chat)) {
                    getMessagesController().deleteDialog(selectedDialog, 0, revoke);
                } else {
                    TLRPC.User currentUser = getMessagesController().getUser(getUserConfig().getClientUserId());
                    getMessagesController().deleteParticipantFromChat((int) -selectedDialog, currentUser, null, null, revoke, false);
                }
            } else {
                getMessagesController().deleteDialog(selectedDialog, 0, revoke);
                if (isBot) {
                    getMessagesController().blockPeer((int) selectedDialog);
                }
            }
            if (AndroidUtilities.isTablet()) {
                getNotificationCenter().postNotificationName(NotificationCenter.closeChats, selectedDialog);
            }
            getMessagesController().checkIfFolderEmpty(folderId);
        }
    }

    public UndoView getUndoView() {
        if (undoView[0].getVisibility() == View.VISIBLE) {
            UndoView old = undoView[0];
            undoView[0] = undoView[1];
            undoView[1] = old;
            old.hide(true, 2);
            FrameLayout contentView = (FrameLayout) fragmentView;
            contentView.removeView(undoView[0]);
            contentView.addView(undoView[0]);
        }
        return undoView[0];
    }

    private void hideActionMode(boolean animateCheck) {
        actionBar.hideActionMode();
        if (menuDrawable != null) {
            actionBar.setBackButtonContentDescription(LocaleController.getString("AccDescrOpenMenu", R.string.AccDescrOpenMenu));
        }
        selectedDialogs.clear();
        if (currentTab == 0) {
            friendAdapter.updateSelect(selectedDialogs);
        } else if (currentTab == 1) {
            pinnedAdapter.updateSelect(selectedDialogs);
            participantAdapter.updateSelect(selectedDialogs);
        } else if (currentTab == 2) {
            otherAdapter.updateSelect(selectedDialogs);
        }
        if (menuDrawable != null) {
            menuDrawable.setRotation(0, true);
        } else if (backDrawable != null) {
            backDrawable.setRotation(0, true);
        }
//        if (filterTabsView != null) {
//            filterTabsView.animateColorsTo(Theme.key_actionBarTabLine, Theme.key_actionBarTabActiveText, Theme.key_actionBarTabUnactiveText, Theme.key_actionBarTabSelector, Theme.key_actionBarDefault);
//        }
//        if (actionBarColorAnimator != null) {
//            actionBarColorAnimator.cancel();
//        }
//        actionBarColorAnimator = ValueAnimator.ofFloat(progressToActionMode, 0);
//        actionBarColorAnimator.addUpdateListener(valueAnimator -> {
//            progressToActionMode = (float) valueAnimator.getAnimatedValue();
//            for (int i = 0; i < actionBar.getChildCount(); i++) {
//                if (actionBar.getChildAt(i).getVisibility() == View.VISIBLE && actionBar.getChildAt(i) != actionBar.getActionMode() && actionBar.getChildAt(i) != actionBar.getBackButton()) {
//                    if (actionBar.getChildAt(i) == homeTopView) continue;
//                    actionBar.getChildAt(i).setAlpha(1f - progressToActionMode);
//                }
//            }
//            if (homeTitleBar != null && progressToActionMode == 1f) {
//                homeTitleBar.setVisibility(View.VISIBLE);
//            }
//            if (fragmentView != null) {
//                fragmentView.invalidate();
//            }
//        });
//        actionBarColorAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
//        actionBarColorAnimator.setDuration(200);
//        actionBarColorAnimator.start();
//        allowMoving = false;
//        if (!movingDialogFilters.isEmpty()) {
//            for (int a = 0, N = movingDialogFilters.size(); a < N; a++) {
//                MessagesController.DialogFilter filter = movingDialogFilters.get(a);
//                FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, true, false, DialogsActivity.this, null);
//            }
//            movingDialogFilters.clear();
//        }
//        if (movingWas) {
//            getMessagesController().reorderPinnedDialogs(folderId, null, 0);
//            movingWas = false;
//        }
        updateCounters(true);
//        if (viewPages != null) {
//            for (int a = 0; a < viewPages.length; a++) {
//                viewPages[a].dialogsAdapter.onReorderStateChanged(false);
//            }
//        }
//        updateVisibleRows(MessagesController.UPDATE_MASK_REORDER | MessagesController.UPDATE_MASK_CHECK | (animateCheck ? MessagesController.UPDATE_MASK_CHAT : 0));
    }

    private void initView() {
        initIndicator();
        //好友
        View friendView = LayoutInflater.from(getParentActivity()).inflate(R.layout.view_dialog_list_comm, null);
        RecyclerView friendRv = friendView.findViewById(R.id.recycler_view);
        friendRv.setLayoutManager(new LinearLayoutManager(getParentActivity()));
        friendAdapter = new DialogListAdapter();
        friendAdapter.setOnItemClickListener(this);
        friendAdapter.setOnItemLongClickListener(this);
        friendRv.setAdapter(friendAdapter);

        //和我相关
        View atView = LayoutInflater.from(getParentActivity()).inflate(R.layout.view_dialog_list_at, null);
        TextView tv_archive_or_top = atView.findViewById(R.id.tv_archive_or_top);
        tv_archive_or_top.setText(LocaleController.getString("home_relatedme", R.string.home_relatedme));
        TextView tv_myjoin_group = atView.findViewById(R.id.tv_myjoin_group);
        tv_myjoin_group.setText(LocaleController.getString("message_center_myjoin_group", R.string.message_center_myjoin_group));
        RecyclerView rvPinned = atView.findViewById(R.id.rv_pinned);
        rvPinned.setLayoutManager(new LinearLayoutManager(getParentActivity()));
        pinnedAdapter = new DialogListAdapter(true, parentLayout);
        pinnedAdapter.setOnItemClickListener(this);
        pinnedAdapter.setOnItemLongClickListener(this);
        rvPinned.setAdapter(pinnedAdapter);

        RecyclerView rvParticipant = atView.findViewById(R.id.rv_participant);
        rvParticipant.setLayoutManager(new LinearLayoutManager(getParentActivity()));
        participantAdapter = new DialogListAdapter();
        participantAdapter.setOnItemClickListener(this);
        participantAdapter.setOnItemLongClickListener(this);
        rvParticipant.setAdapter(participantAdapter);

        //非联络人
        View otherView = LayoutInflater.from(getParentActivity()).inflate(R.layout.view_dialog_list_comm, null);
        RecyclerView otherRv = otherView.findViewById(R.id.recycler_view);
        otherRv.setLayoutManager(new LinearLayoutManager(getParentActivity()));
        otherAdapter = new DialogListAdapter();
        otherAdapter.setOnItemClickListener(this);
        otherAdapter.setOnItemLongClickListener(this);
        otherRv.setAdapter(otherAdapter);

        //viewpage
        List<View> list = new ArrayList<>();
        list.add(friendView);
        list.add(atView);
        list.add(otherView);
        DialogMixPageAdapter dialogMixPageAdapter = new DialogMixPageAdapter(list);
        binding.dialogViewpage.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                binding.magicIndicator.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                binding.magicIndicator.onPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                binding.magicIndicator.onPageScrollStateChanged(state);
            }
        });
        binding.dialogViewpage.setAdapter(dialogMixPageAdapter);

        loadData();

        if (type == 1) {
            binding.magicIndicator.onPageSelected(0);
            binding.dialogViewpage.setCurrentItem(0);
        } else if (type == 2) {
            binding.magicIndicator.onPageSelected(1);
            binding.dialogViewpage.setCurrentItem(1);
        } else if (type == 3) {
            binding.magicIndicator.onPageSelected(2);
            binding.dialogViewpage.setCurrentItem(2);
        }
    }

    private void initIndicator() {
        int textNormalColor = Color.parseColor("#56565C");
        int textSelectColor = Color.parseColor("#ffffff");

        int numberNormalColor = Color.parseColor("#ffffff");
        int numberSelectColor = Color.parseColor("#03BDFF");

        //MagicIndicator
        commonNavigator = new CommonNavigator(getParentActivity());
        //commonNavigator.setAdjustMode(true);
        commonNavigator.setAdapter(new CommonNavigatorAdapter() {
            @Override
            public int getCount() {
                return tabs.length;
            }

            @Override
            public IPagerTitleView getTitleView(Context context, final int index) {
                MCommonPagerTitleView commonPagerTitleView = new MCommonPagerTitleView(context);
                LinearLayout itemContentView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.view_dialog_tab_item, null);
                TextView textView = itemContentView.findViewById(R.id.tv_title);
                TextView textNumber = itemContentView.findViewById(R.id.tv_number);
                textView.setText(tabs[index]);
                if (0 == index) {
                    //好友
                    int value = DialogManager.getInstance(UserConfig.selectedAccount).contactUnRead;
                    if (value == 0) {
                        textNumber.setVisibility(View.GONE);
                    } else {
                        textNumber.setVisibility(View.VISIBLE);
                        textNumber.setText(value + "");
                    }
                } else if (1 == index) {
                    //@我
                    int value = DialogManager.getInstance(UserConfig.selectedAccount).relatedMeUnRead;
                    if (value == 0) {
                        textNumber.setVisibility(View.GONE);
                    } else {
                        textNumber.setVisibility(View.VISIBLE);
                        textNumber.setText(value + "");
                    }
                } else if (2 == index) {
                    //非联系人
                    int value = DialogManager.getInstance(UserConfig.selectedAccount).nonContactUnRead;
                    if (value == 0) {
                        textNumber.setVisibility(View.GONE);
                    } else {
                        textNumber.setVisibility(View.VISIBLE);
                        textNumber.setText(value + "");
                    }
                }
                commonPagerTitleView.setContentView(itemContentView);
                commonPagerTitleView.setOnPagerTitleChangeListener(new CommonPagerTitleView.OnPagerTitleChangeListener() {
                    @Override
                    public void onSelected(int index, int totalCount) {
                        itemContentView.setSelected(true);
                        if (index == currentTab) return;
                        currentTab = index;
                        if (currentTab == 1) {
                            relateMeItem.setVisibility(View.VISIBLE);
                        } else {
                            relateMeItem.setVisibility(View.GONE);
                        }

                        if (currentTab == 2) {
                            deleteOtherViewDialogs.setVisibility(View.VISIBLE);
                        } else {
                            deleteOtherViewDialogs.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onDeselected(int index, int totalCount) {
                        itemContentView.setSelected(false);
                    }

                    @Override
                    public void onLeave(int index, int totalCount, float leavePercent, boolean leftToRight) {
                        //渐变色
                        //textView.setTextColor(ArgbEvaluatorHolder.eval(leavePercent, textSelectColor, textNormalColor));
                        //textNumber.setTextColor(ArgbEvaluatorHolder.eval(leavePercent, numberSelectColor, numberNormalColor));
                    }

                    @Override
                    public void onEnter(int index, int totalCount, float enterPercent, boolean leftToRight) {
                        //渐变色
                        //textView.setTextColor(ArgbEvaluatorHolder.eval(enterPercent, textNormalColor, textSelectColor));
                        //textNumber.setTextColor(ArgbEvaluatorHolder.eval(enterPercent, numberNormalColor, numberSelectColor));
                    }
                });
                commonPagerTitleView.setOnClickListener(v -> {
                    binding.dialogViewpage.setCurrentItem(index);
                });
                return commonPagerTitleView;
            }

            @Override
            public IPagerIndicator getIndicator(Context context) {
                LinePagerIndicator indicator = new LinePagerIndicator(context);
                float navigatorHeight = ConvertUtils.dp2px(40);
                //indicator.setLineHeight(navigatorHeight);
                indicator.setLineHeight(0);
                indicator.setRoundRadius(navigatorHeight / 2);
                indicator.setYOffset(0);
                indicator.setColors(context.getResources().getColor(R.color.theme_color));
                return indicator;
            }
        });
        binding.magicIndicator.setNavigator(commonNavigator);
        //binding.magicIndicator.onPageSelected(currentTab);

        //item间隔 must after setNavigator
        LinearLayout titleContainer = commonNavigator.getTitleContainer();
        titleContainer.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        titleContainer.setDividerPadding(UIUtil.dip2px(getParentActivity(), 10));
        titleContainer.setDividerDrawable(getParentActivity().getResources().getDrawable(R.drawable.ic_dialog_tab_splitter));
    }

    private void loadData() {
        friendAdapter.setList(DialogManager.getInstance(currentAccount).contactDialogs);
        pinnedAdapter.setList(DialogManager.getInstance(currentAccount).relatedMeDialogs.get(2));
        participantAdapter.setList(DialogManager.getInstance(currentAccount).relatedMeDialogs.get(3));
        otherAdapter.setList(DialogManager.getInstance(currentAccount).nonContactDialogs);
    }

    private void maskAsRead(List<TLRPC.Dialog> list) {
        List<Long> selectedDialog = new ArrayList<>();
        for (TLRPC.Dialog dialog : list) {
            if (dialog.unread_count > 0) {
                selectedDialog.add(dialog.id);
            }
        }
        EventBus.getDefault().post(new MessageEvent(EventBusTags.MARK_AS_READ, selectedDialog));
        fragmentView.postDelayed(new Runnable() {
            @Override
            public void run() {
                DialogManager.getInstance(currentAccount).updateAllDialogs(false);
            }
        }, 500);
    }

    @Override
    public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
        TLRPC.Dialog dialog = (TLRPC.Dialog) adapter.getItem(position);
        if (actionBar.isActionModeShowed()) {
            if (dialog instanceof TLRPC.TL_dialogFolder) {
                return;
            }
            showOrUpdateActionMode(dialog.id, position);
            return;
        }
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

    @Override
    public boolean onItemLongClick(@NonNull BaseQuickAdapter adapter, @NonNull View view, int position) {
        TLRPC.Dialog dialog = (TLRPC.Dialog) adapter.getItem(position);
        if (dialog instanceof TLRPC.TL_dialogFolder) {
            return false;
        }
        showOrUpdateActionMode(dialog.id, position);
        return true;
    }

    private void showOrUpdateActionMode(long dialogId, int position) {
        cleanAllItem.setVisibility(View.GONE);
        addOrRemoveSelectedDialog(dialogId, position);
        boolean updateAnimated = false;
        if (actionBar.isActionModeShowed()) {
            if (selectedDialogs.isEmpty()) {
                hideActionMode(true);
                return;
            }
            updateAnimated = true;
        } else {
            createActionMode(null);
            AndroidUtilities.hideKeyboard(fragmentView.findFocus());
            actionBar.setActionModeOverrideColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            actionBar.showActionMode();
//            resetScroll();
            if (menuDrawable != null) {
                actionBar.setBackButtonContentDescription(LocaleController.getString("AccDescrGoBack", R.string.AccDescrGoBack));
            }
//            if (getPinnedCount() > 1) {
//                if (viewPages != null) {
//                    for (int a = 0; a < viewPages.length; a++) {
//                        viewPages[a].dialogsAdapter.onReorderStateChanged(true);
//                    }
//                }
//                updateVisibleRows(MessagesController.UPDATE_MASK_REORDER);
//            }

//            if (!searchIsShowed) {
            AnimatorSet animatorSet = new AnimatorSet();
            ArrayList<Animator> animators = new ArrayList<>();
            for (int a = 0; a < actionModeViews.size(); a++) {
                View view = actionModeViews.get(a);
                view.setPivotY(ActionBar.getCurrentActionBarHeight() / 2);
                AndroidUtilities.clearDrawableAnimation(view);
                animators.add(ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.1f, 1.0f));
            }
            animatorSet.playTogether(animators);
            animatorSet.setDuration(200);
            animatorSet.start();
//            }

//            if (actionBarColorAnimator != null) {
//                actionBarColorAnimator.cancel();
//            }
//            actionBarColorAnimator = ValueAnimator.ofFloat(progressToActionMode, 1f);
//            actionBarColorAnimator.addUpdateListener(valueAnimator -> {
//                progressToActionMode = (float) valueAnimator.getAnimatedValue();
//                for (int i = 0; i < actionBar.getChildCount(); i++) {
//                    if (actionBar.getChildAt(i).getVisibility() == View.VISIBLE && actionBar.getChildAt(i) != actionBar.getActionMode() && actionBar.getChildAt(i) != actionBar.getBackButton()) {
//                        if (actionBar.getChildAt(i) == homeTopView) continue;
//                        actionBar.getChildAt(i).setAlpha(1f - progressToActionMode);
//                    }
//                }
//                if (homeTitleBar != null && progressToActionMode == 0f) {
//                    homeTitleBar.setVisibility(View.GONE);
//                }
//                if (fragmentView != null) {
//                    fragmentView.invalidate();
//                }
//            });
//            actionBarColorAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
//            actionBarColorAnimator.setDuration(200);
//            actionBarColorAnimator.start();
//
//            if (filterTabsView != null) {
//                filterTabsView.animateColorsTo(Theme.key_profile_tabSelectedLine, Theme.key_profile_tabSelectedText, Theme.key_profile_tabText, Theme.key_profile_tabSelector, Theme.key_actionBarActionModeDefault);
//            }
            if (menuDrawable != null) {
                menuDrawable.setRotateToBack(false);
                menuDrawable.setRotation(1, true);
            } else if (backDrawable != null) {
                backDrawable.setRotation(1, true);
            }
        }
        updateCounters(false);
        selectedDialogsCountTextView.setNumber(selectedDialogs.size(), updateAnimated);
    }

    public void addOrRemoveSelectedDialog(long did, int position) {
        if (selectedDialogs.contains(did)) {
            selectedDialogs.remove(did);
        } else {
            selectedDialogs.add(did);
        }
        if (currentTab == 0) {
            friendAdapter.updateSelect(selectedDialogs);
        } else if (currentTab == 1) {
            pinnedAdapter.updateSelect(selectedDialogs);
            participantAdapter.updateSelect(selectedDialogs);
        } else if (currentTab == 2) {
            otherAdapter.updateSelect(selectedDialogs);
        }
    }

    private void updateCounters(boolean hide) {
        int canClearHistoryCount = 0;
        int canDeleteCount = 0;
        int canUnpinCount = 0;
        int canArchiveCount = 0;
        canDeletePsaSelected = false;
        canUnarchiveCount = 0;
        canUnmuteCount = 0;
        canMuteCount = 0;
        canPinCount = 0;
        canReadCount = 0;
        canClearCacheCount = 0;
        int cantBlockCount = 0;
        canReportSpamCount = 0;
        if (hide) {
            return;
        }
        int count = selectedDialogs.size();
        long selfUserId = getUserConfig().getClientUserId();
        SharedPreferences preferences = getNotificationsSettings();
        for (int a = 0; a < count; a++) {
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(selectedDialogs.get(a));
            if (dialog == null) {
                continue;
            }

            long selectedDialog = dialog.id;
            boolean pinned = isDialogPinned(dialog);
            boolean hasUnread = dialog.unread_count != 0 || dialog.unread_mark;
            if (getMessagesController().isDialogMuted(selectedDialog)) {
                canUnmuteCount++;
            } else {
                canMuteCount++;
            }

            if (hasUnread) {
                canReadCount++;
            }

            if (dialog.folder_id == 1) {
                canUnarchiveCount++;
            } else if (selectedDialog != selfUserId && selectedDialog != 777000 && !getMessagesController().isPromoDialog(selectedDialog, false)) {
                canArchiveCount++;
            }

            if (!DialogObject.isUserDialog(selectedDialog) || selectedDialog == selfUserId) {
                cantBlockCount++;
            } else {
                TLRPC.User user = getMessagesController().getUser(selectedDialog);
                if (MessagesController.isSupportUser(user)) {
                    cantBlockCount++;
                } else {
                    if (preferences.getBoolean("dialog_bar_report" + selectedDialog, true)) {
                        canReportSpamCount++;
                    }
                }
            }

            if (DialogObject.isChannel(dialog)) {
                final TLRPC.Chat chat = getMessagesController().getChat(-selectedDialog);
                CharSequence[] items;
                if (getMessagesController().isPromoDialog(dialog.id, true)) {
                    canClearCacheCount++;
                    if (getMessagesController().promoDialogType == MessagesController.PROMO_TYPE_PSA) {
                        canDeleteCount++;
                        canDeletePsaSelected = true;
                    }
                } else {
                    if (pinned) {
                        canUnpinCount++;
                    } else {
                        canPinCount++;
                    }
                    if (chat != null && chat.megagroup) {
                        if (TextUtils.isEmpty(chat.username)) {
                            canClearHistoryCount++;
                        } else {
                            canClearCacheCount++;
                        }
                    } else {
                        canClearCacheCount++;
                    }
                    canDeleteCount++;
                }
            } else {
                final boolean isChat = DialogObject.isChatDialog(dialog.id);
                TLRPC.User user;
                TLRPC.Chat chat = isChat ? getMessagesController().getChat(-dialog.id) : null;
                if (DialogObject.isEncryptedDialog(dialog.id)) {
                    TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(dialog.id));
                    if (encryptedChat != null) {
                        user = getMessagesController().getUser(encryptedChat.user_id);
                    } else {
                        user = new TLRPC.TL_userEmpty();
                    }
                } else {
                    user = !isChat && DialogObject.isUserDialog(dialog.id) ? getMessagesController().getUser(dialog.id) : null;
                }
                final boolean isBot = user != null && user.bot && !MessagesController.isSupportUser(user);

                if (pinned) {
                    canUnpinCount++;
                } else {
                    canPinCount++;
                }
                canClearHistoryCount++;
                canDeleteCount++;
            }
        }
        if (canDeleteCount != count) {
            deleteItem.setVisibility(View.GONE);
        } else {
            deleteItem.setVisibility(View.VISIBLE);
        }
        if (canClearCacheCount != 0 && canClearCacheCount != count || canClearHistoryCount != 0 && canClearHistoryCount != count) {
            clearItem.setVisibility(View.GONE);
        } else {
            clearItem.setVisibility(View.VISIBLE);
            if (canClearCacheCount != 0) {
                clearItem.setText(LocaleController.getString("ClearHistoryCache", R.string.ClearHistoryCache));
            } else {
                clearItem.setText(LocaleController.getString("ClearHistory", R.string.ClearHistory));
            }
        }
        if (canUnarchiveCount != 0) {
            final String contentDescription = LocaleController.getString("Unarchive", R.string.Unarchive);
            archiveItem.setTextAndIcon(contentDescription, R.drawable.msg_unarchive);
            archive2Item.setIcon(R.drawable.msg_unarchive);
            archive2Item.setContentDescription(contentDescription);
            archive2Item.setVisibility(View.VISIBLE);
            archiveItem.setVisibility(View.GONE);
        } else if (canArchiveCount != 0) {
            final String contentDescription = LocaleController.getString("Archive", R.string.Archive);
            archiveItem.setTextAndIcon(contentDescription, R.drawable.msg_archive);
            archive2Item.setIcon(R.drawable.msg_archive);
            archive2Item.setContentDescription(contentDescription);
            archive2Item.setVisibility(View.VISIBLE);
            archiveItem.setVisibility(View.GONE);
        } else {
            archiveItem.setVisibility(View.GONE);
            archive2Item.setVisibility(View.GONE);
        }
        if (canPinCount + canUnpinCount != count) {
            pinItem.setVisibility(View.GONE);
            pin2Item.setVisibility(View.GONE);
        } else {
            pin2Item.setVisibility(View.GONE);
            pinItem.setVisibility(View.GONE);
        }
        if (cantBlockCount != 0) {
            blockItem.setVisibility(View.GONE);
        } else {
            blockItem.setVisibility(View.VISIBLE);
        }
        if (!FiltersListBottomSheet.getCanAddDialogFilters(this, selectedDialogs).isEmpty()) {
            addToFolderItem.setVisibility(View.VISIBLE);
        } else {
            addToFolderItem.setVisibility(View.GONE);
        }
        if (canUnmuteCount != 0) {
            muteItem.setIcon(R.drawable.msg_unmute);
            muteItem.setContentDescription(LocaleController.getString("ChatsUnmute", R.string.ChatsUnmute));
        } else {
            muteItem.setIcon(R.drawable.msg_mute);
            muteItem.setContentDescription(LocaleController.getString("ChatsMute", R.string.ChatsMute));
        }
        if (canReadCount != 0) {
            readItem.setTextAndIcon(LocaleController.getString("MarkAsRead", R.string.MarkAsRead), R.drawable.msg_markread);
        } else {
            readItem.setTextAndIcon(LocaleController.getString("MarkAsUnread", R.string.MarkAsUnread), R.drawable.msg_markunread);
        }
        if (canPinCount != 0) {
            pinItem.setIcon(R.drawable.msg_pin);
            pinItem.setContentDescription(LocaleController.getString("PinToTop", R.string.PinToTop));
            pin2Item.setText(LocaleController.getString("DialogPin", R.string.DialogPin));
        } else {
            pinItem.setIcon(R.drawable.msg_unpin);
            pinItem.setContentDescription(LocaleController.getString("UnpinFromTop", R.string.UnpinFromTop));
            pin2Item.setText(LocaleController.getString("DialogUnpin", R.string.DialogUnpin));
        }
    }

    private boolean isDialogPinned(TLRPC.Dialog dialog) {
        MessagesController.DialogFilter filter = getCurrentDialogFilter();
        if (filter != null) {
            return filter.pinnedDialogs.indexOfKey(dialog.id) >= 0;
        }
        return dialog.pinned;
    }

    private MessagesController.DialogFilter getCurrentDialogFilter() {
        if (currentTab == 1) return null;
        return CollectionUtils.find(getMessagesController().dialogFilters, new CollectionUtils.Predicate<MessagesController.DialogFilter>() {
            @Override
            public boolean evaluate(MessagesController.DialogFilter item) {
                return item.flags == (currentTab == 0 ? MessagesController.DIALOG_FILTER_FLAG_CONTACTS : MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS);
            }
        });
    }

    @Override
    public boolean onBackPressed() {
        if (actionBar != null && actionBar.isActionModeShowed()) {
            hideActionMode(true);
            return false;
        }
        return super.onBackPressed();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessage(MessageEvent event) {
        switch (event.getType()) {
            case EventBusTags.UPDATE_DIALOGS_DATA:
                if (actionBar.isActionModeShowed()) return;
                if (event.getFrom().equals(type + "")) {
                    loadData();
                }
                break;
            case EventBusTags.UPDATE_UNREAD_COUNT:
                int value = 0;
                View contentView = null;
                if ("1".equals(event.getFrom())) {
                    //联系人
                    value = (Integer) event.getData();
                    MCommonPagerTitleView commonPagerTitleView = (MCommonPagerTitleView) commonNavigator.getPagerTitleView(0);
                    contentView = commonPagerTitleView.getContentView();
                } else if ("2".equals(event.getFrom())) {
                    //@我的
                    value = (Integer) event.getData();
                    MCommonPagerTitleView commonPagerTitleView = (MCommonPagerTitleView) commonNavigator.getPagerTitleView(1);
                    contentView = commonPagerTitleView.getContentView();
                } else if ("3".equals(event.getFrom())) {
                    //非联系人
                    value = (Integer) event.getData();
                    MCommonPagerTitleView commonPagerTitleView = (MCommonPagerTitleView) commonNavigator.getPagerTitleView(2);
                    contentView = commonPagerTitleView.getContentView();
                }
                if (contentView != null) {
                    if (value == 0) {
                        contentView.findViewById(R.id.tv_number).setVisibility(View.GONE);
                    } else {
                        TextView textNumber = contentView.findViewById(R.id.tv_number);
                        textNumber.setVisibility(View.VISIBLE);
                        textNumber.setText(value + "");
                    }
                }
                break;
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.dialogDeleted) {
            DialogManager.getInstance(currentAccount).getRelatedMeDialogs(null, false);
        } else if (id == NotificationCenter.needDeleteDialog) {
            // 代码来自：DialogsActivity#7519
            long dialogId = (Long) args[0];
            TLRPC.User user = (TLRPC.User) args[1];
            TLRPC.Chat chat = (TLRPC.Chat) args[2];
            boolean revoke = (Boolean) args[3];
            deleteRunnable = () -> {
                if (chat != null) {
                    if (ChatObject.isNotInChat(chat)) {
                        getMessagesController().deleteDialog(dialogId, 0, revoke);
                    } else {
                        getMessagesController().deleteParticipantFromChat(-dialogId, getMessagesController().getUser(getUserConfig().getClientUserId()), null, null, revoke, revoke);
                    }
                } else {
                    getMessagesController().deleteDialog(dialogId, 0, revoke);
                    if (user != null && user.bot) {
                        getMessagesController().blockPeer(user.id);
                    }
                }
                getMessagesController().checkIfFolderEmpty(folderId);
                deleteRunnable = null;
            };
            if (undoView[0] != null) {
                getUndoView().showWithAction(dialogId, UndoView.ACTION_DELETE, deleteRunnable);
            } else {
                deleteRunnable.run();
            }
        }
    }
}