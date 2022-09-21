package teleblock.ui.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.telegram.messenger.R;
import org.telegram.messenger.databinding.ViewBottomTabsBinding;

/**
 * 创建日期：2022/4/19
 * 描述：
 */
public class BottomTabsView extends LinearLayout {

    private ViewBottomTabsBinding binding;
    public TabsViewDelegate delegate;

    public BottomTabsView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        binding = ViewBottomTabsBinding.inflate(LayoutInflater.from(getContext()), this, true);
        LayoutInflater.from(getContext()).inflate(R.layout.view_bottom_tabs, this);

        for (int i = 0; i < binding.llTabs.getChildCount(); i++) {
            int finalI = i;
            RelativeLayout relativeLayout = (RelativeLayout) binding.llTabs.getChildAt(i);
            relativeLayout.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    for (int i = 0; i < binding.llTabs.getChildCount(); i++) {
                        RelativeLayout relativeLayout = (RelativeLayout) binding.llTabs.getChildAt(i);
                        relativeLayout.getChildAt(0).setSelected(false);
                        relativeLayout.getChildAt(1).setVisibility(INVISIBLE);
                    }
                    relativeLayout.getChildAt(0).setSelected(true);
                    relativeLayout.getChildAt(1).setVisibility(VISIBLE);
                    if (delegate != null) {
                        delegate.onTabClick(finalI);
                    }
                }
            });
        }

        RelativeLayout relativeLayout = (RelativeLayout) binding.llTabs.getChildAt(0);
        relativeLayout.getChildAt(0).setSelected(true);
        relativeLayout.getChildAt(1).setVisibility(VISIBLE);
    }

    public void setDelegate(TabsViewDelegate tabsViewDelegate) {
        delegate = tabsViewDelegate;
    }

    public interface TabsViewDelegate {

        void onTabClick(int index);
    }
}