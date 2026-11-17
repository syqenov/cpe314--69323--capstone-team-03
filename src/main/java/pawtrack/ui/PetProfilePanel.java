package pawtrack.ui;

import pawtrack.api.PetImageServiceFactory;
import pawtrack.model.Animal;
import pawtrack.model.Species;
import pawtrack.service.PetPhotoStore;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;

/** Read-only animal information and optional asynchronous sample API image. */
public final class PetProfilePanel extends JPanel {
    private static final Color NAVY = new Color(0x1B3157);
    private final Animal animal;
    private final boolean staff;
    private final PetPhotoStore photos;
    private List<Path> gallery = List.of();
    private int galleryIndex;
    private final JButton previous = new JButton("◀ Previous");
    private final JButton next = new JButton("Next ▶");
    private final JButton addPhoto = new JButton("Add actual photo");
    private final JButton deletePhoto = new JButton("Remove photo");
    private final JLabel image = new JLabel("Sample image not loaded", SwingConstants.CENTER);
    private final JLabel imageStatus = new JLabel("Images are illustrative, not photographs of this pet");
    private final JButton loadImage = new JButton("Load sample pet image");

    public PetProfilePanel(Animal animal) { this(animal, false); }

    public PetProfilePanel(Animal animal, boolean staff) {
        this(animal, staff, PetPhotoStore.defaultStore());
    }

    public PetProfilePanel(Animal animal, boolean staff, PetPhotoStore photos) {
        super(new BorderLayout(16, 12));
        this.staff = staff;
        this.photos = Objects.requireNonNull(photos);
        this.animal = Objects.requireNonNull(animal, "animal");
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(16, 18, 16, 18));
        setPreferredSize(new Dimension(560, 380));

        JLabel title = new JLabel(animal.getName() + "  ·  " + animal.species().label());
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));
        title.setForeground(NAVY);
        add(title, BorderLayout.NORTH);

        image.setPreferredSize(new Dimension(240, 225));
        image.setOpaque(true);
        image.setBackground(new Color(0xEFF5FC));
        image.setForeground(NAVY);
        JPanel imageArea = new JPanel(new BorderLayout(0, 8));
        imageArea.setOpaque(false);
        imageArea.add(image, BorderLayout.CENTER);
        imageStatus.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        imageStatus.setForeground(new Color(0x657A90));
        JPanel photoNavigation = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        photoNavigation.setOpaque(false);
        previous.addActionListener(e -> showPhoto(galleryIndex - 1));
        next.addActionListener(e -> showPhoto(galleryIndex + 1));
        photoNavigation.add(previous);
        photoNavigation.add(next);
        JPanel photoFoot = new JPanel(new BorderLayout(0, 4));
        photoFoot.setOpaque(false);
        photoFoot.add(imageStatus, BorderLayout.NORTH);
        photoFoot.add(photoNavigation, BorderLayout.SOUTH);
        imageArea.add(photoFoot, BorderLayout.SOUTH);
        add(imageArea, BorderLayout.WEST);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        addDetail(details, "Breed", animal.getBreed());
        addDetail(details, "Age", animal.getAgeMonths() + " months");
        addDetail(details, "Status", animal.getStatus().label());
        addDetail(details, "Intake date", animal.getIntakeDate().toString());
        addDetail(details, "Source", animal.getSource());
        addDetail(details, "Adoption fee", String.format("%.2f", animal.adoptionFee()));
        addDetail(details, "Care schedule", animal.careSchedule());
        add(details, BorderLayout.CENTER);

        loadImage.setName("loadPetProfileSampleImage");
        loadImage.addActionListener(e -> loadSampleImage());
        // The current public image integrations cover dogs and cats only.
        loadImage.setEnabled(animal.species() == Species.DOG || animal.species() == Species.CAT);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.add(loadImage);
        if (staff) {
            addPhoto.addActionListener(e -> choosePhoto());
            deletePhoto.addActionListener(e -> deleteCurrentPhoto());
            actions.add(addPhoto);
            actions.add(deletePhoto);
        }
        add(actions, BorderLayout.SOUTH);
        refreshPhotos();
    }

    private static void addDetail(JPanel panel, String key, String value) {
        JLabel label = new JLabel("<html><b>" + escape(key) + ":</b> " + escape(value) + "</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(NAVY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(11));
    }

    private static String escape(String text) {
        return Objects.toString(text, "").replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void refreshPhotos() {
        try {
            gallery = photos.list(animal.getId());
            if (!gallery.isEmpty()) {
                showPhoto(Math.min(galleryIndex, gallery.size() - 1));
            } else {
                galleryIndex = 0;
                image.setIcon(null);
                image.setText("No actual pet photos yet");
                imageStatus.setText("Sample images are not photos of this animal");
                previous.setEnabled(false);
                next.setEnabled(false);
                deletePhoto.setEnabled(false);
            }
            addPhoto.setEnabled(staff && gallery.size() < PetPhotoStore.MAX_PHOTOS);
        } catch (IOException failure) {
            image.setText("Unable to load local pet photos");
        }
    }

    private void showPhoto(int index) {
        if (index < 0 || index >= gallery.size()) return;
        galleryIndex = index;
        try {
            BufferedImage original;
            try (InputStream stream = Files.newInputStream(gallery.get(index))) {
                original = ImageIO.read(stream);
            }
            if (original == null) throw new IOException("Unreadable photo");
            Image scaled = original.getScaledInstance(240, 225, Image.SCALE_SMOOTH);
            image.setIcon(new ImageIcon(scaled));
            image.setText("");
            imageStatus.setText("Actual pet photo " + (index + 1) + " of " + gallery.size());
        } catch (IOException failure) {
            image.setIcon(null);
            image.setText("Photo unavailable");
        }
        previous.setEnabled(index > 0);
        next.setEnabled(index < gallery.size() - 1);
        deletePhoto.setEnabled(staff);
    }

    private void choosePhoto() {
        if (!staff) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose an actual pet photo (up to 5 MB)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            photos.add(animal.getId(), chooser.getSelectedFile().toPath());
            galleryIndex = Integer.MAX_VALUE;
            refreshPhotos();
        } catch (IOException | IllegalArgumentException failure) {
            JOptionPane.showMessageDialog(this, failure.getMessage(), "Photo not saved", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteCurrentPhoto() {
        if (!staff || gallery.isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "Remove this photo?", "Confirm", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        try {
            photos.remove(animal.getId(), gallery.get(galleryIndex));
            refreshPhotos();
        } catch (IOException failure) {
            JOptionPane.showMessageDialog(this, failure.getMessage(), "Photo not removed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void loadSampleImage() {
        loadImage.setEnabled(false);
        image.setText("Loading sample image...");
        image.setIcon(null);
        ImageWorker worker = new ImageWorker(PetImageServiceFactory.forSpecies(animal.species()),
                animal.getBreed(), 240);
        worker.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName()) &&
                    evt.getNewValue() == SwingWorker.StateValue.DONE) {
                try {
                    ImageWorker.Result result = worker.get();
                    image.setIcon(result.icon());
                    image.setText("");
                    imageStatus.setText("Sample from " + result.info().provider() + " — not this actual pet");
                } catch (Exception failure) {
                    image.setText("Image unavailable (check connection)");
                    imageStatus.setText("Pet details are still available offline");
                } finally {
                    loadImage.setEnabled(true);
                }
            }
        });
        worker.execute();
    }

    public Animal getAnimal() { return animal; }
    public JButton getLoadImageButton() { return loadImage; }
    public int getActualPhotoCount() { return gallery.size(); }
    public boolean canAddActualPhotos() { return staff; }
}
