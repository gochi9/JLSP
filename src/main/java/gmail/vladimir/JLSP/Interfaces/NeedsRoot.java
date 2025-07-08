package gmail.vladimir.JLSP.Interfaces;

import gmail.vladimir.JLSP.Variables.Formula;

import java.util.Set;

//A interface for objects that require may require a root, and update said root, to function normally.
@FunctionalInterface
public interface NeedsRoot {

    void setRoot(Formula formula, Set<Character> vars);

}
