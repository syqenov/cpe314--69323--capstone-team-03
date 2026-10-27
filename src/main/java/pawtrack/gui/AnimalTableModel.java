package com.petshelter.gui;

import com.petshelter.model.Animal;
import com.petshelter.service.WaitlistService;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model backing the animal list. Kept separate from the frame so it has a
 * single responsibility: turning {@link Animal} objects into table rows.
 */
public final class AnimalTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "ID", "Name", "Species", "Breed", "Age (mo)", "Intake Date", "Status", "Fee ($)", "Waitlist"
    };

    private final WaitlistService waitlistService;
    private List<Animal> animals = new ArrayList<>();

    public AnimalTableModel(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    public void setAnimals(List<Animal> animals) {
        this.animals = animals == null ? new ArrayList<>() : new ArrayList<>(animals);
        fireTableDataChanged();
    }

    public Animal getAnimalAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= animals.size()) {
            return null;
        }
        return animals.get(rowIndex);
    }

    public List<Animal> getAnimals() {
        return new ArrayList<>(animals);
    }

    @Override
    public int getRowCount() {
        return animals.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 || columnIndex == 4 || columnIndex == 8 ? Integer.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Animal animal = animals.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> animal.getId();
            case 1 -> animal.getName();
            case 2 -> animal.species().label();
            case 3 -> animal.getBreed();
            case 4 -> animal.getAgeMonths();
            case 5 -> animal.getIntakeDate().toString();
            case 6 -> animal.getStatus().label();
            case 7 -> String.format("%.2f", animal.adoptionFee());
            case 8 -> waitlistService.size(animal.getId());
            default -> "";
        };
    }
}
