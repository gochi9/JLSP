package gmail.vladimir.JLSP.Parser;

import gmail.vladimir.JLSP.Helpers.NameSearcher;
import gmail.vladimir.JLSP.Helpers.TempList;
import gmail.vladimir.JLSP.Helpers.TempMap;
import gmail.vladimir.JLSP.Interfaces.FunctionCompute;
import gmail.vladimir.JLSP.Interfaces.OperatorCompute;
import gmail.vladimir.JLSP.Interfaces.ParseCompute;
import gmail.vladimir.JLSP.Pairs.OldOperatorPair;
import gmail.vladimir.JLSP.Variables.*;

import java.util.*;

/**
 * Individual parser instance that can hold individual configuration
 */
public class Parser implements ParseCompute{

    /**
     * 127 all ASCII chars, uses like 1-2kb ram<br/>
     * If you want to support all character, then modify this to 0x10000 which is about 65k+ which is going to use like 1mb of ram
     */
    private int limit;

    private OperatorCompute[] opImpl;
    private int[] opPrio;
    private boolean[] opPresent;
    private int[] opIndexByPrio = new int[0];
    int opIndexMapSize = 1;

    private boolean[] commas;
    private boolean[] delims;

    /**
     * I've tried implementing a char[] and use a map/trie for prefix/fullName detection but every implementation turned out to be noticeably slower.<br/>
     * So, I'm either too incompetent to implement that correctly, or this impl (maps + string builder) is the best solution. In my opinion, it's probably the former, not the latter.
     */
    private HashMap<String, FunctionCompute> functions;
    private NameSearcher funcNameSearcher;
    private Deque<ParsingState> statePool;

    private ParseCompute parseLogic;
    private boolean skipEmptySpace = true;
    private char defaultOperator = '+';
    private char betweenVariables = '*';
    private double defaultValueBetweenEmptyOperators = 0D;

    /**
     * Sets the initial array sizes for the character limit on OPERATORS, commas, Delimiters<br/>
     * Default = 127 = All ASCHII chars<br/><br/>
     * Using characters that are above this limit will result in exceptions. If you except to use characters higher than 127, use the {@link Parser#Parser(int)} constructor, or the {@link Parser#changeLimit(int)} method
     */
    public Parser(){
        this(127);
    }

    /**
     * Sets the initial array sizes for the character limit on OPERATORS, commas, DElimitERS<br/>
     * Default = 127 = All ASCHII chars (Occupies less space ~ 1-2kb total)<br/>
     * Max = 0x10000 = All chars, ever (Occupies more space ~ 1mb total)<br/>
     * Search for the unicode of the characters you're expecting to use and use the highest value here
     *
     * @param size The amount
     */
    public Parser(int size){
        this.limit = size;
        reset();
    }

    /**
     * Changes array sizes for the character limit on OPERATORS, commas, DElimitERS</br>
     * Default = 127 = All ASCHII chars (Occupies less space ~ 1-2kb total)<br/>
     * Max = 0x10000 = All chars, ever (Occupies more space ~ 1mb total)<br/>
     * Search for the unicode of the characters you're expecting to use and use the highest value here<br/><br/>
     * This method will create new arrays and move the old ones over
     *
     * @param size The new size
     */
    public void changeLimit(int size){
        int oldlimit = this.limit;
        this.limit = size;

        if(opImpl == null){
            initDefaults();
            return;
        }

        OperatorCompute[] newOpImpl = new OperatorCompute[size];
        int[] newOpPrio = new int[size];
        boolean[] newOpPresent = new boolean[size];
        boolean[] newCommas = new boolean[size];
        boolean[] newDelims = new boolean[size];
        System.arraycopy(opImpl, 0, newOpImpl, 0, oldlimit);
        System.arraycopy(opPrio, 0, newOpPrio, 0, oldlimit);
        System.arraycopy(opPresent, 0, newOpPresent, 0, oldlimit);
        System.arraycopy(commas, 0, newCommas, 0, oldlimit);
        System.arraycopy(delims, 0, newDelims, 0, oldlimit);

        this.opImpl = newOpImpl;
        this.opPrio = newOpPrio;
        this.opPresent = newOpPresent;
        this.commas = newCommas;
        this.delims = newDelims;
        rebuildPriorityIndex();
    }

    /**
     * Resets this object to its default state.
     */
    public void reset(){
        initDefaults();
    }

    /**
     * Adds a new character as a valid operator that will be parsed,
     * or replaces an existing operator if one was already present.
     * <br/>
     *
     * DoubleBinaryOperator is an interface that accepts two double variables for left and right.
     * <br/>
     * A simple example:
     * <pre> {@code
     * addOperator('+', (left, right, extra) -> left + right, 0);
     * }</pre>
     * <br/>
     * The extra value is used internally to calculate the default implementation for the ^ operator and can be ignored.<br/><br/>
     * Lowest priority is 0.
     * Providing a value less than 0 will get defaulted to 0
     * <br/><br/>
     * You can modify already, default operators like this. For example,
     * you could have + have a higher priority than * and will get calculated first
     * <br/><br/>
     * If Parser.shouldSkipEmptySpace() is false then empty space could be used as a valid variable operator
     *
     * @param c The character which will be added as an operator
     * @param compute The compute logic. For example (a, b) -> a + b
     * @param priority The priority this operator will have
     * @return A pair containing the old OperatorCompute logic and its old priority if the operator was already present and this method overrode it. Or if it wasn't present:
     * <pre> {@code
     * new OldOperatorPair(null, -1);
     * }</pre>
     *
     * @throws UnsupportedOperationException if c is already present as a delimiter/comma
     *
     */
    public OldOperatorPair addOperator(char c, OperatorCompute compute, int priority) {
        checkFree(c);
        OperatorCompute oldImpl = opImpl[c];
        int oldPrio = opPrio[c];
        opImpl[c] = compute;
        opPrio[c] = Math.max(priority, 0);
        opPresent[c] = true;
        rebuildPriorityIndex();
        return new OldOperatorPair(oldImpl, oldPrio);
    }

    /**
     * Changes the priority of an operator.
     * If the operator doesn't exist, nothing happens.
     * <p/>
     * Lowest priority is 0.
     * Providing a value less than 0 will get defaulted to 0
     * <p/>
     * You can modify already, well-known operators like this. For example,
     * you could have + have a higher priority than * and will get calculated first
     * <br/><br/>
     * If Parser.shouldSkipEmptySpace() is false then empty space could be used as a valid variable operator
     *
     * @param c Operator
     * @param priority New priority
     */
    public void changeOperatorPriority(char c, int priority) {
        opPrio[c] = Math.max(priority, 0);
        rebuildPriorityIndex();
    }

    /**
     * Removes a character from the operator list. If the provided character is not an operator, nothing happens.
     * <br/>
     * You may remove any operator, even the default one (e.g '+').
     */
    public void removeOperator(char c) {
        opImpl[c] = null;
        opPrio[c] = -1;
        opPresent[c] = false;
        rebuildPriorityIndex();
    }

    /**
     * Default priorities:<br/>
     * <pre> {@code
     * 10 - ^
     * 5 - /*%
     * 0 - +-
     * }</pre>
     * @param c The operator you are searching for
     * @return The priority of the operator if it exists, or -1 otherwise
     */
    public int getOperatorPriority(char c) {
        return opPrio[c];
    }

    /**
     *
     * Checks if the provided character is a valid operator
     *
     * @param c Character to check
     * @return Whether the provided character is a recognized operator
     */
    public boolean isOperator(char c) {
        return opPresent[c];
    }

    /**
     * Checks whether empty space will be checked or skipped entirely. True by default. <br/>
     * This is useful if you decide to add empty space as a possible operator/comma/func-delimiter
     */
    public boolean shouldSkipEmptySpace(){
        return skipEmptySpace;
    }

    /**
     * Sets whether empty space will be checked or skipped entirely. True by default. <br/>
     * This is useful if you decide to add empty space as a possible operator/comma/func-delimiter
     */
    public void setSkipEmptySpace(boolean skipEmptySpace){
        this.skipEmptySpace = skipEmptySpace;
    }

    /**
     * Returns a default neutral operator that will be used for operations to fill in case an operator is missing. Default value '+'
     */
    public char getDefaultOperator(){
        return defaultOperator;
    }

    /**
     * Sets the default neutral operator that will be used for operations to fill in case an operator is missing. Default value '+'
     */
    public void setDefaultOperator(char defaultOperator){
        this.defaultOperator = defaultOperator;
    }

    /**
     * Returns a default neutral operator that will be used for operations to fill in case an operator is missing for specific cases. Default value '*' <br/>
     * For example, having the alphabet 'a' and 'b', betweenVariables '*' and the following evaluation: 2+ab. The example will be interpreted as 2+a*b.
     */
    public char getBetweenVariables(){
        return betweenVariables;
    }

    /**
     * Sets the default neutral operator that will be used for operations to fill in case an operator is missing for specific cases. Default value '*' <br/>
     * For example, having the alphabet 'a' and 'b', betweenVariables '*' and the following evaluation: 2+ab. The example will be interpreted as 2+a*b.
     */
    public void setBetweenVariables(char betweenVariables){
        this.betweenVariables = betweenVariables;
    }

    /**
     * Returns the current parse class logic if the default one was overridden. Or null if the default logic is used
     */
    public ParseCompute getParseLogic(){
        return parseLogic;
    }

    /**
     * Changes the current logic used for parsing formulas. If a null value is provided, then the default parse logic will be used.
     */
    public void setParseLogic(ParseCompute parseLogic){
        this.parseLogic = parseLogic;
    }

    /**
     * Returns d - The default value that will be used to fill in empty operators. Default 0.
     * <br/>
     * For example, the following expression:
     * <pre> {@code
     * -*+--4
     * }</pre>
     * Will be interpreted as:
     * <pre> {@code
     * -d*d+d-(-4)
     * }</pre>
     * With the default value 0:
     * <pre> {@code
     * -0*0+0-(-4)
     * }</pre>
     */
    public double getDefaultValueBetweenEmptyOperators(){
        return defaultValueBetweenEmptyOperators;
    }

    /**
     * Sets d - The default value that will be used to fill in empty operators. Default 0.
     * <br/>
     * For example, the following expression:
     * <pre> {@code
     * -*+--4
     * }</pre>
     * Will be interpreted as:
     * <pre> {@code
     * -d*d+d-(-4)
     * }</pre>
     * With the default value 0:
     * <pre> {@code
     * -0*0+0-(-4)
     * }</pre>
     */
    public void setDefaultValueBetweenEmptyOperators(double d){
        this.defaultValueBetweenEmptyOperators = d;
    }

    /**
     *
     * Adds a case-sensitive as a comma for floating-point numbers, e.g. commas = ['.']
     * <pre> {@code
     * 2.5
     * }</pre>
     *
     * Or e.g. commas = ['.', '_']
     * <pre> {@code
     * 2.5 + 4_8
     * }</pre>
     *
     * @param c The character that'll be added as a comma
     * @return True if the comma was added, or False if the character was already a valid comma
     * @throws UnsupportedOperationException if c is already present as a delimiter/operator
     */
    public boolean addComma(char c) {
        checkFreeComma(c);
        boolean was = commas[c];
        commas[c] = true;
        return !was;
    }

    /**
     *
     * Returns whether the char is a valid comma for floating-point numbers, e.g. c = '.'
     * <pre> {@code
     * 2.5
     * }</pre>
     *
     * Or e.g. c = ['.', '_']
     * <pre> {@code
     * 2.5 + 4_8
     * }</pre>
     *
     * @return True if the char is a valid comma, or False otherwise
     */
    public boolean isComma(char c) {
        return commas[c];
    }

    /**
     *
     * Returns the char as a valid comma for floating-point numbers, e.g. c = '.'
     * <pre> {@code
     * 2.5
     * }</pre>
     *
     * Or e.g. c = ['.', '_']
     * <pre> {@code
     * 2.5 + 4_8
     * }</pre>
     *
     * @param c The character that'll be removed as a comma
     * @return True if the comma was removed, or False if the character wasn't already a valid comma
     */
    public boolean removeComma(char c) {
        boolean was = commas[c];
        commas[c] = false;
        return was;
    }

    /**
     *
     * Adds a character that'll be used to delimit the vars/params in a function, e.g. delimiters = [',']
     *
     * <pre> {@code
     * f(x, y)
     * }</pre>
     *
     * Or e.g. c = [',', '/']
     * <pre> {@code
     * f(x, y/ z)
     * }</pre>
     *
     * @param c The case-sensitive character that'll be added as a variable/params delimiter for functions
     * @return True if the delimiter was added, or False if the character was already a valid delimiter
     * @throws UnsupportedOperationException if c is already present as a comma/operator
     */
    public boolean addDelimiter(char c) {
        checkFreeDelim(c);
        boolean was = delims[c];
        delims[c] = true;
        return !was;
    }

    /**
     *
     * Checks whether the char is a valid delimiter that can be used to separate vars/params in a function, e.g. delimiters = [',']
     *
     * <pre> {@code
     * f(x, y)
     * }</pre>
     *
     * Or e.g. c = [',', '/']
     * <pre> {@code
     * f(x, y/ z)
     * }</pre>
     *
     * @return True if the delimiter is a delimiter, or False otherwise
     * @throws UnsupportedOperationException if c is already present as a comma/alphabet/operator
     */
    public boolean isDelimiter(char c) {
        return delims[c];
    }

    /**
     *
     * Removes the char as a valid delimiter that can be used to separate vars/params in a function, e.g. delimiters = [',']
     *
     * <pre> {@code
     * f(x, y)
     * }</pre>
     *
     * Or e.g. c = [',', '/']
     * <pre> {@code
     * f(x, y/ z)
     * }</pre>
     *
     * @return True if the delimiter was removed, or False if the char wasn't a valid delimiter
     * @throws UnsupportedOperationException if c is already present as a comma/alphabet/operator
     */
    public boolean removeDelimiter(char c) {
        boolean was = delims[c];
        delims[c] = false;
        return was;
    }

    /**
     *
     * Adds a function tied to an id string. If a function was already present at that id then it'll get overridden and the old value will be returned.<br/><br/>
     *
     * The function compute interface method requires the following params:<br/>
     * 1. The formula that is calling the function<br/>
     * 2. Whether we're calculating inOrder or naively<br/>
     * 3. An array of the formula entities that are being used<br/><br/>
     *
     * The values will be used for the parser's method of processing entities and returning the appropriate result. Or you could use your own method of extracting the value from an entity.<br/><br/>
     *
     * Examples of how to add functions<br/>
     * 1. Simple
     * <pre> {@code
     *   addFunction("abs", (caller, inOperationOrder, entities) -> Math.abs(processEntity(caller, entities[0], inOperationOrder)));
     * }</pre>
     *
     * 2. Adding condition
     * <pre> {@code
     *   addFunction("abs", (caller, inOperationOrder, entities) -> {
     *      if(entities.length == 0)
     *          throw new UnsupportedOperationException("Invalid number of params");
     *
     *      return Math.abs(processEntity(caller, entities[0], inOperationOrder));
     *   });
     * }</pre>
     *
     * 3. Multiple params
     * <pre> {@code
     * addFunction("nthroot", (caller, inOperationOrder, entities) -> Math.pow(processEntity(caller, entities[0], inOperationOrder), 1.0 / processEntity(caller, entities[1], inOperationOrder)));
     * }</pre>
     *
     * 4. Loop
     * <pre> {@code
     *   addFunction("avg", (caller, inOperationOrder, a) -> {
     *      double sum = 0;
     *      for (int i = 0; i < a.length; i++)
     *          sum += processEntity(caller, a[i], inOperationOrder);
     *      return sum / a.length;
     *   });
     * }</pre>
     *
     * 5. No args
     * <pre> {@code
     *   addFunction("rand", (caller, inOperationOrder, a) -> randInstance.nextDouble());
     * }</pre>
     *
     * @param id The id tied to that function. Name is case-sensitive
     * @param compute The compute logic for the function
     * @return The old function tied to the provided if it exist, otherwise null
     */
    public FunctionCompute addFunction(String id, FunctionCompute compute){
        FunctionCompute old = functions.put(id, compute);

        if(old != null)
            return old;

        funcNameSearcher.addName(id);
        return null;
    }

    /**
     * Checks if the given id has a function.
     * @param id To check for function. Case-sensitive
     * @return True if there is a function linked to this id, false otherwise
     */
    public boolean isFunction(String id){
        return functions.containsKey(id);
    }

    /**
     * Retrieves the function logic for the given id, if it exists.
     * @param id To check for function. Case-sensitive
     * @return The function logic tied to the given id, null otherwise
     */
    public FunctionCompute getFunction(String id){
        return functions.get(id);
    }

    /**
     * Removes and returns the function logic for the given id, if it exists.
     * @param id To check for function. Case-sensitive
     * @return The function logic that as tied to the given id, null otherwise
     */
    public FunctionCompute removeFunction(String id){
        FunctionCompute removed = functions.remove(id);

        if(removed == null)
            return null;

        funcNameSearcher.removeName(id);
        return removed;
    }

    /**
     * see {@link Parser#parseFormula(String, boolean)}
     */
    public Formula parse(String formula){
        return parseFormula(formula, false);
    }

    /**
     *
     * Parses the given string and returns a Formula entity that contains all the other variables inside, and can be stored for later use.<br/>
     * Parsing the formula is done in O(n) as the chars in the string are only looped once, no backtracking.<br/><br/>
     * Each char is verified individually, a {@link ParsingState} helper class is used to keep track of the current parsing information.<br/>
     * As soon as a '(' char is present, the current ParsingState helper class is pushed into a queue and a new one is created. From this point, the program will treat the current state as a mini-Formula entity
     * and will be solved individually afterward. This process repeats itself every time a '(' appears, the states just get pushed into a queue and get resumed later.<br/>
     * If at the end of the parsing process there are entities unfinished, they will forcefully get finished, like 4+(1/(2*3 will get treated as 4+(1/(2*3))<br/><br/>
     *
     * When parsing for functions, the program will see something like, a, then ab = a*b, which will be stored as {@link ReplaceableVariable} then finally abs. The other two variables a and b will be removed from their variables lists.<br/>
     * Removing the 'temporary' variables in done in O(1) as the data structures used for these operations ({@link TempList} and {@link TempMap}) are made specifically for this specific use case
     *
     * @param formula The string that's going to be parsed
     * @param acceptNull If the returned value can be null, if false, an empty Formula object will be provided. The default implementation never returns null, only if the parse logic has been replaced using {@link Parser#setParseLogic(ParseCompute)}
     * @return A Formula entity or null.
     */
    public Formula parseFormula(String formula, boolean acceptNull) {
        if(parseLogic != null) {
            Formula result = parseLogic.parse(formula);
            return acceptNull || result != null ? result : new Formula(new FormulaEntity[0], 0, new FormulaEntity[0], 0, new FormulaEntity[0], 0, new LinkedHashMap<>(), this);
        }

        ParsingState state = getState();
        ParsingState currentState = state;

        Deque<ParsingState> stateStack = new ArrayDeque<>();

        Character last = null;

        char[] arr = formula.toCharArray();
        for(char c : arr){
            if(skipEmptySpace && c == ' ')
                continue;

            if(currentState.funcString.length() == 0 && currentState.preFuncOp == null)
                currentState.preFuncOp = isOperator(c) ? c : betweenVariables;

            String funcString = currentState.funcString.toString();
            if(!(functions.containsKey(funcString))){
                boolean wasEmpty = currentState.funcString.length() == 0;
                if(!funcNameSearcher.exists(funcString = currentState.funcString.append(c).toString())){
                    currentState.dump();
                    currentState.resetFunc();
                }
                else if(wasEmpty)
                    currentState.dump();
            }

            if(c == '('){
                char operator = last != null && last != '(' ? betweenVariables : defaultOperator;

                if((currentState.func = functions.get(funcString)) == null){
                    currentState.dump();
                    currentState.resetFunc();

                    if(last != null && isOperator(last))
                        operator = last;
                }
                else{
                    currentState.clearTemp();
                    operator = currentState.preFuncOp;
                }

                boolean isFunc = currentState.func != null;
                stateStack.push(currentState);
                currentState = getState();
                currentState.isFunc = isFunc;
                currentState.parenthesesOperation = operator;
                last = c;
                continue;
            }
            last = c;

            if(c == ')'){
                if(stateStack.isEmpty())
                    throw new IllegalArgumentException("Unbalanced parenthesis");

                if(currentState.isFunc)
                    currentState.addFuncVariable(this);

                currentState = addState(currentState, stateStack);
                continue;
            }

            processChar(c, currentState, currentState.inOrder, currentState.inOperationOrder);
        }

        while(!stateStack.isEmpty() && state != currentState)
            currentState = addState(currentState, stateStack);

        return finishFormula(state, null);
    }

    /**
     * Once a '(' appears, a new mini-formula entity is created and the current state is being pushed into a queue and a new parse state is created for this new entity. <br/>
     * Once a ')' appears, the state is considered as finished, all relevant information is finished, the current state gets recycled and the previous state gets resumed.
     * @param currentState The current state for the mini-formula
     * @param stateStack The queue the holds the paused states
     * @return The previous valid parsing state that'll get resumed for future operations
     */
    private ParsingState addState(ParsingState currentState, Deque<ParsingState> stateStack){
        currentState.dump();

        ParsingState st = currentState;
        currentState = stateStack.pop();
        char op = st.parenthesesOperation == '-' ? currentState.isNegative ? '-' : defaultOperator : st.parenthesesOperation;
        FormulaEntity<?> toAdd;

        if(currentState.func == null)
            toAdd = finishFormula(st, op);
        else
            toAdd = new Function(currentState.funcString.toString(), st.funVars.getArray(), st.funVars.size(), op);

        currentState.inOrder.add(toAdd);
        resolveAddition(currentState, st.parenthesesOperation, toAdd, currentState.inOperationOrder);
        currentState.reset();
        recycleState(st, this);
        return currentState;
    }

    /**
     * Called once the main formula, or a mini-formula is finished and ready to be saved
     * @param currentState The parsing state associated with this formula
     * @param c The previous operator character associated with this formula. If the value is not null, this is a mini-formula, if it is null, this is the main formula
     * @return The formula entity object
     */
    protected Formula finishFormula(ParsingState currentState, Character c){
        if(currentState.currentValue != null)
            addStaticVariable(currentState, currentState.inOrder, currentState.inOperationOrder, false);

        TempList lowest = !currentState.inOperationOrder.isEmpty() ? currentState.inOperationOrder.getAndRemoveLast() : new TempList();
        if(lowest == null)
            lowest = new TempList();

        TempList inOrder = currentState.inOrder;
        TempList inOperationOrderList = currentState.inOperationOrder.complete();
        int inOperationOrderSize = inOperationOrderList.size();

        if(c != null)
            return new Formula(c, inOrder.getArray(), inOrder.size(), inOperationOrderList.getArray(), inOperationOrderSize, lowest.getArray(), lowest.size(), this);
        else
            return new Formula(inOrder.getArray(), inOrder.size(), inOperationOrderList.getArray(), inOperationOrderSize, lowest.getArray(), lowest.size(), this);
    }

    /**
     * Checks the current char for validation, then continues building the current entity that is not yet finished, or saves a finished one and starts a new one.<br/>
     * Used for StaticVariables and ReplaceableVariables
     */
    private void processChar(char c, ParsingState state,TempList inOrder, TempMap inOperationOrder){
        if(state.currentOperation == null && !inOrder.isEmpty()){
            if(isValidVar(c))
                state.currentOperation = betweenVariables;

            else if(isDelimiter(c) && state.isFunc){
                state.addFuncVariable(this);
                return;
            }

//            else if(isDelimiter(c))
//                state.currentOperation = defaultOperator;

            else if(!isOperator(c))
                throw new RuntimeException("Invalid operation symbol: " + c);

            else{
                state.currentOperation = c;
                state.isNegative = c == '-';
                return;
            }
        }

        if(state.currentValue == null){
            state.currentValue = 0D;

            if(!state.started && c != '-')
                state.currentValue = defaultValueBetweenEmptyOperators;

            if(c == '-'){
                state.isNegative = !state.isNegative;
                state.currentValue = null;
                return;
            }
        }

        if(isComma(c)){
            if(state.hasDecimalPoint)
                throw new RuntimeException("Double comma/decimal point: " + c);

            state.hasDecimalPoint = true;
            return;
        }

        if(isDelimiter(c) && state.isFunc){
            state.addFuncVariable(this);
            return;
        }

        byte val = fromChar(c);

        if(val == -1){
            if(isOperator(c)){
                if(state.func == null)
                    addStaticVariable(state, inOrder, inOperationOrder, true);

                state.currentOperation = c;
                state.isNegative = c == '-';
                return;
            }

            if(state.currentValue != 0D)
                addStaticVariable(state, inOrder, inOperationOrder, false);

            addVariable(c, state, inOrder, inOperationOrder);
            return;
        }
        else if (!state.started)
            state.started = true;

        if(state.hasDecimalPoint){
            state.currentValue += val * state.decimalPlace;
            state.decimalPlace *= 0.1;
            return;
        }

        state.currentValue *= 10;
        state.currentValue += val;
    }

    /**
     * Creates a static variable, adds it and resolves its priority in the operation order
     */
    private StaticVariable addStaticVariable(ParsingState state, TempList toAdd, TempMap inOperationOrder, boolean reset){
        char operation = state.currentOperation != null ? state.currentOperation : defaultOperator;
        StaticVariable staticVariable = new StaticVariable(formatStatic(state.isNegative, state.currentValue), operation == '-' ? defaultOperator : operation);

        preResolve(state, staticVariable, operation, toAdd, inOperationOrder, reset);
        return staticVariable;
    }

    /**
     * Creates a replaceable variable, adds it and resolves its priority in the operation order
     */
    private ReplaceableVariable addVariable(char c, ParsingState state, TempList toAdd, TempMap inOperationOrder){
        char operation = state.currentValue != 0.0 ? betweenVariables : (state.currentOperation != null ? state.currentOperation : defaultOperator);
        ReplaceableVariable replaceableVariable = new ReplaceableVariable(c, operation != '-' ? operation : defaultOperator, state.isNegative);

        preResolve(state, replaceableVariable, operation, toAdd, inOperationOrder, true);
        return replaceableVariable;
    }

    /**
     * Helper method used by {@link Parser#addStaticVariable(ParsingState, TempList, TempMap, boolean)} and {@link Parser#addVariable(char, ParsingState, TempList, TempMap)}
     * Checks if it's the first entity added, if it is, add default values. If not, resolve priority
     */
    private void preResolve(ParsingState state, FormulaEntity<?> entity, char c, TempList toAdd, TempMap inOperationOrder, boolean reset){
        if(state.lastAddedEntity != null)
            resolveAddition(state, c, entity, inOperationOrder);
        else{
            state.lastAddedEntity = entity;
            state.lastOperationPriority = 0;
            state.lastOperation = c;
            inOperationOrder.getOrAdd(getIndexByPriority(0)).add(entity);
        }

        toAdd.add(entity);

        if(reset)
            state.reset();
    }

    /**
     * When adding a new entity, the parser follows these three rules:<br/>
     * 1. Checks if the current operator has the same priority as the past one, if yes, the entity is added to that priority's list<br/>
     * 2. Checks if the current operator has a lesser priority value than the past one, if yes, the prev operation value is modified and added to its lesser list<br/>
     * 3. Checks if the current operator has a greater priority value than the past one, if yes, then the last added entity is removed from its list and added to this high priority list, then the current entity is added after it<br/>
     * All operations related to lists and maps are in O(1) as they are special implementation for this use case
     */
    private void resolveAddition(ParsingState state, char operation, FormulaEntity<?> entity, TempMap inOperationOrder){
        int prio = getOperatorPriority(operation);

        FormulaEntity<?> lastEntity = state.lastAddedEntity;
        char lastOperation = state.lastOperation;

        if(prio == state.lastOperationPriority){
            inOperationOrder.getOrAdd(getIndexByPriority(state.lastOperationPriority)).add(entity);
            state.lastAddedEntity = entity;
            state.lastOperation = operation;
            groupAddition(operation, prio, (byte) 0, entity, lastEntity, lastOperation, inOperationOrder, state);
            return;
        }

        if(prio < state.lastOperationPriority){
            state.lastOperationPriority = prio;
            state.lastAddedEntity = entity;
            state.lastOperation = operation;
            inOperationOrder.getOrAdd(getIndexByPriority(state.lastOperationPriority)).add(entity);
            groupAddition(operation, prio, (byte) 1, entity, lastEntity, lastOperation, inOperationOrder, state);
            return;
        }

        TempList list = inOperationOrder.getOrAdd(getIndexByPriority(state.lastOperationPriority));

        if(!list.isEmpty())
            list.removeLast();

        if(!list.isEmpty() && state.lastOperationPriority > 0)
            list.getFirst().setPrecedentSymbol(state.lastOperation);

        state.lastOperationPriority = prio;
        state.lastOperation = operation;
        list = inOperationOrder.getOrAdd(getIndexByPriority(state.lastOperationPriority));
        if(state.lastAddedEntity != null)
            list.add(state.lastAddedEntity);

        list.add(entity);
        state.lastAddedEntity = entity;
        groupAddition(operation, prio, (byte) 2, entity, lastEntity, lastOperation, inOperationOrder, state);
    }

    /** Can be overridden for your needs, contains all the parameters necessary to make modifications<br/>
     * Currently only used for grouping pow operations eg 2^3^2^4 -> 2^(3^(2^4))
     * op = 0 = same<br/>
     * op = 1 = less<br/>
     * op = 2 = higher
     */
    private void groupAddition(char operation, int prio, byte op, FormulaEntity<?> currentEntity, FormulaEntity<?> lastEntity, char lastOperation, TempMap inOperationOrder, ParsingState state){
        if(op != 0 || operation != '^' || lastOperation != '^')
            return;

        TempList list = inOperationOrder.sureGet(getIndexByPriority(prio));

        if(list.size() < 3)
            return;


        TempList newList = new TempList(2);
        FormulaEntity<?> e1 = list.getAndRemoveLast(), e2 = list.getAndRemoveLast();

        Formula toAdd;
        if(e2 instanceof Formula){
            toAdd = (Formula) e2;
            newList.add(toAdd.getLast());
            newList.add(e1);
            toAdd.setLast(new Formula('^', newList.getArray(), 2, newList.getArray(), 2, EMPTY, DEF, this));
        }
        else{
            newList.add(e2);
            newList.add(e1);

            toAdd = new Formula('^', newList.getArray(), 2, newList.getArray(), 2, EMPTY, DEF, this);
        }

        list.add(toAdd);
    }
    private final static FormulaEntity<?>[] EMPTY = new FormulaEntity<?>[0];
    private final int DEF = 0;

    //Creates and initializes the default values for this class. Can also be used to reset this object
    private void initDefaults(){
        functions = new HashMap<>();
        funcNameSearcher = new NameSearcher();
        statePool = new ArrayDeque<>();

        for(int i = 0; i < 3; i++)
            statePool.push(new ParsingState(this));

        parseLogic = null;
        skipEmptySpace = true;
        defaultOperator = '+';
        betweenVariables = '*';
        defaultValueBetweenEmptyOperators = 0D;

        opImpl = new OperatorCompute[limit];
        opPrio = new int[limit];
        opPresent = new boolean[limit];
        commas = new boolean[limit];
        delims = new boolean[limit];

        addOperator('^', (a, b, extra) -> {
            double base = Math.abs(a);
            int sign = 1;

            if (extra[0] && extra[1]) {
                if (b != Math.floor(b))
                    throw new ArithmeticException("cannot raise negative number to fractional power");
                if ((long) b % 2 != 0)
                    sign = -sign;
            }
            else if (!extra[0] && a < 0)
                sign = -sign;

            if (extra[0] && extra[2])
                sign = -sign;

            return sign * Math.pow(base, b);
        }, 10);

        addOperator('*', (a, b, extra) -> a * b, 5);
        addOperator('/', (a, b, extra) -> a / b, 5);
        addOperator('%', (a, b, extra) -> a % b, 5);

        addOperator('+', (a, b, extra) -> a + b, 0);
        addOperator('-', (a, b, extra) -> a - b, 0);

        addComma('.');

        addDelimiter(',');

        functions.clear();
        funcNameSearcher.clear();

        addFunction("pi", (caller, inOperationOrder, a) -> Math.PI);
        addFunction("e", (caller, inOperationOrder, a) -> Math.E);
        addFunction("phi", (caller, inOperationOrder, a) -> (1 + Math.sqrt(5)) / 2);
        addFunction("tau", (caller, inOperationOrder, a) -> 2 * Math.PI);
        addFunction("sqrt2", (caller, inOperationOrder, a) -> Math.sqrt(2));
        addFunction("sqrt3", (caller, inOperationOrder, a) -> Math.sqrt(3));
        addFunction("ln2", (caller, inOperationOrder, a) -> Math.log(2));
        addFunction("ln10", (caller, inOperationOrder, a) -> Math.log(10));
        addFunction("log2e", (caller, inOperationOrder, a) -> 1 / Math.log(2));
        addFunction("log10e", (caller, inOperationOrder, a) -> 1 / Math.log(10));
        addFunction("inf", (caller, inOperationOrder, a) -> Double.POSITIVE_INFINITY);
        addFunction("nan", (caller, inOperationOrder, a) -> Double.NaN);

        addFunction("abs", (caller, inOperationOrder, a) -> Math.abs(processEntity(caller, a[0], inOperationOrder)));
        addFunction("round", (caller, inOperationOrder, a) -> (double) Math.round(processEntity(caller, a[0], inOperationOrder)));
        addFunction("floor", (caller, inOperationOrder, a) -> Math.floor(processEntity(caller, a[0], inOperationOrder)));
        addFunction("ceil", (caller, inOperationOrder, a) -> Math.ceil(processEntity(caller, a[0], inOperationOrder)));
        addFunction("mod", (caller, inOperationOrder, a) -> processEntity(caller, a[0], inOperationOrder) % processEntity(caller, a[1], inOperationOrder));
        addFunction("sqrt", (caller, inOperationOrder, a) -> Math.sqrt(processEntity(caller, a[0], inOperationOrder)));
        addFunction("pow", (caller, inOperationOrder, a) -> Math.pow(processEntity(caller, a[0], inOperationOrder), processEntity(caller, a[1], inOperationOrder)));
        addFunction("exp", (caller, inOperationOrder, a) -> Math.exp(processEntity(caller, a[0], inOperationOrder)));
        addFunction("log", (caller, inOperationOrder, a) -> Math.log(processEntity(caller, a[0], inOperationOrder)));
        addFunction("log10", (caller, inOperationOrder, a) -> Math.log10(processEntity(caller, a[0], inOperationOrder)));
        addFunction("log2", (caller, inOperationOrder, a) -> Math.log(processEntity(caller, a[0], inOperationOrder)) / Math.log(2));
        addFunction("sin", (caller, inOperationOrder, a) -> Math.sin(processEntity(caller, a[0], inOperationOrder)));
        addFunction("cos", (caller, inOperationOrder, a) -> Math.cos(processEntity(caller, a[0], inOperationOrder)));
        addFunction("tan", (caller, inOperationOrder, a) -> Math.tan(processEntity(caller, a[0], inOperationOrder)));
        addFunction("asin", (caller, inOperationOrder, a) -> Math.asin(processEntity(caller, a[0], inOperationOrder)));
        addFunction("acos", (caller, inOperationOrder, a) -> Math.acos(processEntity(caller, a[0], inOperationOrder)));
        addFunction("atan", (caller, inOperationOrder, a) -> Math.atan(processEntity(caller, a[0], inOperationOrder)));
        addFunction("atan2", (caller, inOperationOrder, a) -> Math.atan2(processEntity(caller, a[0], inOperationOrder), processEntity(caller, a[1], inOperationOrder)));
        addFunction("sinh", (caller, inOperationOrder, a) -> Math.sinh(processEntity(caller, a[0], inOperationOrder)));
        addFunction("cosh", (caller, inOperationOrder, a) -> Math.cosh(processEntity(caller, a[0], inOperationOrder)));
        addFunction("tanh", (caller, inOperationOrder, a) -> Math.tanh(processEntity(caller, a[0], inOperationOrder)));
        addFunction("asinh", (caller, inOperationOrder, a) -> Math.log(processEntity(caller, a[0], inOperationOrder) + Math.sqrt(Math.pow(processEntity(caller, a[0], inOperationOrder), 2) + 1)));
        addFunction("acosh", (caller, inOperationOrder, a) -> Math.log(processEntity(caller, a[0], inOperationOrder) + Math.sqrt(Math.pow(processEntity(caller, a[0], inOperationOrder), 2) - 1)));
        addFunction("atanh", (caller, inOperationOrder, a) -> 0.5 * Math.log((1 + processEntity(caller, a[0], inOperationOrder)) / (1 - processEntity(caller, a[0], inOperationOrder))));

        addFunction("fact", (caller, inOperationOrder, a) -> {
            int val = (int) processEntity(caller, a[0], inOperationOrder);

            if (val < 0)
                throw new IllegalArgumentException("factorial undefined for negative");

            long result = 1L;
            for (int i = 1; i <= val; i++)
                result *= i;

            return (double) result;
        });
        addFunction("min", (caller, inOperationOrder, a) -> {
            double result = Double.POSITIVE_INFINITY;
            for (int i = 0; i < a.length; i++) {
                double val = processEntity(caller, a[i], inOperationOrder);
                if (val < result) result = val;
            }
            return result;
        });
        addFunction("max", (caller, inOperationOrder, a) -> {
            double result = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < a.length; i++) {
                double val = processEntity(caller, a[i], inOperationOrder);
                if (val > result) result = val;
            }
            return result;
        });
        addFunction("avg", (caller, inOperationOrder, a) -> {
            double sum = 0;
            for (int i = 0; i < a.length; i++)
                sum += processEntity(caller, a[i], inOperationOrder);
            return sum / a.length;
        });
        addFunction("sum", (caller, inOperationOrder, a) -> {
            double total = 0;
            for (int i = 0; i < a.length; i++)
                total += processEntity(caller, a[i], inOperationOrder);
            return total;

        });
        addFunction("median", (caller, inOperationOrder, a) -> {
            double[] vals = new double[a.length];
            for (int i = 0; i < a.length; i++) vals[i] = processEntity(caller, a[i], inOperationOrder);
            Arrays.sort(vals);
            int mid = a.length / 2;
            return a.length % 2 == 1 ? vals[mid] : (vals[mid - 1] + vals[mid]) / 2.0;
        });
        addFunction("sign", (caller, inOperationOrder, a) -> Math.signum(processEntity(caller, a[0], inOperationOrder)));
        addFunction("deg", (caller, inOperationOrder, a) -> Math.toDegrees(processEntity(caller, a[0], inOperationOrder)));
        addFunction("rad", (caller, inOperationOrder, a) -> Math.toRadians(processEntity(caller, a[0], inOperationOrder)));
        addFunction("clamp", (caller, inOperationOrder, a) -> {
            double val = processEntity(caller, a[0], inOperationOrder);
            double min = processEntity(caller, a[1], inOperationOrder);
            double max = processEntity(caller, a[2], inOperationOrder);
            return Math.max(min, Math.min(max, val));
        });
        addFunction("brt", (caller, inOperationOrder, a) -> Math.cbrt(processEntity(caller, a[0], inOperationOrder)));
        addFunction("nthroot", (caller, inOperationOrder, a) -> Math.pow(processEntity(caller, a[0], inOperationOrder), 1.0 / processEntity(caller, a[1], inOperationOrder)));
        addFunction("hypot", (caller, inOperationOrder, a) -> Math.hypot(processEntity(caller, a[0], inOperationOrder), processEntity(caller, a[1], inOperationOrder)));
        addFunction("root", (caller, inOperationOrder, a) -> Math.pow(processEntity(caller, a[0], inOperationOrder), 1.0 / processEntity(caller, a[1], inOperationOrder)));
        addFunction("bitand", (caller, inOperationOrder, a) -> (double)((long)processEntity(caller, a[0], inOperationOrder) & (long)processEntity(caller, a[1], inOperationOrder)));
        addFunction("bitor", (caller, inOperationOrder, a) -> (double)((long)processEntity(caller, a[0], inOperationOrder) | (long)processEntity(caller, a[1], inOperationOrder)));
        addFunction("bitxor", (caller, inOperationOrder, a) -> (double)((long)processEntity(caller, a[0], inOperationOrder) ^ (long)processEntity(caller, a[1], inOperationOrder)));
        addFunction("bitnot", (caller, inOperationOrder, a) -> (double)(~(long)processEntity(caller, a[0], inOperationOrder)));
        addFunction("shl", (caller, inOperationOrder, a) -> (double) ((long) processEntity(caller, a[0], inOperationOrder) << (int) (long) processEntity(caller, a[1], inOperationOrder)));
        addFunction("shr", (caller, inOperationOrder, a) -> (double) ((long) processEntity(caller, a[0], inOperationOrder) >> (int) (long) processEntity(caller, a[1], inOperationOrder)));
        addFunction("sec", (caller, inOperationOrder, a) -> 1.0 / Math.cos(processEntity(caller, a[0], inOperationOrder)));
        addFunction("csc", (caller, inOperationOrder, a) -> 1.0 / Math.sin(processEntity(caller, a[0], inOperationOrder)));
        addFunction("cot", (caller, inOperationOrder, a) -> 1.0 / Math.tan(processEntity(caller, a[0], inOperationOrder)));
        addFunction("normalize_angle", (caller, inOperationOrder, a) -> {
            double angle = processEntity(caller, a[0], inOperationOrder);
            double twoPi = 2 * Math.PI;
            return ((angle % twoPi) + twoPi) % twoPi;
        });
        addFunction("wrap", (caller, inOperationOrder, a) -> {
            double val = processEntity(caller, a[0], inOperationOrder);
            double min = processEntity(caller, a[1], inOperationOrder);
            double max = processEntity(caller, a[2], inOperationOrder);
            double range = max - min;
            return ((val - min) % range + range) % range + min;
        });
        Random randInstance = new Random();
        addFunction("rand", (caller, inOperationOrder, a) -> randInstance.nextDouble());
        addFunction("randint", (caller, inOperationOrder, a) -> {
            int min = (int)processEntity(caller, a[0], inOperationOrder);
            int max = (int)processEntity(caller, a[1], inOperationOrder);
            return min + randInstance.nextInt(max - min + 1);
        });
        addFunction("randrange", (caller, inOperationOrder, a) -> {
            double min = processEntity(caller, a[0], inOperationOrder);
            double max = processEntity(caller, a[1], inOperationOrder);
            return min + (max - min) * randInstance.nextDouble();
        });
        addFunction("seed", (caller, inOperationOrder, a) -> {
            randInstance.setSeed((long)processEntity(caller, a[0], inOperationOrder));
            return 0.0;
        });
        addFunction("noise", (caller, inOperationOrder, a) -> {
            double x = processEntity(caller, a[0], inOperationOrder);
            return (Math.sin(x * 12.9898 + 78.233) + 1.0) * 0.5;
        });
        addFunction("dot", (caller, inOperationOrder, a) -> processEntity(caller, a[0], inOperationOrder) * processEntity(caller, a[1], inOperationOrder));
        addFunction("cross", (caller, inOperationOrder, a) -> processEntity(caller, a[0], inOperationOrder) * processEntity(caller, a[1], inOperationOrder));
        addFunction("length", (caller, inOperationOrder, a) -> Math.abs(processEntity(caller, a[0], inOperationOrder)));
        addFunction("normalize", (caller, inOperationOrder, a) -> {
            double val = processEntity(caller, a[0], inOperationOrder);
            return val == 0 ? 0 : val / Math.abs(val);
        });
        addFunction("distance", (caller, inOperationOrder, a) -> {
            double dx = processEntity(caller, a[0], inOperationOrder) - processEntity(caller, a[1], inOperationOrder);
            return Math.abs(dx);
        });
        addFunction("angle", (caller, inOperationOrder, a) -> {
            double x = processEntity(caller, a[0], inOperationOrder);
            double y = processEntity(caller, a[1], inOperationOrder);
            return Math.atan2(y, x);
        });
        addFunction("lerp", (caller, inOperationOrder, a) -> {
            double a0 = processEntity(caller, a[0], inOperationOrder);
            double a1 = processEntity(caller, a[1], inOperationOrder);
            double t = processEntity(caller, a[2], inOperationOrder);
            return a0 + (a1 - a0) * t;
        });
        addFunction("mix", (caller, inOperationOrder, a) -> {
            double a0 = processEntity(caller, a[0], inOperationOrder);
            double a1 = processEntity(caller, a[1], inOperationOrder);
            double t = processEntity(caller, a[2], inOperationOrder);
            return a0 + (a1 - a0) * t;
        });
    }

    /**
     * {@link TempMap} is created based on the current present OPERATORS and their priorities, so the map is always (reverse)sorted and has O(1) insertion time. This is method is used to retrieve the index
     */
    public int getIndexByPriority(int priority) {
        if (priority < 0 || priority >= opIndexByPrio.length) return -1;
        return opIndexByPrio[priority];
    }

    /**
     * Computes the two variables using the selected operator logic.
     * @throws NullPointerException If the character does not a custom compute logic
     * @throws IndexOutOfBoundsException If the index does not fit inside the current {@link Parser#limit}
     */
    public final double compute(char c, double left, double right, boolean... extra){
        return opImpl[c].compute(left, right, extra);
    }

    private final static FunctionCompute defaultFuncCompute = (caller, inOperationOrder, a) -> 0;

    /**
     * Computes the function given its current arguments.
     * @return The value after computing if the logic exists, or 0.0
     * @throws IndexOutOfBoundsException If the selected function uses more arguments than what {@link Function#getParams()} holds
     */
    public final double computeFunction(Formula caller, boolean inOperationOrder, Function func){
        return functions.getOrDefault(func.getId(), defaultFuncCompute).compute(caller, inOperationOrder, func.getParams());
    }

    public final double processEntity(Formula caller, FormulaEntity<?> entity, boolean inOperationOrder) {
        if (entity instanceof StaticVariable)
            return ((StaticVariable) entity).getValue();

        else if (entity instanceof Formula) {
            Formula formula = (Formula) entity;
            return inOperationOrder ? formula.inOperationOrderResult() : formula.naiveResult();
        }

        else if (entity instanceof Function)
            return computeFunction(caller, inOperationOrder, (Function) entity);

        ReplaceableVariable replVar = (ReplaceableVariable) entity;
        Formula base = caller.getRoot() == null ? caller : caller.getRoot();
        double value = base.getVariableValues()[replVar.getIndex()];
        return replVar.isNegative() ? -value : value;
    }

    private void rebuildPriorityIndex() {
        int maxPrio = -1;
        int distinct = 0;
        for (int i = 0; i < limit; i++) {
            if (opPresent[i]) {
                int p = opPrio[i];
                if (p > maxPrio) maxPrio = p;
                distinct++;
            }
        }
        if (maxPrio < 0) {
            opIndexByPrio = new int[0];
            opIndexMapSize = 1;
            return;
        }

        int[] idx = new int[maxPrio + 1];
        Arrays.fill(idx, -1);

        int[] tmp = new int[distinct];
        int t = 0;
        for (int i = 0; i < limit; i++)
            if (opPresent[i]) tmp[t++] = opPrio[i];

        Arrays.sort(tmp);
        int rank = 0;
        for (int k = tmp.length - 1; k >= 0; k--) {
            int p = tmp[k];
            if (idx[p] == -1) idx[p] = rank++;
        }
        opIndexByPrio = idx;
        opIndexMapSize = getIndexByPriority(0) + 1;
    }

    private boolean isValidVar(char c){
        return !isComma(c) && !isDelimiter(c) && !isOperator(c);
    }

    private ParsingState getState() {
        ParsingState st = statePool.poll();
        if (st == null) return new ParsingState(this);
        st.inOperationOrder = new TempMap(opIndexMapSize);
        return st;
    }

    private void recycleState(ParsingState state, Parser parser) {
        state.finalFullReset(parser, false);
        statePool.push(state);
    }

    private void checkTaken(boolean[] arr, char c, String name) {
        if (arr[c]) throw new UnsupportedOperationException("char " + c + " already in " + name);
    }

    private void checkFree(char c) {
        checkTaken(commas, c, "commas");
        checkTaken(delims, c, "delimiters");
    }

    private void checkFreeComma(char c) {
        checkTaken(opPresent, c, "operators");
        checkTaken(delims, c, "delimiters");
    }

    private void checkFreeDelim(char c) {
        checkTaken(opPresent, c, "operators");
        checkTaken(commas, c, "commas");
    }

    private static byte fromChar(char c) {
        return (c >= '0' && c <= '9') ? (byte)(c - '0') : -1;
    }

    private static double formatStatic(boolean negative, double i){
        return negative && i >= 0.0 ? -i : i;
    }

}