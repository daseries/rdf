package nw;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.system.Txn;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.commons.math3.distribution.ExponentialDistribution;

public class Spawner  implements Runnable{
	// Spawns products at the given rate (Exponential Distribution)
	public static int exp_dis = 1000;

	// Reference to the current triple store
    private Dataset current;
    
    //Query to get the product models that should be delivered
    private Query query_model;

    // Request the current triple store to add the product
    UpdateRequest request_current;
    
	// Exponential distribution to get the duration of this specific action simulation
    public static ExponentialDistribution sampler = null;

    public Spawner(Dataset current) {
    
    	
    sampler = new ExponentialDistribution(exp_dis); 	
    	
  //initialize with current Dataset
    this.current = current;
    
    //query for all the delivery models with a slot capacity greater than 0
	StringBuilder sb = new StringBuilder();
    sb.append("PREFIX : <" + Server.BASE_URI + "current#>\n");
    sb.append("PREFIX arena: <http://paul.ti.rw.fau.de/~pi69geby/arena/>\n");
    sb.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
    sb.append("PREFIX schema: <http://schema.org/>\n");
    sb.append("SELECT ?model\n");
    sb.append("WHERE {\n");
    sb.append("    :delivery arena:hasSlot ?slot .\n");
    sb.append("    ?slot arena:capacity ?capacity ;\n");
    sb.append("        schema:model ?model .\n");
    sb.append("    FILTER(?capacity > 0)\n");
    sb.append("}\n");
    query_model = QueryFactory.create(sb.toString());
    
    //setting all capacities greater than 0 to capacity-1
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
    sb.append("    :delivery arena:hasSlot ?slot .\n");
    sb.append("    ?slot arena:capacity ?capacity ;\n");
    sb.append("        schema:model ?model .\n");
    sb.append("    BIND(?capacity - 1 AS ?capacityMinusOne)\n");
    sb.append("    FILTER(?capacity > 0)\n");
    sb.append("}\n");
    
    request_current = UpdateFactory.create(sb.toString());
    
    }    
       
    /**
     * Spawn products in a loop
     */
    @Override
    public void run() {
        while(true) {
            spawn();
            nw.Test.stats[0] =  nw.Test.stats[0] + 1;
            long timeout = (long) sampler.sample();
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
	public boolean spawn() {
        //getting a Resource list of all models that fulfill the "query_model" Query
        List<Resource> models = Txn.calculateRead(current, () -> {
            try(QueryExecution qexec = QueryExecutionFactory.create(query_model, current)) {
                ResultSet res = qexec.execSelect();
                if(!res.hasNext()) {
                    return null;
                }
                List<Resource> ms = new LinkedList<>();
                while(res.hasNext()) {
                    ms.add(res.next().get("model").asResource());
                    
                }
                return ms;
            }
        });
        //update the model with the request_current Query
        if(models != null) {
        	//Random item of the list is determined to be delivered
        	int rand = models.size();
        	rand = (int)Math.round(Math.random() * (rand - 1));

            Resource model = models.get(rand);

            QuerySolutionMap bindings = new QuerySolutionMap();
            bindings.add("model", model);
            Txn.executeWrite(current, () -> {
                UpdateAction.execute(request_current, current, bindings);
                
            });
            
            return true;
        } else {
            return false;
        }
    }
	
	//starting to spawn as long as there is a model with space left
	public void fillDelivery() {
        boolean spaceLeft = true;
        while(spaceLeft) {
            spaceLeft = spawn();
        }
    }
}
