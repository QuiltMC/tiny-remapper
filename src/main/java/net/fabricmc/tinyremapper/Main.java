/*
 * Copyright (C) 2016, 2018 Player, asie
 * Copyright (C) 2021 QuiltMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fabricmc.tinyremapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.constraints.Min;

import net.fabricmc.tinyremapper.TinyRemapper.LinkedMethodPropagation;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;


/* @formatter:off - eclipse formatter doesn't seem to like annotations */
@Command(name = "tiny-remapper", mixinStandardHelpOptions = true, version = {
    "Tiny Remapper " + Main.version == null ? "DEVELOP" : Main.version,
    "Picocli " + picocli.CommandLine.VERSION,
    "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
    "OS: ${os.name} ${os.version} ${os.arch}"},
    description = "A tool for remapping JAR files using \"Tiny\"-format mappings",
    usageHelpAutoWidth = true)
public class Main implements Callable<Integer> {
    /* @formatter:on */
    @Spec
    CommandSpec spec; // injected by picocli

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "Display version info")
    boolean versionInfoRequested;
    public static String version = getClass().getPackage().getImplementationVersion();

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    boolean usageHelpRequested;

    @Option(names = "--reverse",
            description = "Reverse the mapping. @|bold,underline,yellow NOT YET IMPLEMENTED!|@")
    boolean reverse = false;

    @Option(names = "--ignore-field-desc",
            description = "Ignore the field descriptions in mappings.")
    boolean ignoreFieldDesc = false;

    Set<String> forcePropagation = Collections.emptySet();

    /**
     * Set the forcePropagation variable and validate.
     *
     * @param value input file from the user
     */
    @Option(names = "--force-propagation",
            description = "A file with methods to force propagation to.")
    public void setForcePropagation(String value) {
        File forcePropagationFile = new File(value);

        if (forcePropagationFile != null) {
            forcePropagation = new HashSet<>();

            if (!forcePropagationFile.canRead()) {
                throw new ParameterException(spec.commandLine(),
                        "Cannot read forcePropagation file " + forcePropagationFile + ".");
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(forcePropagationFile))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty() || line.charAt(0) == '#') {
                        continue;
                    }

                    forcePropagation.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new ParameterException(spec.commandLine(),
                        "An error occurred while parsing the forcePropagation file.");
            }
        }
    }

    @Option(names = "--propagate-private",
            description = "Propagate mappings to private methods.")
    boolean propagatePrivate = false;

    LinkedMethodPropagation propagateBridges = LinkedMethodPropagation.DISABLED;

    /**
     * Set the propagateBridges variable and validate.
     *
     * @param value input from the user
     */
    @Option(names = "propagate-bridges",
            description = "Propagate methods to bridge methods. "
                    + "Must be one of \"disabled\", \"enabled\", or \"compatible\".")
    public void setPropagateBridges(String value) {
        switch (value.toLowerCase(Locale.ENGLISH)) {
            case "disabled":
                propagateBridges = LinkedMethodPropagation.DISABLED;
                break;
            case "enabled":
                propagateBridges = LinkedMethodPropagation.ENABLED;
                break;
            case "compatible":
                propagateBridges = LinkedMethodPropagation.COMPATIBLE;
                break;
            default:
                throw new ParameterException(spec.commandLine(),
                        "Invalid propagateBridges value: " + value);
        }
    }

    @Option(names = "--remove-frames",
            description = "Ignore the StackMap and StackMapTable frames.")
    boolean removeFrames = false;

    @Option(names = "--ignore-conflicts",
            description = "Ignore any mapping conflicts.")
    boolean ignoreConflicts = false;

    @Option(names = "--check-package-access",
            description = "Check package access.")
    boolean checkPackageAccess = false;

    @Option(names = "--fix-package-access",
            description = "Fix package access. Implies \"--fix-package-access\".")
    boolean fixPackageAccess = false;

    @Option(names = "--resolve-missing", description = "Resolve missing methods.")
    boolean resolveMissing = false;

    @Option(names = "--rebuild-source-filenames",
            description = "Rebuild the filenames of sources.")
    boolean rebuildSourceFilenames = false;

    @Option(names = "--skip-local-variable-mapping", description = "Skip remapping local variables")
    boolean skipLocalVariableMapping = false;

    @Option(names = "--rename-invalid-locals", description = "Rename invalid local variables.")
    boolean renameInvalidLocals = false;

    NonClassCopyMode ncCopyMode = NonClassCopyMode.FIX_META_INF;

    @Option(names = "--non-class-copy-mode",
            description = "How to deal with non-class files in a JAR. "
                    + "Must be one of \"unchanged\", \"fixmeta\", or \"skipmeta\".")
    private void setNonClassCopyMode(String value) {
        switch (value.toLowerCase(Locale.ENGLISH)) {
            case "unchanged":
                ncCopyMode = NonClassCopyMode.UNCHANGED;
                break;
            case "fixmeta":
                ncCopyMode = NonClassCopyMode.FIX_META_INF;
                break;
            case "skipmeta":
                ncCopyMode = NonClassCopyMode.SKIP_META_INF;
                break;
            default:
                throw new ParameterException(spec.commandLine(),
                        "Invalid nonClassCopyMode value: " + value);
        }
    }

    @Min(value = 1, message = "Thread count must be greater than 0.")
    @Option(names = "--threads",
            description = "Number of threads to use while remapping. "
                    + "Defaults to the number of CPU cores available.")
    int threads = -1;

    public static void main(String[] rawArgs) {
        List<String> args = new ArrayList<String>(rawArgs.length);

        // for (String arg : rawArgs) {
        // if (arg.startsWith("--")) {
        // int valueSepPos = arg.indexOf('=');

        // String argKey =
        // valueSepPos == -1 ? arg.substring(2) : arg.substring(2, valueSepPos);
        // argKey = argKey.toLowerCase(Locale.ROOT);

        // switch (argKey.toLowerCase()) {
        // case "reverse":
        // System.err.println("WARNING: --reverse is not currently implemented!");
        // reverse = true;
        // break;
        // case "ignorefielddesc":
        // ignoreFieldDesc = true;
        // break;
        // case "forcepropagation":
        // forcePropagationFile = new File(arg.substring(valueSepPos + 1));
        // break;
        // case "propagateprivate":
        // propagatePrivate = true;
        // break;
        // case "propagatebridges":
        // switch (arg.substring(valueSepPos + 1).toLowerCase(Locale.ENGLISH)) {
        // case "disabled":
        // propagateBridges = LinkedMethodPropagation.DISABLED;
        // break;
        // case "enabled":
        // propagateBridges = LinkedMethodPropagation.ENABLED;
        // break;
        // case "compatible":
        // propagateBridges = LinkedMethodPropagation.COMPATIBLE;
        // break;
        // default:
        // System.out.println("invalid propagateBridges: "
        // + arg.substring(valueSepPos + 1));
        // System.exit(1);
        // }
        // break;
        // case "removeframes":
        // removeFrames = true;
        // break;
        // case "ignoreconflicts":
        // ignoreConflicts = true;
        // break;
        // case "checkpackageaccess":
        // checkPackageAccess = true;
        // break;
        // case "fixpackageaccess":
        // fixPackageAccess = true;
        // break;
        // case "resolvemissing":
        // resolveMissing = true;
        // break;
        // case "rebuildsourcefilenames":
        // rebuildSourceFilenames = true;
        // break;
        // case "skiplocalvariablemapping":
        // skipLocalVariableMapping = true;
        // break;
        // case "renameinvalidlocals":
        // renameInvalidLocals = true;
        // break;
        // case "nonclasscopymode":
        // switch (arg.substring(valueSepPos + 1).toLowerCase(Locale.ENGLISH)) {
        // case "unchanged":
        // ncCopyMode = NonClassCopyMode.UNCHANGED;
        // break;
        // case "fixmeta":
        // ncCopyMode = NonClassCopyMode.FIX_META_INF;
        // break;
        // case "skipmeta":
        // ncCopyMode = NonClassCopyMode.SKIP_META_INF;
        // break;
        // default:
        // System.out.println("invalid nonClassCopyMode: "
        // + arg.substring(valueSepPos + 1));
        // System.exit(1);
        // }
        // break;
        // case "threads":
        // threads = Integer.parseInt(arg.substring(valueSepPos + 1));
        // if (threads <= 0) {
        // System.out.println("Thread count must be > 0");
        // System.exit(1);
        // }
        // break;
        // default:
        // System.out.println("invalid argument: " + arg + ".");
        // System.exit(1);
        // }
        // } else {
        // args.add(arg);
        // }
        // }

        if (args.size() < 5) {
            System.out.println(
                    "usage: <input> <output> <mappings> <from> <to> [<classpath>]... [--reverse] [--forcePropagation=<file>] [--propagatePrivate] [--ignoreConflicts]");
            System.exit(1);
        }

        Path input = Paths.get(args.get(0));
        if (!Files.isReadable(input)) {
            System.out.println("Can't read input file " + input + ".");
            System.exit(1);
        }

        Path output = Paths.get(args.get(1));

        Path mappings = Paths.get(args.get(2));
        if (!Files.isReadable(mappings) || Files.isDirectory(mappings)) {
            System.out.println("Can't read mappings file " + mappings + ".");
            System.exit(1);
        }

        String fromM = args.get(3);
        String toM = args.get(4);

        Path[] classpath = new Path[args.size() - 5];

        for (int i = 0; i < classpath.length; i++) {
            classpath[i] = Paths.get(args.get(i + 5));
            if (!Files.isReadable(classpath[i])) {
                System.out.println("Can't read classpath file " + i + ": " + classpath[i] + ".");
                System.exit(1);
            }
        }

        // if (forcePropagationFile != null) {
        // forcePropagation = new HashSet<>();

        // if (!forcePropagationFile.canRead()) {
        // System.out
        // .println("Can't read forcePropagation file " + forcePropagationFile + ".");
        // System.exit(1);
        // }

        // try (BufferedReader reader = new BufferedReader(new FileReader(forcePropagationFile))) {
        // String line;

        // while ((line = reader.readLine()) != null) {
        // line = line.trim();

        // if (line.isEmpty() || line.charAt(0) == '#') {
        // continue;
        // }

        // forcePropagation.add(line);
        // }
        // } catch (IOException e) {
        // e.printStackTrace();
        // System.exit(1);
        // }
        // }

        long startTime = System.nanoTime();

        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(TinyUtils.createTinyMappingProvider(mappings, fromM, toM))
                .ignoreFieldDesc(ignoreFieldDesc).withForcedPropagation(forcePropagation)
                .propagatePrivate(propagatePrivate).propagateBridges(propagateBridges)
                .removeFrames(removeFrames)
                .ignoreConflicts(ignoreConflicts).checkPackageAccess(checkPackageAccess)
                .fixPackageAccess(fixPackageAccess).resolveMissing(resolveMissing)
                .rebuildSourceFilenames(rebuildSourceFilenames)
                .skipLocalVariableMapping(skipLocalVariableMapping)
                .renameInvalidLocals(renameInvalidLocals).threads(threads).build();

        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(output).build()) {
            outputConsumer.addNonClassFiles(input, ncCopyMode, remapper);

            remapper.readInputs(input);
            remapper.readClassPath(classpath);

            remapper.apply(outputConsumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            remapper.finish();
        }

        System.out.printf("Finished after %.2f ms.\n", (System.nanoTime() - startTime) / 1e6);
    }
}
