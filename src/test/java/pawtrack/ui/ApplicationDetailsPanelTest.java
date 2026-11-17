package pawtrack.ui;

import org.junit.jupiter.api.Test;
import pawtrack.controller.WaitlistController;
import pawtrack.dao.ReservationDao;
import pawtrack.model.Adopter;
import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationDetailsPanelTest {
    private WaitlistController.WaitlistRequest sample() {
        Animal pet = Animal.create(Species.DOG, 5, "Milo", "Beagle", 12,
                LocalDate.of(2026, 1, 1), AnimalStatus.PENDING);
        Adopter adopter = new Adopter(8, "Alex Tester", "alex@example.com", "", LocalDate.of(2026, 1, 1));
        return new WaitlistController.WaitlistRequest(pet, adopter, LocalDateTime.of(2026, 1, 2, 9, 0), 1);
    }

    @Test void showsApplicantAndQueueDetails() {
        ApplicationDetailsPanel panel = new ApplicationDetailsPanel(sample(), Optional.empty());
        assertEquals("WAITING", panel.getReviewStatus());
        assertTrue(contains(panel, "Milo"));
        assertTrue(contains(panel, "Alex Tester"));
        assertTrue(contains(panel, "#1"));
    }

    @Test void activeReservationShownAsReservedWithExpiry() {
        ReservationDao.Reservation reservation = new ReservationDao.Reservation(2,5,8,
                LocalDateTime.of(2026,1,2,10,0),LocalDateTime.of(2026,1,5,10,0),"ACTIVE");
        ApplicationDetailsPanel panel = new ApplicationDetailsPanel(sample(), Optional.of(reservation));
        assertEquals("RESERVED", panel.getReviewStatus());
        assertTrue(contains(panel, "Reservation expires"));
    }

    @Test void viewApplicationActionInvokesCallback() {
        AtomicInteger count = new AtomicInteger();
        WaitlistPanel panel = new WaitlistPanel(Color.WHITE, Color.BLUE, Color.GRAY,
                new JButton(), new JButton(), new JButton(), new JButton(), scroll -> {}, count::incrementAndGet);
        JButton view = button(panel,"View Application");
        assertNotNull(view);
        view.doClick();
        assertEquals(1,count.get());
        assertSame(panel.getTable(),panel.getScrollPane().getViewport().getView());
    }

    private static boolean contains(Container c,String value) {
        for(Component child : c.getComponents()) {
            if(child instanceof JLabel l && l.getText().contains(value)) return true;
            if(child instanceof Container sub && contains(sub,value)) return true;
        }
        return false;
    }
    private static JButton button(Container c,String value) {
        for(Component child : c.getComponents()) {
            if(child instanceof JButton b && b.getText().equals(value)) return b;
            if(child instanceof Container sub) {
                JButton result=button(sub,value);
                if(result!=null)return result;
            }
        }
        return null;
    }
}
