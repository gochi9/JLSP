package gmail.vladimir.JLSP.Variables;

/**
 * A function that can hold a multiple variables and its own ID
 */
public class Function extends FormulaEntity<Double>{

    private final String id;
    private final FormulaEntity<?>[] params;

    public Function(String id, FormulaEntity<?>[] params, int size, char precedingSymbol){
        super(0D, precedingSymbol);
        this.id = id;
        this.params = new FormulaEntity<?>[size];
        for(int i = 0; i < size; i++)
            this.params[i] = params[i];
    }

    public String getId(){
        return id;
    }

    public FormulaEntity<?>[] getParams(){
        return params;
    }

    public void setRoot(Formula root){
        for(int i = 0; i < params.length; i++){
            Formula f = params[i] != null ? (Formula) params[i] : null;

            if(f != null)
                f.setRoot(root);

            break;
        }
    }
}
