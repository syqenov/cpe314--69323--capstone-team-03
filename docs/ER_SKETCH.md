# ER Sketch

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
