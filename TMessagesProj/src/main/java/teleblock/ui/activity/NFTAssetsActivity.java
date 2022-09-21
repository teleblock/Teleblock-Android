package teleblock.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.listener.OnLoadMoreListener;
import com.hjq.http.EasyHttp;
import com.hjq.http.lifecycle.ApplicationLifecycle;
import com.hjq.http.listener.OnDownloadListener;
import com.hjq.http.listener.OnHttpListener;
import com.hjq.http.model.HttpMethod;
import com.luck.picture.lib.config.Crop;
import com.luck.picture.lib.utils.DateUtils;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.yalantis.ucrop.UCrop;

import org.greenrobot.eventbus.EventBus;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.databinding.ActivityNftAssetsBinding;
import org.telegram.messenger.databinding.ViewNftEmptyBinding;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

import java.io.File;

import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.model.wallet.NFTAssets;
import teleblock.model.wallet.NFTInfo;
import teleblock.model.wallet.NFTInfoList;
import teleblock.network.api.NftInfoApi;
import teleblock.network.api.opensea.RetrieveAssentsApi;
import teleblock.ui.adapter.NFTAssetsAdapter;
import teleblock.util.MMKVUtil;
import teleblock.widget.divider.CustomItemDecoration;
import timber.log.Timber;

public class NFTAssetsActivity extends BaseFragment implements OnItemClickListener, OnRefreshListener, OnLoadMoreListener {

    private ActivityNftAssetsBinding binding;
    private NFTAssetsAdapter nftListAdapter;
    private long currentTimeMillis;
    private String owner;
    private String order_direction = "desc";
    private String limit = "10";
    private String cursor;
    private NFTInfo nftInfo;

    public NFTAssetsActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        if (getArguments() != null) {
            currentTimeMillis = arguments.getLong("currentTimeMillis");
        }
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("nft_assets_title", R.string.nft_assets_title));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        binding = ActivityNftAssetsBinding.inflate(LayoutInflater.from(context));
        initView();
        initData();
        return fragmentView = binding.getRoot();
    }

    private void initView() {
        binding.refreshLayout.setOnRefreshListener(this);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(getParentActivity(), 2));
        binding.recyclerView.addItemDecoration(new CustomItemDecoration(2, 20f, 20f, true));
        nftListAdapter = new NFTAssetsAdapter();
        nftListAdapter.setEmptyView(createEmptyView());
        nftListAdapter.getEmptyLayout().setVisibility(View.GONE);
        nftListAdapter.getLoadMoreModule().setOnLoadMoreListener(this);
        nftListAdapter.setOnItemClickListener(this);
        binding.recyclerView.setAdapter(nftListAdapter);
    }

    private View createEmptyView() {
        ViewNftEmptyBinding binding = ViewNftEmptyBinding.inflate(LayoutInflater.from(getParentActivity()));
        binding.tvEmptyNft.setText(LocaleController.getString("nft_assets_empty_text", R.string.nft_assets_empty_text));
        return binding.getRoot();
    }

    private void initData() {
        owner = MMKVUtil.connectedWalletAddress();
//        owner = "0xe89552758DEcfa70f60611413a848055842289fD";
        binding.refreshLayout.autoRefresh();
    }


    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        cursor = null;
        loadData();
    }

    @Override
    public void onLoadMore() {
        loadData();
    }

    private void loadData() {
        EasyHttp.get(new ApplicationLifecycle())
                .api(new RetrieveAssentsApi()
                        .setOwner(owner)
                        .setOrder_direction(order_direction)
                        .setLimit(limit)
                        .setCursor(cursor)
                        .setInclude_orders(true))
                .request(new OnHttpListener<NFTAssets>() {
                    @Override
                    public void onSucceed(NFTAssets result) {
                        NFTInfoList nftInfoList = NFTAssets.parse(result);
                        if (TextUtils.isEmpty(cursor)) {
                            nftListAdapter.setList(nftInfoList.assets);
                        } else {
                            nftListAdapter.addData(nftInfoList.assets);
                        }
                        if (TextUtils.isEmpty(nftInfoList.next)) {
                            nftListAdapter.getLoadMoreModule().loadMoreEnd();
                        } else {
                            nftListAdapter.getLoadMoreModule().loadMoreComplete();
                            cursor = nftInfoList.next;
                        }
                        binding.refreshLayout.finishRefresh();
                        if (nftListAdapter.getData().isEmpty()) {
                            nftListAdapter.getEmptyLayout().setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        nftListAdapter.getLoadMoreModule().loadMoreFail();
                        binding.refreshLayout.finishRefresh();
                        nftListAdapter.getEmptyLayout().setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
        NFTInfo nftInfo = nftListAdapter.getItem(position);
        String path = PathUtils.getExternalAppFilesPath() + "/nft_images" + nftInfo.original_url.substring(nftInfo.original_url.lastIndexOf("/"));
        boolean svg = FileUtils.getFileExtension(path).equals("svg");
        if (svg) path = path.substring(0, path.length() - 4) + ".jpg";
        if (!FileUtils.isFileExists(path)) {
            AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
            progressDialog.setCanCancel(false);
            FileUtils.createOrExistsFile(path);
            if (svg) { // svg图片重新生成
                handleSvgPhoto(nftInfo, progressDialog, path);
            } else {
                downloadPhoto(nftInfo, progressDialog, path);
            }
        } else {
            openPhotoForSelect(nftInfo, new File(path));
        }
    }

    private void downloadPhoto(NFTInfo nftInfo, AlertDialog progressDialog, String path) {
        EasyHttp.download(new ApplicationLifecycle())
                .method(HttpMethod.GET)
                .file(new File(path))
                .url(nftInfo.original_url)
                .listener(new OnDownloadListener() {
                    @Override
                    public void onStart(File file) {
                        progressDialog.show();
                    }

                    @Override
                    public void onProgress(File file, int progress) {
                        Timber.i("onProgress-->" + progress);
                    }

                    @Override
                    public void onComplete(File file) {
                        openPhotoForSelect(nftInfo, file);
                    }

                    @Override
                    public void onError(File file, Exception e) {
                        Timber.e("onError-->" + e);
                        FileUtils.delete(file);
                    }

                    @Override
                    public void onEnd(File file) {
                        progressDialog.dismiss();
                    }
                }).start();
    }

    private void handleSvgPhoto(NFTInfo nftInfo, AlertDialog progressDialog, String path) {
        progressDialog.show();
        Glide.with(getParentActivity()).load(nftInfo.original_url).addListener(new RequestListener<Drawable>() {

            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                progressDialog.dismiss();
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                ThreadUtils.executeByIo(new ThreadUtils.SimpleTask<String>() {
                    @Override
                    public String doInBackground() throws Throwable {
                        Bitmap bitmap = ImageUtils.view2Bitmap(binding.ivNftAvatar);
                        FileIOUtils.writeFileFromBytesByStream(path, ConvertUtils.bitmap2Bytes(bitmap));
                        return null;
                    }

                    @Override
                    public void onSuccess(String result) {
                        progressDialog.dismiss();
                        openPhotoForSelect(nftInfo, new File(path));
                    }
                });
                return false;
            }
        }).into(binding.ivNftAvatar);

    }

    private void openPhotoForSelect(NFTInfo nftInfo, File file) {
        this.nftInfo = nftInfo;
        Uri inputUri = Uri.fromFile(file);
        String fileName = DateUtils.getCreateFileName("CROP_") + ".jpg";
        Uri destinationUri = Uri.fromFile(new File(PathUtils.getExternalAppCachePath(), fileName));
        UCrop uCrop = UCrop.of(inputUri, destinationUri);
        UCrop.Options options = new UCrop.Options();
        options.setHideBottomControls(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);
        options.setCircleDimmedLayer(true);
        options.withAspectRatio(1, 1);
        options.isDarkStatusBarBlack(true);
        uCrop.withOptions(options);
        uCrop.start(getParentActivity());
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == UCrop.REQUEST_CROP) {
                Uri output = Crop.getOutput(data);
                if (output == null) return;
                NftInfoApi nftInfoApi = new NftInfoApi()
                        .setNft_path(output.getPath())
                        .setNft_contract(nftInfo.contract_address)
                        .setNft_contract_image(nftInfo.original_url)
                        .setNft_token_id(nftInfo.token_id)
                        .setNft_name(nftInfo.nft_name);
                EventBus.getDefault().post(new MessageEvent(EventBusTags.UPLOAD_USER_PROFILE, currentTimeMillis + "", nftInfoApi));
                finishFragment();
            }
        }
    }
}
