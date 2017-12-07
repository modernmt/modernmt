package eu.modernmt.processing.string;

/**
 * created by andrea on 25/02/17
 * <p>
 * An IndexMap object maps the position of each character in the SentenceBuilder current String
 * to the positions of the corresponding character in the SentenceBuilder original string.
 * <p>
 * The IndexMap thus stores information from all the editing activities committed so far on the current String.
 * When the SentenceBuilder starts working on a new string,
 * the IndexMap is just re-initialized instead of creating a new one,
 * in order to save memory and time:
 * a SentenceBuilder uses one and only IndexMap object during its whole lifecycle.
 */
public class IndexMap {
    /* array that actually represents the mapping:
    for each int i representing a position in the SentenceBuilder current String,
    positions[i] represents the corresponding position in the SentenceBuilder original String*/
    private int[] positions;
    /* Length of the relevant portion in the positions array;
    * (positions is managed as a buffer, so as a data structure it has constant length.
    * Nonetheless, its constant length (positions.length) may not be the same as the length of its relevant portion)*/
    private int arrayLength;

    /**
     * This constructor generates an empty IndexMap object.
     * The positions array is set to null, and its length to 0.
     * <p>
     * This constructor should only be called
     * during the creation of the SentenceBuilder,
     * before the processing of any string.
     */
    public IndexMap() {
        this.positions = null;
        this.arrayLength = 0;
    }

    /**
     * This constructor generates an IndexMap object
     * based on the length of the string to process.
     */
    public IndexMap(int length) {
        this();
        this.initialize(length);
    }

    /**
     * This method initializes an IndexMap object
     * by setting its length and populating the positions array,
     * on the basis of the length of the string to edit.
     * <p>
     * The positions array length must be equal to the string length + 1
     * (this lets us use the substring method on the last part of the string too).
     * Therefore, if the current version of the positions array is too small
     * for the current string, it is replaced by a larger array.
     * Anyway its content is set so that for each i, positions[i] holds i itself.
     * <p>
     * A new IndexMap, in fact, since no edits have been committed yet,
     * just maps the content of the SentenceBuilder original String with itself.
     * (because the original string is the first version of the currentString).
     *
     * @param stringLength is the length of the original string to process.
     */
    public void initialize(int stringLength) {

        this.arrayLength = stringLength + 1;

        /*if necessary, create a new positions array*/
        if (this.positions == null || this.positions.length < this.arrayLength)
            this.positions = new int[this.arrayLength];

        /*positions is initialized so that in each position i it contains the value i itself:
        * therefore, at the very beginning it maps the original string with itself*/
        for (int i = 0; i < this.arrayLength; i++) {
            positions[i] = i;
        }
    }

    /**
     * This method transforms the current positions array
     * by taking its portion between start (included) and end (not included)
     * and making it become as long as newLength says.
     * It is only invoked when a replacement is performed by the Editor.
     * <p>
     * Note: at the moment, only non-void replacements are allowed
     * (you can not replace something with "")
     * except when the void replacement takes places at the start or at the end of the string
     *
     * @param start     the starting position of the array portion to transform
     * @param end       the position that follows the end of the portion to transform
     * @param newLength the length that the portion under analysis must assume
     * @return the new version of the positions array
     */
    public void update(int start, int end, int newLength) {

        /*the length of the text to replace*/
        int oldLength = end - start;
        /*the new length that the positions array must assume*/
        this.arrayLength = this.arrayLength - oldLength + newLength;

        /*if the replacement is longer than the text to replace*/
        if (newLength > oldLength) {
            /*if necessary, replace the whole array with a new bigger one*/
            if (this.arrayLength > this.positions.length) {
                int[] newArray = new int[arrayLength];
                System.arraycopy(this.positions, 0, newArray, 0, start);
                this.positions = newArray;
            }

            /*shift rightwards the array portion that follows the end of the text to replace*/
            shiftPortion(end, this.arrayLength, newLength - oldLength);
            /*update the positions between start and the new end
            * with values that distribute proportionally in relation to
            * the values at start and at the end of the new portion*/
            double ratio = ((double) newLength) / oldLength;
            for (int i = 1; i < newLength; i++) {
                this.positions[start + i] = (int) Math.round(this.positions[start] + ((this.positions[start + i] - positions[start]) * ratio));
            }
        }

        /*else, the replacement is shorter than the text to replace*/
        else if (newLength < oldLength) {
            /*first update the portion between start and the new end
            * with values that distribute proportionally in relation to
            * the values at start and at the end of the new portion*/
            double ratio = ((double) newLength) / oldLength;
            for (int i = 1; i < newLength; i++) {
                this.positions[start + i] = (int) Math.round(this.positions[start] + ((this.positions[start + i] - positions[start]) * ratio));
            }

            /*after that shift leftwards the whole portion of the array
            * that comes after the end of the text to replace*/

            /*if the replacement length is 0, it means we are
            either at the beginning of the string or at its end*/
            if (newLength == 0) {
                /*if this is the beginning of the string
                * actually we can do the same update we usually make*/
                if (start == 0) {
                    shiftPortion(end, this.positions.length, newLength - oldLength);
                }
                /*if this is the end of the string, actually, we don't need to do anything,
                because we have already updated the array length
                and we must not change the value of its "new" last cell.
                So we don't even need a case "else { ... } " */
            } else {
                shiftPortion(end, this.positions.length, newLength - oldLength);
            }
        }
    }

    /**
     * This method selects a portion of the array and
     * and shifts it backwards or forwards for a certain amount of positions.
     *
     * @param start     the index of the first position of the portion to shift
     * @param end       the index of the position that follows the end of the portion to shift
     * @param shiftStep the amount of positions that the portion must be shifted of.
     *                  if it is greater than 0, the shift must be shifted forwards;
     *                  if it is smaller than 0 the portion must be shifted backwards.
     */
    private void shiftPortion(int start, int end, int shiftStep) {
        int portionSize = end - start;
        int newStart = start + shiftStep;
        System.arraycopy(this.positions, start, this.positions, newStart, portionSize);
    }

    /**
     * Method that returns the value stored in a certain position of the indexMap array
     *
     * @param index the position where the target value is stored
     * @return the target value
     */
    public int get(int index) {
        return this.positions[index];
    }

    /**
     * Method that overwrites the value stored in a certain position of the indexMap array
     *
     * @param index the position the value that must be overwritten is stored
     * @param value the new value to write in position index
     */
    public void put(int index, int value) {
        this.positions[index] = value;
    }
}