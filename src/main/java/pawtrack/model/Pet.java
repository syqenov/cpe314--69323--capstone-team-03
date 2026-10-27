package pawtrack.model;

public class Pet {
    private String name;
    private String species; //sample code --- Enovero & Lausa update will this

    public Pet(String name, String species) {
        this.name = name;
        this.species = species;
    }

    public String getName() {
        return name;
    }

    public String getSpecies() {
        return species;
    }
}