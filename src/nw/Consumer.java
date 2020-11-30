package nw;


import java.util.LinkedList;
import java.util.List;


import org.apache.commons.math3.distribution.ExponentialDistribution;
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



  

public class Consumer implements Runnable {
	 // Consumes products at the given rate (Exponential Distribution)
	//timeout the consumer, so that it is not running & consuming products constantly
	public static int exp_dis = 1000;
	
	 // Exponential distribution to get the duration of this specific action simulation
    public static ExponentialDistribution sampler = null;

    // Reference to the current triple store	
    private Dataset current;

    //Query which product should be consumed
    private Query shipping_product;
    
    //Request to consume the product determined by the shipping_product Query
    private UpdateRequest request_current;
    
   
    /** 
     * Set the lambda of the exponential distribution
     */
    

    /**
     * Constructor for consumer
     * @param current Triple store for the current state of the factory
     */
    public Consumer(Dataset current) {

        this.current = current;
        sampler = new ExponentialDistribution(exp_dis); 

      //Request to consume the product determined by the shipping_product Query
        StringBuilder sb = new StringBuilder();
        sb.append("PREFIX : <" + Server.BASE_URI + "current#>\n");
        sb.append("PREFIX arena: <http://paul.ti.rw.fau.de/~pi69geby/arena/>\n");
        sb.append("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n");
        sb.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
        sb.append("PREFIX schema: <http://schema.org/>\n");
        sb.append("SELECT ?model\n");
        sb.append("WHERE {\n");
        sb.append("    :shipping a schema:Place ;\n");
        sb.append("    	arena:hasSlot ?slot .\n");
        sb.append("    ?slot arena:capacity ?capacity ;\n");
        sb.append("        schema:model ?model .\n");
        sb.append("    FILTER(?capacity < 10)\n");
        sb.append("}\n");
        sb.append("LIMIT 1\n");
        shipping_product = QueryFactory.create(sb.toString());

        
     // Exponential distribution to get the duration of this specific action simulation
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
        sb.append("    :shipping arena:hasSlot ?slot .\n");
        sb.append("    ?slot arena:capacity ?capacity ;\n");
        sb.append("        schema:model ?model .\n");
        sb.append("    BIND(?capacity + 1 AS ?capacityPlusOne)\n");
        sb.append("}\n");
        request_current = UpdateFactory.create(sb.toString());

    }

    /**
     * Getting a product and deleting it from the current triple store
     */
    @Override
    public void run() {
        while(true) {
        	//Query a list of the products to be consumed 
        	List<Resource> models = Txn.calculateRead(current, () -> {
                try(QueryExecution qexec = QueryExecutionFactory.create(shipping_product, current)) {
                    ResultSet res = qexec.execSelect();
                    
                    //Getting the next product to be consumed as a Resource
                    List<Resource> ms = new LinkedList<>();
                    if(res.hasNext()) {
                        ms.add(res.next().get("model").asResource());
                        return ms;
                    }
                    return null;
                   
                }
            });
            //update the model with the request_current Query
            if(models != null) {
            	
                Resource model = models.get(0);

                QuerySolutionMap bindings = new QuerySolutionMap();
                //binding the model, determined for the product to be consumed, into the request_current query
                bindings.add("model", model);
                Txn.executeWrite(current, () -> {
                    UpdateAction.execute(request_current, current, bindings);
                });
                nw.Test.stats[2] = nw.Test.stats[2] + 1;
                
            }
            // Sleep for random time
            long timeout = (long) sampler.sample();
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
