# cpe314--69323--capstone-team-03
CPE 314 capstone project — Pet Shelter &amp; Adoption Tracker

1. One paragraph Pitch - Enovero 
The Pet Shelter & Adoption Tracker is an offline-first Java desktop application designed to help animal shelter staff and volunteers transition away from scattered spreadsheets and paper records into a centralized system. The app streamlines daily operations by modeling species with custom care schedules, managing adoption waitlists using queues, supporting status undo actions via a stack, and enabling historical log imports from CSV files. Integrated pet APIs (Dog CEO API and TheCatAPI) auto-fill sample images and breed details, while built-in analytics render adoption trends and stay durations as visual bar charts. Ultimately, the 
system empowers shelter teams to make data-driven decisions and efficiently manage animals from intake through final adoption. 

2. Entity-Relationship Sketch - Agang

(List your entities, their key attributes, and how they relate — text notation is fine, e.g., Member(id, name)
--1:M--> Loan(id, memberId FK, bookId FK) <--M:1-- Book(id, title))

Entity
Key Attributes
Classification
id (PK), name,  parent_id (FK → Classification.id)
Animal
id (PK), name, species, breed, age_months, intake_date, status, image_url  classification_id (FK → Classification.id)
Adopter
id (PK), name, email (unique), phone
AdoptionRecord
id (PK), animal_id (FK → Animal.id), adopter_id (FK → Adopter.id), adoption_date, fee_paid
WaitlistEntry
id (PK), animal_id (FK → Animal.id), adopter_id (FK → Adopter.id), requested_at, position


Parent Entity 
Relationship 
Child Entity 
Foreign Key 
Description
Classification			
1:M
Classification	
parent_id (nullable)
Self-referencing taxonomy tree of any depth, e.g. Dog > Sporting Group > Retriever > Golden Retriever
Classification
1:M
Animal
classification_id
Each animal belongs to one classification
Animal
1:M
AdoptionRecord
animal_id
An animal can have adoption records
Adopter
1:M
AdoptionRecord
adopter_id
An adopter can adopt multiple animals
Animal
1:M
WaitlistEntry
animal_id
An animal can have many adopters waiting
Adopter
1:M
WaitlistEntry
adopter_id
An adopter can wait on multiple animals

## 4. Task Split - Elizar

| Member | Primary Responsibility (this milestone) |
|----------|----------------------------------------|
| **Sofia Yvonne Enovero** | OOD model and class hierarchy, Queue/Stack implementation, recursive tree search |
| **Shann Michael Agang** | Swing GUI and event handlers |
| **Cleint Lausa** | SQLite/JDBC data layer and DAOs |
| **Kyle Cheznie Taghoy** | REST API client, JSON parsing, and CSV parsing/validation |
| **Aeisha Mae Elizar** | Concurrency (import/API workers), analytics, and chart generation |

> **Note:** Each team member is responsible for writing tests for their own module.

---

## 5. Public APIs You Plan to Use

### Dog API
- **Website:** https://dog.ceo/dog-api/documentation
- **Purpose:** Provides free dog images and breed lists without requiring an API key.

### The Cat API
- **Website:** https://thecatapi.com
- **Purpose:** Provides cat images and breed information; most features require a free API key.

---

## 6. Risks / Open Questions

The following items require further clarification or may present implementation challenges:

- How should the application handle situations when an external API becomes unavailable or experiences downtime?
- Should the SQLite database file be stored within the project directory or in a separate location?
- Do the JUnit 5 tests need to run successfully without a database connection or network access?
