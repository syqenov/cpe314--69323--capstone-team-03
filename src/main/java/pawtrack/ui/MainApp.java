package pawtrack.ui;

import pawtrack.model.Pet;
import pawtrack.db.DatabaseConnection;

public class MainApp {
    public static void main(String[] args) {
        System.out.println("=== Welcome to PawTrack ===");
        DatabaseConnection.connect();
        // Sample code --- Agang will update this.
        Pet samplePet = new Pet("Bantay", "Dog");
        System.out.println("Loaded sample record: " + samplePet.getName() + " (" + samplePet.getSpecies() + ")");
    }
}