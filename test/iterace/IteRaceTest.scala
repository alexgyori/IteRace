package iterace
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TestName
import scala.reflect.BeanProperty
import iterace.pointeranalysis.AnalysisScopeBuilder
import iterace.util.log
import iterace.stage.StageConstructor
import iterace.util.debug

abstract class IteRaceTest extends JavaTest {

  val analysisScope = new AnalysisScopeBuilder(
    if (System.getProperty("os.name").contains("Linux"))
      "/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/rt.jar"
    else
      "/System/Library/Frameworks/JavaVM.framework/Classes/classes.jar")

  analysisScope.setExclusionsFile("walaExclusions.txt")
  analysisScope.addBinaryDependency("../lib/parallelArray.mock")

  def analyze(entryClass: String, entryMethod: String, options: Set[IteRaceOption]) = {
    debug("test: " + entryMethod)
    IteRace(entryClass, entryMethod, analysisScope, options)
  }
}