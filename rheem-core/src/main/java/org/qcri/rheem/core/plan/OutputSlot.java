package org.qcri.rheem.core.plan;

import org.qcri.rheem.core.types.DataSetType;

import java.util.*;

/**
 * An output slot declares an output of an {@link Operator}.
 */
public class OutputSlot<T> extends Slot<T> {

    private final List<InputSlot<T>> occupiedSlots = new LinkedList<>();

    /**
     * Copy the {@link OutputSlot}s of a given {@link Operator}.
     */
    public static void mock(Operator template, Operator mock) {
        if (template.getNumOutputs() != mock.getNumOutputs()) {
            throw new IllegalArgumentException("Cannot mock outputs: Mismatching number of outputs.");
        }

        OutputSlot[] mockSlots = mock.getAllOutputs();
        for (int i = 0; i < template.getNumOutputs(); i++) {
            mockSlots[i] = template.getOutput(i).copyFor(mock);
        }
    }

    /**
     * Take the output connections away from one operator and give them to another one.
     */
    public static void stealConnections(Operator victim, Operator thief) {
        if (victim.getNumOutputs() != thief.getNumOutputs()) {
            throw new IllegalArgumentException("Cannot steal outputs: Mismatching number of outputs.");
        }

        for (int i = 0; i < victim.getNumOutputs(); i++) {
            final List<? extends InputSlot<?>> occupiedSlots = victim.getOutput(i).getOccupiedSlots();
            for (InputSlot<?> occupiedSlot : occupiedSlots) {
                ((OutputSlot<Object>) victim.getOutput(i)).disconnectFrom((InputSlot<Object>) occupiedSlot);
                ((OutputSlot<Object>) thief.getOutput(i)).connectTo((InputSlot<Object>) occupiedSlot);
            }
        }
    }

    public OutputSlot(OutputSlot blueprint, Operator owner) {
        this(blueprint.getName(), owner, blueprint.getType());
    }

    public OutputSlot(String name, Operator owner, DataSetType type) {
        super(name, owner, type);
    }

    @Override
    public int getIndex() throws IllegalStateException {
        if (Objects.isNull(getOwner())) throw new IllegalStateException("This slot has no owner.");
        for (int i = 0; i < getOwner().getNumOutputs(); i++) {
            if (getOwner().getOutput(i) == this) return i;
        }
        throw new IllegalStateException("Could not find this slot within its owner.");
    }

    public OutputSlot copyFor(Operator owner) {
        return new OutputSlot(this, owner);
    }

    /**
     * Connect this output slot to an input slot. The input slot must not be occupied already.
     *
     * @param inputSlot the input slot to connect to
     */
    public void connectTo(InputSlot<T> inputSlot) {
        if (inputSlot.getOccupant() != null) {
            throw new IllegalStateException("Cannot connect: input slot is already occupied");
        }

        occupiedSlots.add(inputSlot);
        inputSlot.setOccupant(this);
    }

    public void disconnectFrom(InputSlot<T> inputSlot) {
        if (inputSlot.getOccupant() != this) {
            throw new IllegalStateException("Cannot disconnect: input slot is not occupied by this output slot");
        }

        occupiedSlots.remove(inputSlot);
        inputSlot.setOccupant(null);
    }

    public List<InputSlot<T>> getOccupiedSlots() {
        return occupiedSlots;
    }

    @SuppressWarnings("unchecked")
    public OutputSlot<Object> unchecked() {
        return (OutputSlot<Object>) this;
    }

    /**
     * Recursively follow the given {@code outputSlot}.
     *
     * @param outputSlot the {@link OutputSlot} to follow
     * @return the interfacing {@link OutputSlot}s (either belong to a top-level {@link Operator} or occupy an
     * {@link InputSlot}) that represent given {@code outputSlot}
     * @see Operator#getContainer()
     * @see OperatorContainer#followOutput(OutputSlot)
     */
    public static <T> Collection<OutputSlot<T>> followOutputRecursively(OutputSlot<T> outputSlot) {
        Queue<OutputSlot<T>> processableOutputs = new LinkedList<>();
        processableOutputs.add(outputSlot);
        Collection<OutputSlot<T>> resolvedOutputs = new LinkedList<>();

        while (!processableOutputs.isEmpty()) {
            final OutputSlot<T> processableOutput = processableOutputs.poll();
            if (!processableOutput.getOccupiedSlots().isEmpty() || processableOutput.getOwner().getContainer() == null) {
                resolvedOutputs.add(processableOutput);
            } else {
                final OperatorContainer container = processableOutput.getOwner().getContainer();
                processableOutputs.addAll(container.followOutput(processableOutput));
            }
        }

        return resolvedOutputs;
    }
}
