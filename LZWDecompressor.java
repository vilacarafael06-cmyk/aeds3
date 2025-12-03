package dao;

import java.io.*;
import java.util.*;

/**
 * LZW Decompressor compatível com compressor que:
 * - usa dicionário inicial ASCII (0..255)
 * - usa códigos fixos de 12 bits
 * - não faz reset do dicionário
 *
 * Uso: java LZWDecompressor arquivoEntrada.lzw arquivoSaida
 */
public class LZWDecompressor {

    private static final int CODE_WIDTH = 12;
    private static final int MAX_DICT_SIZE = 1 << CODE_WIDTH; // 4096

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Uso: java LZWDecompressor <arquivoEntrada.lzw> <arquivoSaida>");
            System.exit(1);
        }

        String inputPath = args[0];
        String outputPath = args[1];

        try (FileInputStream fis = new FileInputStream(inputPath);
             BitInputStream bis = new BitInputStream(fis);
             FileOutputStream fos = new FileOutputStream(outputPath)) {

            decompress(bis, fos);
            System.out.println("Descompressão concluída: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void decompress(BitInputStream bis, OutputStream out) throws IOException {
        // Inicializa dicionário: 0..255 -> bytes únicos
        List<byte[]> dict = new ArrayList<>(MAX_DICT_SIZE);
        for (int i = 0; i < 256; i++) {
            dict.add(new byte[]{(byte) i});
        }
        int nextCode = 256;

        // Ler o primeiro código
        Integer first = bis.readBits(CODE_WIDTH);
        if (first == null) return; // arquivo vazio ou sem códigos completos
        int prevCode = first;
        byte[] prevEntry = dict.get(prevCode);
        out.write(prevEntry);

        Integer curCode;
        while ((curCode = bis.readBits(CODE_WIDTH)) != null) {
            byte[] entry;
            if (curCode < dict.size()) {
                // caso normal
                entry = dict.get(curCode);
            } else if (curCode == nextCode) {
                // caso especial: KwKwK (W + primeira letra de W)
                // monta entry = prevEntry + firstByte(prevEntry)
                entry = concat(prevEntry, prevEntry[0]);
            } else {
                throw new IOException("Código inválido lido: " + curCode);
            }

            // escreve entrada ao output
            out.write(entry);

            // adiciona nova entrada ao dicionário: prevEntry + primeiro byte de entry
            if (nextCode < MAX_DICT_SIZE) {
                byte firstByte = entry[0];
                dict.add(concat(prevEntry, firstByte));
                nextCode++;
            }

            // atualiza prevEntry
            prevEntry = entry;
            prevCode = curCode;
        }
        out.flush();
    }

    // concatena um byte[] com um único byte no final
    private static byte[] concat(byte[] a, byte b) {
        byte[] res = Arrays.copyOf(a, a.length + 1);
        res[a.length] = b;
        return res;
    }

    /**
     * BitInputStream lê bits MSB-first do InputStream.
     * readBits(n) retorna Integer (0..(2^n-1)) ou null se não houver bits suficientes (final).
     */
    static class BitInputStream implements Closeable {
        private final InputStream in;
        private int buffer; // armazena bits lidos mas ainda não consumidos (à direita os menos significativos)
        private int nbits;  // quantos bits válidos estão em 'buffer' (0..7..16..)

        public BitInputStream(InputStream in) {
            this.in = in;
            this.buffer = 0;
            this.nbits = 0;
        }

        /**
         * Lê 'count' bits e retorna o valor (bits lidos são MSB-first).
         * Se não houver bits suficientes (fim do arquivo), retorna null.
         */
        public Integer readBits(int count) throws IOException {
            if (count < 0 || count > 32) throw new IllegalArgumentException("count inválido");

            // preenche buffer até ter pelo menos 'count' bits
            while (nbits < count) {
                int next = in.read();
                if (next == -1) break;
                // adiciona 8 bits ao buffer à direita (os mais significativos lidos primeiro)
                buffer = (buffer << 8) | (next & 0xFF);
                nbits += 8;
            }

            if (nbits < count) {
                // bits finais incompletos — consideramos fim
                return null;
            }

            // agora extrai os 'count' bits mais significativos do buffer
            int shift = nbits - count;
            int mask = (1 << count) - 1;
            int value = (buffer >> shift) & mask;

            // mantém apenas os bits restantes no buffer
            buffer = buffer & ((1 << shift) - 1);
            nbits = shift;

            return value;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
