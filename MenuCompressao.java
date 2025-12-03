package view;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import dao.LZWCompressor;
import dao.LZWDecompressor;
import util.ContainerBuilder;
import util.ContainerExtractor;

public class MenuCompressao {

    private Scanner console;

    public MenuCompressao(Scanner console) {
        this.console = console;
    }

    public void menu() {
        int opcao;

        do {
            System.out.println("\n--- Menu de Compressão ---");
            System.out.println("1 - Comprimir arquivos em .lzw");
            System.out.println("2 - Descomprimir arquivos .lzw");
            System.out.println("0 - Voltar");

            System.out.print("Opção: ");
            try {
                opcao = Integer.parseInt(console.nextLine());
            } catch (Exception e) {
                opcao = -1;
            }

            switch (opcao) {
                case 1:
                    compactarArquivosDB();
                    break;
                case 2:
                    descompactarArquivosLZW();
                    break;
                case 0:
                    System.out.println("Voltando...");
                    break;
                default:
                    System.out.println("Opção inválida!");
                    break;
            }
        } while (opcao != 0);
    }

    private void compactarArquivosDB() {
        try {
            Path origDir = Paths.get("dados/DadosOriginais");
            File comprimidosDir = new File("dados/DadosComprimidos/LZW");

            if (!Files.exists(origDir) || !Files.isDirectory(origDir)) {
                System.out.println("A pasta 'dados/DadosOriginais' não existe!");
                return;
            }

            if (!comprimidosDir.exists()) {
                comprimidosDir.mkdirs();
            }

            File containerBin = new File(comprimidosDir, "backup.bin");
            File containerLzw = new File(comprimidosDir, "backup.lzw");

            ContainerBuilder.buildContainer(origDir, containerBin);
            LZWCompressor.main(new String[]{ containerBin.getPath(), containerLzw.getPath() });

            // remover .bin temporário
            if (containerBin.exists()) {
                boolean deleted = containerBin.delete();
                if (!deleted) {
                    System.out.println("Aviso: não foi possível apagar o container temporário: " + containerBin.getPath());
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao compactar tudo em um arquivo:");
            e.printStackTrace();
        }
    }

    // ---------- Descompactar tudo.lzw e restaurar arquivos ----------
    private void descompactarArquivosLZW() {
        try {
            File comprimidosDir = new File("dados/DadosComprimidos/LZW");
            File descomprimidosDir = new File("dados/DadosDescomprimidos/Lzw");

            if (!comprimidosDir.exists()) {
                System.out.println("A pasta 'dados/DadosComprimidos' não existe!");
                return;
            }
            if (!descomprimidosDir.exists()) {
                descomprimidosDir.mkdirs();
            }

            File containerLzw = new File(comprimidosDir, "backup.lzw");
            if (!containerLzw.exists()) {
                System.out.println("Arquivo 'tudo.lzw' não encontrado em dados/DadosComprimidos/");
                return;
            }

            // descomprime tudo.lzw -> tudo.bin (temporário)
            File tempBin = new File(comprimidosDir, "backup_descompactado.bin");
            LZWDecompressor.main(new String[]{ containerLzw.getPath(), tempBin.getPath() });

            // extrai container para DadosDescomprimidos
            ContainerExtractor.extractContainer(tempBin, descomprimidosDir.toPath());

            // remove temporário
            if (tempBin.exists()) {
                boolean deleted = tempBin.delete();
                if (!deleted) {
                    System.out.println("Aviso: não foi possível apagar o arquivo temporário: " + tempBin.getPath());
                }
            }

        } catch (Exception e) {
            System.err.println("Erro ao descompactar tudo.lzw:");
            e.printStackTrace();
        }
    }
}
