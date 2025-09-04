package eu.nk2.apathy.goal;

import eu.nk2.apathy.context.OnBlockBrokenEventRegistry;
import eu.nk2.apathy.context.OnLivingEntityDeadEventRegistry;
import eu.nk2.apathy.logging.ApathyLogger;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Pair;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;

public class ApathyIfBlockBrokenActiveTargetGoal extends ActiveTargetGoal<PlayerEntity> {
    private final Logger logger = LogManager.getLogger("Apathy");

    private final float maximalReactionDistance;
    private final Block reactionBlock;

    private final UUID onBlockBrokenHandlerId;
    private UUID onLivingEntityDeadHandlerId;

    private final Map<UUID, BlockState> playerMemory = new HashMap<>();

    public ApathyIfBlockBrokenActiveTargetGoal(
        MobEntity mob,
        int reciprocalChance,
        boolean checkVisibility,
        boolean checkCanNavigate,
        TargetPredicate targetPredicate,
        float maximalReactionDistance,
        Block reactionBlock
    ) {
        super(
            mob,
            PlayerEntity.class,
            reciprocalChance,
            checkVisibility,
            checkCanNavigate,
            null
        );
        this.targetPredicate = targetPredicate;
        this.maximalReactionDistance = maximalReactionDistance;
        this.reactionBlock = reactionBlock;

        this.onBlockBrokenHandlerId = OnBlockBrokenEventRegistry.INSTANCE.registerOnBlockBrokenHandler((pos, state, playerUuid) -> {
            PlayerEntity player = this.mob
                .getWorld()
                .getPlayerByUuid(playerUuid);
            if (player == null) {
                return;
            }

            ApathyLogger.debug(
                "[{}] Block broken: {} {}",
                this.mob,
                playerUuid,
                state
            );
            if (state
                .getBlock()
                .getDefaultState()
                .isOf(this.reactionBlock) && mob.distanceTo(player) <= this.maximalReactionDistance) {
                ApathyLogger.debug(
                    "[{}] Add to memory: {}",
                    this.mob,
                    playerUuid
                );
                playerMemory.put(
                    playerUuid,
                    state
                );
            }
        });

        this.onLivingEntityDeadHandlerId = OnLivingEntityDeadEventRegistry.INSTANCE.registerOnLivingEntityDeadHandler((world, livingEntity, damageSource) -> {
            if (this.mob.getId() == livingEntity.getId()) {
                ApathyLogger.debug(
                    "[{}] Unregister goal from events",
                    this.mob
                );
                OnBlockBrokenEventRegistry.INSTANCE.unregisterOnBlockBrokenHandler(onBlockBrokenHandlerId);
                OnLivingEntityDeadEventRegistry.INSTANCE.unregisterOnLivingEntityDeadHandler(onLivingEntityDeadHandlerId);
            }
        });
    }

    @Override
    protected void findClosestTarget() {
        this.playerMemory
            .keySet()
            .stream()
            .map(playerUuid -> mob
                .getWorld()
                .getPlayerByUuid(playerUuid))
            .filter(Objects::nonNull)
            .map(player -> new Pair<>(
                player,
                mob.distanceTo(player)
            ))
            .min(Comparator.comparing(Pair::getRight))
            .ifPresent(playerDistancePair -> {
                this.targetEntity = playerDistancePair.getLeft();
                this.playerMemory.clear();
            });
    }
}
