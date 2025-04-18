package util;

import app.musicplayer.util.CustomSliderSkin;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomSliderSkinTest {

    private Slider slider;
    private CustomSliderSkin skin;

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX runtime
        JavaFXInitializer.initialize();
    }

    @BeforeEach
    void setUp() {
        // Create components on JavaFX thread
        JavaFXInitializer.runLater(() -> {
            slider = new Slider(0, 100, 50);
            skin = new CustomSliderSkin(slider);
        });
    }

    @Test
    void testInitialization() {
        JavaFXInitializer.runLater(() -> {
            assertNotNull(skin.getThumb(), "Thumb should be initialized");
            assertNotNull(skin.getTrack(), "Track should be initialized");
            
            assertTrue(skin.getThumb() instanceof StackPane, "Thumb should be a StackPane");
            assertTrue(skin.getTrack() instanceof StackPane, "Track should be a StackPane");
            
            assertTrue(skin.getThumb().getStyleClass().contains("thumb"), 
                    "Thumb should have the 'thumb' style class");
            assertTrue(skin.getTrack().getStyleClass().contains("track"), 
                    "Track should have the 'track' style class");
        });
    }

    @Test
    void testGetThumb() {
        JavaFXInitializer.runLater(() -> {
            StackPane thumb = skin.getThumb();
            assertNotNull(thumb, "Thumb should not be null");
            assertEquals("thumb", thumb.getStyleClass().get(0), 
                    "Thumb should have the 'thumb' style class");
        });
    }

    @Test
    void testGetTrack() {
        JavaFXInitializer.runLater(() -> {
            StackPane track = skin.getTrack();
            assertNotNull(track, "Track should not be null");
            assertEquals("track", track.getStyleClass().get(0), 
                    "Track should have the 'track' style class");
        });
    }
} 