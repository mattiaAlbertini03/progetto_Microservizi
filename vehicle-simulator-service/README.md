Gestione Vehicle Simulator (ACMEMobility)

Il Vehicle Simulator è un servizio interno utilizzato per simulare il comportamento dei veicoli durante il noleggio.

ACMEMobility avvia la simulazione quando riceve dalla stazione la conferma di sblocco del veicolo.

Durante il noleggio il simulatore invia aggiornamenti periodici al Fleet Management riguardanti:

la posizione del veicolo (vehicle-tracking)

il livello della batteria (vehicle-battery)

Fleet Management utilizza questi aggiornamenti per monitorare in tempo reale lo stato del veicolo.

ACMEMobility interroga periodicamente Fleet Management per ottenere lo stato aggiornato del mezzo.
Quando rileva che il veicolo ha raggiunto la stazione di destinazione simulata (cioè le coordinate della stazione di arrivo note ad ACMEMobility al momento dell’avvio della simulazione), invia una notifica all’app per indicare all’utente che il veicolo può essere riconsegnato.

Quando l’utente conferma la riconsegna e la stazione conferma l’avvenuto blocco del veicolo, ACMEMobility:

termina la simulazione del veicolo

notifica a Fleet Management la fine del monitoraggio

Avvio simulazione veicolo

Il Vehicle Simulator espone un endpoint REST:

POST /startSimulation

ACMEMobility invia una richiesta con un body JSON contenente:

vehicleId

fromStationId

toStationId

Esempio:

{
  "vehicleId": "V12",
  "fromStationId": "S03",
  "toStationId": "S07"
}

Alla ricezione della richiesta il Vehicle Simulator:

avvia la simulazione del veicolo tra le due stazioni;

genera periodicamente aggiornamenti di:

posizione (vehicle-tracking)

batteria (vehicle-battery);

invia tali aggiornamenti al Fleet Management.

Il simulatore non gestisce logiche di business (prenotazioni, noleggi, pagamenti, riconsegne).
Il suo unico compito è simulare il movimento del veicolo e produrre gli aggiornamenti di stato.

La simulazione viene terminata da ACMEMobility tramite una richiesta dedicata (es. POST /stopSimulation) dopo la conferma di blocco del veicolo da parte della stazione.
