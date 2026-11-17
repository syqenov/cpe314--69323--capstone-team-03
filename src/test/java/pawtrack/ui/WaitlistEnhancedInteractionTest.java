package pawtrack.ui;

import org.junit.jupiter.api.Test;
import pawtrack.controller.WaitlistController;
import pawtrack.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class WaitlistEnhancedInteractionTest {
    @Test void animalThumbnailOpensCurrentlySelectedApplication() throws Exception {
        AtomicInteger views = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            WaitlistPanel panel = new WaitlistPanel(Color.WHITE, Color.BLUE, Color.GRAY,
                    new JButton("Refresh Waitlist"), new JButton("Reserve First in Queue"),
                    new JButton("Complete Selected Adoption"), new JButton("Cancel Reservation"),
                    x -> {}, views::incrementAndGet);
            panel.getTableModel().addRow(new Object[]{"Buddy (Dog)","Alex","alex@example.com","2026-11-16",1,"WAITING"});
            JTable table = panel.getTable();
            table.setSize(850, 300);
            table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(), 0, 24, 20, 1, false, MouseEvent.BUTTON1));
            assertEquals(1, views.get());
            assertEquals(0, table.getSelectedRow());
            assertEquals("WAITING", table.getModel().getValueAt(0, 5));
        });
    }

    @Test void applicationDetailsKeepsReviewStatusEditableForCurrentWorkflow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Animal animal = Animal.create(Species.DOG, 5, "Milo", "Beagle", 12,
                    LocalDate.of(2026, 1, 1), AnimalStatus.PENDING);
            Adopter adopter = new Adopter(8, "Alex Tester", "alex@example.com", "",
                    LocalDate.of(2026, 1, 1));
            var request = new WaitlistController.WaitlistRequest(animal, adopter,
                    LocalDateTime.of(2026, 1, 2, 9, 0), 1);
            ApplicationDetailsPanel panel = new ApplicationDetailsPanel(request, Optional.empty());
            assertEquals("WAITING", panel.getReviewStatus());
            panel.setReviewStatus("IN_REVIEW");
            assertEquals("IN_REVIEW", panel.getReviewStatus());
        });
    }
}
