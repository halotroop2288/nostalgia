package net.fabricmc.filament.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import javax.inject.Inject;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

import net.fabricmc.filament.FilamentExtension;
import net.fabricmc.filament.FilamentGradlePlugin;
import net.fabricmc.loom.configuration.providers.minecraft.ManifestVersion;
import net.fabricmc.loom.configuration.providers.minecraft.MinecraftVersionMeta;
import net.fabricmc.loom.util.download.Download;

public abstract class MinecraftVersionMetaHelper {
	public abstract Property<String> getMinecraftVersion();

	public abstract Property<String> getMinecraftVersionManifestUrl();

	public abstract RegularFileProperty getVersionManifestFile();
	public abstract RegularFileProperty getVersionMetadataFile();

	@Inject
	public MinecraftVersionMetaHelper(FilamentExtension extension) {
		// Use the Minecraft version as an input to ensure the task re-runs on upgrade
		getMinecraftVersion().set(extension.getMinecraftVersion());
		getMinecraftVersionManifestUrl().set(extension.getMinecraftVersionManifestUrl());

		getVersionManifestFile().set(extension.getMinecraftFile("version_manifest.json"));
		getVersionMetadataFile().set(extension.getMinecraftFile("version.json"));
	}

	public MinecraftVersionMeta setup() throws IOException, URISyntaxException {
		final Path versionManifestPath = getVersionManifestFile().getAsFile().get().toPath();
		final Path versionMetadataPath = getVersionMetadataFile().getAsFile().get().toPath();

		final String versionManifest = Download.create(getMinecraftVersionManifestUrl().get())
				.defaultCache()
				.downloadString(versionManifestPath);

		final ManifestVersion mcManifest = FilamentGradlePlugin.OBJECT_MAPPER.readValue(versionManifest, ManifestVersion.class);

		ManifestVersion.Versions version = mcManifest.versions().stream()
				.filter(versions -> versions.id.equalsIgnoreCase(getMinecraftVersion().get()))
				.findFirst()
				.orElse(null);

		if (version == null) {
			throw new RuntimeException("Failed to find minecraft version: " + getMinecraftVersion().get());
		}

		final String versionMetadata = Download.create("https://babric.github.io/manifest-polyfill/" + getMinecraftVersion().get() +".json")
				.sha1("3325c74de7e4ef9b7b4634b1427d7e985eafe994")
				.downloadString(versionMetadataPath);

		try {
			return FilamentGradlePlugin.OBJECT_MAPPER.readValue(versionMetadata, MinecraftVersionMeta.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
