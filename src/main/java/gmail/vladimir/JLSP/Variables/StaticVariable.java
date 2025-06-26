package gmail.vladimir.JLSP.Variables;

/**
 * A static variable is a simple double, nothing special about this one
 */
public class StaticVariable extends FormulaEntity<Double> {

    public StaticVariable(Double value, char precedentSymbol) {
        super(value, precedentSymbol);
    }

}
