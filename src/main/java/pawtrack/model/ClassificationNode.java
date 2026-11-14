package pawtrack.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Node of the species/breed classification tree. The tree is intentionally
 * unbounded in depth (e.g. Dog &gt; Sporting Group &gt; Retriever &gt; Golden Retriever)
 * and is explored with recursion only.
 */
public final class ClassificationNode {

    private int id;
    private final String name;
    private ClassificationNode parent;
    private final List<ClassificationNode> children = new ArrayList<>();

    public ClassificationNode(int id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Classification name must not be blank");
        }
        this.id = id;
        this.name = name.trim();
    }

    public ClassificationNode addChild(ClassificationNode child) {
        if (child == null) {
            throw new IllegalArgumentException("child must not be null");
        }
        if (child == this) {
            throw new IllegalArgumentException("A classification node cannot be its own child");
        }
        child.parent = this;
        children.add(child);
        return child;
    }

    public ClassificationNode addChild(String childName) {
        return addChild(new ClassificationNode(0, childName));
    }

    public void assignId(int generatedId) {
        if (this.id == 0 && generatedId > 0) {
            this.id = generatedId;
        }
    }

    /**
     * Recursive depth-first search for the node reached by following {@code path}
     * from this node (inclusive). Searches to any depth.
     */
    public Optional<ClassificationNode> findByPath(List<String> path) {
        if (path == null || path.isEmpty()) {
            return Optional.of(this);
        }
        if (!name.equalsIgnoreCase(path.get(0))) {
            return Optional.empty();
        }
        if (path.size() == 1) {
            return Optional.of(this);
        }
        List<String> remainder = path.subList(1, path.size());
        for (ClassificationNode child : children) {
            Optional<ClassificationNode> found = child.findByPath(remainder);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** Recursive exhaustive search returning every node whose name matches. */
    public List<ClassificationNode> searchByName(String target) {
        List<ClassificationNode> matches = new ArrayList<>();
        collectByName(target, matches);
        return matches;
    }

    private void collectByName(String target, List<ClassificationNode> matches) {
        if (name.equalsIgnoreCase(target)) {
            matches.add(this);
        }
        for (ClassificationNode child : children) {
            child.collectByName(target, matches);
        }
    }

    /** @return the height of the subtree rooted here (a leaf has depth 1). */
    public int depth() {
        int deepest = 0;
        for (ClassificationNode child : children) {
            deepest = Math.max(deepest, child.depth());
        }
        return 1 + deepest;
    }

    /** @return number of nodes in this subtree, including this node. */
    public int countNodes() {
        int total = 1;
        for (ClassificationNode child : children) {
            total += child.countNodes();
        }
        return total;
    }

    /** @return every node in this subtree (self included) in pre-order. */
    public List<ClassificationNode> flatten() {
        List<ClassificationNode> out = new ArrayList<>();
        collectPreOrder(out);
        return out;
    }

    private void collectPreOrder(List<ClassificationNode> out) {
        out.add(this);
        for (ClassificationNode child : children) {
            child.collectPreOrder(out);
        }
    }

    /** Recursively builds the "A > B > C" path from the root down to this node. */
    public String path() {
        if (parent == null) {
            return name;
        }
        return parent.path() + " > " + name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ClassificationNode getParent() {
        return parent;
    }

    public List<ClassificationNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    @Override
    public String toString() {
        return name;
    }
}
