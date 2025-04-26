package app.musicplayer.util;

import app.musicplayer.util.ClippedTableCell;
import javafx.scene.control.OverrunStyle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClippedTableCellTest {

    // Testable subclass that exposes the protected method
    private static class TestableClippedTableCell<S, T> extends ClippedTableCell<S, T> {
        public void testUpdateItem(T item, boolean empty) {
            updateItem(item, empty);
        }
    }

    private TestableClippedTableCell<Object, String> cell;

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX runtime
        JavaFXInitializer.initialize();
    }

    @BeforeEach
    void setUp() {
        // Create cell on JavaFX thread
        JavaFXInitializer.runLater(() -> {
            cell = new TestableClippedTableCell<>();
        });
    }

    @Test
    void testConstructor() {
        JavaFXInitializer.runLater(() -> {
            assertEquals(OverrunStyle.CLIP, cell.getTextOverrun(), 
                "TextOverrun should be set to CLIP");
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
    void testUpdateItemWithValue() {
        JavaFXInitializer.runLater(() -> {
            String testValue = "Test String";
            cell.testUpdateItem(testValue, false);
            assertEquals(testValue, cell.getText(), 
                "Text should be set to the item's toString() value");
        });
    }
} 