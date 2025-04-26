package app.musicplayer.util;

import app.musicplayer.util.ImportMusicTask;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImportMusicTaskTest {

    // Create a concrete implementation of the abstract class for testing
    private static class TestImportMusicTask extends ImportMusicTask<Integer> {
        private boolean callWasSuccessful = false;

        @Override
        protected Integer call() throws Exception {
            // Simple implementation for testing
            updateProgress(50, 100);
            callWasSuccessful = true;
            return 10;
        }

        public boolean wasCallSuccessful() {
            return callWasSuccessful;
        }
    }

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX runtime
        JavaFXInitializer.initialize();
    }

    @Test
    void testUpdateProgressMethod() {
        final TestImportMusicTask[] task = new TestImportMusicTask[1];

        JavaFXInitializer.runLater(() -> {
            task[0] = new TestImportMusicTask();
            
            // Test the updateProgress method
            task[0].updateProgress(25, 50);
        });
        
        // Verify the progress was updated by checking the progress property
        JavaFXInitializer.runLater(() -> {
            assertEquals(0.5, task[0].getProgress(), "Progress should be 0.5 (25/50)");
        });
    }

    @Test
    void testTaskExecution() throws Exception {
        final TestImportMusicTask[] task = new TestImportMusicTask[1];

        JavaFXInitializer.runLater(() -> {
            task[0] = new TestImportMusicTask();
            
            // Start the task
            Thread thread = new Thread(task[0]);
            thread.start();
            try {
                thread.join(1000); // Wait for the task to complete
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Thread was interrupted: " + e.getMessage());
            }
        });
        
        // Verify the task executed successfully
        JavaFXInitializer.runLater(() -> {
            assertTrue(task[0].wasCallSuccessful(), "Task call method should have executed");
            try {
                assertEquals(10, task[0].get().intValue(), "Task result should be 10");
                assertEquals(0.5, task[0].getProgress(), "Progress should be 0.5 (50/100)");
            } catch (Exception e) {
                fail("Failed to get task result: " + e.getMessage());
            }
        });
    }
} 