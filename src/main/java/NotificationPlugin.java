import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NotificationPlugin implements ProjectComponent {

    private final Project project;
    private static final String GRADLE_BUILD_FILE_NAME = "build.gradle";
    private static final String MAVEN_BUILD_FILE_NAME = "pom.xml";

    public NotificationPlugin(Project project) {
        this.project = project;
    }

    private enum BuildTool {
        MAVEN {
            public String getText() {
                return "This is Maven project";
            }

            public Icon getIcon() {
                return new ImageIcon(NotificationPlugin.class.getResource("icons/maven.png"));
            }

            @Override
            public List<String> getBuildToolFiles() {
                return Collections.singletonList(MAVEN_BUILD_FILE_NAME);
            }
        },
        GRADLE {
            public String getText() {
                return "This is Gradle project";
            }

            public Icon getIcon() {
                return new ImageIcon(NotificationPlugin.class.getResource("icons/gradle.png"));
            }

            @Override
            public List<String> getBuildToolFiles() {
                return Collections.singletonList(GRADLE_BUILD_FILE_NAME);
            }
        },
        MAVEN_OR_GRADLE {
            public String getText() {
                return "This is Maven or Gradle project";
            }

            public Icon getIcon() {
                return new ImageIcon(NotificationPlugin.class.getResource("icons/unknown.png"));
            }

            @Override
            public List<String> getBuildToolFiles() {
                return Arrays.asList(GRADLE_BUILD_FILE_NAME, MAVEN_BUILD_FILE_NAME);
            }
        },
        UNKNOWN {
            public String getText() {
                return "This is unknown project";
            }

            public Icon getIcon() {
                return new ImageIcon(NotificationPlugin.class.getResource("icons/unknown.png"));
            }

            @Override
            public List<String> getBuildToolFiles() {
                return Collections.emptyList();
            }
        };

        public abstract Icon getIcon();

        public abstract String getText();

        public abstract List<String> getBuildToolFiles();
    }

    @Override
    public void projectOpened() {

        Path path = Paths.get(Objects.requireNonNull(project.getBasePath()));
        BuildTool buildTool;

        boolean gradleBuildFileExists = Files.isRegularFile(path.resolve(GRADLE_BUILD_FILE_NAME));
        boolean mavenBuildFileExists = Files.isRegularFile(path.resolve(MAVEN_BUILD_FILE_NAME));
        if (gradleBuildFileExists) {
            if (mavenBuildFileExists) {
                buildTool = BuildTool.MAVEN_OR_GRADLE;
            } else {
                buildTool = BuildTool.GRADLE;
            }
        } else if (mavenBuildFileExists) {
            buildTool = BuildTool.MAVEN;
        } else {
            buildTool = BuildTool.UNKNOWN;
        }
        Notification notification = new Notification("ProjectOpenNotification",
                buildTool.getIcon(),
                NotificationType.INFORMATION);
        notification.setTitle("Project Build Tool");
        StringBuilder content = new StringBuilder(buildTool.getText());
        if (buildTool.equals(BuildTool.GRADLE) || buildTool.equals(BuildTool.MAVEN)) {
            content.append("<br><a href=\"URL\">")
                   .append(buildTool.getBuildToolFiles().get(0))
                   .append("</a>");
            notification.setListener(
                    (n, hyperlinkEvent) -> {
                        if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project,
                                    Objects.requireNonNull(LocalFileSystem.getInstance()
                                                                          .refreshAndFindFileByPath(path.resolve(buildTool
                                                                                  .getBuildToolFiles().get(0))
                                                                                                        .toString())));
                            FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
                        }
                    });
        } else if (buildTool.equals(BuildTool.MAVEN_OR_GRADLE)) {
            content.append("<br><a href=\"" + GRADLE_BUILD_FILE_NAME + "\">")
                   .append(buildTool.getBuildToolFiles().get(0))
                   .append("</a>")
                   .append("\t<a href=\"" + MAVEN_BUILD_FILE_NAME + "\">")
                   .append(buildTool.getBuildToolFiles().get(1))
                   .append("</a>");
            notification.setListener(
                    (n, hyperlinkEvent) -> {
                        if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            int i = -1;
                            switch (hyperlinkEvent.getDescription()) {
                                case GRADLE_BUILD_FILE_NAME:
                                    i = 0;
                                    break;
                                case MAVEN_BUILD_FILE_NAME:
                                    i = 1;
                                    break;
                            }
                            OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project,
                                    Objects.requireNonNull(LocalFileSystem.getInstance()
                                                                          .refreshAndFindFileByPath(path.resolve(buildTool
                                                                                  .getBuildToolFiles().get(i))
                                                                                                        .toString())));
                            FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
                        }
                    });
        }
        notification.setContent(content.toString());
        notification.notify(project);
    }
}