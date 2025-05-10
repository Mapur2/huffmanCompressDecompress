package com.zipper.huffman.service;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class ZipService {
    public String getOriginalFileName(String compressedFileName) {
        if (compressedFileName.endsWith(".huff")) {
            return compressedFileName.substring(0, compressedFileName.length() - 5);
        }
        return "decompressed_file";
    }

    @Override
    public String toString() {
        return super.toString();
    }

    // Huffman Coding Implementation
    static class Node {
        int frequency;
        byte data;
        Node left, right;

        Node(int frequency, byte data) {
            this.frequency = frequency;
            this.data = data;
        }

        Node(int frequency, Node left, Node right) {
            this.frequency = frequency;
            this.left = left;
            this.right = right;
        }
    }

     private Map<Byte, String> generateHuffmanCodes(Node root) {
        Map<Byte, String> huffmanCodes = new HashMap<>();
        generateHuffmanCodes(root, "", huffmanCodes);
        return huffmanCodes;
    }

     private void generateHuffmanCodes(Node node, String code, Map<Byte, String> huffmanCodes) {
        if (node == null) return;
        if (node.left == null && node.right == null) {
            huffmanCodes.put(node.data, code);
            return;
        }
        generateHuffmanCodes(node.left, code + "0", huffmanCodes);
        generateHuffmanCodes(node.right, code + "1", huffmanCodes);
    }

    public byte[] compress(byte[] data) {
        // 1. Calculate Frequency of Each Byte
        Map<Byte, Integer> frequencyMap = new HashMap<>();
        for (byte b : data) {
            frequencyMap.put(b, frequencyMap.getOrDefault(b, 0) + 1);
        }

        // 2. Build Huffman Tree
        Node root = buildHuffmanTree(frequencyMap);

        // 3. Generate Huffman Codes
        Map<Byte, String> huffmanCodes = generateHuffmanCodes(root);

        // 4. Encode the Data
        StringBuilder encodedData = new StringBuilder();
        for (byte b : data) {
            encodedData.append(huffmanCodes.get(b));
        }

        // 5.  Prepare metadata and compressed data for output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            // Write the frequency map (for decompression)
            writeFrequencyMap(outputStream, frequencyMap);

            // Write the encoded data
            writeEncodedData(outputStream, encodedData.toString());

        } catch (IOException e) {
            throw new RuntimeException("Error during compression: " + e.getMessage());
        }
        return outputStream.toByteArray();
    }

    private static void writeFrequencyMap(ByteArrayOutputStream outputStream, Map<Byte, Integer> frequencyMap) throws IOException {
        outputStream.write(frequencyMap.size()); // Number of distinct bytes
        for (Map.Entry<Byte, Integer> entry : frequencyMap.entrySet()) {
            outputStream.write(entry.getKey());        // The byte
            outputStream.write(intToBytes(entry.getValue())); // The frequency (as 4 bytes)
        }
    }

    private static void writeEncodedData(OutputStream outputStream, String encodedData) throws IOException {
        // Write the length of the encoded data as a 4-byte integer
        outputStream.write(intToBytes(encodedData.length()));

        // Write the encoded data as a sequence of bytes.  Pad with zeros to make
        // the length a multiple of 8.
        int padding = 8 - (encodedData.length() % 8);
        if (padding == 8) {
            padding = 0;
        }
        for (int i = 0; i < padding; i++) {
            encodedData += '0'; // Pad with 0s.
        }

        for (int i = 0; i < encodedData.length(); i += 8) {
            String byteString = encodedData.substring(i, i + 8);
            byte b = (byte) Integer.parseInt(byteString, 2);
            outputStream.write(b);
        }
        outputStream.write(padding); // Write the padding
    }

    private static byte[] intToBytes(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value
        };
    }

    public byte[] decompress(byte[] compressedData) {
        InputStream inputStream = new ByteArrayInputStream(compressedData);
        try {
            Map<Byte, Integer> frequencyMap = readFrequencyMap(inputStream);

            Node root = buildHuffmanTree(frequencyMap);
            DecodedDataResult decodedResult = readEncodedData(inputStream);
            String encodedData = decodedResult.encodedData;
            int padding = decodedResult.padding;
            encodedData = encodedData.substring(0, encodedData.length() - padding);

            // 4. Decode the Data
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Node current = root;
            for (char c : encodedData.toCharArray()) {
                if (c == '0') {
                    current = current.left;
                } else {
                    current = current.right;
                }

                if (current.left == null && current.right == null) {
                    outputStream.write(current.data);
                    current = root;
                }
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error during decompression: " + e.getMessage());
        }
    }

    private static Map<Byte, Integer> readFrequencyMap(InputStream inputStream) throws IOException {
        Map<Byte, Integer> frequencyMap = new HashMap<>();
        int mapSize = inputStream.read(); // Number of distinct bytes
        for (int i = 0; i < mapSize; i++) {
            byte data = (byte) inputStream.read();
            int frequency = bytesToInt(readBytes(inputStream, 4));
            frequencyMap.put(data, frequency);
        }
        return frequencyMap;
    }

    private static DecodedDataResult readEncodedData(InputStream inputStream) throws IOException {
        int encodedDataLength = bytesToInt(readBytes(inputStream, 4));

        StringBuilder encodedData = new StringBuilder();
        int bytesToRead = (int) Math.ceil(encodedDataLength / 8.0);
        for (int i = 0; i < bytesToRead; i++) {
            int b = inputStream.read();
            if (b == -1) {
                break; // Handle end of stream
            }
            encodedData.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        int padding = inputStream.read();
        return new DecodedDataResult(encodedData.toString(), padding);
    }

    private static class DecodedDataResult {
        String encodedData;
        int padding;

        public DecodedDataResult(String encodedData, int padding) {
            this.encodedData = encodedData;
            this.padding = padding;
        }
    }

    private static int bytesToInt(byte[] bytes) {
        return (bytes[0] & 0xFF) << 24 |
                (bytes[1] & 0xFF) << 16 |
                (bytes[2] & 0xFF) << 8 |
                (bytes[3] & 0xFF);
    }

    private static byte[] readBytes(InputStream inputStream, int numBytes) throws IOException {
        byte[] buffer = new byte[numBytes];
        int bytesRead = inputStream.read(buffer);
        if (bytesRead != numBytes) {
            throw new IOException("Could not read enough bytes. Expected " + numBytes + ", got " + bytesRead);
        }
        return buffer;
    }

    static Node buildHuffmanTree(Map<Byte, Integer> frequencyMap) {
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(node -> node.frequency));
        for (Map.Entry<Byte, Integer> entry : frequencyMap.entrySet()) {
            pq.offer(new Node(entry.getValue(), entry.getKey()));
        }

        while (pq.size() > 1) {
            Node left = pq.poll();
            Node right = pq.poll();
            if (left != null && right != null) { //Make sure left and right are not null
                pq.offer(new Node(left.frequency + right.frequency, left, right));
            }
        }
        return pq.poll();
    }
}

