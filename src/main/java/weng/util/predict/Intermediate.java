package weng.util.predict;

public class Intermediate {
    private int id;
    private int important;
    private String name;
    private String cui;
    public Intermediate(int id, int important, String cui, String name){
        this.id = id;
        this.important = important;
        this.cui= cui;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public int getImportant() {
        return important;
    }

    public String getName() {
        return name;
    }
}