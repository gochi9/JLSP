package gmail.vladimir.JLSP.Helpers;

import gmail.vladimir.JLSP.Variables.FormulaEntity;

/**
 * Specialized list. Supports basic operations only
 */
public class TempList{

    private FormulaEntity<?>[] array;
    private short size = 0, tempSize = 0;
    private int length;

    public TempList() {
        this.array = new FormulaEntity[16];
        this.length = 16;
    }

    public TempList(int size) {
        this.array = new FormulaEntity[size];
        this.length = size;
    }

    public boolean isEmpty(){
        return size == 0;
    }

    public void clear(){
        this.size = 0;
        this.tempSize = 0;
    }

    public short size(){
        return size;
    }

    public void removeLast(){
        if(tempSize > 0)
            tempSize--;

        if(size > 0)
            size--;
    }

    public FormulaEntity<?> getAndRemoveLast(){
        FormulaEntity<?> ent = array[size - 1];
        removeLast();
        return ent;
    }

    public FormulaEntity<?> getFirst(){
        return array[0];
    }

    protected final FormulaEntity<?> get(int i){
        return array[i];
    }

    public void add(FormulaEntity<?> e) {
        if(size == length) {
            FormulaEntity<?>[] newArray = new FormulaEntity[length *= 3];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray;
        }

        array[size] = e;
        size++;
        tempSize++;
    }

    public void clearTemp(){
        tempSize = 0;
    }

    public void removeTemp(){
        for (int i = 0; i < tempSize; i++)
            array[--size] = null;

        clearTemp();
    }

    public FormulaEntity<?>[] getArray(){
        return array;
    }

}
