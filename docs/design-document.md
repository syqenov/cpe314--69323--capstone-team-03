# Pet Shelter & Adoption Tracker
## Project Design Document

| Field | Details | 
| ----- | ----- | 
| **Group Name / No.:** | *Group 3* | 
| **Members:** |  | 
|  | *Shann Michael Agang - UI Designer / Developer* | 
|  | *Sofia Yvonne Enovero - Team Lead / Developer* | 
|  | *Kyle Cheznie Taghoy - Quality Assurance* | 
|  | *Aeisha Mae Elizar - Database Admin* | 
|  | *Cleint Lausa - Developer* | 
| **Instructor:** | Richyll Son | 
| **Term:** | First Semester, S.Y. 2026-2027 | 
| **Document Version / Date:** | *V 0.3 - 11/07/2026* | 

<br>

# Table of Contents

- Pet Shelter & Adoption Tracker  ........................................................................................................................................................... 1
  - Table of Contents ................................................................................................................................................................... 2
  - Revision History ...................................................................................................................................................................... 3
  - 1. Introduction & Project Overview ............................................................................................................................ 3
  - 2. Preliminary ER Sketch & Team Role ....................................................................................................................... 4
    - Table 1: Preliminary entities and key attributes ................................................................................... 5
    - Table 2: Preliminary relationships and cardinality ............................................................................... 5
    - Table 3: Team members and primary roles ............................................................................................ 5
  - 3. System Architecture & Object-Oriented Design ............................................................................................... 6
    - 3.1 Class Hierarchy - PawTrack: Pet Shelter & Adoption Tracker .................................................. 6
    - 3.2 Single Responsibility Principle Justification Table  ....................................................................... 6
    - 3.3 Design Rationale: PawTrack ................................................................................................................. 8
  - 4. Data Model & Database Design ............................................................................................................................. 9
    - 4.1 ER Diagram ................................................................................................................................................ 9
    - 4.2 SQLite schema .......................................................................................................................................... 9
  - 5. API Integration Design.................................................................................................................................................10 
    - 5.1 API 1: Dog CEO API (https://dog.ceo/dog-api/) .........................................................................10 
    - 5.2 API 2: TheCatAPI (https://thecatapi.com/) ....................................................................................11 
    - 5.3 Error Handling...........................................................................................................................................11 
    - 5.4 Example JSON Responses.....................................................................................................................11 
  - 6. Concurrency Design.......................................................................................................................................................12 
    - 6.1 Background Operations.........................................................................................................................12 
    - 6.2 How the Background Work Runs.......................................................................................................12 
    - 6.3 Keeping the UI Responsive...................................................................................................................13 
  - 7. Data Mining & Visualization Design.......................................................................................................................13 
    - 7.1 Data Mining and Aggregation.............................................................................................................13 
    - 7.2 Recursive Classification Analysis.........................................................................................................14 
    - 7.3 Data Visualization.....................................................................................................................................14 
   <br>
## Revision History

| Version | Date | Author | Changes |
| :---: | :--- | :--- | :--- |
| 0.1 | Nov. 6, 2026 | Sofia Yvonne Enovero | Add section 1 One paragraph Pitch |
| |  | Shann Michael Agang | Add Section 2 Entity-Relationship |
|  |  | Kyle Cheznie Taghoy | Add ER Sketch |
|  |  | Aeisha Mae Elizar | Update Section 2 Table 2 |
|  |  | Cleint Lausa | Add Task Split Table |
| 0.2 | Oct. 27, 2026 | Kyle Cheznie Taghoy | Add Class Hierarchy Diagram |
| |  | Cleint Lausa | Update SRP Table |
| |  | Aeisha Mae Elizar | Add Design Rationale |
| |  | Sofia Yvonne Enovero | Update ER diagram |
|  |  | Shann Michael Agang | Add Database SQL Schema |
| 0.3 | Nov. 3, 2026 | Kyle Cheznie Taghoy | Add the subsection 5.1 and 5.2 table |
|  |  | Aeisha Elizar | Add Error handling and json example |
|  |  | Shann Michael Agang | Added Concurrency Design |
|  |  | Sofia Yvonne Enovero | Add 7.1 table and 7.2 subsection |
|  |  | Cleint Lausa | Add Data Visualization |
<br>

## 1. Introduction & Project Overview

  The Pet Shelter & Adoption Tracker is a Java desktop application designed to help animal shelter staff manage intake, waitlists, and adoptions in one offline-first tool. Many small shelters rely on scattered spreadsheets and paper records, making it difficult to track animal stays and adoption demand. Our application centralizes these operations: it models species with distinct care schedules, manages adoption waitlists using queues, and enables status undo operations via a stack. Integrated pet APIs (Dog CEO API and TheCatAPI) provide sample pet images. Bulk historical CSV log imports and built-in analytics that render adoption trends and stay durations as bar charts are planned for Milestone 2.

<br>

## 2. Preliminary ER Sketch & Team Roles
![Priliminary ERD](./erd.png)

### Table 1: Preliminary Entities and Key Attributes

| Entity | Key Attributes |
|---|---|
| Classification | id (PK), name, parent_id (FK → Classification.id) |
| Animal | id (PK), name, species, breed, age_months, intake_date, status, classification_id (FK → Classification.id) |
| Adopter | id (PK), name, email (unique), phone |
| AdoptionRecord | id (PK), animal_id (FK → Animal.id), adopter_id (FK → Adopter.id), adoption_date, fee_paid |
| WaitlistEntry | id (PK), animal_id (FK → Animal.id), adopter_id (FK → Adopter.id), requested_at, position |

### Table 2: Preliminary Relationships and Cardinality

| Parent Entity | Relationship | Child Entity | Foreign Key | Description |
|---|---|---|---|---|
| Classification | 1:M | Classification | parent_id (nullable) | Self-referencing taxonomy tree of any depth, e.g., Dog → Sporting Group → Retriever → Golden Retriever |
| Classification | 1:M | Animal | classification_id | Each animal belongs to one classification |
| Animal | 1:M | AdoptionRecord | animal_id | An animal can have adoption records |
| Adopter | 1:M | AdoptionRecord | adopter_id | An adopter can adopt multiple animals |
| Animal | 1:M | WaitlistEntry | animal_id | An animal can have many adopters waiting |
| Adopter | 1:M | WaitlistEntry | adopter_id | An adopter can wait on multiple animals |

### Table 3: Team Members and Primary Roles

| Member | Primary Responsibility (this milestone) |
| --- | --- |
| Sofia Yvonne Enovero | Team Lead / Developer |
| Shann Michael Agang | UI Designer/Developer |
| Cleint Lausa | Developer |
| Kyle Cheznie Taghoy | QA (Quality Assurance) |
| Aeisha Mae Elizar | Database Admin |

<br>

## 3. System Architecture & Object-Oriented Design
 
### 3.1 Class Hierarchy - PawTrack: Pet Shelter & Adoption Tracker
 
![Class hierarchy diagram](./class-hierarchy.png)

*Figure 3.1 Class hierarchy of PawTrack: the abstract Animal class with its Dog and Cat subclasses, the Species and AnimalStatus enumerations, and the PetImageService interface with its two API implementations.*

<br>

### 3.2 Single Responsibility Principle Justification Table
 
**Table 3.1: Class hierarchy (animals)**
 
| Class | Single responsibility | Why it follows SRP |
|---|---|---|
| **Animal (abstract)** | Holds the data every animal shares (id, name, breed, age, intake date, status, classification) and validates it. It declares the species-specific methods species(), adoptionFee(), careSchedule(), and isSenior(). | It contains no fee rules, no database code, no GUI code, and no API code. It changes only when the shared animal data or the contract changes. |
| **Dog** | Applies the dog rules: puppy, standard, and senior fees, the senior age of 84 months, and the dog care schedule. | It changes only when the dog fee or care rules change. |
| **Cat** | Applies the cat rules: kitten, standard, and senior fees, the senior age of 120 months, and the cat care schedule. | It changes only when the cat fee or care rules change. |
| **Species (enum)** | Defines the supported species (DOG, CAT) and their display label. | It changes only when a species is added. |
| **AnimalStatus (enum)** | Defines the animal's status values (INTAKE, AVAILABLE, PENDING, ADOPTED, RETURNED) and converts them to and from labels. | It changes only when the status lifecycle changes. |
 
**Table 3.2: Image API classes**
 
| Class | Single responsibility | Why it follows SRP |
|---|---|---|
| **PetImageService (interface)** | Defines what the app needs from an image source: providerName(), supports(Species), and fetchRandomImage(String). | The GUI depends on this contract, not on a specific API. |
| **DogCeoImageService** | Talks only to the Dog CEO API and parses its JSON response. | It changes only if that API changes. |
| **CatApiImageService** | Talks only to TheCatAPI (including the optional API key) and parses its JSON response. | It changes only if that API changes. |
| **PetImageServiceFactory** | Chooses the right PetImageService for a species. | Selection logic stays out of the GUI and the services. |
| **PetImageResult** | Holds one fetched image result (URL, species, breed, provider). | It is data only, with no behavior. |
| **HttpJson** | Performs the HTTP GET request and returns the JSON text. | Network code is in one place, not repeated in each service. |
 
**Table 3.3: Services and data structures**
 
| Class | Single responsibility | Why it follows SRP |
|---|---|---|
| **WaitlistService** | Manages each animal's first-come, first-served queue of interested adopters. | It holds only queue logic, with no database or GUI code. |
| **StatusHistoryService** | Manages the undo stack of status changes. | It holds only stack logic. |
| **StatusChange** | Holds one status change (animal, from, to) for the undo stack. | Data only. |
| **ClassificationService** | Searches the classification tree recursively and builds the breed paths. | It holds only tree-search logic. |
| **ClassificationNode** | Represents one node in the classification tree and walks its children recursively. | It models one tree node and nothing else. |
| **ShelterSeeder** | Fills an empty database with sample animals. | Demo data is kept out of the other classes. |

**Table 3.4: Database layer**
 
| Class | Single responsibility | Why it follows SRP |
|---|---|---|
| **Database** | Opens the SQLite connection, enables foreign keys, and creates the tables. | Connection and schema setup in one place. |
| **AnimalDao, AdopterDao, AdoptionRecordDao, ClassificationDao, WaitlistDao** | Each runs the SQL for one table, using PreparedStatement for every query that takes input. | One class per table, so a change to one table doesn't touch the others. |
| **Adopter, AdoptionRecord** | Hold adopter and adoption data. | Data only. |
 
**Table 3.5: GUI layer**
 
| Class | Single responsibility | Why it follows SRP |
|---|---|---|
| **MainFrame** | Builds the window and connects the controls to the services and DAOs. | It handles screen events and delegates the real work to other classes. |
| **AnimalTableModel** | Feeds animal data to the table. | Display only. |
| **ImageWorker** | Downloads the sample image on a background thread, so the window doesn't freeze. | Threading for images is in one class. |

<br>

### 3.3 Design Rationale: PawTrack
 
The system is built around an abstract Animal class that stores the data every animal shares (id, name, breed, age in months, intake date, status, and classification) and validates it. It declares the behavior that differs by species: species(), adoptionFee(), careSchedule(), and isSenior(). Dog and Cat extend Animal and implement only their own rules: the fee for juvenile, standard, and senior animals, the senior age threshold, and the care schedule. The rest of the application works with Animal references and calls these methods without checking the species, so the same code serves both. A static Animal.create() factory builds the right subclass from a Species, and two enums, Species and AnimalStatus, keep the allowed species and status values in one place.
 
This follows the Single Responsibility Principle. Animal models shared animal data and the contract, and each subclass owns the rules for one species. None of them contains database, GUI, or API code. Those concerns are in separate classes: DAO classes, one per table, run the SQL for their table and use PreparedStatement for every query that takes input. Services hold the data-structure logic: WaitlistService keeps a first-in, first-out queue of interested adopters, StatusHistoryService keeps the undo stack, and ClassificationService searches the classification tree recursively. Pet images are fetched through the PetImageService interface, with DogCeoImageService and CatApiImageService each responsible for one external API and its JSON, and PetImageServiceFactory choosing the right one. The GUI depends only on the interface, so an API can be changed, replaced, or fail without touching the animal classes, and ImageWorker keeps downloads off the Swing event thread so the window stays responsive.

<br>

## 4. Data Model & Database Design

### 4.1 ER Diagram
 
![Finalized ERD](./er-diagram.png)
 
*Figure 4.1 Finalized ERD. PK = primary key, FK = foreign key, NN = NOT NULL*

This section supersedes the preliminary ER sketch in Section 2.

Notes on the diagram:

- `parent_id` in `classification` and `classification_id` in `animal` may be empty (NULL): a top-level classification has no parent, and an animal may have no classification.
- `waitlist_entry` has a unique pair (`animal_id`, `adopter_id`), so an adopter can join the waitlist of an animal only once.
- The foreign keys in `adoption_record` and `waitlist_entry`, and `parent_id` in `classification`, use `ON DELETE CASCADE`.

<br>

### 4.2 SQLite Schema

The database file is `pawtrack.db`. It is created automatically by `Database.java` on first run, and the app runs `PRAGMA foreign_keys = ON` on every connection.

*Listing 4.1 SQLite schema (CREATE TABLE statements)*

```sql
CREATE TABLE IF NOT EXISTS classification (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  parent_id INTEGER REFERENCES classification(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS adopter (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  email TEXT NOT NULL COLLATE NOCASE UNIQUE,
  phone TEXT
);

CREATE TABLE IF NOT EXISTS animal (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  species TEXT NOT NULL,
  breed TEXT NOT NULL,
  age_months INTEGER NOT NULL CHECK (age_months >= 0),
  intake_date TEXT NOT NULL,
  status TEXT NOT NULL,
  classification_id INTEGER REFERENCES classification(id)
);

CREATE TABLE IF NOT EXISTS adoption_record (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  animal_id INTEGER NOT NULL REFERENCES animal(id) ON DELETE CASCADE,
  adopter_id INTEGER NOT NULL REFERENCES adopter(id) ON DELETE CASCADE,
  adoption_date TEXT NOT NULL,
  fee_paid REAL NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS waitlist_entry (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  animal_id INTEGER NOT NULL REFERENCES animal(id) ON DELETE CASCADE,
  adopter_id INTEGER NOT NULL REFERENCES adopter(id) ON DELETE CASCADE,
  requested_at TEXT NOT NULL,
  position INTEGER NOT NULL,
  UNIQUE(animal_id, adopter_id)
);
```

5. API Integration Design

PawTrack uses two public REST APIs to retrieve sample pet images: Dog CEO for dogs and TheCatAPI for cats. Both follow the PetImageService interface, and PetImageServiceFactory selects the service from the animal's species. Every call goes through HttpJson, which sends an HTTP GET with an 8-second connect timeout and an 8-second read timeout. The JSON is parsed with Gson into small DTO records (DogCeoResponse, CatImageDto), and the Dog CEO breed catalogue is read with org.json. The service returns a PetImageResult (image URL, species, breed, provider). ImageWorker then downloads the image bytes and the Sample Pet Image panel shows the picture with the provider and the animal's recorded breed.

5.1 API 1: Dog CEO API (https://dog.ceo/dog-api/)

The Dog CEO API provides dog images and a catalogue of breeds. It needs no API key.

Table 11: Dog CEO API integration

Endpoint (HTTP GET)	JSON response shape	Parsing and use in PawTrack
https://dog.ceo/api/breeds/list/all	message: object mapping each breed to a list of sub-breeds; status.	Read with org.json and flattened recursively into breed entries, e.g. retriever + golden becomes "Golden Retriever". Used to find the API path matching the animal's recorded breed.
https://dog.ceo/api/breed/{path}/images/random	message: one image URL string; status.	Parsed into DogCeoResponse; the URL is stored in a PetImageResult. For example, retriever/golden returns a Golden Retriever image.
https://dog.ceo/api/breeds/image/random	message: a random dog image URL; status.	Used when the animal's breed is blank, so an image is still shown. If the breed is not in the catalogue (or the catalogue cannot be loaded), the service guesses a path from the last word of the breed name instead.
5.2 API 2: TheCatAPI (https://thecatapi.com/)

TheCatAPI provides cat images with breed information. It supports API-key authentication and also works without a key at a low rate limit. When configured, the key is read from the cat.api.key system property or the CAT_API_KEY environment variable and sent in the x-api-key header.

Table 12: TheCatAPI integration

Endpoint (HTTP GET)	JSON response shape	Parsing and use in PawTrack
https://api.thecatapi.com/v1/breeds	Array of breed objects with id and name.	Parsed into an id-to-name map and searched (ignoring case) for the animal's recorded breed.
https://api.thecatapi.com/v1/images/search?limit=1&breed_ids={id}	Array with one object: id, url, width, height, and a breeds array.	Parsed into CatImageDto; the first image URL is stored in a PetImageResult. This returns an image of the matching breed.
https://api.thecatapi.com/v1/images/search?limit=1&has_breeds=1	Same array shape.	Used when the breed is blank or not found, so a random cat image is shown. The caption shows the provider and the animal's own recorded breed.
5.3 Error Handling

PawTrack keeps working when an external API is unavailable. Image requests run in the background (see Section 6), and the GUI handles these cases:

Slow or unreachable service: the 8-second connect and read timeouts stop the request from waiting forever.
Unsuccessful HTTP response: a status outside 200 to 299 is reported as an error that includes the status code.
Invalid or missing JSON data: a response that cannot be parsed, has a non-success status, or has no image URL is reported as an error (for example, "TheCatAPI returned no image").
Image not available: the panel shows "Image unavailable", the status bar says the fetch failed, and the problem is written to the Activity Log.
Missing breed: the Dog CEO service falls back to a general random dog image, and TheCatAPI falls back to a random cat image.

A failed request never stops the rest of the application. Recording interest, marking animals adopted, importing history, filtering, undoing status changes, and viewing analytics all use the local SQLite database and do not depend on either API.

5.4 Example JSON Responses

Dog CEO /breeds/list/all: each breed maps to a list of sub-breeds.

json
{
  "message": {
    "labrador": [],
    "retriever": ["chesapeake", "curly", "flatcoated", "golden"]
  },
  "status": "success"
}

Dog CEO /breed/{path}/images/random and /breeds/image/random: both return one image URL in message.

json
{
  "message": "https://images.dog.ceo/breeds/retriever-golden/n02099601_1024.jpg",
  "status": "success"
}

TheCatAPI /images/search?limit=1&has_breeds=1: an array with one image object.

json
[
  {
    "id": "aXYvRd7Do",
    "url": "https://cdn2.thecatapi.com/images/aXYvRd7Do.jpg",
    "width": 1445,
    "height": 1080,
    "breeds": [ { "id": "abys", "name": "Abyssinian" } ]
  }
]
6. Concurrency Design

PawTrack uses Java Swing's SwingWorker to run long operations on background threads, so the Event Dispatch Thread (EDT), which handles clicks and repainting, is not blocked.

6.1 Background Operations

Table 13: Background operations and threading approach

Operation	Background worker	Threading approach
Bulk CSV import	SwingWorker created in ImportController.importCsv() (ImportWorker is an equivalent stand-alone class)	doInBackground() calls CsvImportService.importFile(), which reads and validates each row and saves animals, adopters, and adoption records to SQLite in one transaction (committed at the end, rolled back on failure). Progress is reported as a percentage every 250 rows. done() runs on the EDT and reads the result with get().
Pet image retrieval	ImageWorker	SwingWorker<ImageWorker.Result, Void>. doInBackground() asks the PetImageService for an image URL, downloads the image bytes, and scales the image. The GUI listens for the worker's DONE state.
Analytics refresh	None (runs on the EDT)	AnalyticsController.compute() reads the local SQLite data and makes one in-memory pass. It runs after data changes, when Refresh Analytics is clicked, and with F5.

Figure 4

Figure 4: Which steps run on the EDT (blue) and which on a worker thread (orange) for CSV import and image retrieval.

6.2 How the Background Work Runs

The import is the longest operation. The worker computes the percentage from the number of rows processed and the number of lines in the file, and reports it every 250 rows. The image worker makes two network calls, one for the image URL and one for the image itself, and neither runs on the EDT.

6.3 Keeping the UI Responsive
While an import runs: the status bar says the import is running on a background thread, and the progress bar shows the percentage.
When a worker finishes: for an import, the GUI reads the result, shows the summary in the status bar and the Activity Log, then refreshes the animal table and the analytics. For an image, it shows the picture, the provider, and the breed.
If a worker fails: the GUI shows an "Import failed" message or "Image unavailable", and the application continues.

Because the long work runs off the EDT, the window keeps repainting and accepting clicks during an import or an image download.

7. Data Mining & Visualization Design

PawTrack uses AnalyticsService to analyze the animal and adoption records stored in the SQLite database. It performs frequency analysis to find the most adopted species and breeds, calculates average shelter stay, and aggregates adoption counts through the classification hierarchy. The results are returned as an AnalyticsReport and shown on the analytics dashboard. The sample intake log in data/intake_history.csv contains 6,000 records, so the charts reflect real volumes of data.

7.1 Data Mining and Aggregation

The service matches each completed adoption record with its animal, groups and counts the records by species and breed, calculates stay durations, and performs a recursive roll-up through the classification tree.

Table 14: Data mining and aggregation operations

Analysis	Aggregation method	Purpose
Adoptions by Species	Counts completed adoption records for each species and sorts from highest to lowest (name breaks ties).	Identifies the species with the most adoptions.
Adoptions by Breed	Counts completed adoption records for each breed and ranks them by frequency.	Identifies the most adopted breeds.
Average Shelter Stay	Calculates the days between an animal's intake date and adoption date (negative values from bad data count as 0), then computes the overall and per-species averages.	Shows how long animals usually stay before adoption.
Adoptions by Classification	Recursively counts animals with status ADOPTED under each classification node, including all descendant nodes.	Gives adoption totals across the classification hierarchy.

Figure 5

Figure 5: Data flow from the database to the chart.

The analysis reads the live records each time it runs. The analytics refresh after interest is recorded, an animal is adopted, a status is undone, or a CSV file is imported, and whenever Refresh Analytics is clicked, so the results and charts change whenever the underlying data changes.

7.2 Recursive Classification Analysis

ClassificationService and ClassificationNode organize breeds into a tree stored in the classification table (each row points to its parent). The tree has no fixed depth, for example: Dog > Sporting Group > Retriever > Golden Retriever. Recursive methods search for classifications by name or by full path, build paths, and walk the tree. During analytics, AnalyticsService.adoptionCountByClassification() calls itself on each child node and adds the children's totals to the node's own count, so each node reports its own adopted animals plus all of its descendants. This lets staff compare adoption patterns at different levels of the hierarchy instead of only by species or breed name.

7.3 Data Visualization

PawTrack uses a bar chart, drawn by the BarChartPanel class, because a bar chart compares a number across separate categories such as species, breeds, and classifications. Each chart has a title, a labeled category axis, a labeled value axis, and value labels on the bars. No legend is needed because each chart shows one series. AnalyticsPanel provides a metric dropdown and a Refresh Analytics button; if there is no adoption history yet, the chart says to import the CSV intake log.

Table 15: Analytics dashboard visualizations

Chart (dropdown choice)	Data displayed	Axis labels	Purpose
Adoptions by Species	Completed adoptions per species	Species / Adoptions	Compare adoption frequency
Top Adopted Breeds (Top 8)	The eight breeds with the most adoptions	Breed / Adoptions	Highlight the most adopted breeds
Average Shelter Stay (days) by Species	Average shelter days per species	Species / Days	Compare stay length
Adoptions by Classification (recursive)	Adoptions per classification, including descendants (up to 12 non-zero entries)	Classification / Adoptions	Show adoption spread across the hierarchy

The dashboard also shows a text summary: total animals, completed adoptions, most adopted species and breed, average shelter stay, and adopters currently on waitlists.