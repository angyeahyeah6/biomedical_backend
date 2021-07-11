package weng.labelSys;

public class Nickname {
    String generalCui;
    String general;
    String cui;
    String name;

    public Nickname(String generalCui, String general, String cui, String name) {
        this.generalCui = generalCui;
        this.general = general;
        this.cui = cui;
        this.name = name;
    }

    public void setGeneral(String general) {
        this.general = general;
    }

    public void setGeneralCui(String generalCui) {
        this.generalCui = generalCui;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCui(String cui) {
        this.cui = cui;
    }

    public String getGeneral() {
        return general;
    }

    public String getGeneralCui() {
        return generalCui;
    }

    public String getName() {
        return name;
    }

    public String getCui() {
        return cui;
    }
}
