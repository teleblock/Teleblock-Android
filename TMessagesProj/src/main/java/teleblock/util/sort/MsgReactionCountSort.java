package teleblock.util.sort;


public class MsgReactionCountSort implements Comparable<MsgReactionCountSort> {

    private boolean chosen;
    private String reaction;
    private int count;

    public MsgReactionCountSort(boolean chosen, String reaction, int count) {
        this.chosen = chosen;
        this.reaction = reaction;
        this.count = count;
    }

    public boolean isChosen() {
        return chosen;
    }

    public String getReaction() {
        return reaction;
    }

    public int getCount() {
        return count;
    }

    @Override
    public int compareTo(MsgReactionCountSort m) {
        return Integer.compare(this.count, m.count);
    }


}
