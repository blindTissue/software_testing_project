package app.musicplayer.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class RecentlyPlayedPlaylistTest {

    private RecentlyPlayedPlaylist playlist;
    private List<Song> testSongs;

    @BeforeEach
    public void setUp() {
        playlist = new RecentlyPlayedPlaylist(1);
        testSongs = new ArrayList<>();

        // Create test songs with different play counts and dates
        for (int i = 0; i < 150; i++) {
            LocalDateTime playDate = LocalDateTime.now().minusMinutes(i);
            int playCount = i % 5 == 0 ? 0 : (i % 10 + 1);

            Song song = new Song(
                    i,
                    "Title " + i,
                    "Artist " + i,
                    "Album " + i,
                    Duration.ofMinutes(3).plusSeconds(i % 60),
                    i % 20 + 1,
                    i % 5 + 1,
                    playCount,
                    playDate,
                    "location/song" + i + ".mp3"
            );

            testSongs.add(song);
        }
    }

    @Test
    public void testConstructor() {
        assertEquals(1, playlist.getId());
    }

    @Test
    public void testGetSongsWithPlayedSongs() {
        try (MockedStatic<Library> mockedLibrary = Mockito.mockStatic(Library.class)) {
            ObservableList<Song> observableTestSongs = FXCollections.observableArrayList(testSongs);
            mockedLibrary.when(Library::getSongs).thenReturn(observableTestSongs);

            ObservableList<Song> result = playlist.getSongs();

            // all songs should have playCount > 0
            assertTrue(result.stream().allMatch(song -> song.getPlayCount() > 0));

            // songs should be sorted by play date (most recent first)
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).getPlayDate().isAfter(result.get(i + 1).getPlayDate())
                        || result.get(i).getPlayDate().isEqual(result.get(i + 1).getPlayDate()));
            }

            // no more than 100 songs should be returned
            assertTrue(result.size() <= 100);
            assertEquals(100, result.size());
        }
    }

    @Test
    public void testGetSongsWithNoPlayedSongs() {
        // Create a list with only unplayed songs (playCount = 0)
        List<Song> unplayedSongs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Song song = new Song(
                    i,
                    "Title " + i,
                    "Artist " + i,
                    "Album " + i,
                    Duration.ofMinutes(3),
                    i + 1,
                    1,
                    0,
                    null,
                    "location/song" + i + ".mp3"
            );

            unplayedSongs.add(song);
        }

        try (MockedStatic<Library> mockedLibrary = Mockito.mockStatic(Library.class)) {
            ObservableList<Song> observableUnplayedSongs = FXCollections.observableArrayList(unplayedSongs);
            mockedLibrary.when(Library::getSongs).thenReturn(observableUnplayedSongs);

            ObservableList<Song> result = playlist.getSongs();

            // Verify that no songs are returned since all have playCount = 0
            assertEquals(0, result.size());
        }
    }

    @Test
    public void testGetSongsWithFewerThan100PlayedSongs() {
        List<Song> fewPlayedSongs = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            LocalDateTime playDate = LocalDateTime.now().minusMinutes(i);
            Song song = new Song(
                    i,
                    "Title " + i,
                    "Artist " + i,
                    "Album " + i,
                    Duration.ofMinutes(3),
                    i + 1,
                    1,
                    i + 1, // playCount > 0
                    playDate,
                    "location/song" + i + ".mp3"
            );

            fewPlayedSongs.add(song);
        }

        try (MockedStatic<Library> mockedLibrary = Mockito.mockStatic(Library.class)) {
            ObservableList<Song> observableFewPlayedSongs = FXCollections.observableArrayList(fewPlayedSongs);
            mockedLibrary.when(Library::getSongs).thenReturn(observableFewPlayedSongs);

            ObservableList<Song> result = playlist.getSongs();

            // Verify that all 50 songs are returned
            assertEquals(50, result.size());

            assertTrue(result.stream().allMatch(song -> song.getPlayCount() > 0));
            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i).getPlayDate().isAfter(result.get(i + 1).getPlayDate())
                        || result.get(i).getPlayDate().isEqual(result.get(i + 1).getPlayDate()));
            }
        }
    }

    @Test
    public void testGetSongsWithEmptyLibrary() {
        try (MockedStatic<Library> mockedLibrary = Mockito.mockStatic(Library.class)) {
            ObservableList<Song> emptyObservableList = FXCollections.observableArrayList();
            mockedLibrary.when(Library::getSongs).thenReturn(emptyObservableList);

            ObservableList<Song> result = playlist.getSongs();

            // Verify that no songs are returned with empty library
            assertEquals(0, result.size());
        }
    }
}