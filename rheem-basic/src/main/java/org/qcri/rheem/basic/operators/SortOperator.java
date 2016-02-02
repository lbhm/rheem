package org.qcri.rheem.basic.operators;

import org.apache.commons.lang3.Validate;
import org.qcri.rheem.core.function.ReduceDescriptor;
import org.qcri.rheem.core.optimizer.costs.CardinalityEstimator;
import org.qcri.rheem.core.optimizer.costs.DefaultCardinalityEstimator;
import org.qcri.rheem.core.plan.UnaryToUnaryOperator;
import org.qcri.rheem.core.types.DataSetType;

import java.util.Optional;


/**
 * This operator sorts the elements in this dataset.
 */
public class SortOperator<Type> extends UnaryToUnaryOperator<Type, Type> {


    /**
     * Creates a new instance.
     *
     * @param type type of the dataunit elements
     */
    public SortOperator(DataSetType<Type> type) {
        super(type, type, null);
    }

    @Override
    public Optional<CardinalityEstimator> getCardinalityEstimator(int outputIndex) {
        Validate.inclusiveBetween(0, this.getNumOutputs() - 1, outputIndex);

        return Optional.of(new DefaultCardinalityEstimator(1d, 1, inputCards -> inputCards[0]));
    }

}
