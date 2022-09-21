package teleblock.ui.activity;

import android.content.Context;
import android.graphics.Color;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.databinding.ActivityThemeBinding;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ThemeActivity;
import org.telegram.ui.ThemePreviewActivity;

import java.io.File;
import java.util.Arrays;

import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.event.data.ThemePreviewEvent;
import teleblock.ui.adapter.ThemePageAdapter;
import teleblock.util.EventUtil;
import teleblock.util.ViewUtil;


/**
 * Created by LSD on 2021/9/8.
 * Desc
 */
public class TGThemeActivity extends BaseFragment {

    private ActivityThemeBinding binding;
    private ThemePageAdapter themePageAdapter;
    private Context context;

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onFragmentCreate() {
        EventBus.getDefault().register(this);
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        this.context = context;
        actionBar.setItemsColor(0, false);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("ac_title_theme", R.string.ac_title_theme));
        actionBar.setBackgroundColor(Color.parseColor("#232C3D"));
        actionBar.setTitleColor(Color.parseColor("#ffffff"));
        actionBar.setItemsBackgroundColor(Color.parseColor("#00000000"), false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        if (binding == null) {
            binding = ActivityThemeBinding.inflate(LayoutInflater.from(context));
            initView();
        }
        return fragmentView = binding.getRoot();
    }

    private void initView() {
        binding.ivThemeEntry.setOnClickListener(view -> {
            presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC));
        });

        String[] tabs = LocaleController.getString("array_theme_tables", R.string.array_theme_tables).split("\\|");
        ViewUtil.vbBindMitabHotStyle(context, binding.magicIndicator, tabs, binding.viewPager2, null);
        binding.viewPager2.setAdapter(themePageAdapter = new ThemePageAdapter((FragmentActivity) context, Arrays.asList(tabs)));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemePreviewEvent(MessageEvent event) {
        if (event.getType().equals(EventBusTags.PRE_THEME_VIEW)) {
            if (event.getData() instanceof ThemePreviewEvent) {
                ThemePreviewEvent eventData = (ThemePreviewEvent) event.getData();
                Theme.ThemeInfo themeInfo = Theme.applyThemeFile(new File(eventData.path), eventData.name, null, true);
                if (themeInfo != null) {
                    presentFragment(new ThemePreviewActivity(themeInfo));
                }
            }
        }
    }
}