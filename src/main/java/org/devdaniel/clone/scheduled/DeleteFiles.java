package org.devdaniel.clone.scheduled;

import org.apache.commons.io.FileUtils;
import org.devdaniel.clone.config.ClonerProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

@Component
public class DeleteFiles {

    private final ClonerProperties props;
    private static final long FIVE_DAYS_MILLIS = 5L * 24 * 60 * 60 * 1000;

    public DeleteFiles(ClonerProperties props) {
        this.props = props;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public ResponseEntity<String> cleanupOldJobs() {
        File baseDir = new File(props.getBaseDir());

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return ResponseEntity.status(500).body("Diretorio não encontrado.");
        }

        File[] files = baseDir.listFiles();
        if (files == null || files.length == 0) {
            return ResponseEntity.status(500).body("Não há arquivos para deletar.");
        }

        Instant now = Instant.now();

        for (File f : files) {
            try {
                Path path = f.toPath();
                BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                Instant creationTime = attr.creationTime().toInstant();
                long ageMillis = now.toEpochMilli() - creationTime.toEpochMilli();

                if (ageMillis > FIVE_DAYS_MILLIS) {
                    if (f.isDirectory()) {
                        System.out.println("[Cleanup] Deleting folder: " + f.getName());
                        FileUtils.deleteDirectory(f);
                        return ResponseEntity.status(200).body("Diretório " + f.getName() + " deletado com sucesso.");
                    } else if (f.isFile() && f.getName().endsWith(".zip")) {
                        System.out.println("[Cleanup] Deleting file: " + f.getName());
                        Files.deleteIfExists(path);
                        return ResponseEntity.status(200).body("Arquivo " + f.getName() + " deletado com sucesso.");
                    }
                } else {
                }
            } catch (Exception e) {
                return ResponseEntity.status(400).body("Erro ao deletar arquivo/pasta: " + f.getName());
            }
        }
        return ResponseEntity.ok("Nenhum arquivo ou pasta antigo para deletar.");
    }

}