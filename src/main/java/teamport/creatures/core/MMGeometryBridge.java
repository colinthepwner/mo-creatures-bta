package teamport.creatures.core;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Set;
import java.util.TreeSet;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Converts entity models out of a copy of the original Mo' Creatures that the <em>player</em>
 * supplies into the Bedrock geometry BTA 8.0 renders, and writes them beside the bridged textures.
 * <p>
 * Companion to {@link MMAssetBridge}, and for the same reason: the original textures are painted
 * against the original box layout, so bridged textures only look right on the box layout they were
 * painted for. Geometry and textures have to come from the same source. Nothing here ships the
 * original's model data — only the converter — and it reads a file the player already owns.
 *
 * <h2>How the originals are read</h2>
 * The model classes are compiled against Minecraft Beta 1.7.3, whose types do not exist in BTA, so
 * they can never be loaded or instantiated. They are read as <em>bytecode</em> with ASM (already on
 * the classpath via fabric-loader/Mixin) and their constructors are interpreted symbolically: a
 * small abstract interpreter tracks constants, locals and object identity well enough to recover
 * every {@code new ModelRenderer(u,v)}, {@code addBox(...)}, {@code setRotationPoint(...)} and
 * rotation-angle assignment. Calls are matched on <em>descriptor</em>, never on name — the method
 * names are obfuscated single letters and differ between versions.
 *
 * <h2>The coordinate transform</h2>
 * Java model space hangs downward from a 24-unit origin; Bedrock is Y-up. Reading a point out of one
 * and into the other is therefore
 * <pre>
 *     pivot  = ( rpX, 24 - rpY, rpZ )
 *     origin = ( pivotX + offX, pivotY - offY - height, pivotZ + offZ )
 * </pre>
 * with size, texture offset and the {@code addBox} scale carrying over unchanged as {@code size},
 * {@code uv} and {@code inflate}, and rotation angles carrying over verbatim in degrees.
 * <p>
 * These constants are not guessed. BTA ships Bedrock geometry converted from the same Beta 1.7.3
 * models it inherited — {@code assets/minecraft/models/entity/{pig,wolf,sheep,cow,player}} — and
 * every bone in those files reproduces exactly under the formula above. Dragonfly's own
 * {@code StaticEntityModelMojang.boneTransform} confirms the rotation half: it applies
 * {@code rotateZ(-(rotZ + rad(rotation[2])))}, {@code rotateY(+(rotY + rad(rotation[1])))},
 * {@code rotateX(-(rotX + rad(rotation[0])))}, so a geometry {@code rotation} entry and a runtime
 * {@link org.useless.dragonfly.models.entity.BoneTransform} angle share one sign convention — and
 * this mod's renderers already feed Beta 1.7.3 angles straight into those fields.
 *
 * <h2>Inherited boxes</h2>
 * Most originals extend {@code ModelBase} and are self-contained. A few extend Minecraft's own
 * {@code ModelQuadruped} or {@code ModelBiped}, which are not in the archive and build boxes in a
 * constructor this cannot see. Those base layers are <em>vanilla Minecraft shapes, not the original
 * author's work</em>, so they are reconstructed here from the geometry BTA already ships (see
 * {@link #quadrupedBase} and {@link #bipedBase}) and the subclass's own boxes are composed on top.
 * A model whose base layer is unknown is reported and skipped rather than emitted half-built.
 * <p>
 * Two mobs take that to its conclusion: the original gave its boar and its duck no class at all and
 * rendered them straight on {@code ModelPig} and {@code ModelChicken}. There is nothing to read, so
 * the <em>whole</em> model is reconstructed the same way — see {@link #vanillaModel} — and paired
 * with the {@code boar.png} and {@code duck.png} that were painted for those layouts.
 */
public final class MMGeometryBridge {
	private MMGeometryBridge() {}

	private static final String MANIFEST = "/assets/creatures/model-bridge.properties";

	/** Java model space hangs from this many units; Bedrock counts up from the ground. */
	private static final float JAVA_ORIGIN_Y = 24.0F;

	/** Set once the bridge has run, so the audit can report on it. */
	public static int convertedCount = -1;
	public static int composedCount = -1;
	public static int skippedCount = -1;
	public static List<String> problems = new ArrayList<>();

	// ------------------------------------------------------------------------------------------
	// Manifest
	// ------------------------------------------------------------------------------------------

	/** One output model: which original class to read, what to call its bones, where to write it. */
	static final class ModelEntry {
		String id;
		String sourceClass;
		/** A second class the original renders as an extra layer over the first, sharing its texture. */
		String overlayClass;
		String base;
		/**
		 * One of Minecraft's own models, when the original had no class of its own at all and simply
		 * rendered the mob on a vanilla one. There is nothing in the archive to read, so the whole
		 * model is reconstructed the same way {@link #base} layers are — see {@link #vanillaModel}.
		 */
		String vanilla;
		/**
		 * Every texture this mob's renderers load, as original file name -> the paths in the pack it
		 * supplies. The converted UVs only line up with the art they were painted for, so the geometry
		 * ships if and only if all of these came out of the same archive.
		 * <p>
		 * One original can feed several paths: where this port splits a mob into states the original
		 * drew with one skin — a tamed kitty is the same cat — naming the source twice is the honest
		 * mapping, and the alternative is either an unrelated skin or no art at all.
		 */
		final Map<String, List<String>> textures = new LinkedHashMap<>();
		/**
		 * The UV space the original's texture offsets are expressed in — {@code ModelBase}'s own
		 * defaults unless the original overrode them. Deliberately not read from the PNG: Mo'
		 * Creatures ships double-resolution art, so a 128x64 file is still a 64x32 UV layout.
		 */
		int uvWidth = 64;
		int uvHeight = 32;
		/** Arguments the original's own renderer passes the model's constructor. */
		double[] args = new double[0];
		/** source slot (original field name) -> output bone name, or {@code null} to drop the bone. */
		final Map<String, String> renames = new LinkedHashMap<>();
		/** output bone name -> parent bone name. */
		final Map<String, String> parents = new LinkedHashMap<>();
		/**
		 * Output bone names whose cubes take Bedrock's {@code mirror} flag.
		 * <p>
		 * BTA's own conversion of Minecraft's models sets this on the bones that sit at {@code +x}
		 * even where the Java model never touched its mirror field — vanilla {@code ModelQuadruped}
		 * sets no mirror at all, yet BTA's {@code cow.geo.json} carries {@code "mirror": true} on
		 * {@code leg1} and {@code leg3}, the two legs at {@code midX = 4}. Bedrock unwraps those
		 * faces the other way round, and the flag is what puts them back.
		 * <p>
		 * The converter otherwise only mirrors what the original explicitly asked for, so a model
		 * that relies on the convention rather than the flag comes out with its {@code +x} side
		 * reversed. Invisible on symmetric fur; obvious on an asymmetric detail like a bird's toes.
		 */
		final Set<String> mirrorBones = new LinkedHashSet<>();
	}

	static final class Manifest {
		final List<ModelEntry> models = new ArrayList<>();
		/** base layer id -> the base class's field slots, in declaration order. */
		final Map<String, String[]> baseSlots = new HashMap<>();
		/** ModelRenderer field name -> what it means. */
		final Map<String, String> rendererFields = new HashMap<>();
	}

	private static Manifest manifest;

	static synchronized Manifest manifest() {
		if (manifest != null) return manifest;
		Manifest m = new Manifest();
		Properties props = new Properties();
		try (InputStream in = MMGeometryBridge.class.getResourceAsStream(MANIFEST)) {
			if (in != null) props.load(in);
		} catch (IOException e) {
			// Left empty; the caller reports an empty manifest.
		}
		Map<String, ModelEntry> byId = new LinkedHashMap<>();
		for (String key : new java.util.TreeSet<>(props.stringPropertyNames())) {
			String value = props.getProperty(key).trim();
			if (key.startsWith("base.")) {
				String rest = key.substring("base.".length());
				if (rest.endsWith(".slots")) {
					m.baseSlots.put(rest.substring(0, rest.length() - ".slots".length()), split(value));
				}
				continue;
			}
			if (key.startsWith("renderer.")) {
				// Inverted on purpose: the manifest reads "field-in-the-archive = meaning".
				m.rendererFields.put(value, key.substring("renderer.".length()));
				continue;
			}
			int dot = key.lastIndexOf('.');
			if (dot < 0) continue;
			ModelEntry entry = byId.computeIfAbsent(key.substring(0, dot), id -> {
				ModelEntry e = new ModelEntry();
				e.id = id;
				return e;
			});
			switch (key.substring(dot + 1)) {
				case "source" -> entry.sourceClass = value;
				case "overlay" -> entry.overlayClass = value.isEmpty() || "-".equals(value) ? null : value;
				case "base" -> entry.base = value.isEmpty() || "-".equals(value) ? null : value;
				case "vanilla" -> entry.vanilla = value.isEmpty() || "-".equals(value) ? null : value;
				case "textures" -> {
					for (String pair : split(value)) {
						int eq = pair.indexOf('=');
						if (eq < 0) continue;
						entry.textures.computeIfAbsent(pair.substring(0, eq).trim(), k -> new ArrayList<>())
							.add(pair.substring(eq + 1).trim());
					}
				}
				case "args" -> {
					String[] parts = split(value);
					entry.args = new double[parts.length];
					for (int i = 0; i < parts.length; i++) entry.args[i] = Double.parseDouble(parts[i]);
				}
				case "uv-size" -> {
					String[] parts = split(value);
					if (parts.length == 2) {
						entry.uvWidth = Integer.parseInt(parts[0]);
						entry.uvHeight = Integer.parseInt(parts[1]);
					}
				}
				case "mirror" -> entry.mirrorBones.addAll(List.of(split(value)));
				case "bones" -> {
					for (String pair : split(value)) {
						int eq = pair.indexOf('=');
						if (eq < 0) continue;
						String from = pair.substring(0, eq).trim();
						String to = pair.substring(eq + 1).trim();
						int at = to.indexOf('@');
						if (at >= 0) {
							entry.parents.put(to.substring(0, at).trim(), to.substring(at + 1).trim());
							to = to.substring(0, at).trim();
						}
						entry.renames.put(from, "-".equals(to) ? null : to);
					}
				}
				default -> { }
			}
		}
		for (ModelEntry entry : byId.values()) {
			if (entry.sourceClass != null || entry.vanilla != null) m.models.add(entry);
		}
		manifest = m;
		return m;
	}

	private static String[] split(String value) {
		if (value.isEmpty()) return new String[0];
		String[] parts = value.split(",");
		for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
		return parts;
	}

	/** Archive entry base names the bridge needs, so the archive is only walked once. */
	public static List<String> wantedEntries() {
		List<String> wanted = new ArrayList<>();
		for (ModelEntry entry : manifest().models) {
			// A vanilla-derived entry has no class to look for; only its art comes out of the archive.
			if (entry.sourceClass != null) wanted.add(entry.sourceClass.toLowerCase(Locale.ROOT) + ".class");
			if (entry.overlayClass != null) wanted.add(entry.overlayClass.toLowerCase(Locale.ROOT) + ".class");
			for (String texture : entry.textures.keySet()) wanted.add(texture.toLowerCase(Locale.ROOT));
		}
		return wanted;
	}

	// ------------------------------------------------------------------------------------------
	// Entry point
	// ------------------------------------------------------------------------------------------

	public static final class Result {
		/** Fully recovered from the original class alone. */
		public final List<String> converted = new ArrayList<>();
		/** Recovered, but with a vanilla Minecraft base layer composed underneath. */
		public final List<String> composed = new ArrayList<>();
		/** Not written: source missing, or something in the constructor could not be read. */
		public final List<String> skipped = new ArrayList<>();
		public final List<String> problems = new ArrayList<>();
	}

	/**
	 * @param archive entry base name (lower case) to bytes, as flattened out of the player's copy
	 * @param packDir the generated texture pack the textures already go to
	 */
	public static Result run(Map<String, byte[]> archive, File packDir) {
		Result result = new Result();
		Manifest m = manifest();
		if (m.models.isEmpty()) {
			result.problems.add("model manifest " + MANIFEST + " is empty or missing");
			publish(result);
			return result;
		}

		for (ModelEntry entry : m.models) {
			// Converted UVs are painted against the original art. Shipping the geometry without it
			// would land the original's box layout on this repo's own textures, which is worse than
			// leaving both alone, so this is all or nothing. It holds for a vanilla-derived model too:
			// the boar and the duck are painted for Minecraft's own pig and chicken layouts, and this
			// repo's own boxes are not those either.
			if (entry.textures.isEmpty()) {
				result.skipped.add(entry.id);
				result.problems.add(entry.id + ": the manifest lists no textures for it, so there is nothing "
					+ "to guarantee the converted geometry would be drawn with the art it was painted for");
				continue;
			}
			List<String> missingArt = new ArrayList<>();
			for (String texture : entry.textures.keySet()) {
				if (!archive.containsKey(texture.toLowerCase(Locale.ROOT))) missingArt.add(texture);
			}
			if (!missingArt.isEmpty()) {
				result.skipped.add(entry.id);
				result.problems.add(entry.id + ": the archive has no " + String.join(", ", missingArt)
					+ ", so its geometry would not match the textures it would be drawn with");
				continue;
			}

			List<Bone> recovered;
			boolean composedBase;
			if (entry.vanilla != null) {
				// No class of its own in the archive: the original rendered this mob straight on one of
				// Minecraft's own models, so the whole thing is reconstructed rather than read.
				recovered = vanillaModel(entry.vanilla);
				if (recovered == null) {
					result.skipped.add(entry.id);
					result.problems.add(entry.id + ": no reconstruction is known for Minecraft's own '"
						+ entry.vanilla + "' model");
					continue;
				}
				composedBase = true;
			} else {
				byte[] bytes = archive.get(entry.sourceClass.toLowerCase(Locale.ROOT) + ".class");
				if (bytes == null) {
					result.skipped.add(entry.id);
					result.problems.add(entry.id + ": '" + entry.sourceClass + ".class' is not in the archive");
					continue;
				}
				Extraction extraction;
				try {
					extraction = extract(bytes, m, entry.args);
				} catch (Throwable t) {
					result.skipped.add(entry.id);
					result.problems.add(entry.id + ": could not read " + entry.sourceClass + " (" + t + ")");
					continue;
				}
				if (!extraction.problems.isEmpty()) {
					result.skipped.add(entry.id);
					for (String problem : extraction.problems) result.problems.add(entry.id + ": " + problem);
					continue;
				}

				composedBase = false;
				if (extraction.superName != null && !extraction.superIsPlainBase) {
					if (entry.base == null) {
						result.skipped.add(entry.id);
						result.problems.add(entry.id + ": " + entry.sourceClass + " extends '" + extraction.superName
							+ "', whose boxes are built outside the archive, and the manifest names no base layer");
						continue;
					}
					List<Bone> base = baseLayer(entry.base, extraction, m, result, entry.id);
					if (base == null) {
						result.skipped.add(entry.id);
						continue;
					}
					extraction.bones = merge(base, extraction.bones);
					composedBase = true;
				}

				if (entry.overlayClass != null) {
					byte[] overlay = archive.get(entry.overlayClass.toLowerCase(Locale.ROOT) + ".class");
					if (overlay == null) {
						result.problems.add(entry.id + ": overlay '" + entry.overlayClass + ".class' is not in the archive");
					} else {
						try {
							Extraction second = extract(overlay, m, new double[0]);
							if (second.problems.isEmpty() && second.superIsPlainBase) {
								extraction.bones = merge(extraction.bones, second.bones);
							} else {
								result.problems.add(entry.id + ": overlay " + entry.overlayClass + " could not be read"
									+ (second.problems.isEmpty() ? " (it inherits boxes)" : " " + second.problems));
							}
						} catch (Throwable t) {
							result.problems.add(entry.id + ": overlay " + entry.overlayClass + " failed (" + t + ")");
						}
					}
				}
				recovered = extraction.bones;
			}

			List<Bone> bones = rename(recovered, entry);
			if (bones.isEmpty()) {
				result.skipped.add(entry.id);
				result.problems.add(entry.id + ": no bones survived extraction");
				continue;
			}
			reparent(bones, result, entry.id);

			String json = toGeometryJson(entry.id, bones, entry.uvWidth, entry.uvHeight);
			try {
				// The art goes with it, so a half-written pack can never pair one mob's geometry
				// with another source's textures.
				for (Map.Entry<String, List<String>> art : entry.textures.entrySet()) {
					byte[] image = archive.get(art.getKey().toLowerCase(Locale.ROOT));
					for (String path : art.getValue()) write(new File(packDir, path), image);
				}
				write(new File(packDir, "assets/creatures/models/entity/" + entry.id + ".json"),
					json.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				result.skipped.add(entry.id);
				result.problems.add(entry.id + ": could not write into the pack (" + e + ")");
				continue;
			}
			if (composedBase) {
				result.composed.add(entry.id);
			} else {
				result.converted.add(entry.id);
			}
		}

		writeModelManifest(result, packDir);

		publish(result);
		return result;
	}

	/**
	 * Declares the converted geometry to Dragonfly.
	 * <p>
	 * Writing the model files is not enough on its own, and the failure is silent. Dragonfly's
	 * {@code getGeometry} only ever reads its in-memory cache — it does no file I/O and never loads a
	 * path on demand — and that cache is filled exclusively by {@code Cache.reload}, which walks the
	 * selected packs reading {@code /assets/<namespace>/models/entity/models.json} and loading the
	 * paths listed under {@code model_paths}. Anything not named there is simply never read, and
	 * {@code getModel} hands back the fallback cube instead. This mirrors what BTA ships for its own
	 * namespace.
	 * <p>
	 * The built-in models are listed too. This pack shadows the namespace, and a pack that named only
	 * the converted models would leave every un-converted mob with nothing to load.
	 */
	private static void writeModelManifest(Result result, File packDir) {
		List<String> ids = new ArrayList<>(result.converted);
		ids.addAll(result.composed);
		ids.addAll(BUILTIN_MODEL_IDS);

		Set<String> paths = new TreeSet<>();
		for (String id : ids) {
			paths.add("/assets/creatures/models/entity/" + id + ".json");
		}

		StringBuilder json = new StringBuilder("{\n    \"model_paths\": [\n");
		int i = 0;
		for (String path : paths) {
			json.append("        \"").append(path).append('"');
			if (++i < paths.size()) json.append(',');
			json.append('\n');
		}
		json.append("    ]\n}\n");

		try {
			write(new File(packDir, "assets/creatures/models/entity/models.json"),
				json.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			result.problems.add("could not write the model manifest, so no converted model will load (" + e + ")");
		}
	}

	/** Models shipped in the mod's own jar, which the generated pack's manifest must not drop. */
	private static final List<String> BUILTIN_MODEL_IDS = Arrays.asList(
		"bear", "bird", "boar", "bunny", "deer", "fox",
		"horse", "horse_pegasus", "horse_unicorn", "kitty", "litterbox");

	private static void publish(Result result) {
		convertedCount = result.converted.size();
		composedCount = result.composed.size();
		skippedCount = result.skipped.size();
		problems = result.problems;
	}

	// ------------------------------------------------------------------------------------------
	// Model representation
	// ------------------------------------------------------------------------------------------

	/** One {@code addBox} call, still in Java model space. */
	static final class Cube {
		float offX, offY, offZ;
		int width, height, depth;
		float inflate;
		int u, v;
		boolean mirror;
	}

	/** One {@code ModelRenderer}, still in Java model space. */
	static final class Bone {
		/** The field the original assigned it to, or {@code #n} when it went nowhere named. */
		String slot;
		String name;
		String parent;
		float pointX, pointY, pointZ;
		float angleX, angleY, angleZ;
		int u, v;
		boolean mirror;
		final List<Cube> cubes = new ArrayList<>();
	}

	static final class Extraction {
		List<Bone> bones = new ArrayList<>();
		String superName;
		boolean superIsPlainBase;
		/** Constant arguments the constructor handed its superclass, when they were constant. */
		double[] superArgs = new double[0];
		final List<String> problems = new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------
	// Bytecode extraction
	// ------------------------------------------------------------------------------------------

	/** Marks {@code this} on the interpreter's stack. */
	private static final Object THIS = new Object();

	/** A {@code ModelRenderer[]} field, so array-held bones keep a usable slot name. */
	private static final class BoneArray {
		String field = "array";
		final Bone[] elements;

		BoneArray(int size) {
			this.elements = new Bone[size];
		}
	}

	static Extraction extract(byte[] classBytes, Manifest m, double[] args) {
		ClassNode node = new ClassNode();
		new org.objectweb.asm.ClassReader(classBytes).accept(node, org.objectweb.asm.ClassReader.SKIP_FRAMES
			| org.objectweb.asm.ClassReader.SKIP_DEBUG);

		Extraction out = new Extraction();
		out.superName = node.superName;

		Map<String, MethodNode> ctors = new LinkedHashMap<>();
		for (MethodNode method : node.methods) {
			if ("<init>".equals(method.name)) ctors.put(method.desc, method);
		}
		if (ctors.isEmpty()) {
			out.problems.add("no constructor found");
			return out;
		}
		// The no-arg constructor is what the original's own renderer usually calls, and the others are
		// reached from it by delegation with the arguments it passes. When the manifest records that
		// the original passes something else, start from the constructor that takes those instead.
		MethodNode entry = null;
		if (args.length > 0) {
			for (MethodNode ctor : ctors.values()) {
				if (Type.getArgumentTypes(ctor.desc).length == args.length) {
					entry = ctor;
					break;
				}
			}
			if (entry == null) out.problems.add("no constructor takes " + args.length + " arguments");
		}
		if (entry == null) {
			entry = ctors.containsKey("()V") ? ctors.get("()V") : ctors.values().iterator().next();
			args = new double[0];
		}

		Interpreter interpreter = new Interpreter(node, ctors, m, out);
		interpreter.run(entry, args);
		if (out.problems.isEmpty()) interpreter.readRestPose(node);
		return out;
	}

	/**
	 * Descriptors of the methods a Beta 1.7.3 model poses itself in — {@code setRotationAngles} and
	 * {@code render}, both of which take the six animation floats, plus {@code ModelBiped}'s variant
	 * that also takes the "is riding" flag. Matched on descriptor, like everything else here: the
	 * names are obfuscated and differ between the base class and its subclasses.
	 */
	private static boolean isPoseMethod(MethodNode method) {
		return !"<init>".equals(method.name) && !"<clinit>".equals(method.name)
			&& ("(FFFFFF)V".equals(method.desc) || "(FFFFFFZ)V".equals(method.desc));
	}

	private static final class Interpreter {
		private final ClassNode owner;
		private final Map<String, MethodNode> ctors;
		private final Manifest manifest;
		private final Extraction out;
		private final Map<String, Object> fields = new HashMap<>();
		private String rendererType;
		private int depth;
		/**
		 * Set while a pose method is being read rather than a constructor. In this mode nothing is
		 * built and nothing is reported: the only thing taken out is a constant rotation angle, and
		 * anything the reader does not understand ends that method quietly instead of failing the
		 * model. See {@link #readRestPose}.
		 */
		private boolean pose;
		/** Set by {@link #step} when the instruction it read was a jump it decided to take. */
		private AbstractInsnNode jumped;

		/** The six animation floats plus the trailing flag, all at rest. See {@link #readRestPose}. */
		private static final double[] REST_ARGUMENTS = {0, 0, 0, 0, 0, 0, 0};

		Interpreter(ClassNode owner, Map<String, MethodNode> ctors, Manifest manifest, Extraction out) {
			this.owner = owner;
			this.ctors = ctors;
			this.manifest = manifest;
			this.out = out;
		}

		/**
		 * The rest pose a model puts itself in, for the models that build a box in one frame and turn
		 * it into another when they pose rather than when they construct.
		 * <p>
		 * Minecraft's own {@code ModelQuadruped} is the pattern: it builds a body box standing on end
		 * and lays it along the spine with {@code body.rotateAngleX = PI/2} inside
		 * {@code setRotationAngles}, every frame, unconditionally. A subclass that copies that shape
		 * without extending the class — the original's {@code ModelBigCat2} is exactly this — carries
		 * the same assignment in its own pose method, and a converter that only reads constructors
		 * emits a cat standing on its tail. So the pose methods are read too.
		 * <p>
		 * Rotation points are read the same way, and matter just as much: the original's
		 * {@code ModelOgre2} builds its feet about y = 0 and moves them to y = 12 in its pose method,
		 * exactly as {@code ModelBiped} does, so a converter that only reads constructors leaves an
		 * ogre's feet at knee height. See {@link #writeRestPose} for which of the two wins when the
		 * constructor and the pose method disagree.
		 * <p>
		 * Only <em>constant</em> writes count, so a value computed from the animation arguments is
		 * skipped rather than frozen at whatever the reader made of it. Conditional bodies are stepped
		 * over — the branch a flag this can evaluate takes with the flag clear, and the branch an
		 * undecidable test is assumed to take — which reads the pose a model puts itself in with
		 * nothing applying: its rest pose, rather than its sitting, sneaking or swinging variant.
		 */
		void readRestPose(ClassNode node) {
			pose = true;
			try {
				for (MethodNode method : node.methods) {
					if (!isPoseMethod(method)) continue;
					depth = 0;
					// Every animation argument at zero: no stride, no head turn, nothing held. That is
					// what "rest pose" means, and it is what makes the constant beside an animation
					// term readable rather than unknown.
					run(method, REST_ARGUMENTS);
				}
			} catch (RuntimeException e) {
				// A pose method is a bonus, never a requirement: the model is already built.
			} finally {
				pose = false;
			}
		}

		void run(MethodNode method, double[] args) {
			if (depth++ > 8) {
				if (!pose) out.problems.add("constructor delegation is too deep to follow");
				return;
			}
			Object[] locals = new Object[Math.max(method.maxLocals, 8) + 8];
			locals[0] = THIS;
			int slot = 1;
			int index = 0;
			for (Type type : Type.getArgumentTypes(method.desc)) {
				locals[slot] = index < args.length ? args[index] : defaultValue(type.getDescriptor());
				slot += type.getSize();
				index++;
			}

			List<Object> stack = new ArrayList<>();
			AbstractInsnNode insn = method.instructions.getFirst();
			// A jump can only be taken so many times before something is looping; a model that loops is
			// not one this reads, and the budget is what stops it hanging rather than failing.
			int budget = method.instructions.size() * 4 + 64;
			try {
				while (insn != null && budget-- > 0) {
					jumped = null;
					if (!step(insn, stack, locals)) return;
					insn = jumped != null ? jumped : insn.getNext();
				}
			} finally {
				depth--;
			}
		}

		/** @return true when {@code target} comes after {@code from}; a backwards jump is a loop. */
		private static boolean isForward(AbstractInsnNode from, AbstractInsnNode target) {
			for (AbstractInsnNode at = from.getNext(); at != null; at = at.getNext()) {
				if (at == target) return true;
			}
			return false;
		}

		/**
		 * Takes, skips or gives up on a jump.
		 *
		 * @param taken {@code TRUE} to jump, {@code FALSE} to fall through, {@code null} when the test
		 *              could not be decided — which ends the method rather than guessing a branch
		 * @return false when the walk must stop
		 */
		private boolean branch(AbstractInsnNode insn, Boolean taken) {
			if (taken == null) {
				// A constructor is the model, so a branch this cannot decide has to fail it rather
				// than leave half the boxes out and call the result converted.
				if (!pose) {
					out.problems.add("constructor branches on something this cannot evaluate");
					return false;
				}
				// A pose method is read for its rest pose, and javac compiles 'if (x) { body }' as a
				// jump over the body. So an undecidable test skips its block and carries on, rather
				// than abandoning the method: that is the same answer every flag this can evaluate
				// already gives — none of the optional poses apply — and it is what reaches the
				// unconditional tail. ModelBiped's swing block guards on a float the base class
				// initialises outside the archive; stopping there loses everything after it.
				taken = Boolean.TRUE;
			}
			if (!taken) return true;
			AbstractInsnNode label = ((org.objectweb.asm.tree.JumpInsnNode) insn).label;
			// Forwards only: a backwards jump is a loop, and this reads straight-line code.
			if (!isForward(insn, label)) {
				if (!pose) out.problems.add("constructor jumps backwards, which this cannot follow");
				return false;
			}
			jumped = label;
			return true;
		}

		/** @return whether the jump is taken, or {@code null} when its operands are not both known. */
		private static Boolean decide(int op, Object left, Object right) {
			if (op == Opcodes.IFNULL || op == Opcodes.IFNONNULL) {
				// Only a tracked object proves non-null; an unknown could be either.
				if (left == null) return null;
				return op == Opcodes.IFNONNULL;
			}
			if (op == Opcodes.IF_ACMPEQ || op == Opcodes.IF_ACMPNE) {
				if (left == null || right == null) return null;
				return op == Opcodes.IF_ACMPEQ ? left == right : left != right;
			}
			if (!(left instanceof Double a)) return null;
			double b;
			if (right == null) {
				b = 0.0;
			} else if (right instanceof Double d) {
				b = d;
			} else {
				return null;
			}
			if (Double.isNaN(a) || Double.isNaN(b)) return null;
			return switch (op) {
				case Opcodes.IFEQ, Opcodes.IF_ICMPEQ -> a == b;
				case Opcodes.IFNE, Opcodes.IF_ICMPNE -> a != b;
				case Opcodes.IFLT, Opcodes.IF_ICMPLT -> a < b;
				case Opcodes.IFGE, Opcodes.IF_ICMPGE -> a >= b;
				case Opcodes.IFGT, Opcodes.IF_ICMPGT -> a > b;
				case Opcodes.IFLE, Opcodes.IF_ICMPLE -> a <= b;
				default -> null;
			};
		}

		/** Java's own default for a field or parameter this has no recorded value for. */
		private static Object defaultValue(String descriptor) {
			return switch (descriptor) {
				case "Z", "B", "C", "S", "I", "J" -> 0.0;
				default -> null;   // floats stay unknown: an animated angle must not read as zero
			};
		}

		/**
		 * Takes a rest pose out of a pose method — where a bone is turned to, and where it is hung
		 * from, before anything is animated. Only a constant counts: a value built from the animation
		 * arguments is a movement, not a shape.
		 * <p>
		 * Rotation and rotation point are taken on different terms, because the originals treat them
		 * differently. An angle is <em>added to</em> across a frame, so one the constructor already
		 * declared is the model's own and wins; a pose method only fills in an angle left at zero.
		 * A rotation point is <em>assigned</em>, unconditionally, immediately before the box is drawn,
		 * so the pose method's value is the one the original renders with and it replaces whatever the
		 * constructor set. Minecraft's own {@code ModelBiped} is the pattern: it builds its legs about
		 * y = 12 and then writes {@code rotationPointY = 12} again every frame — harmless there, but
		 * the original's {@code ModelOgre2} builds its feet about y = 0 and writes 12 in the pose
		 * method, so a converter that only reads constructors hangs an ogre's feet at knee height.
		 * The boxes are held as offsets from the point, so moving it carries them along.
		 */
		private void writeRestPose(Bone bone, FieldInsnNode field, Object value) {
			if (!(value instanceof Double d) || Double.isNaN(d)) return;
			switch (meaning(field.name)) {
				case "point-x" -> bone.pointX = (float) (double) d;
				case "point-y" -> bone.pointY = (float) (double) d;
				case "point-z" -> bone.pointZ = (float) (double) d;
				case "rotate-x" -> { if (unrotated(bone) && d != 0.0) bone.angleX = (float) (double) d; }
				case "rotate-y" -> { if (unrotated(bone) && d != 0.0) bone.angleY = (float) (double) d; }
				case "rotate-z" -> { if (unrotated(bone) && d != 0.0) bone.angleZ = (float) (double) d; }
				default -> { }
			}
		}

		private static boolean unrotated(Bone bone) {
			return bone.angleX == 0 && bone.angleY == 0 && bone.angleZ == 0;
		}

		/** @return false when the constructor cannot be read any further. */
		private boolean step(AbstractInsnNode insn, List<Object> stack, Object[] locals) {
			int op = insn.getOpcode();
			switch (op) {
				case -1 -> {
					return true; // labels, line numbers, frames
				}
				case Opcodes.ACONST_NULL -> push(stack, null);
				case Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2,
					Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5 ->
					push(stack, (double) (op - Opcodes.ICONST_0));
				case Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2 ->
					push(stack, (double) (op - Opcodes.FCONST_0));
				case Opcodes.BIPUSH, Opcodes.SIPUSH -> push(stack, (double) ((IntInsnNode) insn).operand);
				case Opcodes.LDC -> {
					Object value = ((LdcInsnNode) insn).cst;
					push(stack, value instanceof Number n ? (Object) n.doubleValue() : null);
				}
				case Opcodes.ILOAD, Opcodes.FLOAD, Opcodes.ALOAD -> push(stack, locals[((VarInsnNode) insn).var]);
				case Opcodes.ISTORE, Opcodes.FSTORE, Opcodes.ASTORE -> locals[((VarInsnNode) insn).var] = pop(stack);
				case Opcodes.DUP -> push(stack, peek(stack));
				case Opcodes.POP -> pop(stack);
				case Opcodes.FADD, Opcodes.IADD -> binary(stack, '+');
				case Opcodes.FSUB, Opcodes.ISUB -> binary(stack, '-');
				case Opcodes.FMUL, Opcodes.IMUL -> binary(stack, '*');
				case Opcodes.FDIV -> binary(stack, '/');
				case Opcodes.FNEG, Opcodes.INEG -> {
					Object value = pop(stack);
					push(stack, value instanceof Double d ? (Object) (-d) : null);
				}
				case Opcodes.I2F, Opcodes.F2I, Opcodes.I2D, Opcodes.F2D, Opcodes.D2F, Opcodes.CHECKCAST -> {
					// Identity for the values this interpreter tracks.
				}
				case Opcodes.NEW -> {
					String type = ((TypeInsnNode) insn).desc;
					push(stack, type.equals(rendererType) ? new Bone() : new PendingNew(type));
				}
				case Opcodes.ANEWARRAY -> {
					Object size = pop(stack);
					push(stack, size instanceof Double d ? new BoneArray((int) (double) d) : null);
				}
				case Opcodes.AASTORE -> {
					Object value = pop(stack);
					Object index = pop(stack);
					Object array = pop(stack);
					if (array instanceof BoneArray a && index instanceof Double i && value instanceof Bone bone) {
						int at = (int) (double) i;
						if (at >= 0 && at < a.elements.length) a.elements[at] = bone;
						bone.slot = a.field + "[" + at + "]";
					}
				}
				case Opcodes.AALOAD -> {
					Object index = pop(stack);
					Object array = pop(stack);
					push(stack, array instanceof BoneArray a && index instanceof Double i
						&& (int) (double) i >= 0 && (int) (double) i < a.elements.length
						? a.elements[(int) (double) i] : null);
				}
				case Opcodes.GETSTATIC -> push(stack, null);
				case Opcodes.PUTSTATIC -> pop(stack);
				case Opcodes.GETFIELD -> {
					FieldInsnNode field = (FieldInsnNode) insn;
					Object target = pop(stack);
					if (target == THIS) {
						// A field nothing has assigned still has a value: Java's own default. That is
						// what makes reading a pose method mean something -- every flag a model tests
						// is false, so the branch taken is the one it rests in.
						push(stack, fields.containsKey(field.name) ? fields.get(field.name)
							: defaultValue(field.desc));
					} else if (target instanceof Bone bone) {
						push(stack, readRendererField(bone, field.name));
					} else {
						push(stack, null);
					}
				}
				case Opcodes.PUTFIELD -> {
					FieldInsnNode field = (FieldInsnNode) insn;
					Object value = pop(stack);
					Object target = pop(stack);
					if (pose) {
						// A pose method only ever tells this one thing: the rest pose a bone is put
						// in before anything is animated. Its own fields are left alone -- rewriting
						// them here would replace a bone the constructor already built.
						if (target instanceof Bone bone) writeRestPose(bone, field, value);
					} else if (target == THIS) {
						if (value instanceof Bone bone) {
							bone.slot = field.name;
						} else if (value instanceof BoneArray array) {
							array.field = field.name;
						}
						fields.put(field.name, value);
					} else if (target instanceof Bone bone) {
						if (!writeRendererField(bone, field, value)) {
							out.problems.add("unreadable assignment to " + field.owner + "." + field.name);
							return false;
						}
					}
				}
				case Opcodes.INVOKESTATIC -> {
					// Nothing static this reads returns a constant -- MathHelper.cos and friends take
					// the animation arguments -- so the arguments come off and an unknown comes back.
					MethodInsnNode call = (MethodInsnNode) insn;
					popArgs(stack, Type.getArgumentTypes(call.desc));
					if (!Type.VOID_TYPE.equals(Type.getReturnType(call.desc))) push(stack, null);
				}
				case Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE,
					Opcodes.IFNULL, Opcodes.IFNONNULL -> {
					return branch(insn, decide(op, pop(stack), null));
				}
				case Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE,
					Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE, Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE -> {
					Object right = pop(stack);
					return branch(insn, decide(op, pop(stack), right));
				}
				case Opcodes.GOTO -> {
					return branch(insn, Boolean.TRUE);
				}
				case Opcodes.FCMPL, Opcodes.FCMPG, Opcodes.DCMPL, Opcodes.DCMPG, Opcodes.LCMP -> {
					pop(stack);
					pop(stack);
					push(stack, null);
				}
				case Opcodes.INVOKESPECIAL -> {
					MethodInsnNode call = (MethodInsnNode) insn;
					Type[] params = Type.getArgumentTypes(call.desc);
					double[] args = popArgs(stack, params);
					Object target = pop(stack);
					if (pose) return true;
					if (!"<init>".equals(call.name)) return true;
					if (target instanceof PendingNew && "(II)V".equals(call.desc) && rendererType == null) {
						// The first two-int constructor in a model constructor is ModelRenderer(u,v).
						// Matched on descriptor: the class and method names are obfuscated.
						rendererType = ((PendingNew) target).type;
						Bone bone = new Bone();
						replace(stack, target, bone);
						applyRendererCtor(bone, args);
					} else if (target instanceof Bone bone && "(II)V".equals(call.desc)) {
						applyRendererCtor(bone, args);
					} else if (target == THIS) {
						if (call.owner.equals(owner.name)) {
							MethodNode delegate = ctors.get(call.desc);
							if (delegate == null) {
								out.problems.add("constructor delegates to a missing " + call.desc);
								return false;
							}
							run(delegate, args);
						} else {
							out.superArgs = args;
							out.superIsPlainBase = params.length == 0;
						}
					}
				}
				case Opcodes.INVOKEVIRTUAL -> {
					MethodInsnNode call = (MethodInsnNode) insn;
					Type[] params = Type.getArgumentTypes(call.desc);
					double[] args = popArgs(stack, params);
					Object target = pop(stack);
					if (!Type.VOID_TYPE.equals(Type.getReturnType(call.desc))) push(stack, null);
					if (pose) return true;
					if (!(target instanceof Bone bone)) return true;
					switch (call.desc) {
						// Matched on descriptor, never on name.
						case "(FFFIIIF)V" -> addBox(bone, args, (float) args[6]);   // addBox + scale
						case "(FFFIII)V" -> addBox(bone, args, 0.0F);               // addBox
						case "(FFF)V" -> {                                          // setRotationPoint
							bone.pointX = (float) args[0];
							bone.pointY = (float) args[1];
							bone.pointZ = (float) args[2];
						}
						default -> {
							out.problems.add("unreadable ModelRenderer call " + call.desc + " in the constructor");
							return false;
						}
					}
				}
				case Opcodes.RETURN -> {
					return false;
				}
				default -> {
					if (pose) return false;
					out.problems.add("constructor uses an instruction this cannot follow (opcode " + op + ")");
					return false;
				}
			}
			return true;
		}

		private void applyRendererCtor(Bone bone, double[] args) {
			bone.u = args.length > 0 ? (int) args[0] : 0;
			bone.v = args.length > 1 ? (int) args[1] : 0;
			bone.slot = "#" + out.bones.size();
			out.bones.add(bone);
		}

		private void addBox(Bone bone, double[] args, float inflate) {
			Cube cube = new Cube();
			cube.offX = (float) args[0];
			cube.offY = (float) args[1];
			cube.offZ = (float) args[2];
			cube.width = (int) args[3];
			cube.height = (int) args[4];
			cube.depth = (int) args[5];
			cube.inflate = inflate;
			cube.u = bone.u;
			cube.v = bone.v;
			cube.mirror = bone.mirror;
			bone.cubes.add(cube);
		}

		private Object readRendererField(Bone bone, String name) {
			return switch (meaning(name)) {
				case "rotate-x" -> (double) bone.angleX;
				case "rotate-y" -> (double) bone.angleY;
				case "rotate-z" -> (double) bone.angleZ;
				case "point-x" -> (double) bone.pointX;
				case "point-y" -> (double) bone.pointY;
				case "point-z" -> (double) bone.pointZ;
				default -> null;
			};
		}

		private boolean writeRendererField(Bone bone, FieldInsnNode field, Object value) {
			String meaning = meaning(field.name);
			if ("mirror".equals(meaning)) {
				bone.mirror = value instanceof Double d && d != 0.0;
				return true;
			}
			if (!(value instanceof Double d)) return false;
			switch (meaning) {
				case "rotate-x" -> bone.angleX = (float) (double) d;
				case "rotate-y" -> bone.angleY = (float) (double) d;
				case "rotate-z" -> bone.angleZ = (float) (double) d;
				case "point-x" -> bone.pointX = (float) (double) d;
				case "point-y" -> bone.pointY = (float) (double) d;
				case "point-z" -> bone.pointZ = (float) (double) d;
				default -> {
					return false;
				}
			}
			return true;
		}

		private String meaning(String field) {
			String meaning = manifest.rendererFields.get(field);
			return meaning == null ? "" : meaning;
		}

		private double[] popArgs(List<Object> stack, Type[] params) {
			double[] args = new double[params.length];
			for (int i = params.length - 1; i >= 0; i--) {
				Object value = pop(stack);
				args[i] = value instanceof Double d ? d : Double.NaN;
			}
			return args;
		}

		private void replace(List<Object> stack, Object from, Object to) {
			for (int i = 0; i < stack.size(); i++) {
				if (stack.get(i) == from) stack.set(i, to);
			}
		}

		private void push(List<Object> stack, Object value) {
			stack.add(value);
		}

		private Object pop(List<Object> stack) {
			return stack.isEmpty() ? null : stack.remove(stack.size() - 1);
		}

		private Object peek(List<Object> stack) {
			return stack.isEmpty() ? null : stack.get(stack.size() - 1);
		}

		private static boolean isZero(Object value) {
			return value instanceof Double d && d == 0.0;
		}

		private void binary(List<Object> stack, char operator) {
			Object right = pop(stack);
			Object left = pop(stack);
			if (operator == '*' && (isZero(left) || isZero(right))) {
				// Anything finite times zero is zero, and everything a model multiplies is finite.
				// This is what makes a rest pose readable: an animation term is a trigonometric
				// function of arguments this does not track, scaled by the limb-swing amount, and at
				// rest that amount is zero -- so the term vanishes and the constant beside it is the
				// value the original actually draws with.
				push(stack, 0.0);
				return;
			}
			if (!(left instanceof Double a) || !(right instanceof Double b)) {
				push(stack, null);
				return;
			}
			push(stack, switch (operator) {
				case '+' -> a + b;
				case '-' -> a - b;
				case '*' -> a * b;
				default -> b == 0 ? Double.NaN : a / b;
			});
		}
	}

	/** A {@code new} that has not been identified as a ModelRenderer yet. */
	private record PendingNew(String type) {}

	// ------------------------------------------------------------------------------------------
	// Vanilla base layers
	// ------------------------------------------------------------------------------------------

	private static List<Bone> baseLayer(String id, Extraction extraction, Manifest m, Result result, String modelId) {
		String[] slots = m.baseSlots.get(id);
		if (slots == null) {
			result.problems.add(modelId + ": manifest names base layer '" + id + "' but lists no field slots for it");
			return null;
		}
		double[] args = extraction.superArgs;
		List<Bone> bones = switch (id) {
			case "quadruped" -> quadrupedBase(arg(args, 0, 8), (float) arg(args, 1, 0));
			case "biped" -> bipedBase((float) arg(args, 0, 0), (float) arg(args, 1, 0));
			default -> null;
		};
		if (bones == null) {
			result.problems.add(modelId + ": no reconstruction is known for base layer '" + id + "'");
			return null;
		}
		for (int i = 0; i < bones.size() && i < slots.length; i++) {
			bones.get(i).slot = slots[i];
		}
		return bones;
	}

	private static double arg(double[] args, int index, double fallback) {
		return index < args.length && !Double.isNaN(args[index]) ? args[index] : fallback;
	}

	/**
	 * Minecraft's own {@code ModelQuadruped(legLength, inflate)} — the pig/cow/sheep box layout.
	 * <p>
	 * Read back out of the Bedrock geometry BTA already ships rather than from memory:
	 * {@code pig.geo.json} is this class unmodified at {@code legLength = 6} and {@code sheep.geo.json}
	 * overrides only head and body, so its legs pin the layout again at {@code legLength = 12}. Every
	 * value below reproduces those files exactly through {@link #toGeometryJson}.
	 */
	static List<Bone> quadrupedBase(double legLength, float inflate) {
		int leg = (int) legLength;
		List<Bone> bones = new ArrayList<>();
		bones.add(bone("head", 0, 0, 0, 18 - leg, -6, box(-4, -4, -8, 8, 8, 8, inflate)));
		Bone body = bone("body", 28, 8, 0, 11, 2, box(-5, -10, -7, 10, 16, 8, inflate));
		body.angleX = (float) (Math.PI / 2.0);
		bones.add(body);
		bones.add(bone("legLeftBack", 0, 16, -3, 24 - leg, 7, box(-2, 0, -2, 4, leg, 4, inflate)));
		bones.add(bone("legRightBack", 0, 16, 3, 24 - leg, 7, box(-2, 0, -2, 4, leg, 4, inflate)));
		bones.add(bone("legLeftFront", 0, 16, -3, 24 - leg, -5, box(-2, 0, -2, 4, leg, 4, inflate)));
		bones.add(bone("legRightFront", 0, 16, 3, 24 - leg, -5, box(-2, 0, -2, 4, leg, 4, inflate)));
		return bones;
	}

	/**
	 * Minecraft's own {@code ModelBiped(inflate, yOffset)} — the player box layout.
	 * <p>
	 * Read back out of BTA's {@code player.geo.json} and {@code humanoid.geo.json}, whose head,
	 * hat (the {@code inflate + 0.5} one), chest, arm and leg cubes all reproduce exactly from the
	 * values below.
	 */
	static List<Bone> bipedBase(float inflate, float yOffset) {
		List<Bone> bones = new ArrayList<>();
		bones.add(bone("head", 0, 0, 0, yOffset, 0, box(-4, -8, -4, 8, 8, 8, inflate)));
		bones.add(bone("headwear", 32, 0, 0, yOffset, 0, box(-4, -8, -4, 8, 8, 8, inflate + 0.5F)));
		bones.add(bone("body", 16, 16, 0, yOffset, 0, box(-4, 0, -2, 8, 12, 4, inflate)));
		bones.add(bone("armRight", 40, 16, -5, 2 + yOffset, 0, box(-3, -2, -2, 4, 12, 4, inflate)));
		bones.add(bone("armLeft", 40, 16, 5, 2 + yOffset, 0, box(-1, -2, -2, 4, 12, 4, inflate)));
		bones.add(bone("legRight", 0, 16, -2, 12 + yOffset, 0, box(-2, 0, -2, 4, 12, 4, inflate)));
		bones.add(bone("legLeft", 0, 16, 2, 12 + yOffset, 0, box(-2, 0, -2, 4, 12, 4, inflate)));
		for (Bone bone : bones) {
			if (bone.name.endsWith("Left")) bone.cubes.get(0).mirror = true;
		}
		return bones;
	}

	/**
	 * A whole model of Minecraft's own, for the two mobs the original never gave a class to: it
	 * rendered its boar on {@code ModelPig} and its duck on {@code ModelChicken}, so there is nothing
	 * in the archive to read and the art is painted for the vanilla box layout.
	 * <p>
	 * These are reconstructed exactly the way {@link #quadrupedBase} and {@link #bipedBase} are, and
	 * from the same source: the Bedrock geometry BTA already ships. That matters more here than for a
	 * base layer, because it is the <em>whole</em> model — a boar drawn on anything but BTA's own pig
	 * boxes would put {@code boar.png} in the wrong places.
	 *
	 * @return {@code null} when the manifest names a model with no reconstruction here
	 */
	private static List<Bone> vanillaModel(String id) {
		return switch (id) {
			case "pig" -> pigBase();
			case "chicken" -> chickenBase();
			default -> null;
		};
	}

	/**
	 * Minecraft's own {@code ModelPig} — {@code ModelQuadruped(6, 0)} plus the snout, which the pig
	 * adds as a second cube on the inherited head rather than as a bone of its own.
	 * <p>
	 * Reproduces {@code assets/minecraft/models/entity/pig/pig.geo.json} exactly, mirrored legs
	 * included: BTA's conversion mirrors the two legs on {@code +x} and the plain quadruped base does
	 * not, so this sets it here rather than in {@link #quadrupedBase}, where it would also change the
	 * bear and the lion's mane.
	 */
	static List<Bone> pigBase() {
		List<Bone> bones = quadrupedBase(6, 0);
		Cube snout = box(-2, 0, -9, 4, 3, 1, 0);
		snout.u = 16;
		snout.v = 16;
		bones.get(0).cubes.add(snout);
		for (Bone bone : bones) {
			if (bone.name.startsWith("legRight")) bone.cubes.get(0).mirror = true;
		}
		return bones;
	}

	/**
	 * Minecraft's own {@code ModelChicken}.
	 * <p>
	 * Read back out of BTA's {@code chicken/chicken.geo.json} the same way the other bases are, and it
	 * is that file rather than Beta 1.7.3's class that is authoritative: BTA moved the wing pivots one
	 * unit inboard, and the duck has to hang on the boxes BTA actually draws a chicken with. Every
	 * texture offset is untouched, which is what {@code duck.png} is painted against.
	 * <p>
	 * Sides follow the manifest's convention rather than vanilla's field names — {@code -x} is left.
	 */
	static List<Bone> chickenBase() {
		List<Bone> bones = new ArrayList<>();
		Bone body = bone("body", 0, 9, 0, 16, 0, box(-3, -4, -3, 6, 8, 6, 0));
		body.angleX = (float) (Math.PI / 2.0);
		bones.add(body);
		bones.add(bone("head", 0, 0, 0, 15, -4, box(-2, -6, -2, 4, 6, 3, 0)));
		bones.add(bone("beak", 14, 0, 0, 15, -4, box(-2, -4, -4, 4, 2, 2, 0)));
		bones.add(bone("wattle", 14, 4, 0, 15, -4, box(-1, -2, -3, 2, 2, 2, 0)));
		bones.add(bone("legLeft", 26, 0, -2, 19, 1, box(-1, 0, -3, 3, 5, 3, 0)));
		bones.add(bone("legRight", 26, 0, 1, 19, 1, box(-1, 0, -3, 3, 5, 3, 0)));
		bones.add(bone("wingLeft", 24, 13, -3, 13, 0, box(-1, 0, -3, 1, 4, 6, 0)));
		bones.add(bone("wingRight", 24, 13, 3, 13, 0, box(0, 0, -3, 1, 4, 6, 0)));
		return bones;
	}

	private static Bone bone(String name, int u, int v, float pointX, float pointY, float pointZ, Cube cube) {
		Bone bone = new Bone();
		bone.name = name;
		bone.slot = name;
		bone.u = u;
		bone.v = v;
		bone.pointX = pointX;
		bone.pointY = pointY;
		bone.pointZ = pointZ;
		cube.u = u;
		cube.v = v;
		bone.cubes.add(cube);
		return bone;
	}

	static Cube box(float offX, float offY, float offZ, int w, int h, int d, float inflate) {
		Cube cube = new Cube();
		cube.offX = offX;
		cube.offY = offY;
		cube.offZ = offZ;
		cube.width = w;
		cube.height = h;
		cube.depth = d;
		cube.inflate = inflate;
		return cube;
	}

	/**
	 * Composes the subclass's own boxes onto the base layer. A subclass that assigns to a field slot
	 * the base class already owns is replacing that bone, so it keeps the base's position and name;
	 * anything else is appended.
	 */
	private static List<Bone> merge(List<Bone> base, List<Bone> own) {
		List<Bone> merged = new ArrayList<>(base);
		for (Bone bone : own) {
			int existing = -1;
			for (int i = 0; i < merged.size(); i++) {
				if (merged.get(i).slot.equals(bone.slot)) {
					existing = i;
					break;
				}
			}
			if (existing >= 0) {
				Bone replaced = merged.get(existing);
				bone.name = replaced.name;
				// The base class still poses whatever ends up in its slot, so a rest rotation it
				// applies there survives the replacement. ModelQuadruped stands its body upright
				// this way, and a bear whose body forgot that is a bear lying on its face.
				if (bone.angleX == 0 && bone.angleY == 0 && bone.angleZ == 0) {
					bone.angleX = replaced.angleX;
					bone.angleY = replaced.angleY;
					bone.angleZ = replaced.angleZ;
				}
				merged.set(existing, bone);
			} else {
				merged.add(bone);
			}
		}
		return merged;
	}

	private static List<Bone> rename(List<Bone> bones, ModelEntry entry) {
		List<Bone> kept = new ArrayList<>();
		for (Bone bone : bones) {
			String name = bone.name != null ? bone.name : bone.slot;
			if (entry.renames.containsKey(bone.slot)) {
				name = entry.renames.get(bone.slot);
				if (name == null) continue;
			}
			bone.name = name;
			bone.parent = entry.parents.get(name);
			// Applied here rather than at extraction, because the manifest names bones by their
			// output name and this is where that name is settled.
			if (entry.mirrorBones.contains(name)) {
				for (Cube cube : bone.cubes) {
					cube.mirror = true;
				}
			}
			kept.add(bone);
		}
		return kept;
	}

	/**
	 * Puts every bone the manifest reattached with {@code @parent} back where the original drew it.
	 * <p>
	 * Reattaching is not free. The originals have no hierarchy at all — each {@code ModelRenderer} is
	 * drawn about its own rotation point, and never about another's — while Dragonfly composes a bone's
	 * transform on top of its parent's. So a bone hung off a parent that has a rest rotation of its own
	 * would pick that rotation up a second time and swing about the parent's pivot rather than its own.
	 * A rat's snout at 90 degrees to its face, or a werewolf's shin bent 45 degrees out of its thigh.
	 * <p>
	 * Dragonfly builds a bone as {@code T(pivot) · Rz · Ry · Rx · T(-pivot)} and applies the parent's
	 * first, so reproducing the original {@code M} needs {@code M' = M_parent⁻¹ · M}: the child's pivot
	 * moves into the parent's unrotated frame and the parent's angles come off its own. Moving the pivot
	 * carries the cubes with it — a cube's {@code origin} is derived from the pivot in
	 * {@link #toGeometryJson} — which is exactly the shift the composition needs.
	 * <p>
	 * The pivot half is exact for any parent. The angle half subtracts componentwise, which is exact as
	 * long as the parent turns about one axis and the child does not also turn about an axis Dragonfly
	 * applies <em>outside</em> it — the order is Z, then Y, then X. Every pairing the manifest asks for
	 * is a parent that turns about X alone or not at all; anything else is reported rather than bent.
	 */
	private static void reparent(List<Bone> bones, Result result, String modelId) {
		Map<String, Bone> byName = new LinkedHashMap<>();
		for (Bone bone : bones) byName.put(bone.name, bone);

		List<Bone> children = new ArrayList<>();
		for (Bone bone : bones) {
			if (bone.parent != null) children.add(bone);
		}
		// Shallowest first, so a parent is already corrected by the time its own children are reached.
		children.sort(Comparator.comparingInt(bone -> depth(bone, byName, bones.size())));

		for (Bone bone : children) {
			Bone parent = byName.get(bone.parent);
			if (parent == null) {
				result.problems.add(modelId + ": bone '" + bone.name + "' is reattached to '" + bone.parent
					+ "', which this model has no bone for");
				bone.parent = null;
				continue;
			}
			if (parent.angleX == 0 && parent.angleY == 0 && parent.angleZ == 0) continue;
			if (turnsOutside(bone, parent)) {
				result.problems.add(modelId + ": '" + bone.name + "' and its parent '" + parent.name
					+ "' turn about axes that do not compose, so its rest pose is approximate");
			}

			float[] pivot = unrotate(parent, bone.pointX, JAVA_ORIGIN_Y - bone.pointY, bone.pointZ);
			bone.pointX = pivot[0];
			bone.pointY = JAVA_ORIGIN_Y - pivot[1];
			bone.pointZ = pivot[2];
			bone.angleX -= parent.angleX;
			bone.angleY -= parent.angleY;
			bone.angleZ -= parent.angleZ;
		}
	}

	private static int depth(Bone bone, Map<String, Bone> byName, int limit) {
		int depth = 0;
		Bone at = bone;
		while (at != null && at.parent != null && depth <= limit) {
			at = byName.get(at.parent);
			depth++;
		}
		return depth;
	}

	/** @return true when subtracting the parent's angles is not exact; see {@link #reparent}. */
	private static boolean turnsOutside(Bone bone, Bone parent) {
		if (parent.angleZ != 0) return parent.angleY != 0 || parent.angleX != 0;
		if (parent.angleY != 0) return parent.angleX != 0 || bone.angleZ != 0;
		return bone.angleY != 0 || bone.angleZ != 0;
	}

	/**
	 * A point read back out of the parent's rest rotation. Dragonfly turns a bone by
	 * {@code Rz(-angleZ) · Ry(angleY) · Rx(-angleX)} about its pivot, so undoing that is
	 * {@code Rx(angleX) · Ry(-angleY) · Rz(angleZ)} about the same point.
	 */
	private static float[] unrotate(Bone parent, float x, float y, float z) {
		float pivotX = parent.pointX;
		float pivotY = JAVA_ORIGIN_Y - parent.pointY;
		float pivotZ = parent.pointZ;
		double[] d = {x - pivotX, y - pivotY, z - pivotZ};
		d = turnZ(d, parent.angleZ);
		d = turnY(d, -parent.angleY);
		d = turnX(d, parent.angleX);
		return new float[]{(float) (pivotX + d[0]), (float) (pivotY + d[1]), (float) (pivotZ + d[2])};
	}

	private static double[] turnX(double[] v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		return new double[]{v[0], v[1] * cos - v[2] * sin, v[1] * sin + v[2] * cos};
	}

	private static double[] turnY(double[] v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		return new double[]{v[0] * cos + v[2] * sin, v[1], -v[0] * sin + v[2] * cos};
	}

	private static double[] turnZ(double[] v, double angle) {
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		return new double[]{v[0] * cos - v[1] * sin, v[0] * sin + v[1] * cos, v[2]};
	}

	// ------------------------------------------------------------------------------------------
	// Java model space -> Bedrock geometry
	// ------------------------------------------------------------------------------------------

	static String toGeometryJson(String id, List<Bone> bones, int textureWidth, int textureHeight) {
		float reach = 0;
		float minY = Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;

		StringBuilder body = new StringBuilder();
		for (int i = 0; i < bones.size(); i++) {
			Bone bone = bones.get(i);
			float pivotX = bone.pointX;
			float pivotY = JAVA_ORIGIN_Y - bone.pointY;
			float pivotZ = bone.pointZ;

			body.append("\t\t\t\t{\n");
			body.append("\t\t\t\t\t\"name\": \"").append(bone.name).append("\",\n");
			if (bone.parent != null) body.append("\t\t\t\t\t\"parent\": \"").append(bone.parent).append("\",\n");
			body.append("\t\t\t\t\t\"pivot\": [").append(num(pivotX)).append(", ").append(num(pivotY))
				.append(", ").append(num(pivotZ)).append("]");
			if (bone.angleX != 0 || bone.angleY != 0 || bone.angleZ != 0) {
				body.append(",\n\t\t\t\t\t\"rotation\": [").append(num(Math.toDegrees(bone.angleX))).append(", ")
					.append(num(Math.toDegrees(bone.angleY))).append(", ")
					.append(num(Math.toDegrees(bone.angleZ))).append("]");
			}
			if (!bone.cubes.isEmpty()) {
				body.append(",\n\t\t\t\t\t\"cubes\": [\n");
				for (int c = 0; c < bone.cubes.size(); c++) {
					Cube cube = bone.cubes.get(c);
					float originX = pivotX + cube.offX;
					float originY = pivotY - cube.offY - cube.height;
					float originZ = pivotZ + cube.offZ;
					// A single width covers both horizontal axes, so take the furthest reach on either.
					reach = Math.max(reach, Math.abs(originX) + cube.inflate);
					reach = Math.max(reach, Math.abs(originX + cube.width) + cube.inflate);
					reach = Math.max(reach, Math.abs(originZ) + cube.inflate);
					reach = Math.max(reach, Math.abs(originZ + cube.depth) + cube.inflate);
					minY = Math.min(minY, originY - cube.inflate);
					maxY = Math.max(maxY, originY + cube.height + cube.inflate);

					body.append("\t\t\t\t\t\t{\"origin\": [").append(num(originX)).append(", ").append(num(originY))
						.append(", ").append(num(originZ)).append("], \"size\": [").append(cube.width).append(", ")
						.append(cube.height).append(", ").append(cube.depth).append("], \"uv\": [").append(cube.u)
						.append(", ").append(cube.v).append("]");
					if (cube.inflate != 0) body.append(", \"inflate\": ").append(num(cube.inflate));
					if (cube.mirror) body.append(", \"mirror\": true");
					body.append("}").append(c < bone.cubes.size() - 1 ? "," : "").append("\n");
				}
				body.append("\t\t\t\t\t]");
			}
			body.append("\n\t\t\t\t}").append(i < bones.size() - 1 ? "," : "").append("\n");
		}

		if (minY > maxY) {
			reach = 8;
			minY = 0;
			maxY = 24;
		}
		float width = reach * 2 / 16.0F;
		float height = (maxY - minY) / 16.0F;
		float offsetY = (maxY + minY) / 2 / 16.0F;

		StringBuilder json = new StringBuilder();
		json.append("{\n\t\"format_version\": \"1.12.0\",\n\t\"minecraft:geometry\": [\n\t\t{\n");
		json.append("\t\t\t\"description\": {\n");
		json.append("\t\t\t\t\"identifier\": \"geometry.").append(id).append("\",\n");
		json.append("\t\t\t\t\"texture_width\": ").append(textureWidth).append(",\n");
		json.append("\t\t\t\t\"texture_height\": ").append(textureHeight).append(",\n");
		json.append("\t\t\t\t\"visible_bounds_width\": ").append(num(width)).append(",\n");
		json.append("\t\t\t\t\"visible_bounds_height\": ").append(num(height)).append(",\n");
		json.append("\t\t\t\t\"visible_bounds_offset\": [0, ").append(num(offsetY)).append(", 0]\n");
		json.append("\t\t\t},\n\t\t\t\"bones\": [\n");
		json.append(body);
		json.append("\t\t\t]\n\t\t}\n\t]\n}\n");
		return json.toString();
	}

	/** Locale-independent, and integral values stay integral so the output reads like the hand-written models. */
	private static String num(double value) {
		if (Math.abs(value - Math.rint(value)) < 1.0E-6) return Long.toString((long) Math.rint(value));
		return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}

	private static void write(File target, byte[] bytes) throws IOException {
		File parent = target.getParentFile();
		if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("could not create " + parent);
		try (OutputStream out = new FileOutputStream(target)) {
			out.write(bytes);
		}
	}

	/** Reads a stream fully; shared with {@link MMAssetBridge}'s archive walk. */
	static byte[] readFully(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int read;
		while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
		return out.toByteArray();
	}
}
