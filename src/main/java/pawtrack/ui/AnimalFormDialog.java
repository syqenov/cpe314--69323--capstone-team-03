package pawtrack.ui;

import pawtrack.model.Animal;
import pawtrack.model.AnimalStatus;
import pawtrack.model.Species;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/**
 * Staff animal create/edit dialog. Owns the form layout and input validation;
 * persistence and permissions stay in the controller.
 */
final class AnimalFormDialog {
    private static final Color NAVY_BLUE = new Color(0x1D3F5C);
    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 13);

    private AnimalFormDialog() { }

    // Private helper for MainFrame GUI logic
    static Animal showAnimalForm(JFrame parent, Animal existing, String title) {
        JTextField name = animalTextField(existing == null ? "" : existing.getName());
        JComboBox<Species> species = new JComboBox<>(Species.values());
        if (existing != null) species.setSelectedItem(existing.species());
        JTextField breed = animalTextField(existing == null ? "" : existing.getBreed());
        JTextField age = animalTextField(existing == null ? "0" : String.valueOf(existing.getAgeMonths()));
        JTextField intake = animalTextField(existing == null ? LocalDate.now().toString()
                : existing.getIntakeDate().toString());
        JComboBox<AnimalStatus> status = new JComboBox<>(AnimalStatus.values());
        status.setSelectedItem(existing == null ? AnimalStatus.INTAKE : existing.getStatus());
        styleAnimalCombo(species);
        styleAnimalCombo(status);

        JPanel fields = new JPanel(new GridLayout(0, 2, 16, 13));
        fields.setOpaque(false);
        addAnimalFormField(fields, "Pet name", name);
        addAnimalFormField(fields, "Species", species);
        addAnimalFormField(fields, "Breed", breed);
        addAnimalFormField(fields, "Age in months", age);
        addAnimalFormField(fields, "Intake date (yyyy-MM-dd)", intake);
        addAnimalFormField(fields, "Current status", status);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(new Color(0xF6F9FD));
        content.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 4));
        heading.setOpaque(false);
        JLabel headingTitle = new JLabel(existing == null ? "Register a new pet" : "Update pet details");
        headingTitle.setFont(new Font("Segoe UI", Font.BOLD, 21));
        headingTitle.setForeground(NAVY_BLUE);
        JLabel description = new JLabel(existing == null
                ? "Enter the animal's shelter information below."
                : "Review the animal's information before saving changes.");
        description.setFont(BODY_FONT);
        description.setForeground(new Color(0x59758B));
        heading.add(headingTitle);
        heading.add(description);
        content.add(heading, BorderLayout.NORTH);
        JPanel fieldCard = new JPanel(new BorderLayout());
        fieldCard.setBackground(Color.WHITE);
        fieldCard.setBorder(BorderFactory.createCompoundBorder(
                new RoundedUi.RoundedBorder(18, new Color(0xD5E4F0), 1),
                BorderFactory.createEmptyBorder(22, 20, 22, 20)));
        fieldCard.add(fields, BorderLayout.CENTER);
        content.add(fieldCard, BorderLayout.CENTER);

        JLabel validation = new JLabel(" ");
        validation.setFont(BODY_FONT);
        validation.setForeground(new Color(0xA93838));
        JButton cancel = new RoundedUi.RoundedButton("Cancel", 16, new Color(0x71869B));
        JButton save = new RoundedUi.RoundedButton(existing == null ? "Add Animal" : "Save Changes", 16,
                new Color(0x315FAD));
        cancel.setPreferredSize(new Dimension(128, 42));
        save.setPreferredSize(new Dimension(145, 42));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        actions.setOpaque(false);
        actions.add(cancel);
        actions.add(save);
        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(validation, BorderLayout.NORTH);
        bottom.add(actions, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(parent, title, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(content);
        dialog.setMinimumSize(new Dimension(560, 510));
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        final Animal[] result = new Animal[1];
        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> {
            try {
                String animalName = name.getText().trim();
                String animalBreed = breed.getText().trim();
                if (animalName.isEmpty()) throw new IllegalArgumentException("Pet name is required.");
                if (animalBreed.isEmpty()) throw new IllegalArgumentException("Breed is required.");
                int months;
                try { months = Integer.parseInt(age.getText().trim()); }
                catch (NumberFormatException ex) { throw new IllegalArgumentException("Age must be a whole number."); }
                if (months < 0) throw new IllegalArgumentException("Age cannot be negative.");
                LocalDate date;
                try { date = LocalDate.parse(intake.getText().trim()); }
                catch (java.time.format.DateTimeParseException ex) {
                    throw new IllegalArgumentException("Use yyyy-MM-dd for the intake date.");
                }
                if (date.isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException("Intake date cannot be in the future.");
                }
                int id = existing == null ? 0 : existing.getId();
                result[0] = Animal.create((Species) species.getSelectedItem(), id,
                        animalName, animalBreed, months, date, (AnimalStatus) status.getSelectedItem());
                dialog.dispose();
            } catch (RuntimeException failure) {
                validation.setText(failure.getMessage() == null ? "Check the form values." : failure.getMessage());
            }
        });
        dialog.getRootPane().setDefaultButton(save);
        dialog.setVisible(true);
        return result[0];
    }

    private static JTextField animalTextField(String initialValue) {
        JTextField field = new JTextField(initialValue, 18);
        field.setFont(BODY_FONT);
        field.setForeground(NAVY_BLUE);
        field.setBackground(new Color(0xF8FBFF));
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedUi.RoundedBorder(12, new Color(0xC7DDEE), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return field;
    }

    private static void styleAnimalCombo(JComboBox<?> combo) {
        combo.setFont(BODY_FONT);
        combo.setForeground(NAVY_BLUE);
        combo.setBackground(new Color(0xF8FBFF));
        combo.setBorder(new RoundedUi.RoundedBorder(12, new Color(0xC7DDEE), 1));
    }

    private static void addAnimalFormField(JPanel fields, String title, Component editor) {
        JLabel label = new JLabel(title);
        label.setFont(LABEL_FONT);
        label.setForeground(NAVY_BLUE);
        fields.add(label);
        fields.add(editor);
    }

}
