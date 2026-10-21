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

