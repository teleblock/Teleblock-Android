package teleblock.ui.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemChildClickListener;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.listener.OnLoadMoreListener;
import com.hjq.http.EasyHttp;
import com.hjq.http.lifecycle.ApplicationLifecycle;
import com.hjq.http.listener.OnDownloadListener;
import com.hjq.http.listener.OnHttpListener;
import com.hjq.http.model.HttpMethod;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.messenger.databinding.ActWalletHomeBinding;
import org.telegram.messenger.databinding.ViewWalletHomeBinding;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.PhotoViewer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.model.BaseLoadmoreModel;
import teleblock.model.HotRecommendData;
import teleblock.model.wallet.NFTAssets;
import teleblock.model.wallet.NFTInfo;
import teleblock.model.wallet.NFTInfoList;
import teleblock.model.wallet.WalletInfo;
import teleblock.network.BaseBean;
import teleblock.network.api.HotRecommendApi;
import teleblock.network.api.NftInfoApi;
import teleblock.network.api.WalletInfoApi;
import teleblock.network.api.opensea.RetrieveAnAssentApi;
import teleblock.network.api.opensea.RetrieveAssentsApi;
import teleblock.ui.adapter.NFTAssetsAdapter;
import teleblock.ui.adapter.WalletHotRecommendAdapter;
import teleblock.ui.dialog.CommonTipsDialog;
import teleblock.util.AdapterUtil;
import teleblock.util.StringUtil;
import teleblock.widget.ViewPageAdapter;
import teleblock.widget.divider.CustomItemDecoration;
import timber.log.Timber;

/**
 * 钱包主页
 */
public class WalletHomeAct extends BaseFragment implements OnItemChildClickListener, View.OnClickListener {

    private ActWalletHomeBinding binding;
    private long currentTimeMillis;
    private boolean userSelf;
    private String address;
    private WalletHotRecommendAdapter walletHotRecommendAdapter;
    private TLRPC.User user;

    public WalletHomeAct(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        EventBus.getDefault().register(this);
        if (getArguments() != null) {
            currentTimeMillis = arguments.getLong("currentTimeMillis");
            address = getArguments().getString("address");
            userSelf = getArguments().getBoolean("userSelf");
        }
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean isLightStatusBar() {
        return false;
    }

    @Override
    public View createView(Context context) {
        removeActionbarViews();
        binding = ActWalletHomeBinding.inflate(LayoutInflater.from(context));
        fragmentView = binding.getRoot();
        initView();
        initData();
        return fragmentView;
    }

    private void initView() {
        binding.ivWalletClose.setOnClickListener(this);
        binding.llWalletAddress.setOnClickListener(this);
        binding.tvSearchWallet.setOnClickListener(this);
        if (userSelf) { // 查看自己
            binding.llMyselfContent.setVisibility(View.VISIBLE);
            binding.llOthersContent.setVisibility(View.GONE);
            binding.rvHotRecommend.setLayoutManager(new LinearLayoutManager(getParentActivity(), RecyclerView.HORIZONTAL, false));
            walletHotRecommendAdapter = new WalletHotRecommendAdapter();
            walletHotRecommendAdapter.addChildClickViewIds(R.id.tv_add_channel, R.id.tv_showall_channel);
            walletHotRecommendAdapter.setOnItemChildClickListener(this);
            binding.rvHotRecommend.setAdapter(walletHotRecommendAdapter);
        } else { // 查看别人
            binding.llMyselfContent.setVisibility(View.GONE);
            binding.llOthersContent.setVisibility(View.VISIBLE);
        }
    }

    private void initData() {
        binding.tvWalletAddress.setText(StringUtil.formatAddress(address));
        getWalletInfo();
        if (userSelf) { // 查看自己
            user = getUserConfig().getCurrentUser();
            binding.tvTgName.setText("@" + user.username);
            getHotRecommendData();
        } else { // 查看别人

        }
        initTabLayout();
    }

    private void initTabLayout() {
        String[] titles = {"代币", "NFTs"};
        List<View> views = new ArrayList<>();
        for (String s : titles) {
            views.add(new WalletHomeView(getParentActivity(), s));
        }
        binding.tabLayout.setTitle(titles);
        binding.viewPager.setAdapter(new ViewPageAdapter(views));
        binding.viewPager.setOffscreenPageLimit(views.size());
        binding.tabLayout.setViewPager(binding.viewPager);
    }

    /**
     * 錢包主頁搜尋錢包時會3種：
     * 1.是TG用戶有設置過NFT頭像：有NFT頭像 錢包頁
     * 2.是TG用戶沒設置過NFT頭像：TG預設頭像 錢包頁
     * 3.不是TG用戶：NFT預設頭像 錢包頁
     */
    private void getWalletInfo() {
        EasyHttp.post(new ApplicationLifecycle())
                .api(new WalletInfoApi()
                        .setWallet_type("metamask")
                        .setWallet_address(address))
                .request(new OnHttpListener<BaseBean<List<WalletInfo>>>() {
                    @Override
                    public void onSucceed(BaseBean<List<WalletInfo>> result) {
                        if (!CollectionUtils.isEmpty(result.getData())) {
                            WalletInfo walletInfo;
                            if (userSelf) {
                                walletInfo = CollectionUtils.find(result.getData(), item -> item.getTg_user_id() == user.id);
                            } else {
                                walletInfo = result.getData().get(0);
                            }
                            if (walletInfo.getTg_user_id() != 0) { // TG用户
                                if (!TextUtils.isEmpty(walletInfo.nft_contract_image)) {
                                    Glide.with(getParentActivity()).load(walletInfo.nft_contract_image).into(binding.ivNftPhoto);
                                    getNftInfo(walletInfo);
                                }else {

                                }
                            }
                        }
                    }

                    @Override
                    public void onFail(Exception e) {

                    }
                });
    }

    private void getNftInfo(WalletInfo walletInfo) {
        EasyHttp.get(new ApplicationLifecycle())
                .api(new RetrieveAnAssentApi()
                        .setAsset_contract_address(walletInfo.nft_contract)
                        .setToken_id(walletInfo.nft_token_id)
                        .setInclude_orders(true))
                .request(new OnHttpListener<NFTAssets.AssetsEntity>() {
                    @Override
                    public void onSucceed(NFTAssets.AssetsEntity result) {
                        NFTInfo nftInfo = NFTAssets.AssetsEntity.parse(result);
                        binding.tvNftName.setText(nftInfo.nft_name);
                        if (!TextUtils.isEmpty(nftInfo.price)) {
                            binding.tvNftPrice.setText(nftInfo.getEthPrice() + " ETH");
                        }
                        if (!userSelf) {
                            binding.tvContractAddressContent.setText(StringUtil.formatAddress(nftInfo.contract_address));
                            binding.tvTokenIdContent.setText(nftInfo.token_id);
                            binding.tvBlockchainContent.setText(nftInfo.blockchain);
                            binding.tvTokenStandardContent.setText(nftInfo.token_standard);
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                    }
                });
    }

    private void getHotRecommendData() {
        EasyHttp.post(new ApplicationLifecycle())
                .api(new HotRecommendApi(1, 10))
                .request(new OnHttpListener<BaseBean<BaseLoadmoreModel<HotRecommendData>>>() {
                    @Override
                    public void onSucceed(BaseBean<BaseLoadmoreModel<HotRecommendData>> result) {
                        walletHotRecommendAdapter.setList(dealWithHotRecommendData(result.getData().getData()));
                    }

                    @Override
                    public void onFail(Exception e) {
                        binding.rvHotRecommend.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * 处理
     * @param data
     */
    private List<HotRecommendData> dealWithHotRecommendData(List<HotRecommendData> data) {
        if (data.size() > 3) {
            int dataSize = data.size();
            HotRecommendData item = new HotRecommendData();
            List<String> avatar = new ArrayList<>();
            avatar.add(data.get(dataSize - 2).getAvatar());
            avatar.add(data.get(dataSize - 1).getAvatar());
            item.setAvatarList(avatar);
            data.add(item);
        }

        return data;
    }


    @Override
    public void onItemChildClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
        switch (view.getId()) {
            case R.id.tv_add_channel:
                //进入群聊或者频道
                AdapterUtil.recommendListIndexChatActOperation(
                        this,
                        walletHotRecommendAdapter,
                        walletHotRecommendAdapter.getData().get(position),
                        position
                );
                break;

            case R.id.tv_showall_channel:
                //显示所有
                presentFragment(new AllRecommendActivity(AllRecommendActivity.RECOMMEND_TYPE));
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_wallet_close:
                finishFragment();
                break;
            case R.id.ll_wallet_address:
                ClipboardUtils.copyText(address);
                ToastUtils.showLong("钱包地址已复制");
                break;
            case R.id.tv_search_wallet:
                if (TextUtils.isEmpty(binding.etWalletAddress.getText().toString())) {
                    ToastUtils.showLong("请输入钱包地址");
                    return;
                }
                Bundle args = new Bundle();
                args.putString("address", binding.etWalletAddress.getText().toString());
                args.putBoolean("userSelf", false);
                presentFragment(new WalletHomeAct(args));
                break;
        }
    }

    private class WalletHomeView extends FrameLayout implements OnRefreshListener, OnLoadMoreListener, OnItemClickListener {

        private ViewWalletHomeBinding viewWalletHomeBinding;
        private String title;
        private NFTAssetsAdapter nftListAdapter;
        private String order_direction = "desc";
        private String limit = "10";
        private String cursor;

        public WalletHomeView(Context context, String title) {
            super(context);
            this.title = title;
            initView();
            initData();
        }

        private void initView() {
            viewWalletHomeBinding = ViewWalletHomeBinding.inflate(LayoutInflater.from(getContext()), this, true);

        }

        private void initData() {
            if (!"NFTs".equals(title)) return;
            viewWalletHomeBinding.refreshLayout.setOnRefreshListener(this);
            viewWalletHomeBinding.recyclerView.setLayoutManager(new GridLayoutManager(getParentActivity(), 2));
            viewWalletHomeBinding.recyclerView.addItemDecoration(new CustomItemDecoration(2, 20f, 20f, true));
            nftListAdapter = new NFTAssetsAdapter();
            nftListAdapter.getLoadMoreModule().setOnLoadMoreListener(this);
            nftListAdapter.setOnItemClickListener(this);
            viewWalletHomeBinding.recyclerView.setAdapter(nftListAdapter);
            viewWalletHomeBinding.refreshLayout.autoRefresh();
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
                            .setOwner(address)
//                            .setOwner("0xdF1Cc8163f61B6F7648b8250d5E916A8837c44A2")
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
                            viewWalletHomeBinding.refreshLayout.finishRefresh();
                        }

                        @Override
                        public void onFail(Exception e) {
                            nftListAdapter.getLoadMoreModule().loadMoreFail();
                            viewWalletHomeBinding.refreshLayout.finishRefresh();
                        }
                    });
        }

        @Override
        public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
            if (!userSelf) return; // 只能选自己的NFT
            new CommonTipsDialog(getParentActivity(), "使用此 NFT 作为头像显示？") {
                @Override
                public void onRightClick() {
                    NFTInfo nftInfo = nftListAdapter.getItem(position);
                    AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
                    progressDialog.setCanCancel(false);

                    String path = PathUtils.getExternalAppFilesPath() + "/nft_images" + nftInfo.original_url.substring(nftInfo.original_url.lastIndexOf("/"));
                    boolean svg = FileUtils.getFileExtension(path).equals("svg");
                    if (svg) path = path.substring(0, path.length() - 4) + ".jpg";
                    if (!FileUtils.isFileExists(path)) {
                        FileUtils.createOrExistsFile(path);
                        if (svg) { // svg图片重新生成
                            progressDialog.show();
                            String finalPath = path;
                            Glide.with(getParentActivity()).load(nftInfo.original_url).addListener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    ThreadUtils.executeByIo(new ThreadUtils.SimpleTask<String>() {
                                        @Override
                                        public String doInBackground() throws Throwable {
                                            Bitmap bitmap = ImageUtils.view2Bitmap(viewWalletHomeBinding.ivNftAvatar);
                                            FileIOUtils.writeFileFromBytesByStream(finalPath, ConvertUtils.bitmap2Bytes(bitmap));
                                            return null;
                                        }

                                        @Override
                                        public void onSuccess(String result) {
                                            progressDialog.dismiss();
                                            openPhotoForSelect(nftInfo, new File(finalPath));
                                        }
                                    });
                                    return false;
                                }
                            }).into(viewWalletHomeBinding.ivNftAvatar);
                            return;
                        }
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
                    } else {
                        openPhotoForSelect(nftInfo, new File(path));
                    }
                }
            }
                    .setLeftTextAndColor("取消", Color.parseColor("#868686"))
                    .setRightTextAndColor("确认", Color.parseColor("#03BDFF"))
                    .show();
        }

        private void openPhotoForSelect(NFTInfo nftInfo, File file) {
            PhotoViewer.getInstance().setParentActivity(getParentActivity(), null);
            ArrayList<Object> photos = new ArrayList<>();
            MediaController.PhotoEntry photoEntry = new MediaController.PhotoEntry(0, 0, 0, file.getAbsolutePath(), 0, false, 0, 0, 0);
            photos.add(photoEntry);
            PhotoViewer.getInstance().openPhotoForSelect(photos, 0, PhotoViewer.SELECT_TYPE_AVATAR, false, new PhotoViewer.EmptyPhotoViewerProvider() {
                @Override
                public PhotoViewer.PlaceProviderObject getPlaceForPhoto(MessageObject messageObject, TLRPC.FileLocation fileLocation, int index, boolean needPreview) {
                    return null;
                }

                @Override
                public void sendButtonPressed(int index, VideoEditedInfo videoEditedInfo, boolean notify, int scheduleDate, boolean forceDocument) {
                    EventBus.getDefault().post(new MessageEvent(EventBusTags.UPLOAD_USER_PROFILE, currentTimeMillis + "", (Object) file.getAbsolutePath()));
                    EasyHttp.post(new ApplicationLifecycle())
                            .api(new NftInfoApi()
                                    .setNft_contract(nftInfo.contract_address)
                                    .setNft_contract_image(nftInfo.original_url)
                                    .setNft_token_id(nftInfo.token_id))
                            .request(null);
                    WalletInfo walletInfo = new WalletInfo();
                    walletInfo.nft_contract_image = nftInfo.original_url;
                    walletInfo.nft_contract = nftInfo.contract_address;
                    walletInfo.nft_token_id = nftInfo.token_id;
                    Glide.with(getParentActivity()).load(walletInfo.nft_contract_image).into(binding.ivNftPhoto);
                    getNftInfo(walletInfo);
                }
            }, null);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receiveMessage(MessageEvent event) {
        switch (event.getType()) {
        }
    }
}
