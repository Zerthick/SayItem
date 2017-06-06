/*
 * Copyright (C) 2017  Zerthick
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;

@Plugin(
        id = "sayitem",
        name = "SayItem",
        authors = {
                "Zerthick"
        }
)
public class SayItem {

    @Inject
    private Logger logger;
    @Inject
    private PluginContainer instance;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {

        Sponge.getCommandManager().register(instance, CommandSpec.builder()
                .permission("sayitem.command.say")
                .arguments(GenericArguments.remainingJoinedStrings(Text.of("Message")))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Player player = (Player) src;

                        Optional<ItemStack> itemStackOptional = player.getItemInHand(HandTypes.MAIN_HAND);

                        if (itemStackOptional.isPresent()) {
                            args.getOne(Text.of("Message")).ifPresent(message -> {
                                String msgString = message.toString();
                                int matches = StringUtils.countMatches(msgString, "[item]");
                                if (matches != 0) {
                                    String[] splitMessage = msgString.split("\\[item]");
                                    Text.Builder finalMsgBuilder = Text.builder();
                                    for (int i = 0; i < splitMessage.length; i++) {
                                        finalMsgBuilder.append(Text.of(splitMessage[i]));
                                        if (matches > 0) {
                                            finalMsgBuilder.append(buildItemName(itemStackOptional.get()));
                                            matches--;
                                        }
                                    }
                                    player.simulateChat(finalMsgBuilder.build(), Cause.of(NamedCause.source(instance)));
                                } else {
                                    src.sendMessage(Text.of(TextColors.RED, "You must include the [item] placeholder in your message!"));
                                }
                            });
                        } else {
                            src.sendMessage(Text.of(TextColors.RED, "You must hold an item in your hand!"));
                        }
                    } else {
                        src.sendMessage(Text.of("You must be a player to use the sayItem command!"));
                    }
                    return CommandResult.success();
                }).build(), "sayItem", "si");

        // Log Start Up to Console
        logger.info(
                instance.getName() + " version " + instance.getVersion().orElse("")
                        + " enabled!");
    }

    private Text buildItemName(ItemStack itemStack) {
        Text displayName = itemStack.get(Keys.DISPLAY_NAME).orElse(Text.of(itemStack.getTranslation()));
        TextColor itemColor = displayName.getColor();

        if (!displayName.getChildren().isEmpty()) {
            itemColor = displayName.getChildren().get(0).getColor();
        }

        return Text.builder().color(itemColor)
                .append(Text.of("["), displayName, Text.of("]"))
                .onHover(TextActions.showItem(itemStack.createSnapshot())).build();
    }

}
