package org.qcri.rheem.basic.operators;

import org.apache.commons.lang3.Validate;
import org.qcri.rheem.core.optimizer.costs.CardinalityEstimator;
import org.qcri.rheem.core.optimizer.costs.DefaultCardinalityEstimator;
import org.qcri.rheem.core.plan.BinaryToUnaryOperator;
import org.qcri.rheem.core.plan.Operator;
import org.qcri.rheem.core.plan.OperatorContainer;
import org.qcri.rheem.core.types.DataSetType;

import java.util.Optional;

/**
 * This {@link Operator} creates the union (bag semantics) of two .
 */
public class CoalesceOperator<Type> extends BinaryToUnaryOperator<Type, Type, Type> {
    /**
     * Creates a new instance.
     *
     * @param type      the type of the datasets to be coalesced
     */
    public CoalesceOperator(DataSetType<Type> type) {
        super(type, type, type);
    }

    @Override
    public Optional<CardinalityEstimator> getCardinalityEstimator(int outputIndex) {
        Validate.inclusiveBetween(0, this.getNumOutputs() - 1, outputIndex);
        return Optional.of(new DefaultCardinalityEstimator(1d, 2, inputCards -> inputCards[0] + inputCards[1]));
    }
}
