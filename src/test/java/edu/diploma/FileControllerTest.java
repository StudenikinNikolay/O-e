package edu.diploma;

import edu.diploma.auth.JwtHelper;
import edu.diploma.controller.FileController;
import edu.diploma.model.AppError;
import edu.diploma.model.File;
import edu.diploma.model.FileContent;
import edu.diploma.model.NewFilename;
import edu.diploma.repository.FileContentRepository;
import edu.diploma.repository.FileRepository;
import edu.diploma.service.FileService;
import io.vavr.control.Either;
import org.javatuples.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

public class FileControllerTest {

    private final List<File> files = List.of(
            new File("File One", "text/plain", 123,124,123),
            new File("File Two", "text/html", 234,245,234),
            new File("File Three", "image/jpeg", 345,456,236),
            new File("File Four", "audio/mpeg", 456,567,345)
    );

    private AuthenticationManager authManager;
    private JwtHelper jwtHelper;
    private FileRepository fileRepository;
    private FileContentRepository fileContentRepository;
    private FileController fileController;
    private FileService fileService;

    @BeforeEach
    public void setUp() {

    }

    @Test
    public void testPutFile400WhenNewFilenameNull() {

        final String filename = "A File.txt";
        final NewFilename newFilename = null;

        fileService = mock(FileService.class);
        when(fileService.renameFile(filename, newFilename)).thenReturn(Either.left(FileService.ERROR_INPUT_DATA));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.putFile(filename, newFilename);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void testPutFile400WhenFilenameNull() {

        final String filename = null;
        final NewFilename newFilename = new NewFilename("A New File.txt");

        fileService = mock(FileService.class);
        when(fileService.renameFile(filename, newFilename)).thenReturn(Either.left(FileService.ERROR_INPUT_DATA));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.putFile(filename, newFilename);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void testPutFile500WhenSaving() {

        final String filename = "A File.txt";
        final NewFilename newFilename = new NewFilename("A New File.txt");

        fileService = mock(FileService.class);
        when(fileService.renameFile(filename, newFilename)).thenReturn(Either.left(FileService.SERVER_ERROR));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.putFile(filename, newFilename);

        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void testPutFileOk() {

        final String filename = "A File.txt";
        final NewFilename newFilename = new NewFilename("A New File.txt");

        fileService = mock(FileService.class);
        when(fileService.renameFile(filename, newFilename)).thenReturn(Either.right(new File(newFilename.getFilename())));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.putFile(filename, newFilename);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testGetFile400WhenFilenameNull() throws Exception {

        final String filename = null;

        fileService = mock(FileService.class);
        when(fileService.getFile(filename)).thenReturn(Either.left(FileService.ERROR_INPUT_DATA));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.getFile(filename);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void testGetFile500WhenStreamingContentErrors() throws Exception {

        final String filename = "A File.txt";
        final File file = new File(filename);

        FileContent fileContent = mock(FileContent.class);
        when(fileContent.getContent()).thenThrow(new RuntimeException());

        fileService = mock(FileService.class);
        when(fileService.getFile(filename)).thenReturn(Either.right(Pair.with(file,fileContent)));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.getFile(filename);

        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void testGetFile500WhenReadingContentErrors() throws Exception {

        final String filename = "A File.txt";

        fileService = mock(FileService.class);
        when(fileService.getFile(filename)).thenReturn(Either.left(FileService.SERVER_ERROR));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.getFile(filename);

        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void testGetFileOk() throws Exception {

        final String filename = "A File.txt";
        final String contents = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
        final File file = new File(filename);

        FileContent fileContent = new FileContent(file, contents.getBytes(StandardCharsets.UTF_8));

        fileService = mock(FileService.class);
        when(fileService.getFile(filename)).thenReturn(Either.right(Pair.with(file,fileContent)));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.getFile(filename);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(
                ((InputStreamResource) response.getBody()).getInputStream().readAllBytes(),
                is(fileContent.getContent())
        );
    }

    @Test
    public void testDeleteFile400WhenFilenameNull() {

        final String filename = null;

        fileService = mock(FileService.class);
        when(fileService.deleteFile(filename)).thenReturn(Either.left(FileService.ERROR_INPUT_DATA));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.deleteFile(filename);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void testDeleteFile500WhenDbErrors() {

        fileService = mock(FileService.class);
        when(fileService.deleteFile(files.get(0).getName())).thenReturn(Either.left(FileService.SERVER_ERROR));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.deleteFile(files.get(0).getName());

        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    public void testDeleteFileOk() {

        fileService = mock(FileService.class);
        when(fileService.deleteFile(files.get(0).getName())).thenReturn(Either.right(List.of(files.get(0))));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.deleteFile(files.get(0).getName());

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testPostFile400WhenNullContent() throws Exception {

        final String filename = "File One";
        final MultipartFile content = null;

        fileService = mock(FileService.class);
        when(fileService.saveFile(filename, content)).thenReturn(Either.left(FileService.ERROR_INPUT_DATA));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.postFile(filename, content);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void testPostFile400WhenBlankFilename() throws Exception {

        MultipartFile content = mock(MultipartFile.class);

        final String filename = "   \t\n     ";

        fileService = mock(FileService.class);
        when(fileService.saveFile(filename, content)).thenReturn(Either.left(FileService.ERROR_INPUT_DATA));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.postFile(filename, content);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void testPostFile400WhenEmptyFilename() throws Exception {

        MultipartFile content = mock(MultipartFile.class);

        final String filename = "";

        fileService = mock(FileService.class);
        when(fileService.saveFile(filename, content)).thenReturn(Either.left(FileService.ERROR_INPUT_DATA));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.postFile(filename, content);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void testPostFile400WhenNullFilename() throws Exception {

        MultipartFile content = mock(MultipartFile.class);

        final String filename = null;

        fileService = mock(FileService.class);
        when(fileService.saveFile(filename, content)).thenReturn(Either.left(FileService.ERROR_INPUT_DATA));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.postFile(filename, content);

        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void testPostFileOk() throws Exception {

        MultipartFile content = mock(MultipartFile.class);
        when(content.getContentType()).thenReturn(files.get(0).getContentType());
        when(content.getSize()).thenReturn(files.get(0).getSize());
        when(content.getBytes()).thenReturn(new byte[0]);

        fileService = mock(FileService.class);
        when(fileService.saveFile(files.get(0).getName(),content)).thenReturn(
                Either.right(files.get(0))
        );

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.postFile(files.get(0).getName(), content);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }

    @Test
    public void testGetFiles500WithoutLimitOnRepositoryException() {

        final Integer limit = null;

        final AppError error = new AppError(500, "Server Error");

        fileService = mock(FileService.class);
        when(fileService.getFiles(limit)).thenReturn(Either.left(error));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.getFiles(limit);

        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(((AppError) response.getBody()).getCode(), is(error.getCode()));
        assertThat(((AppError) response.getBody()).getMessage(), is(error.getMessage()));
    }

    @Test
    public void testGetFiles500WithLimitOnRepositoryException() {

        Integer limit = 3;

        final AppError error = new AppError(500, "Server Error");

        fileService = mock(FileService.class);
        when(fileService.getFiles(limit)).thenReturn(Either.left(error));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.getFiles(limit);

        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(((AppError) response.getBody()).getCode(), is(error.getCode()));
        assertThat(((AppError) response.getBody()).getMessage(), is(error.getMessage()));
    }

    @Test
    public void testGetFilesOkWithNoLimit() {

        Integer limit = null;

        fileService = mock(FileService.class);
        when(fileService.getFiles(limit)).thenReturn(Either.right(files));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.getFiles(limit);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(((List<File>) response.getBody()).size(), is(files.size()));
    }

    @Test
    public void testGetFilesOkWithGreaterLimit() {

        Integer limit = 3 + 100;

        fileService = mock(FileService.class);
        when(fileService.getFiles(limit)).thenReturn(Either.right(files));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.getFiles(limit);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(((List<File>) response.getBody()).size(), is(files.size()));
    }

    @Test
    public void testGetFilesOkWithDefaultLimit() {

        Integer limit = 3;

        fileService = mock(FileService.class);
        when(fileService.getFiles(limit)).thenReturn(Either.right(files.subList(0,limit)));

        fileController = new FileController(
                authManager, jwtHelper, fileRepository, fileContentRepository, fileService
        );

        ResponseEntity<?> response = fileController.getFiles(limit);

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(((List<File>) response.getBody()).size(), is(limit));
    }
}
