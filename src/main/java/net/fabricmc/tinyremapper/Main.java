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

import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyRemapper.LinkedMethodPropagation;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.jansi.graalvm.AnsiConsole;

/* @formatter:off - eclipse formatter doesn't seem to like annotations */
@Command(name = "tiny-remapper", mixinStandardHelpOptions = true, version = {
    "@|bold,underline Tiny Remapper v" + TinyRemapper.VERSION + "|@",
    "ASM v" + "9.1",
    "Picocli v" + picocli.CommandLine.VERSION,
    "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
    "OS: ${os.name} ${os.version} ${os.arch}"},
    description = "A tool for remapping JAR files using \"Tiny\"-format mappings.",
    footer = {"%nCopyright (C) 2016, 2018 Player, asie; 2021 QuiltMC",
        "Report bugs at <https://github.com/QuiltMC/tiny-remapper/issues>."},
    usageHelpAutoWidth = true, abbreviateSynopsis = true)
public class Main implements Callable<Integer> {
    /* @formatter:on */

    /*
     * ================== PicoCLI stuff =====================
     */
    @Spec
    private CommandSpec spec; // injected by picocli

    /*
     * ================== Input parameters ==================
     */

    // Input file
    private Path input;

    /**
     * Set the input path variable and validate.
     *
     * @param value input path from the user
     */
    @Parameters(index = "0", description = "Path to input file to remap.")
    private void setInput(Path value) {
        if (!Files.isReadable(input)) {
            throw new ParameterException(spec.commandLine(),
                    "Cannot read input file " + value + ".");
        }
        input = value;
    }

    @Parameters(index = "1", description = "Path to output remapped file.")
    private Path output;

    // Mappings file
    private Path mappings;

    /**
     * Set the mappings path variable and validate.
     *
     * @param value input path from the user
     */
    @Parameters(index = "2", description = "Path to mappings file.")
    private void setMappings(Path value) {
        if (!Files.isReadable(input)) {
            throw new ParameterException(spec.commandLine(),
                    "Cannot read mappings file " + value + ".");
        }
        mappings = value;
    }

    @Parameters(index = "3", description = "Namespace to map from.")
    private String fromMapping;

    @Parameters(index = "4", description = "Namespace to map to.")
    private String toMapping;

    // Classpath
    private Path[] classpath;

    /**
     * Set the classpath variable and validate.
     *
     * @param value input paths from the user
     */
    @Parameters(index = "5..*", description = "Additional files to add to the classpath.")
    private void setClasspath(Path[] value) {
        classpath = new Path[value.length];
        for (int i = 0; i < value.length; i++) {
            classpath[i] = value[i];
            if (!Files.isReadable(value[i])) {
                throw new ParameterException(spec.commandLine(),
                        "Cannot read classpath file " + value[i] + ".");
            }
        }
    }

    /*
     * ================== Options and switches ==============
     */
    @Option(names = {"-R", "--reverse"},
            description = "Reverse the mapping. @|bold,underline,yellow NOT YET IMPLEMENTED!|@")
    private boolean reverse;

    @Option(names = {"-i", "--ignore-field-desc"},
            description = "Ignore the field descriptions in mappings.")
    private boolean ignoreFieldDesc;

    // Force propagation option
    private Set<String> forcePropagation = Collections.emptySet();

    /**
     * Set the forcePropagation variable and validate.
     *
     * @param forcePropagationFile input file from the user
     */
    @Option(names = {"-f", "--force-propagation"},
            description = "A file with methods to force propagation to.")
    private void setForcePropagation(File forcePropagationFile) {
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

    @Option(names = {"-p", "--propagate-private"},
            description = "Propagate mappings to private methods.")
    private boolean propagatePrivate;

    // Propagate bridges option
    private LinkedMethodPropagation propagateBridges = LinkedMethodPropagation.DISABLED;

    /**
     * Set the propagateBridges variable and validate.
     *
     * @param value input from the user
     */
    @Option(names = {"-b", "--propagate-bridges"},
            description = "Propagate methods to bridge methods. "
                    + "Must be one of \"disabled\", \"enabled\", or \"compatible\".")
    private void setPropagateBridges(String value) {
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
    private boolean removeFrames;

    @Option(names = {"-I", "--ignore-conflicts"},
            description = "Ignore any mapping conflicts.")
    private boolean ignoreConflicts;

    @Option(names = {"-C", "--check-package-access"},
            description = "Check package access.")
    private boolean checkPackageAccess;

    @Option(names = {"-F", "--fix-package-access"},
            description = "Fix package access. Implies \"--check-package-access\".")
    private boolean fixPackageAccess;

    @Option(names = {"-m", "--resolve-missing"}, description = "Resolve missing methods.")
    private boolean resolveMissing;

    @Option(names = {"-r", "--rebuild-source-filenames"},
            description = "Rebuild the filenames of sources.")
    private boolean rebuildSourceFilenames;

    @Option(names = {"-l", "--skip-local-variable-mapping"},
            description = "Skip remapping local variables")
    private boolean skipLocalVariableMapping;

    @Option(names = {"-L", "--rename-invalid-locals"},
            description = "Rename invalid local variables.")
    private boolean renameInvalidLocals;

    // Non-class file copy mode option
    private NonClassCopyMode ncCopyMode = NonClassCopyMode.FIX_META_INF;

    /**
     * Set the setNonClassCopyMode variable and validate.
     *
     * @param value input from the user
     */
    @Option(names = {"-M", "--non-class-copy-mode"},
            description = "How to deal with non-class files in a JAR (i.e. META-INF). "
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

    // Threads option
    private int threads = -1;

    // @Min(value = 1, message = "Thread count must be greater than 0.")
    @Option(names = {"-t", "--threads"},
            description = "Number of threads to use while remapping. "
                    + "Defaults to the number of CPU cores available.")
    private void setThreads(int input) {
        if (input <= 0) {
            throw new ParameterException(spec.commandLine(), "Threads must be greater than 0.");
        }
        threads = input;
    }

    /**
     * The main function of the CLI.
     *
     * @return exit code
     */
    public Integer call() throws Exception {
        long startTime = System.nanoTime();

        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(TinyUtils.createTinyMappingProvider(mappings, fromMapping, toMapping))
                .ignoreFieldDesc(ignoreFieldDesc)
                .withForcedPropagation(forcePropagation)
                .propagatePrivate(propagatePrivate)
                .propagateBridges(propagateBridges)
                .removeFrames(removeFrames)
                .ignoreConflicts(ignoreConflicts)
                .checkPackageAccess(checkPackageAccess)
                .fixPackageAccess(fixPackageAccess)
                .resolveMissing(resolveMissing)
                .rebuildSourceFilenames(rebuildSourceFilenames)
                .skipLocalVariableMapping(skipLocalVariableMapping)
                .renameInvalidLocals(renameInvalidLocals)
                .threads(threads)
                .build();

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
        return 0;
    }

    /**
     * Main runner function.
     *
     * @param args args from the user
     */
    public static void main(String... args) {
        int exitCode;
        try (AnsiConsole ansi = AnsiConsole.windowsInstall()) {
            exitCode = new CommandLine(new Main()).execute(args);
        }
        System.exit(exitCode);
    }
}
