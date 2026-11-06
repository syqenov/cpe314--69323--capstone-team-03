# Pet Shelter & Adoption Tracker
## Project Design Document

| Field | Details |
| ----- | ----- |
| **Group Name / No.:** | *Group 3* |
| **Members:** | *Shann Michael Agang – UI Designer / Developer* |
| | *Sofia Yvonne Enovero – Team Lead / Developer* |
| | *Kyle Cheznie Taghoy – Quality Assurance* |
| | *Aeisha Mae Elizar – Database Admin* |
| | *Cleint Lausa – Developer* |
| **Instructor:** | Richyll Son |
| **Term:** | First Semester, S.Y. 2026-2027 |
| **Document Version / Date:** | *V0.1 – October 19, 2026* |

<br> 

# Table of Contents

- Pet Shelter & Adoption Tracker  ....................................................................................................................... 1
  - Table of Contents ............................................................................................................................. 2
  - Revision History ................................................................................................................................ 3
  - (1) Introduction & Project Overview ........................................................................................ 3
  - (2) Preliminary ER Sketch & Team Role ................................................................................... 4
    - Table 1: Preliminary entities and key attributes ...................................................... 5
    - Table 2: Preliminary relationships and cardinality .................................................. 5
    - Table 3: Team members and primary roles ............................................................... 5
<br>

# Revision History

| Version | Date | Author | Changes |
| :---: | :--- | :--- | :--- |
| 0.1 | Oct 22, 2026 | Sofia Yvonne Enovero | Add Section 1 One Paragraph Pitch |
| 0.1 | Oct 22, 2026 | Shann Michael Agang | Add Section 2 Entity-Relationship |
| 0.1 | Oct 22, 2026 | Kyle Cheznie Taghoy | Add ER Sketch |
| 0.1 | Oct 23, 2026 | Aeisha Mae Elizar | Update Section 2 Table 2 |
| 0.1 | Oct 23, 2026 | Cleint Lausa | Add Task Split Table |
| 0.1 | Nov. 06, 2026 | Cleint Lausa | Update design template format |


<br>

# 1. Introduction & Project Overview

The Pet Shelter & Adoption Tracker is a Java desktop application designed to help animal shelter staff manage intake, waitlists, and adoptions in one offline-first tool. Many small shelters rely on scattered spreadsheets and paper records, making it difficult to track animal stays and adoption demand. Our application centralizes these operations: it models species with distinct care schedules, manages adoption waitlists using queues, enables status undo operations via a stack, and supports bulk historical CSV log imports. Integrated pet APIs (Dog CEO API and TheCatAPI) auto-fill sample images and breed details, while built-in analytics render adoption trends and stay durations as visual bar charts to assist staff in operational planning.

<br>

# 2. Preliminary ER Sketch & Team Roles

### Table 2.1. Preliminary Entities and Key Attributes

| Entity | Key Attributes |
|---|---|
| Classification | id (PK), name, parent_id (FK → Classification.id) |
| Animal | id (PK), name, species, breed, age_months, intake_date, status, image_url, classification_id (FK → Classification.id) |
| Adopter | id (PK), name, email (unique), phone |
| AdoptionRecord | id (PK), animal_id (FK → Animal.id), adopter_id (FK → Adopter.id), adoption_date, fee_paid |
| WaitlistEntry | id (PK), animal_id (FK → Animal.id), adopter_id (FK → Adopter.id), requested_at, position |

### Table 2.2. Preliminary Relationships and Cardinality

| Parent Entity | Relationship | Child Entity | Foreign Key | Description |
|---|---|---|---|---|
| Classification | 1:M | Classification | parent_id (nullable) | Self-referencing taxonomy tree of any depth, e.g., Dog → Sporting Group → Retriever → Golden Retriever |
| Classification | 1:M | Animal | classification_id | Each animal belongs to one classification |
| Animal | 1:M | AdoptionRecord | animal_id | An animal can have adoption records |
| Adopter | 1:M | AdoptionRecord | adopter_id | An adopter can adopt multiple animals |
| Animal | 1:M | WaitlistEntry | animal_id | An animal can have many adopters waiting |

### Table 2.3. Team Members and Primary Roles

| Member | Primary Responsibility (this milestone) |
| --- | --- |
| Sofia Yvonne Enovero | Team Lead / Developer |
| Shann Michael Agang | UI Designer / Developer |
| Cleint Lausa | Developer |
| Kyle Cheznie Taghoy | QA (Quality Assurance) |
| Aeisha Mae Elizar | Database Admin |

<br>
