package parkerordermanager;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class ParkerOrderHandlerTest {

    private static Request request;

    @BeforeClass
    public static void createInput() throws IOException {
        // TODO: set up your sample input object here.
        request = null;
    }

    private Context createContext() {
        TestContext ctx = new TestContext();

        // TODO: customize your context here if needed.
        ctx.setFunctionName("ParkerOrderManager");

        return ctx;
    }

    @Test
    public void testParkerOrderHandler() {
        ParkerOrderHandler handler = new ParkerOrderHandler();
        Context ctx = createContext();

        Request request = new Request();
        request.setInstance("local-staging-test");
        //request.setInstance("local-production");
        Response response = handler.handleRequest(request, ctx);

        // TODO: validate output here if needed.
        if (response != null) {
            System.out.println(response.toString());
        }
    }
}
