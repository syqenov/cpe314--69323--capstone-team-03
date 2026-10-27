package com.petshelter.gui;

import com.petshelter.api.HttpJson;
import com.petshelter.api.PetImageResult;
import com.petshelter.api.PetImageService;

import javax.swing.ImageIcon;
import javax.swing.SwingWorker;
import java.awt.Image;

/**
 * Fetches a sample pet image from a live public REST API on a background thread:
 * the JSON lookup and the image download both happen off the Event Dispatch
 * Thread.
 */
public final class ImageWorker extends SwingWorker<ImageWorker.Result, Void> {

    /** Icon plus metadata returned to the EDT. */
    public record Result(ImageIcon icon, PetImageResult info) {
    }

    private final PetImageService service;
    private final String breed;
    private final int targetWidth;

    public ImageWorker(PetImageService service, String breed, int targetWidth) {
        this.service = service;
        this.breed = breed;
        this.targetWidth = targetWidth;
    }

    @Override
    protected Result doInBackground() throws Exception {
        PetImageResult info = service.fetchRandomImage(breed);
        byte[] bytes = HttpJson.getBytes(info.imageUrl());
        ImageIcon raw = new ImageIcon(bytes);
        ImageIcon scaled = raw;
        if (raw.getIconWidth() > targetWidth && raw.getIconWidth() > 0) {
            int height = (int) (raw.getIconHeight() * (targetWidth / (double) raw.getIconWidth()));
            scaled = new ImageIcon(raw.getImage().getScaledInstance(targetWidth, height, Image.SCALE_SMOOTH));
        }
        return new Result(scaled, info);
    }
}
