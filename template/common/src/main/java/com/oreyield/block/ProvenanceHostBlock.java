package com.oreyield.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
//? if block_properties_require_id {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
//?}

/**
 * A visually and mechanically equivalent host block which persists its origin in
 * the block id itself.  This survives saves, chunk unloads, and piston movement.
 */
public final class ProvenanceHostBlock extends Block {
    private final Block source;
    private final HostOrigin origin;

    //? if block_properties_require_id {
    public ProvenanceHostBlock(Block source, HostOrigin origin, ResourceLocation id) {
        super(propertiesFor(source, id));
        this.source = source;
        this.origin = origin;
        ProvenanceHostBlocks.register(source, this, origin);
    }
    //?} else {
    public ProvenanceHostBlock(Block source, HostOrigin origin) {
        super(propertiesFor(source));
        this.source = source;
        this.origin = origin;
        ProvenanceHostBlocks.register(source, this, origin);
    }
    //?}

    public Block source() {
        return source;
    }

    public HostOrigin origin() {
        return origin;
    }

    //? if block_properties_require_id {
    private static BlockBehaviour.Properties propertiesFor(Block source, ResourceLocation id) {
        return BlockBehaviour.Properties.ofFullCopy(source)
                .setId(ResourceKey.create(Registries.BLOCK, id));
    }
    //?} else {
    private static BlockBehaviour.Properties propertiesFor(Block source) {
        //? if block_properties_full_copy {
        return BlockBehaviour.Properties.ofFullCopy(source);
        //?} else {
        return BlockBehaviour.Properties.copy(source).dropsLike(source);
        //?}
    }
    //?}
}
