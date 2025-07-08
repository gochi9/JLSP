package gmail.vladimir.JLSP.Helpers;

import gmail.vladimir.JLSP.Variables.FormulaEntity;

/**
 * Specialized map. Already sorted. Supports basic operations only
 */
public class TempMap {

    private final TempList[] data;
    private final int[] tempIndexes;
    private final boolean[] existsTemp;
    private short tempIndex, size;

    public TempMap(int max) {
        this.data = new TempList[max];
        this.tempIndexes = new int[max];
        this.existsTemp = new boolean[max];
        this.tempIndex = 0;
        this.size = 0;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public TempList getAndRemoveLast() {
        if (isEmpty())
            return null;

        TempList d = data[data.length - 1];
        data[data.length - 1] = null;
        return d;
    }

    public TempList put(int key, TempList list) {
        tempIndexes[tempIndex++] = key;
        existsTemp[key] = true;
        size++;
        data[key] = list;
        return list;
    }

    public TempList sureGet(int key){
        return data[key];
    }

    public TempList getOrAdd(int key) {
        if (key < data.length && data[key] != null) {
            if (!existsTemp[key]) {
                tempIndexes[tempIndex++] = key;
                existsTemp[key] = true;
            }

            return data[key];
        }

        return put(key, new TempList());
    }

    public void clear() {
        int size = data.length;
        for (int i = 0; i < size; i++)
            data[i] = null;
        tempIndex = 0;
        this.size = 0;
    }

    public void clearLists(boolean clearTemp) {
        for (int i = 0; i < tempIndex; i++) {
            TempList list = data[tempIndexes[i]];
            if (clearTemp) list.clearTemp();
            else list.removeTemp();
        }
        tempIndex = 0;
    }

    public TempList complete() {
        if (size == 0)
            return new TempList(0);

        int length = 0;
        int size = this.data.length;

        for (int i = 0; i < size; i++) {
            TempList list = data[i];
            if (list != null)
                length += list.size();
        }

        TempList ret = new TempList(length + 3);

        for (int i = 0; i < size; i++) {
            TempList list = data[i];

            if (list == null)
                continue;

            int smallSize = list.size();
            for (int k = 0; k < smallSize; k++) {
                FormulaEntity<?> entity = list.get(k);
                if (entity != null)
                    ret.add(entity);
            }
        }

        return ret;
    }
}
