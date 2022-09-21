package teleblock.event.data;


public class CollectChangeEvent {
    public boolean collect;
    public int messageId;

    public CollectChangeEvent(int messageId, boolean collect) {
        this.messageId = messageId;
        this.collect = collect;
    }
}
