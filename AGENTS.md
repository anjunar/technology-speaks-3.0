# AGENTS.md

## Zweck
Technology Speaks ist eine Wissensdatenbank fuer Spiritualitaet und Philosophie mit Social-Network-Funktionen, Timeline-Posts, Likes, Kommentaren, Follower-Beziehungen und wiki-aehnlichen Dokumenten.

Diese Datei beschreibt nicht nur Arbeitsregeln, sondern die tragenden Architekturentscheidungen des Projekts. Agenten sollen vor Aenderungen zuerst diese Architektur respektieren und nur dann abstrahieren oder vereinfachen, wenn das bestehende Muster klar fehlerhaft ist.

## Arbeitsregeln
- Fuehre Headless-Tests mit `1980x1080` aus.
- Frontend-Dev-Server ist `localhost:5173`.
- Fuer Server-Neustarts immer erst den Nutzer fragen.
- Test-Login:
  - User: `p_bittner@gmx.de`
  - Passwort: `test`
- Fuer sbt das bereits laufende `sbtn-x86_64-pc-win32.exe` bzw. den vorhandenen sbt-Server nutzen.

## Architekturueberblick
- Das Projekt ist ein SBT-Monorepo mit bewusst getrennten Schichten fuer Frontend, Backend, Domain und Infrastruktur.
- Das Frontend ist kein React/Vue-Projekt, sondern eine eigene Scala.js-UI-Plattform auf Basis des internen `jfx`-Frameworks.
- Das Backend ist ein Spring-Boot-Webserver in Scala mit JPA/Hibernate.
- Die API ist HATEOAS- und schema-getrieben: Responses liefern nicht nur Daten, sondern Links und Schemainformationen fuer erlaubte Aktionen und Feldsichtbarkeit.
- Das Frontend modelliert viele Backend-DTOs bewusst nochmals als eigene Scala.js-Modelle. Diese Duplikation ist Teil der Architektur, kein Versehen.

## Modulstruktur
- `application`
  - Startet Spring Boot und verdrahtet die Web-Konfiguration.
- `rest`
  - Enthaelt die REST-Controller fuer die fachlichen Endpunkte.
- `domain`
  - Enthaelt die fachlichen Entitaeten, Regeln, Security-Bausteine und einen Teil der HATEOAS-/Schema-Logik.
- `system`
  - Infrastruktur fuer Persistence, JSON/Mapper, Search, REST-Konverter und Request-Transaktionen.
- `frontend/jfx`
  - Eigenes UI-/State-/DSL-Framework fuer Scala.js.
- `frontend/app`
  - Die eigentliche Web-App auf Basis von `jfx`.
- `library/json-mapper`, `library/scala-enterprise`, `library/scala-universe`
  - Interne Bibliotheken, auf denen Backend und Mapping aufbauen.

## Harte Architekturregeln

### Klare Abgrenzung von Domain-, API- und UI-Modellen
- `domain` enthaelt fachliche Backend-Entitaeten, Regeln und Persistenzmodelle.
- `rest` und `system/rest/types` definieren das API-Verhalten und die Transportformen wie `Data[...]`, `Table[...]`, `Link`, `Schema`.
- `frontend/app/domain` enthaelt UI-seitige Clientmodelle fuer Scala.js.
- Backend-Entity, API-DTO und Frontend-Modell sind konzeptionell getrennte Schichten, auch wenn sie fachlich gleich benannt sind.
- Agenten duerfen diese Ebenen nicht unbedacht zusammenziehen.
- Keine direkte Wiederverwendung von JPA-Entities im Frontend denken oder simulieren.
- Keine UI-spezifischen concerns in Backend-Entities einbauen.
- Keine API-Transportform nur deshalb aufloesen, weil Daten fachlich identisch erscheinen.

### Serialisierungs-/Mapping-Architektur als harte Regel
- Serialisierung ist in diesem Projekt kein Detail, sondern ein eigener Architekturpfeiler.
- Backend-JSON laeuft ueber die interne Mapper-/DTO-Architektur, nicht ueber beliebige Standard-Case-Class-Serialisierung.
- Frontend-Deserialisierung laeuft ueber `AppJson.mapper`.
- Frontend-Modelle muessen ihre `properties` explizit deklarieren.
- Wenn ein Feld im Backend oder Frontend neu eingefuehrt wird, muss geprueft werden:
  - Backend-Entity/DTO
  - API-Transport
  - Frontend-Modell
  - Mapping/Schema
  - Validierung
- Mapping darf nicht stillschweigend implizit werden. Neue Datenpfade muessen in die vorhandene Mapping-Architektur eingepasst werden.

### Security-Grundsatz
- Security ist serverseitig bindend und darf nie nur im Frontend modelliert werden.
- Sichtbarkeit einer Aktion im UI ist nur eine Darstellung dessen, was Backend-Rollen, HATEOAS-Links und Schemas bereits erlauben.
- Rollenpruefung bleibt auf Backend-Seite ueber `@RolesAllowed` und `SecurityInterceptor` autoritativ.
- UI darf Aktionen verstecken, aber nicht als einzige Schutzmassnahme verwenden.
- Wenn eine Aktion sicherheitsrelevant ist, muss geprueft werden:
  - Rollenfreigabe
  - vorhandene Links
  - Sichtbarkeitsregeln im Schema
  - Ownership-/ManagedProperty-Kontext

## Backend-Architektur

### Request-Fluss
- Spring Boot laeuft unter `/service`.
- Vite proxyt `/service` auf den Backend-Server `localhost:8080`.
- `TransactionPerRequestFilter` spannt fuer jeden `/service`-Request eine Transaktion auf.
- `GET` und `HEAD` werden als read-only behandelt.

### Controller-Stil
- Controller liegen im Modul `rest`.
- Zugriff wird ueber `@RolesAllowed` abgesichert und zentral im `SecurityInterceptor` geprueft.
- Der Einstiegspunkt der API ist `ApplicationController`, der die fuer den aktuellen Nutzer verfuegbaren Hauptlinks liefert.
- Listenendpunkte geben typischerweise `Table[...]` zurueck.
- Detailendpunkte geben typischerweise `Data[...]` oder eine Entitaet mit Links zurueck.

### Persistence-Stil
- Entitaeten sind JPA-Entities im Modul `domain`.
- `AbstractEntity` liefert `id`, `version`, `created`, `modified`.
- Repositories sind nicht als klassische Spring Data Interfaces modelliert, sondern ueber `RepositoryContext[E]`.
- Entitaetsinstanzen koennen ueber `EntityContext[E]` selbst `persist`, `merge` und `remove` ausfuehren.
- Dieses Projekt nutzt also aktiv ein Active-Record-aehnliches Muster, nicht strikt Repository + Service pro Aggregate.

### Schema- und Sichtbarkeitsmodell
- Das Projekt nutzt Entity-Schemas als First-Class-Konzept.
- `SchemaHateoas.enhance(...)` erweitert Schemas zur Laufzeit anhand des aktuellen Nutzers.
- Sichtbarkeit einzelner Properties ist nicht nur UI-Logik, sondern fachliches Backend-Verhalten.
- `ManagedProperty`, `ManagedRule` und verwandte Klassen bilden ein Feld-basiertes Visibility-System.
- Wenn ein Feld fuer den Eigentuemer verwaltbar ist, bekommt das Schema Property-Links wie `property` oder `updateProperty`.
- UI-Aenderungen an Feldsichtbarkeit muessen deshalb mit dem Schema-/ManagedProperty-Modell zusammenpassen.

### Link-Erzeugung
- HATEOAS-Links werden nicht manuell zusammengesetzt, sondern ueber `LinkBuilder`.
- `LinkBuilder.create[Controller](_.method(...))` ist ein zentrales Architekturpattern.
- Der Builder liest Spring-Mapping-Annotationen und Rolleninformationen aus und erzeugt daraus Links.
- Wenn moeglich, dieses Muster weiterverwenden statt URLs als Strings zu duplizieren.

### Suche und Listen
- Suchlogik laeuft ueber die abstrahierte Hibernate-Suche in `system/.../hibernate/search`.
- Suchparameter kommen haeufig als Search-Objekte wie `UserSearch`, `PostSearch`, `RelationShipSearch`.
- Listen-Endpoints kombinieren Suchkontext, Result-Row-Typ und Tabellen-Schema.

### Such-/Tabellenarchitektur als Pattern
- Listenansichten folgen einem wiederkehrenden End-to-End-Pattern:
  - Backend-Search-Objekt
  - Backend-Controller liefert `Table[...]`
  - Frontend nutzt `RemoteTableList` bzw. `RemoteListProperty`
  - UI bindet Tabellen-/Listenstate an `Property` und `ListProperty`
- Pagination, Sortierung und Query-Strings sollen diesem Pattern folgen statt pro Seite neu erfunden zu werden.
- Neue Listenfunktionalitaet bevorzugt in bestehende Search-/Table-Pfade integrieren.
- Sortierlogik soll ueber die vorhandenen `sort`-/`effectiveSortSpecs`-Mechanismen laufen.
- Tabellen- und Suchverhalten nach Moeglichkeit datengetrieben halten, nicht durch verstreute lokale Sonderlogik.

## Frontend-Architektur

### UI-Framework
- Das Frontend verwendet `jfx`, ein eigenes deklaratives DSL fuer DOM, Komponenten, Formulare, Router, State und Overlays.
- Typische primitives sind `div`, `hbox`, `vbox`, `span`, `form`, `input`, `subForm`, `observeRender`, `forEach`, `windowRouter`.
- Komponenten sind meist `DivComposite` oder `PageComposite`.
- Komponenten werden ueber `CompositeSupport.buildComposite(...)` bzw. `buildPage(...)` gebaut.
- Neue UI sollte dem bestehenden `jfx`-Pattern folgen und nicht mit fremden Frontend-Stacks vermischt werden.

### State-Management
- State basiert auf `Property`, `ListProperty`, `RemoteListProperty`, Observern und Disposables.
- Reaktivitaet entsteht durch `observe`, `observeWithoutInitial`, `observeRender` und aehnliche Mechanismen.
- Es gibt kein Redux, kein React-Hook-System und keine virtuelle React-Komponentenstruktur.

### Lifecycle/Dispose/Observer-Disziplin
- Observer, Bindings und Listener muessen als Lifecycle-Ressourcen behandelt werden.
- Alles, was beobachtet oder registriert wird, soll sauber ueber `addDisposable(...)` oder gleichwertige Mechanismen gebunden werden.
- Keine langfristig lebenden Observer ohne Besitzkontext erzeugen.
- Keine Event-Listener oder Property-Observer anlegen, die bei Re-Render oder Navigation weiterleben koennen.
- Bei Komponenten mit dynamischem Rendern immer auf Disposal und Mehrfachregistrierung achten.
- Wenn ein UI-Bug nach wiederholter Navigation schlimmer wird, ist ein Lifecycle-/Dispose-Fehler wahrscheinlicher als ein CSS-Problem.

### Routing
- Frontend-Routing ist in `frontend/app/src/main/scala/app/Routes.scala` zentral definiert.
- Routen sind entweder synchron (`route`) oder asynchron (`asyncRoute`).
- Asynchrone Routen laden ihre Daten vor dem Rendern und bauen danach die jeweilige Page.
- Navigation laeuft ueber das History-API in `Navigation.navigate(...)`.

### Asynchronitaets- und Datenflussmodell
- Datenfluss ist in diesem Projekt ueberwiegend API-getrieben und property-basiert.
- Routen laden initiale Daten asynchron und erzeugen danach die UI.
- Weitere Aktualisierungen laufen ueber Properties, Observer und explizite Reloads.
- Asynchronitaet soll sichtbar im Datenfluss bleiben. Keine verdeckten Seiteneffekte in scheinbar reinem UI-Code.
- API-Requests sollen ueber `Api` oder bestehende Domainmethoden laufen, nicht ueber verteilte `fetch`-Direktaufrufe.
- Nach Mutationen ist zu entscheiden, ob:
  - lokaler State gezielt aktualisiert wird
  - ein Detailobjekt neu geladen wird
  - eine Remote-Liste reloadet wird
- Diese Entscheidung bewusst treffen, nicht mischen.

### API-Kopplung
- Das Frontend spricht die API ueber `app.support.Api`.
- Die API-Schicht serialisiert und deserialisiert nicht mit beliebigem JSON-Mapping, sondern ueber `AppJson.mapper`.
- Frontend-Modelle implementieren `JsonModel` oder `Model` und besitzen explizite `properties`.
- Backend-DTOs wie `Data`, `Table`, `Schema`, `Link` und Fachentitaeten werden im Frontend erneut abgebildet.

### HATEOAS im Frontend
- UI-Aktionen sollen moeglichst von vorhandenen Links (`rel`) abhaengen, nicht nur von vermuteten URLs.
- Typische Patterns:
  - `renderByRel("update", model.links) { ... }`
  - `linkByRel(...)`
  - `Api.invokeLink(link, body)`
- Wenn eine Aktion ueber Links modelliert ist, soll die UI diese Links nutzen statt Endpunkte hart zu coden.

### Formulare
- Formulare sind stark modellgebunden.
- Validierung und Serialisierung orientieren sich an den deklarierten Properties des Modells.
- Nested Data wird ueber `subForm` modelliert.
- Sichtbarkeits- oder Editierbarkeitsschalter werden oft ueber `editable` und Property-Observer umgesetzt.

### Fehler- und Validierungsarchitektur
- Fehlerbehandlung ist geschichtet und soll nicht in generischen `catch all`-Mustern verflachen.
- Backend liefert Validierungsfehler als strukturierte Fehlerresponses.
- Frontend behandelt `400` gezielt als `ErrorResponseException`.
- Formfehler sollen nach Moeglichkeit auf Formular-/Feldebene landen, nicht nur als globale Notification.
- `403` wird im Frontend als Security-/Login-Problem behandelt und ueber Redirectlogik verarbeitet.
- Unerwartete Fehler duerfen global gemeldet werden, sollten aber zusaetzlich geloggt werden.
- Validierungsregeln sollen, wenn vorhanden, sowohl im Modell als auch im Backend-Konsens sichtbar sein statt nur kosmetisch im UI.

### Rich Text / Editor
- Das Projekt verwendet ProseMirror, aber gekapselt hinter `jfx.form.Editor` und eigene Plugins.
- Editor-Erweiterungen sollten bevorzugt in den vorhandenen Plugin-Strukturen umgesetzt werden, nicht direkt ad hoc im Page-Code.

### Visuelle und komponentenseitige Aenderungsregeln
- Visuelle Aenderungen sollen zuerst die bestehende Komponentenhierarchie und Layoutlogik respektieren.
- Nicht vorschnell CSS ueberladen, wenn das eigentliche Problem durch falsche Struktur, Komponentengrenzen oder jfx-Verhalten entsteht.
- Page-spezifische Styles gehoeren in die jeweilige Page-CSS; Framework-Verhalten gehoert nach `frontend/jfx`.
- Komponenten sollen in ihrer Rolle klar bleiben:
  - Page fuer Seitenkomposition
  - Component fuer wiederverwendbare Darstellung oder Interaktion
  - jfx fuer generisches UI-Verhalten
- Bei visuellen Fixes zuerst pruefen:
  - Ist die DOM-/DSL-Struktur richtig?
  - Ist der Datenzustand richtig?
  - Ist die Komponente am richtigen Ort?
  - Erst danach CSS feinjustieren.
- Keine schnelle Styling-Loesung bauen, die ein Architekturproblem nur kaschiert.

## Wichtige Architekturentscheidungen
- Duplizierte Domainmodelle zwischen Backend und Frontend sind beabsichtigt.
- HATEOAS-Links sind Teil des Sicherheits- und UI-Modells.
- Schemas sind Laufzeitdaten und nicht nur Dokumentation.
- Feldsichtbarkeit ist fachlich modelliert und darf nicht nur kosmetisch im Frontend geloest werden.
- Das Frontend rendert datengetrieben und linkgetrieben, nicht nur routegetrieben.
- Das Projekt bevorzugt eigene Framework-Abstraktionen gegenueber Mainstream-Frameworks.
- Persistenzoperationen liegen teilweise direkt auf Entities bzw. Repository-Contexts, nicht in einem strikten Service-Layer.
- Domain-, API- und UI-Modelle sind bewusst getrennt und duerfen nicht vermischt werden.
- Dispose-/Observer-Disziplin ist Teil der Frontend-Architektur, nicht nur Cleanup-Kosmetik.
- Mapping und Serialisierung sind bindende Architekturregeln.
- Such- und Tabellenfluesse folgen einem wiederverwendbaren Pattern.
- Visuelle Aenderungen sollen komponenten- und strukturorientiert erfolgen, nicht nur CSS-getrieben.

## Aenderungsregeln fuer Agenten
- Vor jeder groesseren Aenderung zuerst pruefen, in welcher Schicht das Problem wirklich liegt:
  - `frontend/app` fuer Seiten, Routing und App-spezifische UI
  - `frontend/jfx` fuer Framework-Verhalten
  - `rest` fuer Endpunkte
  - `domain` fuer Fachmodell, Sichtbarkeit, Rollen, Links
  - `system` fuer Infrastruktur, Mapper, Search, Transaktion
- Keine REST-URLs hart kodieren, wenn bereits ein `Link` oder `LinkBuilder`-Pattern existiert.
- Keine Feldsichtbarkeit nur im Frontend verstecken, wenn sie ueber `ManagedProperty`/Schema geregelt ist.
- Keine neue Fremdbibliothek einfuehren, wenn das bestehende `jfx`- oder Mapping-System das Problem bereits loesen soll.
- Bei UI-Aenderungen immer pruefen, ob die eigentliche Ursache im `jfx`-Framework statt in der konkreten Page liegt.
- Bei Backend-Aenderungen immer beachten, dass Frontend-Modelle, JSON-Mapping und Schemas synchron bleiben muessen.

## Test- und Laufzeitkontext
- Vite-Frontend laeuft auf `localhost:5173`.
- Backend-Endpunkte liegen unter `/service` und werden im Devbetrieb nach `localhost:8080` proxied.
- Playwright ist als Dependency vorhanden, aber Tests sind nicht prominent ausgebaut; Browserchecks deshalb gezielt und pragmatisch einsetzen.
- Vor Eingriffen, die einen Server-Neustart brauchen, immer den Nutzer fragen.

## Warum diese Datei fuer Agenten wichtig ist
- Ja, diese Datei hilft deutlich.
- Sie reduziert Fehlentscheidungen wie:
  - versehentliches Umgehen von HATEOAS-Links
  - falsche Annahmen ueber React/Vue-artige UI-Strukturen
  - rein kosmetische Loesungen fuer fachlich modellierte Sichtbarkeit
  - Aenderungen in der falschen Schicht
- Je klarer Architekturentscheidungen hier festgehalten sind, desto schneller und konsistenter koennen Agenten arbeiten.
