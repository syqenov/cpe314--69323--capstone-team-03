package pawtrack.service;

	import pawtrack.model.Adopter;
	import org.junit.jupiter.api.BeforeEach;
	import org.junit.jupiter.api.DisplayName;
	import org.junit.jupiter.api.Test;

	import static org.junit.jupiter.api.Assertions.assertEquals;
	import static org.junit.jupiter.api.Assertions.assertFalse;
	import static org.junit.jupiter.api.Assertions.assertTrue;

	/**
	 * FIFO behaviour and edge cases of the per-animal adoption-interest Queue.
	 */
	class WaitlistServiceTest {

	    private WaitlistService waitlist;

	    @BeforeEach
	    void setUp() {
	        waitlist = new WaitlistService();
	    }

	    @Test
	    @DisplayName("Interest is served first-in-first-out")
	    void fifoOrder() {
	        waitlist.addInterest(1, new Adopter("A", "a@example.com", ""));
	        waitlist.addInterest(1, new Adopter("B", "b@example.com", ""));
	        waitlist.addInterest(1, new Adopter("C", "c@example.com", ""));

	        assertEquals("A", waitlist.pollNext(1).orElseThrow().getName());
	        assertEquals("B", waitlist.pollNext(1).orElseThrow().getName());
	        assertEquals("C", waitlist.pollNext(1).orElseThrow().getName());
	        assertTrue(waitlist.pollNext(1).isEmpty());
	    }

	    @Test
	    @DisplayName("Queue positions are 1-based and duplicates are ignored")
	    void positionsAndDuplicates() {
	        assertEquals(1, waitlist.addInterest(7, new Adopter("A", "a@example.com", "")));
	        assertEquals(2, waitlist.addInterest(7, new Adopter("B", "b@example.com", "")));
	        // same email again -> same position, queue length unchanged
	        assertEquals(1, waitlist.addInterest(7, new Adopter("A", "A@EXAMPLE.COM", "")));
	        assertEquals(2, waitlist.size(7));
	    }

	    @Test
	    @DisplayName("Empty queue returns empty results instead of throwing (edge case)")
	    void emptyQueue() {
	        assertFalse(waitlist.hasAnyone(99));
	        assertEquals(0, waitlist.size(99));
	        assertTrue(waitlist.peekNext(99).isEmpty());
	        assertTrue(waitlist.pollNext(99).isEmpty());
	    }

	    @Test
	    @DisplayName("peek does not remove, poll does")
	    void peekVersusPoll() {
	        waitlist.addInterest(3, new Adopter("A", "a@example.com", ""));
	        waitlist.addInterest(3, new Adopter("B", "b@example.com", ""));

	        assertEquals("A", waitlist.peekNext(3).orElseThrow().getName());
	        assertEquals(2, waitlist.size(3));

	        assertEquals("A", waitlist.pollNext(3).orElseThrow().getName());
	        assertEquals(1, waitlist.size(3));
	        assertEquals("B", waitlist.peekNext(3).orElseThrow().getName());
	    }

	    @Test
	    @DisplayName("Clearing and totals are tracked across animals")
	    void totalsAndClear() {
	        waitlist.addInterest(1, new Adopter("A", "a@example.com", ""));
	        waitlist.addInterest(2, new Adopter("B", "b@example.com", ""));
	        waitlist.addInterest(2, new Adopter("C", "c@example.com", ""));

	        assertEquals(3, waitlist.totalWaiters());
	        assertTrue(waitlist.animalIds().contains(1));
	        assertTrue(waitlist.animalIds().contains(2));

	        waitlist.clear(2);
	        assertEquals(1, waitlist.totalWaiters());
	        assertFalse(waitlist.hasAnyone(2));
	    }
	}

