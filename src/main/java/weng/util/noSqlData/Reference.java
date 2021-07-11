package weng.util.noSqlData;

public class Reference {
    private int id;
    private String cui;

    public Reference(int id, String cui) {
        this.id = id;
        this.cui = cui;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCui(String cui) {
        this.cui = cui;
    }

    public int getId() {
        return id;
    }

    public String getCui() {
        return cui;
    }
}
