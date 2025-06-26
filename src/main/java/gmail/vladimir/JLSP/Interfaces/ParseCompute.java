package gmail.vladimir.JLSP.Interfaces;

import gmail.vladimir.JLSP.Variables.Formula;

/**
 * Interface used to store custom parsing logic that replaces the default one
 */
@FunctionalInterface
public interface ParseCompute {

    Formula parse(String toParse);

}
