package gmail.vladimir.JLSP.Parser;

import gmail.vladimir.JLSP.Helpers.TempList;
import gmail.vladimir.JLSP.Helpers.TempMap;
import gmail.vladimir.JLSP.Interfaces.FunctionCompute;
import gmail.vladimir.JLSP.Variables.FormulaEntity;

/**
 * Helper class used by the parser to store information such as current value, operation, last entity, last operation, func information, etc.
 */
public class ParsingState {
    TempList inOrder = new TempList();
    TempMap inOperationOrder;
    Double currentValue;
    Character currentOperation;
    char parenthesesOperation;
    boolean isNegative;
    boolean hasDecimalPoint;
    double decimalPlace;
    int lastOperationPriority = 0;
    char lastOperation;
    FormulaEntity<?> lastAddedEntity = null;
    boolean started = false;
    StringBuilder funcString = new StringBuilder(16);
    Character preFuncOp = null;
    TempList funVars = new TempList();
    FunctionCompute func = null;
    boolean isFunc = false;
    FormulaEntity<?> lastEntityBeforeFunc = null;
    Character lastOperationBeforeFunc;
    int lastOperationPriorityBeforeFunc = 0;

    public ParsingState(Parser parser){finalFullReset(parser, true);}

    void reset(){
        currentValue = null;
        currentOperation = null;
        isNegative = false;
        hasDecimalPoint = false;
        decimalPlace = 0.1;
        started = false;
    }

    private void fullReset(Parser parser){
        reset();
        lastOperation = parser.getDefaultOperator();
        lastAddedEntity = null;
    }

    void finalFullReset(Parser parser, boolean newMap){
        fullReset(parser);
        inOrder.clear();
        parenthesesOperation = parser.getBetweenVariables();
        lastOperationPriority = 0;
        funcString.setLength(0);
        preFuncOp = null;
        funVars.clear();
        func = null;
        isFunc = false;
        lastEntityBeforeFunc = null;
        lastOperationBeforeFunc = null;
        lastOperationPriorityBeforeFunc = 0;

        if(newMap)
           inOperationOrder = new TempMap(parser.opIndexMapSize);
    }

    void dump(){
        inOrder.clearTemp();
        inOperationOrder.clearLists(true);
    }

    void resetFunc(){
        funcString.setLength(0);
        preFuncOp = null;
        if(!isFunc)
            funVars.clear();
        func = null;
    }

    void clearTemp(){
        inOrder.removeTemp();
        inOperationOrder.clearLists(false);
    }

    void addFuncVariable(Parser parser){
        dump();
        funVars.add(parser.finishFormula(this, parenthesesOperation));
        inOrder.clear();

        inOperationOrder.clear();
        fullReset(parser);
    }
}
