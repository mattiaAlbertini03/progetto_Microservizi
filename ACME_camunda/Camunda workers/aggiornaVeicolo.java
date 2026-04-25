import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.client.api.worker.JobClient; 
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@Component
public class aggiornaVeicoloWorker {

	@JobWorker(type = "aggiorna-veicolo")
	public void aggiornaVeicolo(final JobClient client, final ActivatedJob job) {

		String newValore = (String) job.getCustomHeaders().get("stato");
		String vehicleId = (String) job.getVariablesAsMap().get("vehicleId");

		String stazione = (String) job.getVariablesAsMap().get("newStazioneId");
		
		Integer batteria = (Integer) job.getVariablesAsMap().get("valBatteria");

//TODO veicolo contiene anche lat/lon???
//se si da prendere la stazione e copiare i valori
//
//per ora la stazione rimane e cambia solo alla fine

		File jsonFile = new File("_______/veicoli.json");

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(jsonFile);
			boolean updated = false;

			if (rootNode.isArray()) {
				for (JsonNode item : rootNode) {
					if (item.has("idVehicle") && item.get("idVehicle").asText().equals(vehicleId)) {
						((ObjectNode) item).put("status", newValue);
						if(stazione != null){
							((ObjectNode) item).put("IdStazione", stazione);
						}
						if(batteria != null){
							((ObjectNode) item).put("batteria", batteria);
							if (batteria <= 15){
								((ObjectNode) item).put("status", "ricarica");
							}
						}
						updated = true;
						break; 
					}
				}
			}

			if (updated) {
				mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, rootNode);
			}

			client.newCompleteCommand(job.getKey()).send().join();

		} catch (Exception e) {
			client.newFailCommand(job.getKey()).retries(0).errorMessage("Error: " + e.getMessage()).send().join();
		}
	}
}
