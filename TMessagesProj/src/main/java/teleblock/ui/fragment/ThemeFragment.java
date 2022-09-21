package teleblock.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;

import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ResourceUtils;
import com.hjq.http.EasyHttp;
import com.hjq.http.lifecycle.ActivityLifecycle;
import com.hjq.http.listener.OnHttpListener;

import org.greenrobot.eventbus.EventBus;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.databinding.FragmentThemeBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.event.data.ThemePreviewEvent;
import teleblock.model.MiddleData;
import teleblock.model.ThemeEntity;
import teleblock.network.BaseBean;
import teleblock.network.api.ThemeListApi;
import teleblock.ui.adapter.ThemeRvAdapter;
import teleblock.util.SpacesItemDecoration;

/**
 * Created by LSD on 2021/3/20.
 * Desc
 */
public class ThemeFragment extends BaseFragment {

    private FragmentThemeBinding binding;
    private ThemeRvAdapter themeRvAdapter;
    private int page = 1;
    private int type;

    private String names[] = new String[]{"Classic", "iOS", "Whatsapp"};
    private int ids[] = new int[]{R.drawable.ic_theme_classic, R.drawable.ic_theme_ios, R.drawable.ic_theme_whatsapp};
    private String urls[] = new String[]{"theme/classic.attheme", "theme/ios.attheme", "theme/whatsapp.attheme"};

    public static ThemeFragment instance(int type) {
        ThemeFragment fragment = new ThemeFragment();
        Bundle args = new Bundle();
        args.putInt("type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected View getFrameLayout(LayoutInflater inflater) {
        binding = FragmentThemeBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    protected void onViewCreated() {
        Bundle bundle = getArguments();
        if (bundle != null || bundle.containsKey("type")) {
            type = bundle.getInt("type");
        }

        initView();
        binding.refreshLayout.autoRefresh();
    }

    private void initView() {
        binding.refreshLayout.setOnRefreshListener(refreshLayout -> {
            page = 1;
            loadData();
        });

        binding.themeRv.addItemDecoration(new SpacesItemDecoration(2, 12, true));
        binding.themeRv.setLayoutManager(new GridLayoutManager(mActivity, 2));
        binding.themeRv.setAdapter(themeRvAdapter = new ThemeRvAdapter());

        themeRvAdapter.getLoadMoreModule().setPreLoadNumber(4);
//        themeRvAdapter.getLoadMoreModule().setLoadMoreView(new NSLoadMoreView());
        themeRvAdapter.getLoadMoreModule().setOnLoadMoreListener(() -> {
            page++;
            loadData();
        });

        themeRvAdapter.setOnItemClickListener((adapter, view, position) -> {
            ThemeEntity.ItemEntity entity = themeRvAdapter.getItem(position);
            MiddleData.getInstance().themeInfo = entity;
            if (entity.getType() == 0) {
                Browser.openUrl(mActivity, entity.url, true);
            } else {
                String themeFilePath = PathUtils.getExternalAppFilesPath() + "/" + entity.url;
                File file = new File(themeFilePath);
                if (!file.exists()) {
                    ResourceUtils.copyFileFromAssets(entity.url, themeFilePath);
                }
                EventBus.getDefault().post(new MessageEvent(EventBusTags.PRE_THEME_VIEW, new ThemePreviewEvent(themeFilePath, entity.title)));
            }
        });
    }

    private List<ThemeEntity.ItemEntity> handleThemeList(List<ThemeEntity.ItemEntity> list) {
        List<ThemeEntity.ItemEntity> newList = new ArrayList<>();
        List<ThemeEntity.ItemEntity> normalList = new ArrayList<>();
        if (list != null && list.size() > 0) {
            for (ThemeEntity.ItemEntity itemEntity : list) {
                normalList.add(itemEntity);
            }
            newList.addAll(localTheme());
            newList.addAll(normalList);
        } else {
            newList.addAll(localTheme());
        }
        return newList;
    }

    private List<ThemeEntity.ItemEntity> localTheme() {
        List<ThemeEntity.ItemEntity> local = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            ThemeEntity.ItemEntity itemEntity = new ThemeEntity.ItemEntity();
            itemEntity.id = i - 3;
            itemEntity.type = 1;
            itemEntity.avatarId = ids[i];
            itemEntity.used = (int) (Math.random() * 600) + 200;
            itemEntity.title = names[i];
            itemEntity.url = urls[i];
            local.add(itemEntity);
        }
        return local;
    }

    private void loadData() {
        EasyHttp.post(new ActivityLifecycle(mActivity))
                .api(new ThemeListApi(type).setPage(page))
                .request(new OnHttpListener<BaseBean<ThemeEntity>>() {
                    @Override
                    public void onEnd(Call call) {
                        binding.refreshLayout.finishRefresh();
                    }

                    @Override
                    public void onSucceed(BaseBean<ThemeEntity> result) {
                        themeRvAdapter.getLoadMoreModule().loadMoreComplete();
                        if (page >= result.getData().total) {
                            themeRvAdapter.getLoadMoreModule().loadMoreEnd(true);
                        }
                        List<ThemeEntity.ItemEntity> list = result.getData().item;
                        if (page == 1) {
                            if (type == 0) {//hot
                                List<ThemeEntity.ItemEntity> nList = handleThemeList(list);
                                themeRvAdapter.setList(nList);
                            } else {
                                themeRvAdapter.setList(result.getData().item);
                            }
                        } else {
                            themeRvAdapter.addData(result.getData().item);
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        themeRvAdapter.getLoadMoreModule().loadMoreEnd(true);
                        if (page == 1 && type == 0) {
                            List<ThemeEntity.ItemEntity> local = handleThemeList(null);
                            themeRvAdapter.setList(local);
                        }
                    }
                });
    }
}
