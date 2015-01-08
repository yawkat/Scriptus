package net.md_5.scriptus;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import java.io.File;
import java.util.Iterator;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Mojo to set a user defined property to the current commit hash, in a user
 * defined format.
 */
@Mojo(name = "describe", defaultPhase = LifecyclePhase.INITIALIZE)
public class DescribeMojo extends AbstractMojo
{

    /**
     * Maven project we are invoking.
     */
    @Parameter(property = "project", readonly = true)
    private MavenProject project;
    /**
     * Format used to set describe property. This first string argument will be
     * the abbreviated HEAD commit hash.
     */
    @Parameter(defaultValue = "git-${project.name}-%s")
    private String format;
    /**
     * The property to set, useful it we want to stack describes.
     */
    @Parameter(defaultValue = "describe")
    private String descriptionProperty;
    /**
     * Directory of the .git folder. Normally the current directory will be
     * sufficient.
     */
    @Parameter(property = "maven.changeSet.scmDirectory")
    private File scmDirectory;
    /**
     * Format to use if we fail to get the Git info.
     */
    @Parameter(defaultValue = "unknown")
    private String failFormat;
    /**
     * Whether or not to fail the build when we cannot get info.
     */
    @Parameter(defaultValue = "false")
    private boolean fail;

    @SuppressWarnings("UseSpecificCatch")
    public void execute() throws MojoExecutionException
    {
        try
        {
            Git git = Git.open( scmDirectory );

            try
            {
                Iterator<RevCommit> log = git.log().setMaxCount( 1 ).call().iterator();

                if ( log.hasNext() )
                {
                    String hash = git.getRepository().newObjectReader().abbreviate( log.next() ).name();
                    String formatted = String.format( format, hash );

                    project.getProperties().put( descriptionProperty, formatted );
                    getLog().info( String.format( "Set property \"%s\" to \"%s\"", descriptionProperty, formatted ) );
                }
            } finally
            {
                git.close();
            }
        } catch ( Exception ex )
        {
            if ( fail )
            {
                throw new MojoExecutionException( "Exception reading Git repo", ex );
            }
            getLog().warn( "Failed to get HEAD commit hash: " + ex.getClass().getName() + ":" + ex.getMessage() );

            project.getProperties().put( descriptionProperty, failFormat );
        }
    }
}