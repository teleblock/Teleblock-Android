package teleblock.event.data;


public class ThemePreviewEvent {
    public String path;
    public String name;

    public ThemePreviewEvent(String path, String name) {
        this.path = path;
        this.name = name;
    }
}
