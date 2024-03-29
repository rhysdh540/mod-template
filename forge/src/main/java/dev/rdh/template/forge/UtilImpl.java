package dev.rdh.template.forge;

import dev.rdh.template.Template;

import net.minecraftforge.common.util.MavenVersionStringHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.List;

public class UtilImpl {
	public static String getVersion(String modid) {
		List<IModInfo> infoList = ModList.get().getModFileById(modid).getMods();
		if (infoList.size() > 1) {
			Template.LOGGER.error("Multiple mods for ID: " + modid);
		}
		for (IModInfo info : infoList) {
			if (info.getModId().equals(modid)) {
				return MavenVersionStringHelper.artifactVersionToString(info.getVersion());
			}
		}
		throw new RuntimeException("No mod found for ID: " + modid);
	}

	public static boolean isDevEnv() {
		return !FMLLoader.isProduction();
	}

	public static String platformName() {
		return "Forge";
	}

	public static boolean isModLoaded(String modid) {
		return ModList.get().isLoaded(modid);
	}
}
