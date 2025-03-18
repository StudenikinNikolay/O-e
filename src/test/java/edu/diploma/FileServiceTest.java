package edu.diploma;

import edu.diploma.auth.JwtHelper;
import edu.diploma.service.FileService;
import edu.diploma.model.AppError;
import edu.diploma.model.File;
import edu.diploma.model.FileContent;
import edu.diploma.model.NewFilename;
import edu.diploma.repository.FileContentRepository;
import edu.diploma.repository.FileRepository;
import io.vavr.control.Either;
import org.javatuples.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FileServiceTest {

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

    private FileService fileService;

    @BeforeEach
    public void setUp() {

    }

    @Test
    public void testRenameFile400WhenNewFilenameNull() {

        final String filename = "A File.txt";
        final NewFilename newFilename = null;

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.renameFile(filename, newFilename);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(400));
    }

    @Test
    public void testRenameFile400WhenNoFileToRename() {

        final String filename = "Sample File.txt";
        final NewFilename newFilename = new NewFilename("A New File.txt");

        fileRepository = mock(FileRepository.class);
        when(fileRepository.findOne(any(Example.class))).thenReturn(Optional.empty());

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.renameFile(filename, newFilename);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(400));
    }

    @Test
    public void testRenameFile400WhenFilenameNull() {

        final String filename = null;
        final NewFilename newFilename = new NewFilename("A New File.txt");

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.renameFile(filename, newFilename);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(400));
    }

    @Test
    public void testRenameFile500WhenSave() {

        final String filename = "A File.txt";
        final NewFilename newFilename = new NewFilename("A New File.txt");
        final File file = new File(filename);

        fileRepository = mock(FileRepository.class);
        when(fileRepository.findOne(any(Example.class))).thenReturn(Optional.of(file));
        when(fileRepository.save(any(File.class))).thenThrow(new RuntimeException("DB Error"));

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.renameFile(filename, newFilename);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(500));
    }

    @Test
    public void testRenameFileOk() {

        final String filename = "A File.txt";
        final NewFilename newFilename = new NewFilename("A New File.txt");
        final File file = new File(filename);

        fileRepository = mock(FileRepository.class);
        when(fileRepository.findOne(any(Example.class))).thenReturn(Optional.of(file));
        when(fileRepository.save(file)).thenReturn(file);

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.renameFile(filename, newFilename);

        assertThat(response.isRight(), is(true));
        assertThat(response.get().getName(), is(newFilename.getFilename()));
    }

    @Test
    public void testGetFile400WhenFilenameNull() throws Exception {

        final String filename = null;

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,Pair<File,FileContent>> response = fileService.getFile(filename);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(400));
    }

    @Test
    public void testGetFile500WhenNoFileCotent() throws Exception {

        final String filename = "A File.txt";
        final File file = new File(filename);

        fileRepository = mock(FileRepository.class);
        when(fileRepository.findOne(any(Example.class))).thenReturn(Optional.of(file));

        fileContentRepository = mock(FileContentRepository.class);
        when(fileContentRepository.findOne(any(Example.class))).thenReturn(Optional.empty());

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,Pair<File,FileContent>> response = fileService.getFile(filename);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(500));
    }

    @Test
    public void testGetFile500WhenNoFile() throws Exception {

        final String filename = "A File.txt";
        final String contents = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
        final File file = new File(filename);

        fileRepository = mock(FileRepository.class);
        when(fileRepository.findOne(any(Example.class))).thenReturn(Optional.empty());

        FileContent fileContent = new FileContent(file, contents.getBytes(StandardCharsets.UTF_8));

        fileContentRepository = mock(FileContentRepository.class);
        when(fileContentRepository.findOne(any(Example.class))).thenReturn(Optional.of(fileContent));

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,Pair<File,FileContent>> response = fileService.getFile(filename);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(500));
    }

    @Test
    public void testGetFileOk() throws Exception {

        final String filename = "A File.txt";
        final String contents = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
        final File file = new File(filename);

        fileRepository = mock(FileRepository.class);
        when(fileRepository.findOne(any(Example.class))).thenReturn(Optional.of(file));

        FileContent fileContent = new FileContent(file, contents.getBytes(StandardCharsets.UTF_8));

        fileContentRepository = mock(FileContentRepository.class);
        when(fileContentRepository.findOne(any(Example.class))).thenReturn(Optional.of(fileContent));

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError, Pair<File,FileContent>> response = fileService.getFile(filename);

        assertThat(response.isRight(), is(true));
        assertThat(
                response.get().getValue1().getContent(),
                is(fileContent.getContent())
        );
    }

    @Test
    public void testDeleteFile400WhenFilenameNull() {

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        final String filename = null;

        Either<AppError,List<File>> response = fileService.deleteFile(filename);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(400));
    }

    @Test
    public void testDeleteFile500WhenDbErrors() {
        fileRepository = mock(FileRepository.class);

        when(fileRepository.findAll(any(Example.class))).thenReturn(List.of(files.get(0)));
        doThrow(new RuntimeException("DB Error")).when(fileRepository).deleteAll(any(List.class));

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError, List<File>> response = fileService.deleteFile(files.get(0).getName());

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(500));
    }

    @Test
    public void testDeleteFileOk() {
        fileRepository = mock(FileRepository.class);

        when(fileRepository.findAll(any(Example.class))).thenReturn(List.of(files.get(0)));
        doNothing().when(fileRepository).deleteAll(List.of(files.get(0)));

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError, List<File>> response = fileService.deleteFile(files.get(0).getName());

        assertThat(response.isRight(), is(true));
    }

    @Test
    public void testPostFile400WhenDbExceptionOnSaveContent() throws Exception {

        MultipartFile content = mock(MultipartFile.class);
        when(content.getContentType()).thenReturn(files.get(0).getContentType());
        when(content.getSize()).thenReturn(files.get(0).getSize());
        when(content.getBytes()).thenReturn(new byte[0]);

        fileRepository = mock(FileRepository.class);
        when(fileRepository.save(ArgumentMatchers.any(File.class))).thenReturn(files.get(0));

        FileContent fileContent = new FileContent(files.get(0), content.getBytes());

        fileContentRepository = mock(FileContentRepository.class);
        when(fileContentRepository.save(fileContent))
                .thenThrow(new RuntimeException("DB Error"));

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.saveFile(files.get(0).getName(), content);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(400));
    }

    @Test
    public void testPostFile400WhenDbExceptionOnSave() throws Exception {

        MultipartFile content = mock(MultipartFile.class);
        when(content.getContentType()).thenReturn(files.get(0).getContentType());
        when(content.getSize()).thenReturn(files.get(0).getSize());
        when(content.getBytes()).thenReturn(new byte[0]);

        fileRepository = mock(FileRepository.class);
        when(fileRepository.save(ArgumentMatchers.any(File.class)))
                .thenThrow(new RuntimeException("DB Error"));

        FileContent fileContent = new FileContent(files.get(0), content.getBytes());

        fileContentRepository = mock(FileContentRepository.class);
        when(fileContentRepository.save(fileContent)).thenReturn(fileContent);

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.saveFile(files.get(0).getName(), content);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(400));
    }

    @Test
    public void testPostFile400WhenNullContent() throws Exception {

        final String filename = "File One";
        final MultipartFile content = null;

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.saveFile(filename, content);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(400));
    }

    @Test
    public void testPostFile400WhenBlankFilename() throws Exception {

        MultipartFile content = mock(MultipartFile.class);

        final String filename = "   \t\n     ";

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.saveFile(filename, content);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(400));
    }

    @Test
    public void testPostFile400WhenEmptyFilename() throws Exception {

        MultipartFile content = mock(MultipartFile.class);

        final String filename = "";

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.saveFile(filename, content);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(400));
    }

    @Test
    public void testPostFile400WhenNullFilename() throws Exception {

        MultipartFile content = mock(MultipartFile.class);

        final String filename = null;

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.saveFile(filename, content);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(400));
    }

    @Test
    public void testPostFileOk() throws Exception {

        MultipartFile content = mock(MultipartFile.class);
        when(content.getContentType()).thenReturn(files.get(0).getContentType());
        when(content.getSize()).thenReturn(files.get(0).getSize());
        when(content.getBytes()).thenReturn(new byte[0]);

        fileRepository = mock(FileRepository.class);
        when(fileRepository.save(ArgumentMatchers.any(File.class))).thenReturn(files.get(0));

        FileContent fileContent = new FileContent(files.get(0), content.getBytes());

        fileContentRepository = mock(FileContentRepository.class);
        when(fileContentRepository.save(fileContent)).thenReturn(fileContent);

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError,File> response = fileService.saveFile(files.get(0).getName(), content);

        assertThat(response.isRight(), is(true));
    }

    @Test
    public void testGetFiles500WithoutLimitOnRepositoryException() {

        final Integer limit = null;

        fileRepository = mock(FileRepository.class);
        when(fileRepository.findAll()).thenThrow(new RuntimeException("Some DB Error"));

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError, List<File>> response = fileService.getFiles(limit);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(500));
        assertThat(response.getLeft().getMessage(), is("Server Error"));
    }

    @Test
    public void testGetFiles500WithLimitOnRepositoryException() {

        Integer limit = 3;
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "name"));

        fileRepository = mock(FileRepository.class);
        when(fileRepository.findAll(pageRequest)).thenThrow(new RuntimeException("Some DB Error"));

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError, List<File>> response = fileService.getFiles(limit);

        assertThat(response.isLeft(), is(true));
        assertThat(response.getLeft().getCode(), is(500));
        assertThat(response.getLeft().getMessage(), is("Server Error"));
    }

    @Test
    public void testGetFilesOkWithNoLimit() {

        Integer limit = null;


        fileRepository = mock(FileRepository.class);
        when(fileRepository.findAll()).thenReturn(files);

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError, List<File>> response = fileService.getFiles(limit);

        assertThat(response.isRight(), is(true));
        assertThat(response.get().size(), is(files.size()));
    }

    @Test
    public void testGetFilesOkWithGreaterLimit() {

        Integer limit = 3 + 100;
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "name"));

        Page<File> page = mock(Page.class);
        when(page.stream()).thenReturn(files.stream());


        fileRepository = mock(FileRepository.class);
        when(fileRepository.findAll(pageRequest)).thenReturn(page);

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError, List<File>> response = fileService.getFiles(limit);

        assertThat(response.isRight(), is(true));
        assertThat(response.get().size(), is(files.size()));
    }

    @Test
    public void testGetFilesOkWithDefaultLimit() {

        Integer limit = 3;
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "name"));

        Page<File> page = mock(Page.class);
        when(page.stream()).thenReturn(files.subList(0,limit).stream());


        fileRepository = mock(FileRepository.class);
        when(fileRepository.findAll(pageRequest)).thenReturn(page);

        fileService = new FileService(
                authManager, jwtHelper, fileRepository, fileContentRepository
        );

        Either<AppError, List<File>> response = fileService.getFiles(limit);

        assertThat(response.isRight(), is(true));
        assertThat(response.get().size(), is(limit));
    }
}
