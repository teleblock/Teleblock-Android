package org.telegram.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.GroupCreateSpan;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.LimitReachedBottomSheet;
import org.telegram.ui.Components.ProgressButton;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public class FiltersSetupActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private ListAdapter adapter;
    private ItemTouchHelper itemTouchHelper;

    private boolean orderChanged;
    private boolean showAllChats;

    private int filterHelpRow;
    private int recommendedHeaderRow;
    private int recommendedStartRow;
    private int recommendedEndRow;
    private int recommendedSectionRow;
    private int createRecommendedAllRow;
    private int deleteRecommendedAllRow;
    private int filtersHeaderRow;
    private int filtersStartRow;
    private int filtersEndRow;
    private int createFilterRow;
    private int createSectionRow;
    private int rowCount = 0;

    private boolean ignoreUpdates;

    private ArrayList<TLRPC.TL_dialogFilterSuggested> suggestedFilters = new ArrayList<>();
    private ArrayList<MessagesController.DialogFilter> localFilter = new ArrayList<>();

    public static class TextCell extends FrameLayout {
        private boolean isCenter;
        private SimpleTextView textView;
        private ImageView imageView;

        public TextCell(Context context) {
            super(context);
            initView(context);
        }

        public TextCell(Context context,boolean isCenter) {
            super(context);
            this.isCenter = isCenter;
            initView(context);
        }

        private void initView(Context context){
            textView = new SimpleTextView(context);
            textView.setTextSize(16);
            textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2));
            textView.setTag(Theme.key_windowBackgroundWhiteBlueText2);
            addView(textView);

            imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            addView(imageView);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = AndroidUtilities.dp(48);

            textView.measure(MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(71 + 23), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.EXACTLY));
            imageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(50), MeasureSpec.EXACTLY));
            setMeasuredDimension(width, AndroidUtilities.dp(50));
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int height = bottom - top;
            int width = right - left;

            int viewLeft;
            int viewTop = (height - textView.getTextHeight()) / 2;
            if (LocaleController.isRTL) {
                viewLeft = getMeasuredWidth() - textView.getMeasuredWidth() - AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? 64 : 23);
            } else {
                viewLeft = AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? 64 : 23);
            }
            if(isCenter){
                viewLeft = (getMeasuredWidth() - textView.getMeasuredWidth()) / 2;
            }
            textView.layout(viewLeft, viewTop, viewLeft + textView.getMeasuredWidth(), viewTop + textView.getMeasuredHeight());

            viewLeft = !LocaleController.isRTL ? AndroidUtilities.dp(20) : width - imageView.getMeasuredWidth() - AndroidUtilities.dp(20);
            imageView.layout(viewLeft, 0, viewLeft + imageView.getMeasuredWidth(), imageView.getMeasuredHeight());
        }

        public void setGravity(int gravity) {
            textView.setGravity(gravity);
        }

        public void setTextDrawableLeft(int id) {
            textView.setLeftDrawable(id);
            textView.setDrawablePadding(AndroidUtilities.dp(5));
        }

        public void setTextAndIcon(String text, Drawable icon, boolean divider) {
            textView.setText(text);
            if (icon != null) {
                imageView.setImageDrawable(icon);
            }
        }

        public void setTextAndIcon(String text, int color, Drawable icon, boolean divider) {
            setTextAndIcon(text, icon, divider);
            textView.setTextColor(color);
        }
    }

    public static class SuggestedFilterCell extends FrameLayout {

        private TextView textView;
        private TextView valueTextView;
        private ProgressButton addButton;
        private boolean needDivider;
        private TLRPC.TL_dialogFilterSuggested suggestedFilter;

        public SuggestedFilterCell(Context context) {
            super(context);

            textView = new TextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setLines(1);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 22, 10, 22, 0));

            valueTextView = new TextView(context);
            valueTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            valueTextView.setLines(1);
            valueTextView.setMaxLines(1);
            valueTextView.setSingleLine(true);
            valueTextView.setEllipsize(TextUtils.TruncateAt.END);
            valueTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 22, 35, 22, 0));

            addButton = new ProgressButton(context);
            addButton.setText(LocaleController.getString("Add", R.string.Add));
            addButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
            addButton.setProgressColor(Theme.getColor(Theme.key_featuredStickers_buttonProgress));
            addButton.setBackgroundRoundRect(Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButtonPressed));
            addView(addButton, LayoutHelper.createFrameRelatively(LayoutHelper.WRAP_CONTENT, 28, Gravity.TOP | Gravity.END, 0, 18, 14, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(64));
            measureChildWithMargins(addButton, widthMeasureSpec, 0, heightMeasureSpec, 0);
            measureChildWithMargins(textView, widthMeasureSpec, addButton.getMeasuredWidth(), heightMeasureSpec, 0);
            measureChildWithMargins(valueTextView, widthMeasureSpec, addButton.getMeasuredWidth(), heightMeasureSpec, 0);
        }

        public void setFilter(TLRPC.TL_dialogFilterSuggested filter, boolean divider) {
            needDivider = divider;
            suggestedFilter = filter;
            setWillNotDraw(!needDivider);

            textView.setText(filter.filter.title);
            valueTextView.setText(filter.description);
        }

        public TLRPC.TL_dialogFilterSuggested getSuggestedFilter() {
            return suggestedFilter;
        }

        public void setAddOnClickListener(OnClickListener onClickListener) {
            addButton.setOnClickListener(onClickListener);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (needDivider) {
                canvas.drawLine(0, getHeight() - 1, getWidth() - getPaddingRight(), getHeight() - 1, Theme.dividerPaint);
            }
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setEnabled(true);
            info.setText(addButton.getText());
            info.setClassName("android.widget.Button");
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    public static class HintInnerCell extends FrameLayout {

        private RLottieImageView imageView;
        private TextView messageTextView;

        public HintInnerCell(Context context) {
            super(context);

            imageView = new RLottieImageView(context);
            imageView.setAnimation(R.raw.filters, 90, 90);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.playAnimation();
            imageView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(imageView, LayoutHelper.createFrame(90, 90, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 14, 0, 0));
            imageView.setOnClickListener(v -> {
                if (!imageView.isPlaying()) {
                    imageView.setProgress(0.0f);
                    imageView.playAnimation();
                }
            });

            messageTextView = new TextView(context);
            messageTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
            messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            messageTextView.setGravity(Gravity.CENTER);
            messageTextView.setText(AndroidUtilities.replaceTags(LocaleController.formatString("CreateNewFilterInfo", R.string.CreateNewFilterInfo)));
            addView(messageTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 40, 121, 40, 24));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), heightMeasureSpec);
        }
    }

    public static class FilterCell extends FrameLayout {

        private SimpleTextView textView;
        private TextView valueTextView;
        @SuppressWarnings("FieldCanBeLocal")
        private ImageView moveImageView;
        @SuppressWarnings("FieldCanBeLocal")
        private ImageView optionsImageView;
        private boolean needDivider;
        float progressToLock;

        private MessagesController.DialogFilter currentFilter;

        public FilterCell(Context context) {
            super(context);
            setWillNotDraw(false);

            moveImageView = new ImageView(context);
            moveImageView.setFocusable(false);
            moveImageView.setScaleType(ImageView.ScaleType.CENTER);
            moveImageView.setImageResource(R.drawable.list_reorder);
            moveImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_stickers_menu), PorterDuff.Mode.MULTIPLY));
            moveImageView.setContentDescription(LocaleController.getString("FilterReorder", R.string.FilterReorder));
            moveImageView.setClickable(true);
            addView(moveImageView, LayoutHelper.createFrame(48, 48, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 6, 0, 6, 0));

            textView = new SimpleTextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textView.setTextSize(16);
            textView.setMaxLines(1);
            textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.other_lockedfolders2);
            drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_stickers_menu), PorterDuff.Mode.MULTIPLY));
            textView.setRightDrawable(drawable);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 80 : 64, 14, LocaleController.isRTL ? 64 : 80, 0));

            valueTextView = new TextView(context);
            valueTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            valueTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            valueTextView.setLines(1);
            valueTextView.setMaxLines(1);
            valueTextView.setSingleLine(true);
            valueTextView.setPadding(0, 0, 0, 0);
            valueTextView.setEllipsize(TextUtils.TruncateAt.END);
            addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 80 : 64, 35, LocaleController.isRTL ? 64 : 80, 0));
            valueTextView.setVisibility(GONE);

            optionsImageView = new ImageView(context);
            optionsImageView.setFocusable(false);
            optionsImageView.setScaleType(ImageView.ScaleType.CENTER);
            optionsImageView.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_stickers_menuSelector)));
            optionsImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_stickers_menu), PorterDuff.Mode.MULTIPLY));
            optionsImageView.setImageResource(R.drawable.msg_actions);
            optionsImageView.setContentDescription(LocaleController.getString("AccDescrMoreOptions", R.string.AccDescrMoreOptions));
            addView(optionsImageView, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 6, 0, 6, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(50), MeasureSpec.EXACTLY));
        }

        public void setFilter(MessagesController.DialogFilter filter, boolean divider) {
            int oldId = currentFilter == null ? -1 : currentFilter.id;
            currentFilter = filter;
            int newId = currentFilter == null ? -1 : currentFilter.id;
            boolean animated = oldId != newId;

            StringBuilder info = new StringBuilder();
            if (filter.isDefault() || (filter.flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) == MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) {
                info.append(LocaleController.getString("FilterAllChats", R.string.FilterAllChats));
            } else {
                if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_CONTACTS) != 0) {
                    if (info.length() != 0) {
                        info.append(", ");
                    }
                    info.append(LocaleController.getString("FilterContacts", R.string.FilterContacts));
                }
                if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) != 0) {
                    if (info.length() != 0) {
                        info.append(", ");
                    }
                    info.append(LocaleController.getString("FilterNonContacts", R.string.FilterNonContacts));
                }
                if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_GROUPS) != 0) {
                    if (info.length() != 0) {
                        info.append(", ");
                    }
                    info.append(LocaleController.getString("FilterGroups", R.string.FilterGroups));
                }
                if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_CHANNELS) != 0) {
                    if (info.length() != 0) {
                        info.append(", ");
                    }
                    info.append(LocaleController.getString("FilterChannels", R.string.FilterChannels));
                }
                if ((filter.flags & MessagesController.DIALOG_FILTER_FLAG_BOTS) != 0) {
                    if (info.length() != 0) {
                        info.append(", ");
                    }
                    info.append(LocaleController.getString("FilterBots", R.string.FilterBots));
                }
            }
            if (!filter.alwaysShow.isEmpty() || !filter.neverShow.isEmpty()) {
                if (info.length() != 0) {
                    info.append(", ");
                }
                info.append(LocaleController.formatPluralString("Exception", filter.alwaysShow.size() + filter.neverShow.size()));
            }
            if (info.length() == 0) {
                info.append(LocaleController.getString("FilterNoChats", R.string.FilterNoChats));
            }

            String name = filter.name;
            if (filter.isDefault()) {
                name = LocaleController.getString("FilterAllChats", R.string.FilterAllChats);
            }
            if (!animated) {
                progressToLock = currentFilter.locked ? 1f : 0;
            }
            textView.setText(Emoji.replaceEmoji(name, textView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(20), false));

            valueTextView.setText(info);
            needDivider = divider;

            if (filter.isDefault()) {
                optionsImageView.setVisibility(View.GONE);
            } else {
                optionsImageView.setVisibility(View.VISIBLE);
            }
            invalidate();
        }

        public MessagesController.DialogFilter getCurrentFilter() {
            return currentFilter;
        }

        public void setOnOptionsClick(OnClickListener listener) {
            optionsImageView.setOnClickListener(listener);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (needDivider) {
                canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(62), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(62) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
            if (currentFilter != null) {
                if (currentFilter.locked && progressToLock != 1f) {
                    progressToLock += 16 / 150f;
                    invalidate();
                } else if (!currentFilter.locked && progressToLock != 0) {
                    progressToLock -= 16 / 150f;
                    invalidate();
                }
            }
            progressToLock = Utilities.clamp(progressToLock, 1f, 0f);
            textView.setRightDrawableScale(progressToLock);
            textView.invalidate();
        }

        @SuppressLint("ClickableViewAccessibility")
        public void setOnReorderButtonTouchListener(OnTouchListener listener) {
            moveImageView.setOnTouchListener(listener);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        updateRows(true);
        getMessagesController().loadRemoteFilters(true);
        getNotificationCenter().addObserver(this, NotificationCenter.dialogFiltersUpdated);
        getNotificationCenter().addObserver(this, NotificationCenter.suggestedFiltersLoaded);
        //getMessagesController().loadSuggestedFilters();
        return super.onFragmentCreate();
    }

    private void updateRows(boolean notify) {
        recommendedHeaderRow = -1;
        recommendedStartRow = -1;
        recommendedEndRow = -1;
        recommendedSectionRow = -1;
        createRecommendedAllRow = -1;
        deleteRecommendedAllRow = -1;

        handlerFilterSuggested();

        rowCount = 0;
        filterHelpRow = rowCount++;
        int count = getMessagesController().dialogFilters.size();
        if (!getUserConfig().isPremium()) {
            count--;
            showAllChats = false;
        } else {
            showAllChats = true;
        }
        if (!suggestedFilters.isEmpty() /*&& count < 10*/) {
            recommendedHeaderRow = rowCount++;
            recommendedStartRow = rowCount;
            rowCount += suggestedFilters.size();
            recommendedEndRow = rowCount;
            if (suggestedFilters.size() > 0) {
                createRecommendedAllRow = rowCount++;
            }
            recommendedSectionRow = rowCount++;
        }

        if (count != 0) {
            filtersHeaderRow = rowCount++;
            filtersStartRow = rowCount;
            rowCount += count;
            filtersEndRow = rowCount;
        } else {
            filtersHeaderRow = -1;
            filtersStartRow = -1;
            filtersEndRow = -1;
        }
        if (count > 0) {
            deleteRecommendedAllRow = rowCount++;
            createSectionRow = rowCount++;
        }
//        if (count < getMessagesController().dialogFiltersLimitPremium) {
        createFilterRow = rowCount++;
//        } else {
//            createFilterRow = -1;
//        }
        if (notify && adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onFragmentDestroy() {
        getNotificationCenter().removeObserver(this, NotificationCenter.dialogFiltersUpdated);
        getNotificationCenter().removeObserver(this, NotificationCenter.suggestedFiltersLoaded);
        if (orderChanged) {
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            getMessagesStorage().saveDialogFiltersOrder();
            TLRPC.TL_messages_updateDialogFiltersOrder req = new TLRPC.TL_messages_updateDialogFiltersOrder();
            ArrayList<MessagesController.DialogFilter> filters = getMessagesController().dialogFilters;
            for (int a = 0, N = filters.size(); a < N; a++) {
                MessagesController.DialogFilter filter = filters.get(a);
                req.order.add(filter.id);
            }
            getConnectionsManager().sendRequest(req, (response, error) -> {

            });
        }
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("home_msg_group", R.string.home_msg_group));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context) {
            @Override
            public boolean onTouchEvent(MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    AndroidUtilities.runOnUIThread(() -> {
                        getMessagesController().lockFiltersInternal();
                    }, 250);
                }
                return super.onTouchEvent(e);
            }
        };
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        itemTouchHelper = new ItemTouchHelper(new TouchHelperCallback());
        itemTouchHelper.attachToRecyclerView(listView);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(adapter = new ListAdapter(context));
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position >= filtersStartRow && position < filtersEndRow) {
                int filterPosition = position - filtersStartRow;
                if (!showAllChats) {
                    filterPosition++;
                }
                if (getMessagesController().dialogFilters.get(filterPosition).isDefault()) {
                    return;
                }
                MessagesController.DialogFilter filter = getMessagesController().dialogFilters.get(filterPosition);
                if (filter.locked) {
                    showDialog(new LimitReachedBottomSheet(this, context, LimitReachedBottomSheet.TYPE_FOLDERS, currentAccount));
                } else {
                    presentFragment(new FilterCreateActivity(getMessagesController().dialogFilters.get(filterPosition)));
                }
            } else if (position == createFilterRow) {
                if ((getMessagesController().dialogFilters.size() - 1 >= getMessagesController().dialogFiltersLimitDefault && !getUserConfig().isPremium()) || getMessagesController().dialogFilters.size() >= getMessagesController().dialogFiltersLimitPremium) {
                    showDialog(new LimitReachedBottomSheet(this, context, LimitReachedBottomSheet.TYPE_FOLDERS, currentAccount));
                } else {
                    presentFragment(new FilterCreateActivity());
                }
            }
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.dialogFiltersUpdated) {
            if (ignoreUpdates) {
                return;
            }
            int rowCount = this.rowCount;
            updateRows(false);
            if (rowCount != this.rowCount) {
                adapter.notifyDataSetChanged();
            } else {
                adapter.notifyItemRangeChanged(0, rowCount);
            }
        } else if (id == NotificationCenter.suggestedFiltersLoaded) {
            //updateRows(true);
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type != 3 && type != 0 && type != 5 && type != 1;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new HintInnerCell(mContext);
                    view.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_top, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 2:
                    FilterCell filterCell = new FilterCell(mContext);
                    filterCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    filterCell.setOnReorderButtonTouchListener((v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            itemTouchHelper.startDrag(listView.getChildViewHolder(filterCell));
                        }
                        return false;
                    });
                    filterCell.setOnOptionsClick(v -> {
                        FilterCell cell = (FilterCell) v.getParent();
                        MessagesController.DialogFilter filter = cell.getCurrentFilter();
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(getParentActivity());
                        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                        paint.setTextSize(AndroidUtilities.dp(20));
                        builder1.setTitle(Emoji.replaceEmoji(filter.name, paint.getFontMetricsInt(), AndroidUtilities.dp(20), false));
                        final CharSequence[] items = new CharSequence[]{
                                LocaleController.getString("FilterEditItem", R.string.FilterEditItem),
                                LocaleController.getString("FilterDeleteItem", R.string.FilterDeleteItem),
                        };
                        final int[] icons = new int[]{
                                R.drawable.msg_edit,
                                R.drawable.msg_delete
                        };
                        builder1.setItems(items, icons, (dialog, which) -> {
                            if (which == 0) {
                                if (filter.locked) {
                                    showDialog(new LimitReachedBottomSheet(FiltersSetupActivity.this, mContext, LimitReachedBottomSheet.TYPE_FOLDERS, currentAccount));
                                } else {
                                    presentFragment(new FilterCreateActivity(filter));
                                }
                            } else if (which == 1) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                builder.setTitle(LocaleController.getString("FilterDelete", R.string.FilterDelete));
                                builder.setMessage(LocaleController.getString("FilterDeleteAlert", R.string.FilterDeleteAlert));
                                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                                builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog2, which2) -> {
                                    AlertDialog progressDialog = null;
                                    if (getParentActivity() != null) {
                                        progressDialog = new AlertDialog(getParentActivity(), 3);
                                        progressDialog.setCanCancel(false);
                                        progressDialog.show();
                                    }
                                    final AlertDialog progressDialogFinal = progressDialog;
                                    TLRPC.TL_messages_updateDialogFilter req = new TLRPC.TL_messages_updateDialogFilter();
                                    req.id = filter.id;
                                    getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                                        try {
                                            if (progressDialogFinal != null) {
                                                progressDialogFinal.dismiss();
                                            }
                                        } catch (Exception e) {
                                            FileLog.e(e);
                                        }
                                        int idx = getMessagesController().dialogFilters.indexOf(filter);
                                        if (idx >= 0) {
                                            idx += filtersStartRow;
                                        }
                                        if (!showAllChats) {
                                            idx--;
                                        }
                                        ignoreUpdates = true;
                                        getMessagesController().removeFilter(filter);
                                        getMessagesStorage().deleteDialogFilter(filter);
                                        ignoreUpdates = false;

                                        int prevAddRow = createFilterRow;
                                        int prevRecommendedHeaderRow = recommendedHeaderRow;
                                        updateRows(true);
//                                        if (idx != -1) {
//                                            if (filtersStartRow == -1) {
//                                                adapter.notifyItemRangeRemoved(idx - 1, 2);
//                                            } else {
//                                                adapter.notifyItemRemoved(idx);
//                                            }
//                                            if (prevRecommendedHeaderRow == -1 && recommendedHeaderRow != -1) {
//                                                adapter.notifyItemRangeInserted(prevRecommendedHeaderRow, recommendedSectionRow - recommendedHeaderRow + 1);
//                                            }
//                                            if (prevAddRow == -1 && createFilterRow != -1) {
//                                                adapter.notifyItemInserted(createFilterRow);
//                                            }
//                                        }
                                    }));
                                });
                                AlertDialog alertDialog = builder.create();
                                showDialog(alertDialog);
                                TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                                if (button != null) {
                                    button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
                                }
                            }
                        });
                        final AlertDialog dialog = builder1.create();
                        showDialog(dialog);
                        dialog.setItemColor(items.length - 1, Theme.getColor(Theme.key_dialogTextRed2), Theme.getColor(Theme.key_dialogRedIcon));
                    });
                    view = filterCell;
                    break;
                case 3:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 4:
                    view = new TextCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 99:
                    view = new TextCell(mContext,true);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    ((TextCell) view).setGravity(Gravity.CENTER);
                    ((TextCell) view).setTextAndIcon(LocaleController.getString("view_filter_add_all",R.string.view_filter_add_all), null, false);
                    view.setOnClickListener(v -> {
                        AlertDialog progressDialog = new AlertDialog(mContext, 3);
                        progressDialog.setCanCancel(false);
                        progressDialog.show();

                        new Thread(() -> {
                            addAllSuggestedFilters();
                            AndroidUtilities.runOnUIThread(() -> {
                                progressDialog.dismiss();
                                orderChanged = true; // 解决一键添加后的云端顺序不对
                                NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.dialogFiltersUpdated);
                            });
                        }).start();
                    });
                    break;
                case 100:
                    view = new TextCell(mContext,true);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    ((TextCell) view).setTextAndIcon(LocaleController.getString("view_filter_delete_all",R.string.view_filter_delete_all), Color.parseColor("#889198"), null, false);
                    ((TextCell) view).setTextDrawableLeft(R.drawable.ic_folder_delete_all);
                    ((TextCell) view).setGravity(Gravity.CENTER);
                    view.setOnClickListener(v -> {
                        AlertDialog progressDialog = new AlertDialog(mContext, 3);
                        progressDialog.setCanCancel(false);
                        progressDialog.show();
                        new Thread(() -> {
                            deleteAllSuggested();
                            AndroidUtilities.runOnUIThread(() -> {
                                progressDialog.dismiss();
                                removeLocalFilter();
                            });
                        }).start();
                    });
                    break;
                case 5:
                default:
                    SuggestedFilterCell suggestedFilterCell = new SuggestedFilterCell(mContext);//推荐分组
                    suggestedFilterCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    suggestedFilterCell.setAddOnClickListener(v -> {
                        TLRPC.TL_dialogFilterSuggested suggested = suggestedFilterCell.getSuggestedFilter();
                        MessagesController.DialogFilter filter = new MessagesController.DialogFilter();
                        filter.name = suggested.filter.title;
                        filter.id = 2;
                        while (getMessagesController().dialogFiltersById.get(filter.id) != null) {
                            filter.id++;
                        }
                        filter.pendingUnreadCount = filter.unreadCount = -1;
                        for (int b = 0; b < 2; b++) {
                            ArrayList<TLRPC.InputPeer> fromArray = b == 0 ? suggested.filter.include_peers : suggested.filter.exclude_peers;
                            ArrayList<Long> toArray = b == 0 ? filter.alwaysShow : filter.neverShow;
                            for (int a = 0, N = fromArray.size(); a < N; a++) {
                                TLRPC.InputPeer peer = fromArray.get(a);
                                long lowerId;
                                if (peer.user_id != 0) {
                                    lowerId = peer.user_id;
                                } else if (peer.chat_id != 0) {
                                    lowerId = -peer.chat_id;
                                } else {
                                    lowerId = -peer.channel_id;
                                }
                                toArray.add(lowerId);
                            }
                        }
                        if (suggested.filter.groups) {
                            filter.flags |= MessagesController.DIALOG_FILTER_FLAG_GROUPS;
                        }
                        if (suggested.filter.bots) {
                            filter.flags |= MessagesController.DIALOG_FILTER_FLAG_BOTS;
                        }
                        if (suggested.filter.contacts) {
                            filter.flags |= MessagesController.DIALOG_FILTER_FLAG_CONTACTS;
                        }
                        if (suggested.filter.non_contacts) {
                            filter.flags |= MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS;
                        }
                        if (suggested.filter.broadcasts) {
                            filter.flags |= MessagesController.DIALOG_FILTER_FLAG_CHANNELS;
                        }
                        if (suggested.filter.exclude_archived) {
                            filter.flags |= MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED;
                        }
                        if (suggested.filter.exclude_read) {
                            filter.flags |= MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ;
                        }
                        if (suggested.filter.exclude_muted) {
                            filter.flags |= MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED;
                        }
                        ignoreUpdates = true;
                        FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, true, false, true, true, false, FiltersSetupActivity.this, () -> {
                            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                            ignoreUpdates = false;
                            int index = suggestedFilters.indexOf(suggested);
                            if (index != -1) {
                                boolean wasEmpty = filtersStartRow == -1;
                                suggestedFilters.remove(index);
                                index += recommendedStartRow;
                                int prevAddRow = createFilterRow;
                                int prevRecommendedHeaderRow = recommendedHeaderRow;
                                int prevRecommendedSectionRow = recommendedSectionRow;
                                updateRows(false);
                                if (prevAddRow != -1 && createFilterRow == -1) {
                                    adapter.notifyItemRemoved(prevAddRow);
                                }
                                if (prevRecommendedHeaderRow != -1 && recommendedHeaderRow == -1) {
                                    adapter.notifyItemRangeRemoved(prevRecommendedHeaderRow, prevRecommendedSectionRow - prevRecommendedHeaderRow + 1);
                                } else {
                                    adapter.notifyItemRemoved(index);
                                }
                                if (wasEmpty) {
                                    adapter.notifyItemInserted(filtersHeaderRow);
                                }
                                adapter.notifyItemInserted(filtersStartRow+getMessagesController().dialogFilters.size()-1);
                            } else {
                                updateRows(true);
                            }
                            // 解决添加未读后的云端顺序不对
                            if (filter.flags == 95) {
                                orderChanged = true;
                            }
                        });
                    });
                    view = suggestedFilterCell;
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == filtersHeaderRow) {
                        headerCell.setText(LocaleController.getString("FiltersNew", R.string.FiltersNew));
                    } else if (position == recommendedHeaderRow) {
                        headerCell.setText(LocaleController.getString("FilterRecommendedNew", R.string.FilterRecommendedNew));
                    }
                    break;
                }
                case 2: {
                    FilterCell filterCell = (FilterCell) holder.itemView;
                    int filterPosition = position - filtersStartRow;
                    if (!showAllChats) {
                        filterPosition++;
                    }
                    filterCell.setFilter(getMessagesController().dialogFilters.get(filterPosition), true);
                    break;
                }
                case 3: {
                    if (position == createSectionRow) {
                        holder.itemView.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 4: {
                    TextCell textCell = (TextCell) holder.itemView;
                    SharedPreferences preferences = MessagesController.getNotificationsSettings(currentAccount);
                    if (position == createFilterRow) {
                        Drawable drawable1 = mContext.getResources().getDrawable(R.drawable.poll_add_circle);
                        Drawable drawable2 = mContext.getResources().getDrawable(R.drawable.poll_add_plus);
                        drawable1.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_switchTrackChecked), PorterDuff.Mode.MULTIPLY));
                        drawable2.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_checkboxCheck), PorterDuff.Mode.MULTIPLY));
                        CombinedDrawable combinedDrawable = new CombinedDrawable(drawable1, drawable2);

                        textCell.setTextAndIcon(LocaleController.getString("CreateNewFilterNew", R.string.CreateNewFilterNew), combinedDrawable, false);
                    }
                    break;
                }
                case 99:
                case 100:
                    break;
                case 5: {
                    SuggestedFilterCell filterCell = (SuggestedFilterCell) holder.itemView;
                    filterCell.setFilter(suggestedFilters.get(position - recommendedStartRow), true);
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == filtersHeaderRow || position == recommendedHeaderRow) {
                return 0;
            } else if (position == filterHelpRow) {
                return 1;
            } else if (position >= filtersStartRow && position < filtersEndRow) {
                return 2;
            } else if (position == createSectionRow || position == recommendedSectionRow) {
                return 3;
            } else if (position == createFilterRow) {
                return 4;
            } else if (position == createRecommendedAllRow) {
                return 99;
            } else if (position == deleteRecommendedAllRow) {
                return 100;
            } else {
                return 5;
            }
        }

        public void swapElements(int fromIndex, int toIndex) {
            int idx1 = fromIndex - filtersStartRow;
            int idx2 = toIndex - filtersStartRow;
            int count = filtersEndRow - filtersStartRow;
            if (!showAllChats) {
                idx1++;
                idx2++;
                count++;
            }

            if (idx1 < 0 || idx2 < 0 || idx1 >= count || idx2 >= count) {
                return;
            }
            ArrayList<MessagesController.DialogFilter> filters = getMessagesController().dialogFilters;
            MessagesController.DialogFilter filter1 = filters.get(idx1);
            MessagesController.DialogFilter filter2 = filters.get(idx2);
            int temp = filter1.order;
            filter1.order = filter2.order;
            filter2.order = temp;
            filters.set(idx1, filter2);
            filters.set(idx2, filter1);
            orderChanged = true;
            notifyItemMoved(fromIndex, toIndex);
        }
    }

    public class TouchHelperCallback extends ItemTouchHelper.Callback {

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            boolean canMove = getUserConfig().isPremium() || !((viewHolder.itemView instanceof FilterCell) && ((FilterCell) viewHolder.itemView).currentFilter.isDefault());
            if (viewHolder.getItemViewType() != 2 || !canMove) {
                return makeMovementFlags(0, 0);
            }
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            boolean canMove = getUserConfig().isPremium() || !((target.itemView instanceof FilterCell) && ((FilterCell) target.itemView).currentFilter.isDefault());
            if (source.getItemViewType() != target.getItemViewType() || !canMove) {
                return false;
            }
            adapter.swapElements(source.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                listView.cancelClickRunnables(false);
                viewHolder.itemView.setPressed(true);
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setPressed(false);
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{HeaderCell.class, TextCell.class, FilterCell.class, SuggestedFilterCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{FilterCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{FilterCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{FilterCell.class}, new String[]{"moveImageView"}, null, null, null, Theme.key_stickers_menu));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{FilterCell.class}, new String[]{"optionsImageView"}, null, null, null, Theme.key_stickers_menu));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_USEBACKGROUNDDRAWABLE | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, new Class[]{FilterCell.class}, new String[]{"optionsImageView"}, null, null, null, Theme.key_stickers_menuSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueText2));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_switchTrackChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_checkboxCheck));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        return themeDescriptions;
    }

    private void handlerFilterSuggested() {
        suggestedFilters.clear();
        String[] descriptions = new String[]{"contacts", "non_contacts", "groups", "channels", "bots", "unread"};
        String[] objects = new String[]{"contacts", "non_contacts", "groups", "channels", "bots", "unread"};
        int[] flags = new int[]{
                MessagesController.DIALOG_FILTER_FLAG_CONTACTS,
                MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS,
                MessagesController.DIALOG_FILTER_FLAG_GROUPS,
                MessagesController.DIALOG_FILTER_FLAG_CHANNELS,
                MessagesController.DIALOG_FILTER_FLAG_BOTS,
                95,
        };
        String[] newFilters = new String[]{
                LocaleController.getString("FilterContacts", R.string.FilterContacts),
                LocaleController.getString("FilterNonContacts", R.string.FilterNonContacts),
                LocaleController.getString("FilterGroups", R.string.FilterGroups),
                LocaleController.getString("FilterChannels", R.string.FilterChannels),
                LocaleController.getString("FilterBots", R.string.FilterBots),
                LocaleController.getString("FilterUnread", R.string.FilterUnread)
        };

        for (int i = 0; i < newFilters.length; i++) {
            String string = newFilters[i];
            boolean has = false;
            for (MessagesController.DialogFilter filter : getMessagesController().dialogFilters) {
                if (filter.flags == flags[i] || TextUtils.equals(filter.name, string)) {
                    has = true;
                    break;
                } else if (filter.flags ==31){ // 兼容之前版本的未读
                    has = true;
                    break;
                }
            }

            if (!has) {
                String object = objects[i];
                GroupCreateSpan span = new GroupCreateSpan(ApplicationLoader.applicationContext, object);
                int filterFlags = 0;
                filterFlags |= flags[i];

                ArrayList<Long> result = new ArrayList<>();
                result.add(span.getUid());

                MessagesController.DialogFilter filter = new MessagesController.DialogFilter();
                filter.id = 2;
                while (MessagesController.getInstance(UserConfig.selectedAccount).dialogFiltersById.get(filter.id) != null) {
                    filter.id++;
                }
                filter.name = "";
                int newFilterFlags = filterFlags;
                String newFilterName = string;
                TLRPC.TL_dialogFilterSuggested filterSuggested = new TLRPC.TL_dialogFilterSuggested();
                filterSuggested.description = descriptions[i];
                filterSuggested.filter = new TLRPC.TL_dialogFilter();
                filterSuggested.filter.contacts = (newFilterFlags & MessagesController.DIALOG_FILTER_FLAG_CONTACTS) != 0;
                filterSuggested.filter.non_contacts = (newFilterFlags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) != 0;
                filterSuggested.filter.groups = (newFilterFlags & MessagesController.DIALOG_FILTER_FLAG_GROUPS) != 0;
                filterSuggested.filter.broadcasts = (newFilterFlags & MessagesController.DIALOG_FILTER_FLAG_CHANNELS) != 0;
                filterSuggested.filter.bots = (newFilterFlags & MessagesController.DIALOG_FILTER_FLAG_BOTS) != 0;
                filterSuggested.filter.exclude_muted = (newFilterFlags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0;
                filterSuggested.filter.exclude_read = (newFilterFlags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ) != 0;
                filterSuggested.filter.exclude_archived = (newFilterFlags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_ARCHIVED) != 0;
                filterSuggested.filter.id = filter.id;
                filterSuggested.filter.flags = flags[i];
                filterSuggested.filter.title = newFilterName;
                suggestedFilters.add(filterSuggested);
            }
        }
    }

    private void addAllSuggestedFilters() {
        int[] flags = new int[]{
                MessagesController.DIALOG_FILTER_FLAG_CONTACTS,
                MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS,
                MessagesController.DIALOG_FILTER_FLAG_GROUPS,
                MessagesController.DIALOG_FILTER_FLAG_CHANNELS,
                MessagesController.DIALOG_FILTER_FLAG_BOTS,
                95,
        };
        String[] newFilters = new String[]{
                LocaleController.getString("FilterContactsNew", R.string.FilterContactsNew),
                LocaleController.getString("FilterNonContactsNew", R.string.FilterNonContactsNew),
                LocaleController.getString("FilterGroupsNew", R.string.FilterGroupsNew),
                LocaleController.getString("FilterChannelsNew", R.string.FilterChannelsNew),
                LocaleController.getString("FilterBotsNew", R.string.FilterBotsNew),
                LocaleController.getString("FilterUnreadNew", R.string.FilterUnreadNew)
        };
        for (int i = 0; i < newFilters.length; i++) {
            String string = newFilters[i];
            boolean has = false;
            for (MessagesController.DialogFilter filter : getMessagesController().dialogFilters) {
                if (filter.flags==flags[i] || TextUtils.equals(filter.name, string)) {
                    has = true;
                    break;
                }else if (filter.flags ==31){ // 兼容之前版本的未读
                    has = true;
                    break;
                }
            }

            if (!has) {
                int filterFlags = 0;
                filterFlags |= flags[i];

                MessagesController.DialogFilter filter = new MessagesController.DialogFilter();
                filter.id = 2;
                while (MessagesController.getInstance(UserConfig.selectedAccount).dialogFiltersById.get(filter.id) != null) {
                    filter.id++;
                }
                filter.name = "";
                int newFilterFlags = filterFlags;
                String newFilterName = string;
                ArrayList<Long> newAlwaysShow = new ArrayList<>(filter.alwaysShow);
                ArrayList<Long> newNeverShow = new ArrayList<>(filter.neverShow);
                LongSparseIntArray newPinned = filter.pinnedDialogs.clone();

                final CountDownLatch countDownLatch = new CountDownLatch(1);
                FilterCreateActivity.saveFilterToServer(filter, newFilterFlags, newFilterName, newAlwaysShow, newNeverShow, newPinned, true, false, false, true, false, FiltersSetupActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        countDownLatch.countDown();
                    }
                });
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void deleteAllSuggested() {
        localFilter.clear();
        int[] flags = new int[]{
                MessagesController.DIALOG_FILTER_FLAG_CONTACTS,
                MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS,
                MessagesController.DIALOG_FILTER_FLAG_GROUPS,
                MessagesController.DIALOG_FILTER_FLAG_CHANNELS,
                MessagesController.DIALOG_FILTER_FLAG_BOTS,
                95,
        };
        String[] newFilters = new String[]{
                LocaleController.getString("FilterContacts", R.string.FilterContacts),
                LocaleController.getString("FilterNonContacts", R.string.FilterNonContacts),
                LocaleController.getString("FilterGroups", R.string.FilterGroups),
                LocaleController.getString("FilterChannels", R.string.FilterChannels),
                LocaleController.getString("FilterBots", R.string.FilterBots),
                LocaleController.getString("FilterUnread", R.string.FilterUnread)
        };
        for (int i = 0; i < newFilters.length; i++) {
            String string = newFilters[i];
            boolean has = false;
            MessagesController.DialogFilter tempFilter = null;
            for (MessagesController.DialogFilter filter : getMessagesController().dialogFilters) {
                if (filter.flags==flags[i] || TextUtils.equals(filter.name, string)) {
                    has = true;
                    tempFilter = filter;
                    break;
                }else if (filter.flags ==31){ // 兼容之前版本的未读
                    tempFilter = filter;
                    has = true;
                    break;
                }
            }
            if (has) {
                localFilter.add(tempFilter);
                TLRPC.TL_messages_updateDialogFilter req = new TLRPC.TL_messages_updateDialogFilter();
                req.id = tempFilter.id;
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                getConnectionsManager().sendRequest(req, (response, error) -> {
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void removeLocalFilter() {
        for (MessagesController.DialogFilter filter : localFilter) {
            getMessagesController().removeFilter(filter);
            getMessagesStorage().deleteDialogFilter(filter);
        }
    }
}