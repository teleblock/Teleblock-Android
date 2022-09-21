package teleblock.database;

import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.SparseIntArray;

import androidx.annotation.UiThread;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.support.SparseLongArray;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import teleblock.model.ChannelTagEntity;
import teleblock.model.ChannelWithTagEntity;
import teleblock.model.GifStickerEntity;
import teleblock.util.TGLog;
import teleblock.video.KKMessage;
import timber.log.Timber;

public class KKVideoMessageDB extends BaseController {

    private DispatchQueue storageQueue;
    private SQLiteDatabase database;
    private File cacheFile;
    private File walCacheFile;
    private File shmCacheFile;
    private SparseArray<ArrayList<Runnable>> tasks = new SparseArray<>();

    private final static int LAST_DB_VERSION = 2; // DB版本

    private CountDownLatch openSync = new CountDownLatch(1);

    private static volatile KKVideoMessageDB[] Instance = new KKVideoMessageDB[UserConfig.MAX_ACCOUNT_COUNT];

    public static KKVideoMessageDB getInstance(int num) {
        KKVideoMessageDB localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (KKVideoMessageDB.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new KKVideoMessageDB(num);
                }
            }
        }
        return localInstance;
    }

    private void ensureOpened() {
        try {
            openSync.await();
        } catch (Throwable ignore) {

        }
    }

    public KKVideoMessageDB(int account) {
        super(account);
        storageQueue = new DispatchQueue(getClass().getSimpleName() + "_" + account);
        //storageQueue.setPriority(Thread.MAX_PRIORITY);
        storageQueue.postRunnable(() -> openDatabase(1));
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public DispatchQueue getStorageQueue() {
        return storageQueue;
    }

    @UiThread
    public void bindTaskToGuid(Runnable task, int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            tasks.put(guid, arrayList);
        }
        arrayList.add(task);
    }

    @UiThread
    public void cancelTasksForGuid(int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            return;
        }
        for (int a = 0, N = arrayList.size(); a < N; a++) {
            storageQueue.cancelRunnable(arrayList.get(a));
        }
        tasks.remove(guid);
    }

    @UiThread
    public void completeTaskForGuid(Runnable runnable, int guid) {
        ArrayList<Runnable> arrayList = tasks.get(guid);
        if (arrayList == null) {
            return;
        }
        arrayList.remove(runnable);
        if (arrayList.isEmpty()) {
            tasks.remove(guid);
        }
    }

    public long getDatabaseSize() {
        long size = 0;
        if (cacheFile != null) {
            size += cacheFile.length();
        }
        if (shmCacheFile != null) {
            size += shmCacheFile.length();
        }
        /*if (walCacheFile != null) {
            size += walCacheFile.length();
        }*/
        return size;
    }

    public void openDatabase(int openTries) {
        File filesDir = ApplicationLoader.getFilesDirFixed();
        if (currentAccount != 0) {
            filesDir = new File(filesDir, "account" + currentAccount + "/");
            filesDir.mkdirs();
        }
        cacheFile = new File(filesDir, getClass().getSimpleName() + ".db");
        walCacheFile = new File(filesDir, getClass().getSimpleName() + ".db-wal");
        shmCacheFile = new File(filesDir, getClass().getSimpleName() + ".db-shm");

        boolean createTable = false;

        if (!cacheFile.exists()) {
            createTable = true;
        }
        try {
            database = new SQLiteDatabase(cacheFile.getPath());
            database.executeFast("PRAGMA secure_delete = ON").stepThis().dispose();
            database.executeFast("PRAGMA temp_store = MEMORY").stepThis().dispose();
            database.executeFast("PRAGMA journal_mode = WAL").stepThis().dispose();
            database.executeFast("PRAGMA journal_size_limit = 10485760").stepThis().dispose();

            if (createTable) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("create new database");
                }

                database.executeFast("CREATE TABLE messages(mid INTEGER PRIMARY KEY, uid INTEGER, read_state INTEGER, send_state INTEGER, date INTEGER, data BLOB, out INTEGER, ttl INTEGER, media INTEGER, replydata BLOB, imp INTEGER, mention INTEGER, forwards INTEGER, replies_data BLOB, thread_reply_id INTEGER, download_time INTEGER, dialog_id INTEGER, local_attach_name TEXT, message_flag TEXT)").stepThis().dispose();
                database.executeFast("CREATE TABLE message_filter(mid INTEGER PRIMARY KEY, date INTEGER)").stepThis().dispose();
                database.executeFast("CREATE TABLE chat_filter(cid INTEGER PRIMARY KEY, date INTEGER)").stepThis().dispose();
                database.executeFast("CREATE TABLE message_collect(mid INTEGER PRIMARY KEY, uid INTEGER, read_state INTEGER, send_state INTEGER, date INTEGER, data BLOB, out INTEGER, ttl INTEGER, media INTEGER, replydata BLOB, imp INTEGER, mention INTEGER, forwards INTEGER, replies_data BLOB, thread_reply_id INTEGER, collect_time INTEGER, dialog_id INTEGER, local_attach_name TEXT)").stepThis().dispose();

                //联系人
                database.executeFast("CREATE TABLE contacts_info(user_id INTEGER, read_message INTEGER)").stepThis().dispose();

                //收藏的gif、sticker
                database.executeFast("CREATE TABLE gif_sticker(id INTEGER PRIMARY KEY, data BLOB, type INTEGER, mid INTEGER, date INTEGER)").stepThis().dispose();

                //version
                database.executeFast("PRAGMA user_version = " + LAST_DB_VERSION).stepThis().dispose();

                //channel标签表
                database.executeFast("CREATE TABLE channel_tag(tag_id INTEGER PRIMARY KEY AUTOINCREMENT, tag_name TEXT, level INTEGER DEFAULT 999, update_time INTEGER)").stepThis().dispose();
                database.executeFast("CREATE TABLE channel_with_tag(channel_id INTEGER, tag_id INTEGER, tag_name TEXT, update_time INTEGER)").stepThis().dispose();
            } else {
                int version = database.executeInt("PRAGMA user_version");
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("current db version = " + version);
                }
                if (version == 0) {
                    throw new Exception("malformed");
                }
                if (version < LAST_DB_VERSION) {
                    updateDbToLastVersion(LAST_DB_VERSION);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);

            if (openTries < 3 && e.getMessage().contains("malformed")) {
                if (openTries == 2) {
                    cleanupInternal(true);
                    for (int a = 0; a < 2; a++) {
                        getUserConfig().setDialogsLoadOffset(a, 0, 0, 0, 0, 0, 0);
                        getUserConfig().setTotalDialogsCount(a, 0);
                    }
                    getUserConfig().saveConfig(false);
                } else {
                    cleanupInternal(false);
                }
                openDatabase(openTries == 1 ? 2 : 3);
            }
        }
        try {
            openSync.countDown();
        } catch (Throwable ignore) {

        }
    }

    private void updateDbToLastVersion(final int currentVersion) {
        try {
            database.executeFast("CREATE TABLE IF NOT EXISTS messages(mid INTEGER PRIMARY KEY, uid INTEGER, read_state INTEGER, send_state INTEGER, date INTEGER, data BLOB, out INTEGER, ttl INTEGER, media INTEGER, replydata BLOB, imp INTEGER, mention INTEGER, forwards INTEGER, replies_data BLOB, thread_reply_id INTEGER, download_time INTEGER, dialog_id INTEGER, local_attach_name TEXT, message_flag TEXT)").stepThis().dispose();
            database.executeFast("CREATE TABLE IF NOT EXISTS message_filter(mid INTEGER PRIMARY KEY, date INTEGER)").stepThis().dispose();
            database.executeFast("CREATE TABLE IF NOT EXISTS chat_filter(cid INTEGER PRIMARY KEY, date INTEGER)").stepThis().dispose();
            database.executeFast("CREATE TABLE IF NOT EXISTS message_collect(mid INTEGER PRIMARY KEY, uid INTEGER, read_state INTEGER, send_state INTEGER, date INTEGER, data BLOB, out INTEGER, ttl INTEGER, media INTEGER, replydata BLOB, imp INTEGER, mention INTEGER, forwards INTEGER, replies_data BLOB, thread_reply_id INTEGER, collect_time INTEGER, dialog_id INTEGER, local_attach_name TEXT)").stepThis().dispose();
            database.executeFast("CREATE TABLE IF NOT EXISTS contacts_info(user_id INTEGER,read_message INTEGER)").stepThis().dispose();
            database.executeFast("CREATE TABLE IF NOT EXISTS gif_sticker(id INTEGER PRIMARY KEY, data BLOB,type INTEGER,mid INTEGER, date INTEGER)").stepThis().dispose();
            database.executeFast("CREATE TABLE IF NOT EXISTS channel_tag(tag_id INTEGER PRIMARY KEY AUTOINCREMENT, tag_name TEXT,level INTEGER DEFAULT 999, update_time INTEGER)").stepThis().dispose();
            database.executeFast("CREATE TABLE IF NOT EXISTS channel_with_tag(channel_id INTEGER, tag_id INTEGER, tag_name TEXT, update_time INTEGER)").stepThis().dispose();

            //version
            database.executeFast("PRAGMA user_version = " + currentVersion).stepThis().dispose();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }


    private void executeNoException(String query) {
        try {
            database.executeFast(query).stepThis().dispose();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void cleanupInternal(boolean deleteFiles) {
        if (database != null) {
            database.close();
            database = null;
        }
        if (deleteFiles) {
            if (cacheFile != null) {
                cacheFile.delete();
                cacheFile = null;
            }
            if (walCacheFile != null) {
                walCacheFile.delete();
                walCacheFile = null;
            }
            if (shmCacheFile != null) {
                shmCacheFile.delete();
                shmCacheFile = null;
            }
        }
    }

    public void cleanup(final boolean isLogin) {
        storageQueue.postRunnable(() -> {
            cleanupInternal(true);
            openDatabase(1);
            if (isLogin) {
                Utilities.stageQueue.postRunnable(() -> getMessagesController().getDifference());
            }
        });
    }

    public List<Integer> loadWatchChatIds() {
        ensureOpened();
        List<Integer> result = new ArrayList<>();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT cid, date FROM chat_filter"));
            while (cursor.next()) {
                result.add(cursor.intValue(0));
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }

    public void addWatchChat(long cid, int date) {
        ensureOpened();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT cid FROM chat_filter WHERE cid=" + cid);
            if (cursor.next()) {
                TGLog.debug("select chat_filter record for cid:" + cid);
            } else {
                Timber.d("insert chat_filter for cid:" + cid);
                SQLitePreparedStatement state_messages = database.executeFast("INSERT INTO chat_filter VALUES(" +
                        "?, " +     //cid
                        "? " +     //date
                        ")");
                state_messages.requery();
                state_messages.bindLong(1, cid);
                state_messages.bindInteger(2, date);
                state_messages.step();
                state_messages.dispose();
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
    }

    public void removeWatchChat(long cid) {
        ensureOpened();
        try {
            SQLitePreparedStatement statement = database.executeFast("DELETE FROM chat_filter WHERE cid = ?");
            statement.requery();
            statement.bindLong(1, cid);
            statement.step();
            statement.dispose();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    //带标示的消息id
    public List<Integer> loadFlagMessageIds(String messageFlag) {
        ensureOpened();
        List<Integer> result = new ArrayList<>();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT mid FROM messages WHERE message_flag='" + messageFlag + "'"));
            while (cursor.next()) {
                result.add(cursor.intValue(0));
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }

    //获取下载消息
    public List<KKMessage> loadMessages(String messageFlag) {
        ensureOpened();
        List<KKMessage> result = new ArrayList<>();
        SQLiteCursor cursor = null;
        try {
            String sql = "SELECT data, mid, date, dialog_id, download_time FROM messages ";
            sql += (" WHERE message_flag='" + messageFlag + "'");
            sql += " ORDER BY download_time DESC ";
            cursor = database.queryFinalized(String.format(Locale.US, sql));
            while (cursor.next()) {
                NativeByteBuffer data = cursor.byteBufferValue(0);
                if (data != null) {
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    if (message != null) {
                        message.readAttachPath(data, getUserConfig().clientUserId);
                        data.reuse();
                        message.id = cursor.intValue(1);
                        message.date = cursor.intValue(2);
                        message.dialog_id = cursor.longValue(3);
                        long downloadTime = cursor.longValue(4);
                        result.add(new KKMessage(message, downloadTime));
                    }
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }

    //通过文件名移除下载消息
    public void removeMessageByAttachFileName(String attachFileName) {
        ensureOpened();
        try {
            SQLitePreparedStatement statement = database.executeFast("DELETE FROM messages WHERE local_attach_name = ?");
            statement.requery();
            statement.bindString(1, attachFileName);
            statement.step();
            statement.dispose();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    //查询是否是手动下载的
    public boolean queryMessageFlag(String fileName, String messageFlag) {
        ensureOpened();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT mid FROM messages WHERE local_attach_name='" + fileName + "' AND message_flag='" + messageFlag + "'");
            if (cursor.next()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return false;
    }

    //添加下载消息
    public void putMessage(TLRPC.Message message, long downloadTime, String localFileName, String messageFlag) {
        ensureOpened();
        int mid = message.id;
        long dialog_id = message.dialog_id;
        if (mid == 0 || dialog_id == 0) return;
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT mid FROM messages WHERE mid=" + mid + " AND dialog_id=" + dialog_id);
            if (cursor.next()) {
                if (!TextUtils.isEmpty(messageFlag)) {
                    database.executeFast("UPDATE messages SET message_flag='" + messageFlag + "' WHERE mid=" + mid + " AND dialog_id=" + dialog_id).stepThis().dispose();
                    return;
                }
                Timber.d("has record for mid:" + mid + ",dialog_id=" + dialog_id);
            } else {
                Timber.d("insert message for mid:" + mid + ",dialog_id=" + dialog_id);
                NativeByteBuffer data = new NativeByteBuffer(message.getObjectSize());
                message.serializeToStream(data);
                SQLitePreparedStatement state_messages = database.executeFast("INSERT INTO messages VALUES(" +
                        "?, " +     //mid
                        "?, " +     //uid
                        "?, " +     //read_state
                        "?, " +     //send_state
                        "?, " +     //date
                        "?, " +     //data
                        "?, " +     //out
                        "?, " +     //ttl
                        "?, " +     //media
                        "NULL, " +  //replydata
                        "?, " +     //imp
                        "?, " +     //mention
                        "?, " +     //forwards
                        "?, " +     //replies_data
                        "?, " +     //thread_reply_id
                        "?, " +     //download_time
                        "?, " +     //dialog_id
                        "?, " +     //local_attach_name
                        "? " +     //message_flag
                        ")");
                state_messages.requery();
                state_messages.bindLong(1, message.id);
                state_messages.bindLong(2, message.dialog_id);
                state_messages.bindInteger(3, MessageObject.getUnreadFlags(message));
                state_messages.bindInteger(4, message.send_state);
                state_messages.bindInteger(5, message.date);
                state_messages.bindByteBuffer(6, data);
                state_messages.bindInteger(7, (MessageObject.isOut(message) || message.from_scheduled ? 1 : 0));
                state_messages.bindInteger(8, message.ttl);
                if ((message.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0) {
                    state_messages.bindInteger(9, message.views);
                } else {
                    state_messages.bindInteger(9, getMessageMediaType(message));
                }
                int flags = 0;
                if (message.stickerVerified == 0) {
                    flags |= 1;
                } else if (message.stickerVerified == 2) {
                    flags |= 2;
                }
                state_messages.bindInteger(10, flags);
                state_messages.bindInteger(11, message.mentioned ? 1 : 0);
                state_messages.bindInteger(12, message.forwards);
                NativeByteBuffer repliesData = null;
                if (message.replies != null) {
                    repliesData = new NativeByteBuffer(message.replies.getObjectSize());
                    message.replies.serializeToStream(repliesData);
                    state_messages.bindByteBuffer(13, repliesData);
                } else {
                    state_messages.bindNull(13);
                }
                if (message.reply_to != null) {
                    state_messages.bindInteger(14, message.reply_to.reply_to_top_id != 0 ? message.reply_to.reply_to_top_id : message.reply_to.reply_to_msg_id);
                } else {
                    state_messages.bindInteger(14, 0);
                }
                state_messages.bindLong(15, downloadTime);
                state_messages.bindLong(16, message.dialog_id);
                state_messages.bindString(17, localFileName);
                state_messages.bindString(18, messageFlag);
                state_messages.step();
                state_messages.dispose();
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
    }

    //收藏消息
    public boolean collectMessage(TLRPC.Message message, long collectTime, String localFileName) {
        ensureOpened();
        int mid = message.id;
        long dialog_id = message.dialog_id;
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT mid FROM message_collect WHERE mid=" + mid + " AND dialog_id=" + dialog_id);
            if (cursor.next()) {
                Timber.d("has collect for mid:" + mid + ",dialog_id:" + dialog_id);
                return true;
            } else {
                Timber.d("insert message_collect for mid:" + mid + ",dialog_id:" + dialog_id);
                NativeByteBuffer data = new NativeByteBuffer(message.getObjectSize());
                message.serializeToStream(data);
                SQLitePreparedStatement state_messages = database.executeFast("INSERT INTO message_collect VALUES(" +
                        "?, " +     //mid
                        "?, " +     //uid
                        "?, " +     //read_state
                        "?, " +     //send_state
                        "?, " +     //date
                        "?, " +     //data
                        "?, " +     //out
                        "?, " +     //ttl
                        "?, " +     //media
                        "NULL, " +  //replydata
                        "?, " +     //imp
                        "?, " +     //mention
                        "?, " +     //forwards
                        "?, " +     //replies_data
                        "?, " +     //thread_reply_id
                        "?, " +     //download_time
                        "?, " +     //dialog_id
                        "? " +     //local_attach_name
                        ")");
                state_messages.requery();
                state_messages.bindLong(1, message.id);
                state_messages.bindLong(2, message.dialog_id);
                state_messages.bindInteger(3, MessageObject.getUnreadFlags(message));
                state_messages.bindInteger(4, message.send_state);
                state_messages.bindInteger(5, message.date);
                state_messages.bindByteBuffer(6, data);
                state_messages.bindInteger(7, (MessageObject.isOut(message) || message.from_scheduled ? 1 : 0));
                state_messages.bindInteger(8, message.ttl);
                if ((message.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0) {
                    state_messages.bindInteger(9, message.views);
                } else {
                    state_messages.bindInteger(9, getMessageMediaType(message));
                }
                int flags = 0;
                if (message.stickerVerified == 0) {
                    flags |= 1;
                } else if (message.stickerVerified == 2) {
                    flags |= 2;
                }
                state_messages.bindInteger(10, flags);
                state_messages.bindInteger(11, message.mentioned ? 1 : 0);
                state_messages.bindInteger(12, message.forwards);
                NativeByteBuffer repliesData = null;
                if (message.replies != null) {
                    repliesData = new NativeByteBuffer(message.replies.getObjectSize());
                    message.replies.serializeToStream(repliesData);
                    state_messages.bindByteBuffer(13, repliesData);
                } else {
                    state_messages.bindNull(13);
                }
                if (message.reply_to != null) {
                    state_messages.bindInteger(14, message.reply_to.reply_to_top_id != 0 ? message.reply_to.reply_to_top_id : message.reply_to.reply_to_msg_id);
                } else {
                    state_messages.bindInteger(14, 0);
                }
                state_messages.bindLong(15, collectTime);
                state_messages.bindLong(16, message.dialog_id);
                state_messages.bindString(17, localFileName);
                state_messages.step();
                state_messages.dispose();
                return true;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return false;
    }

    //收藏状态
    public boolean collectStatus(int mid, long dialog_id) {
        ensureOpened();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT mid FROM message_collect WHERE mid=" + mid + " AND dialog_id=" + dialog_id);
            if (cursor.next()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return false;
    }

    //移除收藏
    public boolean removeCollectMessageByMessageId(int mid, long dialog_id) {
        ensureOpened();
        try {
            SQLitePreparedStatement statement = database.executeFast("DELETE FROM message_collect WHERE mid = ? AND dialog_id=" + dialog_id);
            statement.requery();
            statement.bindInteger(1, mid);
            statement.step();
            statement.dispose();
            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return false;
    }

    //收藏列表
    public List<KKMessage> loadCollectMessages() {
        ensureOpened();
        List<KKMessage> result = new ArrayList<>();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT data, mid, date, dialog_id, collect_time FROM message_collect ORDER BY collect_time DESC"));
            while (cursor.next()) {
                NativeByteBuffer data = cursor.byteBufferValue(0);
                if (data != null) {
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    message.readAttachPath(data, getUserConfig().clientUserId);
                    data.reuse();
                    message.id = cursor.intValue(1);
                    message.date = cursor.intValue(2);
                    message.dialog_id = cursor.longValue(3);
                    long collectTime = cursor.longValue(4);
                    result.add(new KKMessage(message, collectTime));
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }

    private int getMessageMediaType(TLRPC.Message message) {
        if (message instanceof TLRPC.TL_message_secret) {
            if ((message.media instanceof TLRPC.TL_messageMediaPhoto || MessageObject.isGifMessage(message)) && message.ttl > 0 && message.ttl <= 60 ||
                    MessageObject.isVoiceMessage(message) ||
                    MessageObject.isVideoMessage(message) ||
                    MessageObject.isRoundVideoMessage(message)) {
                return 1;
            } else if (message.media instanceof TLRPC.TL_messageMediaPhoto || MessageObject.isVideoMessage(message)) {
                return 0;
            }
        } else if (message instanceof TLRPC.TL_message && (message.media instanceof TLRPC.TL_messageMediaPhoto || message.media instanceof TLRPC.TL_messageMediaDocument) && message.media.ttl_seconds != 0) {
            return 1;
        } else if (message.media instanceof TLRPC.TL_messageMediaPhoto || MessageObject.isVideoMessage(message)) {
            return 0;
        }
        return -1;
    }


    //插入/更新联系人信息
    public boolean insertOrUpdateContact(long user_id, int read_message) {
        ensureOpened();
        try {
            if (queryContactRead(user_id) != -1) {
                Timber.d("insertOrUpdateContact--->" + user_id + " 存在");
                database.executeFast("UPDATE contacts_info SET read_message =" + read_message + " WHERE user_id =" + user_id).stepThis().dispose();
            } else {
                Timber.d("insertOrUpdateContact--->" + "插入 " + user_id);
                SQLitePreparedStatement state = database.executeFast("INSERT INTO contacts_info (user_id,read_message) VALUES (" +
                        "?, " +     //user_id
                        "? " +     //read_message
                        ") ");
                state.requery();
                state.bindLong(1, user_id);
                state.bindInteger(2, read_message);
                state.step();
                state.dispose();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //查询联系人是否无痕已读 -1：没设置过 0：未开启 1：已开启
    public int queryContactRead(long user_id) {
        ensureOpened();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT read_message FROM contacts_info WHERE user_id =" + user_id);
            while (cursor.next()) {
                return cursor.intValue(0);
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return -1;
    }

    //插入sticker包
    public boolean insertStickerSet(TLRPC.TL_messages_stickerSet sticker) {
        ensureOpened();
        long mid = sticker.set.id;
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT mid FROM gif_sticker WHERE mid=" + mid);
            if (cursor.next()) {
                Timber.d("mid:" + mid + "的sticker已经存在");
                return true;
            } else {
                long update_time = System.currentTimeMillis();
                Timber.d("插入sticker for mid:" + mid);
                NativeByteBuffer data = new NativeByteBuffer(sticker.getObjectSize());
                sticker.serializeToStream(data);
                SQLitePreparedStatement state_messages = database.executeFast("INSERT INTO gif_sticker (data,type,mid,date) VALUES(" +
                        "?, " +     //data
                        "?, " +     //type
                        "?, " +     //mid
                        "? " +     //date
                        ")");
                state_messages.requery();
                state_messages.bindByteBuffer(1, data);
                state_messages.bindInteger(2, 1);
                state_messages.bindLong(3, mid);
                state_messages.bindLong(4, update_time);
                state_messages.step();
                state_messages.dispose();
                return true;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return false;
    }

    //删除sticker
    public boolean deleteSticker(long id) {
        ensureOpened();
        try {
            database.executeFast("DELETE FROM gif_sticker WHERE mid=" + id).stepThis().dispose();
            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return false;
    }

    //查询sticker是否存在
    public boolean queryStickerExists(long id) {
        ensureOpened();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT mid FROM gif_sticker WHERE mid =" + id);
            while (cursor.next()) {
                return true;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return false;
    }

    //插入gif、sticker
    public boolean insertGifSticker(TLRPC.Document document, int type) {
        ensureOpened();
        long mid = document.id;
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT mid FROM gif_sticker WHERE mid=" + mid);
            if (cursor.next()) {
                Timber.d("mid:" + mid + "的gif_sticker已经存在");
                return true;
            } else {
                long update_time = System.currentTimeMillis();
                Timber.d("插入gif_sticker for mid:" + mid);
                NativeByteBuffer data = new NativeByteBuffer(document.getObjectSize());
                document.serializeToStream(data);
                SQLitePreparedStatement state_messages = database.executeFast("INSERT INTO gif_sticker (data,type,mid,date) VALUES(" +
                        "?, " +     //data
                        "?, " +     //type
                        "?, " +     //mid
                        "? " +     //date
                        ")");
                state_messages.requery();
                state_messages.bindByteBuffer(1, data);
                state_messages.bindInteger(2, type);
                state_messages.bindLong(3, mid);
                state_messages.bindLong(4, update_time);
                state_messages.step();
                state_messages.dispose();
                return true;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return false;
    }

    //查询sticker列表
    public List<GifStickerEntity> queryStickers() {
        ensureOpened();
        List<GifStickerEntity> result = new ArrayList<>();
        SQLiteCursor cursor = null;
        try {
            String sql = "SELECT data,type,mid,date FROM gif_sticker ORDER BY date DESC";
            cursor = database.queryFinalized(String.format(Locale.US, sql));
            while (cursor.next()) {
                GifStickerEntity entity = new GifStickerEntity();
                NativeByteBuffer data = cursor.byteBufferValue(0);
                int type = cursor.intValue(1);
                if (type == 1) {// sticker包
                    TLRPC.TL_messages_stickerSet stickerSet = TLRPC.TL_messages_stickerSet.TLdeserialize(data, data.readInt32(false), false);
                    entity.stickerSet = stickerSet;
                    data.reuse();
                } else {//gif sticker
                    TLRPC.Document document = TLRPC.Document.TLdeserialize(data, data.readInt32(false), false);
                    entity.data = document;
                    data.reuse();
                }
                entity.type = type;
                entity.mid = cursor.longValue(2);
                entity.date = cursor.longValue(3);
                result.add(entity);
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }

    public void addHideMessageId(int mid, int date) {
        ensureOpened();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT mid FROM message_filter WHERE mid=" + mid);
            if (cursor.next()) {
                TGLog.debug("has message_filter record for mid:" + mid);
            } else {
                TGLog.debug("insert message_filter for mid:" + mid);
                SQLitePreparedStatement state_messages = database.executeFast("INSERT INTO message_filter VALUES(" +
                        "?, " +     //mid
                        "? " +     //date
                        ")");
                state_messages.requery();
                state_messages.bindInteger(1, mid);
                state_messages.bindInteger(2, date);
                state_messages.step();
                state_messages.dispose();
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
    }

    public Set<Integer> loadHideMessageIds(List<Integer> ids, int from, int to) {
        ensureOpened();
        Set<Integer> result = new HashSet<>();
        if (ids == null || ids.size() == 0) {
            return result;
        }
        StringBuilder inStr = new StringBuilder("(");
        for (Integer id : ids) {
            inStr.append(id);
            inStr.append(",");
        }
        inStr.deleteCharAt(inStr.length() - 1);
        inStr.append(")");
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT mid, date " +
                    " FROM message_filter " +
                    " WHERE date >= " + from + " " +
                    " AND date <=" + to +
                    " AND mid in " + inStr));
            while (cursor.next()) {
                result.add(cursor.intValue(0));
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }

    //删除标签
    public boolean deleteTagById(int tag_id) {
        ensureOpened();
        try {
            SQLitePreparedStatement statement = database.executeFast("DELETE FROM channel_tag WHERE tag_id = ?");
            statement.requery();
            statement.bindInteger(1, tag_id);
            statement.step();
            statement.dispose();
            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return false;
    }

    //删除标签
    public boolean deleteTagWhenNoChannel() {
        ensureOpened();
        try {
            SQLitePreparedStatement statement = database.executeFast("DELETE FROM channel_tag WHERE tag_id NOT IN (SELECT tag_id FROM channel_with_tag GROUP BY tag_id)");
            statement.requery();
            statement.step();
            statement.dispose();
            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return false;
    }

    //创建标签
    public boolean createChannelTag(String tag_name) {
        ensureOpened();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT tag_id FROM channel_tag WHERE tag_name='" + tag_name + "'");
            if (cursor.next()) {
                TGLog.erro(tag_name + " 已经存在");
                return false;
            } else {
                TGLog.debug("插入标签 " + tag_name);
                long update_time = System.currentTimeMillis();
                SQLitePreparedStatement state = database.executeFast("INSERT INTO channel_tag (tag_name,update_time) VALUES (" +
                        "?, " +     //tag_name
                        "? " +     //update_time
                        ")");
                state.requery();
                state.bindString(1, tag_name);
                state.bindLong(2, update_time);
                state.step();
                state.dispose();
                return true;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return false;
    }

    //查询tag列表
    public List<ChannelTagEntity> queryChannelTagList() {
        ensureOpened();
        List<ChannelTagEntity> result = new ArrayList<>();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT tag_id,tag_name FROM channel_tag ORDER BY level ASC,update_time ASC"));
            while (cursor.next()) {
                ChannelTagEntity channelTagEntity = new ChannelTagEntity();
                channelTagEntity.tagId = cursor.intValue(0);
                channelTagEntity.tagName = cursor.stringValue(1);
                result.add(channelTagEntity);
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }


    //查询tag
    public ChannelTagEntity queryChannelTagByName(String name) {
        ensureOpened();
        List<ChannelTagEntity> result = new ArrayList<>();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT tag_id,tag_name FROM channel_tag WHERE tag_name='" + name + "'"));
            while (cursor.next()) {
                ChannelTagEntity channelTagEntity = new ChannelTagEntity();
                channelTagEntity.tagId = cursor.intValue(0);
                channelTagEntity.tagName = cursor.stringValue(1);
                result.add(channelTagEntity);
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        if (result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    //给channel打标签
    public boolean createChannelWithTag(long channel_id, int tag_id, String tag_name) {
        ensureOpened();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized("SELECT channel_id FROM channel_with_tag WHERE channel_id=" + channel_id + " AND tag_id=" + tag_id);
            if (cursor.next()) {
                TGLog.erro(channel_id + " 已经已经打过该标签 " + tag_name);
                return false;
            } else {
                TGLog.debug("给 " + channel_id + " 打便签 " + tag_name);
                long update_time = System.currentTimeMillis();
                SQLitePreparedStatement state = database.executeFast("INSERT INTO channel_with_tag (channel_id,tag_id,tag_name,update_time) VALUES (" +
                        "?, " +     //channel_id
                        "?, " +     //tag_id
                        "?, " +     //tag_name
                        "? " +     //update_time
                        ")");
                state.requery();
                state.bindLong(1, channel_id);
                state.bindInteger(2, tag_id);
                state.bindString(3, tag_name);
                state.bindLong(4, update_time);
                state.step();
                state.dispose();
                return true;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return false;
    }

    //通过tag_id获取channel
    public List<ChannelWithTagEntity> queryChannelByTagId(int tag_id) {
        ensureOpened();
        List<ChannelWithTagEntity> result = new ArrayList<>();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT channel_id,tag_id,tag_name FROM channel_with_tag WHERE tag_id=" + tag_id + " ORDER BY update_time ASC"));
            while (cursor.next()) {
                ChannelWithTagEntity channelWithTag = new ChannelWithTagEntity();
                channelWithTag.channelId = cursor.longValue(0);
                channelWithTag.tagId = cursor.intValue(1);
                channelWithTag.tagName = cursor.stringValue(2);
                result.add(channelWithTag);
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }

    //通过channelId获取标签
    public List<ChannelWithTagEntity> queryTagByChannelId(long channel_id) {
        ensureOpened();
        List<ChannelWithTagEntity> result = new ArrayList<>();
        SQLiteCursor cursor = null;
        try {
            cursor = database.queryFinalized(String.format(Locale.US, "SELECT channel_id,tag_id,tag_name FROM channel_with_tag WHERE channel_id=" + channel_id + " ORDER BY update_time ASC"));
            while (cursor.next()) {
                ChannelWithTagEntity channelWithTag = new ChannelWithTagEntity();
                channelWithTag.channelId = cursor.longValue(0);
                channelWithTag.tagId = cursor.intValue(1);
                channelWithTag.tagName = cursor.stringValue(2);
                result.add(channelWithTag);
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.dispose();
            }
        }
        return result;
    }

    //某个channel下自己删除
    public boolean deleteChannelWithTag(long channel_id, int tag_id) {
        ensureOpened();
        try {
            SQLitePreparedStatement statement = database.executeFast("DELETE FROM channel_with_tag WHERE tag_id = ? AND channel_id= ?");
            statement.requery();
            statement.bindInteger(1, tag_id);
            statement.bindLong(2, channel_id);
            statement.step();
            statement.dispose();
            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return false;
    }

    //直接删除了某个标签
    public boolean deleteChannelWithTagByTagId(int tag_id) {
        ensureOpened();
        try {
            SQLitePreparedStatement statement = database.executeFast("DELETE FROM channel_with_tag WHERE tag_id = ?");
            statement.requery();
            statement.bindInteger(1, tag_id);
            statement.step();
            statement.dispose();
            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return false;
    }

    //删除这个channel的所有标签
    public boolean deleteChannelWithTagByChannelId(long channel_id) {
        ensureOpened();
        try {
            SQLitePreparedStatement statement = database.executeFast("DELETE FROM channel_with_tag WHERE channel_id = ?");
            statement.requery();
            statement.bindLong(1, channel_id);
            statement.step();
            statement.dispose();
            return true;
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return false;
    }

    //排序
    public void updateChannelLevelById(int tagId, int level) {
        ensureOpened();
        try {
            database.executeFast("UPDATE channel_tag SET level=" + level + " WHERE tag_id=" + tagId).stepThis().dispose();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }
}
