package teleblock.wallet;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.PathUtils;
import com.hjq.http.EasyHttp;
import com.hjq.http.lifecycle.ApplicationLifecycle;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory;

import org.greenrobot.eventbus.EventBus;
import org.walletconnect.BridgeServer;
import org.walletconnect.Session;
import org.walletconnect.impls.FileWCSessionStore;
import org.walletconnect.impls.MoshiPayloadAdapter;
import org.walletconnect.impls.OkHttpTransport;
import org.walletconnect.impls.WCSession;
import org.walletconnect.impls.WCSessionStore;
import org.web3j.utils.Numeric;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import okhttp3.OkHttpClient;
import teleblock.event.EventBusTags;
import teleblock.event.MessageEvent;
import teleblock.network.api.BindWalletApi;
import teleblock.util.MMKVUtil;
import timber.log.Timber;

/**
 * 创建日期：2022/8/4
 * 描述：
 */
public class WCSessionManager implements Session.Callback {

    private static WCSessionManager instance;
    public Session.Config config;
    private OkHttpClient client;
    private Moshi moshi;
    private BridgeServer bridge;
    private WCSessionStore storage;
    public Session session;

    public static WCSessionManager getInstance() {
        if (instance == null) {
            synchronized (WCSessionManager.class) {
                if (instance == null) {
                    instance = new WCSessionManager();
                }
            }
        }
        return instance;
    }

    public WCSessionManager() {
    }

    public void init() {
        client = new OkHttpClient.Builder().build();
        moshi = new Moshi.Builder().addLast(new KotlinJsonAdapterFactory()).build();
        bridge = new BridgeServer(moshi);
        bridge.start();
        File file = new File(PathUtils.getInternalAppFilesPath(), "session_store.json");
        FileUtils.createOrExistsFile(file);
        storage = new FileWCSessionStore(file, moshi);
    }

    public void resetSession() {
        if (session != null) {
            session.clearCallbacks();
        }
        byte[] bytes = new byte[32];
        new Random().nextBytes(bytes);
        String key = Numeric.toHexStringNoPrefix(bytes);
        config = new Session.Config(
                UUID.randomUUID().toString(),
                "wss://bridge.walletconnect.org",
                key,
                "wc",
                2
        );
        session = new WCSession(
                config,
                new MoshiPayloadAdapter(moshi),
                storage,
                new OkHttpTransport.Builder(client, moshi),
                new Session.PeerMeta(
                        "https://teleblock.io/",
                        AppUtils.getAppName(),
                        "",
                        new ArrayList<>()
                ),
                "56"
        );
        session.addCallback(this);
        session.offer();
    }

    @Override
    public void onStatus(@NonNull Session.Status status) {
        Timber.i("onStatus-->" + status);
        if (Session.Status.Approved.INSTANCE.equals(status)) {
            String address = session.approvedAccounts().get(0);
            MMKVUtil.connectedWalletAddress(address);
            EventBus.getDefault().post(new MessageEvent(EventBusTags.WALLET_CONNECT_APPROVED));
            EasyHttp.post(new ApplicationLifecycle())
                    .api(new BindWalletApi()
                            .setWallet_type("metamask")
                            .setWallet_address(address))
                    .request(null);
        } else if (Session.Status.Closed.INSTANCE.equals(status)) {
        } else if (Session.Status.Connected.INSTANCE.equals(status)) {
        } else if (Session.Status.Disconnected.INSTANCE.equals(status)) {
        }

    }

    @Override
    public void onMethodCall(@NonNull Session.MethodCall call) {

    }
}