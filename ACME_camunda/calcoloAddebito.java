import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.client.api.worker.JobClient; 
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.lang.Math;

@Component
public class calcoloAddebitoWorker {

    @JobWorker(type = "calcolo-addebito")
    public void calculoAddebito(final JobClient client, final ActivatedJob job) {
        
	
	String stazione1 = (String) job.getVariablesAsMap().get("stazioneId");
	String stazione2 = (String) job.getVariablesAsMap().get("newStazioneId");
	Integer valBatteria = (Integer) job.getVariablesAsMap().get("valBatteria");
	Integer velocita = (Integer) job.getVariablesAsMap().get("velocita");
        
	
	String id1 = (String) job.getCustomHeaders().get("firstId"); // usare per la veolocità? o i due tempi???
								     // usare header o è passato dal fleet??
        
        File jsonFile = new File("_______/stazioni.json");
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            // 3. Parse JSON as a List of Maps (since you have a list of couples)
            List<Map<String, Object>> dataList = mapper.readValue(jsonFile, new TypeReference<List<Map<String, Object>>>() {});
            
            float lat1 = 0;
            float lon1 = 0;
            float lat2 = 0;
            float lon3 = 0;

            // 4. Find the values for both IDs in the list
            for (Map<String, Object> entry : dataList) {
                String currentId = entry.get("idStazione").toString();
                
                if (currentId.equals(stazione1)) {
                    lat1 = Float.parseInt(entry.get("lat").toString());
                    lon1 = Float.parseInt(entry.get("lon").toString());
                }
                if (currentId.equals(stazione2)) {
                    lat2 = Float.parseInt(entry.get("lat").toString());
                    lon2 = Float.parseInt(entry.get("lon").toString());
                }
            }


            float deltaLat = Math.abs(lat1 - lat2)*111; // da fare *111 per convertire da lon/lat a km
            deltaLat = Math.pow(deltaLat, 2);
            float deltaLon = Math.abs(lon1 - lon2)*111;
            deltaLon = Math.pow(deltaLon, 2);
	    float dist = deltaLat + deltaLon;
	    dist = Math.pow(dist, 0.5);
	    
	    float tempo = (dist/(float)velocita)*60.0; // oppure faccio la differnza dall'altro lato e qui prendo quella 
						// per ora calcolo in minuti 

	    float conto = (dist * 0.30) + (tempo * 0.20);  // al massimo cambiamo


	    if (valBatteria < 15) {
		    conto *=1.10;
	    }
	   	
	    int valoreTot = (int) conto;



            // 5. Complete the job and send the result back to Camunda as a variable
            client.newCompleteCommand(job.getKey()).variables(Map.of("valoreAddebito", valoreTot)).send().join();

        } catch (Exception e) {
            // If the file is missing or JSON is wrong, fail the job
            client.newFailCommand(job.getKey()).retries(0).errorMessage("Error: " + e.getMessage()).send().join();
        }
    }
}
