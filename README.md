# progetto Microservizi

- gestire errori in BPMN  ( fleet , ACME, App)

SOA, UML , relazione, BPMS,  (isieme)

container, App,  (pierre)


i comandi per generare i wsdl se non vanno per qualche motivo
banca:
jolie2wsdl --namespace mytest.test.com --portName MyInput --portAddr http://localhost:8002 --outputFile MyInput.wsdl server.ol

stazioni:
jolie2wsdl --namespace mytest.test.com --portName MyInput --portAddr http://localhost:8001 --outputFile MyInput.wsdl server.ol



#MODIFICA DA FARE IN STAZIONI

facciamo partire i servizi stazioni prima di ACME.

facciamo che chiamiamo il login 5 volte con valori di posizione diversi e dopo il login facciamo che il sid e i valori vengono generati e salvati su un json. 

cosi facendo possiamo generare le sole 5 stazioni con i 5 sid e il servizio ACME va a leggere da questo json i valori.

questi valori sono qeulli che in veicolo vanno ha definire la stazioni in cui arriva.

es stazione.json
{
{
 lat:   ,
 lon:   ,
 sid:   
}
}
 altro???



per cui il sistema deve far partire le 5 stazioni, generare gli x veicoli ( facciamo 2/3 per stazione?) , far partire il server banca, ( forse anche fleet?? 
dipende da come l'hai implementato), server ACME ( da vedere come fare con il docker ma in teoria si puo' fare), far partire l'APP.

in App utente mette i suoi dati: username, conto, ricarica, ( la posizione generata random dentro uno spazio fra le stazioni? ). appena entra mandare un msg 
ad ACME per farlo partire ( e per far partie banca).  dopo chiede quale fare fra i due servizi. 

NEL CASO NON SIA CHIARE VEDERE IL FILE IN ACME\_camunda 



#Testo del progetto semplificato con le info che servono

I clienti interagiscono attraverso App mobile.

ACMEMobility offre due modalità:   Noleggio Immediato  &&  Prenotazione Breve

Il pagamento due fasi:  Blocco Cauzionale: €8    && Pagamento Finale: tempo utilizzo e Km percorsi. Se batteria <15%, penale d10%. importo addebitato, e sbloccata cauzione.

ACMEMobility gestisce stato veicolo (disponibile, prenotato, in noleggio, in manutenzione, in ricarica) e liv batteria. È responsabilità del servizio di Logistica di ACMEMobility garantire che i veicoli siano caricati e mantenuti efficienti.

Annullamenti:  gratuitamente fino 3 min prima fascia prenotata. Oltre questo termine, o in mancato ritiro, cauzione di 10€ convertito in addebito.

UML TinySOA.

sistema con BPMS (Camunda), Jolie e API Rest, :

    ACMEMobility capabilities attraverso BPMS;

servizi istanziati container usando Kubernetes.

un BPMN documentativo e tanti BPMN eseguibili partecipanti BPMS.

dialogo Jolie-BPMS via SOAP.


3-10 stazioni.

monitoraggio continuo veicoli. app gstisce simulazione tutti mezzi. notifica stato mezzo può avvenire chiamate sincrone “simulatore” Fleet 

assumere mezzo proceda linea retta a velocità uniforme fra due stazioni. 


Consegna e discussione

Tempi: Il progetto va consegnato prima che inizino le lezioni dell’A.A. 2024/27.

consegnare: relazione che descrive il lavoro fatto nelle fasi di modellazione e sviluppo, inclusi i diagrammi prodotti: coreografia e sistema proiettato, diagramma/i UML, diagramma/i di processo BPMN. Repository di progetto (non pubblico) GitHub che con tutto il necessario per fare il build e il deployment della SOA.


Modalità  di consegna: via email con riferimenti al repository.
Discussione del progetto: discussione su richiesta. tutti i membri. valutazione personale.


