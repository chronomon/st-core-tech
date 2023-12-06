package com.chronomon.analysis.trajectory.cooccur;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This code is the implementation in Java for the paper "EBM: An Entropy-Based Model to Infer Social Strength from Spatio-temporal Data" of 2013 ACM SIGMOD.
 *
 * @author yuzisheng
 * @date 2023-11-04
 */
public class SocialStrengthInfer {
    /**
     * user count: user id from zero to (userNum - 1)
     */
    private final int userCount;
    /**
     * location count: location id from zero to (locNum - 1)
     */
    private final int locationCount;
    /**
     * list of check-in
     */
    private final List<CheckIn> checkIns;
    /**
     * the order of diversity for renyi shannon entropy is used to limit coincidences' impact (0.1 is the optimal value in the paper)
     */
    private double diversityOrder = 0.1;
    /**
     * co-occurrence vector
     */
    private int[][][] coVector;
    /**
     * number of occurrences per user per location
     */
    private int[][] locUserVector;
    /**
     * shannon entropy vector for each pair users
     */
    private double[][] shannonEntropyVector;
    /**
     * renyi entropy vector for each pair users
     */
    private double[][] renyiEntropyVector;
    /**
     * diversity for each pair users: quantify how many effective locations the co-occurrences between two people represent
     */
    private double[][] diversityVector;
    /**
     * location entropy: a high value indicates a popular place with many visitors and is not specific to anyone
     */
    private double[] locationEntropyVector;
    /**
     * weighted frequency: a high value indicates a small uncrowded place
     */
    private double[][] weightedFrequencyVector;
    /**
     * social strength for each user pair according to diversity and weighted frequency
     */
    double[][] socialStrengthVector;

    public SocialStrengthInfer(int userCount, int locationCount, List<CheckIn> checkIns) {
        this.userCount = userCount;
        this.locationCount = locationCount;
        this.checkIns = checkIns;
    }

    public SocialStrengthInfer(int userCount, int locationCount, List<CheckIn> checkIns, double diversityOrder) {
        this.userCount = userCount;
        this.locationCount = locationCount;
        this.checkIns = checkIns;
        this.diversityOrder = diversityOrder;
    }

    /**
     * compute social strength for each user pair
     */
    public double[][] computeSocialStrength() {
        // if social strength has been computed already, just return directly
        if (socialStrengthVector != null) return socialStrengthVector;

        initCoVector();
        computeDiversity();
        computeWeightedFrequency();

        // todo: linear regression training to get coefficient
        socialStrengthVector = new double[userCount][userCount];
        for (int i = 0; i < userCount; i++) {
            for (int j = 0; j < userCount; j++) {
                socialStrengthVector[i][j] = diversityVector[i][j] + weightedFrequencyVector[i][j];
            }
        }
        return socialStrengthVector;
    }

    /**
     * compute co-occurrence vector and count occurrence per user per location
     */
    private void initCoVector() {
        coVector = new int[userCount][userCount][locationCount];
        locUserVector = new int[locationCount][userCount];
        for (int i = 0; i < checkIns.size(); i++) {
            // count occurrence per user per location
            locUserVector[checkIns.get(i).getLid()][checkIns.get(i).getUid()]++;
            for (int j = i + 1; j < checkIns.size(); j++) {
                // compute co-occurrence vector
                CheckIn a = checkIns.get(i);
                CheckIn b = checkIns.get(j);
                if (a.getUid() != b.getUid() && a.getLid() == b.getLid() && a.getTime() == b.getTime()) {
                    coVector[a.getUid()][b.getUid()][a.getLid()]++;
                    coVector[b.getUid()][a.getUid()][a.getLid()]++;
                }
            }
        }
    }

    /**
     * compute shannon entropy for each pair
     */
    private void computeShannonEntropy() {
        shannonEntropyVector = new double[userCount][userCount];
        for (int i = 0; i < userCount; i++) {
            for (int j = i + 1; j < userCount; j++) {
                int coLocNum = Arrays.stream(coVector[i][j]).sum();
                if (coLocNum == 0) continue;
                double shannonEntropy = 0.0;
                for (int k = 0; k < locationCount; k++) {
                    if (coVector[i][j][k] == 0) continue;
                    double probability = coVector[i][j][k] / (double) coLocNum;
                    shannonEntropy += (-probability * Math.log(probability));
                }
                shannonEntropyVector[i][j] = shannonEntropy;
                shannonEntropyVector[j][i] = shannonEntropy;
            }
        }
    }

    /**
     * compute renyi entropy for each pair
     */
    private void computeRenyiEntropy() {
        renyiEntropyVector = new double[userCount][userCount];

        // renyi entropy becomes shannon entropy if the order of diversity equals one
        if (Math.abs(diversityOrder - 1.0) < 1e-5) {
            computeShannonEntropy();
            System.arraycopy(shannonEntropyVector, 0, renyiEntropyVector, 0, userCount);
            return;
        }

        for (int i = 0; i < userCount; i++) {
            for (int j = i + 1; j < userCount; j++) {
                int coLocNum = Arrays.stream(coVector[i][j]).sum();
                if (coLocNum == 0) continue;
                double tempValue = 0.0;
                for (int k = 0; k < locationCount; k++) {
                    if (coVector[i][j][k] == 0) continue;
                    double probability = coVector[i][j][k] / (double) coLocNum;
                    tempValue += Math.pow(probability, diversityOrder);
                }
                double renyiEntropy = -Math.log(tempValue) / (diversityOrder - 1);
                renyiEntropyVector[i][j] = renyiEntropy;
                renyiEntropyVector[j][i] = renyiEntropy;
            }
        }
    }

    /**
     * compute diversity for each pair based on renyi entropy
     */
    private void computeDiversity() {
        computeRenyiEntropy();
        diversityVector = new double[userCount][userCount];
        for (int i = 0; i < userCount; i++) {
            for (int j = i + 1; j < userCount; j++) {
                double diversity = Math.exp(renyiEntropyVector[i][j]);
                diversityVector[i][j] = diversity;
                diversityVector[j][i] = diversity;
            }
        }
    }

    /**
     * compute location entropy for each location
     */
    private void computeLocationEntropy() {
        locationEntropyVector = new double[locationCount];
        for (int i = 0; i < locationCount; i++) {
            int totalUserNumPerLoc = Arrays.stream(locUserVector[i]).sum();
            if (totalUserNumPerLoc == 0) continue;
            double locationEntropy = 0.0;
            for (int j = 0; j < userCount; j++) {
                if (locUserVector[i][j] == 0) continue;
                double probability = locUserVector[i][j] / (double) totalUserNumPerLoc;
                locationEntropy += (-probability * Math.log(probability));
            }
            locationEntropyVector[i] = locationEntropy;
        }
    }

    /**
     * compute weighted frequency for each pair
     */
    private void computeWeightedFrequency() {
        computeLocationEntropy();
        weightedFrequencyVector = new double[userCount][userCount];
        for (int i = 0; i < userCount; i++) {
            for (int j = i + 1; j < userCount; j++) {
                double weightedFrequency = 0.0;
                for (int k = 0; k < locationCount; k++) {
                    weightedFrequency += (coVector[i][j][k] * Math.exp(-locationEntropyVector[k]));
                }
                weightedFrequencyVector[i][j] = weightedFrequency;
                weightedFrequencyVector[j][i] = weightedFrequency;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String filePath = Objects.requireNonNull(SocialStrengthInfer.class.getResource("/checkin.txt")).getPath();
        FileInputStream in = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader bufferedReader = new BufferedReader(reader);
        List<CheckIn> checkIns = new ArrayList<>();
        bufferedReader.readLine();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] items = line.split(",");
            checkIns.add(new CheckIn(Integer.parseInt(items[0]) - 1, Integer.parseInt(items[1]) - 1, Long.parseLong(items[2])));
        }

        SocialStrengthInfer socialStrengthInfer = new SocialStrengthInfer(3, 10, checkIns, 1.0);
        double[][] res = socialStrengthInfer.computeSocialStrength();
        System.out.println(res[0][1]);  // 6.9183160963350545
    }
}

/**
 * check-in data including user id, location id, and timestamp in 10 digit
 *
 * @author yuzisheng
 * @date 2023-11-04
 */
class CheckIn {
    /**
     * user id
     */
    int uid;
    /**
     * location id
     */
    int lid;
    /**
     * 10 digit timestamp is used to represent check-in time in seconds
     */
    long time;

    public CheckIn(int uid, int lid, long time) {
        this.uid = uid;
        this.lid = lid;
        this.time = time;
    }

    public int getUid() {
        return uid;
    }

    public int getLid() {
        return lid;
    }

    public long getTime() {
        return time;
    }
}
