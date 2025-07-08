package gmail.vladimir.JLSP.Variables;

import gmail.vladimir.JLSP.Interfaces.NeedsRoot;

import java.util.Set;

/**
 * A function that can hold a multiple variables and its own ID
 */
public class Function extends FormulaEntity<Double> implements NeedsRoot {

    private final String id;
    private final FormulaEntity<?>[] params;
    private final int paramSize;

    public Function(String id, FormulaEntity<?>[] params, int size, char precedingSymbol){
        super(0D, precedingSymbol);
        this.id = id;
        this.params = new FormulaEntity<?>[size];
        this.paramSize = size;
        for(int i = 0; i < size; i++)
            this.params[i] = params[i];
    }

    public String getId(){
        return id;
    }

    public FormulaEntity<?>[] getParams(){
        return params;
    }

    public void setRoot(Formula root, Set<Character> vars){
        for(int i = 0; i < paramSize; i++){
            FormulaEntity<?> entity = params[i];
            if(entity instanceof NeedsRoot)
                ((NeedsRoot)entity).setRoot(root, vars);
//            break;
        }
    }
}
