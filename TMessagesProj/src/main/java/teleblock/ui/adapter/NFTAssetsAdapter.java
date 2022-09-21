package teleblock.ui.adapter;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.module.LoadMoreModule;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.dylanc.viewbinding.brvah.BaseViewHolderUtilKt;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.databinding.ItemNftAssetBinding;

import teleblock.model.wallet.NFTInfo;
import timber.log.Timber;

public class NFTAssetsAdapter extends BaseQuickAdapter<NFTInfo, BaseViewHolder> implements LoadMoreModule {

    public NFTAssetsAdapter() {
        super(R.layout.item_nft_asset);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, NFTInfo item) {
        ItemNftAssetBinding binding = BaseViewHolderUtilKt.getBinding(baseViewHolder, ItemNftAssetBinding::bind);
        Timber.i("url-->" + item.thumb_url + "   " + item.original_url);
        Glide.with(getContext())
                .load(item.thumb_url)
                .transform(new CenterCrop(), new RoundedCorners(AndroidUtilities.dp(5)))
                .into(binding.ivNftAvatar);
        binding.tvAssetName.setText(item.asset_name);
        binding.tvNftName.setText(item.nft_name);
        if (TextUtils.isEmpty(item.price)){
            binding.tvNftPrice.setVisibility(View.INVISIBLE);
        }else {
            binding.tvNftPrice.setVisibility(View.VISIBLE);
            binding.tvNftPrice.setText(item.getEthPrice());
        }
    }
}
