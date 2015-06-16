package fr.scarex.updater;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import fr.scarex.updater.exception.VersionFormatException;
import fr.scarex.updater.handler.UpdaterHandler;
import fr.scarex.updater.utils.Grouper;

/**
 * @author SCAREX
 */
@Mod(modid = Updater.MODID, name = Updater.NAME, version = Updater.VERSION, clientSideOnly = true)
public class Updater
{
	public static final String MODID = "supdater";
	public static final String NAME = "Updater";
	public static final String VERSION = "0.1.0-snapshot";

	public static final Logger logger = LogManager.getLogger("Updater");

	public static Gson gson;

	public static Version mcversion;
	public static File modFolder;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Updater.modFolder = event.getSourceFile().getParentFile();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Version.class, new VersionDeserializer());
		builder.registerTypeAdapter(ModVersions.class, new ModVersionsDeserializer());
		gson = builder.create();
		MinecraftForge.EVENT_BUS.register(new UpdaterHandler());
		Runtime.getRuntime().addShutdownHook(new Thread("Deleter launcher") {
			@Override
			public void run() {
				try {
					File[] deleters = modFolder.listFiles(new FilenameFilter() {
						@Override
						public boolean accept(File paramFile, String name) {
							return name.startsWith("Deleter");
						}
					});
					if (deleters.length > 0) {
						File deleterF = deleters[0];
						Runtime.getRuntime().exec(new String[] {
								"java",
								"-jar",
								deleterF.getCanonicalPath(),
								deleterF.getParentFile().getCanonicalPath() });
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		doUpdate();
	}

	public static ArrayList<ModVersions> modsList = new ArrayList<Updater.ModVersions>();

	public static HashMap<String, Grouper<ModVersions, Version>> filesToDownload = new HashMap<String, Grouper<ModVersions, Version>>();

	public String getVersionFileURL() {
		return "http://scarex.fr/Updater/versions.json";
	}

	public String getUpdaterVersion() {
		return Updater.VERSION;
	}

	public static void doUpdate() {
		Thread t = new Thread("Updater Thread") {
			@Override
			public void run() {
				try {
					mcversion = Version.parseMcVersion();
				} catch (VersionFormatException e1) {
					e1.printStackTrace();
				}

				modsList.clear();
				for (ModContainer mod : Loader.instance().getModList()) {
					if (mod.getMod() != null) {
						Class<?> clazz = mod.getMod().getClass();
						if (!clazz.getCanonicalName().startsWith("net.minecraft")) {
							try {
								String rawVersion;
								Version currentVersion;
								try {
									Method versionMethod = clazz.getDeclaredMethod("getUpdaterVersion");
									rawVersion = (String) versionMethod.invoke(mod.getMod());
									currentVersion = Version.parseVersion(rawVersion);
								} catch (Exception e) {
									rawVersion = mod.getVersion();
									currentVersion = Version.parseVersion(rawVersion);
								}
								String stringUrl = (String) clazz.getDeclaredMethod("getVersionFileURL").invoke(mod.getMod());
								URL versionFile = new URL(stringUrl);

								if (isRemoteFileAccessibleWithType(versionFile, "application/json")) {

									InputStream is = null;
									try {
										HttpURLConnection con = (HttpURLConnection) versionFile.openConnection(Proxy.NO_PROXY);
										con.setConnectTimeout(15000);
										con.setReadTimeout(15000);

										is = con.getInputStream();
										ModVersions modV = gson.fromJson(IOUtils.toString(is, Charsets.UTF_8), ModVersions.class);
										modV.setMODID(mod.getModId());
										modV.setVersionFileLink(stringUrl);
										modV.setCurrentVersion(currentVersion);
										modsList.add(modV);
									} catch (Exception e) {
										logger.warn("Couldn't get version file with given url : " + versionFile, e);
									}
									IOUtils.closeQuietly(is);
								} else {
									logger.warn("Couldn't get version file with given url (not a json object) : " + versionFile);
								}
							} catch (Exception e) {
								logger.warn("Couldn't retrieve mod's informations from mod " + mod.getName() + "(" + clazz.getCanonicalName() + ")", e);
							}
						}
					}
				}
			}
		};
		t.start();
	}

	public static boolean isRemoteFileAccessibleWithType(URL url, String type) {
		try {
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("HEAD");
			return con.getResponseCode() == HttpURLConnection.HTTP_OK && (con.getContentType().equals(type) || type.equals("*"));
		} catch (Throwable t) {
			return false;
		}
	}

	public static void downloadAndDelete(final URL url, final File toDel, final String message) {
		new Thread("Download thread") {
			@Override
			public void run() {
				try {
					FileUtils.copyURLToFile(url, new File(Updater.modFolder, url.getFile().replaceFirst("^/((.+/)*)", "")));
					File f = new File(modFolder, "to_delete.updater");
					if (!f.exists()) f.createNewFile();
					BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
					bw.append(toDel.getName());
					bw.close();
				} catch (Exception e) {
					logger.warn(message, e);
					Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("game.player.hurt.fall.big"), 1.0F));
				}
			}
		}.start();
	}

	public static void appendFile(String name) {
		BufferedWriter bw = null;
		try {
			File f = new File(modFolder, "to_delete.updater");
			if (!f.exists()) f.createNewFile();
			bw = new BufferedWriter(new FileWriter(f, true));
			bw.append(name);
			bw.newLine();
			bw.close();
		} catch (Exception e) {
			logger.error("couldn't write file", e);
		} finally {
			IOUtils.closeQuietly(bw);
		}
	}

	public static class ModVersions
	{
		public static final Comparator<String> comparator = new Comparator<String>() {
			@Override
			public int compare(String s, String s1) {
				try {
					return Version.parseVersion(s).isNewerOrEqualWithSameIndex(mcversion, 1) ? (Version.parseVersion(s1).isNewerOrEqualWithSameIndex(mcversion, 1) ? s.compareTo(s1) : 1) : -1;
				} catch (VersionFormatException e) {
					e.printStackTrace();
				}
				return 0;
			}
		};
		public static final Pattern PARAMETERS_FINDER = Pattern.compile("\\{\\{([^}]+)\\}\\}");
		private String modid;
		private String versionFileLink;
		private String downloadLink;
		private TreeMap<String, Version[]> versions;
		private Version currentVersion;

		public ModVersions(String modid, String versionFile, String dlink, TreeMap<String, Version[]> versions, Version currentV) {
			this.modid = modid;
			this.versionFileLink = versionFile;
			this.downloadLink = dlink;
			this.versions = versions;
			this.currentVersion = currentV;
		}

		public ModVersions(String dlink, TreeMap<String, Version[]> versions) {
			this.downloadLink = dlink;
			this.versions = versions;
		}

		public Version getLatestVersionForUser(byte usertype, boolean acceptSnapshots) {
			Entry<String, Version[]> entry = this.versions.lastEntry();
			if ((entry.getKey().substring(0, 3)).equalsIgnoreCase(Minecraft.getMinecraft().getVersion().substring(0, 3))) {
				Version[] versions = entry.getValue();
				Version version = Version.NULL_VERSION;
				for (int i = 0; i < versions.length; i++) {
					if (versions[i].isNewerThanOrEqual(version) && versions[i].getUserType() == usertype && (versions[i].isSnapshot() ? acceptSnapshots : true)) version = versions[i];
				}
				if (!version.equals(Version.NULL_VERSION))
					return version;
				else
					return null;
			}
			return null;
		}

		public String getMCVersionForLatestVersion() {
			return this.versions.lastKey();
		}

		public URL getDownloadLinkForVersion(Version v, String mcversion) {
			String beginning = this.versionFileLink.replaceFirst("([^/]*)$", "");
			String middle = this.downloadLink != null ? this.downloadLink : "";
			String end = v.getDownloadLink() != null ? v.getDownloadLink() : "";
			String fullURL = "";
			if (v.getDownloadLink().startsWith("http://") || v.getDownloadLink().startsWith("https://")) {
				fullURL = v.getDownloadLink();
			} else {
				if (middle.startsWith("http://") || middle.startsWith("https://"))
					fullURL = middle + end;
				else
					fullURL = beginning + middle + end;
			}
			StringBuffer sb = new StringBuffer();
			Matcher m = PARAMETERS_FINDER.matcher(fullURL);
			while (m.find()) {
				if ("name".equalsIgnoreCase(m.group(1)))
					m.appendReplacement(sb, v.getName());
				else if ("version".equalsIgnoreCase(m.group(1)))
					m.appendReplacement(sb, v.getVersionString());
				else if ("user-type".equalsIgnoreCase(m.group(1)))
					m.appendReplacement(sb, "" + v.getUserType());
				else if ("importance".equalsIgnoreCase(m.group(1)))
					m.appendReplacement(sb, "" + v.getImportance());
				else if ("mc-version".equalsIgnoreCase(m.group(1))) m.appendReplacement(sb, mcversion != null ? mcversion : getMCVersionForVersion(v));
			}
			m.appendTail(sb);
			URL url = null;
			try {
				url = new URL(sb.toString());
			} catch (MalformedURLException e) {
				logger.warn(e);
			}
			return url;
		}

		public String getMCVersionForVersion(Version version) {
			Iterator<Entry<String, Version[]>> ite = this.versions.entrySet().iterator();
			while (ite.hasNext()) {
				Entry<String, Version[]> entry = (Entry<String, Updater.Version[]>) ite.next();
				for (Version v : entry.getValue()) {
					if (version.equals(v)) return entry.getKey();
				}
			}
			return "";
		}

		public String getModID() {
			return this.modid;
		}

		public void setMODID(String modid) {
			this.modid = modid;
		}

		public String getVersionFileLink() {
			return versionFileLink;
		}

		public void setVersionFileLink(String versionFileLink) {
			this.versionFileLink = versionFileLink;
		}

		public Version getCurrentVersion() {
			return currentVersion;
		}

		public void setCurrentVersion(Version currentVersion) {
			this.currentVersion = currentVersion;
		}

		public TreeMap<String, Version[]> getVersions() {
			return versions;
		}

		public void setVersions(TreeMap<String, Version[]> versions) {
			this.versions = versions;
		}

		@Override
		public String toString() {
			return "ModVersions [modid=" + modid + ", versionFileLink=" + versionFileLink + ", downloadLink=" + downloadLink + "]";
		}

		public String getMapAsString() {
			Iterator ite = versions.entrySet().iterator();
			String output = "";
			while (ite.hasNext()) {
				Entry<String, Version[]> entry = (Entry<String, Updater.Version[]>) ite.next();
				output += "[Mc-version=" + entry.getKey() + ", versions=" + Arrays.toString(entry.getValue()) + "]\n";
			}
			return output;
		}
	}

	public static class Version implements Comparable<Version>
	{
		private static final Pattern pattern = Pattern.compile("(?i)(\\d+)((?:\\.\\d+)*)([a-zA-Z])?(-snapshot)?(-src|-source)?");
		private static final Pattern patternMC = Pattern.compile("(?i)(\\d+)((?:\\.\\d+)*)([a-zA-Z1-9\\.-]*)$");
		public static final Version NULL_VERSION = new Version("0", 'a', true, true, new int[] { 0 });
		private String name;
		private String changes;
		private String downloadLink;
		private String versionString;
		private int[] releases;
		private char releaseChar;
		private boolean snapshot;
		private boolean sourceFile;
		private byte userType;
		private byte importance;

		public Version(String versionS, char releaseChar, boolean snapshot, boolean source, int ... releases) {
			this.versionString = versionS;
			this.releaseChar = releaseChar;
			this.snapshot = snapshot;
			this.sourceFile = source;
			this.releases = releases;
		}

		public boolean isNewerThanOrEqual(Version v) {
			if (v.getLength() > this.getLength())
				return false;
			else if (v.getLength() < this.getLength()) return true;
			for (int i = 0; i < this.getLength(); i++) {
				if (v.getIndex(i) > this.getIndex(i))
					return false;
				else if (v.getIndex(i) < this.getIndex(i)) return true;
			}
			if (this.getReleaseChar() >= v.getReleaseChar()) return true;
			return false;
		}

		public boolean isNewerOrEqualWithSameIndex(Version v, int index) {
			for (int i = 0; i <= index; i++) {
				if (v.getIndex(i) != this.getIndex(i)) return false;
			}
			for (int i = index + 1; i < Math.min(this.getLength(), v.getLength()); i++) {
				if (this.getIndex(i) < v.getIndex(i)) return false;
			}
			return true;
		}

		@Override
		public int compareTo(Version v) {
			return this.isNewerThanOrEqual(v) ? -1 : 1;
		}

		public static Version parseVersion(String version) throws VersionFormatException {
			Matcher m = pattern.matcher(version);
			if (m.matches()) {
				List<Integer> list = new ArrayList<Integer>();
				list.add(new Integer(m.group(1)));
				String[] array = m.group(2).split("\\.");
				for (int i = 1; i < array.length; i++) {
					list.add(Integer.parseInt(array[i]));
				}
				char c = m.group(3) != null && m.group(3).length() == 1 && Character.isAlphabetic(m.group(3).charAt(0)) ? m.group(3).charAt(0) : 'a';
				boolean snapshot = "-snapshot".equalsIgnoreCase(m.group(4)) ? true : false;
				boolean source = "-src".equalsIgnoreCase(m.group(5)) || "-source".equalsIgnoreCase(m.group(5)) ? true : false;
				return new Version(version, c, snapshot, source, ArrayUtils.toPrimitive(list.toArray(new Integer[0])));
			} else {
				throw new VersionFormatException(version);
			}
		}

		public static Version parseMcVersion() throws VersionFormatException {
			Matcher m = patternMC.matcher(Minecraft.getMinecraft().getVersion());
			if (m.matches()) {
				List<Integer> list = new ArrayList<Integer>();
				list.add(new Integer(m.group(1)));
				String[] array = m.group(2).split("\\.");
				for (int i = 1; i < array.length; i++) {
					list.add(Integer.parseInt(array[i]));
				}
				return new Version(Minecraft.getMinecraft().getVersion(), 'a', false, false, ArrayUtils.toPrimitive(list.toArray(new Integer[0])));
			} else {
				throw new VersionFormatException(Minecraft.getMinecraft().getVersion());
			}
		}

		public int getIndex(int i) {
			return this.releases[i];
		}

		public int getLength() {
			return this.releases.length;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public char getReleaseChar() {
			return releaseChar;
		}

		public void setReleaseChar(char releaseChar) {
			this.releaseChar = releaseChar;
		}

		public boolean isSnapshot() {
			return snapshot;
		}

		public void setSnapshot(boolean snapshot) {
			this.snapshot = snapshot;
		}

		public byte getUserType() {
			return userType;
		}

		public void setUserType(byte userType) {
			this.userType = userType;
		}

		public String getChanges() {
			return changes;
		}

		public void setChanges(String changes) {
			this.changes = changes;
		}

		public String getDownloadLink() {
			return downloadLink;
		}

		public void setDownloadLink(String downloadLink) {
			this.downloadLink = downloadLink;
		}

		public byte getImportance() {
			return importance;
		}

		public void setImportance(byte importance) {
			this.importance = importance;
		}

		public String getVersionString() {
			return versionString;
		}

		public void setVersionString(String versionString) {
			this.versionString = versionString;
		}

		public boolean isSourceFile() {
			return sourceFile;
		}

		public void setSourceFile(boolean sourceFile) {
			this.sourceFile = sourceFile;
		}

		@Override
		public String toString() {
			return "Version [name=" + name + ", changes=" + changes + ", downloadLink=" + downloadLink + ", versionString=" + versionString + ", releases=" + Arrays.toString(releases) + ", releaseChar=" + releaseChar + ", snapshot=" + snapshot + ", sourceFile=" + sourceFile + ", userType=" + userType + ", importance=" + importance + "]";
		}

		public String toName() {
			String output = "";
			for (int i = 0; i < releases.length - 1; i++) {
				output += releases[i] + ".";
			}
			return output + releases[releases.length - 1] + (releaseChar == 'a' ? "" : releaseChar) + (snapshot ? "-snapshot" : "") + (sourceFile ? "-source" : "");
		}

		public boolean equals(Object input) {
			if (input instanceof Version) return this.toName().equals(((Version) input).toName());
			return false;
		}
	}

	public static class VersionDeserializer implements JsonDeserializer<Version>
	{
		@Override
		public Version deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
			JsonObject obj = json.getAsJsonObject();
			Version version = null;
			try {
				version = Version.parseVersion(obj.get("version").getAsString());
			} catch (VersionFormatException e) {
				throw new JsonParseException("version couldn't be deserialized");
			}
			String name = obj.has("name") ? obj.get("name").getAsString() : "";
			version.setName(name);

			byte usertype = obj.has("user-type") ? obj.get("user-type").getAsByte() : 0;
			version.setUserType(usertype);

			String changes = obj.has("changes") ? obj.get("changes").getAsString() : "";
			version.setChanges(changes);

			byte imp = obj.has("importance") ? obj.get("importance").getAsByte() : 0;
			version.setImportance(imp);

			String dl = obj.has("download-link") ? obj.get("download-link").getAsString() : "";
			version.setDownloadLink(dl);
			return version;
		}
	}

	public static class ModVersionsDeserializer implements JsonDeserializer<ModVersions>
	{
		@Override
		public ModVersions deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
			JsonObject obj = json.getAsJsonObject();

			TreeMap<String, Version[]> map = new TreeMap<String, Version[]>(ModVersions.comparator);
			String dlink = obj.has("download-link") ? obj.get("download-link").getAsString() : "";

			JsonObject mcvobj = obj.get("mc-versions").getAsJsonObject();
			Iterator ite = mcvobj.entrySet().iterator();
			while (ite.hasNext()) {
				Entry<String, JsonElement> entry = (Entry<String, JsonElement>) ite.next();
				JsonArray jarray = entry.getValue().getAsJsonArray();

				Version[] versions = new Version[jarray.size()];
				Iterator ite1 = jarray.iterator();
				int i = 0;
				while (ite1.hasNext()) {
					JsonObject vobj = (JsonObject) ite1.next();
					versions[i] = ctx.<Version> deserialize(vobj, Version.class);
					i++;
				}
				Arrays.sort(versions);
				map.put(entry.getKey(), versions);
			}
			return new ModVersions(dlink, map);
		}
	}
}
