package website.api;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import website.api.inputClass.InDetailPredicate;
import website.api.inputClass.InEval;
import website.api.inputClass.InPredicate;
import weng.labelSys.Predict;
import weng.util.Utils;
import weng.util.file.RepurposingEval;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@CrossOrigin("http://localhost:8081")
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ToraController {
    @PostMapping("/get_predicate")
    public Map getPredictRank(@RequestBody InPredicate getPredicate) throws SQLException {
        int topK = 30;
        String drug = getPredicate.drugName;
        int endYear = getPredicate.endYear;
        int classifierType = getPredicate.classifierType;
        Predict predict = new Predict(
                drug,
                endYear,
                topK,
                classifierType);
//        predict.run();
        return  predict.getCompletePath(drug, endYear);
    }
    @GetMapping("/all_drug")
    public List<String> getAllDrug() {
        List<String> drugsAll = new Utils().readLineFile(RepurposingEval.focalDrugs);
        return drugsAll;
    }
    @PostMapping("/get_eval")
    public Map<String, Map<String, Object>> getRankDisease(@RequestBody InEval getEval) throws SQLException {
        String drug = getEval.drugName;
        int endYear = getEval.endYear;
        int classifierType = getEval.classifierType;
        int topK = 30;
        Predict predict = new Predict(
                drug,
                endYear,
                topK,
                classifierType);
        return predict.getOrderTarget(drug, endYear);
    }
    @PostMapping("/get_detail_predicate")
    public List<List<Object>> getPathDetail(@RequestBody InDetailPredicate inDetailPredicate) throws SQLException {
        String drug = inDetailPredicate.drugName;
        int endYear = inDetailPredicate.endYear;
        int classifierType = inDetailPredicate.classifierType;
        String disease = inDetailPredicate.c_name;
        int topK = 30;
        Predict predict = new Predict(
                drug,
                endYear,
                topK,
                classifierType);
        return predict.getDetailPath(drug, endYear, disease);
    }

}

