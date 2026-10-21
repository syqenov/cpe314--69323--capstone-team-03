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

3. Feature Checklist - Taghoy, Cheznie
Check off which mandatory requirement each planned feature will satisfy:

| Planned Feature | Requirement(s) It Satisfies |
|----------------|----------------------------|
| Animal types: Dog, Cat, Other (one abstract class + three concrete classes) | **Object-Oriented Design:** Creates a main Animal blueprint with specialized sub-types that calculate their own adoption fees and care schedules. |
| Queue waitlist per animal (first person in line gets served first) | **Data Structure / Recursion:** Uses a Queue (first-come, first-served line) for people waiting to adopt. |
| Stack to undo last status change | **Data Structure / Recursion:** Uses a Stack, like a Ctrl+Z for status changes, powering the "Undo Last Action" button. |
| Tree for breeds (Dog > Sporting Group > Retriever > Golden Retriever) searched with recursion | **Data Structure / Recursion:** Uses a recursive search that digs through multi-level breed categories no matter how many levels deep. |
| JUnit 5 tests (at least 6): fee calculation per subtype, queue order, undo on an empty stack, recursive search at shallow and deep levels | **Automated Testing:** Tests normal and edge cases for the core logic classes. |
| SQLite database with PreparedStatement for animals, adopters, and adoption records | **Data Modeling + Database:** Stores shelter animals, adopters, and adoption history locally using SQLite and JDBC. |
| Call Dog CEO API and TheCatAPI, read JSON, use images/breeds in app | **REST API Consumption:** Retrieves live animal data from public APIs and displays the parsed information in the application. |
| Swing GUI with buttons and combo boxes (list animal, import CSV, record interest, mark adopted, view analytics) | **Event-Driven GUI:** Provides an interactive interface for shelter staff through buttons, forms, and lists. |
| Import historical intake/adoption CSV on a background thread (SwingWorker) | **Concurrency:** Runs long imports in the background so the application remains responsive. |
| Analyze most-adopted breed/species and average shelter stay length | **Data Mining:** Performs aggregation and frequency analysis on shelter data. |
| Bar chart of the mining results, with labeled axes and title | **Data Visualization:** Displays data mining results in a properly labeled chart. |
