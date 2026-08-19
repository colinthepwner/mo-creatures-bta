package teamport.creatures.core;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
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

public final class MMGeometryBridge {
	private MMGeometryBridge() {}

	private static final String MANIFEST = "/assets/creatures/model-bridge.properties";

	private static final float JAVA_ORIGIN_Y = 24.0F;

	public static int convertedCount = -1;
	public static int composedCount = -1;
	public static int skippedCount = -1;
	public static List<String> problems = new ArrayList<>();

	static final class ModelEntry {
		String id;
		String sourceClass;

		String overlayClass;
		String base;

		String vanilla;

		final Map<String, List<String>> textures = new LinkedHashMap<>();

		int uvWidth = 64;
		int uvHeight = 32;

		double[] args = new double[0];

		final Map<String, String> renames = new LinkedHashMap<>();

		final Map<String, String> parents = new LinkedHashMap<>();

		final Set<String> mirrorBones = new LinkedHashSet<>();
	}

	static final class Manifest {
		final List<ModelEntry> models = new ArrayList<>();

		final Map<String, String[]> baseSlots = new HashMap<>();

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

	public static List<String> wantedEntries() {
		List<String> wanted = new ArrayList<>();
		for (ModelEntry entry : manifest().models) {

			if (entry.sourceClass != null) wanted.add(entry.sourceClass.toLowerCase(Locale.ROOT) + ".class");
			if (entry.overlayClass != null) wanted.add(entry.overlayClass.toLowerCase(Locale.ROOT) + ".class");
			for (String texture : entry.textures.keySet()) wanted.add(texture.toLowerCase(Locale.ROOT));
		}
		return wanted;
	}

	public static final class Result {

		public final List<String> converted = new ArrayList<>();

		public final List<String> composed = new ArrayList<>();

		public final List<String> skipped = new ArrayList<>();
		public final List<String> problems = new ArrayList<>();
	}

	public static Result run(Map<String, byte[]> archive, File packDir) {
		Result result = new Result();
		Manifest m = manifest();
		if (m.models.isEmpty()) {
			result.problems.add("model manifest " + MANIFEST + " is empty or missing");
			publish(result);
			return result;
		}

		for (ModelEntry entry : m.models) {

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
			mirrorRightHandSide(bones);

			String json = toGeometryJson(entry.id, bones, entry.uvWidth, entry.uvHeight);
			try {

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

	private static final List<String> BUILTIN_MODEL_IDS = Arrays.asList(
		"bear", "bird", "boar", "bunny", "deer_moc", "fox",
		"horse", "horse_pegasus", "horse_unicorn", "kitty", "litterbox");

	private static void publish(Result result) {
		convertedCount = result.converted.size();
		composedCount = result.composed.size();
		skippedCount = result.skipped.size();
		problems = result.problems;
	}

	static final class Cube {
		float offX, offY, offZ;
		int width, height, depth;
		float inflate;
		int u, v;
		boolean mirror;
	}

	static final class Bone {

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

		double[] superArgs = new double[0];
		final List<String> problems = new ArrayList<>();
	}

	private static final Object THIS = new Object();

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

		private boolean pose;

		private AbstractInsnNode jumped;

		private static final double[] REST_ARGUMENTS = {0, 0, 0, 0, 0, 0, 0};

		Interpreter(ClassNode owner, Map<String, MethodNode> ctors, Manifest manifest, Extraction out) {
			this.owner = owner;
			this.ctors = ctors;
			this.manifest = manifest;
			this.out = out;
		}

		void readRestPose(ClassNode node) {
			pose = true;
			try {
				for (MethodNode method : node.methods) {
					if (!isPoseMethod(method)) continue;
					depth = 0;

					run(method, REST_ARGUMENTS);
				}
			} catch (RuntimeException e) {

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

		private static boolean isForward(AbstractInsnNode from, AbstractInsnNode target) {
			for (AbstractInsnNode at = from.getNext(); at != null; at = at.getNext()) {
				if (at == target) return true;
			}
			return false;
		}

		private boolean branch(AbstractInsnNode insn, Boolean taken) {
			if (taken == null) {

				if (!pose) {
					out.problems.add("constructor branches on something this cannot evaluate");
					return false;
				}

				taken = isForward(insn, ((org.objectweb.asm.tree.JumpInsnNode) insn).label);
			}
			if (!taken) return true;
			AbstractInsnNode label = ((org.objectweb.asm.tree.JumpInsnNode) insn).label;

			if (!isForward(insn, label) && !pose) {
				out.problems.add("constructor jumps backwards, which this cannot follow");
				return false;
			}
			jumped = label;
			return true;
		}

		private static Boolean decide(int op, Object left, Object right) {
			if (op == Opcodes.IFNULL || op == Opcodes.IFNONNULL) {

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

		private static Object defaultValue(String descriptor) {
			return switch (descriptor) {
				case "Z", "B", "C", "S", "I", "J" -> 0.0;
				default -> null;
			};
		}

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

		private void movePoint(Bone bone, double[] args) {
			if (args.length < 3) return;
			if (!Double.isNaN(args[0])) bone.pointX = (float) args[0];
			if (!Double.isNaN(args[1])) bone.pointY = (float) args[1];
			if (!Double.isNaN(args[2])) bone.pointZ = (float) args[2];
		}

		private boolean step(AbstractInsnNode insn, List<Object> stack, Object[] locals) {
			int op = insn.getOpcode();
			switch (op) {
				case -1 -> {
					return true;
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
				case Opcodes.IINC -> {

					IincInsnNode increment = (IincInsnNode) insn;
					Object current = locals[increment.var];
					locals[increment.var] = current instanceof Double d ? (Object) (d + increment.incr) : null;
				}
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
					if (pose) {

						if (target instanceof Bone bone && "(FFF)V".equals(call.desc)) movePoint(bone, args);
						return true;
					}
					if (!(target instanceof Bone bone)) return true;
					switch (call.desc) {

						case "(FFFIIIF)V" -> addBox(bone, args, (float) args[6]);
						case "(FFFIII)V" -> addBox(bone, args, 0.0F);
						case "(FFF)V" -> {
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

	private record PendingNew(String type) {}

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

	private static List<Bone> vanillaModel(String id) {
		return switch (id) {
			case "pig" -> pigBase();
			case "chicken" -> chickenBase();
			default -> null;
		};
	}

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

	private static void mirrorRightHandSide(List<Bone> bones) {
		for (Bone bone : bones) {
			if (bone.cubes.isEmpty()) continue;
			String partnerName = counterpartName(bone.name);
			if (partnerName == null) continue;
			for (Bone partner : bones) {
				if (partner == bone || partner.cubes.isEmpty()) continue;
				if (!partnerName.equals(partner.name)) continue;

				Cube a = bone.cubes.get(0);
				Cube b = partner.cubes.get(0);
				if (a.u != b.u || a.v != b.v) continue;
				if (a.width != b.width || a.height != b.height || a.depth != b.depth) continue;

				double mine = midX(bone);
				double theirs = midX(partner);

				if (Math.abs(mine + theirs) > 0.51 || mine <= 0.0) continue;

				for (Cube cube : bone.cubes) {
					cube.mirror = true;
				}
			}
		}
	}

	private static String counterpartName(String name) {
		if (name == null) return null;
		if (name.contains("Left")) return name.replace("Left", "Right");
		if (name.contains("Right")) return name.replace("Right", "Left");

		if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
			if (name.charAt(0) == 'L') return 'R' + name.substring(1);
			if (name.charAt(0) == 'R') return 'L' + name.substring(1);
		}
		return null;
	}

	private static double midX(Bone bone) {
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for (Cube cube : bone.cubes) {
			double left = bone.pointX + cube.offX;
			min = Math.min(min, left);
			max = Math.max(max, left + cube.width);
		}
		return (min + max) / 2.0;
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

			if (entry.mirrorBones.contains(name)) {
				for (Cube cube : bone.cubes) {
					cube.mirror = true;
				}
			}
			kept.add(bone);
		}
		return kept;
	}

	private static void reparent(List<Bone> bones, Result result, String modelId) {
		Map<String, Bone> byName = new LinkedHashMap<>();
		for (Bone bone : bones) byName.put(bone.name, bone);

		List<Bone> children = new ArrayList<>();
		for (Bone bone : bones) {
			if (bone.parent != null) children.add(bone);
		}

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

	private static boolean turnsOutside(Bone bone, Bone parent) {
		if (parent.angleZ != 0) return parent.angleY != 0 || parent.angleX != 0;
		if (parent.angleY != 0) return parent.angleX != 0 || bone.angleZ != 0;
		return bone.angleY != 0 || bone.angleZ != 0;
	}

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

	static byte[] readFully(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int read;
		while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
		return out.toByteArray();
	}
}
