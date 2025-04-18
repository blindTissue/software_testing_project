package util;

import app.musicplayer.model.Song;
import app.musicplayer.util.SubView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SubViewTest {

    // Mock implementation of SubView for testing
    private static class MockSubView implements SubView {
        private char lastScrollChar;
        private boolean playWasCalled;
        private Song mockSong;

        public MockSubView(Song song) {
            this.mockSong = song;
        }

        @Override
        public void scroll(char letter) {
            this.lastScrollChar = letter;
        }

        @Override
        public void play() {
            this.playWasCalled = true;
        }

        @Override
        public Song getSelectedSong() {
            return mockSong;
        }

        public char getLastScrollChar() {
            return lastScrollChar;
        }

        public boolean wasPlayCalled() {
            return playWasCalled;
        }
    }

    private MockSubView mockSubView;
    private Song mockSong;

    @BeforeEach
    void setUp() {
        // Create a song with the correct constructor parameters
        // Parameters: id, title, artist, album, duration, trackNumber, discNumber, year, dateAdded, genre
        mockSong = new Song(1, "Test Song", "Test Artist", "Test Album", 
                            Duration.ofSeconds(180), 1, 1, 2020, 
                            LocalDateTime.now(), "Test Genre");
        mockSubView = new MockSubView(mockSong);
    }

    @Test
    void testScroll() {
        mockSubView.scroll('A');
        assertEquals('A', mockSubView.getLastScrollChar(), 
                    "The scroll method should update the last scroll character");
    }

    @Test
    void testPlay() {
        assertFalse(mockSubView.wasPlayCalled(), "Play should not be called initially");
        mockSubView.play();
        assertTrue(mockSubView.wasPlayCalled(), "Play should be called after invoking the method");
    }

    @Test
    void testGetSelectedSong() {
        Song result = mockSubView.getSelectedSong();
        assertSame(mockSong, result, "GetSelectedSong should return the mock song");
        assertEquals("Test Song", result.getTitle(), "The song title should match");
    }
} 