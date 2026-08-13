package dev.accurateplace.protocol;

import dev.accurateplace.AccuratePlace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Decodes the Accurate Block Placement protocol v3 embedded in the hit X coordinate. */
public final class PlacementProtocolV3 {
    private static final Set<Property<?>> ALLOWED_PROPERTIES = Set.of(
            BlockStateProperties.INVERTED,
            BlockStateProperties.OPEN,
            BlockStateProperties.BELL_ATTACHMENT,
            BlockStateProperties.AXIS,
            BlockStateProperties.HALF,
            BlockStateProperties.ATTACH_FACE,
            BlockStateProperties.CHEST_TYPE,
            BlockStateProperties.MODE_COMPARATOR,
            BlockStateProperties.DOOR_HINGE,
            BlockStateProperties.FACING,
            BlockStateProperties.FACING_HOPPER,
            BlockStateProperties.HORIZONTAL_FACING,
            BlockStateProperties.ORIENTATION,
            BlockStateProperties.RAIL_SHAPE,
            BlockStateProperties.RAIL_SHAPE_STRAIGHT,
            BlockStateProperties.SLAB_TYPE,
            BlockStateProperties.STAIRS_SHAPE,
            BlockStateProperties.BITES,
            BlockStateProperties.DELAY,
            BlockStateProperties.NOTE,
            BlockStateProperties.ROTATION_16
    );

    private PlacementProtocolV3() {}

    public static BlockState apply(BlockState vanillaState, BlockPlaceContext context) {
        int protocolValue = (int) (context.getClickLocation().x - context.getClickedPos().getX()) - 2;
        if (protocolValue < 0) {
            return vanillaState;
        }

        BlockState state = vanillaState;
        BlockState lastValidState = vanillaState;
        Optional<DirectionProperty> directionProperty = firstDirectionProperty(state);

        try {
            if (directionProperty.isPresent()
                    && directionProperty.get() != BlockStateProperties.VERTICAL_DIRECTION) {
                state = applyDirection(state, context, directionProperty.get(), protocolValue);
                if (state == null) {
                    return null;
                }
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    lastValidState = state;
                } else {
                    state = lastValidState;
                }
                protocolValue >>>= 3;
            }

            // One reserved bit follows the optional direction field.
            protocolValue >>>= 1;

            List<Property<?>> properties = new ArrayList<>(state.getProperties());
            properties.sort(Comparator.comparing(Property::getName));

            for (Property<?> property : properties) {
                if (directionProperty.filter(property::equals).isPresent()
                        || !ALLOWED_PROPERTIES.contains(property)) {
                    continue;
                }

                DecodeResult result = decodeProperty(state, property, protocolValue);
                if (!result.consumed()) {
                    continue;
                }
                protocolValue >>>= result.bits();

                if (result.state() != state) {
                    BlockState candidate = result.state();
                    if (candidate.canSurvive(context.getLevel(), context.getClickedPos())) {
                        state = candidate;
                        lastValidState = candidate;
                    }
                }
            }

            // Protocol values may never synthesize powered or waterlogged states.
            state = restoreDeniedProperty(state, vanillaState, BlockStateProperties.POWERED, false);
            state = restoreDeniedProperty(
                    state,
                    vanillaState,
                    BlockStateProperties.WATERLOGGED,
                    vanillaState.hasProperty(BlockStateProperties.WATERLOGGED)
                            && vanillaState.getValue(BlockStateProperties.WATERLOGGED)
            );

            return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : lastValidState;
        } catch (RuntimeException exception) {
            AccuratePlace.LOGGER.warn("Ignoring malformed accurate-placement value", exception);
            return vanillaState;
        }
    }

    private static Optional<DirectionProperty> firstDirectionProperty(BlockState state) {
        return state.getProperties().stream()
                .filter(DirectionProperty.class::isInstance)
                .map(DirectionProperty.class::cast)
                .findFirst();
    }

    private static BlockState applyDirection(
            BlockState state,
            BlockPlaceContext context,
            DirectionProperty property,
            int protocolValue
    ) {
        Direction original = state.getValue(property);
        Direction facing = original;
        int directionIndex = (protocolValue & 0xF) >> 1;

        if (directionIndex == 6) {
            facing = original.getOpposite();
        } else if (directionIndex <= 5) {
            facing = Direction.from3DDataValue(directionIndex);
            if (!property.getPossibleValues().contains(facing)) {
                facing = context.getHorizontalDirection().getOpposite();
            }
        }

        if (facing == original || !property.getPossibleValues().contains(facing)) {
            return state;
        }

        if (state.getBlock() instanceof BedBlock) {
            BlockPos headPos = context.getClickedPos().relative(facing);
            if (!context.getLevel().getBlockState(headPos).canBeReplaced(context)) {
                return null;
            }
        }

        return state.setValue(property, facing);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DecodeResult decodeProperty(BlockState state, Property property, int protocolValue) {
        List<Comparable> values = new ArrayList<>(property.getPossibleValues());
        values.sort(Comparator.naturalOrder());

        int bits = Mth.log2(Mth.smallestEncompassingPowerOfTwo(values.size()));
        int index = protocolValue & ((1 << bits) - 1);
        if (index < 0 || index >= values.size()) {
            return new DecodeResult(state, bits, false);
        }

        Comparable value = values.get(index);
        if (value == SlabType.DOUBLE || state.getValue(property).equals(value)) {
            return new DecodeResult(state, bits, true);
        }
        return new DecodeResult(state.setValue(property, value), bits, true);
    }

    private static <T extends Comparable<T>> BlockState restoreDeniedProperty(
            BlockState state,
            BlockState vanillaState,
            Property<T> property,
            T fallback
    ) {
        if (!state.hasProperty(property)) {
            return state;
        }
        T value = vanillaState.hasProperty(property) ? vanillaState.getValue(property) : fallback;
        return state.setValue(property, value);
    }

    private record DecodeResult(BlockState state, int bits, boolean consumed) {}
}
