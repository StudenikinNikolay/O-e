package edu.diploma.controller;

import edu.diploma.auth.JwtHelper;
import edu.diploma.model.*;
import edu.diploma.repository.FileContentRepository;
import edu.diploma.repository.FileRepository;
import edu.diploma.service.FileService;
import io.vavr.control.Either;
import org.javatuples.Pair;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;


@RestController
public class FileController {

    private final AuthenticationManager authManager;
    private final JwtHelper jwtHelper;
    private final FileRepository fileRepository;
    private final FileContentRepository fileContentRepository;
    private final FileService fileService;

    public FileController(
            AuthenticationManager authManager,
            JwtHelper jwtHelper,
            FileRepository fileRepository,
            FileContentRepository fileContentRepository,
            FileService fileService
    ) {
        this.authManager = authManager;
        this.jwtHelper = jwtHelper;
        this.fileRepository = fileRepository;
        this.fileContentRepository = fileContentRepository;
        this.fileService = fileService;
    }


    @PutMapping("/file")
    public ResponseEntity<?> putFile(
            @RequestParam("filename") String filename,
            @RequestBody NewFilename newFilename
    ) {

        Either<AppError, File> result = fileService.renameFile(filename, newFilename);

        if (result.isRight()) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(result.getLeft().getCode()).body(result.getLeft());

    }


    @GetMapping("/file")
    public ResponseEntity<?> getFile(@RequestParam("filename") String filename) {

        Either<AppError, Pair<File, FileContent>> file = fileService.getFile(filename);

        if (file.isRight()) {
            final FileContent content = file.get().getValue1();
            try (InputStream in = new ByteArrayInputStream(content.getContent())) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE,content.getFile().getContentType())
                        .body(new InputStreamResource(in));
            } catch (Exception e) {
                return ResponseEntity.status(500).body(FileService.SERVER_ERROR);
            }
        }

        return ResponseEntity.status(file.getLeft().getCode()).body(file.getLeft());

    }


    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(@RequestParam("filename") String filename) {

        Either<AppError, List<File>> result = fileService.deleteFile(filename);

        if (result.isRight()) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(result.getLeft().getCode()).body(result.getLeft());


    }


    @PostMapping("/file")
    public ResponseEntity<?> postFile(
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile content
    ) {

        Either<AppError, File> result = fileService.saveFile(filename, content);

        if (result.isRight()) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getLeft());

    }

    @GetMapping("/list")
    public ResponseEntity<?> getFiles(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        Either<AppError, List<File>> files = fileService.getFiles(limit);

        if (files.isRight()) {
            return ResponseEntity.ok().body(files.get());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(files.getLeft());

    }

}