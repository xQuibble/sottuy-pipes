package net.fabricmc.example.blocks.entites;

import net.fabricmc.example.blocks.BasicItemPipe;
import net.fabricmc.example.interfaces.EntityWithController;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class BasicItemPipeEntity extends BlockEntity implements EntityWithController {
    private ArrayList<BlockPos> controllerPoses = new ArrayList<>();
    private boolean isController = false;
    private int nextUpdate = 0;

    private ArrayList<Storage<ItemVariant>> storageInputs = new ArrayList<>();
    private ArrayList<Storage<ItemVariant>> storageOutputs = new ArrayList<>();

    private ArrayList<BlockPos> pipePoses = new ArrayList<>();
    private ArrayList<BlockPos> storagePoses = new ArrayList<>();

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        isController = nbt.getBoolean("controller");
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        nbt.putBoolean("controller", isController);

        super.writeNbt(nbt);
    }

    boolean hasStoragePos(BlockPos pos)
    {
        return storagePoses.contains(pos);
    }

    boolean hasPipePos(BlockPos pos)
    {
        return pipePoses.contains(pos);
    }

    void addPipePos(BlockPos pos)
    {
        pipePoses.add(pos);
    }

    void addStoragePos(BlockPos pos)
    {
        storagePoses.add(pos);
    }

    public ArrayList<Storage<ItemVariant>> getStorageInputs() {
        return storageInputs;
    }

    public ArrayList<Storage<ItemVariant>> getStorageOutputs() {
        return storageOutputs;
    }

    public void update()
    {
        if (nextUpdate <= 0)
        {
            nextUpdate = 20*5;
            initController(this.world, this.pos);
        }
        else
        {
            nextUpdate -= 1;
        }
    }

    public boolean isReady()
    {
        if (storageOutputs.isEmpty() || storageInputs.isEmpty())
            return false;

        return true;
    }



    public BasicItemPipeEntity(BlockPos pos, BlockState state) {
        super(BasicItemPipe.ENTITY_TYPE, pos, state);
    }

    //Ticking
    public static void tick(World world1, BlockPos pos, BlockState state1, BasicItemPipeEntity be) {
        if (be.isController())
        {
            be.update();
            if (!be.isReady())
                return;

            boolean done = false;
            for (Storage<ItemVariant> storage: be.getStorageInputs()) {
                for (Storage<ItemVariant> targetStorage: be.getStorageOutputs()) {
                    if (done)
                        break;
                    try (Transaction transaction = Transaction.openOuter()) {
                        if (StorageUtil.move(storage, targetStorage, variant -> true, 1, transaction) > 0)
                        {
                            transaction.commit();
                            done = true;
                        }
                    }
                }
            }
        }
    }




    // Server<->Clientsync
    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }


    @Override
    public void addControllerPos(BlockPos pos) {
        if (!controllerPoses.contains(pos))
            controllerPoses.add(pos);
    }

    @Override
    public boolean isControlled() {
        return controllerPoses.isEmpty() == false;
    }

    @Override
    public boolean isController() {
        return isController;
    }

    public void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (isController)
        {
            player.sendMessage(Text.of("Not a controller anymore"));
            isController = false;
            storageInputs.clear();
            pipePoses.clear();
            storagePoses.clear();
            storageOutputs.clear();
            return;
        }
        player.sendMessage(Text.of("This is now a controller"));
        isController = true;
        initController(world, pos);
    }

    private void findPipesAndOutput(World world, BlockPos pos, BlockPos controllerPos, BasicItemPipeEntity controllerEntity, boolean findStorage)
    {
        BlockEntity pipe = world.getBlockEntity(pos);
        if (pipe != null && findStorage) // findStorage is only false on first call which is the controller itself
            if (((BasicItemPipeEntity) pipe).isController())
                return;
        for (Direction direction : Direction.values())
        {
            BlockPos tmp = pos.offset(direction);
            if (findStorage)
            {
                if (!controllerEntity.hasStoragePos(tmp))
                {
                    Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, tmp, direction);
                    if (storage != null)
                        storageOutputs.add(storage);
                }
            }

            if (!controllerEntity.hasPipePos(tmp))
            {
                BlockEntity blockEntity = world.getBlockEntity(tmp);
                if (blockEntity != null)
                {
                    if (blockEntity instanceof BasicItemPipeEntity)
                    {
                        ((BasicItemPipeEntity) blockEntity).addControllerPos(controllerPos);
                        controllerEntity.addPipePos(tmp);
                        findPipesAndOutput(world, tmp, controllerPos, controllerEntity, true);
                    }
                }
            }
        }
    }

    public boolean initController(World world, BlockPos pos)
    {
        // clear
        storageInputs.clear();
        pipePoses.clear();
        storagePoses.clear();
        storageOutputs.clear();

        // add ourself to known pipes
        addPipePos(pos);

        //check all directions for input inventorys
        for (Direction direction : Direction.values())
        {
            BlockPos tmp = pos.offset(direction);
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, tmp, direction);
            if (storage != null)
            {
                storageInputs.add(storage);
                addStoragePos(tmp);
            }

        }

        if (storageInputs.isEmpty())
            return false;

        //search pipes & output inventorys
        findPipesAndOutput(world, pos, pos, this, false);

        if (storageOutputs.isEmpty())
        {
            storageInputs.clear();
            pipePoses.clear();
            storagePoses.clear();
            storageOutputs.clear();
            return false;
        }

        return true;
    }

    public void neighborUpdate(World world, BlockPos pos) {
        if (isController)
            initController(this.world, this.pos);
        else
        {
            if (isControlled())
            {
                for (BlockPos controllerPos : controllerPoses)
                {
                    BlockEntity pipe = world.getBlockEntity(controllerPos);
                    if (pipe != null)
                    {
                        if (pipe instanceof BasicItemPipeEntity)
                        {
                            BasicItemPipeEntity pipeEntity = (BasicItemPipeEntity) pipe;
                            if (!pipeEntity.isController())
                                continue;
                            pipeEntity.initController(world, controllerPos);
                        }
                    }
                }
            }
        }
    }
}
