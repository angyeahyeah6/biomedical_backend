package weng.util.predict;

public class Intermediate {
    private int id;
    private boolean important;
    private String name;
    private String cui;
    public Intermediate(int id, boolean important, String cui, String name){
        this.id = id;
        this.important = important;
        this.cui= cui;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public boolean getImportant() {
        return important;
    }

    public String getName() {
        return name;
    }
}