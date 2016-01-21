package org.qcri.rheem.core.plan;

/**
 * An operator is any node that within a data flow plan.
 */
public interface Operator {

    default int getNumInputs() {
        return getAllInputs().length;
    }

    default int getNumOutputs() {
        return getAllOutputs().length;
    }

    InputSlot<?>[] getAllInputs();

    OutputSlot<?>[] getAllOutputs();

    default InputSlot<?> getInput(int index) {
        final InputSlot[] allInputs = getAllInputs();
        if (index < 0 || index >= allInputs.length) {
            throw new IllegalArgumentException(String.format("Illegal input index: %d.", index));
        }
        return allInputs[index];
    }

    default OutputSlot<?> getOutput(int index) {
        final OutputSlot[] allOutputs = getAllOutputs();
        if (index < 0 || index >= allOutputs.length) {
            throw new IllegalArgumentException(String.format("Illegal output index: %d.", index));
        }
        return allOutputs[index];
    }

    default InputSlot<?> getInput(String name) {
        for (InputSlot inputSlot : getAllInputs()) {
            if (inputSlot.getName().equals(name)) return inputSlot;
        }
        throw new IllegalArgumentException(String.format("No slot with such name: %s", name));
    }

    default OutputSlot<?> getOutput(String name) {
        for (OutputSlot outputSlot : getAllOutputs()) {
            if (outputSlot.getName().equals(name)) return outputSlot;
        }
        throw new IllegalArgumentException(String.format("No slot with such name: %s", name));
    }

    /**
     * Connect an output of this operator to the input of a second operator.
     *
     * @param thisOutputIndex index of the output slot to connect to
     * @param that            operator to connect to
     * @param thatInputIndex  index of the input slot to connect from
     */
    default <T> void connectTo(int thisOutputIndex, Operator that, int thatInputIndex) {
        final InputSlot<T> inputSlot = (InputSlot<T>) that.getInput(thatInputIndex);
        final OutputSlot<T> outputSlot = (OutputSlot<T>) this.getOutput(thisOutputIndex);
        if (!inputSlot.getType().isCompatibleTo(outputSlot.getType())) {
            throw new IllegalArgumentException("Cannot connect slots: mismatching types");
        }
        outputSlot.connectTo(inputSlot);
    }


    /**
     * Retrieve the operator that is connected to the input at the given index. If necessary, escape the current
     * parent.
     *
     * @param inputIndex the index of the input to consider
     * @return the input operator or {@code null} if no such operator exists
     * @see #getParent()
     */
    default Operator getInputOperator(int inputIndex) {
        return getInputOperator(getInput(inputIndex));
    }

    /**
     * Retrieve the operator that is connected to the given input. If necessary, escape the current parent.
     *
     * @param input the index of the input to consider
     * @return the input operator or {@code null} if no such operator exists
     * @see #getParent()
     */
    default Operator getInputOperator(InputSlot<?> input) {
        if (!isOwnerOf(input)) {
            throw new IllegalArgumentException("Slot does not belong to this operator.");
        }

        // Try to exit through the parent.
        final Operator parent = this.getParent();
        if (parent != null) {
            if (parent instanceof Subplan) {
                final InputSlot<?> exitInputSlot = ((Subplan) parent).exit(input);
                if (exitInputSlot != null) {
                    return parent.getInputOperator(exitInputSlot);
                }

            } else if (parent instanceof OperatorAlternative.Alternative) {
                final InputSlot<?> exitInputSlot = ((OperatorAlternative.Alternative) parent).exit(input);
                if (exitInputSlot != null) {
                    return parent.getInputOperator(exitInputSlot);
                }
            }

        }

        final OutputSlot occupant = input.getOccupant();
        return occupant == null ? null : occupant.getOwner();
    }

    /**
     * Retrieve the outermost {@link InputSlot} if this operator is nested in other operators.
     *
     * @param input the slot to track
     * @return the outermost {@link InputSlot}
     * @see #getParent()
     */
    default <T> InputSlot<T> getOutermostInputSlot(InputSlot<T> input) {
        if (!isOwnerOf(input)) {
            throw new IllegalArgumentException("Slot does not belong to this operator.");
        }

        // Try to exit through the parent.
        final Operator parent = this.getParent();
        if (parent != null) {
            if (parent instanceof Subplan) {
                final InputSlot<T> parentInputSlot = ((Subplan) parent).exit(input);
                if (parentInputSlot != null) {
                    return parent.getOutermostInputSlot(parentInputSlot);
                }

            } else if (parent instanceof OperatorAlternative.Alternative) {
                final InputSlot<T> parentInputSlot = ((OperatorAlternative.Alternative) parent).exit(input);
                if (parentInputSlot != null) {
                    return parent.getOutermostInputSlot(parentInputSlot);
                }
            }

        }

        return input;
    }

    default boolean isOwnerOf(Slot<?> slot) {
        return slot.getOwner() == this;
    }

    /**
     * Tells whether this operator is a sink, i.e., it has no outputs.
     *
     * @return whether this operator is a sink
     */
    default boolean isSink() {
        return this.getNumOutputs() == 0;
    }

    /**
     * Tells whether this operator is a source, i.e., it has no inputs.
     *
     * @return whether this operator is a source
     */
    default boolean isSource() {
        return this.getNumInputs() == 0;
    }

    default boolean isSubplan() {
        return this instanceof Subplan;
    }

    default boolean isAlternative() {
        return this instanceof OperatorAlternative;
    }

    default boolean isExecutionOperator() {
        return this instanceof ExecutionOperator;
    }

    /**
     * This method is part of the visitor pattern and calls the appropriate visit method on {@code visitor}.
     */
    <Payload, Return> Return accept(TopDownPlanVisitor<Payload, Return> visitor, OutputSlot<?> outputSlot, Payload payload);

//    /**
//     * This method is part of the visitor pattern and calls the appropriate visit method on {@code visitor}.
//     *
//     * @return return values associated with the inner-most input slot
//     */
//    <Payload, Return> Map<InputSlot, Return> accept(BottomUpPlanVisitor<Payload, Return> visitor, InputSlot<?> inputSlot, Payload payload);

    /**
     * Operators can be nested in other operators, e.g., in a {@link Subplan} or a {@link OperatorAlternative}.
     *
     * @return the parent of this operator or {@code null} if this is a top-level operator
     */
    default CompositeOperator getParent() {
        final OperatorContainer container = getContainer();
        return container == null ?
                null :
                container.toOperator();
    }

    OperatorContainer getContainer();

    /**
     * Operators can be nested in other operators, e.g., in a {@link Subplan} or a {@link OperatorAlternative.Alternative}.
     *
     * @param newContainer the new container of this operator or {@code null} to declare it top-level
     */
    void setContainer(OperatorContainer newContainer);

    /**
     * Each operator is associated with an epoch, which is a logical timestamp for the operator's creation.
     * This value is the lowest timestamp and default epoch.
     */
    int FIRST_EPOCH = 0;

    /**
     * <i>Optional operation for non-composite operators.</i>
     *
     * @param epoch the operator's new epoch value
     * @see #FIRST_EPOCH
     */
    void setEpoch(int epoch);

    /**
     * <i>Optional operation for non-composite operators.</i>
     *
     * @return the operator's epoch value
     * @see #FIRST_EPOCH
     */
    int getEpoch();


    /**
     * @return whether this is an elementary operator
     */
    default boolean isElementary() {
        return true;
    }
}
