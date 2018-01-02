/*
 * Copyright (C) 2018  Zerthick
 *
 * This file is part of SayItem.
 *
 * SayItem is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * SayItem is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SayItem.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.zerthick.sayitem;

import com.google.inject.Inject;
import io.github.zerthick.sayitem.util.ItemStackHOCONSerializer;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.EnchantmentData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Plugin(
        id = "sayitem",
        name = "SayItem",
        description = "Display an Item in Chat!",
        authors = {
                "Zerthick"
        }
)
public class SayItem {

    @Inject
    private Logger logger;
    @Inject
    private PluginContainer instance;

    private Pattern jsonPlaceholderPattern = Pattern.compile("\\[(?<Json>\\{.*?})]");

    @Listener
    public void onServerStart(GameStartedServerEvent event) {

        Sponge.getCommandManager().register(instance, CommandSpec.builder()
                .permission("sayitem.command.say")
                .arguments(GenericArguments.flags().permissionFlag("sayitem.command.flag.say.j", "j")
                        .buildWith(GenericArguments.remainingJoinedStrings(Text.of("Message"))))
                .executor((src, args) -> {

                    args.getOne(Text.of("Message")).ifPresent(
                            msg -> {
                                Text finalMsg;

                                Map<String, Text> placeholders = new HashMap<>();

                                // If the json flag was set, let's look for json placeholders as well
                                if (args.hasAny("j")) {
                                    Matcher matcher = jsonPlaceholderPattern.matcher(msg.toString());
                                    while (matcher.find()) {
                                        String json = matcher.group("Json");
                                        try {
                                            placeholders.put("[" + json + "]",
                                                    buildItemName(ItemStackHOCONSerializer.deserializeSnapShot(json).createStack()));
                                        } catch (ObjectMappingException | IOException e) {
                                            src.sendMessage(Text.of(TextColors.RED, e.getMessage()));
                                            logger.error(e.getMessage());
                                        }
                                    }
                                }

                                if (src instanceof Player) {
                                    Player player = (Player) src;
                                    placeholders.putAll(buildPlaceholders(player));
                                    finalMsg = processPlaceholders(Text.of(msg), placeholders);
                                    Sponge.getCauseStackManager().pushCause(instance);
                                    player.simulateChat(finalMsg, Sponge.getCauseStackManager().getCurrentCause());
                                    Sponge.getCauseStackManager().popCause();
                                } else {
                                    finalMsg = processPlaceholders(Text.of(msg), placeholders);
                                    Sponge.getServer().getBroadcastChannel().send(finalMsg);
                                }
                            });
                    return CommandResult.success();
                }).build(), "sayItem", "si");

        // Log Start Up to Console
        logger.info(
                instance.getName() + " version " + instance.getVersion().orElse("")
                        + " enabled!");
    }

    @Listener
    public void onChatPlayer(MessageChannelEvent event, @Root Player player) {

        if (player.hasPermission("sayitem.chat")) {
            Text msgBody = event.getFormatter().getBody().format();
            event.getFormatter().setBody(processPlaceholders(msgBody, buildPlaceholders(player)));
        }

    }

    private Map<String, Text> buildPlaceholders(Player player) {

        Map<String, Text> placeholders = new HashMap<>();

        player.getItemInHand(HandTypes.MAIN_HAND)
                .ifPresent(itemStack -> placeholders.put("[item]", buildItemName(itemStack)));

        Hotbar playerHotbar = player.getInventory().query(Hotbar.class);

        int i = 1;
        Iterator<Inventory> hotBarIterator = playerHotbar.slots().iterator();

        while (hotBarIterator.hasNext()) {
            Slot slot = (Slot) hotBarIterator.next();

            Optional<ItemStack> itemStackOptional = slot.peek();
            if (itemStackOptional.isPresent()) {
                String key = "[item" + i + "]";
                placeholders.put(key, buildItemName(itemStackOptional.get()));
            }
            i++;
        }

        return placeholders;
    }

    private Text processPlaceholders(Text msg, Map<String, Text> placeholders) {

        if (!msg.getChildren().isEmpty()) {
            msg = msg.toBuilder().removeAll().append(msg.getChildren().stream()
                    .map(child -> processPlaceholders(child, placeholders)).collect(Collectors.toList())).build();
        }

        String plainMsg = msg.toPlain();
        for (String placeholder : placeholders.keySet()) {

            // Count the number of item placeholders present
            int matches = StringUtils.countMatches(plainMsg, placeholder);

            // If placeholders a present
            if (matches != 0) {

                // Split the message around the placeholders insert the appropriate number of item reps
                String[] splitMessage = plainMsg.split(Pattern.quote(placeholder));
                Text.Builder finalMsgBuilder = Text.builder();
                for (int i = 0; i < splitMessage.length; i++) {
                    finalMsgBuilder.append(Text.of(splitMessage[i]));
                    if (matches > 0) {
                        finalMsgBuilder.append(placeholders.get(placeholder));
                        matches--;
                    }
                }

                // Insert any left over placeholders
                while (matches > 0) {
                    finalMsgBuilder.append(placeholders.get(placeholder));
                    matches--;
                }

                msg = finalMsgBuilder
                        .style(msg.getStyle())
                        .color(msg.getColor())
                        .build();
                return processPlaceholders(msg, placeholders);
            }
        }

        return msg;
    }

    private Text buildItemName(ItemStack itemStack) {

        Text displayName;
        TextColor itemColor;

        Optional<Text> displayNameOptional = itemStack.get(Keys.DISPLAY_NAME);

        // If the item has a display name, we'll use that
        if (displayNameOptional.isPresent()) {

            displayName = displayNameOptional.get();

            itemColor = displayName.getColor();

            if (!displayName.getChildren().isEmpty()) {
                itemColor = displayName.getChildren().get(0).getColor();
            }
        } else { // Just grab the item name
            displayName = Text.of(itemStack.getTranslation());

            itemColor = displayName.getColor();

            // Color the item aqua if it has an enchantment
            if (itemStack.get(EnchantmentData.class).isPresent()) {
                itemColor = TextColors.AQUA;
            }
        }

        // Build the item text with the color
        return Text.builder().color(itemColor)
                .append(Text.of("["), displayName, Text.of("]"))
                .onHover(TextActions.showItem(itemStack.createSnapshot())).build();
    }

}
