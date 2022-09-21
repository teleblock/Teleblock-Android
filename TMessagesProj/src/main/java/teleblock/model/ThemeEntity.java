package teleblock.model;

import java.util.List;

/**
 * Created by LSD on 2021/9/8.
 * Desc
 */
public class ThemeEntity {
    public int perpage;
    public int page;
    public int total;
    public List<ItemEntity> item;

    public static class ItemEntity {
        public int id;
        public String title;
        public String avatar;
        public String url;//主题URL
        public int used;

        public int avatarId;
        public int type;//0：服务器配置普通主题,1：本地

        public int getType() {
            return type;
        }
    }
}
