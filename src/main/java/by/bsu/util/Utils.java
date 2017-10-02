package by.bsu.util;

import by.bsu.model.IlluminaSNVSample;
import by.bsu.model.PairEndRead;
import by.bsu.model.Sample;
import by.bsu.model.SequencesTree;
import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongSet;
import com.carrotsearch.hppc.ShortArrayList;
import org.apache.commons.math3.distribution.BinomialDistribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Just class with some useful functions
 */
public class Utils {
    private static final List<String> impossibleCharacters = new ArrayList<>();
    private static final char impossibleChar = '%';
    public static final String DEFAULT_ALPHABET = "ACGT";

    public static List<String> numbers = new ArrayList<>();

    static {
        for (int i = 0; i < 100_000; i++) {
            numbers.add(String.valueOf(i));
        }
    }

    static {
        impossibleCharacters.add("");
        for (int i = 1; i < 3000; i++) {
            impossibleCharacters.add(impossibleCharacters.get(i - 1) + impossibleChar);
        }
    }

    public static long getHashValue(int position, int l, String str) {
        return getHashValue(position, l, str, DEFAULT_ALPHABET);
    }

    public static long getHashValue(int position, int l, String str, String alphabet) {
        long hashValue = 0;
        for (int j = 0; j < l; j++) {
            hashValue *= alphabet.length();
            hashValue += convertLetterToDigit(str.charAt(position + j), alphabet);
        }
        return hashValue;
    }

    /**
     * Return hashes of l-mers in string
     */
    public static LongSet getSequenceHashesSet(String seq, int l) {
        LongSet result = new LongHashSet(seq.length() - l + 1);
        long[] sequenceHashesArray = getSequenceHashesArray(seq, l);
        for (long l1 : sequenceHashesArray) {
            result.add(l1);
        }
        return result;
    }

    /**
     * Return hashes of l-mers in string
     */
    public static List<Long> getSequenceHashesList(String seq, int l) {
        List<Long> result = new ArrayList<>(seq.length() - l + 1);
        long[] sequenceHashesArray = getSequenceHashesArray(seq, l);
        for (long l1 : sequenceHashesArray) {
            result.add(l1);
        }
        return result;
    }

    /**
     * Return hashes of l-mers in string
     */
    public static long[] getSequenceHashesArray(String seq, int l) {
        long[] result = new long[seq.length() - l + 1];
        int i = 0;
        long hashValue = 0;

        for (int j = 0; j < l; j++) {
            hashValue *= 4;
            hashValue += convertLetterToDigit(seq.charAt(j));
        }
        result[i++] = hashValue;
        for (int j = 1; j < seq.length() - l + 1; j++) {
            hashValue -= convertLetterToDigit(seq.charAt(j - 1)) << 2 * (l - 1);
            hashValue <<= 2;
            hashValue += convertLetterToDigit(seq.charAt(j + l - 1));
            result[i++] = hashValue;
        }
        return result;
    }

    public static void fillNodeGramsAndChunks(SequencesTree.Node node, int l) {
        long hashValue = 0;
        String seq = node.key;
        if (seq.length() < l) {
            return;
        }
        for (int j = 0; j < l; j++) {
            hashValue *= 4;
            hashValue += convertLetterToDigit(seq.charAt(j));
        }
        if (!node.grams.containsKey(hashValue)) {
            node.grams.put(hashValue, new ShortArrayList());
        }
        node.grams.get(hashValue).add((short) 0);
        node.chunks.add(hashValue);
        for (short j = 1; j < seq.length() - l + 1; j++) {
            hashValue -= convertLetterToDigit(seq.charAt(j - 1)) << 2 * (l - 1);
            hashValue <<= 2;
            hashValue += convertLetterToDigit(seq.charAt(j + l - 1));
            if (!node.grams.containsKey(hashValue)) {
                node.grams.put(hashValue, new ShortArrayList());
            }
            node.grams.get(hashValue).add(j);
            if (j % l == 0) {
                node.chunks.add(hashValue);
            }
        }
    }

    /**
     * The number of hits between reads' q-grams
     */
    public static int qHits(String s1, String s2, int q) {
        long[] h1 = getSequenceHashesArray(s1, q);
        long[] h2 = getSequenceHashesArray(s2, q);
        Arrays.sort(h1);
        Arrays.sort(h2);
        return qHits(h1, h2);
    }

    public static int qHits(long[] h1, long[] h2) {
        int hits = 0;
        int i = 0, j = 0;
        while (i < h1.length && j < h2.length) {
            if (h1[i] == h2[j]) {
                hits++;
                i++;
                j++;
            } else if (h1[i] < h2[j]) {
                i++;
            } else {
                j++;
            }
        }
        return hits;
    }


    public static int convertLetterToDigit(char c) {
        return convertLetterToDigit(c, DEFAULT_ALPHABET);
    }

    public static int convertLetterToDigit(char c, String alphabet) {
        return alphabet.indexOf(c);
    }

    public static char convertIntToLetter(int i) {
        switch (i) {
            case 0:
                return 'A';
            case 1:
                return 'C';
            case 2:
                return 'G';
            case 3:
                return 'T';
        }
        return '_';
    }

    public static String consensus(String[] sequences){
        return consensus(sequences, DEFAULT_ALPHABET);
    }

    public static String consensus(String[] sequences, String alphabet) {
        if (sequences.length == 0) {
            return "";
        }
        int l = sequences[0].length();
        int[][] count = new int[alphabet.length()][l];
        for (String s : sequences) {
            for (int i = 0; i < s.length(); i++) {
                count[convertLetterToDigit(s.charAt(i), alphabet)][i]++;
            }
        }
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < l; i++) {
            int max = 0;
            for (int j = 1; j < alphabet.length(); j++) {
                if (count[j][i] > count[max][i]) {
                    max = j;
                }
            }
            str.append(convertIntToLetter(max));
        }
        return str.toString();
    }

    public static String consensus(Sample sample) {
        return consensus(sample.sequences);
    }

    public static double[][] profile(Sample sample){
        return profile(sample, DEFAULT_ALPHABET);
    }
    public static double[][] profile(Sample sample, String alphabet) {
        String[] sequences = sample.sequences;
        if (sequences.length == 0) {
            return new double[0][alphabet.length()];
        }
        int l = sequences[0].length();
        int[][] count = new int[alphabet.length()][l];
        for (String s : sequences) {
            for (int i = 0; i < s.length(); i++) {
                count[convertLetterToDigit(s.charAt(i), alphabet)][i]++;
            }
        }
        double[][] result = new double[alphabet.length()][l];
        for (int i = 0; i < alphabet.length(); i++) {
            for (int j = 0; j < l; j++) {
                result[i][j] = count[i][j] / (double) sequences.length;
            }
        }
        return result;
    }

    public static double[][] profile(IlluminaSNVSample sample, String alphabet){
        List<PairEndRead> reads = sample.reads;
        if (reads.size() == 0){
            return new double[0][alphabet.length()];
        }
        int[][] count = new int[alphabet.length()][sample.referenceLength];
        reads.forEach( r -> {
            for (int i = 0; i < r.l.length(); i++) {
                count[convertLetterToDigit(r.l.charAt(i), alphabet)][i+r.lOffset]++;
            }
            for (int i = 0; i < r.r.length(); i++) {
                count[convertLetterToDigit(r.r.charAt(i), alphabet)][i+r.rOffset]++;
            }
        });
        double[][] result = new double[alphabet.length()][sample.referenceLength];
        for (int i = 0; i < count[0].length; i++) {
            int sum = 0;
            for (int[] aCount : count) {
                sum += aCount[i];
            }
            if (sum == 0){
                sum = 1;
            }
            for (int j = 0; j < count.length; j++) {
                result[j][i] = count[j][i]/sum;
            }
        }
        return result;
    }

    /**
     * Append missing characters to string so they have the same size
     */
    public static String[] stringsForHamming(String[] sequences) {
        int max = Arrays.stream(sequences).mapToInt(String::length).max().getAsInt();
        String[] result = new String[sequences.length];
        for (int i = 0; i < sequences.length; i++) {
            result[i] = sequences[i].length() < max ?
                    sequences[i] + impossibleCharacters.get(max - sequences[i].length()) :
                    sequences[i];
        }
        return result;
    }

    public static String[] stringsForHamming(String[] sequences, char character) {
        List<String> chars = new ArrayList<>();
        chars.add("");
        for (int i = 1; i < 3000; i++) {
            chars.add(chars.get(i - 1) + character);
        }
        int max = Arrays.stream(sequences).mapToInt(String::length).max().getAsInt();
        String[] result = new String[sequences.length];
        for (int i = 0; i < sequences.length; i++) {
            result[i] = sequences[i].length() < max ?
                    sequences[i] + chars.get(max - sequences[i].length()) :
                    sequences[i];
        }
        return result;
    }

    public static void expandNumbers(int size) {
        if (size > numbers.size()) {
            for (int i = numbers.size(); i < size; i++) {
                numbers.add(String.valueOf(i));
            }
        }
    }

    public static int getMajorAllele(double[][] profile, int i) {
        int major = 0;
        for (int j = 1; j < profile.length; j++) {
            if (profile[j][i] > profile[major][i]){
                major = j;
            }
        }
        return major;
    }

    public static String byteArrayToString(byte[] arr){
        StringBuilder str = new StringBuilder();
        for (byte b : arr) {
            str.append((char)b);
        }
        return str.toString();
    }

    public static double binomialPvalue(int s, double p,int n){
        return 1 - new BinomialDistribution(n, p).cumulativeProbability(s);
    }

}
