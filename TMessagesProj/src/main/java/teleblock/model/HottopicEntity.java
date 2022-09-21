package teleblock.model;


public class HottopicEntity {

    private int id;
    private String name;
    private String color;//后台返回的颜色值，暂时用不到
    private boolean selectorStatus;//选中状态，自己添加的

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean getSelectorStatus() {
        return selectorStatus;
    }

    public void setSelectorStatus(boolean selectorStatus) {
        this.selectorStatus = selectorStatus;
    }
}
