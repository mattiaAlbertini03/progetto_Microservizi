# progetto Microservizi

# SPAZIO PER APPUNTI VARI


- gestire errori in BPMN  ( fleet , ACME, App)



SOA, UML , relazione, BPMS,  (isieme)

container, fleet, App,  (pierre)

stazioni,  aggiustare il BPMN (mattia)

 


- riguardare la lezione extra fatta lunedi (non ricordo quando) che spiega come usare alcune cose che possono essere utili.


le stazioni non fanno niente perche simulano l'azione fisica.

al massimo posso aggiungere un controllo per vedere se il veicolo è a quel veicolo. ( in teoria controllo gia fatto prima) 



 i comandi per generare i wsdl se non vanno per qualche motivo


banca:
jolie2wsdl --namespace mytest.test.com --portName MyInput --portAddr http://localhost:8002 --outputFile MyInput.wsdl server.ol

stazioni:
jolie2wsdl --namespace mytest.test.com --portName MyInput --portAddr http://localhost:8001 --outputFile MyInput.wsdl server.ol



