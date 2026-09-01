package com.coffeeshop.model;

import java.util.List;

/** Effective modifier selection rules for one active product assignment. */
public record PosModifierGroup(long assignmentId, long groupId, String name,
                               int minimumSelections, int maximumSelections,
                               List<PosModifierOption> options) {
    public PosModifierGroup { options=List.copyOf(options); }
}
