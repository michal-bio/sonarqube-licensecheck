package at.porscheinformatik.sonarqube.licensecheck.golang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import at.porscheinformatik.sonarqube.licensecheck.Dependency;
import at.porscheinformatik.sonarqube.licensecheck.LicenseCheckRulesDefinition;
import at.porscheinformatik.sonarqube.licensecheck.Scanner;
import at.porscheinformatik.sonarqube.licensecheck.licensemapping.LicenseMappingService;

public class GoModDependencyScanner implements Scanner
{
    private static final Logger LOGGER = Loggers.get(GoModDependencyScanner.class);
    private final LicenseMappingService licenseMappingService;

    public GoModDependencyScanner(LicenseMappingService licenseMappingService)
    {
        this.licenseMappingService = licenseMappingService;
    }

    @Override
    public Set<Dependency> scan(SensorContext context)
    {
        FileSystem fs = context.fileSystem();
        FilePredicate goModPredicate = fs.predicates().matchesPathPattern("**/go.mod");

        Set<Dependency> allDependencies = new HashSet<>();

        LOGGER.info("Scanning for Golang dependencies (dir={})", fs.baseDir());
        for (InputFile goModFile : fs.inputFiles(goModPredicate))
        {
            context.markForPublishing(goModFile);
            LOGGER.info("Scanning go.mod: (path={})", goModFile);
            allDependencies.addAll(dependencyParser(fs.baseDir(), goModFile));
        }

        return allDependencies;
    }

    private Set<Dependency> dependencyParser(File baseDir, InputFile goModFile)
    {
        Set<Dependency> dependencies = new HashSet<>();
        try (java.util.Scanner scanner = new java.util.Scanner(new FileInputStream(goModFile.file())))
        {
            while (scanner.hasNextLine())
            {
                String line = scanner.nextLine().trim();
                if (line.startsWith("require"))
                {
                    String[] parts = line.split(" ");
                    if (parts.length >= 3)
                    {
                        String packageName = parts[1];
                        String version = parts[2];
                        String license = ""; // You'll likely need to add your own logic for fetching the license

                        license = licenseMappingService.mapLicense(license);

                        dependencies.add(new Dependency(packageName, version, license, LicenseCheckRulesDefinition.LANG_GO));
                    }
                }
            }
        }
        catch (FileNotFoundException e)
        {
            LOGGER.error("Could not load go.mod", e);
        }
        return dependencies;
    }
}
