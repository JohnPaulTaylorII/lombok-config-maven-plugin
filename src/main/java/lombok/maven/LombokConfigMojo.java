package lombok.maven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Creates a root lombok.config file.
 *
 * @author John Paul Taylor II
 * @since 1.0
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.INITIALIZE)
public class LombokConfigMojo extends GeneratedLombokConfigMojo
{
    @Component
    BuildContext buildContext;

    /**
     * Path to the lombok.config file.
     */
    @Parameter(property = "lombok.configFile", defaultValue = "${basedir}/src/lombok.config", required = true)
    File configFile;

    /**
     * <p>
     * Additional configuration lines to append to lombok.config.
     * <p>
     * Example:
     *
     * <pre>
     * &lt;configLines&gt;
     *     &lt;line&gt;lombok.experimental.feature = true&lt;/line&gt;
     * &lt;/configLines&gt;
     * </pre>
     */
    @Parameter(property = "lombok.configLines")
    List<String> configLines;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        final String contents = makeConfig();
        if (shouldWriteConfig(contents)) {
            try {
                Files.write(this.configFile.toPath(), contents.getBytes(StandardCharsets.UTF_8));
                this.buildContext.refresh(this.configFile);
                getLog().info("Updated lombok.config successfully.");
            } catch (final IOException e) {
                throw new MojoExecutionException("Could not write lombok.config file.", e);
            }
        } else {
            getLog().info("No updates to lombok.config were needed.");
        }
    }

    private boolean shouldWriteConfig(final String contents)
        throws MojoExecutionException
    {
        try {
            return !this.configFile.exists() ||
                    !new String(Files.readAllBytes(this.configFile.toPath()), StandardCharsets.UTF_8).equals(contents);
        } catch (final IOException e) {
            throw new MojoExecutionException("Could not read existing lombok.config file.", e);
        }
    }

    private String makeConfig()
    {
        final StringBuilder result = new StringBuilder();
        result.append("config.stopBubbling = true");
        result.append(makeConfigFromFields());
        result.append(makeConfigFromConfigLines());
        return result.toString();
    }

    private String makeConfigFromConfigLines()
    {
        final StringBuilder result = new StringBuilder();
        if (this.configLines != null) {
            getLog().debug("configLines: " + this.configLines);
            for (final String line : this.configLines) {
                result.append('\n').append(line.trim());
            }
        }
        return result.toString();
    }

    private String makeConfigFromFields()
    {
        final StringBuilder result = new StringBuilder();
        for (final String[] field : configuredFields()) {
            if (field[0].isBlank()) {
                result.append('\n').append(field[1]).append(" = ").append(field[2]);
            } else {
                for (final String value : field[2].split("\\s*,\\s*")) {
                    result.append('\n').append(field[1]).append(" += ").append(value);
                }
            }
        }
        final String string = result.toString();
        getLog().debug("Fields: " + string);
        return string;
    }

    private List<String[]> configuredFields()
    {
        final List<String[]> result = new ArrayList<>();
        for (final Field f : GeneratedLombokConfigMojo.class.getDeclaredFields()) {
            try {
                final Config c = f.getAnnotation(Config.class);
                if (c != null) {
                    final String rawValue = (String)f.get(this);
                    if (rawValue != null && !rawValue.matches("^[\\s,]*$")) {
                        result.add(new String[] {
                                c.list(), // is list if not blank
                                c.value(), // configuration name
                                rawValue // configured value
                        });
                    }
                }
            } catch (final Exception e) {
                //Just ignore it
            }
        }
        return result;
    }

}
