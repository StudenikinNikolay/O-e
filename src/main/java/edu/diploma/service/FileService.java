package edu.diploma.service;

import edu.diploma.auth.JwtHelper;
import edu.diploma.controller.AuthController;
import edu.diploma.model.AppError;
import edu.diploma.model.File;
import edu.diploma.model.FileContent;
import edu.diploma.model.NewFilename;
import edu.diploma.repository.FileContentRepository;
import edu.diploma.repository.FileRepository;
import io.vavr.control.Either;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Service
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    public static final AppError ERROR_INPUT_DATA = new AppError(HttpStatus.BAD_REQUEST.value(), "Error input data");
    public static final AppError SERVER_ERROR = new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error");

    private final AuthenticationManager authManager;
    private final JwtHelper jwtHelper;
    private final FileRepository fileRepository;
    private final FileContentRepository fileContentRepository;

    public FileService(
            AuthenticationManager authManager,
            JwtHelper jwtHelper,
            FileRepository fileRepository,
            FileContentRepository fileContentRepository
    ) {
        this.authManager = authManager;
        this.jwtHelper = jwtHelper;
        this.fileRepository = fileRepository;
        this.fileContentRepository = fileContentRepository;
    }


    public Either<AppError, File> renameFile(String filename, NewFilename newFilename) {

        if (Objects.isNull(filename) || filename.trim().isEmpty()
                || Objects.isNull(newFilename) || newFilename.getFilename().trim().isEmpty()) {

            log.error(String.format("Error renaming file"));
            return Either.left(FileService.ERROR_INPUT_DATA);

        }

        Optional<File> file = fileRepository.findOne(Example.of(
                new File(filename),
                ExampleMatcher.matching()
                        .withMatcher("name", m -> m.exact())
                        .withIgnorePaths("contentType", "createdAt", "editedAt", "size")
        ));

        if (file.isPresent()) {
            file.get().setName(newFilename.getFilename());
            file.get().setEditedAt(
                    LocalDate.now()
                            .atTime(LocalTime.now()).atZone(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
            );
            try {
                log.info(String.format("File renamed: %s, %s", filename, newFilename.getFilename()));
                return Either.right(fileRepository.save(file.get()));
            } catch (Exception e) {
                log.error(String.format("Error renaming file: %s, %s", filename, newFilename.getFilename()));
                return Either.left(FileService.SERVER_ERROR);
            }
        }

        log.error(String.format("Error renaming file: %s, %s", filename, newFilename.getFilename()));
        return Either.left(FileService.ERROR_INPUT_DATA);


    }


    public Either<AppError,Pair<File,FileContent>> getFile(String filename) {

        if (Objects.isNull(filename) || filename.trim().isEmpty()) {
            log.error(String.format("Nameless file: %s", filename));
            return Either.left(FileService.ERROR_INPUT_DATA);
        }

        Optional<File> file = fileRepository.findOne(Example.of(
                new File(filename),
                ExampleMatcher.matching()
                        .withMatcher("name", m -> m.exact())
                        .withIgnorePaths("contentType", "createdAt", "editedAt", "size")
        ));

        if (file.isPresent()) {
            Optional<FileContent> content = fileContentRepository.findOne(Example.of(new FileContent(file.get())));
            if (content.isPresent()) {
                log.info(String.format("File content found: %s", filename));
                return Either.right(Pair.with(file.get(),content.get()));
            }

            log.error(String.format("File has no content: %s", filename));
            return Either.left(FileService.SERVER_ERROR);

        }

        log.error(String.format("No file found: %s", filename));
        return Either.left(FileService.SERVER_ERROR);

    }


    public Either<AppError,List<File>> deleteFile(String filename) {

        if (Objects.isNull(filename) || filename.trim().isEmpty()) {

            log.error(String.format("File has no name: %s", filename));
            return Either.left(FileService.ERROR_INPUT_DATA);
        }

        List<File> files = fileRepository.findAll(Example.of(
                new File(filename),
                ExampleMatcher.matching().withMatcher("name", m -> m.caseSensitive())
                        .withIgnorePaths("contentType", "createdAt", "editedAt", "size")
        ));

        try {
            fileRepository.deleteAll(files);

            log.info(String.format("File has been deleted: %s", filename));
            return Either.right(files);

        } catch (Exception e) {

            log.error(String.format("Error while deleting file: %s", filename));
            return Either.left(FileService.SERVER_ERROR);
        }


    }


    public Either<AppError, File> saveFile(String filename, MultipartFile content) {

        if (Objects.isNull(filename) || filename.trim().isEmpty() || Objects.isNull(content)) {

            log.error(String.format("File has no name: %s", filename));
            return Either.left(FileService.ERROR_INPUT_DATA);
        }

        try {
            long now = LocalDate.now().atTime(LocalTime.now()).atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

            File file = fileRepository.save(
                    new File(
                            filename,
                            content.getContentType(),
                            now, now,
                            content.getSize()
                    )
            );
            fileContentRepository.save(new FileContent(file, content.getBytes()));

            log.info(String.format("File saved: %s", filename));
            return Either.right(file);
        } catch (Exception e) {

            log.error(String.format("File cannot be saved: %s", filename));
            return Either.left(FileService.ERROR_INPUT_DATA);
        }
    }

    public Either<AppError, List<File>> getFiles(Integer limit) {

        if (Objects.isNull(limit)) {
            try {

                log.info(String.format("No limit. All files sent: limit: %d", limit));
                return Either.right(fileRepository.findAll());

            } catch (Exception e) {

                log.error(String.format("Error while retrieving all files: limit: %d", limit));
                return Either.left(SERVER_ERROR);
            }
        }

        try {

            PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "name"));

            log.info(String.format("Page of files sent: limit: %d", limit));
            return Either.right(fileRepository.findAll(pageRequest).stream().toList());

        } catch (Exception e) {

            log.error(String.format("Error while servicing files: limit: %d", limit));
            return Either.left(SERVER_ERROR);

        }

    }

}
