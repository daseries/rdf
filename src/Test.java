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

	public static void main(String[] args) throws MalformedURLException, SAXException, IOException, ParserConfigurationException, ParseException{
		// TODO Auto-generated method stub
		
		ExecutorService es = Executors.newCachedThreadPool();
		
		//read in the file
		Server server = new Server(new File("arena2036.ttl"));
		server.start();
		
		
		try {
		//try to start the server

		    es.submit(new Agent(new File("ag.n3")));
		    
		    es.shutdown();
		    
		    TimeUnit.MILLISECONDS.sleep(1000);
        } catch (Exception e){
        	server.dumpResults();
        	server.close();
        	
        } finally {
		
		server.dumpResults();
		server.close();
        }
	}

}
