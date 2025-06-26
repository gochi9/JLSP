package gmail.vladimir.JLSP.Interfaces;

import gmail.vladimir.JLSP.Variables.Formula;
import gmail.vladimir.JLSP.Variables.FormulaEntity;

/**
 * Interface used to store function logic
 */
@FunctionalInterface
public interface FunctionCompute {

    double compute(Formula caller, boolean inOperationOrder, FormulaEntity<?>... entities);

}
