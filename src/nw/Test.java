package nw;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.kit.aifb.datafu.parser.notation3.ParseException;


public class Test {
	
	// array for the statistics (Delivered Items/Assembled Motherboards/ Shipped Motherboards)
	public static int[] stats = new int[3];
	
	//Runtime of the program
	public static int runtime = 10000;
	
	
	public static void main(String[] args) throws MalformedURLException, SAXException, IOException, ParserConfigurationException, ParseException{
		// TODO Auto-generated method stub
		
		//ExecutorService for the LDFU Agent
		ExecutorService es = Executors.newCachedThreadPool();
		
		//Read in the Knowledge Graph (File) and start the server
		Server server = new Server(new File("arena2036.ttl"));
		server.start();
		
		try {
			//trying to read in the agent file and start the LDFU Agent
		    es.submit(new Agent(new File("ag.n3")));
		    es.shutdown();
		   
		    TimeUnit.MILLISECONDS.sleep(runtime);
        } catch (Exception e){
        	//get the current status of the knowledge graph and shutting down the server
        	server.dumpResults();
        	server.close();
        	System.out.println("Delivered Items: " + stats[0]);
        	System.out.println("Assembled Motherboards: " + stats[1]);
        	System.out.println("Shipped Motherboards: " + stats[2]);
        	System.exit(0);
        	
        } finally {
        //get the current status of the knowledge graph and shutting down the server
		server.dumpResults();
		server.close();
		System.out.println("Delivered Items: " + stats[0]);
    	System.out.println("Assembled Motherboards: " + stats[1]);
    	System.out.println("Shipped Motherboards: " + stats[2]);
		System.exit(0);
        }
	}

}
