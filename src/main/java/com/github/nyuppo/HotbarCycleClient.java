package com.github.nyuppo;

import com.github.nyuppo.compat.Clicker;
import com.github.nyuppo.compat.IPNClicker;
import com.github.nyuppo.compat.VanillaClicker;
import com.github.nyuppo.config.ClothConfigHotbarCycleConfig;
import com.github.nyuppo.config.DefaultHotbarCycleConfig;
import com.github.nyuppo.config.HotbarCycleConfig;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.function.Consumer;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HotbarCycleClient implements ClientModInitializer {
    private static final KeyMapping.Category category = new KeyMapping.Category(Identifier.fromNamespaceAndPath("hotbarcycle", "keybinds"));
    private static KeyMapping cycleKeyBinding;
    private static KeyMapping singleCycleKeyBinding;

    private static final HotbarCycleConfig CONFIG;

    private static Clicker clicker;

    public static final Logger LOGGER = LoggerFactory.getLogger("hotbarcycle");

    public static HotbarCycleConfig getConfig() {
        return CONFIG;
    }

    public static KeyMapping getCycleKeyBinding() {
        return cycleKeyBinding;
    }

    public static KeyMapping getSingleCycleKeyBinding() {
        return singleCycleKeyBinding;
    }

    @Override
    public void onInitializeClient() {
        clicker = getClicker();

        cycleKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.hotbarcycle.cycle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                category
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (cycleKeyBinding.consumeClick()) {
                if (client.player != null && !CONFIG.getHoldAndScroll()) {
                    shiftRows(client, CONFIG.getCycleDirection());
                }
            }
        });

        singleCycleKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.hotbarcycle.single_cycle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                category
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (singleCycleKeyBinding.consumeClick()) {
                if (client.player != null && client.player.getInventory() != null && !CONFIG.getHoldAndScroll()) {
                    shiftSingle(client, client.player.getInventory().getSelectedSlot(), CONFIG.getCycleDirection());
                }
            }
        });
    }

    public enum Direction {
        UP,
        DOWN;

        public Direction reverse(final boolean reversed) {
            return switch (this) {
                case UP -> !reversed ? UP : DOWN;
                case DOWN -> !reversed ? DOWN : UP;
            };
        }
    }

    public static void shiftRows(Minecraft client, int amount) {
        shift(client, amount, (dstY) -> {
            for (int x=0; x<9; ++x){
                if (isColumnEnabled(x))
                    clicker.swap(client, (dstY * 9) + x, x);
            }
        });
    
    }

    public static void shiftSingle(Minecraft client, int x, int amount) {
        shift(client, amount, (dstY) -> {
            clicker.swap(client, (dstY * 9) + x, x);
        });
    }

    public static void shift(Minecraft client, int amount, Consumer<Integer> swapper) {
        if (client.gameMode == null || client.player == null) {
            return;
        }

        int[] swapMap = SwapMap.CycleAllRows(amount);

        ProcessBuffer(client, swapMap, swapper);
        // Checks that  each slot  contain  the intended stack. If not, send the
        // stack to the buffer and start processing it again.
        for (int y=1; y<4; ++y)
        if  (swapMap[y] != y){
            swapper.accept(y);
            swapMap[0] = swapMap[y];
            swapMap[y] = 0;
            ProcessBuffer(client, swapMap, swapper);
        }

        if (CONFIG.getPlaySound()) {
            client.player.level().playPlayerSound(SoundEvents.BOOK_PAGE_TURN, SoundSource.MASTER, 0.5f, 1.5f);
        }
    }

    /**
     * Sends  the item stack  in the buffer slot  to its  destination, until the
     * buffer itself contains its intended item stack.
     */
    private static void ProcessBuffer(Minecraft client, int[] swapMap, Consumer<Integer> swapper){
        while (swapMap[0] != 0){
            int dstY = swapMap[0];

            if (swapMap[dstY] == dstY)
                throw new RuntimeException("Bad swap-map, multiple stacks go to the same slot.");

            swapper.accept(dstY);
            swapMap[0] = swapMap[dstY];
            swapMap[dstY] = dstY;
        }
    }

    private static Clicker getClicker() {
        if (FabricLoader.getInstance().isModLoaded("inventoryprofilesnext")) {
            LOGGER.info("Inventory Profiles Next was found, switching to compatible clicker!");
            return new IPNClicker();
        }

        return new VanillaClicker();
    }

    public static boolean isColumnEnabled(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> CONFIG.getEnableColumn0();
            case 1 -> CONFIG.getEnableColumn1();
            case 2 -> CONFIG.getEnableColumn2();
            case 3 -> CONFIG.getEnableColumn3();
            case 4 -> CONFIG.getEnableColumn4();
            case 5 -> CONFIG.getEnableColumn5();
            case 6 -> CONFIG.getEnableColumn6();
            case 7 -> CONFIG.getEnableColumn7();
            case 8 -> CONFIG.getEnableColumn8();
            default -> false;
        };
    }

    public static boolean isRowEnabled(int y) {
        return switch (y){
            // The mix-up is intentional; Row 1 (bottom) in the config is the 
            // last row (y=3) in the slot array.
            case 1 -> CONFIG.getEnableRow3();
            case 2 -> CONFIG.getEnableRow2();
            case 3 -> CONFIG.getEnableRow1();
            case 0 -> true;
            default -> false;
        };
    }

    static {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            CONFIG = AutoConfig.register(ClothConfigHotbarCycleConfig.class, GsonConfigSerializer::new).getConfig();
        } else {
            CONFIG = new DefaultHotbarCycleConfig();
        }

    }
}
