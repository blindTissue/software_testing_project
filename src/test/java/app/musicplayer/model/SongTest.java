package app.musicplayer.model;

import app.musicplayer.util.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;
import javafx.scene.image.Image;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SongTest {

    private Song song;
    private int id;
    private String title;
    private String artist;
    private String album;
    private Duration length;
    private int trackNumber;
    private int discNumber;
    private int playCount;
    private LocalDateTime playDate;
    private String location;

    @BeforeEach
    void setUp() {
        id = 1;
        title = "Test Song";
        artist = "Test Artist";
        album = "Test Album";
        length = Duration.ofMinutes(3).plusSeconds(30);
        trackNumber = 1;
        discNumber = 1;
        playCount = 0;
        playDate = LocalDateTime.now();
        location = "/music/test.mp3";

        song = new Song(id, title, artist, album, length, trackNumber, discNumber,
                playCount, playDate, location);
    }

    @Test
    void testConstructorWithAllFields() {
        assertEquals(id, song.getId());
        assertEquals(title, song.getTitle());
        assertEquals(artist, song.getArtist());
        assertEquals(album, song.getAlbum());
        assertEquals("3:30", song.getLength());
        assertEquals(trackNumber, song.getTrackNumber());
        assertEquals(discNumber, song.getDiscNumber());
        assertEquals(playCount, song.getPlayCount());
        assertEquals(playDate, song.getPlayDate());
        assertEquals(location, song.getLocation());
        assertFalse(song.getPlaying());
        assertFalse(song.getSelected());
    }

    // When the title parameter is null, the name of the mp3 file should be assigned to the song.
    @Test
    void testConstructorWithNullTitle() {
        Song songWithNullTitle = new Song(id, null, artist, album, length,
                trackNumber, discNumber, playCount,
                playDate, "/music/testfile.mp3");

        assertEquals("testfile", songWithNullTitle.getTitle());
    }

    @Test
    void testConstructorWithNullAlbum() {
        Song songWithNullAlbum = new Song(id, title, artist, null, length,
                trackNumber, discNumber, playCount,
                playDate, location);

        assertEquals("Unknown Album", songWithNullAlbum.getAlbum());
    }

    @Test
    void testConstructorWithNullArtist() {
        Song songWithNullArtist = new Song(id, title, null, album, length,
                trackNumber, discNumber, playCount,
                playDate, location);

        assertEquals("Unknown Artist", songWithNullArtist.getArtist());
    }

    @Test
    void testLengthFormattingWithSingleDigitSeconds() {
        Duration shortLength = Duration.ofMinutes(2).plusSeconds(5);
        Song songWithShortLength = new Song(id, title, artist, album, shortLength,
                trackNumber, discNumber, playCount,
                playDate, location);

        assertEquals("2:05", songWithShortLength.getLength());
    }

    @Test
    void testLengthFormattingWithDoubleDigitSeconds() {
        Duration longLength = Duration.ofMinutes(2).plusSeconds(45);
        Song songWithLongLength = new Song(id, title, artist, album, longLength,
                trackNumber, discNumber, playCount,
                playDate, location);

        assertEquals("2:45", songWithLongLength.getLength());
    }

    @Test
    void testGetLengthInSeconds() {
        assertEquals(210, song.getLengthInSeconds());
    }

    @Test
    void testSetAndGetPlaying() {
        assertFalse(song.getPlaying());

        song.setPlaying(true);
        assertTrue(song.getPlaying());

        song.setPlaying(false);
        assertFalse(song.getPlaying());
    }

    @Test
    void testSetAndGetSelected() {
        assertFalse(song.getSelected());

        song.setSelected(true);
        assertTrue(song.getSelected());

        song.setSelected(false);
        assertFalse(song.getSelected());
    }

    @Test
    void testTitleProperty() {
        assertEquals(title, song.titleProperty().get());
    }

    @Test
    void testArtistProperty() {
        assertEquals(artist, song.artistProperty().get());
    }

    @Test
    void testAlbumProperty() {
        assertEquals(album, song.albumProperty().get());
    }

    @Test
    void testLengthProperty() {
        assertEquals("3:30", song.lengthProperty().get());
    }

    @Test
    void testPlayCountProperty() {
        assertEquals(playCount, song.playCountProperty().get());
    }

    @Test
    void testPlayingProperty() {
        assertFalse(song.playingProperty().get());

        song.setPlaying(true);
        assertTrue(song.playingProperty().get());
    }

    @Test
    void testSelectedProperty() {
        assertFalse(song.selectedProperty().get());

        song.setSelected(true);
        assertTrue(song.selectedProperty().get());
    }

    @Test
    void testCompareToWithDifferentDiscNumbers() {
        Song otherSong = new Song(2, "Other Song", artist, album, length,
                trackNumber, discNumber + 1, playCount,
                playDate, location);

        assertTrue(song.compareTo(otherSong) < 0);
        assertTrue(otherSong.compareTo(song) > 0);
    }

    @Test
    void testCompareToWithSameDiscNumberButDifferentTrackNumbers() {
        Song otherSong = new Song(2, "Other Song", artist, album, length,
                trackNumber + 1, discNumber, playCount,
                playDate, location);

        assertTrue(song.compareTo(otherSong) < 0);
        assertTrue(otherSong.compareTo(song) > 0);
    }

    @Test
    void testCompareToWithSameDiscAndTrackNumbers() {
        Song otherSong = new Song(2, "Other Song", artist, album, length,
                trackNumber, discNumber, playCount,
                playDate, location);

        assertEquals(0, song.compareTo(otherSong));
    }

    // Mock testing
    @Test
    void testGetArtwork() {
        // set up mocks
        Album mockAlbum = mock(Album.class);
        Image mockImage = mock(Image.class);
        when(mockAlbum.getArtwork()).thenReturn(mockImage);

        try (MockedStatic<Library> mockedLibrary = mockStatic(Library.class)) {
            mockedLibrary.when(() -> Library.getAlbum(album)).thenReturn(mockAlbum);
            Image result = song.getArtwork();
            assertSame(mockImage, result);
            mockedLibrary.verify(() -> Library.getAlbum(album));
        }
    }

    /**
     * Mock Testing
     * We use mocking here because the method interacts with other classes and external resources (XML file) which makes testing difficult.
     * We do not plan to test this method using the actual classes.
     */
    @Test
    void testPlayedWithFullCoverage() throws Exception {
        // Store initial values
        int initialPlayCount = song.getPlayCount();
        LocalDateTime initialPlayDate = song.getPlayDate();

        // Create a temporary directory and file
        Path tempDir = Files.createTempDirectory("test-library");
        File xmlFile = new File(tempDir.toFile(), "library.xml");

        // Create a simple XML file
        String xmlContent =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<library>\n" +
                        "  <songs>\n" +
                        "    <song>\n" +
                        "      <id>" + id + "</id>\n" +
                        "      <playCount>" + playCount + "</playCount>\n" +
                        "      <playDate>" + playDate + "</playDate>\n" +
                        "    </song>\n" +
                        "  </songs>\n" +
                        "</library>";

        Files.write(xmlFile.toPath(), xmlContent.getBytes());

        // Use reflection to temporarily modify the Resources.JAR static field
        Field jarField = Resources.class.getDeclaredField("JAR");
        jarField.setAccessible(true);

        String originalValue = (String) jarField.get(null);

        try {
            jarField.set(null, tempDir + File.separator);
            song.played();
            Thread.sleep(500);
            // Verify memory updates
            assertEquals(initialPlayCount + 1, song.getPlayCount());
            assertNotEquals(initialPlayDate, song.getPlayDate());
            // Read back the XML file to verify it was updated
            String updatedXml = new String(Files.readAllBytes(xmlFile.toPath()));
            assertTrue(updatedXml.contains("<playCount>" + (initialPlayCount + 1) + "</playCount>"));
            assertTrue(updatedXml.contains("<playDate>" + song.getPlayDate().toString() + "</playDate>"));

        } finally {
            jarField.set(null, originalValue);
            // Clean up
            Files.deleteIfExists(xmlFile.toPath());
            Files.deleteIfExists(tempDir);
        }
    }

}