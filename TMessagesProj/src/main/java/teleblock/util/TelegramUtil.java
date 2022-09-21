package teleblock.util;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import com.blankj.utilcode.constant.TimeConstants;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.coingecko.domain.Coins.CoinList;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.hjq.http.EasyHttp;
import com.hjq.http.lifecycle.ApplicationLifecycle;
import com.hjq.http.listener.OnHttpListener;

import org.greenrobot.eventbus.EventBus;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.support.LongSparseIntArray;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.EmojiThemes;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.FilterCreateActivity;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PrivacyControlActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.manager.CoinGeckoManager;
import teleblock.manager.DialogManager;
import teleblock.manager.LoginManager;
import teleblock.model.EvaluateAppEntity;
import teleblock.model.wallet.WalletInfo;
import teleblock.network.BaseBean;
import teleblock.network.api.CurrencyKeywordsApi;
import teleblock.network.api.TgInfoApi;
import teleblock.network.api.WalletInfoApi;
import teleblock.wallet.WCSessionManager;

/**
 * Time:2022/6/29
 * Author:Perry
 * Description：
 */
public class TelegramUtil {

    public static final int LocaleCN = 1;
    public static final int LocaleHK = 2;
    public static final int LocaleTW = 3;
    public static final int LocaleEN = 4;
    public static final int LocaleBAIN = 5;
    public static final int LocaleBAML = 6;
    public static final int LocalePOR = 7;
    public static final int LocaleESP = 8;

    /**
     * 设置语言
     */
    public static void loadAndSetLanguage(Activity activity) {
        if (activity == null) return;
        int select = MMKVUtil.loginSelectLanguage();
        if (select == -1) {
            return;
        }
        MMKVUtil.loginSelectLanguage(-1);
        LocaleController.LocaleInfo existingInfo = null;
        if (select == LocaleTW) {//台湾
            existingInfo = LocaleController.getInstance().getLanguageFromDict("zh_tw");
        } else if (select == LocaleHK) {//香港
            existingInfo = LocaleController.getInstance().getLanguageFromDict("zh_hk");
        } else if (select == LocaleCN) {//中文
            existingInfo = LocaleController.getInstance().getLanguageFromDict("zh_cn");
        } else if (select == LocaleEN) {//英文
            existingInfo = LocaleController.getInstance().getLanguageFromDict("en");
        } else if (select == LocaleBAIN) {//印尼
            existingInfo = LocaleController.getInstance().getLanguageFromDict("id");
        } else if (select == LocaleBAML) {//马来
            existingInfo = LocaleController.getInstance().getLanguageFromDict("ms");
        } else if (select == LocalePOR) {//葡萄牙
            existingInfo = LocaleController.getInstance().getLanguageFromDict("pt_br");
        } else if (select == LocaleESP) {//西班牙
            existingInfo = LocaleController.getInstance().getLanguageFromDict("es");
        }
        if (existingInfo != null) {
            LocaleController.LocaleInfo fExistingInfo = existingInfo;
            AndroidUtilities.runOnUIThread(() -> {
                LocaleController.getInstance().applyLanguage(fExistingInfo, true, false, false, true, UserConfig.selectedAccount);
                if (activity instanceof LaunchActivity) {
                    ((LaunchActivity) activity).rebuildAllFragments(true);
                }
            });
        }
    }

    /**
     * 应用隐私设置
     */
    public static void applyPrivacySettings() {
        if (MMKVUtil.disallowAllSeePhone() && !MMKVUtil.seePhoneApply()) {
            TLRPC.TL_account_setPrivacy req = new TLRPC.TL_account_setPrivacy();
            req.key = new TLRPC.TL_inputPrivacyKeyPhoneNumber();
            req.rules.add(new TLRPC.TL_inputPrivacyValueDisallowAll());
            ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                if (error == null) {
                    TLRPC.TL_account_privacyRules privacyRules = (TLRPC.TL_account_privacyRules) response;
                    ContactsController.getInstance(UserConfig.selectedAccount).setPrivacyRules(privacyRules.rules, PrivacyControlActivity.PRIVACY_RULES_TYPE_PHONE);
                    MMKVUtil.seePhoneApply(true);
                }
            }), ConnectionsManager.RequestFlagFailOnServerErrors);
        }
    }

    /**
     * 应用主题
     */
    public static void applyTheme(Theme.ThemeInfo themeInfo) {
        if (themeInfo.info != null) {
            if (!themeInfo.themeLoaded) {
                return;
            }
        }
        if (!TextUtils.isEmpty(themeInfo.assetName)) {
            Theme.PatternsLoader.createLoader(false);
        }

        SharedPreferences.Editor editor = ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Activity.MODE_PRIVATE).edit();
        //editor.putString(themeInfo.isDark() ? "lastDarkTheme" : "lastDayTheme", themeInfo.getKey());
        editor.putString("lastDayTheme", themeInfo.getKey());
        editor.commit();

        if (themeInfo == Theme.getCurrentTheme()) {
            return;
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme, themeInfo, false, null, -1);
        EmojiThemes.saveCustomTheme(themeInfo, themeInfo.currentAccentId);
    }

    /**
     * 保存货币数据
     */
    public static void saveCoinData() {
        if (true) return; // 暂时屏蔽
        //登录请求
        LoginManager.userLogin(() -> {
            CoinGeckoManager.getInstance().getCoinList(new CoinGeckoManager.Callback<List<CoinList>>() {
                @Override
                public void onSuccess(List<CoinList> data) {
                    //关键词返回
                    EasyHttp.post(new ApplicationLifecycle())
                            .api(new CurrencyKeywordsApi())
                            .request(new OnHttpListener<BaseBean<List<String>>>() {
                                @Override
                                public void onSucceed(BaseBean<List<String>> result) {
                                    new Thread(() -> {
                                        List<String> keywords = new ArrayList<>();
                                        List<CoinList> coinLists = new ArrayList<>();
                                        for (String s : result.getData()) {
                                            for (CoinList coinList : data) {
                                                if (s.equalsIgnoreCase(coinList.getSymbol())) {
                                                    keywords.add(s);
                                                    coinLists.add(coinList);
                                                    break;
                                                }
                                            }
                                        }
                                        CoinUtil.saveCoinData(keywords, coinLists);
                                    }).start();
                                }

                                @Override
                                public void onFail(Exception e) {

                                }
                            });
                }
            });
        }, null);
    }

    /**
     * 应用内评价
     */
    public static void handleEvaluateApp(Activity activity) {
        if (activity == null) return;
        if (MMKVUtil.isNeverEvaluate()) return;
        EvaluateAppEntity evaluateAppEntity = JsonUtil.parseJsonToBean(MMKVUtil.getEvaluateApp(), EvaluateAppEntity.class);
        if (evaluateAppEntity.isEvaluatedApp()) return;
        long openAppDate = evaluateAppEntity.getFirstOpenAppDate();
        if (openAppDate == 0) {
            evaluateAppEntity.setFirstOpenAppDate(TimeUtil.getTimerShort(TimeUtil.getNowTimestamp2ShortY()));
            MMKVUtil.setEvaluateApp(JsonUtil.parseObjToJson(evaluateAppEntity));
        } else {
            long nowDate = TimeUtil.getTimerShort(TimeUtil.getNowTimestamp2ShortY());
            if (nowDate != evaluateAppEntity.getCurrentDate()) {
                evaluateAppEntity.setCurrentDate(nowDate);
                evaluateAppEntity.setDayFirstTime(true);
            } else {
                evaluateAppEntity.setDayFirstTime(false);
            }
            MMKVUtil.setEvaluateApp(JsonUtil.parseObjToJson(evaluateAppEntity));
            if (!evaluateAppEntity.isDayFirstTime()) return;
            long days = TimeUtils.getTimeSpan(nowDate, openAppDate, TimeConstants.DAY);
            if (days == 3 || days == 5) {
                ReviewManager manager = ReviewManagerFactory.create(activity);
                Task<ReviewInfo> request = manager.requestReviewFlow();
                request.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (activity.isDestroyed()) return;
                        ReviewInfo reviewInfo = task.getResult();
                        Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                        flow.addOnCompleteListener(task1 -> {
                            EventUtil.track(activity, EventUtil.Even.好评弹窗展示, null);
                            evaluateAppEntity.setEvaluatedApp(true);
                            MMKVUtil.setEvaluateApp(JsonUtil.parseObjToJson(evaluateAppEntity));
                        });
                    }
                });
            } else if (days > 5) {
                MMKVUtil.setNeverEvaluate(true);
            }
        }
    }

    /**
     * 添加默认分组
     *
     * @param fragment
     */
    public static void addDefaultFilters(BaseFragment fragment) {
        ArrayList<MessagesController.DialogFilter> filters = fragment.getMessagesController().dialogFilters;
        // 只有全部对话时添加默认分组
        if (filters.size() == 1 && !MMKVUtil.addedDefaultFilters()) {
            AlertDialog progressDialog = new AlertDialog(fragment.getParentActivity(), 3);
            progressDialog.setCanCancel(false);
            progressDialog.show();
            new Thread(() -> {
                int[] flags = new int[]{
                        MessagesController.DIALOG_FILTER_FLAG_GROUPS,
                        MessagesController.DIALOG_FILTER_FLAG_CHANNELS,
                        MessagesController.DIALOG_FILTER_FLAG_CONTACTS,
                        MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS,
                        MessagesController.DIALOG_FILTER_FLAG_BOTS,
                        95,
                };
                String[] newFilters = new String[]{
                        LocaleController.getString("FilterGroupsNew", R.string.FilterGroupsNew),
                        LocaleController.getString("FilterChannelsNew", R.string.FilterChannelsNew),
                        LocaleController.getString("FilterContactsNew", R.string.FilterContactsNew),
                        LocaleController.getString("FilterNonContactsNew", R.string.FilterNonContactsNew),
                        LocaleController.getString("FilterBotsNew", R.string.FilterBotsNew),
                        LocaleController.getString("FilterUnreadNew", R.string.FilterUnreadNew)
                };
                for (int i = 0; i < newFilters.length; i++) {
                    String string = newFilters[i];
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
                    FilterCreateActivity.saveFilterToServer(filter, newFilterFlags, newFilterName, newAlwaysShow, newNeverShow, newPinned, true, false, false, true, false, fragment, new Runnable() {
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
                AndroidUtilities.runOnUIThread(() -> {
                    progressDialog.dismiss();
                    MMKVUtil.addedDefaultFilters(true);
                    fragment.getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                    MessagesStorage.getInstance(UserConfig.selectedAccount).saveDialogFiltersOrder();
                    TLRPC.TL_messages_updateDialogFiltersOrder req = new TLRPC.TL_messages_updateDialogFiltersOrder();
                    ArrayList<MessagesController.DialogFilter> dialogFilters = fragment.getMessagesController().dialogFilters;
                    for (int a = 0, N = dialogFilters.size(); a < N; a++) {
                        MessagesController.DialogFilter filter = dialogFilters.get(a);
                        req.order.add(filter.id);
                    }
                    fragment.getConnectionsManager().sendRequest(req, (response, error) -> {

                    });
                });
            }).start();
        }
    }

    /**
     * 更新用户的tg数量
     */
    public static void updateTgInfo() {
        new Handler().postDelayed(() -> LoginManager.userLogin(() -> {
            EasyHttp.post(new ApplicationLifecycle())
                    .api(new TgInfoApi()
                            .setTg_group_num(DialogManager.getInstance(UserConfig.selectedAccount).groupCount)
                            .setTg_channel_num(DialogManager.getInstance(UserConfig.selectedAccount).channelCount)
                            .setTg_user_num(ContactsController.getInstance(UserConfig.selectedAccount).contacts.size() - 1)
                            .setTg_bot_num(DialogManager.getInstance(UserConfig.selectedAccount).botCount))
                    .request(null);
        }, null), 10000);
    }

    /**
     * 连接钱包
     */
    public static void walletConnect() {
        if (!AppUtils.isAppInstalled("io.metamask")) {
            ToastUtils.showLong(LocaleController.getString("setting_wallet_uninstall_toast", R.string.setting_wallet_uninstall_toast));
            return;
        }
        WCSessionManager.getInstance().resetSession();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(WCSessionManager.getInstance().config.toWCUri()));
        intent.setPackage("io.metamask");
        ActivityUtils.startActivity(intent);
    }

    /**
     * 获取钱包相关数据
     */
    public static void getWalletInfo() {
        LoginManager.userLogin(() -> {
            String address = MMKVUtil.connectedWalletAddress();
            if (TextUtils.isEmpty(address)) return;
            EasyHttp.post(new ApplicationLifecycle())
                    .api(new WalletInfoApi()
                            .setWallet_type("metamask")
                            .setWallet_address(address))
                    .request(new OnHttpListener<BaseBean<List<WalletInfo>>>() {
                        @Override
                        public void onSucceed(BaseBean<List<WalletInfo>> result) {
                            if (!CollectionUtils.isEmpty(result.getData())) {
                                long userId = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
                                WalletInfo walletInfo = CollectionUtils.find(result.getData(), item -> item.getTg_user_id() == userId);
                                MMKVUtil.setWalletInfo(walletInfo);
                                EventBus.getDefault().post(new MessageEvent(EventBusTags.USER_INFO_CHANGED));
                            }
                        }

                        @Override
                        public void onFail(Exception e) {

                        }
                    });
        }, null);
    }
}
