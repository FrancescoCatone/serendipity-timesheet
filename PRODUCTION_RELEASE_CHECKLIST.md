# Production Release Checklist

Questa checklist serve per accompagnare il primo rilascio in produzione di `serendipity-timesheet`.

L'idea e` semplice:
- usiamo questo file come traccia unica
- spuntiamo i passi man mano che li completiamo
- teniamo separati i blocchi tecnici dai passi operativi

## Stato attuale

- Data creazione checklist: 2026-05-28
- Stato rilascio: non pronto per la produzione
- Ambiente attuale: sviluppo locale

## Bloccanti Da Sistemare Prima Del Rilascio

### Sicurezza

- [x] Rimuovere l'admin automatico con credenziali fisse in `backend/src/main/java/com/serendipity/backend/config/BootstrapConfig.java`
  - Oggi viene creato `admin@serendipity.com / AdminTest123!` se il DB e` vuoto.
- [x] Spostare la chiave JWT fuori dal codice in `backend/src/main/java/com/serendipity/backend/security/JwtService.java`
  - Deve arrivare da variabile d'ambiente o file di configurazione esterno.
- [x] Rimuovere credenziali hardcodate del database da `backend/src/main/resources/application.yml`
- [x] Ridurre i log sensibili in produzione
  - `show-sql: true` va disattivato
  - `org.springframework.security: DEBUG` va portato a un livello normale
- [ ] Evitare di restituire messaggi raw del database al frontend in `backend/src/main/java/com/serendipity/backend/exception/GlobalExceptionHandler.java`

### Configurazione Produzione

- [x] Creare una configurazione `prod` per Spring Boot
- [x] Spostare URL DB, utente DB, password DB, secret JWT e origin frontend su variabili d'ambiente
- [x] Sostituire il CORS locale in `backend/src/main/java/com/serendipity/backend/config/SecurityConfig.java`
  - Oggi accetta solo `http://localhost:5173`
- [x] Aggiornare il frontend per usare l'URL produzione API
  - Oggi `frontend/.env` punta a `http://localhost:8080`

### Database E Test

- [ ] Decidere strategia schema database per produzione
  - minimo: `ddl-auto=validate`
  - meglio: introdurre Flyway per le migrazioni
- [ ] Rendere i test backend indipendenti dal PostgreSQL locale
  - oggi i test completi si aspettano un DB disponibile su `localhost:5432`
- [ ] Definire una procedura di backup del database

## Architettura Consigliata

Per il primo rilascio la soluzione consigliata e`:

- `www.serendipity.it` come dominio pubblico dell'app
- frontend React servito da `Nginx`
- backend Spring Boot raggiungibile tramite `/api`
- PostgreSQL in container separato
- reverse proxy e HTTPS sulla stessa VPS Linux

Schema semplificato:

1. L'utente apre `https://www.serendipity.it`
2. Nginx serve il frontend
3. Le chiamate a `/api/...` vengono inoltrate al backend Spring Boot
4. Il backend legge e scrive su PostgreSQL

## Checklist Operativa Di Rilascio

### Fase 1 - Preparazione Del Codice

- [x] Creare file di configurazione separati per sviluppo e produzione
- [x] Spostare tutti i secret su variabili d'ambiente
- [x] Rimuovere bootstrap admin fisso o renderlo controllato e sicuro
- [x] Sistemare CORS per il dominio reale
- [x] Configurare il frontend per chiamare `/api` in produzione
- [x] Verificare che login, timesheet, conferma ed export PDF funzionino ancora in locale

### Fase 2 - Container E Deploy

- [ ] Creare `Dockerfile` per il backend
- [ ] Creare `Dockerfile` per il frontend
- [ ] Creare `docker-compose.prod.yml`
- [ ] Definire volumi persistenti per PostgreSQL
- [ ] Preparare file `.env` di produzione con tutte le variabili necessarie
- [ ] Configurare `Nginx` come reverse proxy

### Fase 3 - Infrastruttura

- [ ] Verificare disponibilita` del dominio `serendipity.it`
- [ ] Acquistare dominio
- [ ] Acquistare o attivare una VPS Linux
- [ ] Collegare il dominio alla VPS tramite DNS
- [ ] Installare Docker e Docker Compose sulla VPS
- [ ] Configurare HTTPS con certificato SSL

### Fase 4 - Database

- [ ] Fare dump del database locale con `pg_dump`
- [ ] Creare il database PostgreSQL sulla VPS
- [ ] Ripristinare il dump sul database di produzione
- [ ] Verificare che dati e tabelle siano corretti
- [ ] Configurare backup periodici del database

### Fase 5 - Primo Rilascio

- [ ] Caricare il progetto sulla VPS
- [ ] Avviare i container in produzione
- [ ] Verificare che frontend e backend rispondano correttamente
- [ ] Testare login
- [ ] Testare creazione timesheet
- [ ] Testare conferma timesheet
- [ ] Testare export PDF
- [ ] Testare gestione utenti e report
- [ ] Verificare che i log non mostrino errori critici

### Fase 6 - Post Rilascio

- [ ] Cambiare eventuali password iniziali residue
- [ ] Salvare in modo sicuro tutte le credenziali di produzione
- [ ] Verificare che il backup sia davvero eseguibile e ripristinabile
- [ ] Definire una procedura semplice per i prossimi deploy
- [ ] Valutare monitoraggio basilare di app e database

## Verifiche Minime Prima Del Go-Live

Prima di considerare concluso il rilascio, dovranno essere vere tutte queste condizioni:

- [ ] Nessuna credenziale sensibile e` hardcodata nel codice
- [ ] Il backend parte con configurazione `prod`
- [ ] Il frontend non punta piu` a `localhost`
- [ ] Il database usa storage persistente
- [ ] Il dominio risponde in HTTPS
- [ ] Le funzioni principali dell'app sono testate manualmente
- [ ] Esiste almeno un backup del database

## Note Operative

- Il frontend usa token in `localStorage`. Per una prima release puo` andare, ma in futuro potremo valutare un approccio piu` robusto.
- Il backend oggi e` gia` abbastanza vicino a una release tecnica, ma non ancora a una release sicura.
- Il rilascio va affrontato come una sequenza di piccoli passi, non tutto insieme.
- Smoke test locale Fase 1 eseguito il 2026-05-29:
  - login API riuscito
  - creazione timesheet riuscita
  - conferma e chiusura timesheet riuscite
  - export PDF riuscito
  - i dati temporanei di smoke test sono stati rimossi dal database locale al termine

## Prossimo Passo Consigliato

Il prossimo passo migliore e` preparare il progetto al deploy:

- [ ] Creare i file necessari per la produzione:
  - `Dockerfile` backend
  - `Dockerfile` frontend
  - `docker-compose.prod.yml`
  - configurazione `prod` Spring Boot
  - configurazione `Nginx`
