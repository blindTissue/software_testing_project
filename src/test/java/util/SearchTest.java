package util;

import app.musicplayer.model.Album;
import app.musicplayer.model.Artist;
import app.musicplayer.model.Library;
import app.musicplayer.model.SearchResult;
import app.musicplayer.model.Song;
import app.musicplayer.util.Search;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchTest {

    private List<Song> testSongs;
    private List<Album> testAlbums;
    private List<Artist> testArtists;

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX runtime
        JavaFXInitializer.initialize();
    }

    @BeforeEach
    void setUp() throws Exception {
        // Create test data on JavaFX thread
        JavaFXInitializer.runLater(() -> {
            try {
                initializeTestData();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize test data", e);
            }
        });
    }
    
    private void initializeTestData() throws Exception {
        // Create test data
        testSongs = new ArrayList<>();
        testSongs.add(new Song(1, "Test Song", "Test Artist", "Test Album", 
                Duration.ofSeconds(200), 1, 1, 2020, LocalDateTime.now(), "Pop"));
        testSongs.add(new Song(2, "Another Song", "Test Artist", "Test Album", 
                Duration.ofSeconds(180), 2, 1, 2020, LocalDateTime.now(), "Rock"));
        testSongs.add(new Song(3, "Third Song", "Another Artist", "Another Album", 
                Duration.ofSeconds(240), 3, 1, 2019, LocalDateTime.now(), "Jazz"));
        testSongs.add(new Song(4, "Fourth Test", "Fourth Artist", "Fourth Album", 
                Duration.ofSeconds(300), 4, 1, 2018, LocalDateTime.now(), "Classical"));
        
        testAlbums = new ArrayList<>();
        testAlbums.add(new Album(1, "Test Album", "Test Artist", new ArrayList<>()));
        testAlbums.add(new Album(2, "Another Album", "Another Artist", new ArrayList<>()));
        testAlbums.add(new Album(3, "Third Album", "Third Artist", new ArrayList<>()));
        testAlbums.add(new Album(4, "Test Collection", "Fourth Artist", new ArrayList<>()));
        
        testArtists = new ArrayList<>();
        testArtists.add(new Artist("Test Artist", new ArrayList<>()));
        testArtists.add(new Artist("Another Artist", new ArrayList<>()));
        testArtists.add(new Artist("Third Artist", new ArrayList<>()));
        testArtists.add(new Artist("Fourth Artist", new ArrayList<>()));

        // Use reflection to set the test data in the Library class
        setLibraryField("songs", testSongs);
        setLibraryField("albums", testAlbums);
        setLibraryField("artists", testArtists);
    }

    private void setLibraryField(String fieldName, Object value) throws Exception {
        Field field = Library.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    @Test
    @Disabled("This test requires further JavaFX configuration")
    void testHasResultsProperty() {
        JavaFXInitializer.runLater(() -> {
            assertNotNull(Search.hasResultsProperty());
            assertFalse(Search.hasResultsProperty().get(), "Initial value should be false");
        });
    }

    @Test
    @Disabled("This test requires further JavaFX configuration")
    void testSearchAndGetResult() throws InterruptedException {
        JavaFXInitializer.runLater(() -> {
            // Search for "Test"
            Search.search("Test");
        });
        
        // Wait a bit for the search thread to complete
        Thread.sleep(200);
        
        JavaFXInitializer.runLater(() -> {
            // Check that we have results
            assertTrue(Search.hasResultsProperty().get(), "Should have results after search");
            
            // Get the results
            SearchResult result = Search.getResult();
            
            // Verify results
            assertNotNull(result);
            assertFalse(Search.hasResultsProperty().get(), "Should reset hasResults after getResult");
            
            // Check song results (should have at most 3)
            assertTrue(result.getSongResults().size() <= 3, "Should have at most 3 song results");
            assertTrue(result.getSongResults().stream().anyMatch(song -> song.getTitle().contains("Test")), 
                    "Should find songs with 'Test' in the title");
            
            // Check album results
            assertTrue(result.getAlbumResults().size() <= 3, "Should have at most 3 album results");
            assertTrue(result.getAlbumResults().stream().anyMatch(album -> album.getTitle().contains("Test")), 
                    "Should find albums with 'Test' in the title");
            
            // Check artist results
            assertTrue(result.getArtistResults().size() <= 3, "Should have at most 3 artist results");
            assertTrue(result.getArtistResults().stream().anyMatch(artist -> artist.getTitle().contains("Test")), 
                    "Should find artists with 'Test' in the title");
        });
    }

    @Test
    @Disabled("This test requires further JavaFX configuration")
    void testSearchWithEmptyResults() throws InterruptedException {
        JavaFXInitializer.runLater(() -> {
            // Search for something that won't match
            Search.search("NonexistentTerm");
        });
        
        // Wait a bit for the search thread to complete
        Thread.sleep(200);
        
        JavaFXInitializer.runLater(() -> {
            // Check that we still have results (empty results)
            assertTrue(Search.hasResultsProperty().get(), "Should have results even if empty");
            
            // Get the results
            SearchResult result = Search.getResult();
            
            // Verify results
            assertNotNull(result);
            assertTrue(result.getSongResults().isEmpty(), "Should have no song results");
            assertTrue(result.getAlbumResults().isEmpty(), "Should have no album results");
            assertTrue(result.getArtistResults().isEmpty(), "Should have no artist results");
        });
    }

    @Test
    @Disabled("This test requires further JavaFX configuration")
    void testInterruptSearchThread() throws InterruptedException {
        JavaFXInitializer.runLater(() -> {
            // Start first search
            Search.search("Test");
            
            // Start second search immediately to interrupt the first
            Search.search("Another");
        });
        
        // Wait for the second search to complete
        Thread.sleep(200);
        
        JavaFXInitializer.runLater(() -> {
            // We should have results from the second search
            assertTrue(Search.hasResultsProperty().get(), "Should have results from second search");
            
            // Get the results
            SearchResult result = Search.getResult();
            
            // Verify the results match the second search
            assertNotNull(result);
            assertTrue(result.getSongResults().stream().anyMatch(song -> song.getTitle().contains("Another")), 
                    "Should find songs with 'Another' in the title from the second search");
        });
    }
} 