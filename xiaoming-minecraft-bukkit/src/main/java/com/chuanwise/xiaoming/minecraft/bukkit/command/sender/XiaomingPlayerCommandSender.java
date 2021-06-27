package com.chuanwise.xiaoming.minecraft.bukkit.command.sender;

import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.*;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.*;
import org.bukkit.map.MapView;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.net.InetSocketAddress;
import java.util.*;

public class XiaomingPlayerCommandSender extends XiaomingCommandSender<Player> implements Player {
    public XiaomingPlayerCommandSender(Player commandSender) {
        super(commandSender);
    }

    @Override
    public String getDisplayName() {
        return commandSender.getDisplayName();
    }

    @Override
    public void setDisplayName(String s) {
        commandSender.setDisplayName(s);
    }

    @Override
    public String getPlayerListName() {
        return commandSender.getPlayerListName();
    }

    @Override
    public void setPlayerListName(String s) {
        commandSender.setPlayerListName(s);
    }

    @Override
    public String getPlayerListHeader() {
        return commandSender.getPlayerListHeader();
    }

    @Override
    public String getPlayerListFooter() {
        return commandSender.getPlayerListFooter();
    }

    @Override
    public void setPlayerListHeader(String s) {
        commandSender.setPlayerListHeader(s);
    }

    @Override
    public void setPlayerListFooter(String s) {
        commandSender.setPlayerListFooter(s);
    }

    @Override
    public void setPlayerListHeaderFooter(String s, String s1) {
        commandSender.setPlayerListHeaderFooter(s, s1);
    }

    @Override
    public void setCompassTarget(Location location) {
        commandSender.setCompassTarget(location);
    }

    @Override
    public Location getCompassTarget() {
        return commandSender.getCompassTarget();
    }

    @Override
    public InetSocketAddress getAddress() {
        return commandSender.getAddress();
    }

    @Override
    public void sendRawMessage(String s) {
        commandSender.sendRawMessage(s);
    }

    @Override
    public void kickPlayer(String s) {
        commandSender.kickPlayer(s);
    }

    @Override
    public void chat(String s) {
        commandSender.chat(s);
    }

    @Override
    public boolean performCommand(String s) {
        return commandSender.performCommand(s);
    }

    @Override
    public boolean isSneaking() {
        return commandSender.isSneaking();
    }

    @Override
    public void setSneaking(boolean b) {
        commandSender.setSneaking(b);
    }

    @Override
    public boolean isSprinting() {
        return commandSender.isSprinting();
    }

    @Override
    public void setSprinting(boolean b) {
        commandSender.setSprinting(b);
    }

    @Override
    public void saveData() {
        commandSender.saveData();
    }

    @Override
    public void loadData() {
        commandSender.loadData();
    }

    @Override
    public void setSleepingIgnored(boolean b) {
        commandSender.setSleepingIgnored(b);
    }

    @Override
    public boolean isSleepingIgnored() {
        return commandSender.isSleepingIgnored();
    }

    @Override
    public void playNote(Location location, byte b, byte b1) {
        commandSender.playNote(location, b, b1);
    }

    @Override
    public void playNote(Location location, Instrument instrument, Note note) {
        commandSender.playNote(location, instrument, note);
    }

    @Override
    public void playSound(Location location, Sound sound, float v, float v1) {
        commandSender.playSound(location, sound, v, v1);
    }

    @Override
    public void playSound(Location location, String s, float v, float v1) {
        commandSender.playSound(location, s, v, v1);
    }

    @Override
    public void playSound(Location location, Sound sound, SoundCategory soundCategory, float v, float v1) {
        commandSender.playSound(location, sound, soundCategory, v, v1);
    }

    @Override
    public void playSound(Location location, String s, SoundCategory soundCategory, float v, float v1) {
        commandSender.playSound(location, s, soundCategory, v, v1);
    }

    @Override
    public void stopSound(Sound sound) {
        commandSender.stopSound(sound);
    }

    @Override
    public void stopSound(String s) {
        commandSender.stopSound(s);
    }

    @Override
    public void stopSound(Sound sound, SoundCategory soundCategory) {
        commandSender.stopSound(sound, soundCategory);
    }

    @Override
    public void stopSound(String s, SoundCategory soundCategory) {
        commandSender.stopSound(s, soundCategory);
    }

    @Override
    public void playEffect(Location location, Effect effect, int i) {
        commandSender.playEffect(location, effect, i);
    }

    @Override
    public <T> void playEffect(Location location, Effect effect, T t) {
        commandSender.playEffect(location, effect, t);
    }

    @Override
    public void sendBlockChange(Location location, Material material, byte b) {
        commandSender.sendBlockChange(location, material, b);
    }

    @Override
    public void sendBlockChange(Location location, BlockData blockData) {
        commandSender.sendBlockChange(location, blockData);
    }

    @Override
    public boolean sendChunkChange(Location location, int i, int i1, int i2, byte[] bytes) {
        return commandSender.sendChunkChange(location, i, i1, i2, bytes);
    }

    @Override
    public void sendSignChange(Location location, String[] strings) throws IllegalArgumentException {
        commandSender.sendSignChange(location, strings);
    }

    @Override
    public void sendSignChange(Location location, String[] strings, DyeColor dyeColor) throws IllegalArgumentException {
        commandSender.sendSignChange(location, strings, dyeColor);
    }

    @Override
    public void sendMap(MapView mapView) {
        commandSender.sendMap(mapView);
    }

    @Override
    public void updateInventory() {
        commandSender.updateInventory();
    }

    @Override
    public void incrementStatistic(Statistic statistic) throws IllegalArgumentException {
        commandSender.incrementStatistic(statistic);
    }

    @Override
    public void decrementStatistic(Statistic statistic) throws IllegalArgumentException {
        commandSender.decrementStatistic(statistic);
    }

    @Override
    public void incrementStatistic(Statistic statistic, int i) throws IllegalArgumentException {
        commandSender.incrementStatistic(statistic, i);
    }

    @Override
    public void decrementStatistic(Statistic statistic, int i) throws IllegalArgumentException {
        commandSender.decrementStatistic(statistic, i);
    }

    @Override
    public void setStatistic(Statistic statistic, int i) throws IllegalArgumentException {
        commandSender.setStatistic(statistic, i);
    }

    @Override
    public int getStatistic(Statistic statistic) throws IllegalArgumentException {
        return commandSender.getStatistic(statistic);
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        commandSender.incrementStatistic(statistic, material);
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        commandSender.decrementStatistic(statistic, material);
    }

    @Override
    public int getStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
        return commandSender.getStatistic(statistic, material);
    }

    @Override
    public void incrementStatistic(Statistic statistic, Material material, int i) throws IllegalArgumentException {
        commandSender.incrementStatistic(statistic, material, i);
    }

    @Override
    public void decrementStatistic(Statistic statistic, Material material, int i) throws IllegalArgumentException {
        commandSender.decrementStatistic(statistic, material, i);
    }

    @Override
    public void setStatistic(Statistic statistic, Material material, int i) throws IllegalArgumentException {
        commandSender.setStatistic(statistic, material, i);
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        commandSender.incrementStatistic(statistic, entityType);
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        commandSender.decrementStatistic(statistic, entityType);
    }

    @Override
    public int getStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
        return commandSender.getStatistic(statistic, entityType);
    }

    @Override
    public void incrementStatistic(Statistic statistic, EntityType entityType, int i) throws IllegalArgumentException {
        commandSender.incrementStatistic(statistic, entityType, i);
    }

    @Override
    public void decrementStatistic(Statistic statistic, EntityType entityType, int i) {
        commandSender.decrementStatistic(statistic, entityType, i);
    }

    @Override
    public void setStatistic(Statistic statistic, EntityType entityType, int i) {
        commandSender.setStatistic(statistic, entityType, i);
    }

    @Override
    public void setPlayerTime(long l, boolean b) {
        commandSender.setPlayerTime(l, b);
    }

    @Override
    public long getPlayerTime() {
        return commandSender.getPlayerTime();
    }

    @Override
    public long getPlayerTimeOffset() {
        return commandSender.getPlayerTimeOffset();
    }

    @Override
    public boolean isPlayerTimeRelative() {
        return commandSender.isPlayerTimeRelative();
    }

    @Override
    public void resetPlayerTime() {
        commandSender.resetPlayerTime();
    }

    @Override
    public void setPlayerWeather(WeatherType weatherType) {
        commandSender.setPlayerWeather(weatherType);
    }

    @Override
    public WeatherType getPlayerWeather() {
        return commandSender.getPlayerWeather();
    }

    @Override
    public void resetPlayerWeather() {
        commandSender.resetPlayerWeather();
    }

    @Override
    public void giveExp(int i) {
        commandSender.giveExp(i);
    }

    @Override
    public void giveExpLevels(int i) {
        commandSender.giveExpLevels(i);
    }

    @Override
    public float getExp() {
        return commandSender.getExp();
    }

    @Override
    public void setExp(float v) {
        commandSender.setExp(v);
    }

    @Override
    public int getLevel() {
        return commandSender.getLevel();
    }

    @Override
    public void setLevel(int i) {
        commandSender.setLevel(i);
    }

    @Override
    public int getTotalExperience() {
        return commandSender.getTotalExperience();
    }

    @Override
    public void setTotalExperience(int i) {
        commandSender.setTotalExperience(i);
    }

    @Override
    public void sendExperienceChange(float v) {
        commandSender.sendExperienceChange(v);
    }

    @Override
    public void sendExperienceChange(float v, int i) {
        commandSender.sendExperienceChange(v, i);
    }

    @Override
    public float getExhaustion() {
        return commandSender.getExhaustion();
    }

    @Override
    public void setExhaustion(float v) {
        commandSender.setExhaustion(v);
    }

    @Override
    public float getSaturation() {
        return commandSender.getSaturation();
    }

    @Override
    public void setSaturation(float v) {
        commandSender.setSaturation(v);
    }

    @Override
    public int getFoodLevel() {
        return commandSender.getFoodLevel();
    }

    @Override
    public void setFoodLevel(int i) {
        commandSender.setFoodLevel(i);
    }

    @Override
    public boolean getAllowFlight() {
        return commandSender.getAllowFlight();
    }

    @Override
    public void setAllowFlight(boolean b) {
        commandSender.setAllowFlight(b);
    }

    @Override
    public void hidePlayer(Player player) {
        commandSender.hidePlayer(player);
    }

    @Override
    public void hidePlayer(Plugin plugin, Player player) {
        commandSender.hidePlayer(plugin, player);
    }

    @Override
    public void showPlayer(Player player) {
        commandSender.showPlayer(player);
    }

    @Override
    public void showPlayer(Plugin plugin, Player player) {
        commandSender.showPlayer(plugin, player);
    }

    @Override
    public boolean canSee(Player player) {
        return commandSender.canSee(player);
    }

    @Override
    public boolean isFlying() {
        return commandSender.isFlying();
    }

    @Override
    public void setFlying(boolean b) {
        commandSender.setFlying(b);
    }

    @Override
    public void setFlySpeed(float v) throws IllegalArgumentException {
        commandSender.setFlySpeed(v);
    }

    @Override
    public void setWalkSpeed(float v) throws IllegalArgumentException {
        commandSender.setWalkSpeed(v);
    }

    @Override
    public float getFlySpeed() {
        return commandSender.getFlySpeed();
    }

    @Override
    public float getWalkSpeed() {
        return commandSender.getWalkSpeed();
    }

    @Override
    public void setTexturePack(String s) {
        commandSender.setTexturePack(s);
    }

    @Override
    public void setResourcePack(String s) {
        commandSender.setResourcePack(s);
    }

    @Override
    public void setResourcePack(String s, byte[] bytes) {
        commandSender.setResourcePack(s, bytes);
    }

    @Override
    public Scoreboard getScoreboard() {
        return commandSender.getScoreboard();
    }

    @Override
    public void setScoreboard(Scoreboard scoreboard) throws IllegalArgumentException, IllegalStateException {
        commandSender.setScoreboard(scoreboard);
    }

    @Override
    public boolean isHealthScaled() {
        return commandSender.isHealthScaled();
    }

    @Override
    public void setHealthScaled(boolean b) {
        commandSender.setHealthScaled(b);
    }

    @Override
    public void setHealthScale(double v) throws IllegalArgumentException {
        commandSender.setHealthScale(v);
    }

    @Override
    public double getHealthScale() {
        return commandSender.getHealthScale();
    }

    @Override
    public Entity getSpectatorTarget() {
        return commandSender.getSpectatorTarget();
    }

    @Override
    public void setSpectatorTarget(Entity entity) {
        commandSender.setSpectatorTarget(entity);
    }

    @Override
    public void sendTitle(String s, String s1) {
        commandSender.sendTitle(s, s1);
    }

    @Override
    public void sendTitle(String s, String s1, int i, int i1, int i2) {
        commandSender.sendTitle(s, s1, i, i1, i2);
    }

    @Override
    public void resetTitle() {
        commandSender.resetTitle();
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int i) {
        commandSender.spawnParticle(particle, location, i);
    }

    @Override
    public void spawnParticle(Particle particle, double v, double v1, double v2, int i) {
        commandSender.spawnParticle(particle, v, v1, v2, i);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int i, T t) {
        commandSender.spawnParticle(particle, location, i, t);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double v, double v1, double v2, int i, T t) {
        commandSender.spawnParticle(particle, v, v1, v2, i, t);
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int i, double v, double v1, double v2) {
        commandSender.spawnParticle(particle, location, i, v, v1, v2);
    }

    @Override
    public void spawnParticle(Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5) {
        commandSender.spawnParticle(particle, v, v1, v2, i, v3, v4, v5);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int i, double v, double v1, double v2, T t) {
        commandSender.spawnParticle(particle, location, i, v, v1, v2, t);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5, T t) {
        commandSender.spawnParticle(particle, v, v1, v2, i, v3, v4, v5, t);
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int i, double v, double v1, double v2, double v3) {
        commandSender.spawnParticle(particle, location, i, v, v1, v2, v3);
    }

    @Override
    public void spawnParticle(Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5, double v6) {
        commandSender.spawnParticle(particle, v, v1, v2, i, v3, v4, v5, v6);
    }

    @Override
    public <T> void spawnParticle(Particle particle, Location location, int i, double v, double v1, double v2, double v3, T t) {
        commandSender.spawnParticle(particle, location, i, v, v1, v2, v3, t);
    }

    @Override
    public <T> void spawnParticle(Particle particle, double v, double v1, double v2, int i, double v3, double v4, double v5, double v6, T t) {
        commandSender.spawnParticle(particle, v, v1, v2, i, v3, v4, v5, v6, t);
    }

    @Override
    public AdvancementProgress getAdvancementProgress(Advancement advancement) {
        return commandSender.getAdvancementProgress(advancement);
    }

    @Override
    public int getClientViewDistance() {
        return commandSender.getClientViewDistance();
    }

    @Override
    public String getLocale() {
        return commandSender.getLocale();
    }

    @Override
    public void updateCommands() {
        commandSender.updateCommands();
    }

    @Override
    public void openBook(ItemStack itemStack) {
        commandSender.openBook(itemStack);
    }

    @Override
    public boolean isOnline() {
        return commandSender.isOnline();
    }

    @Override
    public boolean isBanned() {
        return commandSender.isBanned();
    }

    @Override
    public boolean isWhitelisted() {
        return commandSender.isWhitelisted();
    }

    @Override
    public void setWhitelisted(boolean b) {
        commandSender.setWhitelisted(b);
    }

    @Override
    public Player getPlayer() {
        return commandSender.getPlayer();
    }

    @Override
    public long getFirstPlayed() {
        return commandSender.getFirstPlayed();
    }

    @Override
    public long getLastPlayed() {
        return commandSender.getLastPlayed();
    }

    @Override
    public boolean hasPlayedBefore() {
        return commandSender.hasPlayedBefore();
    }

    @Override
    public Map<String, Object> serialize() {
        return commandSender.serialize();
    }

    @Override
    public boolean isConversing() {
        return commandSender.isConversing();
    }

    @Override
    public void acceptConversationInput(String s) {
        commandSender.acceptConversationInput(s);
    }

    @Override
    public boolean beginConversation(Conversation conversation) {
        return commandSender.beginConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation) {
        commandSender.abandonConversation(conversation);
    }

    @Override
    public void abandonConversation(Conversation conversation, ConversationAbandonedEvent conversationAbandonedEvent) {
        commandSender.abandonConversation(conversation, conversationAbandonedEvent);
    }

    @Override
    public PlayerInventory getInventory() {
        return commandSender.getInventory();
    }

    @Override
    public Inventory getEnderChest() {
        return commandSender.getEnderChest();
    }

    @Override
    public MainHand getMainHand() {
        return commandSender.getMainHand();
    }

    @Override
    public boolean setWindowProperty(InventoryView.Property property, int i) {
        return commandSender.setWindowProperty(property, i);
    }

    @Override
    public InventoryView getOpenInventory() {
        return commandSender.getOpenInventory();
    }

    @Override
    public InventoryView openInventory(Inventory inventory) {
        return commandSender.openInventory(inventory);
    }

    @Override
    public InventoryView openWorkbench(Location location, boolean b) {
        return commandSender.openWorkbench(location, b);
    }

    @Override
    public InventoryView openEnchanting(Location location, boolean b) {
        return commandSender.openEnchanting(location, b);
    }

    @Override
    public void openInventory(InventoryView inventoryView) {
        commandSender.openInventory(inventoryView);
    }

    @Override
    public InventoryView openMerchant(Villager villager, boolean b) {
        return commandSender.openMerchant(villager, b);
    }

    @Override
    public InventoryView openMerchant(Merchant merchant, boolean b) {
        return commandSender.openMerchant(merchant, b);
    }

    @Override
    public void closeInventory() {
        commandSender.closeInventory();
    }

    @Override
    public ItemStack getItemInHand() {
        return commandSender.getItemInHand();
    }

    @Override
    public void setItemInHand(ItemStack itemStack) {
        commandSender.setItemInHand(itemStack);
    }

    @Override
    public ItemStack getItemOnCursor() {
        return commandSender.getItemOnCursor();
    }

    @Override
    public void setItemOnCursor(ItemStack itemStack) {
        commandSender.setItemOnCursor(itemStack);
    }

    @Override
    public boolean hasCooldown(Material material) {
        return commandSender.hasCooldown(material);
    }

    @Override
    public int getCooldown(Material material) {
        return commandSender.getCooldown(material);
    }

    @Override
    public void setCooldown(Material material, int i) {
        commandSender.setCooldown(material, i);
    }

    @Override
    public int getSleepTicks() {
        return commandSender.getSleepTicks();
    }

    @Override
    public Location getBedSpawnLocation() {
        return commandSender.getBedSpawnLocation();
    }

    @Override
    public void setBedSpawnLocation(Location location) {
        commandSender.setBedSpawnLocation(location);
    }

    @Override
    public void setBedSpawnLocation(Location location, boolean b) {
        commandSender.setBedSpawnLocation(location, b);
    }

    @Override
    public boolean sleep(Location location, boolean b) {
        return commandSender.sleep(location, b);
    }

    @Override
    public void wakeup(boolean b) {
        commandSender.wakeup(b);
    }

    @Override
    public Location getBedLocation() {
        return commandSender.getBedLocation();
    }

    @Override
    public GameMode getGameMode() {
        return commandSender.getGameMode();
    }

    @Override
    public void setGameMode(GameMode gameMode) {
        commandSender.setGameMode(gameMode);
    }

    @Override
    public boolean isBlocking() {
        return commandSender.isBlocking();
    }

    @Override
    public boolean isHandRaised() {
        return commandSender.isHandRaised();
    }

    @Override
    public int getExpToLevel() {
        return commandSender.getExpToLevel();
    }

    @Override
    public boolean discoverRecipe(NamespacedKey namespacedKey) {
        return commandSender.discoverRecipe(namespacedKey);
    }

    @Override
    public int discoverRecipes(Collection<NamespacedKey> collection) {
        return commandSender.discoverRecipes(collection);
    }

    @Override
    public boolean undiscoverRecipe(NamespacedKey namespacedKey) {
        return commandSender.undiscoverRecipe(namespacedKey);
    }

    @Override
    public int undiscoverRecipes(Collection<NamespacedKey> collection) {
        return commandSender.undiscoverRecipes(collection);
    }

    @Override
    public Entity getShoulderEntityLeft() {
        return commandSender.getShoulderEntityLeft();
    }

    @Override
    public void setShoulderEntityLeft(Entity entity) {
        commandSender.setShoulderEntityLeft(entity);
    }

    @Override
    public Entity getShoulderEntityRight() {
        return commandSender.getShoulderEntityRight();
    }

    @Override
    public void setShoulderEntityRight(Entity entity) {
        commandSender.setShoulderEntityRight(entity);
    }

    @Override
    public double getEyeHeight() {
        return commandSender.getEyeHeight();
    }

    @Override
    public double getEyeHeight(boolean b) {
        return commandSender.getEyeHeight(b);
    }

    @Override
    public Location getEyeLocation() {
        return commandSender.getEyeLocation();
    }

    @Override
    public List<Block> getLineOfSight(Set<Material> set, int i) {
        return commandSender.getLineOfSight(set, i);
    }

    @Override
    public Block getTargetBlock(Set<Material> set, int i) {
        return commandSender.getTargetBlock(set, i);
    }

    @Override
    public List<Block> getLastTwoTargetBlocks(Set<Material> set, int i) {
        return commandSender.getLastTwoTargetBlocks(set, i);
    }

    @Override
    public Block getTargetBlockExact(int i) {
        return commandSender.getTargetBlockExact(i);
    }

    @Override
    public Block getTargetBlockExact(int i, FluidCollisionMode fluidCollisionMode) {
        return commandSender.getTargetBlockExact(i, fluidCollisionMode);
    }

    @Override
    public RayTraceResult rayTraceBlocks(double v) {
        return commandSender.rayTraceBlocks(v);
    }

    @Override
    public RayTraceResult rayTraceBlocks(double v, FluidCollisionMode fluidCollisionMode) {
        return commandSender.rayTraceBlocks(v, fluidCollisionMode);
    }

    @Override
    public int getRemainingAir() {
        return commandSender.getRemainingAir();
    }

    @Override
    public void setRemainingAir(int i) {
        commandSender.setRemainingAir(i);
    }

    @Override
    public int getMaximumAir() {
        return commandSender.getMaximumAir();
    }

    @Override
    public void setMaximumAir(int i) {
        commandSender.setMaximumAir(i);
    }

    @Override
    public int getMaximumNoDamageTicks() {
        return commandSender.getMaximumNoDamageTicks();
    }

    @Override
    public void setMaximumNoDamageTicks(int i) {
        commandSender.setMaximumNoDamageTicks(i);
    }

    @Override
    public double getLastDamage() {
        return commandSender.getLastDamage();
    }

    @Override
    public void setLastDamage(double v) {
        commandSender.setLastDamage(v);
    }

    @Override
    public int getNoDamageTicks() {
        return commandSender.getNoDamageTicks();
    }

    @Override
    public void setNoDamageTicks(int i) {
        commandSender.setNoDamageTicks(i);
    }

    @Override
    public Player getKiller() {
        return commandSender.getKiller();
    }

    @Override
    public boolean addPotionEffect(PotionEffect potionEffect) {
        return commandSender.addPotionEffect(potionEffect);
    }

    @Override
    public boolean addPotionEffect(PotionEffect potionEffect, boolean b) {
        return commandSender.addPotionEffect(potionEffect, b);
    }

    @Override
    public boolean addPotionEffects(Collection<PotionEffect> collection) {
        return commandSender.addPotionEffects(collection);
    }

    @Override
    public boolean hasPotionEffect(PotionEffectType potionEffectType) {
        return commandSender.hasPotionEffect(potionEffectType);
    }

    @Override
    public PotionEffect getPotionEffect(PotionEffectType potionEffectType) {
        return commandSender.getPotionEffect(potionEffectType);
    }

    @Override
    public void removePotionEffect(PotionEffectType potionEffectType) {
        commandSender.removePotionEffect(potionEffectType);
    }

    @Override
    public Collection<PotionEffect> getActivePotionEffects() {
        return commandSender.getActivePotionEffects();
    }

    @Override
    public boolean hasLineOfSight(Entity entity) {
        return commandSender.hasLineOfSight(entity);
    }

    @Override
    public boolean getRemoveWhenFarAway() {
        return commandSender.getRemoveWhenFarAway();
    }

    @Override
    public void setRemoveWhenFarAway(boolean b) {
        commandSender.setRemoveWhenFarAway(b);
    }

    @Override
    public EntityEquipment getEquipment() {
        return commandSender.getEquipment();
    }

    @Override
    public void setCanPickupItems(boolean b) {
        commandSender.setCanPickupItems(b);
    }

    @Override
    public boolean getCanPickupItems() {
        return commandSender.getCanPickupItems();
    }

    @Override
    public boolean isLeashed() {
        return commandSender.isLeashed();
    }

    @Override
    public Entity getLeashHolder() throws IllegalStateException {
        return commandSender.getLeashHolder();
    }

    @Override
    public boolean setLeashHolder(Entity entity) {
        return commandSender.setLeashHolder(entity);
    }

    @Override
    public boolean isGliding() {
        return commandSender.isGliding();
    }

    @Override
    public void setGliding(boolean b) {
        commandSender.setGliding(b);
    }

    @Override
    public boolean isSwimming() {
        return commandSender.isSwimming();
    }

    @Override
    public void setSwimming(boolean b) {
        commandSender.setSwimming(b);
    }

    @Override
    public boolean isRiptiding() {
        return commandSender.isRiptiding();
    }

    @Override
    public boolean isSleeping() {
        return commandSender.isSleeping();
    }

    @Override
    public void setAI(boolean b) {
        commandSender.setAI(b);
    }

    @Override
    public boolean hasAI() {
        return commandSender.hasAI();
    }

    @Override
    public void setCollidable(boolean b) {
        commandSender.setCollidable(b);
    }

    @Override
    public boolean isCollidable() {
        return commandSender.isCollidable();
    }

    @Override
    public <T> T getMemory(MemoryKey<T> memoryKey) {
        return commandSender.getMemory(memoryKey);
    }

    @Override
    public <T> void setMemory(MemoryKey<T> memoryKey, T t) {
        commandSender.setMemory(memoryKey, t);
    }

    @Override
    public AttributeInstance getAttribute(Attribute attribute) {
        return commandSender.getAttribute(attribute);
    }

    @Override
    public void damage(double v) {
        commandSender.damage(v);
    }

    @Override
    public void damage(double v, Entity entity) {
        commandSender.damage(v, entity);
    }

    @Override
    public double getHealth() {
        return commandSender.getHealth();
    }

    @Override
    public void setHealth(double v) {
        commandSender.setHealth(v);
    }

    @Override
    public double getAbsorptionAmount() {
        return commandSender.getAbsorptionAmount();
    }

    @Override
    public void setAbsorptionAmount(double v) {
        commandSender.setAbsorptionAmount(v);
    }

    @Override
    public double getMaxHealth() {
        return commandSender.getMaxHealth();
    }

    @Override
    public void setMaxHealth(double v) {
        commandSender.setMaxHealth(v);
    }

    @Override
    public void resetMaxHealth() {
        commandSender.resetMaxHealth();
    }

    @Override
    public Location getLocation() {
        return commandSender.getLocation();
    }

    @Override
    public Location getLocation(Location location) {
        return commandSender.getLocation(location);
    }

    @Override
    public void setVelocity(Vector vector) {
        commandSender.setVelocity(vector);
    }

    @Override
    public Vector getVelocity() {
        return commandSender.getVelocity();
    }

    @Override
    public double getHeight() {
        return commandSender.getHeight();
    }

    @Override
    public double getWidth() {
        return commandSender.getWidth();
    }

    @Override
    public BoundingBox getBoundingBox() {
        return commandSender.getBoundingBox();
    }

    @Override
    public boolean isOnGround() {
        return commandSender.isOnGround();
    }

    @Override
    public World getWorld() {
        return commandSender.getWorld();
    }

    @Override
    public void setRotation(float v, float v1) {
        commandSender.setRotation(v, v1);
    }

    @Override
    public boolean teleport(Location location) {
        return commandSender.teleport(location);
    }

    @Override
    public boolean teleport(Location location, PlayerTeleportEvent.TeleportCause teleportCause) {
        return commandSender.teleport(location, teleportCause);
    }

    @Override
    public boolean teleport(Entity entity) {
        return commandSender.teleport(entity);
    }

    @Override
    public boolean teleport(Entity entity, PlayerTeleportEvent.TeleportCause teleportCause) {
        return commandSender.teleport(entity, teleportCause);
    }

    @Override
    public List<Entity> getNearbyEntities(double v, double v1, double v2) {
        return commandSender.getNearbyEntities(v, v1, v2);
    }

    @Override
    public int getEntityId() {
        return commandSender.getEntityId();
    }

    @Override
    public int getFireTicks() {
        return commandSender.getFireTicks();
    }

    @Override
    public int getMaxFireTicks() {
        return commandSender.getMaxFireTicks();
    }

    @Override
    public void setFireTicks(int i) {
        commandSender.setFireTicks(i);
    }

    @Override
    public void remove() {
        commandSender.remove();
    }

    @Override
    public boolean isDead() {
        return commandSender.isDead();
    }

    @Override
    public boolean isValid() {
        return commandSender.isValid();
    }

    @Override
    public boolean isPersistent() {
        return commandSender.isPersistent();
    }

    @Override
    public void setPersistent(boolean b) {
        commandSender.setPersistent(b);
    }

    @Override
    public Entity getPassenger() {
        return commandSender.getPassenger();
    }

    @Override
    public boolean setPassenger(Entity entity) {
        return commandSender.setPassenger(entity);
    }

    @Override
    public List<Entity> getPassengers() {
        return commandSender.getPassengers();
    }

    @Override
    public boolean addPassenger(Entity entity) {
        return commandSender.addPassenger(entity);
    }

    @Override
    public boolean removePassenger(Entity entity) {
        return commandSender.removePassenger(entity);
    }

    @Override
    public boolean isEmpty() {
        return commandSender.isEmpty();
    }

    @Override
    public boolean eject() {
        return commandSender.eject();
    }

    @Override
    public float getFallDistance() {
        return commandSender.getFallDistance();
    }

    @Override
    public void setFallDistance(float v) {
        commandSender.setFallDistance(v);
    }

    @Override
    public void setLastDamageCause(EntityDamageEvent entityDamageEvent) {
        commandSender.setLastDamageCause(entityDamageEvent);
    }

    @Override
    public EntityDamageEvent getLastDamageCause() {
        return commandSender.getLastDamageCause();
    }

    @Override
    public UUID getUniqueId() {
        return commandSender.getUniqueId();
    }

    @Override
    public int getTicksLived() {
        return commandSender.getTicksLived();
    }

    @Override
    public void setTicksLived(int i) {
        commandSender.setTicksLived(i);
    }

    @Override
    public void playEffect(EntityEffect entityEffect) {
        commandSender.playEffect(entityEffect);
    }

    @Override
    public EntityType getType() {
        return commandSender.getType();
    }

    @Override
    public boolean isInsideVehicle() {
        return commandSender.isInsideVehicle();
    }

    @Override
    public boolean leaveVehicle() {
        return commandSender.leaveVehicle();
    }

    @Override
    public Entity getVehicle() {
        return commandSender.getVehicle();
    }

    @Override
    public void setCustomNameVisible(boolean b) {
        commandSender.setCustomNameVisible(b);
    }

    @Override
    public boolean isCustomNameVisible() {
        return commandSender.isCustomNameVisible();
    }

    @Override
    public void setGlowing(boolean b) {
        commandSender.setGlowing(b);
    }

    @Override
    public boolean isGlowing() {
        return commandSender.isGlowing();
    }

    @Override
    public void setInvulnerable(boolean b) {
        commandSender.setInvulnerable(b);
    }

    @Override
    public boolean isInvulnerable() {
        return commandSender.isInvulnerable();
    }

    @Override
    public boolean isSilent() {
        return commandSender.isSilent();
    }

    @Override
    public void setSilent(boolean b) {
        commandSender.setSilent(b);
    }

    @Override
    public boolean hasGravity() {
        return commandSender.hasGravity();
    }

    @Override
    public void setGravity(boolean b) {
        commandSender.setGravity(b);
    }

    @Override
    public int getPortalCooldown() {
        return commandSender.getPortalCooldown();
    }

    @Override
    public void setPortalCooldown(int i) {
        commandSender.setPortalCooldown(i);
    }

    @Override
    public Set<String> getScoreboardTags() {
        return commandSender.getScoreboardTags();
    }

    @Override
    public boolean addScoreboardTag(String s) {
        return commandSender.addScoreboardTag(s);
    }

    @Override
    public boolean removeScoreboardTag(String s) {
        return commandSender.removeScoreboardTag(s);
    }

    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        return commandSender.getPistonMoveReaction();
    }

    @Override
    public BlockFace getFacing() {
        return commandSender.getFacing();
    }

    @Override
    public Pose getPose() {
        return commandSender.getPose();
    }

    @Override
    public String getCustomName() {
        return commandSender.getCustomName();
    }

    @Override
    public void setCustomName(String s) {
        commandSender.setCustomName(s);
    }

    @Override
    public void setMetadata(String s, MetadataValue metadataValue) {
        commandSender.setMetadata(s, metadataValue);
    }

    @Override
    public List<MetadataValue> getMetadata(String s) {
        return commandSender.getMetadata(s);
    }

    @Override
    public boolean hasMetadata(String s) {
        return commandSender.hasMetadata(s);
    }

    @Override
    public void removeMetadata(String s, Plugin plugin) {
        commandSender.removeMetadata(s, plugin);
    }

    @Override
    public PersistentDataContainer getPersistentDataContainer() {
        return commandSender.getPersistentDataContainer();
    }

    @Override
    public void sendPluginMessage(Plugin plugin, String s, byte[] bytes) {
        commandSender.sendPluginMessage(plugin, s, bytes);
    }

    @Override
    public Set<String> getListeningPluginChannels() {
        return commandSender.getListeningPluginChannels();
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> aClass) {
        return commandSender.launchProjectile(aClass);
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> aClass, Vector vector) {
        return commandSender.launchProjectile(aClass, vector);
    }
}