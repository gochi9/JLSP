package gmail.vladimir.JLSP.Variables;

/**
 * Defines an object as a valid entity that can be used to retrieve some sort of value that can be used in solving the equation
 */
public abstract class FormulaEntity<T> {

    protected T value;
    private char precedentSymbol;

    public FormulaEntity(T value, char precedentSymbol){
        this.value = value;
        this.precedentSymbol = precedentSymbol;
    }

    public final T getValue(){
        return value;
    }

    public final char getPrecedentSymbol(){
        return precedentSymbol;
    }
    
    public final void setPrecedentSymbol(char precedentSymbol){
        this.precedentSymbol = precedentSymbol;
    }

}
