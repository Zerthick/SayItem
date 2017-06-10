package io.github.zerthick.sayitem.util;

import com.google.common.reflect.TypeToken;
import com.typesafe.config.ConfigRenderOptions;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.io.*;

/**
 * Created by chase on 6/9/17.
 */
public class ItemStackHOCONSerializer {

    public static String serializeSnapShot(ItemStackSnapshot snapshot, boolean concise) throws ObjectMappingException, IOException {
        ConfigurationNode node = HoconConfigurationLoader.builder().build().createEmptyNode();
        StringWriter stringWriter = new StringWriter();

        node.setValue(TypeToken.of(ItemStackSnapshot.class), snapshot);
        HoconConfigurationLoader.Builder builder = HoconConfigurationLoader.builder().setSink(() -> new BufferedWriter(stringWriter));

        if (concise) {
            builder.setRenderOptions(ConfigRenderOptions.concise());
        }

        builder.build().save(node);
        return stringWriter.toString();
    }

    public static ItemStackSnapshot deserializeSnapShot(String serializedSnapshot) throws ObjectMappingException, IOException {
        ConfigurationNode node = HoconConfigurationLoader.builder().setSource(() -> new BufferedReader(new StringReader(serializedSnapshot))).build().load();
        return node.getValue(TypeToken.of(ItemStackSnapshot.class));
    }

}
