package lombok.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Delete generated lombok.config file.
 *
 * @author immens
 * @since 1.9
 */
@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN, threadSafe = true)
public class LombokCleanMojo extends LombokConfigMojo
{

    @Override
    public void execute()
            throws MojoExecutionException
    {
        try {
            final Path path = this.configFile.toPath();
            if (Files.exists(path)) {
                Files.delete(path);
                getLog().info("lombok.config successfully deleted.");
            }
        } catch (final IOException e) {
            throw new MojoExecutionException("Could not delete lombok.config file.", e);
        }
    }
}
