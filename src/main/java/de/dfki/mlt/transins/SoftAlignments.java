package de.dfki.mlt.transins;

import java.util.ArrayList;
import java.util.List;

/**
 * Provide token based alignments based on Marian NMT soft alignments.
 *
 * @author Jörg Steffen, DFKI
 */
public class SoftAlignments implements Alignments {

  // alignment scores;
  // first index is the target token index, second index is the source token index
  private double[][] alignmentScores;

  // shifts in target and source token indexes
  private int targetIndexOffset = 0;
  private int sourceIndexOffset = 0;


  /**
   * Create a new soft alignment instance based on the given raw soft alignments.
   *
   * @param rawAlignments
   *          the raw soft alignments, as provided by Marian NMT
   * @throws NumberFormatException
   *           if raw alignments not as excepted
   */
  public SoftAlignments(String rawAlignments) {

    if (rawAlignments.trim().isEmpty()) {
      // no alignments provided
      return;
    }

    // we have one line for each target token
    String[] targetLines = rawAlignments.split(" ");
    int numOfTargetTokens = targetLines.length;
    int numOfSourceTokens = targetLines[0].split(",").length;
    this.alignmentScores = new double[numOfTargetTokens][numOfSourceTokens];

    for (int targetIndex = 0; targetIndex < targetLines.length; targetIndex++) {
      String oneTargetLine = targetLines[targetIndex];
      String[] sourceTokenScores = oneTargetLine.split(",");
      for (int sourceIndex = 0; sourceIndex < sourceTokenScores.length; sourceIndex++) {
        this.alignmentScores[targetIndex][sourceIndex] =
            Double.parseDouble(sourceTokenScores[sourceIndex]);
      }
    }
  }


  /**
   * @return the underlying alignment scores; {@code null} if not initialized
   */
  public double[][] getAlignmentScores() {

    return this.alignmentScores;
  }


  /**
   * @return the target index offset
   */
  public int getTargetIndexOffset() {

    return this.targetIndexOffset;
  }


  /**
   * @return the source index offset
   */
  public int getSourceIndexOffset() {

    return this.sourceIndexOffset;
  }


  /**
   * Shift all source indexes by the given offset.
   *
   * @param offset
   *          the offset to shift
   */
  @Override
  public void shiftSourceIndexes(int offset) {

    this.sourceIndexOffset += offset;
  }


  /**
   * Shift all target indexes by the given offset.
   *
   * @param offset
   *          the offset to shift
   */
  @Override
  public void shiftTargetIndexes(int offset) {

    this.targetIndexOffset += offset;
  }


  @Override
  public List<Integer> getSourceTokenIndexes(int targetTokenIndex) {

    List<Integer> result = new ArrayList<>();
    result.add(getBestSourceTokenIndex(targetTokenIndex));
    return result;
  }


  /**
   * @param targetTokenIndex
   *          the target token index in the translation
   * @return the source token index with the highest alignment score;
   *         -1 if no source token has alignment score above threshold
   */
  public int getBestSourceTokenIndex(int targetTokenIndex) {

    return getBestSourceTokenIndex(targetTokenIndex, 0.0);
  }


  /**
   * @param targetTokenIndex
   *          the target token index in the translation
   * @param threshold
   *          the threshold score
   * @return the source token index with the highest alignment score equal or above threshold;
   *         -1 if no source token has alignment score above threshold
   */

  public int getBestSourceTokenIndex(int targetTokenIndex, double threshold) {

    double maxScore = 0.0;
    int maxScoreIndex = -1;
    for (int i = 0; i < this.alignmentScores[targetTokenIndex].length; i++) {
      double algnScore = this.alignmentScores[targetTokenIndex][i];
      if (algnScore >= threshold && algnScore > maxScore) {
        maxScore = algnScore;
        maxScoreIndex = i;
      }
    }
    return maxScoreIndex + this.sourceIndexOffset;
  }


  /**
   * @param targetTokenIndex
   *          the target token index in the translation
   * @param threshold
   *          the threshold score
   * @return the source token indexes with an alignment score equals or above threshold, sorted;
   *         empty if no source token has alignment score above threshold
   */
  public List<Integer> getSourceTokenIndexes(int targetTokenIndex, double threshold) {

    List<Integer> sourceTokenIndexes = new ArrayList<>();

    for (int i = 0; i < this.alignmentScores[targetTokenIndex].length; i++) {
      double algnScore = this.alignmentScores[targetTokenIndex][i];
      if (algnScore >= threshold) {
        sourceTokenIndexes.add(i + this.sourceIndexOffset);
      }
    }

    return sourceTokenIndexes;
  }


  /**
   * @param threshold
   *          the alignment scores threshold
   * @return the token index alignments from source tokens to target tokens, in the same format
   *         as returned by Marian NMT with hard alignments
   */
  public String toHardAlignments(double threshold) {

    StringBuilder result = new StringBuilder();

    // iterate over source tokens
    for (int sourceIndex = 0; sourceIndex < this.alignmentScores[0].length; sourceIndex++) {
      for (int targetIndex = 0; targetIndex < this.alignmentScores.length; targetIndex++) {
        if (this.alignmentScores[targetIndex][sourceIndex] >= threshold) {
          result.append(String.format("%d-%d ",
              sourceIndex + this.sourceIndexOffset,
              targetIndex + this.targetIndexOffset));
        }
      }
    }
    return result.toString().trim();
  }


  /**
   * @return the token index alignments from target tokens to single best source tokens,
   *         independent of the actual alignment score
   */
  public String toBestAlignments() {

    StringBuilder result = new StringBuilder();

    // iterate over source tokens
    for (int targetIndex = 0; targetIndex < this.alignmentScores.length; targetIndex++) {
      int maxSourceIndex = -1;
      double maxSourceScore = 0.0;
      for (int sourceIndex = 0; sourceIndex < this.alignmentScores[targetIndex].length;
          sourceIndex++) {
        if (this.alignmentScores[targetIndex][sourceIndex] > maxSourceScore) {
          maxSourceIndex = sourceIndex;
          maxSourceScore = this.alignmentScores[targetIndex][sourceIndex];
        }
      }
      result.append(String.format("%d-%d ",
          targetIndex + this.targetIndexOffset,
          maxSourceIndex + this.sourceIndexOffset));
    }
    return result.toString().trim();
  }


  @Override
  public String toString() {

    return toBestAlignments();
  }
}
