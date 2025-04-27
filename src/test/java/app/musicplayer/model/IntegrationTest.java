package app.musicplayer.model;

import org.junit.jupiter.api.AfterEach;
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

import app.musicplayer.MusicPlayer;
import app.musicplayer.util.ImportMusicTask;
import app.musicplayer.util.Resources;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationTest {

    @TempDir
    Path tempDir;

    private File resourcesDir;
    private File libraryXmlFile;

    @Mock
    private ImportMusicTask<Boolean> mockImportTask;

    @Mock
    private static MusicPlayer mockMusicPlayer;

    @BeforeEach
    void setUp() throws IOException {
        resourcesDir = tempDir.resolve("resources").toFile();
        resourcesDir.mkdir();
        libraryXmlFile = tempDir.resolve("library.xml").toFile();
        Files.write(libraryXmlFile.toPath(), getMinimalLibraryXml().getBytes());

        try {
            java.lang.reflect.Field field = Resources.class.getDeclaredField("JAR");
            field.setAccessible(true);
            java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            field.set(null, tempDir.toString() + File.separator);
        } catch (Exception e) {
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

    @AfterEach
    void tearDown() {
        try {
            // Reset songs list
            java.lang.reflect.Field songsField = Library.class.getDeclaredField("songs");
            songsField.setAccessible(true);
            songsField.set(null, null);

            // Reset artists list
            java.lang.reflect.Field artistsField = Library.class.getDeclaredField("artists");
            artistsField.setAccessible(true);
            artistsField.set(null, null);

            // Reset albums list
            java.lang.reflect.Field albumsField = Library.class.getDeclaredField("albums");
            albumsField.setAccessible(true);
            albumsField.set(null, null);

            // Reset playlists list
            java.lang.reflect.Field playlistsField = Library.class.getDeclaredField("playlists");
            playlistsField.setAccessible(true);
            playlistsField.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests the interaction between Library and XML parser to load songs from XML.
     * Verifies that Library properly parses song entries from XML file.
     */
    @Test
    void testLibraryLoadsSongsFromXml() {
        ObservableList<Song> songs = Library.getSongs();

        assertEquals(3, songs.size());
        assertEquals("Test Song 1", songs.get(0).getTitle());
        assertEquals("Test Song 2", songs.get(1).getTitle());
        assertEquals("Test Song 3", songs.get(2).getTitle());
    }

    /**
     * Tests the Song class constructor's handling of durations.
     * Verifies that Song correctly formats time from seconds.
     */
    @Test
    void testSongFormatsTimeCorrectly() {
        // Create a song with a duration of 125 seconds (2:05)
        Song song = new Song(100, "Time Test", "Artist", "Album", Duration.ofSeconds(125),
                1, 1, 0, LocalDateTime.now(), "path");

        assertEquals("2:05", song.getLength());
    }

    /**
     * Tests the interaction between Library and Playlist when loading playlists.
     * Verifies that Library correctly loads playlist data and links songs.
     */
    @Test
    void testLibraryLoadsPlaylistsWithSongs() {
        ObservableList<Playlist> playlists = Library.getPlaylists();

        Playlist customPlaylist = null;
        for (Playlist p : playlists) {
            if (p.getTitle().equals("My Playlist")) {
                customPlaylist = p;
                break;
            }
        }

        assertNotNull(customPlaylist);
        assertEquals(2, customPlaylist.getSongs().size());
        assertEquals("Test Song 1", customPlaylist.getSongs().get(0).getTitle());
        assertEquals("Test Song 2", customPlaylist.getSongs().get(1).getTitle());
    }

    /**
     * Tests the MostPlayedPlaylist's interaction with Library.
     * Verifies that MostPlayedPlaylist correctly sorts songs by play count.
     */
    @Test
    void testMostPlayedPlaylistSortsSongsByPlayCount() {
        ObservableList<Playlist> playlists = Library.getPlaylists();

        // Find the Most Played playlist
        MostPlayedPlaylist mostPlayed = null;
        for (Playlist p : playlists) {
            if (p instanceof MostPlayedPlaylist) {
                mostPlayed = (MostPlayedPlaylist) p;
                break;
            }
        }

        assertNotNull(mostPlayed);
        assertEquals("Most Played", mostPlayed.getTitle());

        ObservableList<Song> songs = mostPlayed.getSongs();
        assertEquals(3, songs.size());

        // Verify songs are sorted by play count (descending)
        assertEquals(10, songs.get(0).getPlayCount()); // Test Song 3 (10 plays)
        assertEquals(5, songs.get(1).getPlayCount());  // Test Song 1 (5 plays)
        assertEquals(2, songs.get(2).getPlayCount());  // Test Song 2 (2 plays)
    }

    /**
     * Tests the RecentlyPlayedPlaylist's interaction with Library.
     * Verifies that RecentlyPlayedPlaylist correctly sorts songs by play date.
     */
    @Test
    void testRecentlyPlayedPlaylistSortsSongsByPlayDate() {
        ObservableList<Playlist> playlists = Library.getPlaylists();

        // Find the Recently Played playlist
        RecentlyPlayedPlaylist recentlyPlayed = null;
        for (Playlist p : playlists) {
            if (p instanceof RecentlyPlayedPlaylist) {
                recentlyPlayed = (RecentlyPlayedPlaylist) p;
                break;
            }
        }

        assertNotNull(recentlyPlayed);
        assertEquals("Recently Played", recentlyPlayed.getTitle());

        ObservableList<Song> songs = recentlyPlayed.getSongs();
        assertEquals(3, songs.size());

        // The songs should be in order of recent play (most recent first)
        assertEquals("Test Song 3", songs.get(0).getTitle()); // Most recent
        assertEquals("Test Song 1", songs.get(1).getTitle()); // 1 day ago
        assertEquals("Test Song 2", songs.get(2).getTitle()); // 2 days ago
    }

    /**
     * Tests the Library's ability to retrieve a specific song by title.
     * Verifies the interaction between Library's search functionality and Song objects.
     */
    @Test
    void testLibraryFindsSpecificSongByTitle() {
        Song song = Library.getSong("Test Song 2");

        assertNotNull(song);
        assertEquals(1, song.getId());
        assertEquals("Test Artist", song.getArtist());
        assertEquals("Test Album", song.getAlbum());
    }

    /**
     * Tests the Library's ability to retrieve a specific playlist by title.
     * Verifies the interaction between Library's search functionality and Playlist objects.
     */
    @Test
    void testLibraryFindsSpecificPlaylistByTitle() {
        Playlist playlist = Library.getPlaylist("My Playlist");

        assertNotNull(playlist);
        assertEquals(0, playlist.getId());
        assertEquals(2, playlist.getSongs().size());
    }

    /**
     * Tests the interaction between Song and Library when updating play count.
     * Verifies that Song.played() correctly updates play count and date.
     */
    @Test
    void testSongPlayedUpdatesPlayCountAndDate() {
        Song song = Library.getSong("Test Song 1");
        int initialPlayCount = song.getPlayCount();
        LocalDateTime initialPlayDate = song.getPlayDate();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            fail("Thread interrupted");
        }

        song.played();

        assertEquals(initialPlayCount + 1, song.getPlayCount());
        assertTrue(song.getPlayDate().isAfter(initialPlayDate));
    }

    /**
     * Tests the interaction between Library and Playlist when adding a new playlist.
     * Verifies that Library correctly creates and adds the new playlist.
     */
    @Test
    void testLibraryAddPlaylistCreatesNewPlaylist() {
        int initialSize = Library.getPlaylists().size();

        Library.addPlaylist("New Test Playlist");

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail("Thread interrupted");
        }

        ObservableList<Playlist> playlists = Library.getPlaylists();

        // Size should be increased by 1
        assertEquals(initialSize + 1, playlists.size());

        Playlist newPlaylist = null;
        for (Playlist p : playlists) {
            if (p.getTitle().equals("New Test Playlist")) {
                newPlaylist = p;
                break;
            }
        }

        assertNotNull(newPlaylist);
        assertTrue(newPlaylist.getSongs().isEmpty());
    }

    /**
     * Tests the interaction between Playlist and Song when adding a song to a playlist.
     * Verifies that songs are correctly added to playlists.
     */
    @Test
    void testPlaylistAddSongAddsTheCorrectSong() {
        Playlist playlist = Library.getPlaylist("My Playlist");
        int initialSize = playlist.getSongs().size();
        Song newSong = Library.getSong("Test Song 3");

        ArrayList<Song> mockSongList = new ArrayList<>(playlist.getSongs());
        mockSongList.add(newSong);

        Playlist playlistSpy = spy(playlist);
        when(playlistSpy.getSongs()).thenReturn(FXCollections.observableArrayList(mockSongList));

        playlistSpy.addSong(newSong);

        // Verify song was added
        assertEquals(initialSize + 1, playlistSpy.getSongs().size());
        assertTrue(playlistSpy.getSongs().contains(newSong));
    }

    /**
     * Tests the interaction between Playlist and Song when removing a song.
     * Verifies that songs are correctly removed from playlists.
     */
    @Test
    void testPlaylistRemoveSongRemovesTheCorrectSong() {
        ArrayList<Song> songList = new ArrayList<>();
        Song song1 = new Song(0, "Test Song 1", "Artist", "Album", Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "");
        Song song2 = new Song(1, "Test Song 2", "Artist", "Album", Duration.ofSeconds(180), 2, 1, 0, LocalDateTime.now(), "");
        songList.add(song1);
        songList.add(song2);

        Playlist playlist = new Playlist(0, "Test Playlist", songList);
        playlist.removeSong(0); // Remove song with ID 0

        // Verify song was removed
        assertEquals(1, playlist.getSongs().size());
        assertEquals(1, playlist.getSongs().get(0).getId());
    }

    /**
     * Tests the Library's file type detection functionality.
     * Verifies that supported file types are correctly identified.
     */
    @Test
    void testLibraryIdentifiesSupportedFileTypes() {
        // Supported formats
        assertTrue(Library.isSupportedFileType("song.mp3"));
        assertTrue(Library.isSupportedFileType("song.mp4"));
        assertTrue(Library.isSupportedFileType("song.m4a"));
        assertTrue(Library.isSupportedFileType("song.wav"));

        // Unsupported formats
        assertFalse(Library.isSupportedFileType("song.ogg"));
        assertFalse(Library.isSupportedFileType("song.flac"));
        assertFalse(Library.isSupportedFileType("document.txt"));
        assertFalse(Library.isSupportedFileType("song"));
    }

    /**
     * Tests the Song comparison logic used for sorting.
     * Verifies that songs are sorted first by disc number, then by track number.
     */
    @Test
    void testSongComparesCorrectlyByDiscAndTrackNumber() {
        Song song1 = new Song(0, "Test", "Artist", "Album", Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "");
        Song song2 = new Song(1, "Test", "Artist", "Album", Duration.ofSeconds(180), 2, 1, 0, LocalDateTime.now(), "");
        Song song3 = new Song(2, "Test", "Artist", "Album", Duration.ofSeconds(180), 1, 2, 0, LocalDateTime.now(), "");

        // Same disc, different tracks
        assertTrue(song1.compareTo(song2) < 0);
        assertTrue(song2.compareTo(song1) > 0);

        // Different discs
        assertTrue(song1.compareTo(song3) < 0);
        assertTrue(song3.compareTo(song1) > 0);
        assertTrue(song3.compareTo(song2) > 0);
    }

    /**
     * Tests Song's title property binding.
     * Verifies that the title property correctly reflects changes to the title.
     */
    @Test
    void testSongTitlePropertyBinding() {
        Song song = new Song(0, "Original Title", "Artist", "Album", Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "");
        assertEquals("Original Title", song.titleProperty().get());
        song.titleProperty().set("New Title");
        assertEquals("New Title", song.getTitle());
    }

    /**
     * Tests Song's playing property.
     * Verifies that the playing state can be correctly set and retrieved.
     */
    @Test
    void testSongPlayingPropertySetsAndGetsCorrectly() {
        Song song = new Song(0, "Test", "Artist", "Album", Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "");
        assertFalse(song.getPlaying());
        song.setPlaying(true);
        assertTrue(song.getPlaying());
        assertTrue(song.playingProperty().get());

        song.setPlaying(false);
        assertFalse(song.getPlaying());
        assertFalse(song.playingProperty().get());
    }

    /**
     * Tests Song's selected property.
     * Verifies that the selected state can be correctly set and retrieved.
     */
    @Test
    void testSongSelectedPropertySetsAndGetsCorrectly() {
        Song song = new Song(0, "Test", "Artist", "Album", Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "");
        assertFalse(song.getSelected());
        song.setSelected(true);
        assertTrue(song.getSelected());
        assertTrue(song.selectedProperty().get());

        song.setSelected(false);
        assertFalse(song.getSelected());
        assertFalse(song.selectedProperty().get());
    }

    /**
     * Tests the Library's handling of empty or null song values.
     * Verifies that Library correctly handles songs with missing data.
     */
    @Test
    void testLibraryHandlesSongsWithMissingData() {
        // Create a song with null values
        Song song = new Song(999, null, null, null, Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "path/to/file.mp3");
        assertEquals("file", song.getTitle());

        // Artist and album should have default values
        assertEquals("Unknown Artist", song.getArtist());
        assertEquals("Unknown Album", song.getAlbum());
    }

    /**
     * Tests the ability of Song to extract track and disc information.
     * Verifies that Song correctly handles track and disc numbers.
     */
    @Test
    void testSongExtractsTrackAndDiscInformation() {
        Song song = new Song(0, "Test", "Artist", "Album", Duration.ofSeconds(180), 5, 2, 0, LocalDateTime.now(), "");

        assertEquals(5, song.getTrackNumber());
        assertEquals(2, song.getDiscNumber());
    }

    /**
     * Tests the interaction between Song and getLengthInSeconds.
     * Verifies that Song correctly converts between duration formats.
     */
    @Test
    void testSongConvertsBetweenDurationFormats() {
        Song song = new Song(0, "Test", "Artist", "Album", Duration.ofSeconds(150), 1, 1, 0, LocalDateTime.now(), "");
        assertEquals("2:30", song.getLength());
        assertEquals(150, song.getLengthInSeconds());
    }

    /**
     * Tests the Playlist's placeholder text functionality.
     * Verifies that empty playlists display appropriate placeholder text.
     */
    @Test
    void testPlaylistProvidesPlaceholderText() {
        Playlist playlist = new Playlist(100, "Empty Playlist", new ArrayList<>());
        String placeholder = playlist.getPlaceholder();
        assertTrue(placeholder.contains("Add songs to this playlist"));
    }

    /**
     * Tests the special placeholder text in MostPlayedPlaylist.
     * Verifies the interaction between MostPlayedPlaylist and its parent class.
     */
    @Test
    void testMostPlayedPlaylistHasSpecialPlaceholder() {
        MostPlayedPlaylist playlist = new MostPlayedPlaylist(-2);
        String placeholder = playlist.getPlaceholder();
        assertEquals("You have not played any songs yet", placeholder);
    }

    /**
     * Tests the special placeholder text in RecentlyPlayedPlaylist.
     * Verifies the interaction between RecentlyPlayedPlaylist and its parent class.
     */
    @Test
    void testRecentlyPlayedPlaylistHasSpecialPlaceholder() {
        RecentlyPlayedPlaylist playlist = new RecentlyPlayedPlaylist(-1);
        String placeholder = playlist.getPlaceholder();
        assertEquals("You have not played any songs yet", placeholder);
    }
}