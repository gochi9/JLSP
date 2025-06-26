package gmail.vladimir.JLSP.Interfaces;

/**
 * Interface used to store operator logic
 */
@FunctionalInterface
public interface OperatorCompute {

    /**
     * Holds the left and right variables, and extra flags
     * @param extra Flags used by the default logic to calculate proper '^' logic, can be safely ignored.
     * @return The result
     */
    double compute(double left, double right, boolean... extra);

}
