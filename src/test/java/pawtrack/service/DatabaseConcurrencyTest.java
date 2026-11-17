package pawtrack.service;

import org.junit.jupiter.api.Test;
import pawtrack.dao.AdopterDao;
import pawtrack.dao.AdoptionRecordDao;
import pawtrack.dao.AnimalDao;
import pawtrack.dao.Database;
import pawtrack.dao.WaitlistDao;
import pawtrack.model.Adopter;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Dog;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Shared-connection regressions: a long CSV transaction must not overlap other transactions. */
class DatabaseConcurrencyTest {
    private static final String CSV = "name,species,breed,age_months,intake_date,status\n"
            + "Import Dog,Dog,Beagle,12,2024-01-01,Available\n";

    @Test
    void waitlistShiftWaitsForCsvImportTransaction() throws Exception {
        try (Database db = Database.openInMemory()) {
            Animal pet = new Dog(0, "Queue Pet", "Beagle", 24, LocalDate.now(), AnimalStatus.PENDING);
            new AnimalDao(db).insert(pet);
            AdopterDao adopters = new AdopterDao(db);
            int first = adopters.insert(new Adopter("First", "first-queue@example.test", ""));
            int second = adopters.insert(new Adopter("Second", "second-queue@example.test", ""));
            WaitlistDao queue = new WaitlistDao(db);
            queue.insert(pet.getId(), first, LocalDateTime.now().minusDays(1), 1);
            queue.insert(pet.getId(), second, LocalDateTime.now(), 2);

            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            ExecutorService workers = Executors.newFixedThreadPool(2);
            try {
                Future<?> importTask = workers.submit(() -> {
                    try {
                        new CsvImportService(db).importReader(new StringReader(CSV), 2,
                                ImportListener.of((processed, total) -> {
                                    entered.countDown();
                                    waitUntilReleased(release);
                                }, unused -> { }));
                    } catch (Exception failure) {
                        throw new IllegalStateException(failure);
                    }
                });
                assertTrue(entered.await(5, TimeUnit.SECONDS), "Import did not enter its transaction");

                CountDownLatch shiftAttempted = new CountDownLatch(1);
                Future<Boolean> shifted = workers.submit(() -> {
                    shiftAttempted.countDown();
                    return queue.removeAndShift(pet.getId(), first, 1);
                });
                assertTrue(shiftAttempted.await(5, TimeUnit.SECONDS));
                // The queue update must not see the CSV import's uncommitted JDBC state.
                assertThrows(java.util.concurrent.TimeoutException.class,
                        () -> shifted.get(250, TimeUnit.MILLISECONDS),
                        "Waitlist transaction bypassed the import lock");
                release.countDown();
                importTask.get(10, TimeUnit.SECONDS);
                assertTrue(shifted.get(10, TimeUnit.SECONDS));
                List<WaitlistDao.Entry> remaining = queue.loadAll();
                assertEquals(1, remaining.size());
                assertEquals(second, remaining.get(0).adopterId());
                assertEquals(1, remaining.get(0).position());
                assertEquals(2, new AnimalDao(db).count());
                assertTrue(db.connection().getAutoCommit());
            } finally {
                release.countDown();
                workers.shutdownNow();
                assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void adoptionCompletionWaitsUntilCsvImportCommits() throws Exception {
        try (Database db = Database.openInMemory()) {
            Animal pet = new Dog(0, "Adopt Pet", "Retriever", 24,
                    LocalDate.now(), AnimalStatus.AVAILABLE);
            AnimalDao animals = new AnimalDao(db);
            animals.insert(pet);
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            ExecutorService workers = Executors.newFixedThreadPool(2);
            try {
                Future<?> importing = workers.submit(() -> {
                    try {
                        new CsvImportService(db).importReader(new StringReader(CSV), 2,
                                ImportListener.of((processed, total) -> {
                                    entered.countDown();
                                    waitUntilReleased(release);
                                }, unused -> { }));
                    } catch (Exception failure) {
                        throw new IllegalStateException(failure);
                    }
                });
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                CountDownLatch adoptionAttempted = new CountDownLatch(1);
                Future<?> adopting = workers.submit(() -> {
                    adoptionAttempted.countDown();
                    try {
                        new AdoptionTransactionService(db, animals, new AdopterDao(db),
                                new AdoptionRecordDao(db), new StatusHistoryService())
                                .complete(pet, new Adopter("Safe", "safe-adoption@example.test", ""));
                    } catch (Exception failure) {
                        throw new IllegalStateException(failure);
                    }
                });
                assertTrue(adoptionAttempted.await(5, TimeUnit.SECONDS));
                assertThrows(java.util.concurrent.TimeoutException.class,
                        () -> adopting.get(250, TimeUnit.MILLISECONDS),
                        "Adoption transaction bypassed the import lock");
                release.countDown();
                importing.get(10, TimeUnit.SECONDS);
                adopting.get(10, TimeUnit.SECONDS);
                assertEquals(1, new AdoptionRecordDao(db).count());
                assertEquals(AnimalStatus.ADOPTED, animals.findById(pet.getId()).orElseThrow().getStatus());
                assertEquals(2, animals.count());
                assertTrue(db.connection().getAutoCommit());
            } finally {
                release.countDown();
                workers.shutdownNow();
                assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void concurrentWaitlistWritesPreserveAllUniqueAdopters() throws Exception {
        WaitlistService queue = new WaitlistService();
        int threads = 6;
        int each = 30;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> tasks = new java.util.ArrayList<>();
            for (int worker = 0; worker < threads; worker++) {
                final int number = worker;
                tasks.add(workers.submit(() -> {
                    try {
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Queue test never started");
                        }
                        for (int n = 0; n < each; n++) {
                            queue.addInterest(1, new Adopter("Person", "q-" + number + "-" + n
                                    + "@example.test", ""));
                            queue.totalWaiters();
                            queue.queueFor(1);
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(interrupted);
                    }
                }));
            }
            start.countDown();
            for (Future<?> task : tasks) task.get(10, TimeUnit.SECONDS);
            assertEquals(threads * each, queue.size(1));
            assertEquals(threads * each, queue.totalWaiters());
        } finally {
            start.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static void waitUntilReleased(CountDownLatch release) {
        try {
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out releasing test import");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test import interrupted", interrupted);
        }
    }
}
