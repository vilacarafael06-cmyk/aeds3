package dao;

import java.io.*;
import java.util.*;

/**
 * LZW Compressor simples usando tabela ASCII inicial (0-255) e códigos de 12 bits.
 *
 * Uso: java LZWCompressor arquivoEntrada arquivoSaida
 */
public class LZWCompressor {

    // largura fixa do código em bits
    private static final int CODE_WIDTH = 12;
    private static final int MAX_DICT_SIZE = 1 << CODE_WIDTH; // 4096

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Uso: java LZWCompressor <arquivoEntrada> <arquivoSaida>");
            System.exit(1);
        }

        String inputPath = args[0];
        String outputPath = args[1];

        try (FileInputStream fis = new FileInputStream(inputPath);
             FileOutputStream fos = new FileOutputStream(outputPath);
             BitOutputStream bos = new BitOutputStream(fos)) {

            byte[] data = fis.readAllBytes();
            compress(data, bos);

            bos.flush(); // garante escrita de bits restantes
            System.out.println("Compressão concluída: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compressão LZW sobre um array de bytes.
     * Usamos strings de 1 char por byte, codificação ISO-8859-1 para preservar 0-255.
     */
    private static void compress(byte[] inputData, BitOutputStream bos) throws IOException {
        // Inicializa o dicionário: cada símbolo ASCII simples (0..255) mapeado para seu código
        Map<String, Integer> dict = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            // representa o byte como um char único usando ISO-8859-1
            dict.put(new String(new byte[]{(byte) i}, "ISO-8859-1"), i);
        }

        int nextCode = 256;

        // percorre os bytes construindo padrões
        String w = "";
        for (int i = 0; i < inputData.length; i++) {
            String k = new String(new byte[]{ inputData[i] }, "ISO-8859-1");
            String wk = w + k;

            if (dict.containsKey(wk)) {
                w = wk;
            } else {
                // escreve código para w
                int code = dict.get(w);
                bos.writeBits(code, CODE_WIDTH);

                // adiciona wk ao dicionário se houver espaço
                if (nextCode < MAX_DICT_SIZE) {
                    dict.put(wk, nextCode++);
                }
                w = k;
            }
        }

        // escreve o código restante
        if (!w.equals("")) {
            bos.writeBits(dict.get(w), CODE_WIDTH);
        }
    }

    /**
     * Classe auxiliar para escrever bits em um OutputStream.
     * Agrupa bits em bytes e escreve quando o buffer atinge 8 bits.
     */
    static class BitOutputStream implements Closeable {
        private OutputStream out;
        private int buffer; // armazena bits não escritos (direita = menos significante)
        private int nbits;  // quantos bits estão atualmente no buffer (0..7)

        public BitOutputStream(OutputStream out) {
            this.out = out;
            this.buffer = 0;
            this.nbits = 0;
        }

        /**
         * Escreve 'count' bits do valor (assume valor não exceder count bits).
         * Os bits são escritos do mais significativo para o menos significativo.
         */
        public void writeBits(int value, int count) throws IOException {
            if (count < 0 || count > 32) throw new IllegalArgumentException("count inválido");
            // escrevemos os bits mais significativos primeiro
            for (int i = count - 1; i >= 0; i--) {
                int bit = (value >> i) & 1;
                buffer = (buffer << 1) | bit;
                nbits++;
                if (nbits == 8) {
                    out.write(buffer);
                    buffer = 0;
                    nbits = 0;
                }
            }
        }

        /**
         * Escreve zeros no fim para completar o último byte e fecha o stream.
         */
        public void flush() throws IOException {
            if (nbits > 0) {
                buffer = buffer << (8 - nbits); // preenche com zeros à direita
                out.write(buffer);
                buffer = 0;
                nbits = 0;
            }
            out.flush();
        }

        @Override
        public void close() throws IOException {
            flush();
            out.close();
        }
    }
}
