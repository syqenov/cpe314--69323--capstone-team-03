package pawtrack.service;

	import pawtrack.db.ClassificationDao;
	import pawtrack.model.ClassificationNode;
	import pawtrack.model.Species;
	import org.junit.jupiter.api.BeforeEach;
	import org.junit.jupiter.api.DisplayName;
	import org.junit.jupiter.api.Test;

	import java.util.List;
	import java.util.Optional;

	import static org.junit.jupiter.api.Assertions.assertEquals;
	import static org.junit.jupiter.api.Assertions.assertFalse;
	import static org.junit.jupiter.api.Assertions.assertTrue;

	/**
	 * Recursive search over an arbitrarily deep classification tree.
	 */
	class ClassificationServiceTest {

	    private ClassificationService service;

	    @BeforeEach
	    void setUp() {
	        service = new ClassificationService(ClassificationService.defaultTaxonomy());
	    }

	    @Test
	    @DisplayName("Seeded taxonomy has the expected size and nesting depth")
	    void taxonomyShape() {
	        assertEquals(5, service.depth());   // All Animals > Dog > Sporting Group > Retriever > Golden Retriever
	        assertEquals(22, service.nodeCount());
	        assertEquals(22, service.allPaths().size());
	    }

	    @Test
	    @DisplayName("Recursive path search finds a node nested four levels deep")
	    void recursivePathSearchDeep() {
	        String path = ClassificationService.joinPath(
	                "All Animals", "Dog", "Sporting Group", "Retriever", "Golden Retriever");

	        Optional<ClassificationNode> found = service.findByPath(path);
	        assertTrue(found.isPresent());
	        assertEquals("Golden Retriever", found.get().getName());
	        assertEquals(path, found.get().path());
	        assertEquals("Retriever", found.get().getParent().getName());
	    }

	    @Test
	    @DisplayName("Path search returns empty for a non-existent path (edge case)")
	    void pathSearchMissing() {
	        assertTrue(service.findByPath("Dog > Sporting Group > Unicorn").isEmpty());
	        assertTrue(service.findByPath("").isEmpty());
	        assertTrue(service.findByPath(null).isEmpty());
	    }

	    @Test
	    @DisplayName("Searching by name finds nodes at any depth")
	    void searchByName() {
	        List<ClassificationNode> retriever = service.search("Retriever");
	        assertEquals(1, retriever.size());
	        assertEquals("Sporting Group", retriever.get(0).getParent().getName());
	        assertEquals("Dog", retriever.get(0).getParent().getParent().getName());

	        List<ClassificationNode> siamese = service.search("siamese");
	        assertEquals(1, siamese.size());
	        assertEquals("All Animals > Cat > Pedigree > Siamese", siamese.get(0).path());

	        assertTrue(service.search("Drake").isEmpty());
	        assertTrue(service.search(null).isEmpty());
	    }

	    @Test
	    @DisplayName("A breed resolves to its classification node, unknown breeds do not")
	    void classifyByBreed() {
	        Optional<ClassificationNode> golden = service.classifyByBreed("Golden Retriever");
	        assertTrue(golden.isPresent());
	        assertEquals("Retriever", golden.get().getParent().getName());

	        assertTrue(service.classifyByBreed("Sabre-Toothed Tiger").isEmpty());
	    }

	    @Test
	    @DisplayName("Species roots are discoverable for classification fallback")
	    void speciesRoots() {
	        assertEquals("Cat", service.speciesRoot(Species.CAT).orElseThrow().getName());
	        assertEquals("Dog", service.speciesRoot(Species.DOG).orElseThrow().getName());
	        assertEquals("Cat", service.speciesRoot(Species.CAT).orElseThrow().getName());
	        assertFalse(service.speciesRoot(Species.CAT).isEmpty());
	    }

	    @Test
	    @DisplayName("A tree can be rebuilt from flat database rows")
	    void rebuildFromRows() {
	        List<ClassificationDao.Row> rows = List.of(
	                new ClassificationDao.Row(1, "Root", null),
	                new ClassificationDao.Row(2, "Dog", 1),
	                new ClassificationDao.Row(3, "Retriever", 2),
	                new ClassificationDao.Row(4, "Golden Retriever", 3));

	        ClassificationService rebuilt = ClassificationService.fromRows(rows);
	        assertEquals("Root", rebuilt.root().getName());
	        assertEquals(4, rebuilt.nodeCount());
	        assertEquals("Root > Dog > Retriever > Golden Retriever",
	                rebuilt.findByPath("Root > Dog > Retriever > Golden Retriever").orElseThrow().path());
	    }
	}


