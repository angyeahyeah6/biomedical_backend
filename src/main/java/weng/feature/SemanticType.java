package weng.feature;

import java.util.HashMap;

public class SemanticType {

    public SemanticType() {
    }

    /**
     * Semantic types got from: "SELECT DISTINCT `SEMGROUP` FROM `semanticGroup`;"
     */
    public HashMap<String, Integer> initSemGroupAppearedList() {
        HashMap<String, Integer> semGroupAppearedList = new HashMap<>();
        semGroupAppearedList.put("ACTI", 0);
        semGroupAppearedList.put("ANAT", 0);
        semGroupAppearedList.put("CHEM", 0);
        semGroupAppearedList.put("CONC", 0);
        semGroupAppearedList.put("DEVI", 0);
        semGroupAppearedList.put("DISO", 0);
        semGroupAppearedList.put("GENE", 0);
        semGroupAppearedList.put("GEOG", 0);
        semGroupAppearedList.put("LIVB", 0);
        semGroupAppearedList.put("OBJC", 0);
        semGroupAppearedList.put("OCCU", 0);
        semGroupAppearedList.put("ORGA", 0);
        semGroupAppearedList.put("PHEN", 0);
        semGroupAppearedList.put("PHYS", 0);
        semGroupAppearedList.put("PROC", 0);
        return semGroupAppearedList;
    }

    /**
     * Semantic types got from: "SELECT DISTINCT `SEMTYPE` FROM `semanticType`;"
     */
    public HashMap<String, Integer> initSemTypeAppearedList() {
        HashMap<String, Integer> semTypeAppearedList = new HashMap<>();
        semTypeAppearedList.put("aapp", 0);
        semTypeAppearedList.put("acab", 0);
        semTypeAppearedList.put("acty", 0);
        semTypeAppearedList.put("aggp", 0);
        semTypeAppearedList.put("alga", 0);
        semTypeAppearedList.put("amas", 0);
        semTypeAppearedList.put("amph", 0);
        semTypeAppearedList.put("anab", 0);
        semTypeAppearedList.put("anim", 0);
        semTypeAppearedList.put("anst", 0);
        semTypeAppearedList.put("antb", 0);
        semTypeAppearedList.put("arch", 0);
        semTypeAppearedList.put("bacs", 0);
        semTypeAppearedList.put("bact", 0);
        semTypeAppearedList.put("bdsu", 0);
        semTypeAppearedList.put("bdsy", 0);
        semTypeAppearedList.put("bhvr", 0);
        semTypeAppearedList.put("biof", 0);
        semTypeAppearedList.put("bird", 0);
        semTypeAppearedList.put("blor", 0);
        semTypeAppearedList.put("bmod", 0);
        semTypeAppearedList.put("bodm", 0);
        semTypeAppearedList.put("bpoc", 0);
        semTypeAppearedList.put("bsoj", 0);
        semTypeAppearedList.put("carb", 0);
        semTypeAppearedList.put("celc", 0);
        semTypeAppearedList.put("celf", 0);
        semTypeAppearedList.put("cell", 0);
        semTypeAppearedList.put("cgab", 0);
        semTypeAppearedList.put("chem", 0);
        semTypeAppearedList.put("chvf", 0);
        semTypeAppearedList.put("chvs", 0);
        semTypeAppearedList.put("clas", 0);
        semTypeAppearedList.put("clna", 0);
        semTypeAppearedList.put("clnd", 0);
        semTypeAppearedList.put("cnce", 0);
        semTypeAppearedList.put("comd", 0);
        semTypeAppearedList.put("crbs", 0);
        semTypeAppearedList.put("diap", 0);
        semTypeAppearedList.put("dora", 0);
        semTypeAppearedList.put("drdd", 0);
        semTypeAppearedList.put("dsyn", 0);
        semTypeAppearedList.put("edac", 0);
        semTypeAppearedList.put("eehu", 0);
        semTypeAppearedList.put("eico", 0);
        semTypeAppearedList.put("elii", 0);
        semTypeAppearedList.put("emod", 0);
        semTypeAppearedList.put("emst", 0);
        semTypeAppearedList.put("enty", 0);
        semTypeAppearedList.put("enzy", 0);
        semTypeAppearedList.put("euka", 0);
        semTypeAppearedList.put("evnt", 0);
        semTypeAppearedList.put("famg", 0);
        semTypeAppearedList.put("ffas", 0);
        semTypeAppearedList.put("fish", 0);
        semTypeAppearedList.put("fndg", 0);
        semTypeAppearedList.put("fngs", 0);
        semTypeAppearedList.put("food", 0);
        semTypeAppearedList.put("ftcn", 0);
        semTypeAppearedList.put("genf", 0);
        semTypeAppearedList.put("geoa", 0);
        semTypeAppearedList.put("gngm", 0);
        semTypeAppearedList.put("gora", 0);
        semTypeAppearedList.put("grpa", 0);
        semTypeAppearedList.put("grup", 0);
        semTypeAppearedList.put("hcpp", 0);
        semTypeAppearedList.put("hcro", 0);
        semTypeAppearedList.put("hlca", 0);
        semTypeAppearedList.put("hops", 0);
        semTypeAppearedList.put("horm", 0);
        semTypeAppearedList.put("humn", 0);
        semTypeAppearedList.put("idcn", 0);
        semTypeAppearedList.put("imft", 0);
        semTypeAppearedList.put("inbe", 0);
        semTypeAppearedList.put("inch", 0);
        semTypeAppearedList.put("inpo", 0);
        semTypeAppearedList.put("inpr", 0);
        semTypeAppearedList.put("invt", 0);
        semTypeAppearedList.put("irda", 0);
        semTypeAppearedList.put("lang", 0);
        semTypeAppearedList.put("lbpr", 0);
        semTypeAppearedList.put("lbtr", 0);
        semTypeAppearedList.put("lipd", 0);
        semTypeAppearedList.put("mamm", 0);
        semTypeAppearedList.put("mbrt", 0);
        semTypeAppearedList.put("mcha", 0);
        semTypeAppearedList.put("medd", 0);
        semTypeAppearedList.put("menp", 0);
        semTypeAppearedList.put("mnob", 0);
        semTypeAppearedList.put("mobd", 0);
        semTypeAppearedList.put("moft", 0);
        semTypeAppearedList.put("mosq", 0);
        semTypeAppearedList.put("neop", 0);
        semTypeAppearedList.put("nnon", 0);
        semTypeAppearedList.put("npop", 0);
        semTypeAppearedList.put("nsba", 0);
        semTypeAppearedList.put("nusq", 0);
        semTypeAppearedList.put("ocac", 0);
        semTypeAppearedList.put("ocdi", 0);
        semTypeAppearedList.put("opco", 0);
        semTypeAppearedList.put("orch", 0);
        semTypeAppearedList.put("orga", 0);
        semTypeAppearedList.put("orgf", 0);
        semTypeAppearedList.put("orgm", 0);
        semTypeAppearedList.put("orgt", 0);
        semTypeAppearedList.put("ortf", 0);
        semTypeAppearedList.put("patf", 0);
        semTypeAppearedList.put("phob", 0);
        semTypeAppearedList.put("phpr", 0);
        semTypeAppearedList.put("phsf", 0);
        semTypeAppearedList.put("phsu", 0);
        semTypeAppearedList.put("plnt", 0);
        semTypeAppearedList.put("podg", 0);
        semTypeAppearedList.put("popg", 0);
        semTypeAppearedList.put("prog", 0);
        semTypeAppearedList.put("pros", 0);
        semTypeAppearedList.put("qlco", 0);
        semTypeAppearedList.put("qnco", 0);
        semTypeAppearedList.put("rcpt", 0);
        semTypeAppearedList.put("rept", 0);
        semTypeAppearedList.put("resa", 0);
        semTypeAppearedList.put("resd", 0);
        semTypeAppearedList.put("rich", 0);
        semTypeAppearedList.put("rnlw", 0);
        semTypeAppearedList.put("sbst", 0);
        semTypeAppearedList.put("shro", 0);
        semTypeAppearedList.put("socb", 0);
        semTypeAppearedList.put("sosy", 0);
        semTypeAppearedList.put("spco", 0);
        semTypeAppearedList.put("strd", 0);
        semTypeAppearedList.put("tisu", 0);
        semTypeAppearedList.put("tmco", 0);
        semTypeAppearedList.put("topp", 0);
        semTypeAppearedList.put("virs", 0);
        semTypeAppearedList.put("vita", 0);
        semTypeAppearedList.put("vtbt", 0);
        return semTypeAppearedList;
    }
}
