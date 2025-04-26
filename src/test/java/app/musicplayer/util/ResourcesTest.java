package app.musicplayer.util;

import app.musicplayer.util.Resources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourcesTest {

    @Test
    void testResourceConstants() {
        // Test that the constants are not null
        assertNotNull(Resources.FXML);
        assertNotNull(Resources.IMG);
        assertNotNull(Resources.CSS);
        assertNotNull(Resources.APIBASE);
        assertNotNull(Resources.APIKEY);
        
        // Test specific values
        assertEquals("/app/musicplayer/view/", Resources.FXML);
        assertEquals("/app/musicplayer/util/img/", Resources.IMG);
        assertEquals("/app/musicplayer/util/css/", Resources.CSS);
        assertEquals("http://ws.audioscrobbler.com/2.0/?", Resources.APIBASE);
        assertEquals("57ee3318536b23ee81d6b27e36997cde", Resources.APIKEY);
    }
    
    @Test
    void testJarInitialValue() {
        // JAR may be null initially before being set by the application
        // This just verifies the field exists
        Resources.JAR = "test-path/";
        assertEquals("test-path/", Resources.JAR);
    }
} 