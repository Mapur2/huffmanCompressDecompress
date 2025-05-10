package com.zipper.huffman.controller;

import com.zipper.huffman.service.ZipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class ZipController {
    @Autowired
    ZipService zipService;


    @PostMapping("/compress")
    public ResponseEntity<byte[]> compressFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty".getBytes());
        }

        try {
            byte[] fileContent = file.getBytes();
            byte[] compressedData = zipService.compress(fileContent);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + file.getOriginalFilename() + ".huff")
                    .body(compressedData);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(("Error compressing file: " + e.getMessage().getBytes()).getBytes());
        }
    }

    @PostMapping("/decompress")
    public ResponseEntity<byte[]> decompressFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty".getBytes());
        }

        try {
            byte[] compressedData = file.getBytes();
            byte[] decompressedData = zipService.decompress(compressedData);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + zipService.getOriginalFileName(file.getOriginalFilename()))
                    .body(decompressedData);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(("Error decompressing file: " + e.getMessage().getBytes()).getBytes());
        }
    }
}
