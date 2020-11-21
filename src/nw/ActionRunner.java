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
	//setting the distribution to timeout the action runner randomly
	public static int exp_dis = 3000;
	
	//current triple store
	private Dataset current;
	
	//Query to get all actions posted by the LDFU Agent
	private Query get_action;
    
	//update request to update the current triple store
	private UpdateRequest update;
	
	//update the capacity of deliverySlot1 (used to assemble the Motherboard product)
	private UpdateRequest increase_capacity;

	
	//Request to delete the Actions with which were performed
	private UpdateRequest delete_action;
	
	 public static ExponentialDistribution sampler = null;

	 
	 
	public ActionRunner(Dataset current) {
		//setting the current dataset
		this.current = current;
		
		//setting the distribution to timeout the action runner randomly
		sampler = new ExponentialDistribution(exp_dis);
	
		//query for all actions with action status: ActiveActionStatus
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
	    
	  //query for updating the model from the get_action query
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
	    
	    //request to increase the slot capacity of the used products
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
	    sb.append("    ?slot arena:capacity ?capacityPlusOne .\n");
	    sb.append("}\n");
	    sb.append("WHERE {\n");
	    sb.append("    :delivery arena:hasSlot ?slot .\n");
	    sb.append("    ?slot arena:capacity ?capacity ;\n");
	    sb.append("        schema:model ?model .\n");
	    sb.append("    BIND(?capacity + 1 AS ?capacityPlusOne)\n");
	    sb.append("    FILTER(?capacity < 10)\n");
	    sb.append("}\n");
	    increase_capacity = UpdateFactory.create(sb.toString());
	    
	  //query to delete the Action with Active Action Status, which is posted into the knowledge graph by ldfu
	    sb = new StringBuilder();
	    sb.append("PREFIX : <" + Server.BASE_URI + "current#>\n");
	    sb.append("PREFIX arena: <http://paul.ti.rw.fau.de/~pi69geby/arena/>\n");
	    sb.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
	    sb.append("PREFIX schema: <http://schema.org/>\n");
	    sb.append("DELETE {\n");
	    sb.append("    ?action a arena:AssembleAction ;\n");
	    sb.append("      	schema:actionStatus schema:ActiveActionStatus .\n");
	    sb.append("}\n");
	    sb.append("WHERE {\n");
	    sb.append("      ?action a arena:AssembleAction ;\n");
	    sb.append("      	schema:actionStatus schema:ActiveActionStatus .\n");
	    sb.append("}\n");
	    delete_action = UpdateFactory.create(sb.toString());
	    
	    
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
			
		while(true) {
			//loop to get all the posted actions with the get_action query
			try(QueryExecution qexec = QueryExecutionFactory.create(get_action, current)) {
                ResultSet res = qexec.execSelect();
                if(res.hasNext()) {
                	//deleting the action with the delete_action query
               	 Txn.executeWrite(current, () -> {
                        UpdateAction.execute(delete_action, current);            
               });
                	//Update Request if there is an action in the ResultSet
                	 Txn.executeWrite(current, () -> {
                         UpdateAction.execute(update, current);
                        
                });
                	 Txn.executeWrite(current, () -> {
                         UpdateAction.execute(increase_capacity, current);
                        
                });

                	 nw.Test.stats[1] =  nw.Test.stats[1] + 1;
                	 
                	
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
