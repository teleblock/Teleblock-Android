package teleblock.util;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;

import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.blankj.utilcode.util.SizeUtils;

import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.CommonPagerTitleView;

import org.telegram.messenger.R;
import org.telegram.messenger.databinding.SimplePagerTitleThemeBinding;

import java.util.List;

/**
 * Time:2022/7/6
 * Author:Perry
 * Description：视图工具类，一些快捷调用
 */
public class ViewUtil {

    public interface VPOnPageChangeCallback {
        void onPageSelected(int position);
    }

    /**
     * vb绑定mitab
     * @param context
     * @param magicIndicator
     * @param tabs
     * @param viewPager2
     * @param mVPOnPageChangeCallback
     */
    public static void vbBindMitab(Context context, MagicIndicator magicIndicator, List<String> tabs, ViewPager2 viewPager2, VPOnPageChangeCallback mVPOnPageChangeCallback) {
        magicIndicator.setNavigator(mibSetNavigat(context, normalTextMibAdapter(tabs, viewPager2)));
        vbBindMiTabListener(magicIndicator, viewPager2, mVPOnPageChangeCallback);
    }

    /**
     * mib设置导航
     * @param context
     * @param adapter
     * @return
     */
    public static CommonNavigator mibSetNavigat(Context context, CommonNavigatorAdapter adapter) {
        CommonNavigator commonNavigator = new CommonNavigator(context);
        commonNavigator.setAdapter(adapter);
        commonNavigator.setAdjustMode(true);
        return commonNavigator;
    }

    /**
     * 普通文字样式的adapter
     * @param tabs
     * @param viewPager2
     * @return
     */
    public static CommonNavigatorAdapter normalTextMibAdapter(List<String> tabs, ViewPager2 viewPager2) {
        return new CommonNavigatorAdapter() {
            @Override
            public int getCount() {
                return tabs.size();
            }

            @Override
            public IPagerTitleView getTitleView(Context context, final int index) {
                ColorTransitionPagerTitleView titleView = new ColorTransitionPagerTitleView(context);
                titleView.setNormalColor(Color.parseColor("#56565C"));
                titleView.setSelectedColor(ContextCompat.getColor(context, R.color.theme_color));
                titleView.setPadding(SizeUtils.dp2px(12f), 0, SizeUtils.dp2px(12), 0);
                titleView.setText(tabs.get(index));
                titleView.setTextSize(16);
                titleView.setOnClickListener(v -> viewPager2.setCurrentItem(index, false));
                return titleView;
            }

            @Override
            public IPagerIndicator getIndicator(Context context) {
                LinePagerIndicator linePagerIndicator = new LinePagerIndicator(context);
                linePagerIndicator.setMode(LinePagerIndicator.MODE_WRAP_CONTENT);
                linePagerIndicator.setColors(ContextCompat.getColor(context, R.color.theme_color));
                linePagerIndicator.setRoundRadius(5);
                return linePagerIndicator;
            }
        };
    }

    /**
     * vb绑定mitab hot图片样式
     * @param context
     * @param magicIndicator
     * @param tabs
     * @param viewPager2
     * @param mVPOnPageChangeCallback
     */
    public static void vbBindMitabHotStyle(
            Context context,
            MagicIndicator magicIndicator,
            String[] tabs,
            ViewPager2 viewPager2,
            VPOnPageChangeCallback mVPOnPageChangeCallback
    ) {
        CommonNavigator commonNavigator = new CommonNavigator(context);
        commonNavigator.setAdjustMode(true);//水平等分
        commonNavigator.setAdapter(new CommonNavigatorAdapter() {
            @Override
            public int getCount() {
                return tabs.length;
            }

            @Override
            public IPagerTitleView getTitleView(Context context, final int index) {
                CommonPagerTitleView commonPagerTitleView = new CommonPagerTitleView(context);
                SimplePagerTitleThemeBinding binding = SimplePagerTitleThemeBinding.inflate(LayoutInflater.from(context));

                if (index == 0) {
                    binding.ivTabImage.setImageResource(R.drawable.ic_tab_hot);
                } else if (index == 1) {
                    binding.ivTabImage.setImageResource(R.drawable.ic_tab_new);
                }
                commonPagerTitleView.setContentView(binding.getRoot());

                commonPagerTitleView.setOnPagerTitleChangeListener(new CommonPagerTitleView.OnPagerTitleChangeListener() {
                    @Override
                    public void onSelected(int index, int totalCount) {
                        DrawableColorChange drawableColorChange = new DrawableColorChange(context);
                        if (index == 0) {
                            binding.ivTabImage.setImageDrawable(drawableColorChange.changeColorById(R.drawable.ic_tab_hot, R.color.tg_main_color));
                        } else {
//                            if (!newEvent) {
//                                newEvent = true;
//                                EventUtil.track(context, EventUtil.Even.主题页面最新展示, new ArrayMap<>());
//                            }
                            binding.ivTabImage.setImageDrawable(drawableColorChange.changeColorById(R.drawable.ic_tab_new, R.color.tg_main_color));
                        }
                    }

                    @Override
                    public void onDeselected(int index, int totalCount) {
                        DrawableColorChange drawableColorChange = new DrawableColorChange(context);
                        if (index == 0) {
                            binding.ivTabImage.setImageDrawable(drawableColorChange.changeColorById(R.drawable.ic_tab_hot, R.color.half_black));
                        } else {
                            binding.ivTabImage.setImageDrawable(drawableColorChange.changeColorById(R.drawable.ic_tab_new, R.color.half_black));
                        }
                    }

                    @Override
                    public void onLeave(int index, int totalCount, float leavePercent, boolean leftToRight) {
                    }

                    @Override
                    public void onEnter(int index, int totalCount, float enterPercent, boolean leftToRight) {
                    }
                });
                commonPagerTitleView.setOnClickListener(v -> viewPager2.setCurrentItem(index));
                return commonPagerTitleView;
            }

            @Override
            public IPagerIndicator getIndicator(Context context) {
                LinePagerIndicator linePagerIndicator = new LinePagerIndicator(context);
                linePagerIndicator.setMode(LinePagerIndicator.MODE_EXACTLY);
                linePagerIndicator.setLineHeight(8);
                linePagerIndicator.setLineWidth(SizeUtils.dp2px(37f));
                linePagerIndicator.setRoundRadius(4);
                linePagerIndicator.setColors(context.getResources().getColor(R.color.tg_main_color));
                return linePagerIndicator;
            }
        });

        magicIndicator.setNavigator(commonNavigator);
        vbBindMiTabListener(magicIndicator, viewPager2, mVPOnPageChangeCallback);
    }

    public static void vbBindMiTabListener(MagicIndicator magicIndicator, ViewPager2 viewPager2, VPOnPageChangeCallback mVPOnPageChangeCallback) {
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                magicIndicator.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                magicIndicator.onPageSelected(position);
                if (mVPOnPageChangeCallback != null) {
                    mVPOnPageChangeCallback.onPageSelected(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                magicIndicator.onPageScrollStateChanged(state);
            }
        });
    }
}