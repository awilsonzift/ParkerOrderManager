package parkerordermanager;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import parkerordermanager.ParkerOrderManager;

public class ParkerOrderHandler implements RequestHandler<Request, Response> {

    public Response handleRequest(Request request, Context context) {
        
    	ParkerOrderContext.RUN_TIME rt = ParkerOrderContext.RUN_TIME.STAGING_TEST;
    	if (request.getInstance().toLowerCase().equals("local-staging-test")) 
    		rt = ParkerOrderContext.RUN_TIME.LOCAL_STAGING_TEST;
    	else if (request.getInstance().toLowerCase().equals("local-production"))
    		rt = ParkerOrderContext.RUN_TIME.LOCAL_PRODUCTION;
    	else if (request.getInstance().toLowerCase().equals("production"))
    		rt = ParkerOrderContext.RUN_TIME.PRODUCTION;
    	else if (request.getInstance().toLowerCase().equals("staging"))
    		rt = ParkerOrderContext.RUN_TIME.STAGING;
    	
    	ParkerOrderContext poContext = new ParkerOrderContext(rt, context);
    	
    	ParkerOrderManager poMgr = new ParkerOrderManager(poContext);
    	poMgr.processFilesFromSFTP();
    	
    	String result = "Done with " + request.getInstance();
        return new Response(result);
    }

}
