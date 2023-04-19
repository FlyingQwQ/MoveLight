package top.kuoer;

import org.bukkit.inventory.ItemStack;

public class CheckItemStack {

    private ItemStack itemStack;
    private String equipmentSlot = "EQUIP";

    public CheckItemStack(ItemStack itemStack, String equipmentSlot) {
        this.itemStack = itemStack;
        this.equipmentSlot = equipmentSlot;
    }

    public String getEquipmentSlot() {
        return equipmentSlot;
    }

    public void setEquipmentSlot(String equipmentSlot) {
        this.equipmentSlot = equipmentSlot;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

}
