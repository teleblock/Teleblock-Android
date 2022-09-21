package teleblock.network.api;


import com.hjq.http.config.IRequestApi;

/**
 * 根据钱包获取NFT相关数据
 */
public class WalletInfoApi implements IRequestApi {

    private String wallet_type;
    private String wallet_address;

    @Override
    public String getApi() {
        return "/user/wallet/info";
    }

    public WalletInfoApi setWallet_type(String wallet_type) {
        this.wallet_type = wallet_type;
        return this;
    }

    public WalletInfoApi setWallet_address(String wallet_address) {
        this.wallet_address = wallet_address;
        return this;
    }
}
