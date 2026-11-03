package pawtrack.service;

	import pawtrack.model.AnimalStatus;
	import pawtrack.model.StatusChange;
	import org.junit.jupiter.api.BeforeEach;
	import org.junit.jupiter.api.DisplayName;
	import org.junit.jupiter.api.Test;

	import java.util.List;
	import java.util.Optional;

	import static org.junit.jupiter.api.Assertions.assertEquals;
	import static org.junit.jupiter.api.Assertions.assertTrue;

	/**
	 * LIFO behaviour of the undo Stack.
	 */
	class StatusHistoryServiceTest {

	    private StatusHistoryService history;

	    @BeforeEach
	    void setUp() {
	        history = new StatusHistoryService();
	    }

	    @Test
	    @DisplayName("Undo pops the most recent change first (LIFO / Stack)")
	    void undoIsLifo() {
	        history.record(1, AnimalStatus.INTAKE, AnimalStatus.AVAILABLE);
	        history.record(1, AnimalStatus.AVAILABLE, AnimalStatus.PENDING);
	        history.record(1, AnimalStatus.PENDING, AnimalStatus.ADOPTED);

	        assertEquals(AnimalStatus.PENDING, history.undo().orElseThrow().from());
	        assertEquals(AnimalStatus.AVAILABLE, history.undo().orElseThrow().from());
	        assertEquals(AnimalStatus.INTAKE, history.undo().orElseThrow().from());
	        assertEquals(0, history.size());
	    }

	    @Test
	    @DisplayName("Undo on an empty history is a safe no-op (edge case)")
	    void undoEmpty() {
	        assertTrue(history.isEmpty());
	        assertTrue(history.undo().isEmpty());
	        assertTrue(history.peekMostRecent().isEmpty());
	    }

	    @Test
	    @DisplayName("Snapshot lists changes most recent first without removing them")
	    void snapshot() {
	        history.record(2, AnimalStatus.AVAILABLE, AnimalStatus.PENDING);
	        history.record(2, AnimalStatus.PENDING, AnimalStatus.ADOPTED);

	        List<StatusChange> snapshot = history.snapshot();
	        assertEquals(2, snapshot.size());
	        assertEquals(AnimalStatus.ADOPTED, snapshot.get(0).to());
	        assertEquals(AnimalStatus.PENDING, snapshot.get(1).to());
	        assertEquals(2, history.size());
	    }

	    @Test
	    @DisplayName("Null records are ignored and clear empties the stack")
	    void nullAndClear() {
	        history.record((StatusChange) null);
	        assertEquals(0, history.size());

	        history.record(3, AnimalStatus.INTAKE, AnimalStatus.AVAILABLE);
	        history.clear();
	        assertEquals(0, history.size());
	        assertEquals(Optional.empty(), history.peekMostRecent());
	    }
	}
