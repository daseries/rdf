import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;


public class Server {
	
	private FusekiServer fuseki;
	   
    public static String BASE_URI = "http://localhost:2000/";
    
    private Dataset current;

	private File initModel;
    
    public Server(File initModel) {
    	this.initModel = initModel;
    }

    //start the server
    public void start() {
        ARQ.init();
        current = DatasetFactory.createTxnMem();
        
        Model model = current.getDefaultModel();
        model.read(initModel.toURI().toString(), "TURTLE");
        
        
        
        //start the spawner, to fill the capacities
        Spawner spawner = new Spawner(current);
        spawner.fillDelivery();
        new Thread(spawner).start();
        
        ActionRunner act = new ActionRunner(current);
        new Thread(act).start();
        
        
        int port = 80;
        Pattern portPattern = Pattern.compile("https?://.*:([0-9]+)/");
        Matcher portMatcher = portPattern.matcher(BASE_URI); 
        if(portMatcher.find()) {
            port = Integer.parseInt(portMatcher.group(1));
        }
        
        // Create and start Fuseki server
        fuseki = FusekiServer.create().enableStats(true).enablePing(true).port(port).verbose(true)
                .add("current", current).staticFileBase("static").build();
        fuseki.start();
        System.out.println("Started server at " + BASE_URI);
        
    }
    
    //stop the running server
    public void close() {
        fuseki.stop();
        System.out.println("Server stopped");
    }
	
    
    
    
    public void dumpResults() {
        String initialModelPath = initModel.getAbsolutePath();
        File currentDump = new File(initialModelPath.replace(".ttl", "-results") + "/current.ttl");
        try {
            RDFDataMgr.write(new FileOutputStream(currentDump), current, Lang.TRIG);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Dumped results");
    }
}
