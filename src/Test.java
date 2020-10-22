import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import edu.kit.aifb.datafu.parser.notation3.ParseException;


public class Test {

	public static void main(String[] args) throws MalformedURLException, SAXException, IOException, ParserConfigurationException, ParseException{
		// TODO Auto-generated method stub
		
		//read in the file and start a server with it
		Server server = new Server(new File("arena2036.ttl"));
		server.start();
		
		//Agent ass = new Agent(new File("agent.n3"));
		//ass.run();
	
		server.dumpResults();
		server.close();
	}

}
