package teleblock.network.api;


import com.hjq.http.annotation.HttpIgnore;
import com.hjq.http.config.IRequestApi;

/**
 * 更新用户的NFT数据
 */
public class NftInfoApi implements IRequestApi {

    @HttpIgnore
    public String nft_path;

    private String nft_contract;
    private String nft_contract_image;
    private String nft_token_id;
    private String nft_photo_id;
    private String nft_name;

    @Override
    public String getApi() {
        return "/user/nftInfo";
    }

    public NftInfoApi setNft_contract(String nft_contract) {
        this.nft_contract = nft_contract;
        return this;
    }

    public NftInfoApi setNft_contract_image(String nft_contract_image) {
        this.nft_contract_image = nft_contract_image;
        return this;
    }

    public NftInfoApi setNft_token_id(String nft_token_id) {
        this.nft_token_id = nft_token_id;
        return this;
    }

    public NftInfoApi setNft_photo_id(String nft_photo_id) {
        this.nft_photo_id = nft_photo_id;
        return this;
    }

    public NftInfoApi setNft_path(String nft_path) {
        this.nft_path = nft_path;
        return this;
    }

    public NftInfoApi setNft_name(String nft_name) {
        this.nft_name = nft_name;
        return this;
    }
}
