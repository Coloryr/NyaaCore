package cat.nyaa.nyaacore.utils;

import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.EnumHand;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerUtils {
    public static void openWrittenBook(Player player, ItemStack book) {
        if (book != null && book.getType() == Material.WRITTEN_BOOK) {
            sendFakeItemStack(player, player.getInventory().getHeldItemSlot(), book);
            ((CraftPlayer) player).getHandle().openBook(CraftItemStack.asNMSCopy(book), EnumHand.a);
            player.updateInventory();
        }
    }

    public static void sendFakeItemStack(Player p, int index, ItemStack itemStack) {
        EntityPlayer player = ((CraftPlayer) p).getHandle();
        if (player.b == null) {
            return;
        }
        if (index < net.minecraft.world.entity.player.PlayerInventory.getHotbarSize()) {
            index += 36;
        } else if (index > 39) {
            index += 5;
        } else if (index > 35) {
            index = 8 - (index - 36);
        }
        player.b.sendPacket(new PacketPlayOutSetSlot(player.defaultContainer.windowId, index, CraftItemStack.asNMSCopy(itemStack)));
    }

    public static int getPing(Player p) {
        EntityPlayer player = ((CraftPlayer) p).getHandle();
        return player.ping;
    }
}
