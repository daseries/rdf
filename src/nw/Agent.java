package nw;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

import edu.kit.aifb.datafu.Program;
import edu.kit.aifb.datafu.Request;
import edu.kit.aifb.datafu.engine.EvaluateProgram;
import edu.kit.aifb.datafu.io.origins.FileOrigin;
import edu.kit.aifb.datafu.io.origins.RequestOrigin;
import edu.kit.aifb.datafu.parser.ProgramConsumerImpl;
import edu.kit.aifb.datafu.parser.notation3.Notation3Parser;
import edu.kit.aifb.datafu.parser.notation3.ParseException;
import edu.kit.aifb.datafu.planning.EvaluateProgramConfig;
import edu.kit.aifb.datafu.planning.EvaluateProgramGenerator;
import edu.kit.aifb.datafu.planning.EvaluateProgramConfig.ThreadingModel;
import edu.kit.aifb.datafu.utils.Config.Distinct;
import edu.kit.aifb.datafu.utils.Config.QueueStrategy;

public class Agent implements Runnable{
	 // The program of this agent
    private Program program;
    
    //Time the Agent sleeps between choosing requests from the queue
    public static int sleeptime = 1000;

    /**
     * Constructor for an agent
     * 
     * Loads the program for an ldfu instance
     * @param programFile The program file to load
     * @throws FileNotFoundException
     * @throws ParseException
     */
    public Agent(File programFile) throws FileNotFoundException, ParseException {
        try {
            // Declaring program file and server as inputs for the LDFU
        	
            RequestOrigin io = new RequestOrigin(new URI("" + Server.BASE_URI + "current"), Request.Method.GET);
            FileOrigin po = new FileOrigin(programFile, FileOrigin.Mode.READ, null);
                   
            // Parsing rules from the program
            ProgramConsumerImpl pc = new ProgramConsumerImpl(po);
            Notation3Parser parser = new Notation3Parser(new FileInputStream(programFile));
            parser.parse(pc, po);
            
            // Putting it all together
            program = pc.getProgram(po);
            program.addInputOrigin(io);

        } catch (URISyntaxException e) {
            System.err.println("Agent got invalid SERVER.BASE_URI!");
            e.printStackTrace();
            System.exit(1);
        }
    }

	/**
     * Running an ldfu instance with the according program in a loop to execute the agent
     */
    @Override
    public void run() {
        // Configure LDFU to send only one request per run and choose this request randomly from the queue
        EvaluateProgramConfig config = new EvaluateProgramConfig();
        config.setOutputOriginQueueDistinct(Distinct.ON);
        config.setOutputOriginQueueStrategy(QueueStrategy.RANDOM);
        config.setThreadingModel(ThreadingModel.SERIAL);

        // Start LDFU
        EvaluateProgramGenerator engine = new EvaluateProgramGenerator(program, config);
        EvaluateProgram ep = engine.getEvaluateProgram();
        ep.start();
       
        try {
            while (true) {
                ep.awaitIdleAndReset(sleeptime);
            }
        } catch (InterruptedException e) {
            System.err.println("Agent " + this + " got interrupted!");
        }
    }
}
