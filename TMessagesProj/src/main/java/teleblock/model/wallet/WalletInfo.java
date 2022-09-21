package teleblock.model.wallet;

import android.text.TextUtils;

import java.util.List;

/**
 * 创建日期：2022/8/10
 * 描述：
 */
public class WalletInfo {

    public int id;
    public String tg_user_id;
    public String nft_contract;
    public String nft_contract_image;
    public String nft_token_id;
    public String nft_photo_id;
    public String nft_name;

    public long getTg_user_id() {
        if (TextUtils.isEmpty(tg_user_id)) {
            tg_user_id = "0";
        }
        return Long.parseLong(tg_user_id);
    }

    public long getNft_photo_id() {
        if (TextUtils.isEmpty(nft_photo_id)) {
            nft_photo_id = "0";
        }
        return Long.parseLong(nft_photo_id);
    }
}