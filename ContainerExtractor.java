package util;

import java.io.*;
import java.nio.file.*;

public class ContainerExtractor {

    /**
     * Extrai containerFile para targetDir (cria targetDir se necessário).
     */
    public static void extractContainer(File containerFile, Path targetDir) throws IOException {
        if (!containerFile.exists()) {
            throw new FileNotFoundException("Container não encontrado: " + containerFile.getPath());
        }

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(containerFile)))) {
            int nFiles;
            try {
                nFiles = dis.readInt();
            } catch (EOFException e) {
                throw new IOException("Container vazio ou corrompido.", e);
            }

            for (int i = 0; i < nFiles; i++) {
                int pathLen = dis.readInt();
                byte[] pathBytes = new byte[pathLen];
                dis.readFully(pathBytes);
                String relPath = new String(pathBytes, "UTF-8");
                long fileSize = dis.readLong();

                Path outPath = targetDir.resolve(relPath.replace('/', File.separatorChar));
                Path parent = outPath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }

                try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(outPath))) {
                    byte[] buffer = new byte[8192];
                    long remaining = fileSize;
                    while (remaining > 0) {
                        int toRead = (int)Math.min(buffer.length, remaining);
                        int read = dis.read(buffer, 0, toRead);
                        if (read == -1) {
                            throw new EOFException("Fim de arquivo inesperado no container.");
                        }
                        out.write(buffer, 0, read);
                        remaining -= read;
                    }
                }
            }
        }
    }
}
