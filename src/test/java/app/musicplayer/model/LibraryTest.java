package app.musicplayer.model;

import app.musicplayer.MusicPlayer;
import app.musicplayer.util.ImportMusicTask;
import app.musicplayer.util.Resources;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.lang.reflect.Field;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static org.junit.jupiter.api.Assertions.assertEquals;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.io.FileWriter;

@ExtendWith(MockitoExtension.class)
class LibraryTest {

    // Use nested class because we want to have different setup/teardown when testing different methods.
    @Nested
    class importMusicTest {

        @TempDir
        Path tempDir;

        @Mock
        ImportMusicTask<Boolean> mockTask;

        @Mock
        AudioFile mockAudioFile;

        @Mock
        Tag mockTag;

        @Mock
        AudioHeader mockHeader;

        private File mp3File;
        private File m4aFile;
        private File wavFile;
        private File nonSupportedFile;
        private File subDirectory;
        private String originalJarPath;

        @BeforeEach
        void setUp() throws Exception {
            // Save original JAR path to restore after test
            originalJarPath = Resources.JAR;

            // Set Resources.JAR to point to temp directory
            Resources.JAR = tempDir.toString() + File.separator;

            // Create test files
            mp3File = new File(tempDir.resolve("test.mp3").toString());
            m4aFile = new File(tempDir.resolve("test.m4a").toString());
            wavFile = new File(tempDir.resolve("test.wav").toString());
            nonSupportedFile = new File(tempDir.resolve("test.txt").toString());
            subDirectory = new File(tempDir.resolve("subdir").toString());

            // Create the files
            assertTrue(mp3File.createNewFile());
            assertTrue(m4aFile.createNewFile());
            assertTrue(wavFile.createNewFile());
            assertTrue(nonSupportedFile.createNewFile());
            assertTrue(subDirectory.mkdir());

            // Create a nested MP3 file in the subdirectory
            File nestedMp3 = new File(subDirectory, "nested.mp3");
            assertTrue(nestedMp3.createNewFile());
        }

        @AfterEach
        void tearDown() {
            Resources.JAR = originalJarPath;
        }

        @Test
        void testImportMusicWithEmptyDirectory() throws Exception {
            // Create empty directory
            File emptyDir = new File(tempDir.resolve("empty").toString());
            assertTrue(emptyDir.mkdir());

            // Test importing from empty directory
            Library.importMusic(emptyDir.getAbsolutePath(), mockTask);

            // Verify the library.xml file exists
            File xmlFile = new File(tempDir.resolve("library.xml").toString());
            assertTrue(xmlFile.exists());

            // Verify task was updated (called with 0 progress since no files found)
            verify(mockTask).updateProgress(0, 0);
        }

        @Test
        void testImportMusicWithSupportedFiles() throws Exception {
            try (MockedStatic<AudioFileIO> audioFileIOMockedStatic = mockStatic(AudioFileIO.class)) {
                // Mock AudioFileIO.read to return our mocked AudioFile
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(any(File.class))).thenReturn(mockAudioFile);

                // Setup mock behavior
                when(mockAudioFile.getTag()).thenReturn(mockTag);
                when(mockAudioFile.getAudioHeader()).thenReturn(mockHeader);
                when(mockTag.getFirst(FieldKey.TITLE)).thenReturn("Test Title");
                when(mockTag.getFirst(FieldKey.ALBUM_ARTIST)).thenReturn("Test Artist");
                when(mockTag.getFirst(FieldKey.ALBUM)).thenReturn("Test Album");
                when(mockHeader.getTrackLength()).thenReturn(180);
                when(mockTag.getFirst(FieldKey.TRACK)).thenReturn("1");
                when(mockTag.getFirst(FieldKey.DISC_NO)).thenReturn("1");

                // Import music
                Library.importMusic(tempDir.toString(), mockTask);

                // Verify the library.xml file exists
                File xmlFile = new File(tempDir.resolve("library.xml").toString());
                assertTrue(xmlFile.exists());

                // Read the content to verify it contains our test files
                String content = new String(Files.readAllBytes(xmlFile.toPath()));
                assertTrue(content.contains("test.mp3"));
                assertTrue(content.contains("test.m4a"));
                assertTrue(content.contains("test.wav"));
                assertTrue(content.contains("nested.mp3"));
                assertFalse(content.contains("test.txt"));

                // Verify task was updated
                verify(mockTask, atLeastOnce()).updateProgress(anyInt(), eq(4));
            }
        }

        @Test
        void testImportMusicWithNullOrEmptyTagValues() throws Exception {
            try (MockedStatic<AudioFileIO> audioFileIOMockedStatic = mockStatic(AudioFileIO.class)) {
                // Mock AudioFileIO.read
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(any(File.class))).thenReturn(mockAudioFile);
                when(mockAudioFile.getTag()).thenReturn(mockTag);
                when(mockAudioFile.getAudioHeader()).thenReturn(mockHeader);
                when(mockTag.getFirst(FieldKey.TITLE)).thenReturn("Test Title");
                when(mockTag.getFirst(FieldKey.ALBUM_ARTIST)).thenReturn(null);
                when(mockTag.getFirst(FieldKey.ARTIST)).thenReturn("");
                when(mockTag.getFirst(FieldKey.ALBUM)).thenReturn("Test Album");
                when(mockHeader.getTrackLength()).thenReturn(180);
                when(mockTag.getFirst(FieldKey.TRACK)).thenReturn(null);
                when(mockTag.getFirst(FieldKey.DISC_NO)).thenReturn("");

                // Import music
                Library.importMusic(tempDir.toString(), mockTask);

                // Verify the library.xml file
                File xmlFile = new File(tempDir.resolve("library.xml").toString());
                assertTrue(xmlFile.exists());
                String content = new String(Files.readAllBytes(xmlFile.toPath()));
                assertTrue(content.contains("<artist/>"));
                assertTrue(content.contains("<trackNumber>0</trackNumber>"));
                assertTrue(content.contains("<discNumber>0</discNumber>"));
            }
        }

        @Test
        void testImportMusicWithExceptionsInReadingAudioFile() throws Exception {
            try (MockedStatic<AudioFileIO> audioFileIOMockedStatic = mockStatic(AudioFileIO.class)) {
                // Make AudioFileIO.read throw an exception for the first file
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(eq(mp3File)))
                        .thenThrow(new RuntimeException("Test exception"));
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(not(eq(mp3File))))
                        .thenReturn(mockAudioFile);

                // Mock
                when(mockAudioFile.getTag()).thenReturn(mockTag);
                when(mockAudioFile.getAudioHeader()).thenReturn(mockHeader);
                when(mockTag.getFirst(any(FieldKey.class))).thenReturn("Test Value");
                when(mockHeader.getTrackLength()).thenReturn(180);

                // Import music
                Library.importMusic(tempDir.toString(), mockTask);

                // Verify the library.xml file
                File xmlFile = new File(tempDir.resolve("library.xml").toString());
                assertTrue(xmlFile.exists());

                // Verify task was still updated
                verify(mockTask, atLeastOnce()).updateProgress(anyInt(), eq(4));
            }
        }

        @Test
        void testImportMusicNullArtistHandling() throws Exception {
            try (MockedStatic<AudioFileIO> audioFileIOMockedStatic = mockStatic(AudioFileIO.class)) {
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(any(File.class))).thenReturn(mockAudioFile);

                // Setup mock behavior for null ALBUM_ARTIST but valid ARTIST
                when(mockAudioFile.getTag()).thenReturn(mockTag);
                when(mockAudioFile.getAudioHeader()).thenReturn(mockHeader);
                when(mockTag.getFirst(FieldKey.TITLE)).thenReturn("Test Title");
                when(mockTag.getFirst(FieldKey.ALBUM_ARTIST)).thenReturn(null);
                when(mockTag.getFirst(FieldKey.ARTIST)).thenReturn("Fallback Artist");
                when(mockTag.getFirst(FieldKey.ALBUM)).thenReturn("Test Album");
                when(mockHeader.getTrackLength()).thenReturn(180);
                when(mockTag.getFirst(FieldKey.TRACK)).thenReturn("1");
                when(mockTag.getFirst(FieldKey.DISC_NO)).thenReturn("1");

                Library.importMusic(tempDir.toString(), mockTask);

                // Verify XML
                File xmlFile = new File(tempDir.resolve("library.xml").toString());
                String content = new String(Files.readAllBytes(xmlFile.toPath()));
                assertTrue(content.contains("<artist>Fallback Artist</artist>"));
            }
        }

        @Test
        void testIsSupportedFileType() {
            // Test supported file types
            assertTrue(Library.isSupportedFileType("test.mp3"));
            assertTrue(Library.isSupportedFileType("test.mp4"));
            assertTrue(Library.isSupportedFileType("test.m4a"));
            assertTrue(Library.isSupportedFileType("test.m4v"));
            assertTrue(Library.isSupportedFileType("test.wav"));

            // Test unsupported file types
            assertFalse(Library.isSupportedFileType("test.txt"));
            assertFalse(Library.isSupportedFileType("test.jpg"));
            assertFalse(Library.isSupportedFileType("test"));
            assertFalse(Library.isSupportedFileType(""));
        }
    }

    @Nested
    class getMaxProgressTest {

        @TempDir
        Path tempDir;

        private Field maxProgressField;

        @BeforeEach
        void setUp() throws NoSuchFieldException {
            maxProgressField = Library.class.getDeclaredField("maxProgress");
            maxProgressField.setAccessible(true);
        }

        @Test
        void testEmptyDirectory() throws Exception {
            File directory = tempDir.toFile();
            maxProgressField.set(null, 0);
            invokeGetMaxProgress(directory);
            assertEquals(0, maxProgressField.getInt(null));
        }

        @Test
        void testSupportedFiles() throws Exception {
            File directory = tempDir.toFile();

            // Create some supported files
            createEmptyFile(tempDir.resolve("song1.mp3"));
            createEmptyFile(tempDir.resolve("song2.mp4"));
            createEmptyFile(tempDir.resolve("song3.m4a"));

            maxProgressField.set(null, 0);
            invokeGetMaxProgress(directory);
            assertEquals(3, maxProgressField.getInt(null));
        }

        @Test
        void testUnsupportedFiles() throws Exception {
            File directory = tempDir.toFile();

            // Create some unsupported files
            createEmptyFile(tempDir.resolve("document.pdf"));
            createEmptyFile(tempDir.resolve("image.jpg"));
            createEmptyFile(tempDir.resolve("text.txt"));

            maxProgressField.set(null, 0);
            invokeGetMaxProgress(directory);
            assertEquals(0, maxProgressField.getInt(null));
        }

        @Test
        void testMixedFiles() throws Exception {
            File directory = tempDir.toFile();

            // Create a mix of supported and unsupported files
            createEmptyFile(tempDir.resolve("song1.mp3"));
            createEmptyFile(tempDir.resolve("document.pdf"));
            createEmptyFile(tempDir.resolve("song2.wav"));
            createEmptyFile(tempDir.resolve("image.jpg"));

            maxProgressField.set(null, 0);
            invokeGetMaxProgress(directory);
            assertEquals(2, maxProgressField.getInt(null));
        }

        @Test
        void testSubdirectories() throws Exception {
            File directory = tempDir.toFile();

            // Create a subdirectory with some files
            Path subDir = tempDir.resolve("subDir");
            Files.createDirectory(subDir);

            // Add files to main directory
            createEmptyFile(tempDir.resolve("song1.mp3"));
            createEmptyFile(tempDir.resolve("document.pdf"));

            // Add files to subdirectory
            createEmptyFile(subDir.resolve("song2.mp4"));
            createEmptyFile(subDir.resolve("song3.wav"));
            createEmptyFile(subDir.resolve("image.jpg"));

            maxProgressField.set(null, 0);
            invokeGetMaxProgress(directory);
            assertEquals(3, maxProgressField.getInt(null));
        }

        @Test
        void testNestedSubdirectories() throws Exception {
            // Test deeply nested subdirectories
            File directory = tempDir.toFile();

            // Create nested subdirectories
            Path subDir1 = tempDir.resolve("subDir1");
            Files.createDirectory(subDir1);
            Path subDir2 = subDir1.resolve("subDir2");
            Files.createDirectory(subDir2);
            Path subDir3 = subDir2.resolve("subDir3");
            Files.createDirectory(subDir3);

            // Add files at various levels
            createEmptyFile(tempDir.resolve("song1.mp3"));
            createEmptyFile(subDir1.resolve("song2.mp4"));
            createEmptyFile(subDir2.resolve("document.pdf"));
            createEmptyFile(subDir3.resolve("song3.wav"));
            createEmptyFile(subDir3.resolve("song4.m4a"));

            maxProgressField.set(null, 0);
            invokeGetMaxProgress(directory);
            assertEquals(4, maxProgressField.getInt(null));
        }

        @Test
        void testNullListFiles() {
            // Create a mock directory that returns null for listFiles()
            File mockDirectory = new File(tempDir.toFile(), "nonexistent") {
                @Override
                public File[] listFiles() {
                    return null;
                }

                @Override
                public boolean isDirectory() {
                    return true;
                }
            };

            // Reset maxProgress before test
            assertThrows(Exception.class, () -> {
                invokeGetMaxProgress(mockDirectory);
            });
        }

        // Helper method to invoke the private static method getMaxProgress
        private void invokeGetMaxProgress(File directory) throws Exception {
            java.lang.reflect.Method method = Library.class.getDeclaredMethod("getMaxProgress", File.class);
            method.setAccessible(true);
            method.invoke(null, directory);
        }

        // Helper method to create an empty file
        private void createEmptyFile(Path path) throws IOException {
            Files.createFile(path);
        }
    }

    @Nested
    class WriteXMLTest {

        @TempDir
        Path tempDir;

        @Mock
        private Document docMock;

        @Mock
        private Element songsMock;

        @Mock
        private ImportMusicTask<Boolean> taskMock;

        private Element songElementMock;
        private Element idElementMock;
        private Element titleElementMock;
        private Element artistElementMock;
        private Element albumElementMock;
        private Element lengthElementMock;
        private Element trackNumberElementMock;
        private Element discNumberElementMock;
        private Element playCountElementMock;
        private Element playDateElementMock;
        private Element locationElementMock;

        private AudioFile audioFileMock;
        private Tag tagMock;
        private AudioHeader headerMock;

        @BeforeEach
        void setUp() {
            // Set up the Library.task field using reflection
            try {
                java.lang.reflect.Field taskField = Library.class.getDeclaredField("task");
                taskField.setAccessible(true);
                taskField.set(null, taskMock);

                java.lang.reflect.Field maxProgressField = Library.class.getDeclaredField("maxProgress");
                maxProgressField.setAccessible(true);
                maxProgressField.set(null, 10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Helper method to set up mocks for tests that process audio files
        private void setupAudioFileMocks() {
            songElementMock = mock(Element.class);
            idElementMock = mock(Element.class);
            titleElementMock = mock(Element.class);
            artistElementMock = mock(Element.class);
            albumElementMock = mock(Element.class);
            lengthElementMock = mock(Element.class);
            trackNumberElementMock = mock(Element.class);
            discNumberElementMock = mock(Element.class);
            playCountElementMock = mock(Element.class);
            playDateElementMock = mock(Element.class);
            locationElementMock = mock(Element.class);

            // Set up the Document.createElement behavior
            when(docMock.createElement("song")).thenReturn(songElementMock);
            when(docMock.createElement("id")).thenReturn(idElementMock);
            when(docMock.createElement("title")).thenReturn(titleElementMock);
            when(docMock.createElement("artist")).thenReturn(artistElementMock);
            when(docMock.createElement("album")).thenReturn(albumElementMock);
            when(docMock.createElement("length")).thenReturn(lengthElementMock);
            when(docMock.createElement("trackNumber")).thenReturn(trackNumberElementMock);
            when(docMock.createElement("discNumber")).thenReturn(discNumberElementMock);
            when(docMock.createElement("playCount")).thenReturn(playCountElementMock);
            when(docMock.createElement("playDate")).thenReturn(playDateElementMock);
            when(docMock.createElement("location")).thenReturn(locationElementMock);

            // Mock audio file classes
            audioFileMock = mock(AudioFile.class);
            tagMock = mock(Tag.class);
            headerMock = mock(AudioHeader.class);

            // Setup audio file mocks
            when(audioFileMock.getTag()).thenReturn(tagMock);
            when(audioFileMock.getAudioHeader()).thenReturn(headerMock);
            when(headerMock.getTrackLength()).thenReturn(180);
        }

        @Test
        void testEmptyDirectory() throws Exception {
            File directory = new File(tempDir.toString());
            int result = callWriteXML(directory, 0);
            // Verify no interactions with Document since there are no files
            verify(docMock, never()).createElement("song");
            assertEquals(0, result);
        }

        @Test
        void testWithMP3File() throws Exception {
            setupAudioFileMocks();
            // Create a mock MP3 file
            Path mp3Path = tempDir.resolve("test.mp3");
            Files.createFile(mp3Path);

            File directory = new File(tempDir.toString());
            try (MockedStatic<AudioFileIO> audioFileIOMockedStatic = mockStatic(AudioFileIO.class)) {
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(any(File.class))).thenReturn(audioFileMock);

                // Configure tag mock responses
                when(tagMock.getFirst(FieldKey.TITLE)).thenReturn("Test Title");
                when(tagMock.getFirst(FieldKey.ALBUM_ARTIST)).thenReturn("Test Artist");
                when(tagMock.getFirst(FieldKey.ALBUM)).thenReturn("Test Album");
                when(tagMock.getFirst(FieldKey.TRACK)).thenReturn("1");
                when(tagMock.getFirst(FieldKey.DISC_NO)).thenReturn("1");

                int result = callWriteXML(directory, 0);

                verify(docMock).createElement("song");
                verify(songsMock).appendChild(songElementMock);
                verify(songElementMock).appendChild(idElementMock);
                verify(idElementMock).setTextContent("0");
                verify(titleElementMock).setTextContent("Test Title");
                verify(artistElementMock).setTextContent("Test Artist");
                verify(albumElementMock).setTextContent("Test Album");
                verify(lengthElementMock).setTextContent("180");
                verify(trackNumberElementMock).setTextContent("1");
                verify(discNumberElementMock).setTextContent("1");
                verify(taskMock).updateProgress(1, 10);

                assertEquals(1, result);
            }
        }

        @Test
        void testWithM4AFile() throws Exception {
            setupAudioFileMocks();

            // Create a mock M4A file
            Path m4aPath = tempDir.resolve("test.m4a");
            Files.createFile(m4aPath);
            File directory = new File(tempDir.toString());
            try (MockedStatic<AudioFileIO> audioFileIOMockedStatic = mockStatic(AudioFileIO.class)) {
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(any(File.class))).thenReturn(audioFileMock);

                // Configure tag mock responses
                when(tagMock.getFirst(FieldKey.TITLE)).thenReturn("M4A Title");
                when(tagMock.getFirst(FieldKey.ALBUM_ARTIST)).thenReturn("M4A Artist");
                when(tagMock.getFirst(FieldKey.ALBUM)).thenReturn("M4A Album");
                when(tagMock.getFirst(FieldKey.TRACK)).thenReturn("2");
                when(tagMock.getFirst(FieldKey.DISC_NO)).thenReturn("2");

                int result = callWriteXML(directory, 0);

                verify(docMock).createElement("song");
                verify(titleElementMock).setTextContent("M4A Title");
                verify(taskMock).updateProgress(1, 10);

                assertEquals(1, result);
            }
        }

        @Test
        void testWAVFile() throws Exception {
            setupAudioFileMocks();

            // Create a mock WAV file
            Path wavPath = tempDir.resolve("test.wav");
            Files.createFile(wavPath);
            File directory = new File(tempDir.toString());

            // Mock AudioFileIO.read
            try (MockedStatic<AudioFileIO> audioFileIOMockedStatic = mockStatic(AudioFileIO.class)) {
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(any(File.class))).thenReturn(audioFileMock);

                // Configure tag mock responses
                when(tagMock.getFirst(FieldKey.TITLE)).thenReturn("WAV Title");
                when(tagMock.getFirst(FieldKey.ALBUM_ARTIST)).thenReturn(null);
                when(tagMock.getFirst(FieldKey.ARTIST)).thenReturn("WAV Artist");
                when(tagMock.getFirst(FieldKey.ALBUM)).thenReturn("WAV Album");
                when(tagMock.getFirst(FieldKey.TRACK)).thenReturn(null);
                when(tagMock.getFirst(FieldKey.DISC_NO)).thenReturn(null);

                int result = callWriteXML(directory, 0);

                verify(docMock).createElement("song");
                verify(titleElementMock).setTextContent("WAV Title");
                verify(artistElementMock).setTextContent("WAV Artist");
                verify(trackNumberElementMock).setTextContent("0");
                verify(discNumberElementMock).setTextContent("0");
                verify(taskMock).updateProgress(1, 10);

                assertEquals(1, result);
            }
        }

        @Test
        void testUnsupportedFile() throws Exception {
            // Create an unsupported file type
            Path txtPath = tempDir.resolve("test.txt");
            Files.createFile(txtPath);
            File directory = new File(tempDir.toString());

            int result = callWriteXML(directory, 0);

            // Verify no interactions with Document since the file type is not supported
            verify(docMock, never()).createElement("song");
            verify(taskMock, never()).updateProgress(anyInt(), anyInt());
            assertEquals(0, result);
        }

        @Test
        void testSubdirectory() throws Exception {
            setupAudioFileMocks();
            Path subDir = tempDir.resolve("subdir");
            Files.createDirectory(subDir);
            Path mp3Path = subDir.resolve("subdir_test.mp3");
            Files.createFile(mp3Path);

            // Set up the main directory
            File directory = new File(tempDir.toString());

            // Mock AudioFileIO.read
            try (MockedStatic<AudioFileIO> audioFileIOMockedStatic = mockStatic(AudioFileIO.class)) {
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(any(File.class))).thenReturn(audioFileMock);

                // Configure tag mock responses
                when(tagMock.getFirst(FieldKey.TITLE)).thenReturn("Subdir Title");
                when(tagMock.getFirst(FieldKey.ALBUM_ARTIST)).thenReturn("Subdir Artist");
                when(tagMock.getFirst(FieldKey.ALBUM)).thenReturn("Subdir Album");
                when(tagMock.getFirst(FieldKey.TRACK)).thenReturn("3");
                when(tagMock.getFirst(FieldKey.DISC_NO)).thenReturn("3");

                int result = callWriteXML(directory, 0);

                verify(docMock).createElement("song");
                verify(titleElementMock).setTextContent("Subdir Title");
                verify(taskMock).updateProgress(1, 10);

                assertEquals(1, result);
            }
        }

        @Test
        void testExceptionHandling() throws Exception {
            // Create a mock MP3 file
            Path mp3Path = tempDir.resolve("bad.mp3");
            Files.createFile(mp3Path);
            File directory = new File(tempDir.toString());

            // Mock AudioFileIO.read to throw an exception
            try (MockedStatic<AudioFileIO> audioFileIOMockedStatic = mockStatic(AudioFileIO.class)) {
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(any(File.class)))
                        .thenThrow(new RuntimeException("Test exception"));

                int result = callWriteXML(directory, 0);

                verify(docMock, never()).createElement("song");

                // The result should be unchanged as the file processing failed
                assertEquals(0, result);
            }
        }

        @Test
        void testNullEmptyArtistAndTracks() throws Exception {
            setupAudioFileMocks();

            // Create a mock MP3 file
            Path mp3Path = tempDir.resolve("null_fields.mp3");
            Files.createFile(mp3Path);
            File directory = new File(tempDir.toString());
            try (MockedStatic<AudioFileIO> audioFileIOMockedStatic = mockStatic(AudioFileIO.class)) {
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(any(File.class))).thenReturn(audioFileMock);

                // Configure tag mock responses with null/empty values to test the conditional branches
                when(tagMock.getFirst(FieldKey.TITLE)).thenReturn("Test Title");
                when(tagMock.getFirst(FieldKey.ALBUM_ARTIST)).thenReturn("");
                when(tagMock.getFirst(FieldKey.ARTIST)).thenReturn(""); // Empty artist
                when(tagMock.getFirst(FieldKey.ALBUM)).thenReturn("Test Album");
                when(tagMock.getFirst(FieldKey.TRACK)).thenReturn("null"); // "null" string
                when(tagMock.getFirst(FieldKey.DISC_NO)).thenReturn(""); // Empty disc number

                int result = callWriteXML(directory, 0);

                // Verify the conditional paths for empty/null values
                verify(artistElementMock).setTextContent("");
                verify(trackNumberElementMock).setTextContent("0");
                verify(discNumberElementMock).setTextContent("0");

                assertEquals(1, result);
            }
        }

        @Test
        void testMixedFileTypes() throws Exception {
            setupAudioFileMocks();

            // Create various file types
            Path mp3Path = tempDir.resolve("test1.mp3");
            Path m4aPath = tempDir.resolve("test2.m4a");
            Path wavPath = tempDir.resolve("test3.wav");
            Path txtPath = tempDir.resolve("test4.txt");
            Files.createFile(mp3Path);
            Files.createFile(m4aPath);
            Files.createFile(wavPath);
            Files.createFile(txtPath);

            File directory = new File(tempDir.toString());
            try (MockedStatic<AudioFileIO> audioFileIOMockedStatic = mockStatic(AudioFileIO.class)) {
                audioFileIOMockedStatic.when(() -> AudioFileIO.read(any(File.class))).thenReturn(audioFileMock);

                // Configure tag mock responses
                when(tagMock.getFirst(FieldKey.TITLE)).thenReturn("Mixed Title");
                when(tagMock.getFirst(FieldKey.ALBUM_ARTIST)).thenReturn("Mixed Artist");
                when(tagMock.getFirst(FieldKey.ALBUM)).thenReturn("Mixed Album");
                when(tagMock.getFirst(FieldKey.TRACK)).thenReturn("4");
                when(tagMock.getFirst(FieldKey.DISC_NO)).thenReturn("4");

                int result = callWriteXML(directory, 0);

                verify(docMock, times(3)).createElement("song");
                verify(taskMock, times(3)).updateProgress(anyInt(), eq(10));

                assertEquals(3, result);
            }
        }

        // Helper method to call the private writeXML method using reflection
        private int callWriteXML(File directory, int initialId) throws Exception {
            java.lang.reflect.Method writeXMLMethod = Library.class.getDeclaredMethod(
                    "writeXML", File.class, Document.class, Element.class, int.class);
            writeXMLMethod.setAccessible(true);
            return (int) writeXMLMethod.invoke(null, directory, docMock, songsMock, initialId);
        }
    }

    @Nested
    class isSupportedFileTypeTest {

        @Test
        void testFileWithoutExtension() {
            assertFalse(Library.isSupportedFileType("myfile"));
        }

        @Test
        void testFileWithDotButNoExtension() {
            // Test a file with a dot but no extension
            assertFalse(Library.isSupportedFileType("myfile."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"mp3", "MP3"})
        void testMP3Files(String extension) {
            assertTrue(Library.isSupportedFileType("song." + extension));
        }

        @ParameterizedTest
        @ValueSource(strings = {"mp4", "m4a", "m4v", "MP4", "M4A", "M4V"})
        void testMP4Files(String extension) {
            assertTrue(Library.isSupportedFileType("video." + extension));
        }

        @ParameterizedTest
        @ValueSource(strings = {"wav", "WAV"})
        void testWAVFiles(String extension) {
            assertTrue(Library.isSupportedFileType("audio." + extension));
        }

        @ParameterizedTest
        @ValueSource(strings = {"txt", "pdf", "jpg", "doc", "flac", "ogg", "aac"})
        void testUnsupportedExtensions(String extension) {
            assertFalse(Library.isSupportedFileType("file." + extension));
        }
    }

    @Nested
    class GetSongsTest {
        private ArrayList<Song> songList;
        private Song song1, song2;

        @BeforeEach
        void setUp() {
            songList = new ArrayList<>();
            song1 = new Song(0, "Test Song 1", "Test Artist", "Test Album",
                    Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "/path/to/song1");
            song2 = new Song(1, "Test Song 2", "Test Artist", "Test Album",
                    Duration.ofSeconds(240), 2, 1, 0, LocalDateTime.now(), "/path/to/song2");
            songList.add(song1);
            songList.add(song2);
        }

        @Test
        void testSongsIsNull() throws Exception {
            setStaticField(null);
            ObservableList<Song> result = Library.getSongs();
            assertNotNull(result);
            assertTrue(result.isEmpty());
            setStaticField(null);
        }

        @Test
        void testSongsIsNotNull() throws Exception {
            setStaticField(songList);
            ObservableList<Song> result = Library.getSongs();
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Test Song 1", result.get(0).getTitle());
            assertEquals("Test Song 2", result.get(1).getTitle());
            setStaticField(null);
        }

        private void setStaticField(Object value) throws Exception {
            Field field = Library.class.getDeclaredField("songs");
            field.setAccessible(true);
            field.set(null, value);
        }
    }

    @Nested
    class GetSongTest {
        private ArrayList<Song> songList;
        private Song song1, song2;

        @BeforeEach
        void setUp() {
            songList = new ArrayList<>();
            song1 = new Song(0, "Test Song 1", "Test Artist", "Test Album",
                    Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "/path/to/song1");
            song2 = new Song(1, "Test Song 2", "Test Artist", "Test Album",
                    Duration.ofSeconds(240), 2, 1, 0, LocalDateTime.now(), "/path/to/song2");
            songList.add(song1);
            songList.add(song2);
        }

        @Test
        void testGetSongById_WhenSongsIsNull() throws Exception {
            try (MockedStatic<Library> mockedLibrary = Mockito.mockStatic(Library.class)) {
                setStaticField(Library.class, "songs", null);
                mockedLibrary.when(Library::getSongs).then(invocation -> {
                    setStaticField(Library.class, "songs", songList);
                    return FXCollections.observableArrayList(songList);
                });

                Method getSongByIdMethod = Library.class.getDeclaredMethod("getSong", int.class);
                getSongByIdMethod.setAccessible(true);

                // Call the method with reflection
                mockedLibrary.when(() -> getSongByIdMethod.invoke(null, 0)).thenCallRealMethod();
                Song result = (Song) getSongByIdMethod.invoke(null, 0);

                assertNotNull(result);
                assertEquals("Test Song 1", result.getTitle());
                mockedLibrary.verify(Library::getSongs);
            } finally {
                setStaticField(Library.class, "songs", null);
            }
        }

        @Test
        void testGetSongById_WhenSongsIsNotNull() throws Exception {
            try (MockedStatic<Library> mockedLibrary = Mockito.mockStatic(Library.class)) {
                setStaticField(Library.class, "songs", songList);
                Method getSongByIdMethod = Library.class.getDeclaredMethod("getSong", int.class);
                getSongByIdMethod.setAccessible(true);

                mockedLibrary.when(() -> getSongByIdMethod.invoke(null, 1)).thenCallRealMethod();
                Song result = (Song) getSongByIdMethod.invoke(null, 1);

                assertNotNull(result);
                assertEquals("Test Song 2", result.getTitle());
                mockedLibrary.verify(Library::getSongs, Mockito.never());
            } finally {
                setStaticField(Library.class, "songs", null);
            }
        }

        @Test
        void testGetSongByTitle_WhenSongsIsNull() throws Exception {
            try (MockedStatic<Library> mockedLibrary = Mockito.mockStatic(Library.class)) {
                setStaticField(Library.class, "songs", null);
                mockedLibrary.when(Library::getSongs).then(invocation -> {
                    setStaticField(Library.class, "songs", songList);
                    return FXCollections.observableArrayList(songList);
                });

                mockedLibrary.when(() -> Library.getSong("Test Song 1")).thenCallRealMethod();
                Song result = Library.getSong("Test Song 1");

                assertNotNull(result);
                assertEquals("Test Song 1", result.getTitle());
                mockedLibrary.verify(Library::getSongs);
            } finally {
                setStaticField(Library.class, "songs", null);
            }
        }

        @Test
        void testGetSongByTitle_WhenSongsIsNotNull() throws Exception {
            try (MockedStatic<Library> mockedLibrary = Mockito.mockStatic(Library.class)) {
                setStaticField(Library.class, "songs", songList);
                mockedLibrary.when(() -> Library.getSong("Test Song 2")).thenCallRealMethod();
                Song result = Library.getSong("Test Song 2");

                assertNotNull(result);
                assertEquals("Test Song 2", result.getTitle());
                mockedLibrary.verify(Library::getSongs, Mockito.never());
            } finally {
                setStaticField(Library.class, "songs", null);
            }
        }

        private void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        }
    }

    @Nested
    class UpdateSongsListTest {

        @TempDir
        Path tempDir;

        private String originalJarPath;
        private File xmlFile;

        @BeforeEach
        void setUp() throws Exception {
            originalJarPath = Resources.JAR;
            Resources.JAR = tempDir.toString() + File.separator;
            xmlFile = new File(tempDir.resolve("library.xml").toString());
        }

        @AfterEach
        void tearDown() {
            Resources.JAR = originalJarPath;
            try {
                Field songsField = Library.class.getDeclaredField("songs");
                songsField.setAccessible(true);
                songsField.set(null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Test
        void testUpdateSongsListWithValidXml() throws Exception {
            // Create sample XML content with multiple songs
            String xmlContent =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<library>\n" +
                            "    <songs>\n" +
                            "        <song>\n" +
                            "            <id>0</id>\n" +
                            "            <title>Song 1</title>\n" +
                            "            <artist>Artist 1</artist>\n" +
                            "            <album>Album 1</album>\n" +
                            "            <length>180</length>\n" +
                            "            <trackNumber>1</trackNumber>\n" +
                            "            <discNumber>1</discNumber>\n" +
                            "            <playCount>5</playCount>\n" +
                            "            <playDate>2023-01-01T12:00:00</playDate>\n" +
                            "            <location>/path/to/song1.mp3</location>\n" +
                            "        </song>\n" +
                            "        <song>\n" +
                            "            <id>1</id>\n" +
                            "            <title>Song 2</title>\n" +
                            "            <artist>Artist 2</artist>\n" +
                            "            <album>Album 2</album>\n" +
                            "            <length>240</length>\n" +
                            "            <trackNumber>2</trackNumber>\n" +
                            "            <discNumber>1</discNumber>\n" +
                            "            <playCount>3</playCount>\n" +
                            "            <playDate>2023-01-02T12:00:00</playDate>\n" +
                            "            <location>/path/to/song2.mp3</location>\n" +
                            "        </song>\n" +
                            "    </songs>\n" +
                            "</library>";

            // Write content to the file
            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write(xmlContent);
            }

            ObservableList<Song> songs = Library.getSongs();

            // Verify the songs were loaded correctly
            assertEquals(2, songs.size());
            assertEquals("Song 1", songs.get(0).getTitle());
            assertEquals("Artist 1", songs.get(0).getArtist());
            assertEquals("Album 1", songs.get(0).getAlbum());
            assertEquals("3:00", songs.get(0).getLength());
            assertEquals(1, songs.get(0).getTrackNumber());
            assertEquals(1, songs.get(0).getDiscNumber());
            assertEquals(5, songs.get(0).getPlayCount());
            assertEquals(LocalDateTime.parse("2023-01-01T12:00:00"), songs.get(0).getPlayDate());
            assertEquals("/path/to/song1.mp3", songs.get(0).getLocation());
            assertEquals("Song 2", songs.get(1).getTitle());
        }

        @Test
        void testUpdateSongsListWithInvalidXml() throws Exception {
            String invalidXml = "This is not valid XML content";

            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write(invalidXml);
            }

            ObservableList<Song> songs = Library.getSongs();

            // Verify we get an empty list instead of an exception
            assertNotNull(songs);
            assertTrue(songs.isEmpty());
        }

        @Test
        void testUpdateSongsListWithMissingXmlFile() {
            ObservableList<Song> songs = Library.getSongs();
            // Verify we get an empty list instead of an exception
            assertNotNull(songs);
            assertTrue(songs.isEmpty());
        }

        @Test
        void testUpdateSongsListWithEmptyFile() throws Exception {
            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write("");
            }
            ObservableList<Song> songs = Library.getSongs();

            // Verify we get an empty list instead of an exception
            assertNotNull(songs);
            assertTrue(songs.isEmpty());
        }
    }

    @Nested
    class GetAlbumsTest {
        @BeforeEach
        void setUp() throws Exception {
            resetStaticField("albums");
            resetStaticField("songs");
        }

        @AfterEach
        void tearDown() throws Exception {
            resetStaticField("albums");
            resetStaticField("songs");
        }

        @Test
        void testAlbumsIsNullAndSongsIsNull() throws Exception {
            assertNull(getStaticField("albums"));
            assertNull(getStaticField("songs"));

            // Modify the updateAlbumsList method to prevent actual execution
            Method updateAlbumsListMethod = Library.class.getDeclaredMethod("updateAlbumsList");
            updateAlbumsListMethod.setAccessible(true);

            // Inject a mock implementation
            Method getSongsMethod = Library.class.getDeclaredMethod("getSongs");
            getSongsMethod.setAccessible(true);
            ArrayList<Song> mockSongs = new ArrayList<>();
            setStaticField("songs", mockSongs);
            ArrayList<Album> mockAlbums = new ArrayList<>();
            setStaticField("albums", mockAlbums);

            ObservableList<Album> result = Library.getAlbums();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void testAlbumsIsNullAndSongsIsNotNull() throws Exception {
            assertNull(getStaticField("albums"));

            // Create and set songs list
            ArrayList<Song> songList = new ArrayList<>();
            setStaticField("songs", songList);
            assertNotNull(getStaticField("songs"));

            ArrayList<Album> mockAlbums = new ArrayList<>();
            Album mockAlbum = mock(Album.class);
            when(mockAlbum.getTitle()).thenReturn("Test Album");
            mockAlbums.add(mockAlbum);
            setStaticField("albums", mockAlbums);

            ObservableList<Album> result = Library.getAlbums();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Test Album", result.get(0).getTitle());
        }

        @Test
        void testAlbumsIsNotNull() throws Exception {
            ArrayList<Album> albumList = new ArrayList<>();
            Album mockAlbum = mock(Album.class);
            when(mockAlbum.getTitle()).thenReturn("Pre-existing Album");
            albumList.add(mockAlbum);

            setStaticField("albums", albumList);
            assertNotNull(getStaticField("albums"));

            ObservableList<Album> result = Library.getAlbums();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Pre-existing Album", result.get(0).getTitle());
        }

        // Helper methods to access and modify static fields using reflection
        private void resetStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        }

        private Object getStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        }

        private void setStaticField(String fieldName, Object value) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        }
    }

    @Nested
    class UpdateAlbumsListTests {

        @BeforeEach
        void setUp() throws Exception {
            resetLibraryStaticFields();
        }

        @AfterEach
        void tearDown() throws Exception {
            resetLibraryStaticFields();
        }

        @Test
        void testEmptySongsList() throws Exception {
            ArrayList<Song> emptySongsList = new ArrayList<>();
            setStaticField("songs", emptySongsList);

            invokeUpdateAlbumsList();

            ArrayList<Album> albums = (ArrayList<Album>) getStaticField("albums");
            assertNotNull(albums);
            assertTrue(albums.isEmpty());
        }

        @Test
        void testSongsHavingNullAlbum() throws Exception {
            ArrayList<Song> songsList = new ArrayList<>();
            Song songWithNullAlbum = mock(Song.class);
            when(songWithNullAlbum.getAlbum()).thenReturn(null);
            songsList.add(songWithNullAlbum);
            setStaticField("songs", songsList);

            invokeUpdateAlbumsList();

            ArrayList<Album> albums = (ArrayList<Album>) getStaticField("albums");
            assertNotNull(albums);
            assertTrue(albums.isEmpty());
        }

        @Test
        void testSongsHavingNullArtist() throws Exception {
            ArrayList<Song> songsList = new ArrayList<>();
            Song song = mock(Song.class);
            when(song.getAlbum()).thenReturn("Test Album");
            when(song.getArtist()).thenReturn(null);
            songsList.add(song);
            setStaticField("songs", songsList);

            invokeUpdateAlbumsList();

            ArrayList<Album> albums = (ArrayList<Album>) getStaticField("albums");
            assertNotNull(albums);
            assertTrue(albums.isEmpty());
        }

        @Test
        void testUpdateAlbumsList_WithValidSongs() throws Exception {
            ArrayList<Song> songsList = new ArrayList<>();

            Song song1 = mock(Song.class);
            when(song1.getAlbum()).thenReturn("Album 1");
            when(song1.getArtist()).thenReturn("Artist 1");

            Song song2 = mock(Song.class);
            when(song2.getAlbum()).thenReturn("Album 1");
            when(song2.getArtist()).thenReturn("Artist 1");

            Song song3 = mock(Song.class);
            when(song3.getAlbum()).thenReturn("Album 2");
            when(song3.getArtist()).thenReturn("Artist 2");

            songsList.add(song1);
            songsList.add(song2);
            songsList.add(song3);

            setStaticField("songs", songsList);

            try (MockedConstruction<Album> albumMock = mockConstruction(Album.class)) {
                invokeUpdateAlbumsList();
                ArrayList<Album> albums = (ArrayList<Album>) getStaticField("albums");
                assertNotNull(albums);
                assertEquals(2, albums.size());
                verify(song1, atLeastOnce()).getAlbum();
                verify(song1, atLeastOnce()).getArtist();
                verify(song3, atLeastOnce()).getAlbum();
                verify(song3, atLeastOnce()).getArtist();

                // Verify Album constructor was called with expected parameters
                assertEquals(2, albumMock.constructed().size());
            }
        }

        @Test
        void testUpdateAlbumsList_WithMultipleArtistsForSameAlbum() throws Exception {
            ArrayList<Song> songsList = new ArrayList<>();

            Song song1 = mock(Song.class);
            when(song1.getAlbum()).thenReturn("Collaborative Album");
            when(song1.getArtist()).thenReturn("Artist 1");

            Song song2 = mock(Song.class);
            when(song2.getAlbum()).thenReturn("Collaborative Album");
            when(song2.getArtist()).thenReturn("Artist 2");

            songsList.add(song1);
            songsList.add(song2);

            setStaticField("songs", songsList);

            try (MockedConstruction<Album> albumMock = mockConstruction(Album.class)) {
                invokeUpdateAlbumsList();

                ArrayList<Album> albums = (ArrayList<Album>) getStaticField("albums");
                assertNotNull(albums);
                assertEquals(2, albums.size());

                verify(song1, atLeastOnce()).getAlbum();
                verify(song1, atLeastOnce()).getArtist();
                verify(song2, atLeastOnce()).getAlbum();
                verify(song2, atLeastOnce()).getArtist();

                assertEquals(2, albumMock.constructed().size());
            }
        }

        // Helper methods to access and modify static fields using reflection
        private void invokeUpdateAlbumsList() throws Exception {
            Method method = Library.class.getDeclaredMethod("updateAlbumsList");
            method.setAccessible(true);
            method.invoke(null);
        }

        private void resetLibraryStaticFields() throws Exception {
            resetStaticField("albums");
            resetStaticField("songs");
        }

        private void resetStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        }

        private void setStaticField(String fieldName, Object value) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        }

        private Object getStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        }
    }

    @Nested
    class GetArtistsTests {

        @BeforeEach
        void setUp() throws Exception {
            resetLibraryStaticFields();
        }

        @AfterEach
        void tearDown() throws Exception {
            resetLibraryStaticFields();
        }

        @Test
        void testArtistsIsNullAlbumsIsNull() throws Exception {
            assertNull(getStaticField("artists"));
            assertNull(getStaticField("albums"));

            // Mock the updateAlbumsList and updateArtistsList methods to avoid actual execution
            try (MockedConstruction<Artist> artistMock = mockConstruction(Artist.class, (mock, context) -> {
                // Do nothing, just avoid calling the real constructor
            })) {
                // Mock the getAlbums method to return an empty list
                try (MockedConstruction<Album> albumMock = mockConstruction(Album.class, (mock, context) -> {
                    // Do nothing, just avoid calling the real constructor
                })) {
                    // Use reflection to call a private method
                    Method updateAlbumsListMethod = Library.class.getDeclaredMethod("updateAlbumsList");
                    updateAlbumsListMethod.setAccessible(true);

                    // Create empty albums list to mock the result of updateAlbumsList
                    ArrayList<Album> mockAlbums = new ArrayList<>();
                    setStaticField("albums", mockAlbums);

                    ObservableList<Artist> result = Library.getArtists();

                    assertNotNull(result);
                    assertTrue(result.isEmpty());

                    assertNotNull(getStaticField("artists"));
                    assertTrue(((ArrayList<Artist>)getStaticField("artists")).isEmpty());
                }
            }
        }

        @Test
        void testArtistsIsNullAlbumsHasNullArtist() throws Exception {
            assertNull(getStaticField("artists"));

            // Create albums with a null artist
            ArrayList<Album> mockAlbums = new ArrayList<>();
            Album mockAlbum = mock(Album.class);
            when(mockAlbum.getArtist()).thenReturn(null);
            mockAlbums.add(mockAlbum);
            setStaticField("albums", mockAlbums);

            try (MockedConstruction<Artist> artistMock = mockConstruction(Artist.class, (mock, context) -> {
                // Do nothing, just avoid calling the real constructor
            })) {
                ObservableList<Artist> result = Library.getArtists();

                assertNotNull(result);
                assertTrue(result.isEmpty());

                assertNotNull(getStaticField("artists"));
                assertTrue(((ArrayList<Artist>)getStaticField("artists")).isEmpty());

                verify(mockAlbum, atLeastOnce()).getArtist();
            }
        }

        @Test
        void testArtistsIsNullWithValidAlbums() throws Exception {
            assertNull(getStaticField("artists"));

            // Create albums with valid artists
            ArrayList<Album> mockAlbums = new ArrayList<>();

            Album mockAlbum1 = mock(Album.class);
            when(mockAlbum1.getArtist()).thenReturn("Artist 1");
            mockAlbums.add(mockAlbum1);

            Album mockAlbum2 = mock(Album.class);
            when(mockAlbum2.getArtist()).thenReturn("Artist 1");
            mockAlbums.add(mockAlbum2);

            Album mockAlbum3 = mock(Album.class);
            when(mockAlbum3.getArtist()).thenReturn("Artist 2");
            mockAlbums.add(mockAlbum3);

            setStaticField("albums", mockAlbums);

            try (MockedConstruction<Artist> artistMock = mockConstruction(Artist.class, (mock, context) -> {
                // Do nothing, just avoid calling the real constructor
            })) {
                ObservableList<Artist> result = Library.getArtists();
                assertNotNull(result);
                assertEquals(2, result.size());

                assertNotNull(getStaticField("artists"));
                assertEquals(2, ((ArrayList<Artist>)getStaticField("artists")).size());

                verify(mockAlbum1, atLeastOnce()).getArtist();
                verify(mockAlbum2, atLeastOnce()).getArtist();
                verify(mockAlbum3, atLeastOnce()).getArtist();

                assertEquals(2, artistMock.constructed().size());
            }
        }

        @Test
        void testGetArtists_WhenArtistsIsNotNull() throws Exception {
            ArrayList<Artist> existingArtists = new ArrayList<>();
            Artist mockArtist = mock(Artist.class);
            when(mockArtist.getTitle()).thenReturn("Pre-existing Artist");
            existingArtists.add(mockArtist);

            setStaticField("artists", existingArtists);
            assertNotNull(getStaticField("artists"));

            ObservableList<Artist> result = Library.getArtists();

            assertNotNull(result);
            assertEquals(1, result.size());

            assertEquals("Pre-existing Artist", result.get(0).getTitle());
        }

        // Helper methods to access and modify static fields using reflection
        private void resetLibraryStaticFields() throws Exception {
            resetStaticField("artists");
            resetStaticField("albums");
            resetStaticField("songs");
        }

        private void resetStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        }

        private void setStaticField(String fieldName, Object value) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        }

        private Object getStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetArtistTest {

        @Mock
        private Artist artist1;

        @Test
        void testArtistsIsNull() throws Exception {
            try (MockedStatic<Library> mockedLibrary = mockStatic(Library.class)) {
                ArrayList<Artist> artistList = new ArrayList<>();
                when(artist1.getTitle()).thenReturn("Test Artist 1");
                artistList.add(artist1);

                // Set the artists field to null
                setStaticField(Library.class, "artists", null);

                // Mock getArtists() to set up the artists list and return it
                mockedLibrary.when(Library::getArtists).then(invocation -> {
                    setStaticField(Library.class, "artists", artistList);
                    return javafx.collections.FXCollections.observableArrayList(artistList);
                });

                mockedLibrary.when(() -> Library.getArtist("Test Artist 1")).thenCallRealMethod();
                Artist result = Library.getArtist("Test Artist 1");

                assertNotNull(result);
                assertEquals("Test Artist 1", result.getTitle());

                mockedLibrary.verify(Library::getArtists);
            } finally {
                // Reset the static field
                setStaticField(Library.class, "artists", null);
            }
        }

        @Test
        void testArtistsIsNotNull() throws Exception {
            try (MockedStatic<Library> mockedLibrary = mockStatic(Library.class)) {
                ArrayList<Artist> artistList = new ArrayList<>();
                when(artist1.getTitle()).thenReturn("Test Artist 1");
                artistList.add(artist1);
                setStaticField(Library.class, "artists", artistList);

                mockedLibrary.when(() -> Library.getArtist("Test Artist 1")).thenCallRealMethod();
                Artist result = Library.getArtist("Test Artist 1");

                assertNotNull(result);
                assertEquals("Test Artist 1", result.getTitle());

                // Verify getArtists() was not called
                mockedLibrary.verify(Library::getArtists, Mockito.never());
            } finally {
                // Reset the static field
                setStaticField(Library.class, "artists", null);
            }
        }

        // Helper method to set static field values using reflection
        private void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class UpdateArtistsListTest {

        @Mock
        private Album mockAlbum1;

        @Mock
        private Album mockAlbum2;

        @Mock
        private Album mockAlbum3;

        @BeforeEach
        void setUp() throws Exception {
            resetLibraryStaticFields();
        }

        @AfterEach
        void tearDown() throws Exception {
            resetLibraryStaticFields();
        }

        @Test
        void testUpdateArtistsList_WithEmptyAlbumsList() throws Exception {
            // Setup empty albums list
            ArrayList<Album> emptyAlbums = new ArrayList<>();
            setStaticField("albums", emptyAlbums);

            invokeUpdateArtistsList();

            ArrayList<Artist> artists = (ArrayList<Artist>) getStaticField("artists");
            assertNotNull(artists);
            assertTrue(artists.isEmpty());
        }

        @Test
        void testUpdateArtistsList_WithAlbumsHavingNullArtist() throws Exception {
            // Setup albums list with null artist
            ArrayList<Album> albumsList = new ArrayList<>();
            when(mockAlbum1.getArtist()).thenReturn(null);
            albumsList.add(mockAlbum1);
            setStaticField("albums", albumsList);

            invokeUpdateArtistsList();

            ArrayList<Artist> artists = (ArrayList<Artist>) getStaticField("artists");
            assertNotNull(artists);
            assertTrue(artists.isEmpty());
            verify(mockAlbum1, atLeastOnce()).getArtist();
        }

        @Test
        void testUpdateArtistsList_WithValidAlbums() throws Exception {
            // Setup albums list with valid artists
            ArrayList<Album> albumsList = new ArrayList<>();

            when(mockAlbum1.getArtist()).thenReturn("Artist 1");
            when(mockAlbum2.getArtist()).thenReturn("Artist 1");
            when(mockAlbum3.getArtist()).thenReturn("Artist 2");

            albumsList.add(mockAlbum1);
            albumsList.add(mockAlbum2);
            albumsList.add(mockAlbum3);

            setStaticField("albums", albumsList);

            try (MockedConstruction<Artist> artistMock = mockConstruction(Artist.class)) {
                // Call updateArtistsList method using reflection
                invokeUpdateArtistsList();

                ArrayList<Artist> artists = (ArrayList<Artist>) getStaticField("artists");
                assertNotNull(artists);
                assertEquals(2, artists.size());

                verify(mockAlbum1, atLeastOnce()).getArtist();
                verify(mockAlbum2, atLeastOnce()).getArtist();
                verify(mockAlbum3, atLeastOnce()).getArtist();

                assertEquals(2, artistMock.constructed().size());
            }
        }

        @Test
        void testUpdateArtistsList_WithMixedNullAndValidArtists() throws Exception {
            // Setup albums list with a mix of null and valid artists
            ArrayList<Album> albumsList = new ArrayList<>();

            when(mockAlbum1.getArtist()).thenReturn(null);
            when(mockAlbum2.getArtist()).thenReturn("Artist 1");
            when(mockAlbum3.getArtist()).thenReturn("Artist 2");

            albumsList.add(mockAlbum1);
            albumsList.add(mockAlbum2);
            albumsList.add(mockAlbum3);

            setStaticField("albums", albumsList);

            try (MockedConstruction<Artist> artistMock = mockConstruction(Artist.class)) {
                invokeUpdateArtistsList();

                ArrayList<Artist> artists = (ArrayList<Artist>) getStaticField("artists");
                assertNotNull(artists);
                assertEquals(2, artists.size());

                verify(mockAlbum1, atLeastOnce()).getArtist();
                verify(mockAlbum2, atLeastOnce()).getArtist();
                verify(mockAlbum3, atLeastOnce()).getArtist();

                assertEquals(2, artistMock.constructed().size());
            }
        }

        // Helper methods to access and modify static fields using reflection
        private void invokeUpdateArtistsList() throws Exception {
            Method method = Library.class.getDeclaredMethod("updateArtistsList");
            method.setAccessible(true);
            method.invoke(null);
        }

        private void resetLibraryStaticFields() throws Exception {
            resetStaticField("artists");
            resetStaticField("albums");
        }

        private void resetStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        }

        private void setStaticField(String fieldName, Object value) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        }

        private Object getStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class AddPlaylistTest {

        @TempDir
        Path tempDir;

        private String originalJarPath;
        private File xmlFile;

        @BeforeEach
        void setUp() throws Exception {
            // Save original JAR path to restore after test
            originalJarPath = Resources.JAR;
            Resources.JAR = tempDir.toString() + File.separator;

            // Create a basic library.xml file with empty playlists element
            xmlFile = new File(tempDir.resolve("library.xml").toString());
            String basicXml =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<library>\n" +
                            "    <musicLibrary>\n" +
                            "        <path>/music/path</path>\n" +
                            "        <fileNum>0</fileNum>\n" +
                            "        <lastId>0</lastId>\n" +
                            "    </musicLibrary>\n" +
                            "    <songs>\n" +
                            "    </songs>\n" +
                            "    <playlists>\n" +
                            "    </playlists>\n" +
                            "    <nowPlayingList>\n" +
                            "    </nowPlayingList>\n" +
                            "</library>";

            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write(basicXml);
            }

            // Initialize playlists field
            setStaticField("playlists", new ArrayList<>());
        }

        @AfterEach
        void tearDown() {
            // Restore original JAR path
            Resources.JAR = originalJarPath;

            // Reset playlists field
            try {
                setStaticField("playlists", null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Test
        void testAddPlaylist() throws Exception {
            ArrayList<Playlist> playlists = (ArrayList<Playlist>) getStaticField("playlists");
            Playlist playlist1 = new Playlist(0, "Playlist 1", new ArrayList<>());
            Playlist playlist2 = new Playlist(1, "Playlist 2", new ArrayList<>());
            playlists.add(playlist1);
            playlists.add(playlist2);

            Library.addPlaylist("New Test Playlist");

            // wait for completion
            int maxWaitMs = 2000;
            int waited = 0;
            while(waited < maxWaitMs) {
                Thread.sleep(100);
                waited += 100;

                // check if the playlist was added
                if (playlists.size() > 2) {
                    break;
                }
            }

            // Verify the playlist was added to the list
            assertEquals(3, playlists.size());
            Playlist addedPlaylist = playlists.get(2);
            assertEquals("New Test Playlist", addedPlaylist.getTitle());
            assertEquals(0, addedPlaylist.getId()); // Should be playlists.size() - 2

            // Verify the XML file was updated
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            NodeList playlistNodes = doc.getElementsByTagName("playlist");

            assertEquals(1, playlistNodes.getLength());
            Element playlistElement = (Element) playlistNodes.item(0);
            assertEquals("0", playlistElement.getAttribute("id"));
            assertEquals("New Test Playlist", playlistElement.getAttribute("title"));
        }

        @Test
        void testAddPlaylistWithXmlException() throws Exception {
            // Create an invalid XML file to cause an exception during XML processing
            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write("This is not valid XML");
            }

            ArrayList<Playlist> playlists = (ArrayList<Playlist>) getStaticField("playlists");
            Playlist playlist1 = new Playlist(0, "Playlist 1", new ArrayList<>());
            Playlist playlist2 = new Playlist(1, "Playlist 2", new ArrayList<>());
            playlists.add(playlist1);
            playlists.add(playlist2);

            Library.addPlaylist("Exception Test Playlist");

            // wait for completion
            Thread.sleep(500);

            // Verify the playlist was still added to the list
            assertEquals(3, playlists.size());
            Playlist addedPlaylist = playlists.get(2);
            assertEquals("Exception Test Playlist", addedPlaylist.getTitle());
            assertEquals(0, addedPlaylist.getId());
        }

        // Helper methods to access and modify static fields using reflection
        private void setStaticField(String fieldName, Object value) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        }

        private Object getStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class RemovePlaylistTest {

        @BeforeEach
        void setUp() throws Exception {
            // Reset playlists static field before each test
            resetStaticField("playlists");
        }

        @AfterEach
        void tearDown() throws Exception {
            // Reset playlists static field after each test
            resetStaticField("playlists");
        }

        @Test
        void testRemovePlaylist() throws Exception {
            // Create a list of playlists
            ArrayList<Playlist> playlists = new ArrayList<>();
            Playlist playlist1 = new Playlist(0, "Playlist 1", new ArrayList<>());
            Playlist playlist2 = new Playlist(1, "Playlist 2", new ArrayList<>());
            Playlist playlist3 = new Playlist(2, "Playlist 3", new ArrayList<>());

            playlists.add(playlist1);
            playlists.add(playlist2);
            playlists.add(playlist3);

            setStaticField("playlists", playlists);

            Library.removePlaylist(playlist2);

            // Verify the playlist was removed
            ArrayList<Playlist> updatedPlaylists = (ArrayList<Playlist>) getStaticField("playlists");
            assertEquals(2, updatedPlaylists.size());
            assertTrue(updatedPlaylists.contains(playlist1));
            assertFalse(updatedPlaylists.contains(playlist2));
            assertTrue(updatedPlaylists.contains(playlist3));
        }

        @Test
        void testRemoveNonExistentPlaylist() throws Exception {
            ArrayList<Playlist> playlists = new ArrayList<>();
            Playlist playlist1 = new Playlist(0, "Playlist 1", new ArrayList<>());
            Playlist playlist2 = new Playlist(1, "Playlist 2", new ArrayList<>());

            playlists.add(playlist1);
            playlists.add(playlist2);

            setStaticField("playlists", playlists);

            // Create a playlist that isn't in the list
            Playlist nonExistentPlaylist = new Playlist(3, "Non Existent", new ArrayList<>());

            Library.removePlaylist(nonExistentPlaylist);

            // Verify the playlists list remains unchanged
            ArrayList<Playlist> updatedPlaylists = (ArrayList<Playlist>) getStaticField("playlists");
            assertEquals(2, updatedPlaylists.size());
            assertTrue(updatedPlaylists.contains(playlist1));
            assertTrue(updatedPlaylists.contains(playlist2));
        }

        @Test
        void testRemovePlaylistFromEmptyList() throws Exception {
            ArrayList<Playlist> playlists = new ArrayList<>();
            setStaticField("playlists", playlists);

            // Create a playlist to remove
            Playlist playlist = new Playlist(0, "Test Playlist", new ArrayList<>());

            Library.removePlaylist(playlist);

            // Verify the playlists list remains empty
            ArrayList<Playlist> updatedPlaylists = (ArrayList<Playlist>) getStaticField("playlists");
            assertEquals(0, updatedPlaylists.size());
        }

        // Helper methods to access and modify static fields using reflection
        private void resetStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        }

        private void setStaticField(String fieldName, Object value) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        }

        private Object getStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        }
    }

    @Nested
    class GetPlaylistsTest {

        @TempDir
        Path tempDir;

        private String originalJarPath;
        private File xmlFile;

        @BeforeEach
        void setUp() throws Exception {
            // Save original JAR path
            originalJarPath = Resources.JAR;
            // Set Resources.JAR to point to temp directory
            Resources.JAR = tempDir.toString() + File.separator;
            // Create library.xml file path
            xmlFile = new File(tempDir.resolve("library.xml").toString());

            // Reset the playlists static field before each test
            resetStaticField("playlists");
        }

        @AfterEach
        void tearDown() {
            // Restore original JAR path
            Resources.JAR = originalJarPath;
            // Reset playlists to null
            try {
                resetStaticField("playlists");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Test
        void testGetPlaylistsWhenPlaylistsIsNull() throws Exception {
            assertNull(getStaticField("playlists"));

            // Create a valid XML with playlists
            String xmlContent =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<library>\n" +
                            "    <playlists>\n" +
                            "        <playlist id=\"0\" title=\"My Playlist\">\n" +
                            "            <songId>0</songId>\n" +
                            "            <songId>1</songId>\n" +
                            "        </playlist>\n" +
                            "        <playlist id=\"1\" title=\"Another Playlist\">\n" +
                            "            <songId>2</songId>\n" +
                            "        </playlist>\n" +
                            "    </playlists>\n" +
                            "</library>";

            // Setup mock for getSong method to return song objects
            try (MockedStatic<Library> mockedLibrary = mockStatic(Library.class)) {
                Song song0 = new Song(0, "Song 1", "Artist 1", "Album 1",
                        Duration.ofSeconds(180), 1, 1, 0, LocalDateTime.now(), "/path/song1.mp3");
                Song song1 = new Song(1, "Song 2", "Artist 1", "Album 1",
                        Duration.ofSeconds(200), 2, 1, 0, LocalDateTime.now(), "/path/song2.mp3");
                Song song2 = new Song(2, "Song 3", "Artist 2", "Album 2",
                        Duration.ofSeconds(220), 1, 1, 0, LocalDateTime.now(), "/path/song3.mp3");

                // Mock getSong
                mockedLibrary.when(() -> Library.getSong(String.valueOf(0))).thenReturn(song0);
                mockedLibrary.when(() -> Library.getSong(String.valueOf(1))).thenReturn(song1);
                mockedLibrary.when(() -> Library.getSong(String.valueOf(2))).thenReturn(song2);

                mockedLibrary.when(Library::getPlaylists).thenCallRealMethod();

                try (FileWriter writer = new FileWriter(xmlFile)) {
                    writer.write(xmlContent);
                }

                ObservableList<Playlist> playlists = Library.getPlaylists();

                // Verify results
                assertEquals(4, playlists.size());
                assertEquals("Another Playlist", playlists.get(0).getTitle());
                assertEquals("My Playlist", playlists.get(1).getTitle());
                assertEquals(-2, playlists.get(2).getId());
                assertEquals(-1, playlists.get(3).getId());
                assertEquals(1, playlists.get(0).getSongs().size());
                assertEquals(2, playlists.get(1).getSongs().size());
                assertNotNull(getStaticField("playlists"));
            }
        }

        @Test
        void testGetPlaylistsWhenPlaylistsIsNotNull() throws Exception {
            ArrayList<Playlist> existingPlaylists = new ArrayList<>();
            existingPlaylists.add(new Playlist(1, "Existing Playlist", new ArrayList<>()));

            // Use reflection to create instances of special playlists
            Playlist recentlyPlayedPlaylist = createPlaylistViaReflection("app.musicplayer.model.RecentlyPlayedPlaylist", -1);
            Playlist mostPlayedPlaylist = createPlaylistViaReflection("app.musicplayer.model.MostPlayedPlaylist", -2);

            existingPlaylists.add(recentlyPlayedPlaylist);
            existingPlaylists.add(mostPlayedPlaylist);

            setStaticField("playlists", existingPlaylists);

            ObservableList<Playlist> playlists = Library.getPlaylists();

            // Verify results
            assertEquals(3, playlists.size());
            assertEquals("Existing Playlist", playlists.get(0).getTitle());
            assertEquals(-1, playlists.get(1).getId());
            assertEquals(-2, playlists.get(2).getId());
        }

        @Test
        void testGetPlaylistsWithInvalidXml() throws Exception {
            assertNull(getStaticField("playlists"));

            // Create invalid XML content
            String invalidXml = "This is not valid XML content";

            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write(invalidXml);
            }

            ObservableList<Playlist> playlists = Library.getPlaylists();

            // Should still get the two default playlists
            assertEquals(2, playlists.size());
            assertEquals(-2, playlists.get(0).getId());
            assertEquals(-1, playlists.get(1).getId());
        }

        @Test
        void testGetPlaylistsWithMissingXmlFile() throws Exception {
            assertNull(getStaticField("playlists"));

            // Do not create the XML file
            assertFalse(xmlFile.exists());

            ObservableList<Playlist> playlists = Library.getPlaylists();

            assertEquals(2, playlists.size());
            assertEquals(-2, playlists.get(0).getId());
            assertEquals(-1, playlists.get(1).getId());
        }

        // Helper method to create instances of package-private classes via reflection
        private Playlist createPlaylistViaReflection(String className, int id) throws Exception {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getDeclaredConstructor(int.class);
            constructor.setAccessible(true);
            return (Playlist) constructor.newInstance(id);
        }

        // Helper methods to set and reset static fields
        private void resetStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        }

        private void setStaticField(String fieldName, Object value) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        }

        private Object getStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        }
    }

    @Nested
    class GetPlaylistByIdTest {

        @Mock
        private Playlist mockPlaylist1;

        @Mock
        private Playlist mockPlaylist2;

        @Mock
        private Playlist mockPlaylist3;

        @Mock
        private Playlist mockMostPlayed;

        @Mock
        private Playlist mockRecentlyPlayed;

        private ArrayList<Playlist> mockPlaylists;

        @BeforeEach
        void setUp() throws Exception {
            // Initialize mocks
            MockitoAnnotations.openMocks(this);

            // Reset the playlists field before each test
            resetStaticField("playlists");

            // Setup mock playlists list
            mockPlaylists = new ArrayList<>();
            // Add playlists in the correct order for the Library class's sorting logic
            mockPlaylists.add(mockPlaylist3);  // ID 0
            mockPlaylists.add(mockPlaylist2);  // ID 1
            mockPlaylists.add(mockPlaylist1);  // ID 2
            mockPlaylists.add(mockRecentlyPlayed); // ID -1
            mockPlaylists.add(mockMostPlayed);     // ID -2
        }

        @AfterEach
        void tearDown() throws Exception {
            // Clean up after each test
            resetStaticField("playlists");
        }

        @Test
        void testPlaylistsIsNull() throws Exception {
            try (MockedStatic<Library> mockedLibrary = Mockito.mockStatic(Library.class)) {
                setStaticField(Library.class, "playlists", null);

                // Mock getPlaylists() to return our mock list and set the static field
                mockedLibrary.when(Library::getPlaylists).thenAnswer(invocation -> {
                    setStaticField(Library.class, "playlists", mockPlaylists);
                    return FXCollections.observableArrayList(mockPlaylists);
                });

                mockedLibrary.when(() -> Library.getPlaylist(1)).thenCallRealMethod();
                // Call the method with id=1
                Playlist result = Library.getPlaylist(1);
                assertEquals(mockPlaylist2, result);
            }
        }

        @Test
        void testPlaylistsIsNotNull() throws Exception {
            try (MockedStatic<Library> mockedLibrary = Mockito.mockStatic(Library.class)) {
                setStaticField(Library.class, "playlists", mockPlaylists);
                mockedLibrary.when(Library::getPlaylists).thenReturn(FXCollections.observableArrayList(mockPlaylists));

                mockedLibrary.when(() -> Library.getPlaylist(0)).thenCallRealMethod();

                // Call the method with id=0
                Playlist result = Library.getPlaylist(0);
                assertEquals(mockPlaylist1, result);
            }
        }

        @Test
        void testGetPlaylistById_CalculatesCorrectIndex() throws Exception {
            try (MockedStatic<Library> mockedLibrary = Mockito.mockStatic(Library.class)) {
                setStaticField(Library.class, "playlists", mockPlaylists);
                mockedLibrary.when(Library::getPlaylists).thenReturn(FXCollections.observableArrayList(mockPlaylists));
                mockedLibrary.when(() -> Library.getPlaylist(anyInt())).thenCallRealMethod();

                assertEquals(mockPlaylist3, Library.getPlaylist(2));
                assertEquals(mockPlaylist2, Library.getPlaylist(1));
                assertEquals(mockPlaylist1, Library.getPlaylist(0));
            }
        }

        // Helper method to reset static field values using reflection
        private void resetStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        }

        // Helper method to set static field values using reflection
        private void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        }
    }

    @Nested
    class GetPlaylistByTitleTest {

        @BeforeEach
        void setUp() throws Exception {
            resetStaticField("playlists");
        }

        @AfterEach
        void tearDown() throws Exception {
            resetStaticField("playlists");
        }

        @Test
        void testPlaylistsIsNull() throws Exception {
            try (MockedStatic<Library> mockedLibrary = mockStatic(Library.class)) {
                // Set the playlists field to null
                setStaticField(Library.class, "playlists", null);

                // Create mock playlists
                ArrayList<Playlist> playlistList = new ArrayList<>();
                Playlist mockPlaylist = mock(Playlist.class);
                when(mockPlaylist.getTitle()).thenReturn("Test Playlist");
                playlistList.add(mockPlaylist);

                // Mock getPlaylists()
                mockedLibrary.when(Library::getPlaylists).then(invocation -> {
                    setStaticField(Library.class, "playlists", playlistList);
                    return FXCollections.observableArrayList(playlistList);
                });

                mockedLibrary.when(() -> Library.getPlaylist("Test Playlist")).thenCallRealMethod();
                Playlist result = Library.getPlaylist("Test Playlist");

                // Verify the result
                assertNotNull(result);
                assertEquals("Test Playlist", result.getTitle());
                mockedLibrary.verify(Library::getPlaylists);
            } finally {
                // Reset the static field
                setStaticField(Library.class, "playlists", null);
            }
        }

        @Test
        void testPlaylistsIsNotNull() throws Exception {
            try (MockedStatic<Library> mockedLibrary = mockStatic(Library.class)) {
                ArrayList<Playlist> playlistList = new ArrayList<>();
                Playlist mockPlaylist = mock(Playlist.class);
                when(mockPlaylist.getTitle()).thenReturn("Test Playlist");
                playlistList.add(mockPlaylist);

                setStaticField(Library.class, "playlists", playlistList);

                mockedLibrary.when(() -> Library.getPlaylist("Test Playlist")).thenCallRealMethod();
                Playlist result = Library.getPlaylist("Test Playlist");

                // Verify the result
                assertNotNull(result);
                assertEquals("Test Playlist", result.getTitle());
                mockedLibrary.verify(Library::getPlaylists, Mockito.never());
            } finally {
                // Reset the static field
                setStaticField(Library.class, "playlists", null);
            }
        }

        // Helper method to set static field values using reflection
        private void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        }

        // Helper method to reset static fields
        private void resetStaticField(String fieldName) throws Exception {
            Field field = Library.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        }
    }

    @Nested
    class LoadPlayingListTest {

        @TempDir
        Path tempDir;

        private String originalJarPath;
        private File xmlFile;

        @BeforeEach
        void setUp() throws Exception {
            // Save original JAR path
            originalJarPath = Resources.JAR;
            // Set Resources.JAR to point to temp directory
            Resources.JAR = tempDir.toString() + File.separator;
            // Create library.xml file path
            xmlFile = new File(tempDir.resolve("library.xml").toString());
        }

        @AfterEach
        void tearDown() {
            // Restore original JAR path
            Resources.JAR = originalJarPath;
        }

        @Test
        void testLoadPlayingListWithValidXml() throws Exception {
            Song mockSong1 = mock(Song.class);
            Song mockSong2 = mock(Song.class);

            try (MockedStatic<Library> mockedLibrary = mockStatic(Library.class)) {
                Method getSongMethod = Library.class.getDeclaredMethod("getSong", int.class);
                getSongMethod.setAccessible(true);
                mockedLibrary.when(() -> getSongMethod.invoke(null, 0)).thenReturn(mockSong1);
                mockedLibrary.when(() -> getSongMethod.invoke(null, 1)).thenReturn(mockSong2);

                mockedLibrary.when(Library::loadPlayingList).thenCallRealMethod();

                // Create sample XML content with nowPlayingList
                String xmlContent =
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<library>\n" +
                                "    <nowPlayingList>\n" +
                                "        <id>0</id>\n" +
                                "        <id>1</id>\n" +
                                "    </nowPlayingList>\n" +
                                "</library>";

                try (FileWriter writer = new FileWriter(xmlFile)) {
                    writer.write(xmlContent);
                }

                ArrayList<Song> result = Library.loadPlayingList();

                // Verify the results
                assertEquals(2, result.size());
                assertEquals(mockSong1, result.get(0));
                assertEquals(mockSong2, result.get(1));
                mockedLibrary.verify(() -> getSongMethod.invoke(null, 0));
                mockedLibrary.verify(() -> getSongMethod.invoke(null, 1));
            }
        }

        @Test
        void testLoadPlayingListWithEmptyPlayingList() throws Exception {
            // Create XML with empty nowPlayingList
            String xmlContent =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<library>\n" +
                            "    <nowPlayingList>\n" +
                            "    </nowPlayingList>\n" +
                            "</library>";

            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write(xmlContent);
            }

            ArrayList<Song> result = Library.loadPlayingList();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void testLoadPlayingListWithNonIdElements() throws Exception {
            // Create XML with non-id elements
            String xmlContent =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<library>\n" +
                            "    <nowPlayingList>\n" +
                            "        <notId>0</notId>\n" +
                            "        <someOtherElement>1</someOtherElement>\n" +
                            "    </nowPlayingList>\n" +
                            "</library>";

            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write(xmlContent);
            }

            ArrayList<Song> result = Library.loadPlayingList();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void testLoadPlayingListWithNoNowPlayingListElement() throws Exception {
            // Create XML without nowPlayingList
            String xmlContent =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<library>\n" +
                            "    <songs>\n" +
                            "        <song>\n" +
                            "            <id>0</id>\n" +
                            "            <title>Test Song</title>\n" +
                            "        </song>\n" +
                            "    </songs>\n" +
                            "</library>";

            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write(xmlContent);
            }

            ArrayList<Song> result = Library.loadPlayingList();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void testLoadPlayingListWithMissingFile() {
            // Don't create the file
            assertFalse(xmlFile.exists());
            ArrayList<Song> result = Library.loadPlayingList();
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void testLoadPlayingListWithInvalidXml() throws Exception {
            // Create invalid XML
            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write("This is not valid XML");
            }
            ArrayList<Song> result = Library.loadPlayingList();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class SavePlayingListTest {

        @TempDir
        Path tempDir;

        private String originalJarPath;
        private File xmlFile;

        @BeforeEach
        void setUp() throws Exception {
            // Save original JAR path to restore after test
            originalJarPath = Resources.JAR;
            Resources.JAR = tempDir.toString() + File.separator;

            // Create a basic library.xml file with empty nowPlayingList
            xmlFile = new File(tempDir.resolve("library.xml").toString());
            String basicXml =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<library>\n" +
                            "    <musicLibrary>\n" +
                            "        <path>/music/path</path>\n" +
                            "        <fileNum>0</fileNum>\n" +
                            "        <lastId>0</lastId>\n" +
                            "    </musicLibrary>\n" +
                            "    <songs>\n" +
                            "    </songs>\n" +
                            "    <playlists>\n" +
                            "    </playlists>\n" +
                            "    <nowPlayingList>\n" +
                            "    </nowPlayingList>\n" +
                            "</library>";

            try (FileWriter writer = new FileWriter(xmlFile)) {
                writer.write(basicXml);
            }
        }

        @AfterEach
        void tearDown() {
            // Restore original JAR path
            Resources.JAR = originalJarPath;
        }


        @Test
        void testSavePlayingListWithSongs() throws Exception {
            try (MockedStatic<MusicPlayer> musicPlayerMock = Mockito.mockStatic(MusicPlayer.class)) {
                ArrayList<Song> songsList = new ArrayList<>();
                Song song1 = new Song(1, "Test Song 1", "Test Artist", "Test Album",
                        Duration.ofSeconds(180), 1, 1, 0,
                        LocalDateTime.now(), "/path/to/song1");
                Song song2 = new Song(2, "Test Song 2", "Test Artist", "Test Album",
                        Duration.ofSeconds(180), 2, 1, 0,
                        LocalDateTime.now(), "/path/to/song2");
                songsList.add(song1);
                songsList.add(song2);

                // Mock MusicPlayer.getNowPlayingList()
                musicPlayerMock.when(MusicPlayer::getNowPlayingList).thenReturn(songsList);

                Library.savePlayingList();

                // No assertions - we're just verifying that the method doesn't throw any exceptions
            }
        }

        @Test
        void testSavePlayingListWithEmptyList() throws Exception {
            try (MockedStatic<MusicPlayer> musicPlayerMock = Mockito.mockStatic(MusicPlayer.class)) {
                // Mock MusicPlayer.getNowPlayingList()
                ArrayList<Song> emptyList = new ArrayList<>();
                musicPlayerMock.when(MusicPlayer::getNowPlayingList).thenReturn(emptyList);

                Library.savePlayingList();

                // No assertions - we're just verifying that the method doesn't throw any exceptions
            }
        }

        @Test
        void testSavePlayingListWithException() throws Exception {
            try (MockedStatic<MusicPlayer> musicPlayerMock = Mockito.mockStatic(MusicPlayer.class)) {
                // Create an invalid XML file that will cause an exception
                try (FileWriter writer = new FileWriter(xmlFile)) {
                    writer.write("This is not valid XML");
                }

                ArrayList<Song> songsList = new ArrayList<>();
                Song song = new Song(1, "Test Song", "Test Artist", "Test Album",
                        Duration.ofSeconds(180), 1, 1, 0,
                        LocalDateTime.now(), "/path/to/song");
                songsList.add(song);

                // Mock MusicPlayer.getNowPlayingList()
                musicPlayerMock.when(MusicPlayer::getNowPlayingList).thenReturn(songsList);

                Library.savePlayingList();

                // No assertions - we're just verifying that exceptions are properly handled
            }
        }
    }
}