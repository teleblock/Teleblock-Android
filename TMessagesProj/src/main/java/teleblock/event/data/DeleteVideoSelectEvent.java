package teleblock.event.data;


public class DeleteVideoSelectEvent {
    public int num;
    public boolean selectAll;

    public DeleteVideoSelectEvent(int num, boolean selectAll) {
        this.num = num;
        this.selectAll = selectAll;
    }
}
