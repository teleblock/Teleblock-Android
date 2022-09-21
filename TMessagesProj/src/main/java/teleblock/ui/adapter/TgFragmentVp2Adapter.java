package teleblock.ui.adapter;


import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.dylanc.viewbinding.brvah.BaseViewHolderUtilKt;

import org.telegram.messenger.R;
import org.telegram.messenger.databinding.AdapterFragmetPage2Binding;

import java.util.List;

/**
 * Time:2022/7/12
 * Author:Perry
 * Description：TG Fragment专用适配器
 */
public class TgFragmentVp2Adapter extends BaseQuickAdapter<View, BaseViewHolder> {

    public TgFragmentVp2Adapter(@Nullable List<View> data) {
        super(R.layout.adapter_fragmet_page2, data);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, View view) {
        AdapterFragmetPage2Binding binding = BaseViewHolderUtilKt.getBinding(viewHolder, AdapterFragmetPage2Binding::bind);
        binding.flView.addView(view);
    }
}
