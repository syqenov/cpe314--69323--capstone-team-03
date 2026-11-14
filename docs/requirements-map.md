# PawTrack - Requirement to Code Map

This document maps the core project requirements to their corresponding implemented classes and files.

## 1. API Integration
* **Interface & Factory:** `api/PetImageService.java`, `api/PetImageServiceFactory.java`
* **Clients:** `api/DogCeoImageService.java`, `api/CatApiImageService.java`
* **Network & Parsing:** `api/HttpJson.java`, plus API DTO records

## 2. Threading & Concurrency (SwingWorker)
* **CSV Import:** `gui/ImportWorker.java` (Includes cancellation and UI lock state)
* **Image Retrieval:** `gui/ImageWorker.java`
* **Analytics Processing:** `gui/AnalyticsWorker.java`

## 3. Data Structures & Recursion
* **Queue (Waitlist):** `service/WaitlistService.java`
* **Stack (Undo History):** `service/StatusHistoryService.java`, `model/StatusChange.java`
* **Tree & Recursion (Classification):** `service/ClassificationService.java`, `model/ClassificationNode.java`

## 4. Data Mining & Aggregation
* **Mining Logic:** `service/AnalyticsService.java` (Computes frequency analysis for top breeds, species counts, average stay, and recursive classification totals)

## 5. Chart & Visualization
* **Bar Chart:** `gui/BarChartPanel.java`
* **Dashboard:** `gui/AnalyticsPanel.java`

## 6. GUI & Architecture (MVC)
* **Views:** `gui/MainFrame.java`, `gui/LoginPanel.java`, `gui/AnimalTableModel.java`
* **Controllers:** `controller/AnimalController.java`, `controller/AdoptionController.java`, `controller/WaitlistController.java`, `controller/ImportController.java`, `controller/AnalyticsController.java`

## 7. Authentication & Security
* **Security Logic:** `security/LoginManager.java`, `security/PasswordHasher.java` (PBKDF2)
* **User Model:** `model/User.java`, `model/Role.java` (STAFF/ADOPTER accounts)

## 8. Database Schema (SQLite)
* **Initialization & Connection:** `db/Database.java` (includes `created_at` and `source` migrations)
* **Entities:** `db/AnimalDao.java`, `db/AdopterDao.java`, `db/AdoptionRecordDao.java`, `db/WaitlistDao.java`, `db/ClassificationDao.java`

## 9. Tests (JUnit 5)
* **Location:** `src/test/java/pawtrack/`
* **Key Suites:** `PersistenceTest.java` (includes failure/edge cases), `LoginManagerTest.java`, `PetApiParsingTest.java`, `ClassificationServiceTest.java`, `AnalyticsServiceTest.java`

## 10. Design Document
* **Location:** `docs/design-document.md`
