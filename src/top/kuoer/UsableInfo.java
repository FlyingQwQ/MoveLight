package top.kuoer;

public class UsableInfo {

    private String name;
    private int lightLevel;
    private boolean apparel;

    public UsableInfo(String name, int lightLevel, boolean apparel) {
        this.name = name;
        this.lightLevel = lightLevel;
        this.apparel = apparel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public void setLightLevel(int lightLevel) {
        this.lightLevel = lightLevel;
    }

    public boolean isApparel() {
        return apparel;
    }

    public void setApparel(boolean apparel) {
        this.apparel = apparel;
    }

}
