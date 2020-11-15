package nw;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.system.Txn;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;


public class ActionRunner implements Runnable{
	
	private Dataset current;
	 // Action this action runner simulates
	private Query get_action;
    // Query that checks for conflicting actions
	private UpdateRequest update;
	// Delete the ActiveAction from the Triple Store 
	private UpdateRequest delete_action;
	
	 public static ExponentialDistribution sampler = null;

	 
	 
	public ActionRunner(Dataset current) {
		
		this.current = current;
		
		sampler = new ExponentialDistribution(2000);
	
	//query for all the delivery models with a slot capacity greater than 0
		StringBuilder sb = new StringBuilder();
	    sb.append("PREFIX : <" + Server.BASE_URI + "current#>\n");
	    sb.append("PREFIX arena: <http://paul.ti.rw.fau.de/~pi69geby/arena/>\n");
	    sb.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
	    sb.append("PREFIX schema: <http://schema.org/>\n");
	    sb.append("SELECT ?action\n");
	    sb.append("WHERE {\n");
	    sb.append("        ?action schema:actionStatus schema:ActiveActionStatus .\n");
	    sb.append("}\n");
	    get_action = QueryFactory.create(sb.toString());
	    
	    sb = new StringBuilder();
	    sb.append("PREFIX : <" + Server.BASE_URI + "current#>\n");
	    sb.append("PREFIX arena: <http://paul.ti.rw.fau.de/~pi69geby/arena/>\n");
	    sb.append("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n");
	    sb.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
	    sb.append("PREFIX schema: <http://schema.org/>\n");
	    sb.append("DELETE {\n");
	    sb.append("    ?slot arena:capacity ?capacity .\n");
	    sb.append("}\n");
	    sb.append("INSERT {\n");
	    sb.append("    ?slot arena:capacity ?capacityMinusOne .\n");
	    sb.append("}\n");
	    sb.append("WHERE {\n");
	    sb.append("    :shipping a schema:Place ;\n");
	    sb.append("     	arena:hasSlot ?slot .\n");
	    sb.append("    ?slot arena:capacity ?capacity .\n");
	    sb.append("    BIND(?capacity - 1 AS ?capacityMinusOne)\n");
	    sb.append("    FILTER(?capacity > 0)\n");
	    sb.append("}\n");	    
	    update = UpdateFactory.create(sb.toString());
	    
	    sb = new StringBuilder();
	    sb.append("PREFIX : <" + Server.BASE_URI + "current#>\n");
	    sb.append("PREFIX arena: <http://paul.ti.rw.fau.de/~pi69geby/arena/>\n");
	    sb.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
	    sb.append("PREFIX schema: <http://schema.org/>\n");
	    sb.append("DELETE {\n");
	    sb.append("    ?action a arena:AssembleAction .\n");
	    sb.append("}\n");
	    sb.append("WHERE {\n");
	    sb.append("      ?action schema:actionStatus schema:ActiveActionStatus .\n");
	    sb.append("}\n");
	    delete_action = UpdateFactory.create(sb.toString());
	    
	    
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true) {
			try(QueryExecution qexec = QueryExecutionFactory.create(get_action, current)) {
                ResultSet res = qexec.execSelect();
                if(res != null) {
                	 Txn.executeWrite(current, () -> {
                         UpdateAction.execute(update, current);
                });
                	 Txn.executeWrite(current, () -> {
                         UpdateAction.execute(delete_action, current);
                });
                }
                
            }

            long timeout = (long) sampler.sample();
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
		
	}
}
