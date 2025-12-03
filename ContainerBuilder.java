package util;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ContainerBuilder {

    public static File buildContainer(Path dirBase, File containerFile) throws IOException {
        if (dirBase == null || !Files.exists(dirBase) || !Files.isDirectory(dirBase)) {
            throw new IllegalArgumentException("DiretorioBase inválido: " + dirBase);
        }

        // coleta arquivos recursivamente (somente arquivos regulares)
        List<Path> arquivos = new ArrayList<>();
        Files.walk(dirBase)
                .filter(Files::isRegularFile)
                .forEach(arquivos::add);

        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(containerFile)))) {
            // número de arquivos
            dos.writeInt(arquivos.size());

            for (Path p : arquivos) {
                // caminho relativo em relação a diretorioBase, com separador '/'
                Path rel = dirBase.relativize(p);
                String caminho = rel.toString().replace(File.separatorChar, '/');
                byte[] caminhoBytes = caminho.getBytes("UTF-8");
                long fileSize = Files.size(p);

                // escreve meta
                dos.writeInt(caminhoBytes.length);
                dos.write(caminhoBytes);
                dos.writeLong(fileSize);

                // escreve conteúdo
                try (InputStream in = new BufferedInputStream(Files.newInputStream(p))) {
                    byte[] buffer = new byte[8192];
                    long remaining = fileSize;
                    int read;
                    while (remaining > 0 && (read = in.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                        dos.write(buffer, 0, read);
                        remaining -= read;
                    }
                }
            }

            dos.flush();
        }

        return containerFile;
    }
}
