package weng.util;

public class ProgressBar {
    private char[] animationChars = new char[]{'|', '/', '-', '\\'};
    private int total;
    private int current = 0;
    public ProgressBar(int total){
        this.total = total;
    }
    public void update(){
        if (current >= total){
            System.out.println("Processing: Done!          ");
        }
        current += 1;
        System.out.print("Processing: " + current + "% " + this.animationChars[current % 4] + "\r");
    }
}
