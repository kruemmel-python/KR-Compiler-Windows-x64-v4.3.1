import kr.compile.ProjectArtifactIdentity;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class ProjectArtifactIdentityTest {
    public static void main(String[] args) throws Exception {
        File root = Files.createTempDirectory("kr-artifact-identity-").toFile();
        File first = new File(root, "Alpha");
        File second = new File(root, "Beta");
        first.mkdirs(); second.mkdirs();
        Files.writeString(new File(first, "alpha.krproject").toPath(), "[project]\nname = \"Mein Spiel\"\n", StandardCharsets.UTF_8);
        Files.writeString(new File(second, "beta.krproject").toPath(), "[project]\nname = \"Mein Spiel\"\n", StandardCharsets.UTF_8);
        String firstStem = ProjectArtifactIdentity.artifactStem(first);
        String secondStem = ProjectArtifactIdentity.artifactStem(second);
        String firstPackage = ProjectArtifactIdentity.androidPackageId(first);
        String secondPackage = ProjectArtifactIdentity.androidPackageId(second);
        if (firstStem.equals(secondStem) || firstPackage.equals(secondPackage)) {
            throw new AssertionError("Unterschiedliche Projekte erhielten keine unterschiedlichen Kennungen.");
        }
        if (!firstPackage.matches("kr\\.p\\.a[0-9a-f]{11}")) {
            throw new AssertionError("Ungültige Android-Paketkennung: " + firstPackage);
        }
        System.out.println("PROJECT_ARTIFACT_IDENTITY_OK: " + firstStem + " / " + firstPackage);
    }
}
