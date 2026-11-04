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
| **Document Version / Date:** | *V 0.2 - 10/27/2026* | 

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
| 0.1 | Oct 22, 2026 | Sofia Yvonne Enovero | Add section 1 One paragraph Pitch |
| 0.1 | Oct 22, 2026 | Shann Michael Agang | Add Section 2 Entity-Relationship |
| 0.1 | Oct 22, 2026 | Kyle Cheznie Taghoy | Add ER Sketch |
| 0.1 | Oct 23, 2026 | Aeisha Mae Elizar | Update Section 2 Table 2 |
| 0.1 | Oct 23, 2026 | Cleint Lausa | Add Task Split Table |
| 0.2 | Oct. 27, 2026 | Kyle Cheznie Taghoy | Add Class Hierarchy Diagram |
| 0.2 | Oct. 27, 2026 | Cleint Lausa | Update SRP Table |
| 0.2 | Oct. 27, 2026 | Aeisha Mae Elizar | Add Design Rationale |
| 0.2 | Oct. 27, 2026 | Sofia Yvonne Enovero | Update ER diagram |
| 0.2 | Oct. 27, 2026 | Shann Michael Agang | Add Database SQL Schema |
| 0.3 | Nov. 2, 2026 | Kyle Cheznie Taghoy | Add the subsection 5.1 and 5.2 table |
| 0.3 | Nov. 2, 2026 | Aeisha Elizar | Add Error handling and json example |
| 0.3 | Nov. 2, 2026 | Shann Michael Agang | Added Concurrency Design
<br>

## Introduction & Project Overview

  The Pet Shelter & Adoption Tracker is a Java desktop application designed to help animal shelter staff manage intake, waitlists, and adoptions in one offline-first tool. Many small shelters rely on scattered spreadsheets and paper records, making it difficult to track animal stays and adoption demand. Our application centralizes these operations: it models species with distinct care schedules, manages adoption waitlists using queues, enables status undo operations via a stack, and supports bulk historical CSV log imports. Integrated pet APIs (Dog CEO API and TheCatAPI) auto-fill sample images and breed details, while built-in analytics render adoption trends and stay durations as visual bar charts to assist staff in operational planning. 

<br>

## Preliminary ER Sketch & Team Roles
![Sketch ERD](./erd.png)
### Table 1: Preliminary Entities and Key Attributes

| Entity | Key Attributes |
|---|---|
| Classification | id (PK), name, parent_id (FK → Classification.id) |
| Animal | id (PK), name, species, breed, age_months, intake_date, status, image_url, classification_id (FK → Classification.id) |
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

### Table 3: Team members and primary roles

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

<br>

### 3.2 Single Responsibility Principle Justification Table
 
**Class hierarchy (animals)**
 
| Class | Single responsibility | Why it follows SRP |
|---|---|---|
| **Animal (abstract)** | Holds the data every animal shares (id, name, breed, age, intake date, status, classification) and validates it. It declares the species-specific methods species(), adoptionFee(), careSchedule(), and isSenior(). | It contains no fee rules, no database code, no GUI code, and no API code. It changes only when the shared animal data or the contract changes. |
| **Dog** | Applies the dog rules: puppy, standard, and senior fees, the senior age of 84 months, and the dog care schedule. | It changes only when the dog fee or care rules change. |
| **Cat** | Applies the cat rules: kitten, standard, and senior fees, the senior age of 120 months, and the cat care schedule. | It changes only when the cat fee or care rules change. |
| **Species (enum)** | Defines the supported species (DOG, CAT) and their display label. | It changes only when a species is added. |
| **AnimalStatus (enum)** | Defines the animal's status values (INTAKE, AVAILABLE, PENDING, ADOPTED, RETURNED) and converts them to and from labels. | It changes only when the status lifecycle changes. |
 
**Image API classes**
 
| Class | Single responsibility | Why it follows SRP |
|---|---|---|
| **PetImageService (interface)** | Defines what the app needs from an image source: providerName(), supports(Species), and fetchRandomImage(String). | The GUI depends on this contract, not on a specific API. |
| **DogCeoImageService** | Talks only to the Dog CEO API and parses its JSON response. | It changes only if that API changes. |
| **CatApiImageService** | Talks only to TheCatAPI (including the optional API key) and parses its JSON response. | It changes only if that API changes. |
| **PetImageServiceFactory** | Chooses the right PetImageService for a species. | Selection logic stays out of the GUI and the services. |
| **PetImageResult** | Holds one fetched image result (URL, species, breed, provider). | It is data only, with no behavior. |
| **HttpJson** | Performs the HTTP GET request and returns the JSON text. | Network code is in one place, not repeated in each service. |
 
**Services and data structures**
 
| Class | Single responsibility | Why it follows SRP |
|---|---|---|
| **WaitlistService** | Manages each animal's first-come, first-served queue of interested adopters. | It holds only queue logic, with no database or GUI code. |
| **StatusHistoryService** | Manages the undo stack of status changes. | It holds only stack logic. |
| **StatusChange** | Holds one status change (animal, from, to) for the undo stack. | Data only. |
| **ClassificationService** | Searches the classification tree recursively and builds the breed paths. | It holds only tree-search logic. |
| **ClassificationNode** | Represents one node in the classification tree and walks its children recursively. | It models one tree node and nothing else. |
| **ShelterSeeder** | Fills an empty database with sample animals. | Demo data is kept out of the other classes. |

**Database layer**
 
| Class | Single responsibility | Why it follows SRP |
|---|---|---|
| **Database** | Opens the SQLite connection, enables foreign keys, and creates the tables. | Connection and schema setup in one place. |
| **AnimalDao, AdopterDao, AdoptionRecordDao, ClassificationDao, WaitlistDao** | Each runs the SQL (PreparedStatement) for one table. | One class per table, so a change to one table doesn't touch the others. |
| **Adopter, AdoptionRecord** | Hold adopter and adoption data. | Data only. |
 
**GUI layer**
 
| Class | Single responsibility | Why it follows SRP |
|---|---|---|
| **MainFrame** | Builds the window and connects the controls to the services and DAOs. | It handles screen events and delegates the real work to other classes. |
| **AnimalTableModel** | Feeds animal data to the table. | Display only. |
| **ImageWorker** | Downloads the sample image on a background thread, so the window doesn't freeze. | Threading for images is in one class. |

<br>

### 3.3 Design Rationale: PawTrack
 
The system is built around an abstract Animal class that stores the data every animal shares (id, name, breed, age in months, intake date, status, and classification) and validates it. It declares the behavior that differs by species: species(), adoptionFee(), careSchedule(), and isSenior(). Dog and Cat extend Animal and implement only their own rules: the fee for juvenile, standard, and senior animals, the senior age threshold, and the care schedule. The rest of the application works with Animal references and calls these methods without checking the species, so the same code serves both. A static Animal.create() factory builds the right subclass from a Species, and two enums, Species and AnimalStatus, keep the allowed species and status values in one place.
 
This follows the Single Responsibility Principle. Animal models shared animal data and the contract, and each subclass owns the rules for one species. None of them contains database, GUI, or API code. Those concerns are in separate classes: DAO classes, one per table, run the SQL with PreparedStatement. Services hold the data-structure logic: WaitlistService keeps a first-in, first-out queue of interested adopters, StatusHistoryService keeps the undo stack, and ClassificationService searches the classification tree recursively. Pet images are fetched through the PetImageService interface, with DogCeoImageService and CatApiImageService each responsible for one external API and its JSON, and PetImageServiceFactory choosing the right one. The GUI depends only on the interface, so an API can be changed, replaced, or fail without touching the animal classes, and ImageWorker keeps downloads off the Swing event thread so the window stays responsive.

<br>

## 4. Data Model & Database Design

### 4.1 ER Diagram
 
![Finalized ERD](./er-diagram.png)
 
*Figure 4.1 Finalized ERD. PK = primary key, FK = foreign key, NN = NOT NULL*

This section supersedes the preliminary ER sketch in Section 2.

<br>

### 4.2 SQLite schema
-- Database file: pawtrack.db (created automatically by Database.java on first run)

-- The app runs PRAGMA foreign_keys = ON on every connection.

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

## 5. API Integration Design

The Pet Shelter & Adoption Tracker (PawTrack) consumes two public REST APIs to fetch sample pet images. The integrations are hidden behind a common PetImageService interface, and PetImageServiceFactory chooses the right one from the animal’s species. Each service sends an HTTP GET request through HttpJson, parses the JSON response with org.json, and returns the result as a PetImageResult (image URL, species, breed, and provider name). The GUI shows the image and provider in the “Sample Pet Image” panel when staff select an animal and click Fetch Sample Pet Image.

### 5.1 API 1: Dog CEO API (https://dog.ceo/dog-api/)

The Dog CEO API provides dog images and a catalogue of breeds. It needs no API key.

**Table 4. Dog CEO API Integration**

| **Endpoint (HTTP GET)** | **JSON Response Shape** | **Parsing and Use in PawTrack** |
| --- | --- | --- |
| https://dog.ceo/api/breeds/list/all | message: an object that maps each breed to a list of sub-breeds. status: request status. | The app reads the object and converts breed and sub-breed pairs into readable names, for example retriever + golden becomes “Golden Retriever”. It uses this list to find the API path that matches the animal’s recorded breed. |
| https://dog.ceo/api/breed/{path}/images/random | message: a string containing one image URL.  status: request status. | The app extracts the image URL and stores it in a PetImageResult. For example, the path retriever/golden returns a Golden Retriever image. |
| https://dog.ceo/api/breeds/image/random | message: a string containing a random dog image URL. status: request status. | The app uses this when the animal’s breed is blank, is not in the breed list, or the breed list cannot be loaded, so a dog image is still shown. |

### 5.2 API 2: TheCatAPI (https://thecatapi.com/)

TheCatAPI provides cat images with breed information. It supports API-key authentication but also works without a key at a low rate limit. When configured, the key is read from the CAT_API_KEY environment variable or the cat.api.key system property and sent in the x-api-key header.

**Table 5. TheCatAPI Integration**

| **Endpoint (HTTP GET)** | **JSON Response Shape** | **Parsing and Use in PawTrack** |
| --- | --- | --- |
| https://api.thecatapi.com/v1/images/search?limit=1&has_breeds=1 | A JSON array with one object containing the image id, url, width, height, and a breeds array. | The app parses the array with org.json, extracts the image url, and stores it in a PetImageResult. The image is a random cat image, not matched to the animal’s breed. The caption below the image shows the provider and the animal’s own recorded breed. |

### 5.3 Error Handling

PawTrack keeps working when an external API is unavailable. The image requests run in the background (see Section 6), and the GUI handles these cases:

- Slow or unreachable service: connection and read timeouts stop the request from waiting forever.

- Unsuccessful HTTP response: a status outside 200 to 299 is reported as an error that includes the status code.

- Invalid or missing JSON data: a response that cannot be parsed, or that has no image URL, is reported as an error.

- Image not available: the panel shows “Image unavailable”, the status bar says the fetch failed, and the problem is written to the Activity Log.

- Missing breed: the Dog CEO service falls back to a general random dog image.

A failed request never stops the rest of the application. Recording interest, marking animals adopted, importing history, filtering, undoing status changes, and viewing analytics all use the local SQLite database and do not depend on either API.

### 5.4 Example JSON Responses

- **Dog CEO API, `/breeds/list/all`**: each breed maps to a list of its sub-breeds.

```json
{
  "message": {
    "labrador": [],
    "retriever": ["chesapeake", "curly", "flatcoated", "golden"]
  },
  "status": "success"
}
```

- **Dog CEO API, `/breed/{path}/images/random` and `/breeds/image/random`**: both return one image URL in `message`.

```json
{
  "message": "https://images.dog.ceo/breeds/retriever-golden/n02099601_1024.jpg",
  "status": "success"
}
```

- **TheCatAPI, `/images/search?limit=1&has_breeds=1`**: an array with one image object.

```json
[
  {
    "id": "aXYvRd7Do",
    "url": "https://cdn2.thecatapi.com/images/aXYvRd7Do.jpg",
    "width": 1445,
    "height": 1080,
    "breeds": [
      { "id": "abys", "name": "Abyssinian" }
    ]
  }
]
```
## 6. Concurrency Design

PawTrack uses Java Swing’s SwingWorker to run long operations on background threads, so the Event Dispatch Thread (EDT), which handles clicks and repainting, never blocks.

### 6.1 Background Operations

*Table 6. Background Operations and Threading Approach*

| *Operation* | *Background Worker* | *Threading Approach* |
| --- | --- | --- |
| Bulk CSV Import | ImportWorker | SwingWorker<CsvImportResult, Void>. doInBackground() calls CsvImportService.importFile(), which reads and validates each row and saves the animals, adopters, and adoption records to SQLite. It reports progress with setProgress(). |
| Pet Image Retrieval | ImageWorker | SwingWorker<ImageWorker.Result, Void>. doInBackground() asks the PetImageService for an image URL, downloads the image bytes, and scales the image. It returns the image and its provider information. |

### 6.2 How the Background Work Runs

The import is the longest operation. ImportWorker runs it in doInBackground() and publishes a percentage (0 to 100) through the standard progress property. The image worker makes two network calls, one for the image URL and one for the image itself. Neither runs on the EDT.

### 6.3 Keeping the UI Responsive

Swing components may only be changed on the EDT. SwingWorker delivers its progress and state change events on the EDT, so the GUI listens for them:

- While the import runs: the progress event moves the progress bar and its text, and the status bar says the import is running.

- When a worker is finished: the state changes to DONE and the GUI calls get() to read the result. For an import, it shows the summary in the status bar and Activity Log, then refreshes the animal table and the analytics. For an image, it shows the picture and provider.

- If a worker fails: the GUI shows an error message or “Image unavailable” and the application continues.

Because the long work is off the EDT, the window keeps repainting and accepting clicks during an import or an image download, so it never freezes.


