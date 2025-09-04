package eu.nk2.apathy.goal;

import eu.nk2.apathy.context.*;
import eu.nk2.apathy.logging.ApathyLogger;

import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;

public class ApathyIfItemSelectedActiveTargetGoal extends ActiveTargetGoal<PlayerEntity> {
    private final Logger logger = LogManager.getLogger("Apathy");

    private final float maximalFollowDistance;
    private final Item reactionItem;
    private final int reactionItemCount;

    private final UUID onHandStackChangedHandlerId;
    private UUID onLivingEntityDeadHandlerId;

    private final Map<UUID, ItemStack> playerMemory = new HashMap<>();

    public ApathyIfItemSelectedActiveTargetGoal(
        MobEntity mob,
        int reciprocalChance,
        boolean checkVisibility,
        boolean checkCanNavigate,
        TargetPredicate targetPredicate,
        float maximalFollowDistance,
        Item reactionItem,
        int reactionItemCount
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
        this.maximalFollowDistance = maximalFollowDistance;
        this.reactionItem = reactionItem;
        this.reactionItemCount = reactionItemCount;

        this.onHandStackChangedHandlerId = OnHandStackChangedEventRegistry.INSTANCE.registerOnHandStackChangedHandler((hand, playerUuid, previousStack, currentStack) -> {
            ApathyLogger.debug(
                "[{}] {} hand stack changed: {}, from: {}, to: {}",
                this.mob,
                hand.name(),
                playerUuid,
                previousStack,
                currentStack
            );

            if (currentStack != null && currentStack.getItem() == this.reactionItem
                && (this.reactionItemCount <= 0 || currentStack.getCount() == this.reactionItemCount)) {
                ApathyLogger.debug(
                    "[{}] Add to memory: {}",
                    this.mob,
                    playerUuid
                );
                playerMemory.put(
                    playerUuid,
                    currentStack
                );
            }

            if (previousStack != null && previousStack.getItem() == this.reactionItem
                && currentStack != null && (currentStack.getItem() != this.reactionItem
                || this.reactionItemCount > 0 && currentStack.getCount() != this.reactionItemCount)) {
                ApathyLogger.debug(
                    "[{}] Remove from memory: {}",
                    this.mob,
                    playerUuid
                );
                playerMemory.remove(playerUuid);
            }
        });

        this.onLivingEntityDeadHandlerId = OnLivingEntityDeadEventRegistry.INSTANCE.registerOnLivingEntityDeadHandler((world, livingEntity, damageSource) -> {
            if (this.mob.getId() == livingEntity.getId()) {
                ApathyLogger.debug(
                    "[{}] Unregister goal from events",
                    this.mob
                );
                OnHandStackChangedEventRegistry.INSTANCE.unregisterOnHandStackChangedHandler(onHandStackChangedHandlerId);
                OnLivingEntityDeadEventRegistry.INSTANCE.unregisterOnLivingEntityDeadHandler(onLivingEntityDeadHandlerId);
                playerMemory.clear();
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
            .filter(playerDistancePair -> playerDistancePair.getRight() <= maximalFollowDistance)
            .min(Comparator.comparing(Pair::getRight))
            .ifPresent(playerDistancePair -> this.targetEntity = playerDistancePair.getLeft());
        }
}
