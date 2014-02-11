package com.github.ferstl.depgraph;

import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import com.github.ferstl.depgraph.dot.DotBuilder;


class AggregatingDotGraphFactory implements GraphFactory {
  private final DependencyGraphBuilder dependencyGraphBuilder;
  private final ArtifactFilter artifactFilter;
  private final DotBuilder dotBuilder;

  public AggregatingDotGraphFactory(
      DependencyGraphBuilder dependencyGraphBuilder, ArtifactFilter artifactFilter, DotBuilder dotBuilder) {

    this.dependencyGraphBuilder = dependencyGraphBuilder;
    this.artifactFilter = artifactFilter;
    this.dotBuilder = dotBuilder;
  }

  @Override
  public String createGraph(MavenProject project) throws DependencyGraphBuilderException {
    @SuppressWarnings("unchecked")
    List<MavenProject> collectedProjects = project.getCollectedProjects();
    buildModuleTree(project, this.artifactFilter, this.dotBuilder);

    for (MavenProject collectedProject : collectedProjects) {
      // Process project only if its artifact is not filtered
      if (this.artifactFilter.include(collectedProject.getArtifact())) {
        DependencyNode root = this.dependencyGraphBuilder.buildDependencyGraph(collectedProject, this.artifactFilter);

        DotBuildingVisitor visitor = new DotBuildingVisitor(this.dotBuilder);
        root.accept(visitor);
      }
    }

    return this.dotBuilder.toString();
  }

  private void buildModuleTree(MavenProject rootProject, ArtifactFilter filter, DotBuilder dotBuilder) {
    @SuppressWarnings("unchecked")
    Collection<MavenProject> collectedProjects = rootProject.getCollectedProjects();
    for (MavenProject collectedProject : collectedProjects) {
      MavenProject child = collectedProject;
      MavenProject parent = collectedProject.getParent();

      while (parent != null) {
        ArtifactNode parentNode = filterProject(parent, filter);
        ArtifactNode childNode = filterProject(child, filter);

        dotBuilder.addEdge(parentNode, childNode);

        // Stop if we reached this project!
        if (parent.equals(rootProject)) {
          break;
        }

        child = parent;
        parent = parent.getParent();
      }
    }
  }

  private ArtifactNode filterProject(MavenProject project, ArtifactFilter filter) {
    Artifact artifact = project.getArtifact();
    if (filter.include(artifact)) {
      return new ArtifactNode(artifact);
    }

    return null;
  }

}
