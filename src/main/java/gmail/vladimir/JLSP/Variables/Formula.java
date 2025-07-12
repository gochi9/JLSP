package gmail.vladimir.JLSP.Variables;

import gmail.vladimir.JLSP.Helpers.TempList;
import gmail.vladimir.JLSP.Interfaces.NeedsRoot;
import gmail.vladimir.JLSP.Pairs.ResultPair;
import gmail.vladimir.JLSP.Parser.Parser;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * The main holder of all variables(formula entities).<br/>
 * Can be a variable itself when used in nested formulas
 */
public class Formula extends FormulaEntity<Double> implements NeedsRoot {

    private final FormulaEntity<?>[] inOrder, inOperationOrder, lowestPriority;
    private final int inOrderSize;
    private final int inOperationOrderSize;
    private final int lowestPrioritySize;

    private final char[] variableNames;
    private final double[] variableValues;
    private final boolean[] variableSet;
    private final HashMap<Character, Integer> variableIndex;

    private boolean addedVariables;
    private double lastNaiveCachedResult = 0, lastInOrderCachedResult = 0;
    private boolean haveVariablesChangedNaive = true, haveVariablesChangedInOrder = true;

    private final Parser parser;

    /**
     * Used for creating new Formula instances
     */
    public Formula(FormulaEntity<?>[] inOrder, int inOrderSize, FormulaEntity<?>[] inOperationOrder, int inOperationOrderSize, FormulaEntity<?>[] lowestPriority, int lowestPrioritySize, Parser parser) {
        this(inOrder, inOrderSize, inOperationOrder, inOperationOrderSize, lowestPriority, lowestPrioritySize, null, parser);
    }

    /**
     * Main constructor
     */
    public Formula(FormulaEntity<?>[] inOrder, int inOrderSize, FormulaEntity<?>[] inOperationOrder, int inOperationOrderSize, FormulaEntity<?>[] lowestPriority, int lowestPrioritySize, LinkedHashMap<Character, Double> replacedVariables, Parser parser) {
        super(0D, parser.getDefaultOperator());
        this.parser = parser;
        this.inOrder = Arrays.copyOf(inOrder, inOrderSize);
        this.inOperationOrder = Arrays.copyOf(inOperationOrder, inOperationOrderSize);
        this.lowestPriority = Arrays.copyOf(lowestPriority, lowestPrioritySize);
        this.inOrderSize = inOrderSize;
        this.inOperationOrderSize = inOperationOrderSize;
        this.lowestPrioritySize = lowestPrioritySize;

        Set<Character> vars = new LinkedHashSet<>(32);
        for (int i = 0; i < inOrderSize; i++) {
            FormulaEntity<?> fe = inOrder[i];
            if (fe instanceof ReplaceableVariable)
                vars.add(((ReplaceableVariable) fe).getValue());
            else if (fe instanceof NeedsRoot)
                ((NeedsRoot) fe).setRoot(this, vars);
        }

        int nVars = vars.size();
        this.variableNames = new char[nVars];
        this.variableValues = new double[nVars];
        this.variableIndex = new HashMap<>(nVars);
        this.variableSet = new boolean[nVars];

        int idx = 0;
        boolean mapNull = replacedVariables == null;
        for (Character c : vars) {
            variableNames[idx] = c;
            variableValues[idx] = (!mapNull && replacedVariables.getOrDefault(c, null) != null) ? replacedVariables.get(c) : 0;
            variableIndex.put(c, idx);
            variableSet[idx++] = !mapNull && replacedVariables.getOrDefault(c, null) != null;
        }

        assignVariableIndices(inOrder, inOrderSize);

        this.addedVariables = isValid();
        this.root = null;
    }

    private void assignVariableIndices(FormulaEntity<?>[] list, int size) {
        for (int i = 0; i < size; i++) {
            FormulaEntity<?> fe = list[i];
            if (fe instanceof ReplaceableVariable) {
                ReplaceableVariable rv = (ReplaceableVariable) fe;
                Integer idx = variableIndex.get(rv.getValue());
                if (idx != null) rv.setIndex(idx.shortValue());
            }
        }
    }

    private Formula root = null;

    /**
     * Used when this instance gets used as a variable
     */
    public Formula(char last, FormulaEntity<?>[] inOrder, int inOrderSize, FormulaEntity<?>[] inOperationOrder, int inOperationOrderSize, FormulaEntity<?>[] lowestPriority, int lowestPrioritySize, Parser parser){
        super(0D, last);
        this.parser = parser;
        this.inOrder = Arrays.copyOf(inOrder, inOrderSize);
        this.inOperationOrder = Arrays.copyOf(inOperationOrder, inOperationOrderSize);
        this.lowestPriority = Arrays.copyOf(lowestPriority, lowestPrioritySize);
        this.inOrderSize = inOrderSize;
        this.inOperationOrderSize = inOperationOrderSize;
        this.lowestPrioritySize = lowestPrioritySize;
        this.variableNames = new char[0];
        this.variableValues = new double[0];
        this.variableSet = new boolean[0];
        this.variableIndex = new HashMap<>();
        this.addedVariables = true;
    }

    /**
     * Recursively sets the child formula a reference to the root formula
     */
    public void setRoot(Formula root, Set<Character> vars){
        this.root = root;

        if(inOrderSize == 0)
            return;

        for (int i = 0; i < inOrderSize; i++) {
            FormulaEntity<?> entity = inOrder[i];
            if (entity instanceof NeedsRoot)
                ((NeedsRoot)entity).setRoot(root, vars);
            else if (entity instanceof ReplaceableVariable)
                vars.add(((ReplaceableVariable)entity).getValue());
        }
    }

    /**
     * Returns the main(root) formula this instance is part of, or null if this is the root formula
     */
    public Formula getRoot(){
        return root;
    }

    /**
     * Prints a list of the replaceable variables. Shows both values that are empty and filled
     */
    public String getVariablesString(){
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < variableNames.length; i++)
            joiner.add(variableNames[i] + "=" + variableValues[i]);
        return joiner.toString();
    }

    public FormulaEntity<?>[] getInOrder() {
        return inOrder;
    }

    public Set<Character> getVariables(){
        int size = variableNames.length;
        Set<Character> vars = new LinkedHashSet<>(size + 4, 1f);
        for (int i = 0; i < variableNames.length; i++)
            vars.add(variableNames[i]);
        return vars;
    }

    /**
     * Returns the number of replaceable variables this formula has
     */
    public int getRequiredVariables(){
        return variableNames.length;
    }

    /**
     * Sets the value for certain replaceable variables. The values are stored in the order they first appear<br/>
     * So, x+1+y*x, would be stored as {x, y}, meaning the first value in variables will be bound to x and the second to y<br/>
     * If more values are provided than those that are required, they'll just be ignored.
     */
    public Formula setVariables(double... variables){
        int size = getRequiredVariables();
        if(variables.length < size)
            throw new IllegalArgumentException("The number of variables must be the same as the number of variables in the formula");
        for (int i = 0; i < size; ++i){
            variableValues[i] = variables[i];
            variableSet[i] = true;
        }
        addedVariables = true;
        resetCache();
        return this;
    }

    /**
     * Attempts to set a value to a certain variable that is bound to the provided char c
     *
     * @return The same object for easy access to chain other similar methods, or easy access to the result functions
     */
    public Formula setVariable(char c, double value) {
        return setVariable(c, value, false);
    }

    /**
     * Attempts to set a value to a certain variable that is bound to the provided char c
     *
     * @return The same object for easy access to chain other similar methods, or easy access to the result functions
     * @param log If true, it'll send log a message whether the character does not exist, or if the formula is ready to be used after this character has been added, or if it's still missing values
     */
    public Formula setVariable(char c, double value, boolean log){
        Integer idx = variableIndex.get(c);
        if(idx == null){
            if(log)
                System.out.println("Variables " + c + " does not exist in the formula");
            return this;
        }
        variableValues[idx] = value;
        variableSet[idx] = true;
        boolean valid = isValid();
        if(valid)
            addedVariables = true;

        if(log) {
            if(!valid)
                System.out.println("Formula still missing variables");
            else
                System.out.println("Formula has all necessary variables");
        }

        resetCache();
        return this;
    }

    private boolean isValid() {
        for (int i = 0; i < variableSet.length; i++)
            if (!variableSet[i])
                return false;

        return true;
    }

    /**
     * Returns the naive result of this formula.<br/>
     * 'Naive' computing simply gives you the result in the order the operation appear, not taking into account operation order<br/>
     * If this method has been called before, and nothing about the formula has changed (e.g. variables), then this will return a cached result
     */
    public double naiveResult(double... variables){
        if(variables.length > 0)
            setVariables(variables);
        return root == null && !haveVariablesChangedNaive ? lastNaiveCachedResult : result(false);
    }

    /**
     * Returns the naive result of this formula.<br/>
     * 'Naive' computing simply gives you the result in the order the entities appear, not taking into account operation order<br/>
     * If this method has been called before, and nothing about the formula has changed (e.g. variables), then this will return a cached result
     * @return A result pair consisting of a boolean and a completable future.<br/><br/>
     * The boolean value states whether the calculation is taking place (true) or a cached result has been returned (false)<br/><br/>
     * The CompletableFuture will compute async if there was no cache found, or instantly return a completed future if a cache was present
     */
    public ResultPair naiveResultAsync(){
        return root == null && !haveVariablesChangedNaive ? new ResultPair(false, CompletableFuture.completedFuture(lastNaiveCachedResult)) : new ResultPair(true, CompletableFuture.supplyAsync(this::naiveResult));
    }

    /**
     * Returns the in operation order result of this formula.<br/>
     * If this method has been called before, and nothing about the formula has changed (e.g. variables), then this will return a cached result
     */
    public double inOperationOrderResult(double... variables){
        if(variables.length > 0)
            setVariables(variables);

        if(root != null && !haveVariablesChangedInOrder)
            return lastInOrderCachedResult;
        else
            return result(true);
    }

    /**
     * Returns the in operation order result of this formula.<br/>
     * If this method has been called before, and nothing about the formula has changed (e.g. variables), then this will return a cached result
     * @return A result pair consisting of a boolean and a completable future.<br/><br/>
     * The boolean value states whether the calculation is taking place (true) or a cached result has been returned (false)<br/><br/>
     * The CompletableFuture will compute async if there was no cache found, or instantly return a completed future if a cache was present
     */
    public ResultPair inOperationOrderResultAsync(){
        return root == null && !haveVariablesChangedInOrder ? new ResultPair(false, CompletableFuture.completedFuture(lastInOrderCachedResult)) : new ResultPair(true, CompletableFuture.supplyAsync(this::inOperationOrderResult));
    }

    /**
     * Main compute logic for formulas. This will never return a cached value.
     * Both {@link Formula#naiveResult(double...)} and {@link Formula#inOperationOrderResult(double...)} call upon this method, and they may have a cached value for faster use
     *
     * @param inOperationOrder Whether in operation order (true), or naive (false) logic will be used
     * @return The result
     */
    public double result(boolean inOperationOrder) {
        if(!addedVariables)
            throw new IllegalArgumentException("The replacement values cannot be empty");

        double result = 0;
        TempList extra = null;
        if(inOperationOrder && inOperationOrderSize > 0) {
            FormulaEntity<?> entity = this.inOperationOrder[0];
            double tempRes = parser.processEntity(this, entity, true);
            char c = entity.getPrecedentSymbol();
            extra = new TempList(inOperationOrderSize+2);
            boolean wasFormula  = entity instanceof Formula;
            boolean innerNeg = tempRes < 0.0D;
            boolean outerMinus = wasFormula && c == '-';

            for(int i = 1; i < inOperationOrderSize; ++i) {
                entity = this.inOperationOrder[i];

                int prio = parser.getOperatorPriority(entity.getPrecedentSymbol());

                if(prio == 0) {
                    extra.add(new StaticVariable(tempRes, parser.getDefaultOperator()));
                    tempRes = 0D;
                }

                if(prio > 0 && entity.getPrecedentSymbol() != c)
                    c = entity.getPrecedentSymbol();

                double val = parser.processEntity(this, entity, true);
                tempRes = parser.compute(entity.getPrecedentSymbol(), tempRes, val, wasFormula, innerNeg, outerMinus);
                wasFormula = entity instanceof Formula;
                innerNeg = val < 0.0D;
                outerMinus = wasFormula && entity.getPrecedentSymbol() == '-';
            }
            extra.add(new StaticVariable(tempRes, parser.getDefaultOperator()));
        }

        final FormulaEntity<?>[] toUse = inOperationOrder ? lowestPriority : inOrder;
        final int s = inOperationOrder ? lowestPrioritySize : inOperationOrderSize;
        result = processChar(toUse, s, result, inOperationOrder);

        if(extra != null)
            result = processChar(extra.getArray(), extra.size(), result, true);

        if(inOperationOrder) {
            lastInOrderCachedResult = result;
            if(root == null) haveVariablesChangedInOrder = false;
        }
        else {
            lastNaiveCachedResult = result;
            if(root == null) haveVariablesChangedNaive = false;
        }

        return result;
    }

    private double processChar(FormulaEntity<?>[] list, int n, double result, boolean inOperationOrder) {
        //Copied in case the default ^ implementation is changed to have 0 priority
        boolean wasFormula = false;
        boolean innerNeg = false;
        boolean outerMinus = false;
        for(int i = 0; i < n; ++i) {
            FormulaEntity<?> entity = list[i];
            double val = parser.processEntity(this, entity, inOperationOrder);
            result = parser.compute(entity.getPrecedentSymbol(), result, val, wasFormula, innerNeg, outerMinus);

            wasFormula = entity instanceof Formula;
            innerNeg = val < 0.0D;
            outerMinus = wasFormula && entity.getPrecedentSymbol() == '-';
        }
        return result;
    }

    public void resetCache(){
        haveVariablesChangedNaive = true;
        haveVariablesChangedInOrder = true;
        lastNaiveCachedResult = 0;
        lastInOrderCachedResult = 0;
    }

    /**
     * Naive cache if present, or null
     */
    public Double getLastNaiveCacheResult(){
        return (root == null ? !haveVariablesChangedNaive : root.haveVariablesChangedNaive) ? lastNaiveCachedResult : null;
    }

    /**
     * In operation order cache if present, or null
     */
    public Double getInOrderCacheResult(){
        return (root == null ? !haveVariablesChangedInOrder : root.haveVariablesChangedInOrder) ? lastInOrderCachedResult : null;
    }

    /**
     * Returns the replaceable variables current values in the order that they first appear in the formula
     * So, x+1+y*x, would be stored as {x, y}, meaning the first value returned would be x and the second y.
     *
     * @return The replaceable variables current values
     */
    public double[] getVariableValues(){
        return variableValues;
    }

    /**
     * Clones the current object without copying the replaced variable values
     * @return A new separate cloned instance from this object
     */
    @Override
    public Formula clone(){
        return clone(true);
    }

    /**
     * Clones the current object with may be copying the replaced variable values
     *
     * @param cloneReplacementValues Whether the replaced variable values will be copied
     * @return A new separate cloned instance from this object
     */
    public Formula clone(boolean cloneReplacementValues){
        if(!cloneReplacementValues)
            return new Formula(inOrder, inOrderSize, inOperationOrder, inOperationOrderSize, lowestPriority, lowestPrioritySize, null, parser);

        char[] names = Arrays.copyOf(variableNames, variableNames.length);
        double[] values = Arrays.copyOf(variableValues, variableValues.length);
        LinkedHashMap<Character, Double> map = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++)
            map.put(names[i], values[i]);
        return new Formula(inOrder, inOrderSize, inOperationOrder, inOperationOrderSize, lowestPriority, lowestPrioritySize, map, parser);
    }

    //Internal use only
    public FormulaEntity<?> getLast(){
        FormulaEntity<?> last = inOrder[inOrderSize - 1];

        if(!(last instanceof Formula))
            return last;

        return ((Formula)last).getLast();
    }

    //Internal use only
    public void setLast(FormulaEntity<?> entity){
        FormulaEntity<?> last = inOrder[inOrderSize - 1];

        if(last instanceof Formula){
            ((Formula)last).setLast(entity);
            return;
        }

        inOrder[inOrderSize - 1] = entity;
        inOperationOrder[inOperationOrderSize - 1] = entity;
    }
}
