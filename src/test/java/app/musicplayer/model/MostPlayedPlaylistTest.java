package app.musicplayer.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@ExtendWith(MockitoExtension.class)
public class MostPlayedPlaylistTest {

    private MostPlayedPlaylist playlist;
    private List<Song> testSongs;

    @BeforeEach
    public void setup() {
        playlist = new MostPlayedPlaylist(-2);
        testSongs = new ArrayList<>();

        // Create test songs with different play counts
        for (int i = 0; i < 10; i++) {
            testSongs.add(createSong(i, "Song " + i, "Artist", "Album", i));
        }
    }

    @Test
    public void testConstructor() {
        assertEquals(-2, playlist.getId());
        assertEquals("Most Played", playlist.getTitle());
    }

    @Test
    public void testGetSongsWithNoPlayedSongs() {
        // Create songs with playCount of 0
        List<Song> zeroPlayCountSongs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            zeroPlayCountSongs.add(createSong(i, "Zero Play Song " + i, "Artist", "Album", 0));
        }

        try (MockedStatic<Library> mockedLibrary = mockStatic(Library.class)) {
            mockedLibrary.when(Library::getSongs).thenReturn(FXCollections.observableArrayList(zeroPlayCountSongs));

            // getSongs() method should return an empty list since no songs have been played
            ObservableList<Song> result = playlist.getSongs();

            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testGetSongsWithPlayedSongs() {
        try (MockedStatic<Library> mockedLibrary = mockStatic(Library.class)) {
            mockedLibrary.when(Library::getSongs).thenReturn(FXCollections.observableArrayList(testSongs));

            ObservableList<Song> result = playlist.getSongs();

            // Should exclude songs with playCount = 0
            assertEquals(9, result.size());

            assertEquals(9, result.get(0).getPlayCount());
            assertEquals(8, result.get(1).getPlayCount());
            assertEquals(1, result.get(8).getPlayCount());
        }
    }

    @Test
    public void testGetSongsWithMoreThan100Songs() {
        // Create a list with more than 100 songs
        List<Song> manySongs = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            manySongs.add(createSong(i, "Song " + i, "Artist", "Album", i + 1)); // All songs have playCount > 0
        }

        try (MockedStatic<Library> mockedLibrary = mockStatic(Library.class)) {
            mockedLibrary.when(Library::getSongs).thenReturn(FXCollections.observableArrayList(manySongs));

            ObservableList<Song> result = playlist.getSongs();

            assertEquals(100, result.size());

            // Check if the songs with highest playCount are included
            assertEquals(150, result.get(0).getPlayCount());
            assertEquals(149, result.get(1).getPlayCount());
            assertEquals(51, result.get(99).getPlayCount());
        }
    }

    private Song createSong(int id, String title, String artist, String album, int playCount) {
        return new Song(
                id,
                title,
                artist,
                album,
                Duration.ofSeconds(180),
                1,
                1,
                playCount,
                LocalDateTime.now(),
                "path/to/song" + id + ".mp3"
        );
    }
}