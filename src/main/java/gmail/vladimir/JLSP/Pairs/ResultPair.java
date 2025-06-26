package gmail.vladimir.JLSP.Pairs;

import java.util.concurrent.CompletableFuture;

public class ResultPair extends Pair<Boolean, CompletableFuture<Double>> {

    public ResultPair(Boolean key, CompletableFuture<Double> value) {
        super(key, value);
    }

}
