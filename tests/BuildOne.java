import kr.compile.CompilerEngine;
import java.io.File;
public final class BuildOne {
  public static void main(String[] args) throws Exception {
    File project = new File(args[0]);
    CompilerEngine.Target target = CompilerEngine.Target.valueOf(args[1]);
    File artifact = new CompilerEngine(project, target, false, System.out::println).build();
    System.out.println("ARTIFACT=" + artifact);
  }
}
