# Database Schema

This file lists the tables used to save the app's data in SQLite.
Each table is like a spreadsheet. Columns are the fields. Keys link tables together.

```sql
-- Table for animal types/groups (Dog, Cat, and their breeds). A row can point to its parent row (tree).
CREATE TABLE IF NOT EXISTS classification (
    id INTEGER PRIMARY KEY AUTOINCREMENT,        -- unique number for this row
    name TEXT NOT NULL,                          -- name of the group or breed
    parent_id INTEGER REFERENCES classification(id) ON DELETE CASCADE  -- parent group (null for top level)
);

-- Table for people who adopt animals.
CREATE TABLE IF NOT EXISTS adopter (
    id INTEGER PRIMARY KEY AUTOINCREMENT,        -- unique number for this person
    name TEXT NOT NULL,                         -- person's name
    email TEXT NOT NULL COLLATE NOCASE UNIQUE,   -- email (must be unique, case-insensitive)
    phone TEXT                                  -- phone number (optional)
);

-- Table for animals in the shelter.
CREATE TABLE IF NOT EXISTS animal (
    id INTEGER PRIMARY KEY AUTOINCREMENT,        -- unique number for this animal
    name TEXT NOT NULL,                         -- animal's name
    species TEXT NOT NULL,                      -- Dog or Cat
    breed TEXT NOT NULL,                        -- breed name
    age_months INTEGER NOT NULL CHECK (age_months >= 0),  -- age in months
    intake_date TEXT NOT NULL,                  -- date the animal arrived
    status TEXT NOT NULL,                       -- current status (intake, available, adopted, etc.)
    classification_id INTEGER REFERENCES classification(id)  -- link to classification group
);

-- Table for completed adoptions.
CREATE TABLE IF NOT EXISTS adoption_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,        -- unique number for this record
    animal_id INTEGER NOT NULL REFERENCES animal(id) ON DELETE CASCADE,   -- which animal
    adopter_id INTEGER NOT NULL REFERENCES adopter(id) ON DELETE CASCADE, -- which person
    adoption_date TEXT NOT NULL,                 -- date of adoption
    fee_paid REAL NOT NULL DEFAULT 0             -- fee paid in dollars
);

-- Table for people waiting to adopt an animal.
CREATE TABLE IF NOT EXISTS waitlist_entry (
    id INTEGER PRIMARY KEY AUTOINCREMENT,        -- unique number for this entry
    animal_id INTEGER NOT NULL REFERENCES animal(id) ON DELETE CASCADE,   -- which animal
    adopter_id INTEGER NOT NULL REFERENCES adopter(id) ON DELETE CASCADE, -- which person
    requested_at TEXT NOT NULL,                 -- when they joined the waitlist
    position INTEGER NOT NULL,                  -- their place in line
    UNIQUE(animal_id, adopter_id)               -- one entry per animal+person pair
);
```
