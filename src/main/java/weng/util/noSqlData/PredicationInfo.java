package weng.util.noSqlData;

public class PredicationInfo {
    private String sCui;
    private int sNovel;
    private String predicate;
    private String oCui;
    private int oNovel;
    private String pmid;
    private int year;
    private int pid;
    private int isExist;

    public PredicationInfo(String sCui, int sNovel, String predicate, String oCui, int oNovel,
                           String pmid, int year, int pid, int isExist) {
        this.sCui = sCui;
        this.sNovel = sNovel;
        this.predicate = predicate;
        this.oCui = oCui;
        this.oNovel = oNovel;
        this.pmid = pmid;
        this.year = year;
        this.pid = pid;
        this.isExist = isExist;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setIsExist(int isExist) {
        this.isExist = isExist;
    }

    public void setoCui(String oCui) {
        this.oCui = oCui;
    }

    public void setoNovel(int oNovel) {
        this.oNovel = oNovel;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public void setPmid(String pmid) {
        this.pmid = pmid;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public void setsCui(String sCui) {
        this.sCui = sCui;
    }

    public void setsNovel(int sNovel) {
        this.sNovel = sNovel;
    }

    public int getYear() {
        return year;
    }

    public int getIsExist() {
        return isExist;
    }

    public int getoNovel() {
        return oNovel;
    }

    public int getPid() {
        return pid;
    }

    public int getsNovel() {
        return sNovel;
    }

    public String getoCui() {
        return oCui;
    }

    public String getPmid() {
        return pmid;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getsCui() {
        return sCui;
    }
}
