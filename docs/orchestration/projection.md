S::=ACME|App|Stazione|Banca|Fleet

Fleet::=( 
		?notificaSblocco_req@ACME ; !notificaSblocco_res@ACME ; 
		( ?status_req@ACME ; !status_res@ACME)* ; 
		?notificaBlocco_req@ACME ; !notificaBlocco_res@ACME
	)*

Banca::=( 
		( ?addebito_req@ACME ; !addebito_res@ACME)
		+
		( 
		 ?cauzione_req@ACME ; !cauzione_res@ACME ; 
		 ?addebito_req@ACME ; !addebito_res@ACME ; 
		 ?rimuoviCauzione_req@ACME ; !rimuoviCauzione_res@ACME
		)
	)*

Stazione::=( 
		?sbloccoVeicolo_req@ACME ; !sbloccoVeicolo_res@ACME ; 
		?bloccoVeicolo_req@ACME ; !bloccoVeicolo_res@ACME ; 
	   )*

App::=( 
		( 
		 !mappa_req@ACME ; ?mappa_res@ACME ; 
		 !QR_ritiro@ACME ; ?ris_utente@ACME ; 
		 !Riconsegna@ACME ; ?ris_finale@ACME ; 
		)
		+
		( 
		 !prenotazione@ACME ; ?prenotazione_res@ACME ; 
		 ( !annulla_req@ACME ; ?annulla_res@ACME)
		 +
		 ( !ritiro@ACME ; ?ris_utente@ACME ; !Riconsegna@ACME ; ?ris_finale@ACME ; )
		 +
		 ( 1)
		)
      )*


ACME::=( 
		( 
		 ?mappa_req@App ; !mappa_res@App ; 
		 ?QR_ritiro@App ; 
		 !sbloccoVeicolo_req@Stazione ; ?sbloccoVeicolo_res@Stazione ; 
		 !cauzione_req@Banca ; ?cauzione_res@Banca ; 
		 !notificaSblocco_req@Fleet ; ?notificaSblocco_res@Fleet ; 
		 !ris_utente@App ; 
		 ( !status_req@Fleet ; ?status_res@Fleet ; )* ; 
		 ?Riconsegna@App ; !bloccoVeicolo_req@Stazione ; ?bloccoVeicolo_res@Stazione ; 
		 !notificaBlocco_req@Fleet ; ?notificaBlocco_res@Fleet ; 
		 !addebito_req@Banca ; ?addebito_res@Banca ; 
		 !rimuoviCauzione_req@Banca ; ?rimuoviCauzione_res@Banca ; 
		 !ris_finale@App ; 
		)
		+
		( 
		 ?prenotazione@App ; !ris_pre@App ; 
		 ( 
		  ?annulla_req@App ; !annulla_ris@App ; 
		  ( !addebito_req@Banca ; ?addebito_res@Banca ; )+( 1) ; 
		 )
		 +
		 ( !addebito_req@Banca ; ?addebito_res@Banca ; )
		 +
		 ( 
		  ?ritiro@App ; 
		  !sbloccoVeicolo_req@Stazione ; ?sbloccoVeicolo_res@Stazione ; 
		  !cauzione_req@Banca ; ?cauzione_res@Banca ; 
		  !notificaSblocco_req@Fleet ; ?notificaSblocco_res@Fleet ; 
		  !ris_utente@App ; 
		  ( !status_req@Fleet ; ?status_res@Fleet ; )* ; 
		  ?Riconsegna@App ; !bloccoVeicolo_req@Stazione ; ?bloccoVeicolo_res@Stazione ; 
		  !notificaBlocco_req@Fleet ; ?notificaBlocco_res@Fleet ; 
		  !addebito_req@Banca ; ?addebito_res@Banca ; 
		  !rimuoviCauzione_req@Banca ; ?rimuoviCauzione_res@Banca ; 
		  !ris_finale@App ; 
		 )
		 )
	)*

