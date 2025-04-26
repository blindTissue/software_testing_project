package app.musicplayer.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SearchResultTest {

    @Mock
    private List<Song> mockSongList;

    @Mock
    private List<Album> mockAlbumList;

    @Mock
    private List<Artist> mockArtistList;

    private SearchResult searchResult;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConstructorAndGetters() {
        searchResult = new SearchResult(mockSongList, mockAlbumList, mockArtistList);
        assertSame(mockSongList, searchResult.getSongResults());
        assertSame(mockAlbumList, searchResult.getAlbumResults());
        assertSame(mockArtistList, searchResult.getArtistResults());
    }

    @Test
    void testConstructorWithEmptyLists() {
        List<Song> emptySongList = new ArrayList<>();
        List<Album> emptyAlbumList = new ArrayList<>();
        List<Artist> emptyArtistList = new ArrayList<>();

        searchResult = new SearchResult(emptySongList, emptyAlbumList, emptyArtistList);

        assertNotNull(searchResult.getSongResults(), "getSongResults should not return null");
        assertTrue(searchResult.getSongResults().isEmpty(), "getSongResults should return an empty list");

        assertNotNull(searchResult.getAlbumResults(), "getAlbumResults should not return null");
        assertTrue(searchResult.getAlbumResults().isEmpty(), "getAlbumResults should return an empty list");

        assertNotNull(searchResult.getArtistResults(), "getArtistResults should not return null");
        assertTrue(searchResult.getArtistResults().isEmpty(), "getArtistResults should return an empty list");
    }

    @Test
    void testConstructorWithNullLists() {
        searchResult = new SearchResult(null, null, null);

        assertNull(searchResult.getSongResults(), "getSongResults should return null");
        assertNull(searchResult.getAlbumResults(), "getAlbumResults should return null");
        assertNull(searchResult.getArtistResults(), "getArtistResults should return null");
    }

    @Test
    void testConstructorWithMixedNullAndNonNullLists() {
        List<Song> nonNullSongList = new ArrayList<>();

        searchResult = new SearchResult(nonNullSongList, null, mockArtistList);

        assertNotNull(searchResult.getSongResults(), "getSongResults should not return null");
        assertNull(searchResult.getAlbumResults(), "getAlbumResults should return null");
        assertNotNull(searchResult.getArtistResults(), "getArtistResults should not return null");

        assertSame(nonNullSongList, searchResult.getSongResults(), "getSongResults should return the songResults passed to constructor");
        assertSame(mockArtistList, searchResult.getArtistResults(), "getArtistResults should return the artistResults passed to constructor");
    }
}