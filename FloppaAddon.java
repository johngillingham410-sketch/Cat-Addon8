package com.example.floppawalker;

import com.example.floppawalker.modules.FloppaWalker;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FloppaAddon extends MeteorAddon {
    /** Namespace used for the texture and sound in src/main/resources/assets/. */
    public static final String ID = "floppa_walker";

    public static final Logger LOG = LoggerFactory.getLogger("Floppa Walker");
    public static final Category CATEGORY = new Category("Floppa");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Floppa Walker");
        Modules.get().add(new FloppaWalker());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.floppawalker";
    }
}
