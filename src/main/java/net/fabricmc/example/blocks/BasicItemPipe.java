package net.fabricmc.example.blocks;
import net.fabricmc.example.blocks.entites.BasicItemPipeEntity;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class BasicItemPipe extends BlockWithEntity {
    public static Block BLOCK;
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static BlockEntityType<BasicItemPipeEntity> ENTITY_TYPE;
    public static final BooleanProperty active = BooleanProperty.of("active");


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(active);
    }

    public BasicItemPipe(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(active, false));
    }



    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return (BlockState)this.getDefaultState().with(Properties.HORIZONTAL_FACING, ctx.getPlayerFacing().getOpposite());
    }

    public static void Register()
    {
        BLOCK = new BasicItemPipe(FabricBlockSettings.of(Material.METAL).strength(4.0f).requiresTool().nonOpaque());
        Identifier ID = new Identifier("sottuy-pipes", "basicitempipe");
        Registry.register(Registries.BLOCK, ID, BLOCK);
        Registry.register(Registries.ITEM, new Identifier("sottuy-pipes", "basicitempipe"), new BlockItem(BLOCK, new FabricItemSettings()));
        ENTITY_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, "sottuy-pipes:basicitempipe_entity", FabricBlockEntityTypeBuilder.create(BasicItemPipeEntity::new, BLOCK).build(null));
    }

    public static void RegisterClient()
    {
        BlockRenderLayerMap.INSTANCE.putBlock(BLOCK, RenderLayer.getCutout());
    }
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // With inheriting from BlockWithEntity this defaults to INVISIBLE, so we need to change that!
        return BlockRenderType.MODEL;
    }
    //@Override
    //public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
    // return checkType(type, ExampleMod.LASER_VERTEX_ENTITY, (world1, pos, state1, be) -> LaserVertexEntity.tick(world1, pos, state1, be));
    //}
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BasicItemPipeEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be == null)
                return ActionResult.SUCCESS;

            ((BasicItemPipeEntity)be).onUse(state, world, pos, player, hand, hit);


            return ActionResult.SUCCESS;
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ENTITY_TYPE, (world1, pos, state1, be) -> {
            if (!world.isClient)
                BasicItemPipeEntity.tick(world1, pos, state1, be);
        });
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be == null)
                return;

            ((BasicItemPipeEntity)be).neighborUpdate(world, pos);
        }
    }
}

