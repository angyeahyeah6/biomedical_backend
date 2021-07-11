package weng.util.noSqlData;

public class NeighborInfo {
    private String cui;
    private int year;
    private String neighbor;
    private int freq;

    public NeighborInfo(String cui, int year, String neighbor, int freq) {
        this.cui = cui;
        this.year = year;
        this.neighbor = neighbor;
        this.freq = freq;
    }

    public void setCui(String cui) {
        this.cui = cui;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setNeighbor(String neighbor) {
        this.neighbor = neighbor;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }

    public String getCui() {
        return cui;
    }

    public int getYear() {
        return year;
    }

    public String getNeighbor() {
        return neighbor;
    }

    public int getFreq() {
        return freq;
    }
}
