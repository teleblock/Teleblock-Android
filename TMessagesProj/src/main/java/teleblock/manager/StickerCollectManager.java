package teleblock.manager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import teleblock.database.KKVideoMessageDB;
import teleblock.model.GifStickerEntity;


public class StickerCollectManager {

    private final Executor taskExecutor = Executors.newSingleThreadExecutor();
    private static StickerCollectManager instance;

    public interface StickerLoadListener {
        void onLoad(List<GifStickerEntity> list);
    }

    public static StickerCollectManager getInstance() {
        if (instance == null) {
            synchronized (StickerCollectManager.class) {
                if (instance == null) {
                    instance = new StickerCollectManager();
                }
            }
        }
        return instance;
    }

    /**
     * 通过sticker获取pack详情
     */
    public void loadStickerAndSave(TLRPC.InputStickerSet inputStickerSet, ChatActivity.ChatActivityAdapter chatAdapter) {
        TLRPC.TL_messages_getStickerSet req = new TLRPC.TL_messages_getStickerSet();
        req.stickerset = inputStickerSet;
        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TLRPC.TL_messages_stickerSet stickerSet = (TLRPC.TL_messages_stickerSet) response;
                putStickerSet(stickerSet);
                chatAdapter.notifyDataSetChanged();
            }
        }));
    }

    /**
     * 保存sticker包
     */
    public void putStickerSet(TLRPC.TL_messages_stickerSet stickerSet) {
        taskExecutor.execute(() -> {
            KKVideoMessageDB.getInstance(UserConfig.selectedAccount).insertStickerSet(stickerSet);
        });
    }

    /**
     * 保存gif、sticker
     */
    public void insertGifSticker(TLRPC.Document document, int type) {
//        if (type == 2) {//gif
//            EventUtil.post(ApplicationLoader.applicationContext, EventUtil.Even.GIF收藏, new HashMap<>());
//        } else if (type == 3) {//sticker
//            EventUtil.post(ApplicationLoader.applicationContext, EventUtil.Even.表情收藏, new HashMap<>());
//        }
        taskExecutor.execute(() -> {
            KKVideoMessageDB.getInstance(UserConfig.selectedAccount).insertGifSticker(document, type);
            AndroidUtilities.runOnUIThread(() -> {
//                NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.gifStickerCollect);
                NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.loadGifStickerData);
            });
        });
    }

    /**
     * 删除gif、sticker
     */
    public void deleteGifSticker(TLRPC.Document document) {
        KKVideoMessageDB.getInstance(UserConfig.selectedAccount).deleteSticker(document.id);
        NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.loadGifStickerData);
    }

    /**
     * 查询sticker是否存在
     */
    public boolean isStickerExists(TLRPC.Document document) {
        return KKVideoMessageDB.getInstance(UserConfig.selectedAccount).queryStickerExists(document.id);
    }

    /**
     * 加载sticker列表
     */
    public void loadStickerList(StickerLoadListener listener) {
        taskExecutor.execute(() -> {
            List<GifStickerEntity> list = KKVideoMessageDB.getInstance(UserConfig.selectedAccount).queryStickers();
            AndroidUtilities.runOnUIThread(() -> listener.onLoad(list));
        });
    }
}
