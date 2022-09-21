package teleblock.model;

import java.io.Serializable;


public class ChannelFeedEntity implements Serializable {
    public String from;
    public int tagId;
    public long chatId = -1;

    public int position;
    public String title;

    public int tabIndex = -1;
}
