package util;

import app.musicplayer.model.Song;
import app.musicplayer.util.Resources;
import app.musicplayer.util.XMLEditor;
import app.musicplayer.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XMLEditorTest {

    @TempDir
    Path tempDir;
    
    private Path musicDirPath;
    private Path xmlPath;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create a temporary music directory
        musicDirPath = tempDir.resolve("music");
        Files.createDirectories(musicDirPath);
        
        // Set the JAR path to tempDir for resources
        Field jarField = Resources.class.getDeclaredField("JAR");
        jarField.setAccessible(true);
        jarField.set(null, tempDir.toString() + File.separator);
        
        // Create a simple library.xml file in the temp directory
        xmlPath = tempDir.resolve("library.xml");
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<library>\n" +
                "  <songs lastId=\"2\">\n" +
                "    <song id=\"1\">\n" +
                "      <title>Existing Song</title>\n" +
                "      <artist>Existing Artist</artist>\n" +
                "      <album>Existing Album</album>\n" +
                "      <length>180</length>\n" +
                "      <discNumber>1</discNumber>\n" +
                "      <trackNumber>1</trackNumber>\n" +
                "      <year>2020</year>\n" +
                "      <genre>Rock</genre>\n" +
                "      <location>" + musicDirPath + File.separator + "existing_song.mp3</location>\n" +
                "      <playCount>0</playCount>\n" +
                "    </song>\n" +
                "  </songs>\n" +
                "  <playlists lastId=\"0\"/>\n" +
                "</library>";
                
        try (FileWriter writer = new FileWriter(xmlPath.toFile())) {
            writer.write(xmlContent);
        }
        
        // Create the existing song file
        File existingSongFile = new File(musicDirPath.toFile(), "existing_song.mp3");
        existingSongFile.createNewFile();
        
        // Set the music directory in XMLEditor
        XMLEditor.setMusicDirectory(musicDirPath);
    }
    
    @Test
    void testSetMusicDirectory() {
        Path newPath = tempDir.resolve("newMusic");
        XMLEditor.setMusicDirectory(newPath);
        
        try {
            // Use reflection to check if the music directory was set
            Field musicDirField = XMLEditor.class.getDeclaredField("musicDirectory");
            musicDirField.setAccessible(true);
            String musicDir = (String) musicDirField.get(null);
            
            assertEquals(newPath.toString(), musicDir, "Music directory should be updated");
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }
    
    @Test
    void testGetNewSongs() {
        // Initially should be empty
        List<Song> newSongs = XMLEditor.getNewSongs();
        assertNotNull(newSongs, "New songs list should not be null");
        assertTrue(newSongs.isEmpty(), "New songs list should be empty initially");
    }
    
    // Note: Testing the actual XML processing functionality is complex because
    // it involves file system operations and XML parsing. These tests would typically
    // require mocking the file system operations or using specialized libraries.
    // The following test demonstrates how to test a private method using reflection
    
    @Test
    void testMusicDirFileFinderPrivateMethod() throws Exception {
        // Create test MP3 files in music directory
        File testSongFile1 = new File(musicDirPath.toFile(), "test_song1.mp3");
        testSongFile1.createNewFile();
        File testSongFile2 = new File(musicDirPath.toFile(), "test_song2.mp3");
        testSongFile2.createNewFile();
        
        // Create a subfolder
        File subDir = new File(musicDirPath.toFile(), "subdir");
        subDir.mkdir();
        File testSongFile3 = new File(subDir, "test_song3.mp3");
        testSongFile3.createNewFile();
        
        // Use reflection to access and call the private method
        Method musicDirFileFinderMethod = XMLEditor.class.getDeclaredMethod("musicDirFileFinder", File.class);
        musicDirFileFinderMethod.setAccessible(true);
        
        // Reset the arrays first (they're static)
        Field musicDirFilesField = XMLEditor.class.getDeclaredField("musicDirFiles");
        musicDirFilesField.setAccessible(true);
        musicDirFilesField.set(null, new ArrayList<File>());
        
        Field musicDirFileNamesField = XMLEditor.class.getDeclaredField("musicDirFileNames");
        musicDirFileNamesField.setAccessible(true);
        musicDirFileNamesField.set(null, new ArrayList<String>());
        
        // Initialize the Library isSupportedFileType method if needed
        try {
            Field supportedExtensionsField = Library.class.getDeclaredField("SUPPORTED_EXTENSIONS");
            supportedExtensionsField.setAccessible(true);
            if (supportedExtensionsField.get(null) == null) {
                supportedExtensionsField.set(null, new String[]{".mp3"});
            }
        } catch (NoSuchFieldException e) {
            // If the field doesn't exist, we'll continue with the test and let it fail
            // if there's an actual issue with the Library class
        }
        
        // Call the method
        musicDirFileFinderMethod.invoke(null, musicDirPath.toFile());
        
        // Check that the files were found
        ArrayList<File> musicDirFiles = (ArrayList<File>) musicDirFilesField.get(null);
        ArrayList<String> musicDirFileNames = (ArrayList<String>) musicDirFileNamesField.get(null);
        
        // More robust assertions to account for existing_song.mp3 also being present
        assertTrue(musicDirFiles.size() >= 3, "Should find at least 3 MP3 files");
        assertTrue(musicDirFileNames.size() >= 3, "Should find at least 3 MP3 file names");
        assertTrue(musicDirFileNames.contains("test_song1.mp3"), "Should find test_song1.mp3");
        assertTrue(musicDirFileNames.contains("test_song2.mp3"), "Should find test_song2.mp3");
        assertTrue(musicDirFileNames.contains("test_song3.mp3"), "Should find test_song3.mp3");
    }
} 