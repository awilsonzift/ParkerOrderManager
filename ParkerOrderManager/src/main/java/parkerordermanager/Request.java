package parkerordermanager;

public class Request {
    String instance;

    public String getInstance() {
    	return this.instance;
    }
    public void setInstance(String instance) {
    	this.instance = instance;
    }
    public Request(String instance) {
        this.instance = instance;
    }

    public Request() {
    }
}
