package top.kuoer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TaskTimer extends BukkitRunnable {

    private boolean suspend = false;
    private final Hashtable<Player, Block> playerLightBlock = new Hashtable<>();
    private final MoveLight moveLight;
    private Hashtable<String, UsableInfo> usableItemsInfo;
    private List<Material> replaceableBlockList;
    private final List<CheckAroundSeekAir> checkAroundSeekAirList;

    private Hashtable<Player, Long> suspendMoveLightPlayerList = new Hashtable<>();

    public TaskTimer(MoveLight moveLight) {
        this.moveLight = moveLight;
        this.configToUsableItems();

        // 光源方块可替换的方块
        this.replaceableBlockList = new ArrayList<>();
        this.replaceableBlockList.add(Material.AIR);
        this.replaceableBlockList.add(Material.CAVE_AIR);
        this.replaceableBlockList.add(Material.VOID_AIR);
        this.replaceableBlockList.add(Material.LIGHT);

        // 寻找玩家周围有没有方块可以替换
        this.checkAroundSeekAirList = new ArrayList<>();
        this.checkAroundSeekAirList.add(new CheckAroundSeekAir(0, 0, 0));
        this.checkAroundSeekAirList.add(new CheckAroundSeekAir(0, 1, 0));
        this.checkAroundSeekAirList.add(new CheckAroundSeekAir(0, -1, 0));
        this.checkAroundSeekAirList.add(new CheckAroundSeekAir(1, 0, 0));
        this.checkAroundSeekAirList.add(new CheckAroundSeekAir(-1, 0, 0));
        this.checkAroundSeekAirList.add(new CheckAroundSeekAir(0, 0, 1));
        this.checkAroundSeekAirList.add(new CheckAroundSeekAir(0, 0, -1));
        this.checkAroundSeekAirList.add(new CheckAroundSeekAir(1, 1, 0));
        this.checkAroundSeekAirList.add(new CheckAroundSeekAir(-1, 1, 0));
        this.checkAroundSeekAirList.add(new CheckAroundSeekAir(0, 1, 1));
        this.checkAroundSeekAirList.add(new CheckAroundSeekAir(0, 1, -1));
    }

    @Override
    public void run() {
        if(!this.moveLight.getConfig().getBoolean("enable") || suspend) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if(!player.hasPermission("movelight.player.use")) {
                this.removePlayerLight(player);
                continue;
            }



            if(this.suspendMoveLightPlayerList.containsKey(player)) {
                long currTime = System.currentTimeMillis();
                long beforeTime = this.suspendMoveLightPlayerList.get(player);
                if((currTime - beforeTime) > 1000) {
                    this.suspendMoveLightPlayerList.remove(player);
                }
                continue;
            }




            PlayerInventory playerInventory = player.getInventory();

            // 优先级：主手 > 副手 > 头盔 > 胸甲 > 护腿 > 靴子
            List<CheckItemStack> checkItemList = new ArrayList<>();
            checkItemList.add(new CheckItemStack(playerInventory.getItemInMainHand(), "HAND"));
            checkItemList.add(new CheckItemStack(playerInventory.getItemInOffHand(), "OFF_HAND"));
            checkItemList.add(new CheckItemStack(playerInventory.getHelmet(), "EQUIP"));
            checkItemList.add(new CheckItemStack(playerInventory.getChestplate(), "EQUIP"));
            checkItemList.add(new CheckItemStack(playerInventory.getLeggings(), "EQUIP"));
            checkItemList.add(new CheckItemStack(playerInventory.getBoots(), "EQUIP"));
            ItemStack handItem = checkItemList.get(0).getItemStack();
            for(CheckItemStack checkItemStack : checkItemList) {
                ItemStack checkItem = checkItemStack.getItemStack();
                if(null != checkItem && this.usableItemsInfo.containsKey(checkItem.getType().toString())) {
                    if("EQUIP".equals(checkItemStack.getEquipmentSlot())) {
                        UsableInfo usableInfo = this.usableItemsInfo.get(checkItem.getType().toString());
                        if(usableInfo.isApparel()) {
                            handItem = checkItem;
                            break;
                        }
                    } else {
                        handItem = checkItem;
                        break;
                    }
                }
            }

            // 判断物品是不是在可以用移动光源的列表里，有的话就使用
            if (this.usableItemsInfo.containsKey(handItem.getType().toString())) {
                if (null == playerLightBlock.get(player)) {
                    Location lightLocation = this.aroundSeekAir(player.getLocation());
                    if (null == lightLocation) {
                        continue;
                    }
                    Block lightBlock = player.getWorld().getBlockAt(lightLocation);
                    lightBlock.setType(Material.LIGHT);

                    // 设置移动光源亮度
                    Light blockData = (Light) lightBlock.getBlockData();
                    blockData.setLevel(this.usableItemsInfo.get(handItem.getType().toString()).getLightLevel());
                    lightBlock.setBlockData(blockData);

                    this.playerLightBlock.put(player, lightBlock);
                } else {
                    Location lightLocation = this.aroundSeekAir(player.getLocation());
                    if (null == lightLocation) {
                        clearDistanceLightBlock(player, this.playerLightBlock.get(player));
                        continue;
                    }
                    Block newLightBlock = player.getWorld().getBlockAt(this.aroundSeekAir(lightLocation));
                    Block oldLightBlock = this.playerLightBlock.get(player);
                    oldLightBlock.setType(Material.AIR);
                    newLightBlock.setType(Material.LIGHT);

                    // 设置移动光源亮度
                    Light blockData = (Light) newLightBlock.getBlockData();
                    blockData.setLevel(this.usableItemsInfo.get(handItem.getType().toString()).getLightLevel());
                    newLightBlock.setBlockData(blockData);

                    this.playerLightBlock.put(player, newLightBlock);
                }
            } else {
                Block lightBlock = this.playerLightBlock.get(player);
                if (null != lightBlock) {
                    lightBlock.setType(Material.AIR);
                    this.playerLightBlock.remove(player);
                }
            }

        }
    }

    // 获取玩家位置周围有没有可替换光源的方块
    public Location aroundSeekAir(Location playerLocation) {
        World playerWorld = playerLocation.getWorld();
        if(null == playerWorld) {
            return null;
        }

        Location targetLocation;
        Material targetMaterial;
        for(CheckAroundSeekAir checkAroundSeekAir : this.checkAroundSeekAirList) {
            targetLocation = playerLocation.clone().add(checkAroundSeekAir.getX(), checkAroundSeekAir.getY(), checkAroundSeekAir.getZ());
            targetMaterial = playerWorld.getBlockAt(targetLocation).getType();
            if(this.replaceableBlockList.contains(targetMaterial)) {
                return targetLocation;
            }
        }

        return null;
    }

    // 清除距离光源离玩家范围大于10的移动光源
    public void clearDistanceLightBlock(Player player, Block lightBlock) {
        Location playerLocation = player.getLocation();
        Location lightBlockLocation = lightBlock.getLocation();

        if(playerLocation.getWorld() != lightBlockLocation.getWorld() || lightBlockLocation.distanceSquared(playerLocation) > 10) {
            this.removePlayerLight(player);
        }
    }

    // 删除指定玩家的移动光源
    public void removePlayerLight(Player player) {
        Block lightBlock = this.playerLightBlock.get(player);
        if(null != lightBlock) {
            lightBlock.setType(Material.AIR);
            this.playerLightBlock.remove(player);
        }
    }

    // 删除所有玩家的移动光源
    public void removeAllPlayerLight() {
        this.suspend = true;
        Hashtable<Player, Block> playerLightBlockClone = (Hashtable<Player, Block>) this.playerLightBlock.clone();
        Iterator<Player> player = playerLightBlockClone.keySet().iterator();
        while(player.hasNext()) {
            Block lightBlock = this.playerLightBlock.get(player.next());
            lightBlock.setType(Material.AIR);
            this.playerLightBlock.remove(player);
        }
        this.suspend = false;
    }

    // 从配置文件中获取物品配置
    public void configToUsableItems() {
        this.usableItemsInfo = new Hashtable<>();
        List<String> usableItems = this.moveLight.getConfig().getConfigurationSection("usable").getKeys(false).stream().toList();
        for(String itemMaterialName : usableItems) {
            int lightLevel = this.moveLight.getConfig().getInt("usable." + itemMaterialName + ".lightLevel");
            boolean apparel = this.moveLight.getConfig().getBoolean("usable." + itemMaterialName + ".apparel");
            UsableInfo usableInfo = new UsableInfo(itemMaterialName, lightLevel, apparel);
            this.usableItemsInfo.put(itemMaterialName, usableInfo);
        }
        Bukkit.getConsoleSender().sendMessage("[MoveLight] §a从配置文件中加载" + usableItems.size() + "件物品.");
    }

    public Map<Player, Block> getPlayerLightBlock() {
        return this.playerLightBlock;
    }

    public Hashtable<Player, Long> getSuspendMoveLightPlayerList() {
        return this.suspendMoveLightPlayerList;
    }

    public List<CheckAroundSeekAir> getCheckAroundSeekAirList() {
        return this.checkAroundSeekAirList;
    }
}
