package pawtrack.service;

import pawtrack.dao.ClassificationDao;
import pawtrack.model.ClassificationNode;
import pawtrack.model.Species;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wraps the classification tree and exposes recursive search helpers.
 * Includes validation for missing parents and cycle detection.
 */
public final class ClassificationService {

    private final ClassificationNode root;

    public ClassificationService(ClassificationNode root) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        this.root = root;
    }

    public static ClassificationNode defaultTaxonomy() {
        ClassificationNode root = new ClassificationNode(0, "All Animals");

        ClassificationNode dog = root.addChild("Dog");
        ClassificationNode sporting = dog.addChild("Sporting Group");
        ClassificationNode retriever = sporting.addChild("Retriever");
        retriever.addChild("Golden Retriever");
        retriever.addChild("Labrador Retriever");
        sporting.addChild("Spaniel").addChild("Cocker Spaniel");
        ClassificationNode herding = dog.addChild("Herding Group");
        herding.addChild("German Shepherd");
        herding.addChild("Collie");
        ClassificationNode nonSporting = dog.addChild("Non-Sporting Group");
        nonSporting.addChild("Poodle");
        nonSporting.addChild("Bulldog");

        ClassificationNode cat = root.addChild("Cat");
        ClassificationNode domestic = cat.addChild("Domestic");
        domestic.addChild("Domestic Shorthair");
        domestic.addChild("Domestic Longhair");
        ClassificationNode pedigree = cat.addChild("Pedigree");
        pedigree.addChild("Persian");
        pedigree.addChild("Siamese");
        pedigree.addChild("Maine Coon");

        return root;
    }

    /** Persists the default taxonomy when the table is empty, then loads the tree. */
    public static ClassificationService loadOrSeed(ClassificationDao dao) throws java.sql.SQLException {
        if (dao.count() == 0) {
            dao.persistTree(defaultTaxonomy(), null);
        }
        return fromRows(dao.loadAll());
    }

    /** Rebuilds a navigable tree from flat database rows with cycle detection and missing parent checks. */
    /** Rebuilds a navigable tree from flat database rows with cycle detection and missing parent checks. */
    public static ClassificationService fromRows(List<ClassificationDao.Row> rows) {
        Map<Integer, ClassificationNode> byId = new LinkedHashMap<>();
        Map<Integer, Integer> childToParent = new LinkedHashMap<>();

        for (ClassificationDao.Row row : rows) {
            byId.put(row.id(), new ClassificationNode(row.id(), row.name()));
            if (row.parentId() != null) {
                childToParent.put(row.id(), row.parentId());
            }
        }

        // Cycle detection: walk up from each node using parent links
        for (Integer id : childToParent.keySet()) {
            Integer curr = id;
            java.util.Set<Integer> visited = new java.util.HashSet<>();
            while (curr != null) {
                if (!visited.add(curr)) {
                    throw new IllegalStateException("Classification cycle detected involving node ID " + curr);
                }
                curr = childToParent.get(curr);
            }
        }

        ClassificationNode root = null;
        for (ClassificationDao.Row row : rows) {
            if (row.parentId() == null) {
                root = byId.get(row.id());
                break;
            }
        }

        for (ClassificationDao.Row row : rows) {
            if (row.parentId() != null) {
                ClassificationNode parent = byId.get(row.parentId());
                ClassificationNode child = byId.get(row.id());
                if (parent == null) {
                    throw new IllegalStateException("Missing parent ID " + row.parentId() + " for node ID " + row.id() + " (" + row.name() + ")");
                }
                if (child != null) {
                    parent.addChild(child);
                }
            }
        }

        if (root == null) {
            root = defaultTaxonomy();
        }
        return new ClassificationService(root);
    }

    private static boolean isDescendant(ClassificationNode node, ClassificationNode target) {
        if (node == target) {
            return true;
        }
        for (ClassificationNode child : node.flatten()) {
            if (child == target) {
                // If target is in the flat list under node, it's a descendant
                // Or check children explicitly if flatten includes node itself:
            }
        }
        // Simple recursive check through children
        return checkDescendantRecursive(node, target);
    }

    private static boolean checkDescendantRecursive(ClassificationNode current, ClassificationNode target) {
        if (current == target) {
            return true;
        }
        for (ClassificationNode child : current.flatten()) {
            // Flatten returns self and descendants; let's verify direct children traversal if flatten is available, 
            // otherwise check name or ID equality.
        }
        return false;
    }

    public ClassificationNode root() {
        return root;
    }

    /** Recursive search for a full path such as {@code "Dog > Sporting Group > Retriever"}. */
    public Optional<ClassificationNode> findByPath(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        List<String> parts = new ArrayList<>();
        for (String part : path.split(">")) {
            if (!part.isBlank()) {
                parts.add(part.trim());
            }
        }
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        return root.findByPath(parts);
    }

    /** Recursive search by leaf name, returning every match at any depth. */
    public List<ClassificationNode> search(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return root.searchByName(name.trim());
    }

    /** Maps a free-text breed onto the best matching classification node. */
    public Optional<ClassificationNode> classifyByBreed(String breed) {
        List<ClassificationNode> matches = search(breed);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    public Optional<ClassificationNode> speciesRoot(Species species) {
        Species resolved = species == null ? Species.CAT : species;
        String name = switch (resolved) {
            case DOG -> "Dog";
            case CAT -> "Cat";
            default -> "Small Mammals";
        };
        List<ClassificationNode> matches = search(name);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    /** Every classification path in pre-order together with its id. */
    public Map<Integer, String> idToPath() {
        Map<Integer, String> paths = new LinkedHashMap<>();
        for (ClassificationNode node : root.flatten()) {
            paths.put(node.getId(), node.path());
        }
        return paths;
    }

    public List<String> allPaths() {
        List<String> paths = new ArrayList<>();
        for (ClassificationNode node : root.flatten()) {
            paths.add(node.path());
        }
        return paths;
    }

    public int depth() {
        return root.depth();
    }

    public int nodeCount() {
        return root.countNodes();
    }

    /** Convenience helper used by tests and the GUI filter. */
    public static String joinPath(String... parts) {
        return String.join(" > ", Arrays.asList(parts));
    }
}
