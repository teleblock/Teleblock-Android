package teleblock.event.data;


public class DeleteVideoItemEvent {
    public String filePath;

    public DeleteVideoItemEvent(String filePath) {
        this.filePath = filePath;
    }
}
