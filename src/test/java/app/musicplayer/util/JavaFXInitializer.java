package app.musicplayer.util;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class for initializing JavaFX in test environments.
 * This is needed because JavaFX components can only be used after the JavaFX runtime is initialized.
 */
public class JavaFXInitializer {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Initialize the JavaFX environment for tests.
     * This method is idempotent - calling it multiple times will only initialize JavaFX once.
     */
    public static void initialize() {
        if (initialized.getAndSet(true)) {
            return; // Already initialized
        }

        // Initialize JavaFX by creating a JFXPanel
        new JFXPanel();
        
        // Ensure toolkit is initialized on the JavaFX thread
        runLater(() -> {
            // Nothing to do here, just ensure JavaFX is initialized
        });
    }

    /**
     * Run a task on the JavaFX application thread and wait for it to complete.
     * 
     * @param task The task to run on the JavaFX thread
     */
    public static void runLater(Runnable task) {
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    task.run();
                } finally {
                    latch.countDown();
                }
            });
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for JavaFX task to complete", e);
        }
    }
} 