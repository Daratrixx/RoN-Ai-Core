package com.daratrix.ronapi.models.savedata;

import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.ApiResource;
import com.solegendary.reignofnether.resources.ResourceName;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorldApiResourceSaveData extends SavedData {

    public ArrayList<ApiResource> resources = new ArrayList<>();

    private static WorldApiResourceSaveData create() {
        return new WorldApiResourceSaveData();
    }

    public static WorldApiResourceSaveData getInstance(ServerLevel level) {
        if (level == null) {
            return create();
        }
        return level
                .getDataStorage()
                .computeIfAbsent(WorldApiResourceSaveData::load, WorldApiResourceSaveData::create, "ron-api-world-resource-saved-data");
    }

    public static WorldApiResourceSaveData load(CompoundTag tag) {
        WorldApiResourceSaveData output = create();
        System.out.println("Attempting to load WorldApiResourceSaveData...");
        ListTag resourceNodes = (ListTag) tag.get("resourceNodes");
        if (resourceNodes == null) {
            System.out.println("Nothing to load for WorldApiResourceSaveData");
            return output;
        }

        System.out.println("Clear API resource data");

        for (Tag listTag : resourceNodes) {
            var nodeTag = (CompoundTag) listTag;
            var type = Enum.valueOf(ResourceName.class, nodeTag.getString("type"));
            var amount = nodeTag.getInt("amount");
            var blocks = (ListTag) nodeTag.get("blocks");
            var blockCount = blocks.size();
            var longBlocks = new long[blockCount];
            //var intBlocks = new long[blockCount*3];
            for (int b = 0; b < blockCount; ++b) {
                var blockTag = (CompoundTag) blocks.get(b);
                longBlocks[b] = blockTag.getLong("l");
                //longBlocks[b + 0] = blockTag.getLong("x");
                //longBlocks[b + 1] = blockTag.getLong("y");
                //longBlocks[b + 2] = blockTag.getLong("z");
            }

            var apiResource = ApiResource.fromFileData(type, longBlocks, amount);
            //var apiResource = ApiResource.fromFileData(type, intBlocks, amount);
            output.resources.add(apiResource);
        }

        System.out.println("Loaded x" + resourceNodes.size() + " resource nodes from WorldApiResourceSaveData");

        return output;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        System.out.println("Attempting to save WorldApiResourceSaveData...");
        ListTag resourceNodes = new ListTag();

        this.resources.forEach(resource -> {
            var nodeTag = new CompoundTag();
            nodeTag.putString("type", resource.getResourceType().name());
            nodeTag.putInt("amount", resource.getAmount());
            var blockTags = new ListTag();
            resource.getBlocks().forEach(block -> {
                var blockTag = new CompoundTag();
                blockTag.putLong("l", block.asLong());
                //blockTag.putInt( "x", block.getX());
                //blockTag.putInt( "y", block.getY());
                //blockTag.putInt( "z", block.getZ());
                blockTags.add(blockTag);
            });
            nodeTag.put("blocks", blockTags);
            resourceNodes.add(nodeTag);
        });

        tag.put("resourceNodes", resourceNodes);

        System.out.println("Saving x" + resourceNodes.size() + " resource nodes to WorldApiResourceSaveData");
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
