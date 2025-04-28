package app.musicplayer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Nested;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BLACK BOX TESTING APPROACH
 *
 * Since the application heavily depends on JavaFX which causes compilation issues,
 * I created test alternatives of core classes to verify functional behavior
 * without relying on implementation details.
 *
 * Key concepts:
 * 1. test from a user perspective, focusing on expected behavior
 * 2. Test objects mimic the behavior we expect from real  components
 * 3. File system operations use JUnit TempDir for isolation
 *
 * The following test classes mimic (keep most code and core logic same) the core objects in the application:
 * 1 TestSong: Models song metadata and playback behavior
 * 2 TestPlaylist: Manages collections of songs
 * 3 Special playlists: MostPlayed and RecentlyPlayed with sorting logic
 * 4 TestAlbum & TestArtist: Represent music organization elements
 * 5 TestSearchResult: Models search functionality
 */
public class MusicPlayerBlackBoxTest {

    @TempDir
    Path tempDir;

    //================= Utility Classes for Testing ==================

    /**
     * testing song functionality
     */
    class TestSong {
        private int id;
        private String title;
        private String artist;
        private String album;
        private long lengthInSeconds;
        private int trackNumber;
        private int discNumber;
        private int playCount;
        private LocalDateTime playDate;
        private String location;
        private boolean playing;
        private boolean selected;

        /**
         * Constructor for TestSong
         */
        public TestSong(int id, String title, String artist, String album,
            
            long lengthInSeconds, int trackNumber, int discNumber,
            int playCount, LocalDateTime playDate, String location) {
            
            this.id = id;
            this.title = (title == null) ? getFilenameFromPath(location) : title;
            this.artist = (artist == null) ? "Unknown Artist" : artist;
            this.album = (album == null) ? "Unknown Album" : album;
            this.lengthInSeconds = lengthInSeconds;
            this.trackNumber = trackNumber;
            this.discNumber = discNumber;
            this.playCount = playCount;
            this.playDate = playDate;
            this.location = location;
            this.playing = false;
            this.selected = false;
        }
        
        private String getFilenameFromPath(String path) {
            int lastSeparator = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
            String filename = path.substring(lastSeparator + 1);
            int dotIndex = filename.lastIndexOf('.');
            return (dotIndex > 0) ? filename.substring(0, dotIndex) : filename;
        }

        public void played() {
            this.playCount++;
            this.playDate = LocalDateTime.now();
        }

        public int compareTo(TestSong other) {
            int discComparison = Integer.compare(this.discNumber, other.discNumber);

            if (discComparison != 0) {
                return discComparison;
            } else {
                return Integer.compare(this.trackNumber, other.trackNumber);
            }
        }

        //Getters
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getAlbum() { return album; }
        public long getLengthInSeconds() { return lengthInSeconds; }
        public int getTrackNumber() { return trackNumber; }
        public int getDiscNumber() { return discNumber; }
        public int getPlayCount() { return playCount; }
        public LocalDateTime getPlayDate() { return playDate; }
        public String getLocation() { return location; }
        public boolean getPlaying() { return playing; }
        public boolean getSelected() { return selected; }

        //Setters
        public void setPlaying(boolean playing) { this.playing = playing; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }

    /**
     * playlists test
     */
    class TestPlaylist {
        private int id;
        private String title;
        private ArrayList<TestSong> songs;
        private String placeholder;

        public TestPlaylist(int id, String title, ArrayList<TestSong> songs) {
            this.id = id;
            this.title = title;
            this.songs = songs != null ? songs : new ArrayList<>();
            this.placeholder = "Add songs to this playlist by dragging items to the sidebar\n" +
                "or by clicking the Add to Playlist button";
        }

        public TestPlaylist(int id, String title, String placeholder) {
            this.id = id;
            this.title = title;
            this.songs = null;
            this.placeholder = placeholder;
        }
        
        public int getId() { return id; }
        public String getTitle() { return title; }
        public ArrayList<TestSong> getSongs() { return songs; }
        public String getPlaceholder() { return placeholder; }

        //Add song, preventing duplicates
        public void addSong(TestSong song) {
            if (songs != null && !songs.contains(song)) {
                songs.add(song);
            }
        }

        //Remove song
        public void removeSong(int songId) {
            if (songs != null) {
                songs.removeIf(song -> song.getId() == songId);
            }
        }
    }

    /**
     * Most Played Playlist test class
     */
    class MostPlayedPlaylist extends TestPlaylist {
        public MostPlayedPlaylist(int id) {
            super(id, "Most Played", "You have not played any songs yet");
        }

        @Override
        public ArrayList<TestSong> getSongs() {
            ArrayList<TestSong> allSongs = getAllSongs();

            
            List<TestSong> filteredSongs = allSongs.stream()
                .filter(song -> song.getPlayCount() > 0)
                .sorted((x, y) -> Integer.compare(y.getPlayCount(), x.getPlayCount()))
                .collect(Collectors.toList());

            
            if (filteredSongs.size() > 100) {
                filteredSongs = filteredSongs.subList(0, 100);
            }

            return new ArrayList<>(filteredSongs);
        }
    }

    /**
     * Recently Played Playlist calss
     */
    class RecentlyPlayedPlaylist extends TestPlaylist {
        public RecentlyPlayedPlaylist(int id) {
            super(id, "Recently Played", "You have not played any songs yet");
        }

        @Override
        public ArrayList<TestSong> getSongs() {
            ArrayList<TestSong> allSongs = getAllSongs();

            List<TestSong> filteredSongs = allSongs.stream()
                .filter(song -> song.getPlayCount() > 0)
                .sorted((x, y) -> y.getPlayDate().compareTo(x.getPlayDate()))
                .collect(Collectors.toList());
            
            if (filteredSongs.size() > 100) {
                filteredSongs = filteredSongs.subList(0, 100);
            }

            return new ArrayList<>(filteredSongs);
        }
    }

    /**
     * Albums test class
     */
    class TestAlbum {
        private int id;
        private String title;
        private String artist;

        public TestAlbum(int id, String title, String artist) {
            this.id = id;
            this.title = title;
            this.artist = artist;
        }

        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
    }

    /**
     * Artists test class
     */
    class TestArtist {
        private String title;

        public TestArtist(String title) {
            this.title = title;
        }

        public String getTitle() { return title; }
    }

    /**
     * Search Results test class
     */
    class TestSearchResult {
        private List<TestSong> songResults;
        private List<TestAlbum> albumResults;
        private List<TestArtist> artistResults;

        public TestSearchResult(List<TestSong> songResults, List<TestAlbum> albumResults,
            List<TestArtist> artistResults) {
            this.songResults = songResults;
            this.albumResults = albumResults;
            this.artistResults = artistResults;
        }

        public List<TestSong> getSongResults() { return songResults; }
        public List<TestAlbum> getAlbumResults() { return albumResults; }
        public List<TestArtist> getArtistResults() { return artistResults; }
    }
    
    //==================== Test data ====================
    
    //List of test songs
    private ArrayList<TestSong> testSongs;

    //List of test albums
    private List<TestAlbum> allAlbums;

    //List of test artists
    private List<TestArtist> allArtists;

    /**
     * Get all songs for testing
     */
    private ArrayList<TestSong> getAllSongs() {
        return new ArrayList<>(testSongs);
    }

    /**
     * Init test data
     */
    @BeforeEach
    void setUp() throws IOException {
        //Create test songs
        testSongs = new ArrayList<>();

        //Create test music files
        for (int i = 1; i <= 5; i++) {
            Path songFile = tempDir.resolve("song" + i + ".mp3");
            Files.write(songFile, ("MP3 audio data " + i).getBytes());

            //Different play counts and dates
            int playCount = (i % 3) * 2; //0, 2, 4, 0, 2
            LocalDateTime playDate = LocalDateTime.now().minusHours(i);

            // song object structure
            TestSong song = new TestSong(
                i,                         //id
                "Song " + i,               //title
                "Artist " + ((i % 2) + 1), //artist
                "Album " + ((i % 3) + 1),  //album
                180,                       //lengthInSeconds
                i,                         //trackNumber
                (i % 2) + 1,               //discNumber
                playCount,                 //playCount
                playDate,                  //playDate
                songFile.toString()        //location
            );

            testSongs.add(song);
        }

        //Create test albums
        allAlbums = new ArrayList<>();
        allAlbums.add(new TestAlbum(1, "Test Album", "Test Artist"));
        allAlbums.add(new TestAlbum(2, "Another Album", "Another Artist"));
        allAlbums.add(new TestAlbum(3, "Pop Album", "Pop Artist"));
        allAlbums.add(new TestAlbum(4, "Rock Album", "Rock Artist"));

        //Create test artists
        allArtists = new ArrayList<>();
        allArtists.add(new TestArtist("Test Artist"));
        allArtists.add(new TestArtist("Another Artist"));
        allArtists.add(new TestArtist("Pop Artist"));
        allArtists.add(new TestArtist("Rock Artist"));
    }

    // ============= Utility Methods =================

    /**
     * Check if file type is supported
     */
    private boolean isSupportedFileType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i+1).toLowerCase();
        }
        switch (extension) {
            case "mp3": //for audio
            case "mp4":
            case "m4a": //for audio
            case "m4v":
            case "wav": //for audio
                return true;
            default:
                return false;
        }
    }

    /**
     * Count music files in directory
     */
    private int countMusicFiles(File directory) throws IOException {
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IOException("Invalid directory: " + directory.getPath());
        }

        int count = 0;
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isSupportedFileType(file.getName())) {
                    count++;
                } else if (file.isDirectory()) {
                    count += countMusicFiles(file);
                }
            }
        }

        return count;
    }

    /**
     * Find music files in directory
     */
    private List<String> findMusicFiles(File directory) throws IOException {
        List<String> musicFiles = new ArrayList<>();
        processDirectory(directory, musicFiles);
        return musicFiles;
    }

    /**
     * Process directory
     */
    private void processDirectory(File directory) throws IOException {
        processDirectory(directory, null);
    }

    /**
     * Process directory and collect file paths
     */
    private void processDirectory(File directory, List<String> musicFiles) throws IOException {
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IOException("Invalid directory: " + directory.getPath());
        }

        File[] files = directory.listFiles();

        //Check if files are not null
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isSupportedFileType(file.getName())) {
                    if (musicFiles != null) {
                        musicFiles.add(file.getAbsolutePath());
                    }
                } else if (file.isDirectory()) {
                    processDirectory(file, musicFiles);
                }
            }
        }
    }

    /**
     * Create test file
     */
    private void createTestFile(Path path, String content) throws IOException {

        Files.write(path, content.getBytes());
    }

    /**
     * search helper
     */
    private TestSearchResult search(String searchText) {
        String text = searchText.toUpperCase();

        //Search songs
        List<TestSong> songResults = testSongs.stream()
            .filter(song -> song.getTitle().toUpperCase().contains(text))
            .sorted((x, y) -> {
                boolean xMatch = x.getTitle().toUpperCase().equals(text);
                boolean yMatch = y.getTitle().toUpperCase().equals(text);
                if (xMatch && yMatch) return 0;
                if (xMatch) return -1;
                if (yMatch) return 1;

                boolean xStartWith = x.getTitle().toUpperCase().startsWith(text);
                boolean yStartWith = y.getTitle().toUpperCase().startsWith(text);
                if (xStartWith && yStartWith) return 0;
                if (xStartWith) return -1;
                if (yStartWith) return 1;

                boolean xContains = x.getTitle().toUpperCase().contains(" " + text);
                boolean yContains = y.getTitle().toUpperCase().contains(" " + text);
                if (xContains && yContains) return 0;
                if (xContains) return -1;
                if (yContains) return 1;
                return 0;
            })
            .collect(Collectors.toList());

        //Search albums
        List<TestAlbum> albumResults = allAlbums.stream()
            .filter(album -> album.getTitle().toUpperCase().contains(text))
            .sorted((x, y) -> {
                boolean xEqual = x.getTitle().toUpperCase().equals(text);
                boolean yEqual = y.getTitle().toUpperCase().equals(text);

                if (xEqual && yEqual) return 0;
                if (xEqual) return -1;
                if (yEqual) return 1;

                boolean xStartWith = x.getTitle().toUpperCase().startsWith(text);
                boolean yStartWith = y.getTitle().toUpperCase().startsWith(text);

                if (xStartWith && yStartWith) return 0;
                if (xStartWith) return -1;
                if (yStartWith) return 1;

                boolean xContains = x.getTitle().toUpperCase().contains(" " + text);
                boolean yContains = y.getTitle().toUpperCase().contains(" " + text);

                if (xContains && yContains) return 0;
                if (xContains) return -1;
                if (yContains) return 1;
                return 0;
            })
            .collect(Collectors.toList());

        //Search artists
        List<TestArtist> artistResults = allArtists.stream()
            .filter(artist -> artist.getTitle().toUpperCase().contains(text))
            .sorted((x, y) -> {
                boolean xMatch = x.getTitle().toUpperCase().equals(text);
                boolean yMatch = y.getTitle().toUpperCase().equals(text);
                if (xMatch && yMatch) return 0;
                if (xMatch) return -1;
                if (yMatch) return 1;

                boolean xStartWith = x.getTitle().toUpperCase().startsWith(text);
                boolean yStartWith = y.getTitle().toUpperCase().startsWith(text);
                if (xStartWith && yStartWith) return 0;
                if (xStartWith) return -1;
                if (yStartWith) return 1;

                boolean xContains = x.getTitle().toUpperCase().contains(" " + text);
                boolean yContains = y.getTitle().toUpperCase().contains(" " + text);
                if (xContains && yContains) return 0;
                if (xContains) return -1;
                if (yContains) return 1;
                return 0;
            })
            .collect(Collectors.toList());

        //Limit results to top 3 (if more are found)
        if (songResults.size() > 3) songResults = songResults.subList(0, 3);
        if (albumResults.size() > 3) albumResults = albumResults.subList(0, 3);
        if (artistResults.size() > 3) artistResults = artistResults.subList(0, 3);

        return new TestSearchResult(songResults, albumResults, artistResults);
    }


    // =================== End of Created Alternative Classes and Test Data setup ====================

    // =================== Beginning of Test Cases ====================
    // Five Main Testing Areas:
    //  Audio file handling
    //  Song metadata management
    //  Playlist functionality
    //  Search capabilities
    //  Music library import

    //  ============ Tests for Audio File Handling =================

    @Nested
    class AudioFileTests {

        @Test
        void testSupportedFileTypes() {
            //Test supported file types
            assertTrue(isSupportedFileType("test.mp3"));
            assertTrue(isSupportedFileType("test.MP3"));
            assertTrue(isSupportedFileType("test.wav"));
            assertTrue(isSupportedFileType("test.WAV"));
            assertTrue(isSupportedFileType("test.m4a"));
            assertTrue(isSupportedFileType("test.m4v"));

            //Test incorrect file types
            assertFalse(isSupportedFileType("test.txt"));
            assertFalse(isSupportedFileType("test.jpg"));
            assertFalse(isSupportedFileType("noextension"));
        }

        @Test
        void testFileWithoutExtension() {
            assertFalse(isSupportedFileType("onlyname"));
        }

        @Test
        void testFileWithSpecialCharacters() {
            assertTrue(isSupportedFileType("test-song.mp3"));
            assertTrue(isSupportedFileType("test_song.mp3"));
            assertTrue(isSupportedFileType("test song.mp3"));
            assertTrue(isSupportedFileType("test%20song.mp3"));
            assertTrue(isSupportedFileType("test#song.mp3"));
        }

        @Test
        void testActualFilesInDirectory() throws IOException {
            //Create a directory with different file types
            Path musicDir = tempDir.resolve("audioTest");
            Files.createDirectories(musicDir);

            createTestFile(musicDir.resolve("test.mp3"), "MP3 audio data");
            createTestFile(musicDir.resolve("test.wav"), "WAV audio data");
            createTestFile(musicDir.resolve("test.m4a"), "M4A audio data");
            createTestFile(musicDir.resolve("test.m4v"), "M4V video data");
            createTestFile(musicDir.resolve("test.txt"), "Text data");
            createTestFile(musicDir.resolve("test.jpg"), "JPEG image data");
            createTestFile(musicDir.resolve("noextension"), "No extension data");

            //Count supported audio files
            int count = countMusicFiles(musicDir.toFile());

            //created 4 supported format files
            assertEquals(4, count);

            //Find music files
            List<String> musicFiles = findMusicFiles(musicDir.toFile());

            assertEquals(4, musicFiles.size());
            assertTrue(musicFiles.stream().anyMatch(path -> path.endsWith("test.mp3")));
            assertTrue(musicFiles.stream().anyMatch(path -> path.endsWith("test.wav")));
            assertTrue(musicFiles.stream().anyMatch(path -> path.endsWith("test.m4a")));
            assertTrue(musicFiles.stream().anyMatch(path -> path.endsWith("test.m4v")));

            //Verify unsupported files are not included
            assertFalse(musicFiles.stream().anyMatch(path -> path.endsWith("test.txt")));
            assertFalse(musicFiles.stream().anyMatch(path -> path.endsWith("test.jpg")));
            assertFalse(musicFiles.stream().anyMatch(path -> path.endsWith("noextension")));
        }
    }

    // ============= Tests for Song Metadata Handling =====================

    @Nested
    class SongMetadataTests {

        @Test
        void testSongMetadataHandling() throws IOException {
            //test file
            Path songFile = tempDir.resolve("test-song.mp3");
            Files.write(songFile, "MP3 audio data".getBytes());

            //Create test song metadata
            TestSong song = new TestSong(
                1,                          //id
                "Test Song",                //title
                "Test Artist",              //artist
                "Test Album",               //album
                180,                        //lengthInSeconds
                1,                          //trackNumber
                1,                          //discNumber
                0,                          //playCount
                LocalDateTime.now(),        //playDate
                songFile.toString()         //location
            );

            //basic properties
            assertEquals(1, song.getId());
            assertEquals("Test Song", song.getTitle());
            assertEquals("Test Artist", song.getArtist());
            assertEquals("Test Album", song.getAlbum());
            assertEquals(180, song.getLengthInSeconds());
            assertEquals(1, song.getTrackNumber());
            assertEquals(1, song.getDiscNumber());
            assertEquals(0, song.getPlayCount());
            assertNotNull(song.getPlayDate());
            assertEquals(songFile.toString(), song.getLocation());
            assertFalse(song.getPlaying());
            assertFalse(song.getSelected());

            //Test play count increment
            int initialPlayCount = song.getPlayCount();
            LocalDateTime initialPlayDate = song.getPlayDate();

            //Wait to ensure different timestamp
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            song.played();

            assertEquals(initialPlayCount + 1, song.getPlayCount());
            assertTrue(song.getPlayDate().isAfter(initialPlayDate));

            //Test null title handling
            TestSong nullTitleSong = new TestSong(
                2,                          //id
                null,                       //title
                "Test Artist",              //artist
                "Test Album",               //album
                180,                        //lengthInSeconds
                2,                          //trackNumber
                1,                          //discNumber
                0,                          //playCount
                LocalDateTime.now(),        //playDate
                songFile.toString()         //location
            );

            //Verify filename is used as title
            assertTrue(nullTitleSong.getTitle().contains("test-song"));

            //Test null artist handling
            TestSong nullArtistSong = new TestSong(
                3,                          //id
                "Test Song",                //title
                null,                       //artist
                "Test Album",               //album
                180,                        //lengthInSeconds
                3,                          //trackNumber
                1,                          //discNumber
                0,                          //playCount
                LocalDateTime.now(),        //playDate
                songFile.toString()         //location
            );

            assertEquals("Unknown Artist", nullArtistSong.getArtist());

            //Test null album handling
            TestSong nullAlbumSong = new TestSong(
                4,                          //id
                "Test Song",                //title
                "Test Artist",              //artist
                null,                       //album
                180,                        //lengthInSeconds
                4,                          //trackNumber
                1,                          //discNumber
                0,                          //playCount
                LocalDateTime.now(),        //playDate
                songFile.toString()         //location
            );

            assertEquals("Unknown Album", nullAlbumSong.getAlbum());
        }


        @Test
        void testSongComparison() {
            //Create test songs with different disc/track numbers
            TestSong song1 = new TestSong(1, "Song 1", "Artist", "Album", 180, 1, 1, 0,
                LocalDateTime.now(), "song1.mp3");
            TestSong song2 = new TestSong(2, "Song 2", "Artist", "Album", 180, 2, 1, 0,
                LocalDateTime.now(), "song2.mp3");
            TestSong song3 = new TestSong(3, "Song 3", "Artist", "Album", 180, 1, 2, 0,
                LocalDateTime.now(), "song3.mp3");

            //Test comparison
            assertTrue(song1.compareTo(song2) < 0); //Same disc, track1 < track2
            assertTrue(song1.compareTo(song3) < 0); //Disc1 < Disc2
            assertTrue(song3.compareTo(song2) > 0); //Disc2 > Disc1, regardless of track

            //Test equality
            TestSong song1Duplicate = new TestSong(4, "Song 4", "Artist", "Album", 180, 1, 1, 0,
                LocalDateTime.now(), "song4.mp3");
            assertEquals(0, song1.compareTo(song1Duplicate)); //Same disc and track
        }
    }

    // ====================== Tests for Playlist Management ===================

    @Nested
    class PlaylistManagementTests {

        @Test
        void testRegularPlaylist() {
            //Create regular playlist
            ArrayList<TestSong> playlistSongs = new ArrayList<>();
            TestPlaylist playlist = new TestPlaylist(1, "Test Playlist", playlistSongs);

            //Verify basic properties
            assertEquals(1, playlist.getId());
            assertEquals("Test Playlist", playlist.getTitle());
            assertTrue(playlist.getSongs().isEmpty());

            //Add song
            playlist.addSong(testSongs.get(0));
            assertEquals(1, playlist.getSongs().size());
            assertEquals(testSongs.get(0), playlist.getSongs().get(0));

            //Try adding same song again should not dupli
            playlist.addSong(testSongs.get(0));
            assertEquals(1, playlist.getSongs().size());

            //Add different song
            playlist.addSong(testSongs.get(1));
            assertEquals(2, playlist.getSongs().size());

            //Remove song
            playlist.removeSong(testSongs.get(0).getId());
            assertEquals(1, playlist.getSongs().size());
            assertEquals(testSongs.get(1), playlist.getSongs().get(0));

            //Try removing non-exi song ID
            playlist.removeSong(999);
            assertEquals(1, playlist.getSongs().size());
        }

        @Test
        void testMostPlayedPlaylist() {

            //create most played playlist

            MostPlayedPlaylist mostPlayedPlaylist = new MostPlayedPlaylist(-1);

            //verify basic properties
            assertEquals("Most Played", mostPlayedPlaylist.getTitle());
            assertEquals("You have not played any songs yet", mostPlayedPlaylist.getPlaceholder());

            //should be sorted by play count
            ArrayList<TestSong> songs = mostPlayedPlaylist.getSongs();

            //modify here:3 -》4
            assertEquals(4, songs.size()); //有4首歌播放过

            //should by play count descending
            for (int i = 0; i < songs.size() - 1; i++) {
                assertTrue(songs.get(i).getPlayCount() >= songs.get(i + 1).getPlayCount());
            }

            //verify play count
            assertEquals(4, songs.get(0).getPlayCount());
        }

        @Test
        void testRecentlyPlayedPlaylist() {

            RecentlyPlayedPlaylist recentlyPlayedPlaylist = new RecentlyPlayedPlaylist(-2);


            assertEquals("Recently Played", recentlyPlayedPlaylist.getTitle());
            assertEquals("You have not played any songs yet", recentlyPlayedPlaylist.getPlaceholder());

            //should be sorted by play date
            ArrayList<TestSong> songs = recentlyPlayedPlaylist.getSongs();

            //MODIFY HERE: 3 -》4
            assertEquals(4, songs.size()); //有4首歌播放过

            //should be sorted by play date descending
            for (int i = 0; i < songs.size() - 1; i++) {
                assertTrue(songs.get(i).getPlayDate().isAfter(songs.get(i + 1).getPlayDate()) ||
                    songs.get(i).getPlayDate().isEqual(songs.get(i + 1).getPlayDate()));
            }
        }
    }

    // ==============  Tests for Search Functionality ===================

    @Nested
    class SearchFunctionalityTests {

        @Test
        void testSimpleSearch() {
            //Add a specific test song for search
            TestSong testSong = new TestSong(
                10,                    //id
                "Test Song",           //title
                "Test Artist",         //artist
                "Test Album",          //album
                180,                   //lengthInSeconds
                1,                     //trackNumber
                1,                     //discNumber
                0,                     //playCount
                LocalDateTime.now(),   //playDate
                "testsong.mp3"         //location
            );
            testSongs.add(testSong);

            //Search for "Test"
            TestSearchResult result = search("Test");

            //Verify song results
            assertTrue(result.getSongResults().size() >= 1);
            assertTrue(result.getSongResults().stream()
                .anyMatch(song -> song.getTitle().equals("Test Song")));

            //Verify album results
            assertEquals(1, result.getAlbumResults().size());
            assertEquals("Test Album", result.getAlbumResults().get(0).getTitle());

            //Verify artist results
            assertEquals(1, result.getArtistResults().size());
            assertEquals("Test Artist", result.getArtistResults().get(0).getTitle());
        }

        //edge case: search for empty string
        @Test
        void testEmptySearchResults() {
            //Search for non-existent term
            TestSearchResult result = search("NonExistent");

            //Verify empty results
            assertTrue(result.getSongResults().isEmpty());
            assertTrue(result.getAlbumResults().isEmpty());
            assertTrue(result.getArtistResults().isEmpty());
        }

        //Test case: insensitive search
        @Test
        void testCaseInsensitiveSearch() {
            //Add a test song
            TestSong testSong = new TestSong(
                10,                    //id
                "Test Song",           //title
                "Test Artist",         //artist
                "Test Album",          //album
                180,                   //lengthInSeconds
                1,                     //trackNumber
                1,                     //discNumber
                0,                     //playCount
                LocalDateTime.now(),   //playDate
                "testsong.mp3"         //location
            );
            testSongs.add(testSong);

            //Search with different casings
            TestSearchResult result1 = search("test");
            TestSearchResult result2 = search("TEST");
            TestSearchResult result3 = search("TeSt");

            //Verify all searches return same count of results
            assertEquals(result1.getSongResults().size(), result2.getSongResults().size());
            assertEquals(result1.getSongResults().size(), result3.getSongResults().size());
            assertEquals(result1.getAlbumResults().size(), result2.getAlbumResults().size());
            assertEquals(result1.getAlbumResults().size(), result3.getAlbumResults().size());
            assertEquals(result1.getArtistResults().size(), result2.getArtistResults().size());
            assertEquals(result1.getArtistResults().size(), result3.getArtistResults().size());
        }

        @Test
        void testSearchLimit() {
            //Add many test songs with "Search" in title
            for (int i = 0; i < 5; i++) {
                TestSong song = new TestSong(
                    20 + i,              //id
                    "Search Test " + i,  //title
                    "Search Artist",     //artist
                    "Search Album",      //album
                    180,                 //lengthInSeconds
                    i + 1,               //trackNumber
                    1,                   //discNumber
                    0,                   //playCount
                    LocalDateTime.now(), //playDate
                    "search" + i + ".mp3" //location
                );
                testSongs.add(song);
            }

            //Add test album
            allAlbums.add(new TestAlbum(10, "Search Album", "Search Artist"));

            //Add test artist
            allArtists.add(new TestArtist("Search Artist"));

            //Search for "Search"
            TestSearchResult result = search("Search");

            //Verify song results limited to 3
            assertEquals(3, result.getSongResults().size());

            //Verify album results
            assertEquals(1, result.getAlbumResults().size());
            assertEquals("Search Album", result.getAlbumResults().get(0).getTitle());

            //Verify artist results
            assertEquals(1, result.getArtistResults().size());
            assertEquals("Search Artist", result.getArtistResults().get(0).getTitle());
        }

        @Test
        void testSearchSorting() {
            //Add test songs with various titles
            testSongs.add(new TestSong(101, "Exact", "Artist", "Album", 180, 1, 1, 0,
                LocalDateTime.now(), "exact.mp3"));
            testSongs.add(new TestSong(102, "Exact Match", "Artist", "Album", 180, 2, 1, 0,
                LocalDateTime.now(), "exactmatch.mp3"));
            testSongs.add(new TestSong(103, "ExactAtStart Match", "Artist", "Album", 180, 3, 1, 0,
                LocalDateTime.now(), "exactatstart.mp3"));
            testSongs.add(new TestSong(104, "Contains Exact Word", "Artist", "Album", 180, 4, 1, 0,
                LocalDateTime.now(), "containsexact.mp3"));
            testSongs.add(new TestSong(105, "HasExactInside", "Artist", "Album", 180, 5, 1, 0,
                LocalDateTime.now(), "hasexactinside.mp3"));

            //Search for "Exact"
            TestSearchResult result = search("Exact");

            //Verify results are limited to 3
            assertEquals(3, result.getSongResults().size());

            //Verify sort order: exact match > starts with > contains word > contains anywhere
            List<TestSong> songs = result.getSongResults();

            //First result should be exact match
            assertEquals("Exact", songs.get(0).getTitle());

            //Other results should follow priority as well
            if (songs.size() > 1) {
                assertTrue(songs.get(1).getTitle().startsWith("Exact") ||
                    songs.get(1).getTitle().contains(" Exact "));
            }
        }
    }

    // ===========Tests for Music Import Functionality =================

    @Nested
    class MusicImportTests {

        //edge case: empty directory
        @Test
        void testImportEmptyDirectory() throws IOException {
            //Test importing empty directory
            File emptyDir = tempDir.resolve("empty").toFile();
            Files.createDirectories(tempDir.resolve("empty"));

            //Ensure directory is empty
            assertEquals(0, emptyDir.listFiles().length);

            //Import music
            int fileCount = countMusicFiles(emptyDir);

            //Verify results
            assertEquals(0, fileCount);
        }

        //error guessing
        @Test
        void testImportMixedFileTypes() throws IOException {
            //Create test dicct structure
            Path musicDir = tempDir.resolve("mixed");
            Files.createDirectories(musicDir);

            //Create various file types
            Files.write(musicDir.resolve("song1.mp3"), "MP3 data".getBytes());
            Files.write(musicDir.resolve("song2.wav"), "WAV data".getBytes());
            Files.write(musicDir.resolve("song3.m4a"), "M4A data".getBytes());
            Files.write(musicDir.resolve("document.txt"), "Text data".getBytes());
            Files.write(musicDir.resolve("image.jpg"), "JPEG data".getBytes());

            //Create subdirectory with more files
            Path subDir = musicDir.resolve("subfolder");
            Files.createDirectories(subDir);
            Files.write(subDir.resolve("song4.mp3"), "More MP3 data".getBytes());
            Files.write(subDir.resolve("song5.wav"), "More WAV data".getBytes());

            //Import music
            int fileCount = countMusicFiles(musicDir.toFile());

            //Verify results - should only count supported audio files
            assertEquals(5, fileCount);

            //Verify recursive find
            List<String> musicFiles = findMusicFiles(musicDir.toFile());

            assertEquals(5, musicFiles.size());
            assertTrue(musicFiles.stream().anyMatch(path -> path.endsWith("song1.mp3")));
            assertTrue(musicFiles.stream().anyMatch(path -> path.endsWith("song2.wav")));
            assertTrue(musicFiles.stream().anyMatch(path -> path.endsWith("song3.m4a")));
            assertTrue(musicFiles.stream().anyMatch(path -> path.endsWith("song4.mp3")));
            assertTrue(musicFiles.stream().anyMatch(path -> path.endsWith("song5.wav")));

            //Verify unsupported file types are not included
            assertFalse(musicFiles.stream().anyMatch(path -> path.endsWith("document.txt")));
            assertFalse(musicFiles.stream().anyMatch(path -> path.endsWith("image.jpg")));
        }

        //error guessing
        @Test
        void testImportNonExistentDirectory() {
            //Test trying to import non-existent directory
            File nonExistentDir = new File(tempDir.toFile(), "nonexistent");

            //Ensure directory doesn't exist
            assertFalse(nonExistentDir.exists());

            //Verify exception handling
            Exception exception = assertThrows(IOException.class, () -> {
                processDirectory(nonExistentDir);
            });

            //Verify exception message contains directory info
            assertTrue(exception.getMessage().contains(nonExistentDir.getPath()));
        }

        @Test
        void testImportWithSpecialCharacters() throws IOException {


            //Create directory with special characters
            Path specialDir = tempDir.resolve("special characters");
            Files.createDirectories(specialDir);

            //Create files with special character names
            Files.write(specialDir.resolve("song with spaces.mp3"), "MP3 data".getBytes());
            Files.write(specialDir.resolve("song-with-dashes.mp3"), "MP3 data".getBytes());
            Files.write(specialDir.resolve("song_with_underscores.mp3"), "MP3 data".getBytes());
            Files.write(specialDir.resolve("song#with#hash.mp3"), "MP3 data".getBytes());
            Files.write(specialDir.resolve("song(with)parentheses.mp3"), "MP3 data".getBytes());

            //Import music
            int fileCount = countMusicFiles(specialDir.toFile());

            //Verify results
            assertEquals(5, fileCount);

            //Verify special character filenames
            List<String> musicFiles = findMusicFiles(specialDir.toFile());

            assertEquals(5, musicFiles.size());
            assertTrue(musicFiles.stream().anyMatch(path -> path.contains("song with spaces.mp3")));
            assertTrue(musicFiles.stream().anyMatch(path -> path.contains("song-with-dashes.mp3")));
            assertTrue(musicFiles.stream().anyMatch(path -> path.contains("song_with_underscores.mp3")));
            assertTrue(musicFiles.stream().anyMatch(path -> path.contains("song#with#hash.mp3")));
            assertTrue(musicFiles.stream().anyMatch(path -> path.contains("song(with)parentheses.mp3")));
        }
    }
}
