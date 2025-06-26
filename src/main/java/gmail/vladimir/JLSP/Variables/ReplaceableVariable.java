package gmail.vladimir.JLSP.Variables;

import java.util.Objects;

/**
 * A replaceable variable is any single use character (that is not already a comma/operator/delimiter) that will be used as a placeholder for a real value
 */
public class ReplaceableVariable extends FormulaEntity<Character>{

    private final boolean isNegative;
    private short index;

    public ReplaceableVariable(Character value, char precedentSymbol, boolean isNegative) {
        super(value, precedentSymbol);
        this.isNegative = isNegative;
    }

    public final boolean isNegative() {
        return isNegative;
    }

    public final int getIndex() {
        return index;
    }

    public final void setIndex(short index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplaceableVariable)) return false;
        if (!super.equals(o)) return false;
        ReplaceableVariable that = (ReplaceableVariable) o;
        return this.value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

}
