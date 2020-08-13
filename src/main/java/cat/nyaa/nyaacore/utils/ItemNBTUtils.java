package cat.nyaa.nyaacore.utils;

import net.minecraft.server.v1_16_R2.NBTTagCompound;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class ItemNBTUtils {
    private final NBTTagCompound NBT;
    private final ItemStack item;

    public ItemNBTUtils(ItemStack item) {
        this.item = item;
        net.minecraft.server.v1_16_R2.ItemStack NMSItem = CraftItemStack.asNMSCopy(item);
        NBT = NMSItem.getOrCreateTag();
    }

    public ItemStack saveNBT() {
        net.minecraft.server.v1_16_R2.ItemStack NMSItem = CraftItemStack.asNMSCopy(item);
        NMSItem.setTag(NBT);
        return CraftItemStack.asBukkitCopy(NMSItem.cloneItemStack());
    }

    public void setNbt(String key, String value) {
        NBT.setString(key, value);
    }

    public boolean hasKey(String key) {
        return NBT.hasKey(key);
    }

    public String getString(String key) {
        return NBT.getString(key);
    }

    public void setBoolean(String key, boolean b) {
        NBT.setBoolean(key, b);
    }

    public void setInt(String key, int x1) {
        NBT.setInt(key, x1);
    }

    public boolean getBoolean(String key) {
        return NBT.getBoolean(key);
    }

    public int getInt(String key) {
        return NBT.getInt(key);
    }
}
