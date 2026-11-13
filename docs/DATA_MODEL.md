# Data Model & Database Design

## ER Diagram

```
+-------------------+
|  Classifications  |
+-------------------+
| id (PK)           |<---+ (parent_id FK)
| name              |----+ (recursive 1:M)
+-------------------+
          | 1
          |
          | M
+-------------------+
|      Animal       |
+-------------------+
| id (PK)           |
| name              |
| species           |
| breed             |
| age_months        |
| intake_date       |
| status            |
| classification_id (FK)
+-------------------+
          | 1
          |
          | M
+-------------------+
|  Adoption Record  |
+-------------------+
| id (PK)           |
| animal_id (FK)    |<----+
| adopter_id (FK)   |     |
| adoption_date     |     |
| fee_paid          |     |
+-------------------+     |
          | M             | 1:M
          |               |
          | 1             |
+-------------------+     |
|      Adopter      |     |
+-------------------+     |
| id (PK)           |     |
| name              |     |
| email (UQ)        |     |
| phone             |     |
+-------------------+     |
          | 1             |
          |               |
          | M             |
+-------------------+     |
|   WaitlistEntry   |     |
+-------------------+     |
| id (PK)           |     |
| animal_id (FK)    |-----+
| adopter_id (FK)   |
| requested_at      |
| position          |
+-------------------+
```

## SQLite Schema

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
