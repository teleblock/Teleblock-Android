package teleblock.model;


public class BaseModel {
    public static final int NORMAL = 0;
    public static final int AD = 1;


    public boolean needShowAd;
    public boolean adClosed; // 广告被关闭
    public boolean adLoaded; // 广告已加载
    public int adStyle = -1;
    public int modelType = NORMAL;

    public int getModelType() {
        return NORMAL;
    }
}
