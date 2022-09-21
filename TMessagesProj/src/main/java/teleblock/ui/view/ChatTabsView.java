package teleblock.ui.view;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * 聊天顶部菜单栏
 */
public class ChatTabsView extends FrameLayout {

    private Context context;
    private ChatActivity chatActivity;
    private ArrayList<Tab> tabs = new ArrayList<>();
    private RecyclerListView listView;
    private ListAdapter adapter;
    private TabsViewDelegate delegate;

    public ChatTabsView(ChatActivity chatActivity) {
        super(chatActivity.getParentActivity());
        this.chatActivity = chatActivity;
        this.context = chatActivity.getParentActivity();
        initTabs();
        initView();
        updateTabs();
    }

    public void initTabs() {
        tabs.clear();
        if (chatActivity.getCurrentUser() == null || !chatActivity.getCurrentUser().self) {
            tabs.add(new Tab(0, R.drawable.chat_tab_mute_off));
        }
        tabs.add(new Tab(1, R.drawable.chat_tab_watch_video));
        tabs.add(new Tab(2, R.drawable.chat_tab_media));
        if (chatActivity.searchItem != null) {
            tabs.add(new Tab(3, R.drawable.msg_search));
        }
//        tabs.add(new Tab(4, R.drawable.chat_tab_message_recycle));
        if (ChatObject.isChannel(chatActivity.getCurrentChat()) && !chatActivity.getCurrentChat().creator) {
            if (!ChatObject.isNotInChat(chatActivity.getCurrentChat())) {
                tabs.add(new Tab(5, R.drawable.msg_leave));
            }
        } else if (!ChatObject.isChannel(chatActivity.getCurrentChat())) {
            if (chatActivity.getCurrentChat() != null) {
                tabs.add(new Tab(5, R.drawable.msg_leave));
            } else {
                tabs.add(new Tab(5, R.drawable.msg_delete));
            }
        }
    }

    private void initView() {
        listView = new RecyclerListView(context);
        listView.setSelectorDrawableColor(Theme.getColor(Theme.key_listSelector));
        listView.setLayoutManager(new GridLayoutManager(context, tabs.size()));
        adapter = new ListAdapter(context);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (delegate != null) {
                    delegate.onItemClick(tabs.get(position));
                }
            }
        });
        addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        addView(divider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 0.5f, Gravity.BOTTOM));

        setBackgroundColor(Theme.getColor(Theme.key_chat_topPanelBackground));
    }

    public void updateTabs() {
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            if (tab.id == 0) {
                tab.icon = chatActivity.getMessagesController().isDialogMuted(chatActivity.getDialogId()) ?
                        R.drawable.chat_tab_mute_on : R.drawable.chat_tab_mute_off;
            }
        }
        adapter.notifyDataSetChanged();
    }

    public void setDelegate(TabsViewDelegate tabsViewDelegate) {
        delegate = tabsViewDelegate;
    }

    public interface TabsViewDelegate {

        void onItemClick(Tab tab);
    }

    public class Tab {

        public int id;
        public int icon;

        public Tab(int id, int icon) {
            this.icon = icon;
            this.id = id;
        }
    }

    public class TabView extends FrameLayout {

        ImageView imageView;

        public TabView(Context context) {
            super(context);
            imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            addView(imageView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        }

        public void setTab(Tab tab) {
            imageView.setImageResource(tab.icon);
            imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_topPanelClose), PorterDuff.Mode.MULTIPLY));
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return tabs.size();
        }

        @Override
        public long getItemId(int position) {
            return tabs.get(position).id;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = new TabView(mContext);
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TabView tabView = (TabView) holder.itemView;
            tabView.setTab(tabs.get(position));
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }
    }
}