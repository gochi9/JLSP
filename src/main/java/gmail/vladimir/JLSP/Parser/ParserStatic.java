package gmail.vladimir.JLSP.Parser;

import gmail.vladimir.JLSP.Interfaces.FunctionCompute;
import gmail.vladimir.JLSP.Interfaces.OperatorCompute;
import gmail.vladimir.JLSP.Interfaces.ParseCompute;
import gmail.vladimir.JLSP.Pairs.OldOperatorPair;
import gmail.vladimir.JLSP.Variables.Formula;

/**
 * A class that has a global {@link Parser} instance, which wraps default operations, ready for easy use
 */
public class ParserStatic {

    private static Parser INSTANCE;

    /**
     * Re-creates the global default static Parser instance object
     */
    public static void remakeDefaultInstance(){
        INSTANCE = new Parser();
    }

    /**
     * Re-creates the global default static Parser instance object
     * @param size See {@link Parser#Parser(int)}
     */
    public static void remakeDefaultInstance(int size){
        INSTANCE = new Parser(size);
    }

    /**
     * Replaces the global default static Parser instance object
     * @param instance The new instance, cannot be null. If null is provided, a new Parser object will be created
     */
    public static void setDefaultInstance(Parser instance){
        INSTANCE = instance != null ? instance : INSTANCE;
    }

    /**
     * Returns the global default static Parser instance object
     */
    public static Parser getDefaultInstance(){
        return INSTANCE;
    }

    static{
        remakeDefaultInstance();
    }

    /**
     * See {@link Parser#reset()}
     */
    public static void reset(){
        INSTANCE.reset();
    }

    /**
     * See {@link Parser#changeLimit(int)}
     */
    public static void changeLimit(int limit){
        INSTANCE.changeLimit(limit);
    }

    /**
     * See {@link Parser#addOperator(char, OperatorCompute, int)}
     */
    public static OldOperatorPair addOperator(char c, OperatorCompute compute, int priority) {
        return INSTANCE.addOperator(c, compute, priority);
    }

    /**
     * See {@link Parser#changeOperatorPriority(char, int)} 
     */
    public static void changeOperatorPriority(char c, int priority) {
        INSTANCE.changeOperatorPriority(c, priority);
    }

    /**
     * See {@link Parser#removeOperator(char)} 
     */
    public static void removeOperator(char c) {
        INSTANCE.removeOperator(c);
    }

    /**
     * See {@link Parser#getOperatorPriority(char)} 
     */
    public static int getOperatorPriority(char c) {
        return INSTANCE.getOperatorPriority(c);
    }

    /**
     * See {@link Parser#isOperator(char)} 
     */
    public static boolean isOperator(char c) {
        return INSTANCE.isOperator(c);
    }

    /**
     * See {@link Parser#shouldSkipEmptySpace()} 
     */
    public static boolean shouldSkipEmptySpace(){
        return INSTANCE.shouldSkipEmptySpace();
    }

    /**
     * See {@link Parser#setSkipEmptySpace(boolean)} 
     */
    public static void setSkipEmptySpace(boolean skipEmptySpace){
        INSTANCE.setSkipEmptySpace(skipEmptySpace);
    }

    /**
     * See {@link Parser#getDefaultOperator()} 
     */
    public static char getDefaultOperator(){
        return INSTANCE.getDefaultOperator();
    }

    /**
     * See {@link Parser#setDefaultOperator(char)} 
     */
    public static void setDefaultOperator(char defaultOperator){
        INSTANCE.setDefaultOperator(defaultOperator);
    }

    /**
     * See {@link Parser#getBetweenVariables()} 
     */
    public static char getBetweenVariables(){
        return INSTANCE.getBetweenVariables();
    }

    /**
     * See {@link Parser#setBetweenVariables(char)} 
     */
    public static void setBetweenVariables(char betweenVariables){
        INSTANCE.setBetweenVariables(betweenVariables);
    }

    /**
     * See {@link Parser#getParseLogic()} 
     */
    public static ParseCompute getParseLogic(){
        return INSTANCE.getParseLogic();
    }

    /**
     * See {@link Parser#setParseLogic(ParseCompute)} 
     */
    public static void setParseLogic(ParseCompute parseLogic){
        INSTANCE.setParseLogic(parseLogic);
    }

    /**
     * See {@link Parser#getDefaultValueBetweenEmptyOperators()} 
     */
    public static double getDefaultValueBetweenEmptyOperators(){
        return INSTANCE.getDefaultValueBetweenEmptyOperators();
    }

    /**
     * See {@link Parser#setDefaultValueBetweenEmptyOperators(double)} 
     */
    public static void setDefaultValueBetweenEmptyOperators(double d){
        INSTANCE.setDefaultValueBetweenEmptyOperators(d);
    }

    /**
     * See {@link Parser#addComma(char)} 
     */
    public static boolean addComma(char c) {
        return INSTANCE.addComma(c);
    }

    /**
     * See {@link Parser#isComma(char)} 
     */
    public static boolean isComma(char c) {
        return INSTANCE.isComma(c);
    }

    /**
     * See {@link Parser#removeComma(char)} 
     */
    public static boolean removeComma(char c) {
        return INSTANCE.removeComma(c);
    }

    /**
     * See {@link Parser#addDelimiter(char)} 
     */
    public static boolean addDelimiter(char c) {
        return INSTANCE.addDelimiter(c);
    }

    /**
     * See {@link Parser#isDelimiter(char)} 
     */
    public static boolean isDelimiter(char c) {
        return INSTANCE.isDelimiter(c);
    }

    /**
     * See {@link Parser#removeDelimiter(char)} 
     */
    public static boolean removeDelimiter(char c) {
        return INSTANCE.removeDelimiter(c);
    }

    /**
     * See {@link Parser#addFunction(String, FunctionCompute)} 
     */
    public static FunctionCompute addFunction(String id, FunctionCompute compute){
        return INSTANCE.addFunction(id, compute);
    }

    /**
     * See {@link Parser#isFunction(String)} 
     */
    public static boolean isFunction(String id){
        return INSTANCE.isFunction(id);
    }

    /**
     * See {@link Parser#getFunction(String)} 
     */
    public static FunctionCompute getFunction(String id){
        return INSTANCE.getFunction(id);
    }

    /**
     * See {@link Parser#removeFunction(String)} 
     */
    public static FunctionCompute removeFunction(String id){
        return INSTANCE.removeFunction(id);
    }

    /**
     * See {@link Parser#parse(String)}
     */
    public static Formula parse(String formula){
        return INSTANCE.parse(formula);
    }

    /**
     * See {@link Parser#parseNoFunctions(String)}
     */
    public static Formula parseNoFunctions(String formula){
        return INSTANCE.parseNoFunctions(formula);
    }

    /**
     * See {@link Parser#parseFormula(String, boolean, boolean)}
     */
    public static Formula parseFormula(String formula, boolean acceptNull, boolean hasFunctions) {
        return INSTANCE.parseFormula(formula, acceptNull, hasFunctions);
    }

}