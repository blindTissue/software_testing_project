package util;

import app.musicplayer.model.Song;
import app.musicplayer.util.ControlPanelTableCell;
import javafx.scene.control.TableRow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ControlPanelTableCellTest {

    // Testable subclass that exposes the protected method
    private static class TestableControlPanelTableCell<S, T> extends ControlPanelTableCell<S, T> {
        public void testUpdateItem(T item, boolean empty) {
            updateItem(item, empty);
        }
    }

    private TestableControlPanelTableCell<Object, String> cell;
    private TableRow<Song> tableRow;

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX runtime
        JavaFXInitializer.initialize();
    }

    @BeforeEach
    void setUp() {
        // Create components on JavaFX thread
        JavaFXInitializer.runLater(() -> {
            cell = new TestableControlPanelTableCell<>();
            tableRow = new TableRow<>();
            
            try {
                // Use reflection to set the tableRow on the cell
                Field tableRowField = cell.getClass().getSuperclass().getSuperclass().getDeclaredField("tableRow");
                tableRowField.setAccessible(true);
                tableRowField.set(cell, tableRow);
            } catch (Exception e) {
                // If reflection fails, the test will fail later
                e.printStackTrace();
            }
        });
    }

    @Test
    void testUpdateItemWithNullValue() {
        JavaFXInitializer.runLater(() -> {
            cell.testUpdateItem(null, false);
            assertNull(cell.getText(), "Text should be null when item is null");
            assertNull(cell.getGraphic(), "Graphic should be null when item is null");
        });
    }

    @Test
    void testUpdateItemWhenEmpty() {
        JavaFXInitializer.runLater(() -> {
            cell.testUpdateItem("Test", true);
            assertNull(cell.getText(), "Text should be null when empty is true");
            assertNull(cell.getGraphic(), "Graphic should be null when empty is true");
        });
    }

    @Test
    void testUpdateItemWithNullSong() {
        JavaFXInitializer.runLater(() -> {
            // Set the song on the table row to null
            tableRow.setItem(null);
            cell.testUpdateItem("Test", false);
            assertNull(cell.getText(), "Text should be null when song is null");
            assertNull(cell.getGraphic(), "Graphic should be null when song is null");
        });
    }

    // Note: Testing with actual song values would require more complex mocking
    // of the JavaFX environment and is not covered in these basic tests
} 