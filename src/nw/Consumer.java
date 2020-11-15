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


 // Consumes products at a given rate
  

public class Consumer implements Runnable {
    // References to triple stores
    private Dataset current;

    private Query shipping_product;
    private UpdateRequest request_current;
    
    // Exponential distribution to get the duration of this specific action simulation
    public static ExponentialDistribution sampler = null;

    /** 
     * Set the lambda of the exponential distribution
     */
    

    /**
     * Constructor for consumer
     * @param current Triple store for the current state of the factory
     */
    public Consumer(Dataset current) {

        this.current = current;
        sampler = new ExponentialDistribution(1000); 

        StringBuilder sb = new StringBuilder();
        sb.append("PREFIX : <" + Server.BASE_URI + "current#>\n");
        sb.append("PREFIX arena: <http://paul.ti.rw.fau.de/~pi69geby/arena/>\n");
        sb.append("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n");
        sb.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
        sb.append("PREFIX schema: <http://schema.org/>\n");
        sb.append("SELECT ?model\n");
        sb.append("WHERE {\n");
        sb.append("    :shipping a schema:Place .\n");
        sb.append("    ?slot arena:capacity ?capacity ;\n");
        sb.append("        schema:model ?model .\n");
        sb.append("    FILTER(?capacity < 10)\n");
        sb.append("}\n");
        sb.append("LIMIT 1\n");
        shipping_product = QueryFactory.create(sb.toString());

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
     * Getting a product, moving it from current to history triple store and deleting
     * it location in a loop
     */
    @Override
    public void run() {
        while(true) {
        	List<Resource> models = Txn.calculateRead(current, () -> {
                try(QueryExecution qexec = QueryExecutionFactory.create(shipping_product, current)) {
                    ResultSet res = qexec.execSelect();
                    if(!res.hasNext()) {
                        return null;
                    }
                    List<Resource> ms = new LinkedList<>();
                    while(res.hasNext()) {
                        ms.add(res.next().get("model").asResource());
                        
                    }
                    System.out.println(ms);
                    return ms;
                }
            });
            //update the model with the request_current Query
            if(models != null) {

                Resource model = models.get(0);

                QuerySolutionMap bindings = new QuerySolutionMap();
                bindings.add("model", model);
                Txn.executeWrite(current, () -> {
                    UpdateAction.execute(request_current, current, bindings);
                });
                nw.Test.ship_count = nw.Test.ship_count + 1;
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
