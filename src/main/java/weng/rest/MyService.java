//package weng.rest;
//
//import com.google.gson.Gson;
//import org.jboss.resteasy.annotations.Form;
//import weng.form.MyForm;
//import weng.form.ParamForm;
//import weng.form.ResultForm;
//import weng.labelSys.Predict;
//
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.core.Response;
//
//@Path("/myService")
//public class MyService {
//    @GET
//    @Path("/test")
//    public Response myFunc() {
//        return Response.status(200).entity(new Gson().toJson("test")).build();
//    }
//
//    @GET
//    @Path("/search")
//    public Response search(@Form ParamForm param) {
//        ResultForm result = doSomething(param);
//        return Response.status(200).entity(new Gson().toJson(result)).build();
//    }
//
//    public ResultForm doSomething(ParamForm f) {
//        // TODO: debug
//        System.out.println("doSomething params:" + f.getParams());
//
//        MyForm myForm = new Gson().fromJson(f.getParams(), MyForm.class);
//
//        // TODO: debug
//        System.out.println("doSomething A: " + myForm.getDrug());
//        System.out.println("doSomething B: " + myForm.getEndYear());
//        System.out.println("doSomething C: " + myForm.getClassifierType());
//
//        int topK = 30;
//        Predict predict = new Predict(
//                myForm.getDrug(),
//                myForm.getEndYear(),
//                topK,
//                myForm.getClassifierType());
//        predict.run();
//
//        return new ResultForm(myForm.getDrug() + myForm.getEndYear() + myForm.getClassifierType());
//    }
//}
