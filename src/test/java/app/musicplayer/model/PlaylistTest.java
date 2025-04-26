package app.musicplayer.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import app.musicplayer.util.Resources;
import javafx.collections.ObservableList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlaylistTest {

    private Playlist playlist;
    private ArrayList<Song> songs;
    private Song mockSong;

    @Mock
    private DocumentBuilderFactory mockDocFactory;

    @Mock
    private DocumentBuilder mockDocBuilder;

    @Mock
    private Document mockDoc;

    @Mock
    private XPathFactory mockXPathFactory;

    @Mock
    private XPath mockXPath;

    @Mock
    private XPathExpression mockExpr;

    @Mock
    private NodeList mockNodeList;

    @Mock
    private Node mockNode;

    @Mock
    private Element mockElement;

    @Mock
    private TransformerFactory mockTransformerFactory;

    @Mock
    private Transformer mockTransformer;

    @BeforeEach
    public void setUp() {
        songs = new ArrayList<>();
        mockSong = new Song(1, "Test Song", "Test Artist", "Test Album",
                Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "test/location");
        songs.add(mockSong);

        playlist = new Playlist(1, "Test Playlist", songs);
    }

    @Test
    public void testConstructor() {
        assertEquals(1, playlist.getId());
        assertEquals("Test Playlist", playlist.getTitle());
        assertEquals(1, playlist.getSongs().size());
        assertEquals(mockSong, playlist.getSongs().get(0));
    }

    @Test
    public void testProtectedConstructor() {
        Playlist customPlaylist = new Playlist(2, "Custom Playlist", "Custom placeholder text");

        assertEquals(2, customPlaylist.getId());
        assertEquals("Custom Playlist", customPlaylist.getTitle());
        assertEquals("Custom placeholder text", customPlaylist.getPlaceholder());
    }

    @Test
    public void testGetters() {
        assertEquals(1, playlist.getId());
        assertEquals("Test Playlist", playlist.getTitle());

        String expectedPlaceholder = "Add songs to this playlist by dragging items to the sidebar\n" +
                "or by clicking the Add to Playlist button";
        assertEquals(expectedPlaceholder, playlist.getPlaceholder());
    }

    @Test
    public void testGetSongs() {
        ObservableList<Song> retrievedSongs = playlist.getSongs();
        assertEquals(1, retrievedSongs.size());
        assertEquals(mockSong, retrievedSongs.get(0));
    }

    @Test
    public void testToString() {
        assertEquals("Test Playlist", playlist.toString());
    }

    @Test
    public void testAddSongWhenSongNotInPlaylist() throws Exception {
        // Create a new song not in the playlist
        Song newSong = new Song(2, "New Song", "Test Artist", "Test Album",
                Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "test/location");

        // Use reflection to access and modify the JAR static field
        Field jarField = Resources.class.getDeclaredField("JAR");
        jarField.setAccessible(true);

        String originalValue = (String) jarField.get(null);

        try {
            jarField.set(null, "test/path/");

            // Mock the XML document
            try (MockedStatic<DocumentBuilderFactory> docFactoryMockedStatic = Mockito.mockStatic(DocumentBuilderFactory.class);
                 MockedStatic<XPathFactory> xPathFactoryMockedStatic = Mockito.mockStatic(XPathFactory.class);
                 MockedStatic<TransformerFactory> transformerFactoryMockedStatic = Mockito.mockStatic(TransformerFactory.class)) {

                docFactoryMockedStatic.when(DocumentBuilderFactory::newInstance).thenReturn(mockDocFactory);
                when(mockDocFactory.newDocumentBuilder()).thenReturn(mockDocBuilder);
                when(mockDocBuilder.parse("test/path/library.xml")).thenReturn(mockDoc);

                xPathFactoryMockedStatic.when(XPathFactory::newInstance).thenReturn(mockXPathFactory);
                when(mockXPathFactory.newXPath()).thenReturn(mockXPath);
                when(mockXPath.compile(anyString())).thenReturn(mockExpr);
                when(mockExpr.evaluate(mockDoc, XPathConstants.NODESET)).thenReturn(mockNodeList);
                when(mockNodeList.item(0)).thenReturn(mockNode);

                when(mockDoc.createElement("songId")).thenReturn(mockElement);

                transformerFactoryMockedStatic.when(TransformerFactory::newInstance).thenReturn(mockTransformerFactory);
                when(mockTransformerFactory.newTransformer()).thenReturn(mockTransformer);

                playlist.addSong(newSong);

                // Verify song was added to the internal list
                assertEquals(2, playlist.getSongs().size());
                assertTrue(playlist.getSongs().contains(newSong));

                // Verify the XML operations were performed
                verify(mockXPath).compile("/library/playlists/playlist[@id=\"1\"]");
                verify(mockDoc).createElement("songId");
                verify(mockElement).setTextContent("2");  // ID of the new song
                verify(mockNode).appendChild(mockElement);
                verify(mockTransformer).transform(any(DOMSource.class), any(StreamResult.class));
            }
        } finally {
            jarField.set(null, originalValue);
        }
    }


    @Test
    public void testAddSongWhenSongAlreadyInPlaylist() {
        // Try to add a song that's already in the playlist
        int initialSize = playlist.getSongs().size();
        playlist.addSong(mockSong);
        assertEquals(initialSize, playlist.getSongs().size());
    }

    @Test
    public void testAddSongWithException() throws Exception {
        // Create a new song that's not in the playlist
        Song newSong = new Song(2, "New Song", "Test Artist", "Test Album",
                Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "test/location");

        // Mock to throw an exception during XML processing
        java.lang.reflect.Field jarField = Resources.class.getDeclaredField("JAR");
        jarField.setAccessible(true);

        String originalValue = (String) jarField.get(null);

        try {
            jarField.set(null, "test/path/");

            try (MockedStatic<DocumentBuilderFactory> docFactoryMockedStatic = Mockito.mockStatic(DocumentBuilderFactory.class)) {
                docFactoryMockedStatic.when(DocumentBuilderFactory::newInstance).thenThrow(new RuntimeException("Test exception"));

                playlist.addSong(newSong);

                // The song should not be added to the songs list
                assertFalse(playlist.getSongs().contains(newSong));
            }
        } finally {
            jarField.set(null, originalValue);
        }
    }

    @Test
    public void testRemoveSongFound() {
        assertEquals(1, playlist.getSongs().size());
        // Remove song with ID 1
        playlist.removeSong(1);
        // Song should be removed
        assertEquals(0, playlist.getSongs().size());
    }

    @Test
    public void testRemoveSongNotFound() {
        assertEquals(1, playlist.getSongs().size());
        // Try to remove song with ID that doesn't exist
        playlist.removeSong(999);
        // No song should be removed
        assertEquals(1, playlist.getSongs().size());
    }
}