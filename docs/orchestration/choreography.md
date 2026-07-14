(
	(
		mappa_req: App -> ACME ;  mappa_res: ACME -> App ; 	 
        ritiroQR_req: App -> ACME ;  
    	sbloccoVeicolo_req: ACME -> Stazione ; sbloccoVeicolo_res: Stazione -> ACME ; 
		cauzione_req: ACME -> Banca ; cauzione_res: Banca -> ACME ; 
	    notificaSblocco_req: ACME -> Fleet ; notificaSblocco_res: Fleet -> ACME ;
		ritiro_res: ACME -> App;
    	( status_req: ACME -> Fleet ; status_res: Fleet -> ACME )* ;
	    riconsegna_req: App -> ACME ; 
		bloccoVeicolo_req: ACME -> Stazione ; bloccoVeicolo_res: Stazione -> ACME ; 
	    notificaBlocco_req: ACME -> Fleet ; notificaBlocco_res: Fleet -> ACME;  
		addebito_req: ACME -> Banca ; addebito_res: Banca -> ACME ; 
	    rimuoviCauzione_req: ACME -> Banca ; rimuoviCauzione_res: Banca -> ACME ; 
		riconsegna_res: ACME -> App
    )
    +
    (
		prenotazione_req: App -> ACME ; prenotazione_res: ACME -> App ; 
		( 
			annulla_req: App -> ACME ;  annulla_res: ACME -> App ;
			( addebito_req: ACME -> Banca ; addebito_res: Banca -> ACME ) 
			+ 
			( 1 )
		)
		+
		( addebito_req: ACME -> Banca ; addebito_res: Banca -> ACME ) 
	    +
	    (
			ritiroDiretto_req: App -> ACME;
    		sbloccoVeicolo_req: ACME -> Stazione ; sbloccoVeicolo_res: Stazione -> ACME ; 
			cauzione_req: ACME -> Banca ; cauzione_res: Banca -> ACME ; 
		    notificaSblocco_req: ACME -> Fleet ; notificaSblocco_res: Fleet -> ACME ;
			ritiro_res: ACME -> App;
    		( status_req: ACME -> Fleet ; status_res: Fleet -> ACME )* ;
		    riconsegna_req: App -> ACME ; 
			bloccoVeicolo_req: ACME -> Stazione ; bloccoVeicolo_res: Stazione -> ACME ; 
		    notificaBlocco_req: ACME -> Fleet ; notificaBlocco_res: Fleet -> ACME;  
			addebito_req: ACME -> Banca ; addebito_res: Banca -> ACME ; 
		    rimuoviCauzione_req: ACME -> Banca ; rimuoviCauzione_res: Banca -> ACME ; 
			riconsegna_res: ACME -> App
	    )
    )
)*
