package app.musicplayer.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import app.musicplayer.util.ImportMusicTask;
import app.musicplayer.util.Resources;
import javafx.collections.ObservableList;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IntegrationTest {

    @TempDir
    Path tempDir;

    private File resourcesDir;
    private File libraryXmlFile;

    @Mock
    private ImportMusicTask<Boolean> mockImportTask;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary directory structure that mimics the app resources
        resourcesDir = tempDir.resolve("resources").toFile();
        resourcesDir.mkdir();

        // Create a minimal library.xml file
        libraryXmlFile = tempDir.resolve("library.xml").toFile();
        Files.write(libraryXmlFile.toPath(), getMinimalLibraryXml().getBytes());

        // For static mocking, we'd need PowerMock or a similar framework
        // In a real test, you'd set up Resources.JAR redirection
        // For now, we'll use reflection to set the static field
        try {
            java.lang.reflect.Field field = Resources.class.getDeclaredField("JAR");
            field.setAccessible(true);
            java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            field.set(null, tempDir.toString() + File.separator);
        } catch (Exception e) {
            // In a real test, we'd handle this better
            e.printStackTrace();
        }
    }

    private String getMinimalLibraryXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<library>\n" +
                "    <musicLibrary>\n" +
                "        <path>" + tempDir.toString() + "</path>\n" +
                "        <fileNum>3</fileNum>\n" +
                "        <lastId>2</lastId>\n" +
                "    </musicLibrary>\n" +
                "    <songs>\n" +
                "        <song>\n" +
                "            <id>0</id>\n" +
                "            <title>Test Song 1</title>\n" +
                "            <artist>Test Artist</artist>\n" +
                "            <album>Test Album</album>\n" +
                "            <length>180</length>\n" +
                "            <trackNumber>1</trackNumber>\n" +
                "            <discNumber>1</discNumber>\n" +
                "            <playCount>5</playCount>\n" +
                "            <playDate>" + LocalDateTime.now().minusDays(1) + "</playDate>\n" +
                "            <location>" + tempDir.resolve("song1.mp3") + "</location>\n" +
                "        </song>\n" +
                "        <song>\n" +
                "            <id>1</id>\n" +
                "            <title>Test Song 2</title>\n" +
                "            <artist>Test Artist</artist>\n" +
                "            <album>Test Album</album>\n" +
                "            <length>240</length>\n" +
                "            <trackNumber>2</trackNumber>\n" +
                "            <discNumber>1</discNumber>\n" +
                "            <playCount>2</playCount>\n" +
                "            <playDate>" + LocalDateTime.now().minusDays(2) + "</playDate>\n" +
                "            <location>" + tempDir.resolve("song2.mp3") + "</location>\n" +
                "        </song>\n" +
                "        <song>\n" +
                "            <id>2</id>\n" +
                "            <title>Test Song 3</title>\n" +
                "            <artist>Another Artist</artist>\n" +
                "            <album>Another Album</album>\n" +
                "            <length>200</length>\n" +
                "            <trackNumber>1</trackNumber>\n" +
                "            <discNumber>1</discNumber>\n" +
                "            <playCount>10</playCount>\n" +
                "            <playDate>" + LocalDateTime.now() + "</playDate>\n" +
                "            <location>" + tempDir.resolve("song3.mp3") + "</location>\n" +
                "        </song>\n" +
                "    </songs>\n" +
                "    <playlists>\n" +
                "        <playlist id=\"0\" title=\"My Playlist\">\n" +
                "            <songId>0</songId>\n" +
                "            <songId>1</songId>\n" +
                "        </playlist>\n" +
                "    </playlists>\n" +
                "    <nowPlayingList>\n" +
                "        <id>0</id>\n" +
                "        <id>1</id>\n" +
                "    </nowPlayingList>\n" +
                "</library>";
    }

    @Test
    void testGetSongs() {
        // Test that all songs are loaded from the XML file
        ObservableList<Song> songs = Library.getSongs();

        assertEquals(3, songs.size());
        assertEquals("Test Song 1", songs.get(0).getTitle());
        assertEquals("Test Song 2", songs.get(1).getTitle());
        assertEquals("Test Song 3", songs.get(2).getTitle());
    }

    @Test
    void testGetPlaylists() {
        // Test that playlists are correctly loaded from XML
        ObservableList<Playlist> playlists = Library.getPlaylists();

        // +2 for the built-in Most Played and Recently Played playlists
        assertEquals(3, playlists.size());

        // Check that the custom playlist is loaded correctly
        Playlist customPlaylist = playlists.get(0);
        assertEquals("My Playlist", customPlaylist.getTitle());
        assertEquals(2, customPlaylist.getSongs().size());
    }

    @Test
    void testMostPlayedPlaylist() {
        // Test the Most Played playlist functionality
        ObservableList<Playlist> playlists = Library.getPlaylists();

        // Find the Most Played playlist
        Playlist mostPlayed = playlists.stream()
                .filter(p -> p.getTitle().equals("Most Played"))
                .findFirst()
                .orElse(null);

        assertNotNull(mostPlayed);
        ObservableList<Song> songs = mostPlayed.getSongs();

        // Should be sorted by play count in descending order
        assertEquals(3, songs.size());
        assertEquals("Test Song 3", songs.get(0).getTitle()); // 10 plays
        assertEquals("Test Song 1", songs.get(1).getTitle()); // 5 plays
        assertEquals("Test Song 2", songs.get(2).getTitle()); // 2 plays
    }

    @Test
    void testGetSongById() {
        // Test getting a song by its title
        Song song = Library.getSong("Test Song 2");

        assertNotNull(song);
        assertEquals(1, song.getId());
        assertEquals("Test Artist", song.getArtist());
        assertEquals("Test Album", song.getAlbum());
    }

    @Test
    void testLoadPlayingList() {
        // Test loading the now playing list
        ArrayList<Song> nowPlaying = Library.loadPlayingList();

        assertEquals(2, nowPlaying.size());
        assertEquals(0, nowPlaying.get(0).getId());
        assertEquals(1, nowPlaying.get(1).getId());
    }

    @Test
    void testPlayedIncrementsPlayCount() {
        // Test that the played() method increments play count and updates play date
        Song song = Library.getSong("Test Song 1");
        int initialPlayCount = song.getPlayCount();
        LocalDateTime initialPlayDate = song.getPlayDate();

        // We can't easily mock the static method, so we'll just run it
        // In a real test, you'd use a static mocking framework like PowerMock

        song.played();

        assertEquals(initialPlayCount + 1, song.getPlayCount());
        assertTrue(song.getPlayDate().isAfter(initialPlayDate));
    }

    @Test
    void testAddPlaylist() {
        // Test adding a new playlist
        int initialSize = Library.getPlaylists().size();

        Library.addPlaylist("New Test Playlist");

        // Allow time for the async operation to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail("Thread interrupted");
        }

        ObservableList<Playlist> playlists = Library.getPlaylists();
        assertEquals(initialSize + 1, playlists.size());

        // Find the new playlist
        Playlist newPlaylist = playlists.stream()
                .filter(p -> p.getTitle().equals("New Test Playlist"))
                .findFirst()
                .orElse(null);

        assertNotNull(newPlaylist);
        assertTrue(newPlaylist.getSongs().isEmpty());
    }

    @Test
    void testRemoveSongFromPlaylist() {
        // Test removing a song from a playlist
        Playlist playlist = Library.getPlaylist("My Playlist");
        int initialSize = playlist.getSongs().size();
        Song songToRemove = playlist.getSongs().get(0);

        playlist.removeSong(songToRemove.getId());

        assertEquals(initialSize - 1, playlist.getSongs().size());
        assertFalse(playlist.getSongs().contains(songToRemove));
    }

    @Test
    void testIsSupportedFileType() {
        // Test the file type detection
        assertTrue(Library.isSupportedFileType("song.mp3"));
        assertTrue(Library.isSupportedFileType("song.mp4"));
        assertTrue(Library.isSupportedFileType("song.m4a"));
        assertTrue(Library.isSupportedFileType("song.wav"));

        assertFalse(Library.isSupportedFileType("song.ogg"));
        assertFalse(Library.isSupportedFileType("song.flac"));
        assertFalse(Library.isSupportedFileType("document.txt"));
    }

    @Test
    void testSongComparison() {
        // Test the song comparison logic (by disc and track number)
        Song song1 = new Song(0, "Test", "Artist", "Album", Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "");
        Song song2 = new Song(1, "Test", "Artist", "Album", Duration.ofSeconds(180), 2, 1, 0, LocalDateTime.now(), "");
        Song song3 = new Song(2, "Test", "Artist", "Album", Duration.ofSeconds(180), 1, 2, 0, LocalDateTime.now(), "");

        // Same disc, different tracks
        assertTrue(song1.compareTo(song2) < 0); // track 1 < track 2

        // Different discs
        assertTrue(song1.compareTo(song3) < 0); // disc 1 < disc 2
        assertTrue(song3.compareTo(song2) > 0); // disc 2 > disc 1
    }
}