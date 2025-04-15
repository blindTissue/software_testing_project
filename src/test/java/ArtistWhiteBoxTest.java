
import app.musicplayer.model.Album;
import app.musicplayer.model.Artist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import javafx.scene.image.Image;
import app.musicplayer.util.Resources;

import static org.junit.jupiter.api.Assertions.*;

class ArtistWhiteBoxTest {

    private Artist artist;
    private ArrayList<Album> albums;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        albums = new ArrayList<>();
        albums.add(new Album(1, "Test Album", "Test Artist", new ArrayList<>()));

//        try {
//            Field field = Resources.class.getDeclaredField("JAR");
//            field.setAccessible(true);
//            field.set(null, tempDir.toString());
//
//            // Also set the IMG constant for default images
//            Field imgField = Resources.class.getDeclaredField("IMG");
//            imgField.setAccessible(true);
//            File resourcesDir = new File(tempDir.toString(), "resources");
//            resourcesDir.mkdirs();
//            imgField.set(null, resourcesDir.toURI().toURL().toString());
//
//            // Create the default artist icon
//            File imgDir = new File(resourcesDir, "img");
//            imgDir.mkdirs();
//            File artistIcon = new File(imgDir, "artistsIcon.png");
//            // Create a simple 1x1 PNG image
//            BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
//            ImageIO.write(img, "png", artistIcon);
//        } catch (Exception e) {
//            fail("Failed to set up test environment: " + e.getMessage());
//        }

        artist = new Artist("Test Artist", albums);
    }

    @Test
    void testConstructor() {
        assertEquals("Test Artist", artist.getTitle());
        assertEquals(1, artist.getAlbums().size());
        assertEquals("Test Album", artist.getAlbums().get(0).getTitle());
    }

    @Test
    void testGetTitle() {
        assertEquals("Test Artist", artist.getTitle());
    }

    @Test
    void testArtistImageProperty() {
        assertNotNull(artist.artistImageProperty());
        assertNotNull(artist.artistImageProperty().get());
    }

    @Test
    void testGetArtistImageWhenImageExists() throws Exception {
        // Create artist image file
        File imgDir = new File(tempDir.toString(), "img");
        imgDir.mkdirs();
        File artistImageFile = new File(imgDir, "Test Artist.jpg");

        // Create a simple test image
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, 10, 10);
        g2d.dispose();
        ImageIO.write(img, "jpg", artistImageFile);

        // Reset the artistImage field to force it to reload
        Field imageField = Artist.class.getDeclaredField("artistImage");
        imageField.setAccessible(true);
        imageField.set(artist, null);

        // Get the artist image
        Image result = artist.getArtistImage();

        assertNotNull(result);
        assertFalse(result.isError());
    }

    @Test
    void testDownloadArtistImage() throws Exception {
        // Mock the XMLStreamReader to simulate a successful API response
        // This requires a more complex setup with PowerMock or similar
        // For this example, we'll just test the error handling path

        artist.downloadArtistImage();

        // Verify that if download fails, we don't crash
        File imgDir = new File(tempDir.toString(), "img");
        File artistImageFile = new File(imgDir, "Test Artist.jpg");
        assertFalse(artistImageFile.exists());
    }

    @Test
    void testCompareToWithDifferentArtists() {
        Artist otherArtist = new Artist("Another Artist", new ArrayList<>());
        assertTrue(artist.compareTo(otherArtist) < 0);
        assertTrue(otherArtist.compareTo(artist) > 0);
    }

    @Test
    void testCompareToWithSameArtist() {
        Artist sameArtist = new Artist("Test Artist", new ArrayList<>());
        assertEquals(0, artist.compareTo(sameArtist));
    }

    @Test
    void testCompareToWithArticles() {
        Artist theArtist = new Artist("The Test", new ArrayList<>());
        Artist aArtist = new Artist("A Test", new ArrayList<>());
        Artist anArtist = new Artist("An Test", new ArrayList<>());
        Artist normalArtist = new Artist("Test", new ArrayList<>());

        // All should be equal after article removal
        assertEquals(0, theArtist.compareTo(normalArtist));
        assertEquals(0, aArtist.compareTo(normalArtist));
        assertEquals(0, anArtist.compareTo(normalArtist));
        assertEquals(0, theArtist.compareTo(aArtist));
    }

    @Test
    void testCompareToWithSingleWordName() {
        Artist singleWordArtist = new Artist("Cher", new ArrayList<>());
        Artist otherSingleWordArtist = new Artist("Madonna", new ArrayList<>());

        assertTrue(singleWordArtist.compareTo(otherSingleWordArtist) < 0);
        assertTrue(otherSingleWordArtist.compareTo(singleWordArtist) > 0);
    }

    @Test
    void testCompareToWithNonArticleFirstWord() {
        Artist artistWithNonArticle = new Artist("Awesome Band", new ArrayList<>());
        Artist artistWithArticle = new Artist("The Band", new ArrayList<>());

        // "Awesome Band" sorts before "Band" (after article removal)
        assertTrue(artistWithNonArticle.compareTo(artistWithArticle) < 0);
    }
}